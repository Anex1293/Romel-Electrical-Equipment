package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CatalogImportService
import com.example.data.RomelCatalogSeed
import com.example.data.RomelRepository
import com.example.model.CategoryItem
import com.example.model.OrderLineItem
import com.example.model.OrderStatus
import com.example.model.OrderType
import com.example.model.PriceValidator
import com.example.model.Product
import com.example.model.ProductAvailability
import com.example.model.ProductSortOption
import com.example.model.SalesOrder
import com.example.model.StockAvailabilityFilter
import com.example.model.StockMovement
import com.example.model.UserRole
import com.example.model.WishlistItem
import com.example.model.formatEtb
import com.example.model.sanitizeFirestoreId
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Date

enum class AppSection(val label: String, val adminOnly: Boolean = false) {
    HOME("Home"),
    PRODUCTS("Products"),
    CATEGORIES("Categories"),
    SEARCH("Search"),
    WISHLIST("Wishlist"),
    ORDERS("My Order"),
    ORDER_HISTORY("Order History"),
    ADMIN_LOGIN("Admin Login"),
    ADMIN_DASHBOARD("Admin Dashboard", adminOnly = true),
    ADMIN_PRODUCTS("Manage Products", adminOnly = true),
    ADD_PRODUCT("Admin – Add New Product", adminOnly = true),
    PRICE_MANAGEMENT("Price Management", adminOnly = true),
    INVENTORY("Inventory", adminOnly = true),
    REPORTS("Reports", adminOnly = true),
    SETTINGS("Settings")
}

data class AdminSessionState(
    val isInitialized: Boolean? = null,
    val isAdminLoggedIn: Boolean = false,
    val adminUsername: String? = null,
    val isAuthBusy: Boolean = false,
    val authError: String? = null
)

data class BestSellerItem(
    val code: String,
    val name: String,
    val totalUnitsSold: Long,
    val totalRevenueEtb: Double
)

data class CategoryStat(
    val name: String,
    val productCount: Int,
    val totalStock: Long
)

