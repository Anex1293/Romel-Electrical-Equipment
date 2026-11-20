package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val label: String) {
    ADMIN("Administrator"),
    SALES_REP("Sales Representative"),
    VIEWER("Viewer / Customer");

    companion object {
        fun fromRaw(raw: String?): UserRole = when (raw?.trim()?.uppercase()) {
            "ADMIN" -> ADMIN
            "SALES_REP", "SALES" -> SALES_REP
            else -> VIEWER
        }
    }
}

enum class UnitOfMeasure(val label: String) {
    PCS("PCS"),
    BOX("Box"),
    CARTON("Carton"),
    SET("Set"),
    PACK("Pack"),
    OTHER("Other");

    companion object {
        fun fromRaw(raw: String?): UnitOfMeasure =
            entries.firstOrNull { it.label.equals(raw, ignoreCase = true) || it.name == raw } ?: PCS
    }
}

enum class StockAvailabilityFilter(val label: String) {
    ALL("All"),
    IN_STOCK("In Stock"),
    LOW_STOCK("Low Stock"),
    OUT_OF_STOCK("Out of Stock"),
    PRICE_SET("Price Set"),
    NEEDS_REVIEW("Needs Review")
}

enum class ProductAvailability(val label: String) {
    AVAILABLE("Available"),
    OUT_OF_STOCK("Out of Stock"),
    UNAVAILABLE("Hidden / Unavailable");

    companion object {
        fun fromRaw(raw: String?): ProductAvailability = when (raw?.trim()?.uppercase()) {
            "OUT_OF_STOCK" -> OUT_OF_STOCK
            "UNAVAILABLE" -> UNAVAILABLE
            else -> AVAILABLE
        }
    }
}

enum class ProductSortOption(val label: String) {
    CATALOG_ORDER("Catalog Order"),
    NAME_ASC("Name (A–Z)"),
    PRICE_LOW_HIGH("Price (Low–High)"),
    PRICE_HIGH_LOW("Price (High–Low)"),
    STOCK_LOW_HIGH("Stock (Low–High)"),
    RECENTLY_ADDED("Recently Added")
}

enum class OrderType(val label: String) {
    QUOTATION("Quotation"),
    SALES_ORDER("Sales Order");

    companion object {
        fun fromRaw(raw: String?): OrderType =
            entries.firstOrNull { it.name == raw } ?: QUOTATION
    }
}

enum class OrderStatus(val label: String) {
    DRAFT("Draft"),
    QUOTATION("Quotation"),
    CONFIRMED("Confirmed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromRaw(raw: String?): OrderStatus =
            entries.firstOrNull { it.name == raw } ?: DRAFT
    }
}

enum class MovementType(val label: String) {
    STOCK_IN("Stock In"),
    STOCK_OUT("Stock Out"),
    SALE("Sale"),
    PRICE_UPDATE("Price Update"),
    IMPORT("Import");

    companion object {
        fun fromRaw(raw: String?): MovementType =
            entries.firstOrNull { it.name == raw } ?: STOCK_IN
    }
}

@Keep
@IgnoreExtraProperties
data class UserRoleProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = UserRole.VIEWER.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedRole: UserRole get() = UserRole.fromRaw(role)
}

@Keep
@IgnoreExtraProperties
data class CategoryItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val sortOrder: Long = 0L,
    val catalogStatus: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val code: String = "",
    val name: String = "",
    val category: String = "",
    val series: String = "",
    val description: String = "",
    val specifications: String = "",
    val catalogPage: Long = 0L,
    val imageAsset: String = "",
    val customImageUri: String? = null,
    val unit: String = "PCS",
    val piecesPerBox: Long = 10L,
    val piecesPerCarton: Long = 100L,
    val sellingPrice: Double? = null,
    val wholesalePrice: Double? = null,
    val retailPrice: Double? = null,
    val discountPrice: Double? = null,
    val stockQuantity: Long? = null,
    val lowStockThreshold: Long = 10L,
    val needsReview: Boolean = false,
    val reviewNote: String? = null,
    val catalogStatus: String = "ACTIVE",
    val availabilityStatus: String = ProductAvailability.AVAILABLE.name,
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedAvailability: ProductAvailability
        get() = ProductAvailability.fromRaw(availabilityStatus)

    val isVisibleToCustomers: Boolean
        get() = catalogStatus == "ACTIVE" && parsedAvailability != ProductAvailability.UNAVAILABLE

    val effectiveUnitPrice: Double?
        get() = discountPrice ?: sellingPrice ?: retailPrice

    val price: Double?
        get() = effectiveUnitPrice

    val stockStatusLabel: String
        get() = when {
            parsedAvailability == ProductAvailability.UNAVAILABLE -> "Unavailable"
            parsedAvailability == ProductAvailability.OUT_OF_STOCK -> "Out of Stock"
            stockQuantity == null -> "Available"
            stockQuantity <= 0L -> "Out of Stock"
            stockQuantity <= lowStockThreshold -> "Low Stock ($stockQuantity $unit)"
            else -> "In Stock ($stockQuantity $unit)"
        }

    val stockIsOut: Boolean
        get() = parsedAvailability == ProductAvailability.OUT_OF_STOCK ||
            (stockQuantity != null && stockQuantity <= 0L)
    val stockIsLow: Boolean
        get() = parsedAvailability == ProductAvailability.AVAILABLE &&
            stockQuantity != null && stockQuantity in 1L..lowStockThreshold
}

