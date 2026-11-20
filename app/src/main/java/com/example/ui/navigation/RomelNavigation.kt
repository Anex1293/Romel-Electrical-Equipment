package com.example.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.model.Product
import com.example.model.UserRole
import com.example.ui.AddEditProductScreen
import com.example.ui.AdminAllProductsScreen
import com.example.ui.AdminDashboardScreen
import com.example.ui.AdminLoginScreen
import com.example.ui.AppSection
import com.example.ui.CategoriesScreen
import com.example.ui.HomeScreen
import com.example.ui.InventoryScreen
import com.example.ui.MainViewModel
import com.example.ui.MyOrdersScreen
import com.example.ui.OrderHistoryScreen
import com.example.ui.PriceManagementScreen
import com.example.ui.ProductDetailScreen
import com.example.ui.ProductsAndSearchScreen
import com.example.ui.ReportsScreen
import com.example.ui.SettingsScreen
import com.example.ui.WishlistScreen
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.RomelNavy
import com.example.ui.theme.RomelRed
import com.example.ui.theme.SlateMuted

object RomelRoute {
    const val HOME = "home"
    const val PRODUCTS = "products"
    const val WISHLIST = "wishlist"
    const val ORDERS = "orders"
    const val ORDER_HISTORY = "order_history"
    const val SETTINGS = "settings"
    const val CATEGORIES = "categories"
    const val SEARCH = "search"
    const val PRODUCT_DETAIL = "product_detail/{productId}"
    const val ADMIN_LOGIN = "admin_login"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_PRODUCTS = "admin_products"
    const val ADD_PRODUCT = "add_product?productId={productId}"
    const val PRICE_MANAGEMENT = "price_management"
    const val INVENTORY = "inventory"
    const val REPORTS = "reports"

    fun productDetail(productId: String): String = "product_detail/${Uri.encode(productId)}"

    fun addOrEditProduct(productId: String? = null): String =
        if (productId.isNullOrBlank()) "add_product"
        else "add_product?productId=${Uri.encode(productId)}"
}

data class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val allowedRoles: Set<UserRole>,
    val matchingChildRoutes: Set<String> = emptySet()
)

val ALL_ROLES: Set<UserRole> = setOf(UserRole.ADMIN, UserRole.SALES_REP, UserRole.VIEWER)
val ADMIN_ONLY_ROLES: Set<UserRole> = setOf(UserRole.ADMIN)

val romelBottomNavItems: List<BottomNavDestination> = listOf(
    BottomNavDestination(
        route = RomelRoute.HOME,
        label = "Home",
        icon = Icons.Filled.Home,
        allowedRoles = ALL_ROLES
    ),
    BottomNavDestination(
        route = RomelRoute.PRODUCTS,
        label = "Products",
        icon = Icons.Filled.ElectricalServices,
        allowedRoles = ALL_ROLES,
        matchingChildRoutes = setOf(RomelRoute.SEARCH, RomelRoute.CATEGORIES, RomelRoute.PRODUCT_DETAIL)
    ),
    BottomNavDestination(
        route = RomelRoute.WISHLIST,
        label = "Wishlist",
        icon = Icons.Filled.Favorite,
        allowedRoles = ALL_ROLES
    ),
    BottomNavDestination(
        route = RomelRoute.ORDERS,
        label = "Orders",
        icon = Icons.Filled.ShoppingCart,
        allowedRoles = ALL_ROLES,
        matchingChildRoutes = setOf(RomelRoute.ORDER_HISTORY)
    ),
    BottomNavDestination(
        route = RomelRoute.SETTINGS,
        label = "Settings",
        icon = Icons.Filled.Settings,
        allowedRoles = ALL_ROLES,
        matchingChildRoutes = setOf(
            RomelRoute.ADMIN_LOGIN,
            RomelRoute.ADMIN_DASHBOARD,
            RomelRoute.ADMIN_PRODUCTS,
            RomelRoute.ADD_PRODUCT,
            RomelRoute.PRICE_MANAGEMENT,
            RomelRoute.INVENTORY,
            RomelRoute.REPORTS
        )
    )
)

fun bottomNavItemsForRole(role: UserRole): List<BottomNavDestination> =
    romelBottomNavItems.filter { role in it.allowedRoles }

