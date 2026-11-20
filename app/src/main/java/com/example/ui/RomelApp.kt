package com.example.ui

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.Product
import com.example.model.UserRole
import com.example.ui.navigation.BottomNavigationBar
import com.example.ui.navigation.RomelNavHost
import com.example.ui.navigation.RomelRoute
import com.example.ui.navigation.appSectionToRoute
import com.example.ui.navigation.navigateBottomTab
import com.example.ui.navigation.navigateRoleAware
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.LightBlueSurface
import com.example.ui.theme.OffWhiteBg
import com.example.ui.theme.RomelNavy
import com.example.ui.theme.RomelRed
import com.example.ui.theme.RomelText
import com.example.ui.theme.SlateMuted
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    RomelMainShell()
}

private fun sectionIcon(section: AppSection): ImageVector = when (section) {
    AppSection.HOME -> Icons.Filled.Home
    AppSection.PRODUCTS -> Icons.Filled.ElectricalServices
    AppSection.CATEGORIES -> Icons.Filled.Category
    AppSection.SEARCH -> Icons.Filled.Search
    AppSection.WISHLIST -> Icons.Filled.Favorite
    AppSection.ORDERS -> Icons.Filled.ShoppingCart
    AppSection.ORDER_HISTORY -> Icons.Filled.ReceiptLong
    AppSection.ADMIN_LOGIN -> Icons.Filled.AdminPanelSettings
    AppSection.ADMIN_DASHBOARD -> Icons.Filled.AdminPanelSettings
    AppSection.ADMIN_PRODUCTS -> Icons.Filled.Storefront
    AppSection.ADD_PRODUCT -> Icons.Filled.AddCircle
    AppSection.PRICE_MANAGEMENT -> Icons.Filled.LocalOffer
    AppSection.INVENTORY -> Icons.Filled.Inventory
    AppSection.REPORTS -> Icons.Filled.Analytics
    AppSection.SETTINGS -> Icons.Filled.Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RomelMainShell() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val vm: MainViewModel = viewModel(
        key = "romel_main_vm",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(application) as T
            }
        }
    )

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: RomelRoute.HOME

    val calculatorProduct by vm.calculatorProduct.collectAsStateWithLifecycle()
    val adminSession by vm.adminSession.collectAsStateWithLifecycle()
    val effectiveRole by vm.effectiveRole.collectAsStateWithLifecycle()
    val cartItems by vm.cartItems.collectAsStateWithLifecycle()
    val wishlistItems by vm.wishlistItems.collectAsStateWithLifecycle()
    val statusMessage by vm.statusMessage.collectAsStateWithLifecycle()
    val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()

    var priceEditProduct by remember { mutableStateOf<Product?>(null) }
    var stockAdjustProduct by remember { mutableStateOf<Product?>(null) }
    var showQuickMenuSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(statusMessage, errorMessage) {
        val msg = errorMessage ?: statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            vm.clearMessages()
        }
    }

    val notifyAccessDenied: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    val permittedSections = remember(effectiveRole) {
        AppSection.entries.filter {
            it != AppSection.ADMIN_LOGIN && (!it.adminOnly || effectiveRole == UserRole.ADMIN)
        }
    }

    val topLevelRoutes = remember {
        setOf(RomelRoute.HOME, RomelRoute.PRODUCTS, RomelRoute.WISHLIST, RomelRoute.ORDERS, RomelRoute.SETTINGS)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        val showTopBar = currentRoute != RomelRoute.HOME
        val isAdminDashBar = currentRoute == RomelRoute.ADMIN_DASHBOARD
        val barContainerColor = if (isAdminDashBar) RomelRed else RomelNavy
        val hasBackAction = currentRoute !in topLevelRoutes

        val topBarTitle = when (currentRoute) {
            RomelRoute.PRODUCT_DETAIL -> "Product Details"
            RomelRoute.PRODUCTS -> "Products"
            RomelRoute.WISHLIST -> "My Wishlist"
            RomelRoute.ORDERS -> "My Order"
            RomelRoute.ORDER_HISTORY -> "Order History"
            RomelRoute.SETTINGS -> "Settings"
            RomelRoute.CATEGORIES -> "Categories"
            RomelRoute.SEARCH -> "Search Catalog"
            RomelRoute.ADMIN_LOGIN -> "Admin Login"
            RomelRoute.ADMIN_DASHBOARD -> "Admin Dashboard"
            RomelRoute.ADMIN_PRODUCTS -> "Manage All Products"
            RomelRoute.ADD_PRODUCT -> {
                val editId = navBackStackEntry?.arguments?.getString("productId")
                if (editId.isNullOrBlank()) "Admin – Add New Product" else "Admin – Edit Product"
            }
            RomelRoute.PRICE_MANAGEMENT -> "Price Management (Admin Only)"
            RomelRoute.INVENTORY -> "Inventory Control"
            RomelRoute.REPORTS -> "Business Analytics"
            else -> "ROMEL Catalog"
        }

        Scaffold(
            containerColor = OffWhiteBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (showTopBar) {
                    TopAppBar(
                        title = {
                            Text(
                                text = topBarTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            if (isAdminDashBar && !useRail) {
                                IconButton(
                                    onClick = { showQuickMenuSheet = true },
                                    modifier = Modifier.size(48.dp).testTag("topbar_menu")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "Quick Menu",
                                        tint = CleanWhite
                                    )
                                }
                            } else if (hasBackAction) {
                                IconButton(
                                    onClick = { navController.navigateUp() },
                                    modifier = Modifier.size(48.dp).testTag("topbar_back")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = CleanWhite
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = barContainerColor,
                            titleContentColor = CleanWhite,
                            navigationIconContentColor = CleanWhite,
                            actionIconContentColor = CleanWhite
                        ),
                        actions = {
                            if (currentRoute == RomelRoute.ORDERS && cartItems.isNotEmpty()) {
                                IconButton(
                                    onClick = { vm.clearCart() },
                                    modifier = Modifier.size(48.dp).testTag("topbar_clear_cart")
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Clear Order List")
                                }
                            } else if (isAdminDashBar) {
                                IconButton(
                                    onClick = {
                                        vm.logoutAdmin()
                                        navController.popBackStack(RomelRoute.HOME, inclusive = false)
                                    },
                                    modifier = Modifier.size(48.dp).testTag("topbar_logout_admin")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out of Admin")
                                }
                                IconButton(
                                    onClick = {
                                        navigateBottomTab(navController, RomelRoute.SETTINGS, effectiveRole, notifyAccessDenied)
                                    },
                                    modifier = Modifier.size(48.dp).testTag("topbar_profile_settings")
                                ) {
                                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                                }
                            } else if (!useRail && currentRoute in topLevelRoutes) {
                                IconButton(
                                    onClick = { showQuickMenuSheet = true },
                                    modifier = Modifier.size(48.dp).testTag("topbar_more")
                                ) {
                                    Icon(Icons.Filled.MoreHoriz, contentDescription = "More Sections")
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!useRail) {
                    BottomNavigationBar(
                        currentRoute = currentRoute,
                        role = effectiveRole,
                        ordersBadgeCount = cartItems.size,
                        wishlistBadgeCount = wishlistItems.size,
                        onSelectRoute = { targetRoute ->
                            navigateBottomTab(navController, targetRoute, effectiveRole, notifyAccessDenied)
                        }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = CleanWhite,
                        modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())
                    ) {
                        permittedSections.forEach { sec ->
                            val targetRoute = appSectionToRoute(sec)
                            val selected = currentRoute == targetRoute
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (targetRoute in topLevelRoutes) {
                                        navigateBottomTab(navController, targetRoute, effectiveRole, notifyAccessDenied)
                                    } else {
                                        navigateRoleAware(navController, targetRoute, effectiveRole, notifyAccessDenied)
                                    }
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = RomelRed,
                                    selectedTextColor = RomelRed,
                                    indicatorColor = RomelRed.copy(alpha = 0.12f),
                                    unselectedIconColor = SlateMuted,
                                    unselectedTextColor = SlateMuted
                                ),
                                icon = {
                                    val badgeCount = when (sec) {
                                        AppSection.ORDERS -> cartItems.size
                                        AppSection.WISHLIST -> wishlistItems.size
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
                                            Icon(sectionIcon(sec), contentDescription = sec.label)
                                        }
                                    } else {
                                        Icon(sectionIcon(sec), contentDescription = sec.label)
                                    }
                                },
                                label = {
                                    val label = if (sec == AppSection.ORDERS) "Orders" else sec.label
                                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                modifier = Modifier.testTag("rail_${sec.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    RomelNavHost(
                        navController = navController,
                        vm = vm,
                        effectiveRole = effectiveRole,
                        onOpenPriceEditor = { priceEditProduct = it },
                        onOpenStockAdjust = { stockAdjustProduct = it },
                        onAccessDenied = notifyAccessDenied,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showQuickMenuSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showQuickMenuSheet = false },
            sheetState = sheetState,
            containerColor = CleanWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ROMEL Navigation", style = MaterialTheme.typography.titleLarge, color = RomelNavy)
                        Text(
                            text = if (adminSession.isAdminLoggedIn) {
                                "Admin: ${adminSession.adminUsername ?: "Administrator"}"
                            } else {
                                "ROMEL Electrical Equipment Catalog"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                    }
                    Surface(color = LightBlueSurface, shape = RoundedCornerShape(999.dp)) {
                        Text(
                            text = if (adminSession.isAdminLoggedIn) "Admin Mode" else "Catalog",
                            style = MaterialTheme.typography.labelSmall,
                            color = RomelNavy,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                permittedSections.forEach { sec ->
                    val targetRoute = appSectionToRoute(sec)
                    val active = currentRoute == targetRoute
                    val itemLabel = if (sec == AppSection.ORDERS) "Orders" else sec.label
                    ListItem(
                        headlineContent = {
                            Text(
                                text = itemLabel,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) RomelRed else RomelText
                            )
                        },
                        supportingContent = if (sec.adminOnly) {
                            { Text("Admin Only", style = MaterialTheme.typography.labelSmall, color = RomelRed) }
                        } else null,
                        leadingContent = {
                            Icon(
                                imageVector = sectionIcon(sec),
                                contentDescription = itemLabel,
                                tint = if (active) RomelRed else RomelNavy
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (active) RomelRed.copy(alpha = 0.08f) else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showQuickMenuSheet = false
                                if (targetRoute in topLevelRoutes) {
                                    navigateBottomTab(navController, targetRoute, effectiveRole, notifyAccessDenied)
                                } else {
                                    navigateRoleAware(navController, targetRoute, effectiveRole, notifyAccessDenied)
                                }
                            }
                            .testTag("menu_${sec.name.lowercase()}")
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    calculatorProduct?.let { prod ->
        QuantityPriceCalculatorSheet(
            product = prod,
            role = effectiveRole,
            onDismiss = { vm.closeCalculator() },
            onAddToOrder = { unit, mult, price, qty, disc ->
                vm.addToCart(prod, unit, mult, price, qty, disc)
            }
        )
    }

    priceEditProduct?.let { prod ->
        AdminPriceEditorDialog(
            product = prod,
            onDismiss = { priceEditProduct = null },
            onSave = { sell, whole, retail, disc ->
                vm.updateProductPrices(prod, sell, whole, retail, disc)
            }
        )
    }

    stockAdjustProduct?.let { prod ->
        AdminStockAdjustDialog(
            product = prod,
            onDismiss = { stockAdjustProduct = null },
            onConfirm = { delta, reason ->
                vm.adjustProductStock(prod, delta, reason)
            }
        )
    }
}
