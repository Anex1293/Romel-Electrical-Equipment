package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import com.example.model.CategoryItem
import com.example.model.MovementType
import com.example.model.OrderLineItem
import com.example.model.OrderStatus
import com.example.model.OrderType
import com.example.model.PriceValidator
import com.example.model.Product
import com.example.model.ProductAvailability
import com.example.model.SalesOrder
import com.example.model.StockMovement
import com.example.model.UserRole
import com.example.model.WishlistItem
import com.example.model.sanitizeFirestoreId
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.snapshots
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Date

class RomelRepository(
    private val context: Context,
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        context.applicationContext,
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Volatile
    private var activeAdminUsername: String? = null

    @Volatile
    private var activeAdminProofHash: String? = null

    private val _localWishlistFlow by lazy { MutableStateFlow(loadWishlistFromPrefs()) }
    private val _localOrdersFlow by lazy { MutableStateFlow(loadOrdersFromPrefs()) }

    fun isAdminLoggedIn(): Boolean = !activeAdminProofHash.isNullOrBlank()
    fun currentAdminUsername(): String? = activeAdminUsername

    fun logoutAdmin() {
        activeAdminUsername = null
        activeAdminProofHash = null
    }

    private fun requireAdminSession(): Pair<String, String> {
        val user = activeAdminUsername?.takeIf { it.isNotBlank() }
        val proof = activeAdminProofHash?.takeIf { it.length == 64 }
        if (user == null || proof == null) {
            throw SecurityException("Administrator login required.")
        }
        return user to proof
    }

    suspend fun isAdminInitialized(): Result<Boolean> {
        return try {
            val snap = db.collection("admin_config").document("status").get().await()
            val initialized = snap.exists() && snap.getBoolean("initialized") == true
            Result.success(initialized)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "admin_config/status")
            Result.failure(e)
        }
    }

    suspend fun setupAdminCredentials(username: String, password: String): Result<String> {
        val validationError = PriceValidator.validateAdminCredentialsInput(username, password)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }
        val cleanUser = username.trim()
        return try {
            val statusRef = db.collection("admin_config").document("status")
            val existingStatus = statusRef.get().await()
            if (existingStatus.exists() && existingStatus.getBoolean("initialized") == true) {
                return Result.failure(IllegalStateException("Admin credentials are already configured. Please sign in."))
            }

            val salt = generateRandomSalt()
            val hash = computeCredentialHash(salt, cleanUser, password)
            val credRef = db.collection("admin_config").document("credentials")

            val batch = db.batch()
            batch.set(
                statusRef,
                mapOf(
                    "initialized" to true,
                    "salt" to salt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                credRef,
                mapOf(
                    "credentialHash" to hash,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            activeAdminUsername = cleanUser
            activeAdminProofHash = hash
            Result.success(cleanUser)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "admin_config")
            Result.failure(e)
        }
    }

    suspend fun loginAdmin(username: String, password: String): Result<String> {
        val cleanUser = username.trim()
        if (cleanUser.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter both admin username and password."))
        }
        return try {
            val statusSnap = db.collection("admin_config").document("status").get().await()
            if (!statusSnap.exists() || statusSnap.getBoolean("initialized") != true) {
                return Result.failure(IllegalStateException("Admin account is not set up yet. Create an Admin account first."))
            }
            val salt = statusSnap.getString("salt")
                ?: return Result.failure(IllegalStateException("Admin configuration is incomplete."))
            val candidateHash = computeCredentialHash(salt, cleanUser, password)

            db.collection("admin_auth_proofs").document("login_check").set(
                mapOf(
                    "proofHash" to candidateHash,
                    "op" to "LOGIN",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            activeAdminUsername = cleanUser
            activeAdminProofHash = candidateHash
            Result.success(cleanUser)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError(e, OperationType.WRITE, "admin_auth_proofs/login_check")
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Result.failure(SecurityException("Invalid admin username or password."))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "admin_auth_proofs/login_check")
            Result.failure(e)
        }
    }

    private fun WriteBatch.attachAdminProof(proofHash: String, op: String) {
        val proofRef = db.collection("admin_auth_proofs").document("batch_proof")
        set(
            proofRef,
            mapOf(
                "proofHash" to proofHash,
                "op" to op,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
    }

    fun observeCategories(): Flow<List<CategoryItem>> = flow {
        val path = "categories"
        emitAll(
            db.collection("categories")
                .whereEqualTo("catalogStatus", "ACTIVE")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CategoryItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.sortOrder }
                }
                .catch { error ->
                    rethrowIfPureCancellation(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeProducts(): Flow<List<Product>> = flow {
        val path = "products"
        emitAll(
            db.collection("products")
                .whereEqualTo("catalogStatus", "ACTIVE")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Product::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    rethrowIfPureCancellation(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun fetchProducts(): Result<List<Product>> {
        val path = "products"
        return try {
            val snap = db.collection("products")
                .whereEqualTo("catalogStatus", "ACTIVE")
                .get()
                .await()
            val items = snap.toObjects(
                Product::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedBy { it.catalogPage }
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    fun observeOrders(): Flow<List<SalesOrder>> = _localOrdersFlow.asStateFlow()

    fun observeUserOrderHistory(): Flow<List<SalesOrder>> = _localOrdersFlow.asStateFlow()

    fun observeWishlist(): Flow<List<WishlistItem>> = _localWishlistFlow.asStateFlow()

    fun observeStockMovements(): Flow<List<StockMovement>> = flow {
        val path = "stock_movements"
        emitAll(
            db.collection("stock_movements")
                .whereIn("movementType", listOf("STOCK_IN", "STOCK_OUT", "SALE", "PRICE_UPDATE", "IMPORT"))
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        StockMovement::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    rethrowIfPureCancellation(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    private fun rethrowIfPureCancellation(
        error: Throwable,
        op: OperationType,
        path: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    suspend fun seedInitialCatalogIfEmpty(forceSync: Boolean = false): Result<Int> {
        return try {
            val (adminUser, proofHash) = requireAdminSession()
            if (!forceSync) {
                val snap = db.collection("products")
                    .whereEqualTo("catalogStatus", "ACTIVE")
                    .limit(1)
                    .get()
                    .await()
                if (!snap.isEmpty) {
                    return Result.success(0)
                }
            }

            // Seed missing categories in batches of 15
            val existingCatIds = db.collection("categories")
                .whereEqualTo("catalogStatus", "ACTIVE")
                .get()
                .await()
                .documents
                .map { it.id }
                .toSet()

            val catsToCreate = RomelCatalogSeed.categories.filter { it.id !in existingCatIds }
            catsToCreate.chunked(15).forEach { chunk ->
                val catBatch = db.batch()
                catBatch.attachAdminProof(proofHash, "SEED")
                for (cat in chunk) {
                    val catRef = db.collection("categories").document(cat.id)
                    catBatch.set(
                        catRef,
                        mapOf(
                            "name" to cat.name,
                            "description" to cat.description,
                            "sortOrder" to cat.sortOrder,
                            "catalogStatus" to "ACTIVE",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                catBatch.commit().await()
            }

            val existingIds = if (forceSync) {
                db.collection("products")
                    .whereEqualTo("catalogStatus", "ACTIVE")
                    .get()
                    .await()
                    .documents
                    .map { it.id }
                    .toSet()
            } else emptySet()

            val toCreate = RomelCatalogSeed.allProducts.filter { it.id !in existingIds }
            var createdCount = 0
            toCreate.chunked(15).forEach { chunk ->
                val batch = db.batch()
                batch.attachAdminProof(proofHash, "SEED")
                for (product in chunk) {
                    val docRef = db.collection("products").document(product.id)
                    batch.set(docRef, productToCreateMap(product, adminUser))
                    createdCount++
                }
                batch.commit().await()
            }
            Result.success(createdCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "products")
            Result.failure(e)
        }
    }

    suspend fun createCategory(name: String, description: String, sortOrder: Long): Result<Unit> {
        val docId = sanitizeFirestoreId("cat_${name.lowercase()}_${System.currentTimeMillis().toString().takeLast(4)}")
        val path = "categories/$docId"
        return try {
            val (_, proofHash) = requireAdminSession()
            require(name.isNotBlank() && name.length <= 120) { "Category name must be 1–120 characters." }
            val batch = db.batch()
            batch.attachAdminProof(proofHash, "CREATE")
            batch.set(
                db.collection("categories").document(docId),
                mapOf(
                    "name" to name.trim().take(120),
                    "description" to description.trim().take(500),
                    "sortOrder" to sortOrder.coerceAtLeast(0L),
                    "catalogStatus" to "ACTIVE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCategory(
        category: CategoryItem,
        newName: String,
        newDescription: String
    ): Result<Unit> {
        val docId = category.id.ifBlank { return Result.failure(IllegalArgumentException("Invalid category ID.")) }
        val path = "categories/$docId"
        return try {
            val (_, proofHash) = requireAdminSession()
            require(newName.isNotBlank() && newName.length <= 120) { "Category name must be 1–120 characters." }
            val batch = db.batch()
            batch.attachAdminProof(proofHash, "UPDATE")
            batch.update(
                db.collection("categories").document(docId),
                mapOf(
                    "name" to newName.trim().take(120),
                    "description" to newDescription.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        val path = "categories/$categoryId"
        return try {
            val (_, proofHash) = requireAdminSession()
            require(categoryId.isNotBlank()) { "Invalid category ID." }
            val batch = db.batch()
            batch.attachAdminProof(proofHash, "DELETE")
            batch.delete(db.collection("categories").document(categoryId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertProduct(product: Product, isNew: Boolean): Result<Unit> {
        val docId = if (isNew) sanitizeFirestoreId(product.code) else product.id.ifBlank { sanitizeFirestoreId(product.code) }
        val path = "products/$docId"
        return try {
            val (adminUser, proofHash) = requireAdminSession()
            require(product.code.isNotBlank() && product.code.length <= 64) { "Product code must be 1–64 chars" }
            require(product.name.isNotBlank() && product.name.length <= 160) { "Product name must be 1–160 chars" }
            val docRef = db.collection("products").document(docId)
            val batch = db.batch()
            if (isNew) {
                val existing = docRef.get().await()
                if (existing.exists()) {
                    return Result.failure(IllegalArgumentException("Duplicate product code: ${product.code} already exists."))
                }
                batch.attachAdminProof(proofHash, "CREATE")
                batch.set(docRef, productToCreateMap(product.copy(id = docId), adminUser))
            } else {
                batch.attachAdminProof(proofHash, "UPDATE")
                val updateMap = mutableMapOf<String, Any>(
                    "code" to product.code.trim().take(64),
                    "name" to product.name.trim().take(160),
                    "category" to product.category.trim().take(120),
                    "series" to product.series.trim().take(120),
                    "description" to product.description.trim().take(1000),
                    "specifications" to product.specifications.trim().take(2000),
                    "catalogPage" to product.catalogPage.coerceAtLeast(0L),
                    "imageAsset" to product.imageAsset.take(256),
                    "unit" to product.unit.trim().take(32).ifBlank { "PCS" },
                    "piecesPerBox" to product.piecesPerBox.coerceAtLeast(1L),
                    "piecesPerCarton" to product.piecesPerCarton.coerceAtLeast(1L),
                    "lowStockThreshold" to product.lowStockThreshold.coerceAtLeast(0L),
                    "needsReview" to product.needsReview,
                    "catalogStatus" to product.catalogStatus,
                    "availabilityStatus" to product.parsedAvailability.name,
                    "updatedBy" to adminUser,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                product.customImageUri?.takeIf { it.isNotBlank() }?.let { updateMap["customImageUri"] = it.take(500) }
                product.sellingPrice?.takeIf { it >= 0.0 }?.let { updateMap["sellingPrice"] = it }
                product.wholesalePrice?.takeIf { it >= 0.0 }?.let { updateMap["wholesalePrice"] = it }
                product.retailPrice?.takeIf { it >= 0.0 }?.let { updateMap["retailPrice"] = it }
                product.discountPrice?.takeIf { it >= 0.0 }?.let { updateMap["discountPrice"] = it }
                product.stockQuantity?.takeIf { it >= 0L }?.let { updateMap["stockQuantity"] = it }
                product.reviewNote?.takeIf { it.isNotBlank() }?.let { updateMap["reviewNote"] = it.take(500) }
                batch.update(docRef, updateMap)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val path = "products/$productId"
        return try {
            val (_, proofHash) = requireAdminSession()
            require(productId.isNotBlank()) { "Invalid product ID." }
            val batch = db.batch()
            batch.attachAdminProof(proofHash, "DELETE")
            batch.delete(db.collection("products").document(productId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun setProductAvailability(
        product: Product,
        availability: ProductAvailability
    ): Result<Unit> {
        val docId = product.id.ifBlank { sanitizeFirestoreId(product.code) }
        val path = "products/$docId"
        return try {
            val (adminUser, proofHash) = requireAdminSession()
            val prodRef = db.collection("products").document(docId)
            val snap = prodRef.get().await()
            val batch = db.batch()
            if (!snap.exists()) {
                batch.attachAdminProof(proofHash, "CREATE")
                batch.set(
                    prodRef,
                    productToCreateMap(
                        product.copy(id = docId, availabilityStatus = availability.name),
                        adminUser
                    )
                )
            } else {
                batch.attachAdminProof(proofHash, "UPDATE")
                val updates = mutableMapOf<String, Any>(
                    "availabilityStatus" to availability.name,
                    "updatedBy" to adminUser,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (availability == ProductAvailability.OUT_OF_STOCK) {
                    updates["stockQuantity"] = 0L
                }
                batch.update(prodRef, updates)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProductPrices(
        product: Product,
        sellingPrice: Double?,
        wholesalePrice: Double?,
        retailPrice: Double?,
        discountPrice: Double?,
        performedByName: String
    ): Result<Unit> {
        val docId = product.id.ifBlank { sanitizeFirestoreId(product.code) }
        val path = "products/$docId"
        return try {
            val (adminUser, proofHash) = requireAdminSession()
            val validationError = PriceValidator.validateValues(
                selling = sellingPrice,
                wholesale = wholesalePrice,
                retail = retailPrice,
                discount = discountPrice
            )
            if (validationError != null) {
                return Result.failure(IllegalArgumentException(validationError))
            }

            val prodRef = db.collection("products").document(docId)
            val existsInCloud = prodRef.get().await().exists()
            val batch = db.batch()
            if (!existsInCloud) {
                val createdProd = product.copy(
                    id = docId,
                    sellingPrice = sellingPrice,
                    wholesalePrice = wholesalePrice,
                    retailPrice = retailPrice,
                    discountPrice = discountPrice
                )
                batch.attachAdminProof(proofHash, "CREATE")
                batch.set(prodRef, productToCreateMap(createdProd, adminUser))
            } else {
                batch.attachAdminProof(proofHash, "UPDATE")
                val updates = mutableMapOf<String, Any>(
                    "updatedBy" to adminUser,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                updates["sellingPrice"] = sellingPrice?.coerceAtLeast(0.0) ?: FieldValue.delete()
                updates["wholesalePrice"] = wholesalePrice?.coerceAtLeast(0.0) ?: FieldValue.delete()
                updates["retailPrice"] = retailPrice?.coerceAtLeast(0.0) ?: FieldValue.delete()
                updates["discountPrice"] = discountPrice?.coerceAtLeast(0.0) ?: FieldValue.delete()
                batch.update(prodRef, updates)
            }

            val movRef = db.collection("stock_movements").document()
            val movMap = mutableMapOf<String, Any>(
                "productId" to docId,
                "productCode" to product.code,
                "productName" to product.name.take(160),
                "movementType" to MovementType.PRICE_UPDATE.name,
                "quantityDelta" to 0L,
                "reason" to "Admin updated ETB pricing",
                "performedByName" to performedByName.take(100).ifBlank { adminUser },
                "createdAt" to FieldValue.serverTimestamp()
            )
            product.sellingPrice?.let { movMap["previousPrice"] = it }
            sellingPrice?.let { movMap["newPrice"] = it }
            batch.set(movRef, movMap)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adjustStock(
        product: Product,
        delta: Long,
        reason: String,
        performedByName: String
    ): Result<Unit> {
        val docId = product.id.ifBlank { sanitizeFirestoreId(product.code) }
        val path = "products/$docId"
        return try {
            val (adminUser, proofHash) = requireAdminSession()
            val previous = product.stockQuantity ?: 0L
            val next = (previous + delta).coerceAtLeast(0L)
            val movementType = if (delta >= 0L) MovementType.STOCK_IN else MovementType.STOCK_OUT
            val nextAvailability = when {
                product.parsedAvailability == ProductAvailability.UNAVAILABLE -> ProductAvailability.UNAVAILABLE.name
                next <= 0L -> ProductAvailability.OUT_OF_STOCK.name
                else -> ProductAvailability.AVAILABLE.name
            }

            val prodRef = db.collection("products").document(docId)
            val existsInCloud = prodRef.get().await().exists()
            val batch = db.batch()
            if (!existsInCloud) {
                batch.attachAdminProof(proofHash, "CREATE")
                batch.set(
                    prodRef,
                    productToCreateMap(
                        product.copy(id = docId, stockQuantity = next, availabilityStatus = nextAvailability),
                        adminUser
                    )
                )
            } else {
                batch.attachAdminProof(proofHash, "UPDATE")
                batch.update(
                    prodRef,
                    mapOf(
                        "stockQuantity" to next,
                        "availabilityStatus" to nextAvailability,
                        "updatedBy" to adminUser,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            val movRef = db.collection("stock_movements").document()
            batch.set(
                movRef,
                mapOf(
                    "productId" to docId,
                    "productCode" to product.code,
                    "productName" to product.name.take(160),
                    "movementType" to movementType.name,
                    "quantityDelta" to delta,
                    "previousStock" to previous,
                    "newStock" to next,
                    "reason" to reason.trim().take(500).ifBlank { movementType.label },
                    "performedByName" to performedByName.take(100).ifBlank { adminUser },
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitOrder(
        customerName: String,
        customerPhone: String,
        orderType: OrderType,
        status: OrderStatus,
        items: List<OrderLineItem>,
        orderDiscount: Double,
        notes: String?,
        createdByName: String,
        createdByRole: UserRole,
        productsById: Map<String, Product>,
        allowAdminBackorder: Boolean
    ): Result<SalesOrder> {
        return try {
            require(items.isNotEmpty() && items.size <= 100) { "Order must contain 1–100 line items." }

            if (status == OrderStatus.CONFIRMED && orderType == OrderType.SALES_ORDER) {
                val (adminUser, proofHash) = requireAdminSession()
                for (item in items) {
                    val prod = productsById[item.productId]
                    val avail = prod?.stockQuantity ?: 0L
                    if (!allowAdminBackorder && item.totalPieces > avail) {
                        return Result.failure(
                            IllegalStateException("Insufficient stock for ${item.code}: requested ${item.totalPieces} PCS, available $avail.")
                        )
                    }
                }

                // Deduct stock in Firestore atomically with admin batch proof
                val batch = db.batch()
                batch.attachAdminProof(proofHash, "UPDATE")
                var hasFirestoreOps = false
                for (item in items) {
                    val prod = productsById[item.productId]
                    if (prod != null && prod.stockQuantity != null) {
                        val prevStock = prod.stockQuantity
                        val newStock = (prevStock - item.totalPieces).coerceAtLeast(0L)
                        val prodRef = db.collection("products").document(prod.id)
                        batch.update(
                            prodRef,
                            mapOf(
                                "stockQuantity" to newStock,
                                "availabilityStatus" to if (newStock <= 0L) ProductAvailability.OUT_OF_STOCK.name else ProductAvailability.AVAILABLE.name,
                                "updatedBy" to adminUser,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        val movRef = db.collection("stock_movements").document()
                        batch.set(
                            movRef,
                            mapOf(
                                "productId" to prod.id,
                                "productCode" to prod.code,
                                "productName" to prod.name.take(160),
                                "movementType" to MovementType.SALE.name,
                                "quantityDelta" to -item.totalPieces,
                                "previousStock" to prevStock,
                                "newStock" to newStock,
                                "reason" to "Confirmed Sales Order",
                                "performedByName" to createdByName.take(100).ifBlank { adminUser },
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                        hasFirestoreOps = true
                    }
                }
                if (hasFirestoreOps) {
                    batch.commit().await()
                }
            }

            val subtotal = items.sumOf { it.lineTotal }
            val totalAmount = (subtotal - orderDiscount).coerceAtLeast(0.0)
            val totalQty = items.sumOf { it.quantity }.coerceAtLeast(1L)
            val prefix = if (orderType == OrderType.SALES_ORDER) "SO" else "QT"
            val orderNumber = "$prefix-${System.currentTimeMillis().toString().takeLast(6)}"
            val orderId = "ord_${System.currentTimeMillis()}"
            val nowTs = Timestamp(Date())

            val serializedItems = items.map { item ->
                mapOf(
                    "productId" to item.productId,
                    "code" to item.code,
                    "name" to item.name,
                    "unit" to item.unit,
                    "unitMultiplier" to item.unitMultiplier,
                    "unitPrice" to item.unitPrice,
                    "quantity" to item.quantity,
                    "discount" to item.discount,
                    "lineTotal" to item.lineTotal,
                    "imageAsset" to item.imageAsset
                )
            }

            val savedOrder = SalesOrder(
                id = orderId,
                orderNumber = orderNumber,
                userId = if (createdByRole == UserRole.ADMIN) "admin" else "customer",
                createdByName = createdByName.ifBlank { "Customer" },
                createdByRole = createdByRole.name,
                customerName = customerName.trim().take(120),
                customerPhone = customerPhone.trim().take(40),
                orderType = orderType.name,
                status = status.name,
                items = serializedItems,
                totalProducts = items.size.toLong(),
                totalQuantity = totalQty,
                subtotal = subtotal,
                discount = orderDiscount.coerceAtLeast(0.0),
                totalAmount = totalAmount,
                notes = notes?.trim()?.takeIf { it.isNotBlank() }?.take(1000),
                createdAt = nowTs,
                updatedAt = nowTs
            )

            val updatedList = listOf(savedOrder) + _localOrdersFlow.value
            saveOrdersToPrefs(updatedList)
            _localOrdersFlow.value = updatedList
            Result.success(savedOrder)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "orders")
            Result.failure(e)
        }
    }

    suspend fun importProductsBatch(
        imported: List<Product>,
        existingCodes: Set<String>,
        performedByName: String
    ): Result<Pair<Int, Int>> {
        return try {
            val (adminUser, proofHash) = requireAdminSession()
            var added = 0
            var skippedDuplicates = 0
            val uniqueToInsert = mutableListOf<Product>()
            val seenCodes = existingCodes.map { it.uppercase() }.toMutableSet()

            for (raw in imported) {
                val normalized = raw.code.trim().uppercase()
                if (normalized.isBlank() || normalized in seenCodes) {
                    skippedDuplicates++
                } else {
                    seenCodes.add(normalized)
                    uniqueToInsert.add(raw.copy(id = sanitizeFirestoreId(raw.code)))
                }
            }

            uniqueToInsert.chunked(10).forEach { chunk ->
                val batch = db.batch()
                batch.attachAdminProof(proofHash, "SEED")
                for (prod in chunk) {
                    val docRef = db.collection("products").document(prod.id)
                    batch.set(docRef, productToCreateMap(prod, adminUser))
                    val movRef = db.collection("stock_movements").document()
                    batch.set(
                        movRef,
                        mapOf(
                            "productId" to prod.id,
                            "productCode" to prod.code,
                            "productName" to prod.name.take(160),
                            "movementType" to MovementType.IMPORT.name,
                            "quantityDelta" to (prod.stockQuantity ?: 0L),
                            "reason" to if (prod.needsReview) "Imported (Flagged for Admin Review)" else "Catalog Import",
                            "performedByName" to performedByName.take(100).ifBlank { adminUser },
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    added++
                }
                batch.commit().await()
            }

            Result.success(added to skippedDuplicates)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "products")
            Result.failure(e)
        }
    }

    private fun productToCreateMap(product: Product, adminUser: String): Map<String, Any> {
        val map = mutableMapOf<String, Any?>(
            "code" to product.code.trim().take(64),
            "name" to product.name.trim().take(160),
            "category" to product.category.trim().take(120),
            "series" to product.series.trim().take(120),
            "description" to product.description.trim().take(1000),
            "specifications" to product.specifications.trim().take(2000),
            "catalogPage" to product.catalogPage.coerceAtLeast(0L),
            "imageAsset" to product.imageAsset.take(256),
            "customImageUri" to product.customImageUri?.takeIf { it.isNotBlank() }?.take(500),
            "unit" to product.unit.trim().take(32).ifBlank { "PCS" },
            "piecesPerBox" to product.piecesPerBox.coerceAtLeast(1L),
            "piecesPerCarton" to product.piecesPerCarton.coerceAtLeast(1L),
            "sellingPrice" to product.sellingPrice?.takeIf { it >= 0.0 },
            "wholesalePrice" to product.wholesalePrice?.takeIf { it >= 0.0 },
            "retailPrice" to product.retailPrice?.takeIf { it >= 0.0 },
            "discountPrice" to product.discountPrice?.takeIf { it >= 0.0 },
            "stockQuantity" to product.stockQuantity?.takeIf { it >= 0L },
            "lowStockThreshold" to product.lowStockThreshold.coerceAtLeast(0L),
            "needsReview" to product.needsReview,
            "reviewNote" to product.reviewNote?.takeIf { it.isNotBlank() }?.take(500),
            "catalogStatus" to product.catalogStatus,
            "availabilityStatus" to product.parsedAvailability.name,
            "updatedBy" to adminUser,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return map.filterValues { it != null } as Map<String, Any>
    }

    suspend fun addToWishlist(product: Product): Result<WishlistItem> {
        val productId = product.id.ifBlank { sanitizeFirestoreId(product.code) }
        return try {
            val nowTs = Timestamp(Date())
            val newItem = WishlistItem(
                id = productId,
                userId = "customer",
                productId = productId,
                productCode = product.code,
                productName = product.name,
                category = product.category,
                imageAsset = product.imageAsset,
                customImageUri = product.customImageUri,
                createdAt = nowTs,
                updatedAt = nowTs
            )
            val updated = listOf(newItem) + _localWishlistFlow.value.filterNot { it.productId == productId }
            saveWishlistToPrefs(updated)
            _localWishlistFlow.value = updated
            Result.success(newItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromWishlist(productId: String): Result<Unit> {
        return try {
            val updated = _localWishlistFlow.value.filterNot { it.productId == productId }
            saveWishlistToPrefs(updated)
            _localWishlistFlow.value = updated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun loadWishlistFromPrefs(): List<WishlistItem> {
        val raw = prefs.getString(KEY_WISHLIST, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
                val sec = obj.optLong("createdSec", 0L)
                val ts = if (sec > 0L) Timestamp(sec, 0) else null
                WishlistItem(
                    id = obj.optString("id"),
                    userId = "customer",
                    productId = obj.optString("productId"),
                    productCode = obj.optString("productCode"),
                    productName = obj.optString("productName"),
                    category = obj.optString("category"),
                    imageAsset = obj.optString("imageAsset"),
                    customImageUri = obj.optString("customImageUri").takeIf { it.isNotBlank() },
                    createdAt = ts,
                    updatedAt = ts
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveWishlistToPrefs(items: List<WishlistItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("productId", item.productId)
                    put("productCode", item.productCode)
                    put("productName", item.productName)
                    put("category", item.category)
                    put("imageAsset", item.imageAsset)
                    put("customImageUri", item.customImageUri ?: "")
                    put("createdSec", item.createdAt?.seconds ?: (System.currentTimeMillis() / 1000L))
                }
            )
        }
        prefs.edit().putString(KEY_WISHLIST, arr.toString()).apply()
    }

    private fun loadOrdersFromPrefs(): List<SalesOrder> {
        val raw = prefs.getString(KEY_ORDERS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
                val sec = obj.optLong("createdSec", 0L)
                val ts = if (sec > 0L) Timestamp(sec, 0) else null
                val itemsArr = obj.optJSONArray("items") ?: JSONArray()
                val parsedItems = (0 until itemsArr.length()).mapNotNull { iIdx ->
                    val iObj = itemsArr.optJSONObject(iIdx) ?: return@mapNotNull null
                    mapOf<String, Any>(
                        "productId" to iObj.optString("productId"),
                        "code" to iObj.optString("code"),
                        "name" to iObj.optString("name"),
                        "unit" to iObj.optString("unit", "PCS"),
                        "unitMultiplier" to iObj.optLong("unitMultiplier", 1L),
                        "unitPrice" to iObj.optDouble("unitPrice", 0.0),
                        "quantity" to iObj.optLong("quantity", 1L),
                        "discount" to iObj.optDouble("discount", 0.0),
                        "lineTotal" to iObj.optDouble("lineTotal", 0.0),
                        "imageAsset" to iObj.optString("imageAsset")
                    )
                }
                SalesOrder(
                    id = obj.optString("id"),
                    orderNumber = obj.optString("orderNumber"),
                    userId = obj.optString("userId", "customer"),
                    createdByName = obj.optString("createdByName", "Customer"),
                    createdByRole = obj.optString("createdByRole", UserRole.VIEWER.name),
                    customerName = obj.optString("customerName"),
                    customerPhone = obj.optString("customerPhone"),
                    orderType = obj.optString("orderType", OrderType.QUOTATION.name),
                    status = obj.optString("status", OrderStatus.QUOTATION.name),
                    items = parsedItems,
                    totalProducts = obj.optLong("totalProducts", parsedItems.size.toLong()),
                    totalQuantity = obj.optLong("totalQuantity", 1L),
                    subtotal = obj.optDouble("subtotal", 0.0),
                    discount = obj.optDouble("discount", 0.0),
                    totalAmount = obj.optDouble("totalAmount", 0.0),
                    notes = obj.optString("notes").takeIf { it.isNotBlank() },
                    createdAt = ts,
                    updatedAt = ts
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveOrdersToPrefs(orders: List<SalesOrder>) {
        val arr = JSONArray()
        orders.forEach { order ->
            val itemsArr = JSONArray()
            order.items.forEach { map ->
                itemsArr.put(
                    JSONObject().apply {
                        put("productId", map["productId"] ?: "")
                        put("code", map["code"] ?: "")
                        put("name", map["name"] ?: "")
                        put("unit", map["unit"] ?: "PCS")
                        put("unitMultiplier", (map["unitMultiplier"] as? Number)?.toLong() ?: 1L)
                        put("unitPrice", (map["unitPrice"] as? Number)?.toDouble() ?: 0.0)
                        put("quantity", (map["quantity"] as? Number)?.toLong() ?: 1L)
                        put("discount", (map["discount"] as? Number)?.toDouble() ?: 0.0)
                        put("lineTotal", (map["lineTotal"] as? Number)?.toDouble() ?: 0.0)
                        put("imageAsset", map["imageAsset"] ?: "")
                    }
                )
            }
            arr.put(
                JSONObject().apply {
                    put("id", order.id)
                    put("orderNumber", order.orderNumber)
                    put("userId", order.userId)
                    put("createdByName", order.createdByName)
                    put("createdByRole", order.createdByRole)
                    put("customerName", order.customerName)
                    put("customerPhone", order.customerPhone)
                    put("orderType", order.orderType)
                    put("status", order.status)
                    put("items", itemsArr)
                    put("totalProducts", order.totalProducts)
                    put("totalQuantity", order.totalQuantity)
                    put("subtotal", order.subtotal)
                    put("discount", order.discount)
                    put("totalAmount", order.totalAmount)
                    put("notes", order.notes ?: "")
                    put("createdSec", order.createdAt?.seconds ?: (System.currentTimeMillis() / 1000L))
                }
            )
        }
        prefs.edit().putString(KEY_ORDERS, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "romel_customer_store"
        private const val KEY_WISHLIST = "wishlist_items_v1"
        private const val KEY_ORDERS = "customer_orders_v1"

        fun sha256Hex(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        fun generateRandomSalt(): String {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun computeCredentialHash(salt: String, username: String, password: String): String =
            sha256Hex("$salt:${username.trim().lowercase()}:$password")
    }
}