fun canAccessRoute(routeOrPattern: String?, role: UserRole): Boolean {
    if (routeOrPattern == null) return true
    val base = routeOrPattern.substringBefore('?').substringBefore('/')
    return when (base) {
        "admin_dashboard",
        "admin_products",
        "add_product",
        "price_management",
        "inventory",
        "reports" -> role == UserRole.ADMIN
        else -> true
    }
}

fun appSectionToRoute(section: AppSection): String = when (section) {
    AppSection.HOME -> RomelRoute.HOME
    AppSection.PRODUCTS -> RomelRoute.PRODUCTS
    AppSection.CATEGORIES -> RomelRoute.CATEGORIES
    AppSection.SEARCH -> RomelRoute.SEARCH
    AppSection.WISHLIST -> RomelRoute.WISHLIST
    AppSection.ORDERS -> RomelRoute.ORDERS
    AppSection.ORDER_HISTORY -> RomelRoute.ORDER_HISTORY
    AppSection.ADMIN_LOGIN -> RomelRoute.ADMIN_LOGIN
    AppSection.ADMIN_DASHBOARD -> RomelRoute.ADMIN_DASHBOARD
    AppSection.ADMIN_PRODUCTS -> RomelRoute.ADMIN_PRODUCTS
    AppSection.ADD_PRODUCT -> RomelRoute.addOrEditProduct(null)
    AppSection.PRICE_MANAGEMENT -> RomelRoute.PRICE_MANAGEMENT
    AppSection.INVENTORY -> RomelRoute.INVENTORY
    AppSection.REPORTS -> RomelRoute.REPORTS
    AppSection.SETTINGS -> RomelRoute.SETTINGS
}

fun navigateBottomTab(
    navController: NavHostController,
    route: String,
    role: UserRole,
    onAccessDenied: (String) -> Unit
) {
    if (!canAccessRoute(route, role)) {
        onAccessDenied("Administrator permissions are required for this section.")
        return
    }
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

fun navigateRoleAware(
    navController: NavHostController,
    route: String,
    role: UserRole,
    onAccessDenied: (String) -> Unit
) {
    if (!canAccessRoute(route, role)) {
        onAccessDenied("Access denied for role ${role.label}: Administrator permissions required.")
        return
    }
    navController.navigate(route) {
        launchSingleTop = true
    }
}

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    role: UserRole,
    ordersBadgeCount: Int,
    wishlistBadgeCount: Int = 0,
    onSelectRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember(role) { bottomNavItemsForRole(role) }

    Surface(shadowElevation = 8.dp, color = CleanWhite, modifier = modifier) {
        NavigationBar(containerColor = CleanWhite) {
            items.forEach { item ->
                val selected = currentRoute == item.route || currentRoute in item.matchingChildRoutes
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelectRoute(item.route) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RomelRed,
                        selectedTextColor = RomelRed,
                        indicatorColor = RomelRed.copy(alpha = 0.12f),
                        unselectedIconColor = SlateMuted,
                        unselectedTextColor = SlateMuted
                    ),
                    icon = {
                        val badgeCount = when (item.route) {
                            RomelRoute.ORDERS -> ordersBadgeCount
                            RomelRoute.WISHLIST -> wishlistBadgeCount
                            else -> 0
                        }
                        if (badgeCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = RomelRed, contentColor = CleanWhite) {
                                        Text("$badgeCount")
                                    }
                                }
                            ) {
                                Icon(item.icon, contentDescription = item.label)
                            }
                        } else {
                            Icon(item.icon, contentDescription = item.label)
                        }
                    },
                    label = {
                        Text(
                            text = item.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("nav_${item.route}")
                )
            }
        }
    }
}