data class DashboardMetrics(
    val totalProducts: Int = 0,
    val totalCategories: Int = 0,
    val totalAvailableStock: Long = 0L,
    val totalInventoryValueEtb: Double = 0.0,
    val todaysSalesEtb: Double = 0.0,
    val monthlySalesEtb: Double = 0.0,
    val totalOrders: Int = 0,
    val confirmedOrdersCount: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val unavailableCount: Int = 0,
    val needsReviewCount: Int = 0,
    val bestSellers: List<BestSellerItem> = emptyList(),
    val categoryStats: List<CategoryStat> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RomelRepository(application)
    private val importService = CatalogImportService(application)

    private val _adminSession = MutableStateFlow(AdminSessionState())
    val adminSession: StateFlow<AdminSessionState> = _adminSession.asStateFlow()

    private val _firestoreCategories = MutableStateFlow<List<CategoryItem>>(emptyList())
    private val _firestoreProducts = MutableStateFlow<List<Product>>(emptyList())

    private val _orders = MutableStateFlow<List<SalesOrder>>(emptyList())
    val orders: StateFlow<List<SalesOrder>> = _orders.asStateFlow()

    private val _userOrderHistory = MutableStateFlow<List<SalesOrder>>(emptyList())
    val userOrderHistory: StateFlow<List<SalesOrder>> = _userOrderHistory.asStateFlow()

    private val _stockMovements = MutableStateFlow<List<StockMovement>>(emptyList())
    val stockMovements: StateFlow<List<StockMovement>> = _stockMovements.asStateFlow()

    private val _wishlistItems = MutableStateFlow<List<WishlistItem>>(emptyList())
    val wishlistItems: StateFlow<List<WishlistItem>> = _wishlistItems.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _updatingPriceProductId = MutableStateFlow<String?>(null)
    val updatingPriceProductId: StateFlow<String?> = _updatingPriceProductId.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Search & filter states
    private val _homeSearchQuery = MutableStateFlow("")
    val homeSearchQuery: StateFlow<String> = _homeSearchQuery.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _categoryGroupFilter = MutableStateFlow<Pair<String, List<String>>?>(null)
    val categoryGroupFilter: StateFlow<Pair<String, List<String>>?> = _categoryGroupFilter.asStateFlow()

    private val _selectedSeries = MutableStateFlow<String?>(null)
    val selectedSeries: StateFlow<String?> = _selectedSeries.asStateFlow()

    private val _availabilityFilter = MutableStateFlow(StockAvailabilityFilter.ALL)
    val availabilityFilter: StateFlow<StockAvailabilityFilter> = _availabilityFilter.asStateFlow()

    private val _minPriceFilter = MutableStateFlow("")
    val minPriceFilter: StateFlow<String> = _minPriceFilter.asStateFlow()

    private val _maxPriceFilter = MutableStateFlow("")
    val maxPriceFilter: StateFlow<String> = _maxPriceFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(ProductSortOption.CATALOG_ORDER)
    val sortOption: StateFlow<ProductSortOption> = _sortOption.asStateFlow()

    // Cart / Order builder states
    private val _cartItems = MutableStateFlow<List<OrderLineItem>>(emptyList())
    val cartItems: StateFlow<List<OrderLineItem>> = _cartItems.asStateFlow()

    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _customerPhone = MutableStateFlow("")
    val customerPhone: StateFlow<String> = _customerPhone.asStateFlow()

    private val _orderType = MutableStateFlow(OrderType.QUOTATION)
    val orderType: StateFlow<OrderType> = _orderType.asStateFlow()

    private val _orderDiscount = MutableStateFlow("")
    val orderDiscount: StateFlow<String> = _orderDiscount.asStateFlow()

    private val _orderNotes = MutableStateFlow("")
    val orderNotes: StateFlow<String> = _orderNotes.asStateFlow()

    private val _allowAdminBackorder = MutableStateFlow(false)
    val allowAdminBackorder: StateFlow<Boolean> = _allowAdminBackorder.asStateFlow()

    private val _calculatorProductId = MutableStateFlow<String?>(null)

    private var autoSeedAttempted = false

    val effectiveRole: StateFlow<UserRole> = _adminSession
        .map { if (it.isAdminLoggedIn) UserRole.ADMIN else UserRole.VIEWER }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserRole.VIEWER)

    val isCloudSynced: StateFlow<Boolean> = combine(_firestoreProducts) { (cloud) ->
        cloud.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val categories: StateFlow<List<CategoryItem>> = combine(_firestoreCategories) { (cloud) ->
        if (cloud.isEmpty()) {
            RomelCatalogSeed.categories
        } else {
            cloud.sortedBy { it.sortOrder }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, RomelCatalogSeed.categories)

    // Complete product list including unavailable/out-of-stock items for Admin management
    val allProducts: StateFlow<List<Product>> = combine(_firestoreProducts) { (cloud) ->
        if (cloud.isEmpty()) {
            RomelCatalogSeed.allProducts
        } else {
            val cloudById = cloud.associateBy { it.id }
            val mergedSeed = RomelCatalogSeed.allProducts.map { cloudById[it.id] ?: it }
            val seedIds = RomelCatalogSeed.allProducts.map { it.id }.toSet()
            val extras = cloud.filter { it.id !in seedIds }
            mergedSeed + extras
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RomelCatalogSeed.allProducts)

    // Customer-visible catalog products (hides products marked Hidden / Unavailable by Admin)
    val products: StateFlow<List<Product>> = allProducts
        .map { list -> list.filter { it.isVisibleToCustomers } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RomelCatalogSeed.allProducts)

    val wishlistProductIds: StateFlow<Set<String>> = combine(_wishlistItems) { (items) ->
        items.map { it.productId }.toSet()
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val wishlistProducts: StateFlow<List<Product>> = combine(_wishlistItems, products) { saved, visibleProducts ->
        val byId = visibleProducts.associateBy { it.id }
        saved
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
            .map { item -> byId[item.productId] ?: item.toFallbackProduct() }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val calculatorProduct: StateFlow<Product?> = combine(allProducts, _calculatorProductId) { list, id ->
        if (id == null) null else list.firstOrNull { it.id == id }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val availableSeries: StateFlow<List<String>> = combine(products) { (list) ->
        list.map { it.series }.filter { it.isNotBlank() }.distinct().sorted()
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private data class FilterCriteria(
        val query: String,
        val category: String?,
        val categoryGroup: Pair<String, List<String>>?,
        val series: String?,
        val availability: StockAvailabilityFilter,
        val minPrice: Double?,
        val maxPrice: Double?,
        val sort: ProductSortOption
    )

    private val filterCriteria = combine<Any?, FilterCriteria>(
        _searchQuery,
        _selectedCategory,
        _categoryGroupFilter,
        _selectedSeries,
        _availabilityFilter,
        _minPriceFilter,
        _maxPriceFilter,
        _sortOption
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        FilterCriteria(
            query = arr[0] as String,
            category = arr[1] as String?,
            categoryGroup = arr[2] as Pair<String, List<String>>?,
            series = arr[3] as String?,
            availability = arr[4] as StockAvailabilityFilter,
            minPrice = (arr[5] as String).toDoubleOrNull(),
            maxPrice = (arr[6] as String).toDoubleOrNull(),
            sort = arr[7] as ProductSortOption
        )
    }

    val filteredProducts: StateFlow<List<Product>> = combine(products, filterCriteria) { list, criteria ->
        val query = criteria.query.trim()
        val matched = list.mapNotNull { prod ->
            if (criteria.category != null && !prod.category.equals(criteria.category, ignoreCase = true)) {
                return@mapNotNull null
            }
            if (criteria.categoryGroup != null &&
                criteria.categoryGroup.second.none { kw -> prod.category.contains(kw, ignoreCase = true) }
            ) {
                return@mapNotNull null
            }
            if (criteria.series != null && !prod.series.equals(criteria.series, ignoreCase = true)) {
                return@mapNotNull null
            }
            val availMatch = when (criteria.availability) {
                StockAvailabilityFilter.ALL -> true
                StockAvailabilityFilter.IN_STOCK ->
                    prod.parsedAvailability == ProductAvailability.AVAILABLE &&
                        (prod.stockQuantity == null || prod.stockQuantity > prod.lowStockThreshold)
                StockAvailabilityFilter.LOW_STOCK -> prod.stockIsLow
                StockAvailabilityFilter.OUT_OF_STOCK -> prod.stockIsOut
                StockAvailabilityFilter.PRICE_SET -> prod.effectiveUnitPrice != null
                StockAvailabilityFilter.NEEDS_REVIEW -> prod.needsReview
            }
            if (!availMatch) return@mapNotNull null

            val price = prod.effectiveUnitPrice
            if (criteria.minPrice != null && (price == null || price < criteria.minPrice)) return@mapNotNull null
            if (criteria.maxPrice != null && (price == null || price > criteria.maxPrice)) return@mapNotNull null

            val score = if (query.isBlank()) 100 else matchScore(prod, query)
            if (score <= 0) null else (prod to score)
        }

        val sorted = when {
            query.isNotBlank() && criteria.sort == ProductSortOption.CATALOG_ORDER ->
                matched.sortedWith(compareByDescending<Pair<Product, Int>> { it.second }.thenBy { it.first.catalogPage })
            criteria.sort == ProductSortOption.NAME_ASC ->
                matched.sortedBy { it.first.name.lowercase() }
            criteria.sort == ProductSortOption.PRICE_LOW_HIGH ->
                matched.sortedBy { it.first.effectiveUnitPrice ?: Double.MAX_VALUE }
            criteria.sort == ProductSortOption.PRICE_HIGH_LOW ->
                matched.sortedByDescending { it.first.effectiveUnitPrice ?: -1.0 }
            criteria.sort == ProductSortOption.STOCK_LOW_HIGH ->
                matched.sortedBy { it.first.stockQuantity ?: Long.MAX_VALUE }
            criteria.sort == ProductSortOption.RECENTLY_ADDED ->
                matched.sortedByDescending { it.first.updatedAt?.seconds ?: it.first.catalogPage }
            else ->
                matched.sortedBy { it.first.catalogPage }
        }
        sorted.map { it.first }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val searchSuggestions: StateFlow<List<Product>> = combine(products, _searchQuery) { list, q ->
        val trimmed = q.trim()
        if (trimmed.isBlank()) emptyList()
        else list.mapNotNull { prod ->
            val s = matchScore(prod, trimmed)
            if (s > 0) prod to s else null
        }.sortedByDescending { it.second }.take(6).map { it.first }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val homeFilteredProducts: StateFlow<List<Product>> = combine(products, _homeSearchQuery) { list, q ->
        val trimmed = q.trim()
        if (trimmed.isBlank()) {
            list.take(12)
        } else {
            filterHomeProducts(list, trimmed)
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dashboardMetrics: StateFlow<DashboardMetrics> =
        combine(allProducts, categories, _orders) { prodList, catList, orderList ->
            val now = Date()
            val calToday = Calendar.getInstance().apply {
                time = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            val calMonth = Calendar.getInstance().apply {
                time = now
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val confirmed = orderList.filter { it.status == OrderStatus.CONFIRMED.name }
            val todaysSales = confirmed.filter { (it.createdAt?.toDate() ?: now) >= calToday }.sumOf { it.totalAmount }
            val monthlySales = confirmed.filter { (it.createdAt?.toDate() ?: now) >= calMonth }.sumOf { it.totalAmount }

            val bestMap = mutableMapOf<String, Triple<String, Long, Double>>()
            for (ord in confirmed) {
                for (item in ord.parsedItems()) {
                    val prev = bestMap[item.code]
                    bestMap[item.code] = Triple(
                        item.name,
                        (prev?.second ?: 0L) + item.totalPieces,
                        (prev?.third ?: 0.0) + item.lineTotal
                    )
                }
            }
            val bestSellers = bestMap.entries
                .map { BestSellerItem(it.key, it.value.first, it.value.second, it.value.third) }
                .sortedByDescending { it.totalUnitsSold }
                .take(8)

            val catStats = catList.map { cat ->
                val inCat = prodList.filter { it.category.equals(cat.name, ignoreCase = true) }
                CategoryStat(
                    name = cat.name,
                    productCount = inCat.size,
                    totalStock = inCat.sumOf { it.stockQuantity ?: 0L }
                )
            }

            DashboardMetrics(
                totalProducts = prodList.size,
                totalCategories = catList.size,
                totalAvailableStock = prodList.sumOf { it.stockQuantity ?: 0L },
                totalInventoryValueEtb = prodList.sumOf { p ->
                    val qty = p.stockQuantity ?: 0L
                    val unitPrice = p.effectiveUnitPrice ?: 0.0
                    qty * unitPrice
                },
                todaysSalesEtb = todaysSales,
                monthlySalesEtb = monthlySales,
                totalOrders = orderList.size,
                confirmedOrdersCount = confirmed.size,
                lowStockCount = prodList.count { it.stockIsLow },
                outOfStockCount = prodList.count { it.stockIsOut },
                unavailableCount = prodList.count { it.parsedAvailability == ProductAvailability.UNAVAILABLE },
                needsReviewCount = prodList.count { it.needsReview },
                bestSellers = bestSellers,
                categoryStats = catStats
            )
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardMetrics())

    init {
        refreshAdminInitStatus()
        observeStreams()
    }

    private fun observeStreams() {
        viewModelScope.launch {
            try {
                repo.observeCategories().collect {
                    _firestoreCategories.value = it
                    _isLoading.value = false
                }
            } catch (e: CancellationException) {
                if (!hasFirestoreError(e)) throw e
            } catch (_: Exception) {
                _isLoading.value = false
            }
        }
        viewModelScope.launch {
            try {
                repo.observeProducts().collect {
                    _firestoreProducts.value = it
                    _isLoading.value = false
                }
            } catch (e: CancellationException) {
                if (!hasFirestoreError(e)) throw e
            } catch (_: Exception) {
                _isLoading.value = false
            }
        }
        viewModelScope.launch {
            try {
                repo.observeOrders().collect { list ->
                    _orders.value = list.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
            } catch (e: CancellationException) {
                if (!hasFirestoreError(e)) throw e
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                repo.observeUserOrderHistory().collect { list ->
                    _userOrderHistory.value = list.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
            } catch (e: CancellationException) {
                if (!hasFirestoreError(e)) throw e
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                repo.observeStockMovements().collect { list ->
                    _stockMovements.value = list.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
            } catch (e: CancellationException) {
                if (!hasFirestoreError(e)) throw e
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                repo.observeWishlist().collect { _wishlistItems.value = it }
            } catch (e: CancellationException) {
                if (!hasFirestoreError(e)) throw e
            } catch (_: Exception) { }
        }
    }

    private fun hasFirestoreError(e: Throwable): Boolean =
        generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull() != null

    fun refreshAdminInitStatus() {
        viewModelScope.launch {
            repo.isAdminInitialized()
                .onSuccess { initialized ->
                    _adminSession.value = _adminSession.value.copy(isInitialized = initialized)
                }
                .onFailure {
                    // Keep null if offline; UI handles cleanly
                }
        }
    }

    fun clearAdminAuthError() {
        _adminSession.value = _adminSession.value.copy(authError = null)
    }

    fun setupAdminAccount(
        username: String,
        password: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        if (password != confirmPassword) {
            _adminSession.value = _adminSession.value.copy(authError = "Passwords do not match.")
            return
        }
        val err = PriceValidator.validateAdminCredentialsInput(username, password)
        if (err != null) {
            _adminSession.value = _adminSession.value.copy(authError = err)
            return
        }
        viewModelScope.launch {
            _adminSession.value = _adminSession.value.copy(isAuthBusy = true, authError = null)
            repo.setupAdminCredentials(username, password)
                .onSuccess { adminUser ->
                    _adminSession.value = AdminSessionState(
                        isInitialized = true,
                        isAdminLoggedIn = true,
                        adminUsername = adminUser,
                        isAuthBusy = false,
                        authError = null
                    )
                    _statusMessage.value = "Administrator account created and signed in."
                    if (!autoSeedAttempted) {
                        autoSeedAttempted = true
                        repo.seedInitialCatalogIfEmpty(forceSync = false)
                    }
                    onSuccess()
                }
                .onFailure { e ->
                    _adminSession.value = _adminSession.value.copy(
                        isAuthBusy = false,
                        authError = e.localizedMessage ?: "Failed to configure admin account."
                    )
                }
        }
    }

    fun loginAdmin(username: String, password: String, onSuccess: () -> Unit) {
        if (username.isBlank() || password.isBlank()) {
            _adminSession.value = _adminSession.value.copy(authError = "Enter both admin username and password.")
            return
        }
        viewModelScope.launch {
            _adminSession.value = _adminSession.value.copy(isAuthBusy = true, authError = null)
            repo.loginAdmin(username, password)
                .onSuccess { adminUser ->
                    _adminSession.value = AdminSessionState(
                        isInitialized = true,
                        isAdminLoggedIn = true,
                        adminUsername = adminUser,
                        isAuthBusy = false,
                        authError = null
                    )
                    _statusMessage.value = "Welcome, $adminUser. Admin Dashboard unlocked."
                    if (!autoSeedAttempted) {
                        autoSeedAttempted = true
                        repo.seedInitialCatalogIfEmpty(forceSync = false)
                    }
                    onSuccess()
                }
                .onFailure { e ->
                    _adminSession.value = _adminSession.value.copy(
                        isAuthBusy = false,
                        authError = e.localizedMessage ?: "Invalid admin username or password."
                    )
                }
        }
    }

    fun logoutAdmin() {
        repo.logoutAdmin()
        _adminSession.value = _adminSession.value.copy(
            isAdminLoggedIn = false,
            adminUsername = null,
            authError = null
        )
        _statusMessage.value = "Signed out of Administrator mode."
    }

    fun openCategoryInProducts(categoryName: String) {
        _selectedCategory.value = categoryName
        _categoryGroupFilter.value = null
        _selectedSeries.value = null
    }

    fun openCategoryGroupInProducts(groupTitle: String, keywords: List<String>) {
        _selectedCategory.value = null
        _categoryGroupFilter.value = groupTitle to keywords
        _selectedSeries.value = null
    }

    fun clearCategoryGroupFilter() {
        _categoryGroupFilter.value = null
    }

    fun openCalculator(product: Product) {
        _calculatorProductId.value = product.id
    }

    fun closeCalculator() {
        _calculatorProductId.value = null
    }

    fun updateHomeSearchQuery(query: String) {
        _homeSearchQuery.value = query
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategoryFilter(category: String?) {
        _selectedCategory.value = category
        if (category != null) _categoryGroupFilter.value = null
    }

    fun selectSeriesFilter(series: String?) {
        _selectedSeries.value = series
    }

    fun selectAvailabilityFilter(filter: StockAvailabilityFilter) {
        _availabilityFilter.value = filter
    }

    fun updatePriceRange(min: String, max: String) {
        _minPriceFilter.value = min
        _maxPriceFilter.value = max
    }

    fun selectSortOption(option: ProductSortOption) {
        _sortOption.value = option
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _categoryGroupFilter.value = null
        _selectedSeries.value = null
        _availabilityFilter.value = StockAvailabilityFilter.ALL
        _minPriceFilter.value = ""
        _maxPriceFilter.value = ""
        _sortOption.value = ProductSortOption.CATALOG_ORDER
    }

    fun clearMessages() {
        _statusMessage.value = null
        _errorMessage.value = null
    }

    fun toggleWishlist(product: Product) {
        val targetId = product.id.ifBlank { sanitizeFirestoreId(product.code) }
        val alreadySaved = _wishlistItems.value.any { it.productId == targetId }
        viewModelScope.launch {
            if (alreadySaved) {
                repo.removeFromWishlist(targetId)
                    .onSuccess {
                        _statusMessage.value = "Removed ${product.code} from My Wishlist."
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.localizedMessage ?: "Failed to update wishlist."
                    }
            } else {
                repo.addToWishlist(product)
                    .onSuccess {
                        _statusMessage.value = "Saved ${product.code} to My Wishlist."
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.localizedMessage ?: "Failed to save to wishlist."
                    }
            }
        }
    }

    // Cart & Order Builder
    fun addToCart(
        product: Product,
        unit: String,
        unitMultiplier: Long,
        unitPrice: Double,
        quantity: Long,
        discount: Double
    ) {
        val current = _cartItems.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.productId == product.id && it.unit == unit }
        val newItem = OrderLineItem(
            productId = product.id,
            code = product.code,
            name = product.name,
            unit = unit,
            unitMultiplier = unitMultiplier.coerceAtLeast(1L),
            unitPrice = unitPrice.coerceAtLeast(0.0),
            quantity = quantity.coerceAtLeast(1L),
            discount = discount.coerceAtLeast(0.0),
            imageAsset = product.imageAsset,
            customImageUri = product.customImageUri
        )
        if (existingIdx >= 0) {
            current[existingIdx] = newItem
        } else {
            current.add(newItem)
        }
        _cartItems.value = current
        _statusMessage.value = "Added ${product.code} ($quantity $unit) to order."
    }

    fun updateCartItem(index: Int, unitPrice: Double? = null, quantity: Long? = null, discount: Double? = null) {
        val list = _cartItems.value.toMutableList()
        if (index !in list.indices) return
        val old = list[index]
        list[index] = old.copy(
            unitPrice = (unitPrice ?: old.unitPrice).coerceAtLeast(0.0),
            quantity = (quantity ?: old.quantity).coerceAtLeast(1L),
            discount = (discount ?: old.discount).coerceAtLeast(0.0)
        )
        _cartItems.value = list
    }

    fun removeCartItem(index: Int) {
        val list = _cartItems.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _cartItems.value = list
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _orderDiscount.value = ""
        _orderNotes.value = ""
    }

    fun setCustomerName(value: String) { _customerName.value = value }
    fun setCustomerPhone(value: String) { _customerPhone.value = value }
    fun setOrderType(type: OrderType) { _orderType.value = type }
    fun setOrderDiscount(value: String) { _orderDiscount.value = value }
    fun setOrderNotes(value: String) { _orderNotes.value = value }
    fun setAllowAdminBackorder(allow: Boolean) { _allowAdminBackorder.value = allow }

    fun buildPreviewOrder(): SalesOrder {
        val items = _cartItems.value
        val subtotal = items.sumOf { it.lineTotal }
        val discount = (_orderDiscount.value.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
        val total = (subtotal - discount).coerceAtLeast(0.0)
        val serialized = items.map { item ->
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
        val actorName = _adminSession.value.adminUsername ?: "Customer"
        return SalesOrder(
            id = "DRAFT",
            orderNumber = if (_orderType.value == OrderType.SALES_ORDER) "SO-DRAFT" else "QT-DRAFT",
            userId = if (effectiveRole.value == UserRole.ADMIN) "admin" else "customer",
            createdByName = actorName,
            createdByRole = effectiveRole.value.name,
            customerName = _customerName.value,
            customerPhone = _customerPhone.value,
            orderType = _orderType.value.name,
            status = OrderStatus.DRAFT.name,
            items = serialized,
            totalProducts = items.size.toLong(),
            totalQuantity = items.sumOf { it.quantity },
            subtotal = subtotal,
            discount = discount,
            totalAmount = total,
            notes = _orderNotes.value.takeIf { it.isNotBlank() }
        )
    }

    fun submitCurrentOrder(confirmSale: Boolean) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            _errorMessage.value = "Add at least one product before saving an order."
            return
        }
        if (confirmSale && effectiveRole.value != UserRole.ADMIN) {
            _errorMessage.value = "Only the Administrator can confirm sales orders and deduct stock."
            return
        }
        val type = if (confirmSale) OrderType.SALES_ORDER else OrderType.QUOTATION
        val status = if (confirmSale) OrderStatus.CONFIRMED else OrderStatus.QUOTATION
        val actorName = _adminSession.value.adminUsername ?: "Customer"
        val mapById = allProducts.value.associateBy { it.id }

        viewModelScope.launch {
            val result = repo.submitOrder(
                customerName = _customerName.value,
                customerPhone = _customerPhone.value,
                orderType = type,
                status = status,
                items = items,
                orderDiscount = _orderDiscount.value.toDoubleOrNull() ?: 0.0,
                notes = _orderNotes.value.takeIf { it.isNotBlank() },
                createdByName = actorName,
                createdByRole = effectiveRole.value,
                productsById = mapById,
                allowAdminBackorder = effectiveRole.value == UserRole.ADMIN && _allowAdminBackorder.value
            )
            result.onSuccess { saved ->
                _statusMessage.value = if (confirmSale) {
                    "Confirmed Sales Order ${saved.orderNumber} and updated stock."
                } else {
                    "Saved Quotation ${saved.orderNumber} to Order History."
                }
                clearCart()
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Failed to save order."
            }
        }
    }

    // Admin actions
    fun syncCatalogToCloud() {
        if (effectiveRole.value != UserRole.ADMIN) {
            _errorMessage.value = "Administrator login required."
            return
        }
        viewModelScope.launch {
            _isImporting.value = true
            repo.seedInitialCatalogIfEmpty(forceSync = true).onSuccess { count ->
                _statusMessage.value = if (count > 0) {
                    "Synced $count ROMEL catalog items to Firestore."
                } else {
                    "All ROMEL catalog items are already synced."
                }
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Cloud sync failed."
            }
            _isImporting.value = false
        }
    }

    fun saveProduct(
        product: Product,
        isNew: Boolean,
        pickedImageUri: Uri? = null,
        onSuccessCallback: (() -> Unit)? = null
    ) {
        if (effectiveRole.value != UserRole.ADMIN) {
            _errorMessage.value = "Administrator login required."
            return
        }
        viewModelScope.launch {
            var finalProduct = product.copy(id = sanitizeFirestoreId(product.code))
            if (pickedImageUri != null) {
                val savedPath = persistPickedImage(pickedImageUri, finalProduct.id)
                if (savedPath != null) {
                    finalProduct = finalProduct.copy(customImageUri = savedPath)
                }
            }
            val existsInCloud = _firestoreProducts.value.any { it.id == finalProduct.id }
            val treatAsNew = if (isNew) true else !existsInCloud
            repo.upsertProduct(finalProduct, isNew = treatAsNew).onSuccess {
                _statusMessage.value = if (isNew) "Added ${finalProduct.code}." else "Updated ${finalProduct.code}."
                onSuccessCallback?.invoke()
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Failed to save product."
            }
        }
    }

    fun deleteProduct(product: Product) {
        if (effectiveRole.value != UserRole.ADMIN) {
            _errorMessage.value = "Administrator login required."
            return
        }
        viewModelScope.launch {
            val id = product.id.ifBlank { sanitizeFirestoreId(product.code) }
            if (_firestoreProducts.value.none { it.id == id }) {
                _errorMessage.value = "${product.code} is not stored in Firestore yet. Mark it Hidden / Unavailable instead."
                return@launch
            }
            repo.deleteProduct(id)
                .onSuccess { _statusMessage.value = "Deleted ${product.code} from Firestore." }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Failed to delete product." }
        }
    }

    fun setProductAvailability(product: Product, availability: ProductAvailability) {
        if (effectiveRole.value != UserRole.ADMIN) {
            _errorMessage.value = "Administrator login required."
            return
        }
        viewModelScope.launch {
            repo.setProductAvailability(product, availability)
                .onSuccess {
                    _statusMessage.value = "${product.code} marked as ${availability.label}."
                }
                .onFailure {
                    _errorMessage.value = it.localizedMessage ?: "Failed to update product availability."
                }
        }
    }

    private suspend fun persistPickedImage(uri: Uri, productId: String): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(getApplication<Application>().filesDir, "product_images").apply { mkdirs() }
            val dest = File(dir, "${productId}_${System.currentTimeMillis()}.jpg")
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            Uri.fromFile(dest).toString()
        } catch (_: Exception) {
            null
        }
    }

    fun updateProductPrices(
        product: Product,
        selling: Double?,
        wholesale: Double?,
        retail: Double?,
        discount: Double?
    ) {
        if (effectiveRole.value != UserRole.ADMIN) {
            _errorMessage.value = "Permission denied: Only ADMIN can update product prices."
            return
        }
        val validationError = PriceValidator.validateValues(
            selling = selling,
            wholesale = wholesale,
            retail = retail,
            discount = discount
        )
        if (validationError != null) {
            _errorMessage.value = validationError
            return
        }
        val actor = _adminSession.value.adminUsername ?: "Admin"
        viewModelScope.launch {
            _updatingPriceProductId.value = product.id
            try {
                repo.updateProductPrices(product, selling, wholesale, retail, discount, actor)
                    .onSuccess {
                        _statusMessage.value = if (selling != null) {
                            "Updated ${product.code} price in Firestore (${formatEtb(selling)})."
                        } else {
                            "Updated ${product.code} pricing in Firestore."
                        }
                    }
                    .onFailure { _errorMessage.value = it.localizedMessage ?: "Failed to update prices." }
            } finally {
                _updatingPriceProductId.value = null
            }
        }
    }

    fun adjustProductStock(product: Product, delta: Long, reason: String) {
        if (effectiveRole.value != UserRole.ADMIN) {
            _errorMessage.value = "Administrator login required."
            return
        }
        val actor = _adminSession.value.adminUsername ?: "Admin"
        viewModelScope.launch {
            repo.adjustStock(product, delta, reason, actor)
                .onSuccess { _statusMessage.value = "Updated stock for ${product.code}." }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Failed to adjust stock." }
        }
    }

    fun createCategory(name: String, description: String) {
        if (effectiveRole.value != UserRole.ADMIN) {
            _errorMessage.value = "Administrator login required."
            return
        }
        val nextSort = (categories.value.maxOfOrNull { it.sortOrder } ?: 21L) + 1L
        viewModelScope.launch {
            if (_firestoreCategories.value.isEmpty()) {
                repo.seedInitialCatalogIfEmpty(forceSync = false)
            }
            repo.createCategory(name, description, nextSort)
                .onSuccess { _statusMessage.value = "Category '$name' created." }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Failed to create category." }
        }
    }

    fun updateCategory(category: CategoryItem, newName: String, newDescription: String) {
        if (effectiveRole.value != UserRole.ADMIN) {
            _errorMessage.value = "Administrator login required."
            return
        }
        viewModelScope.launch {
            if (_firestoreCategories.value.none { it.id == category.id }) {
                repo.seedInitialCatalogIfEmpty(forceSync = false)
            }
            repo.updateCategory(category, newName, newDescription)
                .onSuccess { _statusMessage.value = "Updated category '$newName'." }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Failed to update category." }
        }
    }

    fun deleteCategory(category: CategoryItem) {
        if (effectiveRole.value != UserRole.ADMIN) {
            _errorMessage.value = "Administrator login required."
            return
        }
        viewModelScope.launch {
            if (_firestoreCategories.value.none { it.id == category.id }) {
                repo.seedInitialCatalogIfEmpty(forceSync = false)
            }
            repo.deleteCategory(category.id)
                .onSuccess { _statusMessage.value = "Deleted category '${category.name}'." }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Failed to delete category." }
        }
    }

    fun importCsv(uri: Uri) {
        if (effectiveRole.value != UserRole.ADMIN) {
            _errorMessage.value = "Administrator login required."
            return
        }
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val parsed = importService.parseCsvUri(uri)
                if (parsed.isEmpty()) {
                    _errorMessage.value = "No products found in the selected CSV file."
                } else {
                    val existingCodes = allProducts.value.map { it.code }.toSet()
                    val actor = _adminSession.value.adminUsername ?: "Admin"
                    repo.importProductsBatch(parsed, existingCodes, actor)
                        .onSuccess { (added, skipped) ->
                            _statusMessage.value = "CSV import complete: $added added, $skipped duplicates skipped."
                        }
                        .onFailure { _errorMessage.value = it.localizedMessage ?: "CSV import failed." }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "CSV import error."
            }
            _isImporting.value = false
        }
    }

    fun importPdfCatalog(uri: Uri) {
        if (effectiveRole.value != UserRole.ADMIN) {
            _errorMessage.value = "Administrator login required."
            return
        }
        viewModelScope.launch {
            _isImporting.value = true
            importService.extractProductsFromPdfUri(uri).onSuccess { extracted ->
                if (extracted.isEmpty()) {
                    _errorMessage.value = "No products detected on the scanned PDF pages."
                } else {
                    val existingCodes = allProducts.value.map { it.code }.toSet()
                    val actor = _adminSession.value.adminUsername ?: "Admin"
                    repo.importProductsBatch(extracted, existingCodes, actor)
                        .onSuccess { (added, skipped) ->
                            val flagged = extracted.count { it.needsReview }
                            _statusMessage.value =
                                "AI PDF Import: $added added ($flagged flagged for review), $skipped duplicates skipped."
                        }
                        .onFailure { _errorMessage.value = it.localizedMessage ?: "Failed to save PDF imports." }
                }
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "PDF AI import failed."
            }
            _isImporting.value = false
        }
    }

    companion object {
        fun filterHomeProducts(products: List<Product>, rawQuery: String): List<Product> {
            val q = rawQuery.trim().lowercase()
            if (q.isEmpty()) return products
            return products.mapNotNull { prod ->
                val name = prod.name.lowercase()
                val category = prod.category.lowercase()
                val code = prod.code.lowercase()
                val score = when {
                    name.startsWith(q) -> 500
                    category.startsWith(q) -> 450
                    name.contains(q) -> 400
                    category.contains(q) -> 350
                    code.startsWith(q) -> 300
                    code.contains(q) -> 250
                    else -> {
                        val tokens = q.split(Regex("\\s+")).filter { it.length >= 2 }
                        if (tokens.isNotEmpty() && tokens.all { "$name $category $code".contains(it) }) 200 else 0
                    }
                }
                if (score > 0) prod to score else null
            }.sortedWith(compareByDescending<Pair<Product, Int>> { it.second }.thenBy { it.first.catalogPage })
                .map { it.first }
        }

        fun matchScore(product: Product, rawQuery: String): Int {
            val q = rawQuery.trim().lowercase()
            if (q.isEmpty()) return 100
            val code = product.code.lowercase()
            val name = product.name.lowercase()
            val cat = product.category.lowercase()
            val ser = product.series.lowercase()
            val desc = product.description.lowercase()
            val specs = product.specifications.lowercase()

            if (code == q) return 1000
            if (code.startsWith(q)) return 850
            if (code.contains(q)) return 700
            if (name.startsWith(q)) return 620
            if (name.contains(q)) return 520
            if (cat.contains(q) || ser.contains(q)) return 400
            if (specs.contains(q) || desc.contains(q)) return 300

            val tokens = q.split(Regex("\\s+")).filter { it.length >= 2 }
            if (tokens.isNotEmpty()) {
                val combined = "$code $name $cat $ser $specs $desc"
                if (tokens.all { combined.contains(it) }) return 260
            }

            if (q.length >= 3 && editDistance(code, q) <= 2) return 220
            val nameWords = name.split(Regex("[\\s\\-/]+")).filter { it.length >= 4 }
            if (q.length >= 4 && nameWords.any { editDistance(it, q) <= 1 }) return 180

            return 0
        }

        private fun editDistance(a: String, b: String): Int {
            if (kotlin.math.abs(a.length - b.length) > 3) return 99
            val dp = IntArray(b.length + 1) { it }
            for (i in 1..a.length) {
                var prev = dp[0]
                dp[0] = i
                for (j in 1..b.length) {
                    val temp = dp[j]
                    dp[j] = if (a[i - 1] == b[j - 1]) prev
                    else 1 + minOf(prev, dp[j], dp[j - 1])
                    prev = temp
                }
            }
            return dp[b.length]
        }
    }
}