data class OrderLineItem(
    val productId: String = "",
    val code: String = "",
    val name: String = "",
    val unit: String = "PCS",
    val unitMultiplier: Long = 1L,
    val unitPrice: Double = 0.0,
    val quantity: Long = 1L,
    val discount: Double = 0.0,
    val imageAsset: String = "",
    val customImageUri: String? = null
) {
    val subtotal: Double get() = unitPrice * quantity
    val lineTotal: Double get() = (subtotal - discount).coerceAtLeast(0.0)
    val totalPieces: Long get() = quantity * unitMultiplier
}

@Keep
@IgnoreExtraProperties
data class SalesOrder(
    @DocumentId val id: String = "",
    val orderNumber: String = "",
    val userId: String = "",
    val createdByName: String = "",
    val createdByRole: String = UserRole.VIEWER.name,
    val customerName: String = "",
    val customerPhone: String = "",
    val orderType: String = OrderType.QUOTATION.name,
    val status: String = OrderStatus.DRAFT.name,
    val items: List<Map<String, Any>> = emptyList(),
    val totalProducts: Long = 0L,
    val totalQuantity: Long = 0L,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedItems(): List<OrderLineItem> = items.map { map ->
        OrderLineItem(
            productId = map["productId"] as? String ?: "",
            code = map["code"] as? String ?: "",
            name = map["name"] as? String ?: "",
            unit = map["unit"] as? String ?: "PCS",
            unitMultiplier = (map["unitMultiplier"] as? Number)?.toLong() ?: 1L,
            unitPrice = (map["unitPrice"] as? Number)?.toDouble() ?: 0.0,
            quantity = (map["quantity"] as? Number)?.toLong() ?: 1L,
            discount = (map["discount"] as? Number)?.toDouble() ?: 0.0,
            imageAsset = map["imageAsset"] as? String ?: "",
            customImageUri = map["customImageUri"] as? String
        )
    }
}

@Keep
@IgnoreExtraProperties
data class StockMovement(
    @DocumentId val id: String = "",
    val productId: String = "",
    val productCode: String = "",
    val productName: String = "",
    val movementType: String = MovementType.STOCK_IN.name,
    val quantityDelta: Long = 0L,
    val previousStock: Long? = null,
    val newStock: Long? = null,
    val previousPrice: Double? = null,
    val newPrice: Double? = null,
    val reason: String = "",
    val userId: String = "",
    val performedByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WishlistItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productCode: String = "",
    val productName: String = "",
    val category: String = "",
    val imageAsset: String = "",
    val customImageUri: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFallbackProduct(): Product = Product(
        id = productId,
        code = productCode,
        name = productName,
        category = category,
        series = category,
        imageAsset = imageAsset,
        customImageUri = customImageUri,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun sanitizeFirestoreId(raw: String): String {
    val cleaned = raw.trim()
        .replace("\\", "_")
        .replace("/", "_")
        .replace(" ", "_")
        .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        .take(120)
    return cleaned.ifBlank { "ITEM_${System.currentTimeMillis()}" }
}

fun formatEtb(amount: Double?): String {
    if (amount == null) return "Price not set"
    return String.format(java.util.Locale.US, "%,.2f ETB", amount)
}