@Composable
fun RomelNavHost(
    navController: NavHostController,
    vm: MainViewModel,
    effectiveRole: UserRole,
    onOpenPriceEditor: (Product) -> Unit,
    onOpenStockAdjust: (Product) -> Unit,
    onAccessDenied: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val adminSession by vm.adminSession.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val allProducts by vm.allProducts.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()
    val filteredProducts by vm.filteredProducts.collectAsStateWithLifecycle()
    val suggestions by vm.searchSuggestions.collectAsStateWithLifecycle()
    val seriesList by vm.availableSeries.collectAsStateWithLifecycle()
    val isCloudSynced by vm.isCloudSynced.collectAsStateWithLifecycle()
    val isImporting by vm.isImporting.collectAsStateWithLifecycle()

    val homeSearchQuery by vm.homeSearchQuery.collectAsStateWithLifecycle()
    val homeFilteredProducts by vm.homeFilteredProducts.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by vm.selectedCategory.collectAsStateWithLifecycle()
    val categoryGroupFilter by vm.categoryGroupFilter.collectAsStateWithLifecycle()
    val selectedSeries by vm.selectedSeries.collectAsStateWithLifecycle()
    val availabilityFilter by vm.availabilityFilter.collectAsStateWithLifecycle()
    val minPrice by vm.minPriceFilter.collectAsStateWithLifecycle()
    val maxPrice by vm.maxPriceFilter.collectAsStateWithLifecycle()
    val sortOption by vm.sortOption.collectAsStateWithLifecycle()

    val cartItems by vm.cartItems.collectAsStateWithLifecycle()
    val customerName by vm.customerName.collectAsStateWithLifecycle()
    val customerPhone by vm.customerPhone.collectAsStateWithLifecycle()
    val orderType by vm.orderType.collectAsStateWithLifecycle()
    val orderDiscount by vm.orderDiscount.collectAsStateWithLifecycle()
    val orderNotes by vm.orderNotes.collectAsStateWithLifecycle()
    val allowAdminBackorder by vm.allowAdminBackorder.collectAsStateWithLifecycle()
    val orders by vm.orders.collectAsStateWithLifecycle()
    val userOrderHistory by vm.userOrderHistory.collectAsStateWithLifecycle()
    val movements by vm.stockMovements.collectAsStateWithLifecycle()
    val metrics by vm.dashboardMetrics.collectAsStateWithLifecycle()
    val wishlistProductIds by vm.wishlistProductIds.collectAsStateWithLifecycle()
    val wishlistProducts by vm.wishlistProducts.collectAsStateWithLifecycle()
    val updatingPriceProductId by vm.updatingPriceProductId.collectAsStateWithLifecycle()

    // Automatically guard restricted routes whenever admin login state changes
    LaunchedEffect(effectiveRole) {
        val current = navController.currentBackStackEntry?.destination?.route
        if (!canAccessRoute(current, effectiveRole)) {
            onAccessDenied("Returned to Home: Administrator sign-in required.")
            navController.popBackStack(RomelRoute.HOME, inclusive = false)
        }
    }

    NavHost(
        navController = navController,
        startDestination = RomelRoute.HOME,
        modifier = modifier
    ) {
        composable(RomelRoute.HOME) {
            HomeScreen(
                role = effectiveRole,
                userName = adminSession.adminUsername ?: "ROMEL Catalog",
                metrics = metrics,
                homeSearchQuery = homeSearchQuery,
                featuredProducts = homeFilteredProducts,
                wishlistProductIds = wishlistProductIds,
                isCloudSynced = isCloudSynced,
                onHomeSearchQueryChange = { vm.updateHomeSearchQuery(it) },
                onSyncCloud = { vm.syncCatalogToCloud() },
                onQuickCodeSearch = { code ->
                    vm.updateHomeSearchQuery(code)
                },
                onNavigateSection = { section ->
                    val target = appSectionToRoute(section)
                    if (target in setOf(RomelRoute.HOME, RomelRoute.PRODUCTS, RomelRoute.WISHLIST, RomelRoute.ORDERS, RomelRoute.SETTINGS)) {
                        navigateBottomTab(navController, target, effectiveRole, onAccessDenied)
                    } else {
                        navigateRoleAware(navController, target, effectiveRole, onAccessDenied)
                    }
                },
                onOpenCategoryGroup = { title, keywords ->
                    vm.openCategoryGroupInProducts(title, keywords)
                    navigateBottomTab(navController, RomelRoute.PRODUCTS, effectiveRole, onAccessDenied)
                },
                onProductClick = { prod ->
                    navigateRoleAware(navController, RomelRoute.productDetail(prod.id), effectiveRole, onAccessDenied)
                },
                onToggleWishlist = { vm.toggleWishlist(it) },
                onOpenCalculator = { vm.openCalculator(it) },
                onEditPrice = onOpenPriceEditor,
                onAdjustStock = onOpenStockAdjust
            )
        }

        composable(RomelRoute.PRODUCTS) {
            ProductsAndSearchScreen(
                isSearchFocused = false,
                role = effectiveRole,
                query = searchQuery,
                suggestions = suggestions,
                products = filteredProducts,
                categories = categories,
                seriesList = seriesList,
                selectedCategory = selectedCategory,
                categoryGroupTitle = categoryGroupFilter?.first,
                selectedSeries = selectedSeries,
                availabilityFilter = availabilityFilter,
                minPrice = minPrice,
                maxPrice = maxPrice,
                sortOption = sortOption,
                onQueryChange = { vm.updateSearchQuery(it) },
                onCategoryChange = { vm.selectCategoryFilter(it) },
                onClearCategoryGroup = { vm.clearCategoryGroupFilter() },
                onSeriesChange = { vm.selectSeriesFilter(it) },
                onAvailabilityChange = { vm.selectAvailabilityFilter(it) },
                onPriceRangeChange = { min, max -> vm.updatePriceRange(min, max) },
                onSortChange = { vm.selectSortOption(it) },
                onClearFilters = { vm.clearAllFilters() },
                onProductClick = { prod ->
                    navigateRoleAware(navController, RomelRoute.productDetail(prod.id), effectiveRole, onAccessDenied)
                },
                wishlistProductIds = wishlistProductIds,
                onToggleWishlist = { vm.toggleWishlist(it) },
                onOpenCalculator = { vm.openCalculator(it) },
                onEditPrice = onOpenPriceEditor,
                onAdjustStock = onOpenStockAdjust
            )
        }

        composable(RomelRoute.WISHLIST) {
            WishlistScreen(
                role = effectiveRole,
                wishlistProducts = wishlistProducts,
                onProductClick = { prod ->
                    navigateRoleAware(navController, RomelRoute.productDetail(prod.id), effectiveRole, onAccessDenied)
                },
                onToggleWishlist = { vm.toggleWishlist(it) },
                onOpenCalculator = { vm.openCalculator(it) },
                onEditPrice = onOpenPriceEditor,
                onAdjustStock = onOpenStockAdjust,
                onBrowseCatalog = {
                    navigateBottomTab(navController, RomelRoute.PRODUCTS, effectiveRole, onAccessDenied)
                }
            )
        }

        composable(RomelRoute.SEARCH) {
            ProductsAndSearchScreen(
                isSearchFocused = true,
                role = effectiveRole,
                query = searchQuery,
                suggestions = suggestions,
                products = filteredProducts,
                categories = categories,
                seriesList = seriesList,
                selectedCategory = selectedCategory,
                categoryGroupTitle = categoryGroupFilter?.first,
                selectedSeries = selectedSeries,
                availabilityFilter = availabilityFilter,
                minPrice = minPrice,
                maxPrice = maxPrice,
                sortOption = sortOption,
                onQueryChange = { vm.updateSearchQuery(it) },
                onCategoryChange = { vm.selectCategoryFilter(it) },
                onClearCategoryGroup = { vm.clearCategoryGroupFilter() },
                onSeriesChange = { vm.selectSeriesFilter(it) },
                onAvailabilityChange = { vm.selectAvailabilityFilter(it) },
                onPriceRangeChange = { min, max -> vm.updatePriceRange(min, max) },
                onSortChange = { vm.selectSortOption(it) },
                onClearFilters = { vm.clearAllFilters() },
                onProductClick = { prod ->
                    navigateRoleAware(navController, RomelRoute.productDetail(prod.id), effectiveRole, onAccessDenied)
                },
                wishlistProductIds = wishlistProductIds,
                onToggleWishlist = { vm.toggleWishlist(it) },
                onOpenCalculator = { vm.openCalculator(it) },
                onEditPrice = onOpenPriceEditor,
                onAdjustStock = onOpenStockAdjust
            )
        }

        composable(RomelRoute.CATEGORIES) {
            val catProducts = if (effectiveRole == UserRole.ADMIN) allProducts else products
            CategoriesScreen(
                role = effectiveRole,
                categories = categories,
                products = catProducts,
                onSelectCategory = { catName ->
                    vm.openCategoryInProducts(catName)
                    navigateBottomTab(navController, RomelRoute.PRODUCTS, effectiveRole, onAccessDenied)
                },
                onCreateCategory = { n, d -> vm.createCategory(n, d) },
                onUpdateCategory = { cat, n, d -> vm.updateCategory(cat, n, d) },
                onDeleteCategory = { cat -> vm.deleteCategory(cat) }
            )
        }

        composable(
            route = RomelRoute.PRODUCT_DETAIL,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getString("productId")?.let(Uri::decode)
            val sourceList = if (effectiveRole == UserRole.ADMIN) allProducts else products
            val detailProd = remember(rawId, sourceList) {
                sourceList.firstOrNull { it.id == rawId }
            }
            if (detailProd == null) {
                LaunchedEffect(rawId) {
                    navController.popBackStack()
                }
            } else {
                val similar = remember(detailProd.id, products) {
                    products.filter { it.category == detailProd.category && it.id != detailProd.id }.take(8)
                }
                ProductDetailScreen(
                    product = detailProd,
                    similarProducts = similar,
                    role = effectiveRole,
                    isWishlisted = detailProd.id in wishlistProductIds,
                    onToggleWishlist = { vm.toggleWishlist(detailProd) },
                    onBack = { navController.popBackStack() },
                    onAddDirectToCart = { unit, mult, price, qty ->
                        vm.addToCart(detailProd, unit, mult, price, qty, 0.0)
                    },
                    onOpenCalculator = { vm.openCalculator(detailProd) },
                    onEditPrices = { onOpenPriceEditor(detailProd) },
                    onAdjustStock = { onOpenStockAdjust(detailProd) },
                    onEditProduct = {
                        navigateRoleAware(
                            navController,
                            RomelRoute.addOrEditProduct(detailProd.id),
                            effectiveRole,
                            onAccessDenied
                        )
                    },
                    onSelectSimilar = { sim ->
                        navigateRoleAware(
                            navController,
                            RomelRoute.productDetail(sim.id),
                            effectiveRole,
                            onAccessDenied
                        )
                    }
                )
            }
        }

        composable(RomelRoute.ORDERS) {
            MyOrdersScreen(
                role = effectiveRole,
                cartItems = cartItems,
                customerName = customerName,
                customerPhone = customerPhone,
                orderType = orderType,
                orderDiscount = orderDiscount,
                orderNotes = orderNotes,
                allowAdminBackorder = allowAdminBackorder,
                orders = orders,
                onCustomerNameChange = { vm.setCustomerName(it) },
                onCustomerPhoneChange = { vm.setCustomerPhone(it) },
                onOrderTypeChange = { vm.setOrderType(it) },
                onOrderDiscountChange = { vm.setOrderDiscount(it) },
                onOrderNotesChange = { vm.setOrderNotes(it) },
                onBackorderToggle = { vm.setAllowAdminBackorder(it) },
                onUpdateItem = { idx, p, q -> vm.updateCartItem(idx, p, q) },
                onRemoveItem = { vm.removeCartItem(it) },
                onClearCart = { vm.clearCart() },
                onSubmitOrder = { confirm -> vm.submitCurrentOrder(confirm) },
                onBuildPreviewOrder = { vm.buildPreviewOrder() },
                onBrowseCatalog = {
                    navigateBottomTab(navController, RomelRoute.PRODUCTS, effectiveRole, onAccessDenied)
                },
                onOpenOrderHistory = {
                    navigateRoleAware(navController, RomelRoute.ORDER_HISTORY, effectiveRole, onAccessDenied)
                }
            )
        }

        composable(RomelRoute.ORDER_HISTORY) {
            OrderHistoryScreen(
                orders = userOrderHistory,
                onBrowseCatalog = {
                    navigateBottomTab(navController, RomelRoute.PRODUCTS, effectiveRole, onAccessDenied)
                }
            )
        }

        composable(RomelRoute.SETTINGS) {
            SettingsScreen(
                adminSession = adminSession,
                isImporting = isImporting,
                onOpenAdminLogin = {
                    if (adminSession.isAdminLoggedIn) {
                        navigateRoleAware(navController, RomelRoute.ADMIN_DASHBOARD, effectiveRole, onAccessDenied)
                    } else {
                        vm.refreshAdminInitStatus()
                        navController.navigate(RomelRoute.ADMIN_LOGIN) { launchSingleTop = true }
                    }
                },
                onLogoutAdmin = { vm.logoutAdmin() },
                onSyncCatalog = { vm.syncCatalogToCloud() },
                onImportCsv = { uri -> vm.importCsv(uri) },
                onImportPdf = { uri -> vm.importPdfCatalog(uri) },
                onOpenAdminDashboard = {
                    navigateRoleAware(navController, RomelRoute.ADMIN_DASHBOARD, effectiveRole, onAccessDenied)
                },
                onOpenAllProducts = {
                    navigateRoleAware(navController, RomelRoute.ADMIN_PRODUCTS, effectiveRole, onAccessDenied)
                },
                onOpenAddProduct = {
                    navigateRoleAware(navController, RomelRoute.addOrEditProduct(null), effectiveRole, onAccessDenied)
                },
                onOpenPriceManagement = {
                    navigateRoleAware(navController, RomelRoute.PRICE_MANAGEMENT, effectiveRole, onAccessDenied)
                },
                onOpenInventory = {
                    navigateRoleAware(navController, RomelRoute.INVENTORY, effectiveRole, onAccessDenied)
                },
                onOpenReports = {
                    navigateRoleAware(navController, RomelRoute.REPORTS, effectiveRole, onAccessDenied)
                },
                onOpenCategories = {
                    navigateRoleAware(navController, RomelRoute.CATEGORIES, effectiveRole, onAccessDenied)
                },
                onOpenSearch = {
                    navigateRoleAware(navController, RomelRoute.SEARCH, effectiveRole, onAccessDenied)
                },
                onOpenOrderHistory = {
                    navigateRoleAware(navController, RomelRoute.ORDER_HISTORY, effectiveRole, onAccessDenied)
                }
            )
        }

        composable(RomelRoute.ADMIN_LOGIN) {
            LaunchedEffect(adminSession.isAdminLoggedIn) {
                if (adminSession.isAdminLoggedIn) {
                    navController.navigate(RomelRoute.ADMIN_DASHBOARD) {
                        popUpTo(RomelRoute.ADMIN_LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            AdminLoginScreen(
                adminSession = adminSession,
                onLogin = { user, pass ->
                    vm.loginAdmin(user, pass) {
                        navController.navigate(RomelRoute.ADMIN_DASHBOARD) {
                            popUpTo(RomelRoute.ADMIN_LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onSetupAdmin = { user, pass, confirm ->
                    vm.setupAdminAccount(user, pass, confirm) {
                        navController.navigate(RomelRoute.ADMIN_DASHBOARD) {
                            popUpTo(RomelRoute.ADMIN_LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onRefreshStatus = { vm.refreshAdminInitStatus() },
                onClearError = { vm.clearAdminAuthError() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(RomelRoute.ADMIN_DASHBOARD) {
            AdminRouteGuard(role = effectiveRole, onBackHome = {
                navController.popBackStack(RomelRoute.HOME, inclusive = false)
            }) {
                AdminDashboardScreen(
                    adminUsername = adminSession.adminUsername ?: "Administrator",
                    metrics = metrics,
                    orders = orders,
                    onAddProduct = {
                        navigateRoleAware(navController, RomelRoute.addOrEditProduct(null), effectiveRole, onAccessDenied)
                    },
                    onOpenAllProducts = {
                        navigateRoleAware(navController, RomelRoute.ADMIN_PRODUCTS, effectiveRole, onAccessDenied)
                    },
                    onOpenPriceManagement = {
                        navigateRoleAware(navController, RomelRoute.PRICE_MANAGEMENT, effectiveRole, onAccessDenied)
                    },
                    onOpenCategories = {
                        navigateRoleAware(navController, RomelRoute.CATEGORIES, effectiveRole, onAccessDenied)
                    },
                    onOpenInventory = {
                        navigateRoleAware(navController, RomelRoute.INVENTORY, effectiveRole, onAccessDenied)
                    },
                    onOpenReports = {
                        navigateRoleAware(navController, RomelRoute.REPORTS, effectiveRole, onAccessDenied)
                    },
                    onOpenSettings = {
                        navigateBottomTab(navController, RomelRoute.SETTINGS, effectiveRole, onAccessDenied)
                    },
                    onLogoutAdmin = {
                        vm.logoutAdmin()
                        navController.popBackStack(RomelRoute.HOME, inclusive = false)
                    }
                )
            }
        }

        composable(RomelRoute.ADMIN_PRODUCTS) {
            AdminRouteGuard(role = effectiveRole, onBackHome = {
                navController.popBackStack(RomelRoute.HOME, inclusive = false)
            }) {
                AdminAllProductsScreen(
                    products = allProducts,
                    onBack = { navController.popBackStack() },
                    onAddProduct = {
                        navigateRoleAware(navController, RomelRoute.addOrEditProduct(null), effectiveRole, onAccessDenied)
                    },
                    onEditProduct = { prod ->
                        navigateRoleAware(navController, RomelRoute.addOrEditProduct(prod.id), effectiveRole, onAccessDenied)
                    },
                    onEditPrices = onOpenPriceEditor,
                    onSetAvailability = { prod, avail -> vm.setProductAvailability(prod, avail) },
                    onDeleteProduct = { prod -> vm.deleteProduct(prod) }
                )
            }
        }

        composable(
            route = RomelRoute.ADD_PRODUCT,
            arguments = listOf(navArgument("productId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val editId = backStackEntry.arguments?.getString("productId")?.let(Uri::decode)
            val existingProd = remember(editId, allProducts) {
                if (editId.isNullOrBlank()) null else allProducts.firstOrNull { it.id == editId }
            }
            AdminRouteGuard(role = effectiveRole, onBackHome = {
                navController.popBackStack(RomelRoute.HOME, inclusive = false)
            }) {
                AddEditProductScreen(
                    existing = existingProd,
                    categories = categories,
                    onBack = { navController.popBackStack() },
                    onSave = { prod, isNew, uri ->
                        vm.saveProduct(
                            product = prod,
                            isNew = isNew,
                            pickedImageUri = uri,
                            onSuccessCallback = { navController.popBackStack() }
                        )
                    }
                )
            }
        }

        composable(RomelRoute.PRICE_MANAGEMENT) {
            AdminRouteGuard(role = effectiveRole, onBackHome = {
                navController.popBackStack(RomelRoute.HOME, inclusive = false)
            }) {
                PriceManagementScreen(
                    role = effectiveRole,
                    products = allProducts,
                    movements = movements,
                    updatingProductId = updatingPriceProductId,
                    onBack = { navController.popBackStack() },
                    onQuickUpdateSellingPrice = { prod, newPrice ->
                        vm.updateProductPrices(
                            product = prod,
                            selling = newPrice,
                            wholesale = prod.wholesalePrice,
                            retail = prod.retailPrice,
                            discount = prod.discountPrice
                        )
                    },
                    onOpenFullPriceDialog = onOpenPriceEditor
                )
            }
        }

        composable(RomelRoute.INVENTORY) {
            AdminRouteGuard(role = effectiveRole, onBackHome = {
                navController.popBackStack(RomelRoute.HOME, inclusive = false)
            }) {
                InventoryScreen(
                    products = allProducts,
                    movements = movements,
                    onAddProduct = {
                        navigateRoleAware(navController, RomelRoute.addOrEditProduct(null), effectiveRole, onAccessDenied)
                    },
                    onEditProduct = { prod ->
                        navigateRoleAware(navController, RomelRoute.addOrEditProduct(prod.id), effectiveRole, onAccessDenied)
                    },
                    onEditPrices = onOpenPriceEditor,
                    onAdjustStock = onOpenStockAdjust
                )
            }
        }

        composable(RomelRoute.REPORTS) {
            AdminRouteGuard(role = effectiveRole, onBackHome = {
                navController.popBackStack(RomelRoute.HOME, inclusive = false)
            }) {
                ReportsScreen(metrics = metrics)
            }
        }
    }
}

@Composable
private fun AdminRouteGuard(
    role: UserRole,
    onBackHome: () -> Unit,
    content: @Composable () -> Unit
) {
    if (role == UserRole.ADMIN) {
        content()
    } else {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Restricted Access",
                    tint = RomelRed,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Administrator Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = RomelNavy,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sign in as Administrator from Settings to access this management screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateMuted
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onBackHome,
                    colors = ButtonDefaults.buttonColors(containerColor = RomelNavy)
                ) {
                    Text("Return Home")
                }
            }
        }
    }
}
