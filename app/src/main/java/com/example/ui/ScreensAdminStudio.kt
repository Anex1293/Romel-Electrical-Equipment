package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.CategoryItem
import com.example.model.MovementType
import com.example.model.PriceValidator
import com.example.model.Product
import com.example.model.ProductAvailability
import com.example.model.SalesOrder
import com.example.model.StockMovement
import com.example.model.UserRole
import com.example.model.formatEtb
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.LightBlueSurface
import com.example.ui.theme.RomelNavy
import com.example.ui.theme.RomelRed
import com.example.ui.theme.RomelText
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberSoft
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusEmeraldSoft
import com.example.ui.theme.StatusRose
import com.example.ui.theme.StatusRoseSoft
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AdminLoginScreen(
    adminSession: AdminSessionState,
    onLogin: (username: String, password: String) -> Unit,
    onSetupAdmin: (username: String, password: String, confirmPassword: String) -> Unit,
    onRefreshStatus: () -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val needsFirstTimeSetup = adminSession.isInitialized == false

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = CleanWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = StatusRoseSoft,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = "Administrator Login",
                            tint = RomelRed,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Text(
                    text = if (needsFirstTimeSetup) "First-Time Admin Setup" else "Administrator Login",
                    style = MaterialTheme.typography.headlineSmall,
                    color = RomelNavy,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (needsFirstTimeSetup) {
                        "Create a private administrator username and password to secure your catalog database."
                    } else {
                        "Sign in with your administrator username and password to open the Admin Dashboard."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateMuted
                )

                if (adminSession.isInitialized == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Checking database security status...", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                        TextButton(onClick = onRefreshStatus) {
                            Text("Retry", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        onClearError()
                    },
                    label = { Text("Admin Username") },
                    singleLine = true,
                    enabled = !adminSession.isAuthBusy,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_username_input")
                )

                val submitAction = {
                    if (needsFirstTimeSetup) {
                        onSetupAdmin(username, password, confirmPassword)
                    } else {
                        onLogin(username, password)
                    }
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        onClearError()
                    },
                    label = { Text(if (needsFirstTimeSetup) "New Admin Password (min 6 chars)" else "Admin Password") },
                    singleLine = true,
                    enabled = !adminSession.isAuthBusy,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (needsFirstTimeSetup) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = if (needsFirstTimeSetup) KeyboardActions.Default else KeyboardActions(onDone = { submitAction() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_password_input")
                )

                if (needsFirstTimeSetup) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            onClearError()
                        },
                        label = { Text("Confirm Admin Password") },
                        singleLine = true,
                        enabled = !adminSession.isAuthBusy,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitAction() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_confirm_password_input")
                    )
                }

                adminSession.authError?.let { err ->
                    Surface(
                        color = StatusRoseSoft,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusRose,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = submitAction,
                    enabled = !adminSession.isAuthBusy && username.isNotBlank() && password.isNotBlank() &&
                        (!needsFirstTimeSetup || confirmPassword.isNotBlank()),
                    colors = ButtonDefaults.buttonColors(containerColor = RomelRed, contentColor = CleanWhite),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("admin_login_submit_button")
                ) {
                    if (adminSession.isAuthBusy) {
                        CircularProgressIndicator(color = CleanWhite, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Text(
                            text = if (needsFirstTimeSetup) "Create Admin Account" else "Sign In to Admin Dashboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TextButton(
                    onClick = onBack,
                    enabled = !adminSession.isAuthBusy,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Back to Settings", color = RomelNavy)
                }
            }
        }
    }
}

@Composable
fun AdminDashboardScreen(
    adminUsername: String,
    metrics: DashboardMetrics,
    orders: List<SalesOrder>,
    onAddProduct: () -> Unit,
    onOpenAllProducts: () -> Unit,
    onOpenPriceManagement: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogoutAdmin: () -> Unit
) {
    var chartPeriod by remember { mutableStateOf("This Month") }
    var periodMenuOpen by remember { mutableStateOf(false) }

    // Build real dual-bar series from live orders or category counts/stock when no sales exist yet
    val chartBars = remember(orders, metrics, chartPeriod) {
        val monthFormat = SimpleDateFormat("MMM", Locale.US)
        val cal = Calendar.getInstance()
        val confirmed = orders.filter { it.status == "CONFIRMED" }
        if (confirmed.isNotEmpty()) {
            val months = (4 downTo 0).map { offset ->
                val c = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
                monthFormat.format(c.time) to (c.get(Calendar.MONTH) to c.get(Calendar.YEAR))
            }
            months.map { (label, my) ->
                val matching = confirmed.filter { ord ->
                    val d = ord.createdAt?.toDate() ?: return@filter false
                    val oc = Calendar.getInstance().apply { time = d }
                    oc.get(Calendar.MONTH) == my.first && oc.get(Calendar.YEAR) == my.second
                }
                val orderCountScaled = matching.size.toFloat() * 100f
                val salesAmount = matching.sumOf { it.totalAmount }.toFloat()
                Triple(label, orderCountScaled.coerceAtLeast(10f), salesAmount)
            }
        } else {
            metrics.categoryStats
                .sortedByDescending { it.productCount }
                .take(5)
                .map { stat ->
                    val shortName = stat.name.split(" ").firstOrNull()?.take(8) ?: stat.name.take(8)
                    Triple(shortName, stat.productCount.toFloat(), stat.totalStock.toFloat().coerceAtLeast(1f))
                }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 2x2 White KPI Grid matching the Red Admin Dashboard reference
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AdminKpiCard(
                        icon = Icons.Filled.Inventory2,
                        value = "${metrics.totalProducts}",
                        label = "Products",
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiCard(
                        icon = Icons.Filled.Category,
                        value = "${metrics.totalCategories}",
                        label = "Categories",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AdminKpiCard(
                        icon = Icons.Filled.Inventory,
                        value = String.format(Locale.US, "%,d", metrics.totalAvailableStock),
                        label = "Stock (PCS)",
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiCard(
                        icon = Icons.Filled.Analytics,
                        value = formatEtb(metrics.monthlySalesEtb),
                        label = "Monthly Sales",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiTile("Today", formatEtb(metrics.todaysSalesEtb), StatusEmerald, Modifier.weight(1f))
                    KpiTile("Low Stock", "${metrics.lowStockCount}", StatusAmber, Modifier.weight(1f))
                    KpiTile("Out / Review", "${metrics.outOfStockCount + metrics.needsReviewCount}", RomelRed, Modifier.weight(1f))
                }
            }
        }

        // Sales Overview Dual Bar Chart Card
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = CleanWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sales Overview", style = MaterialTheme.typography.titleMedium, color = RomelText)
                        Box {
                            OutlinedButton(
                                onClick = { periodMenuOpen = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text(chartPeriod, style = MaterialTheme.typography.labelSmall)
                            }
                            DropdownMenu(expanded = periodMenuOpen, onDismissRequest = { periodMenuOpen = false }) {
                                listOf("This Month", "All Time").forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p) },
                                        onClick = {
                                            chartPeriod = p
                                            periodMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    DualBarChart(
                        bars = chartBars,
                        navyLabel = if (orders.any { it.status == "CONFIRMED" }) "Confirmed Orders" else "Catalog SKUs",
                        redLabel = if (orders.any { it.status == "CONFIRMED" }) "Sales (ETB)" else "Stock Units"
                    )
                }
            }
        }

        // Quick Actions Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Catalog & Store Management", style = MaterialTheme.typography.titleMedium, color = RomelText)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionTile(
                        icon = Icons.Filled.AddCircle,
                        label = "Add Product",
                        accent = RomelNavy,
                        onClick = onAddProduct,
                        modifier = Modifier.weight(1f).testTag("admin_quick_add_product")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.Storefront,
                        label = "All Products",
                        accent = RomelRed,
                        onClick = onOpenAllProducts,
                        modifier = Modifier.weight(1f).testTag("admin_quick_all_products")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionTile(
                        icon = Icons.Filled.LocalOffer,
                        label = "Update Price",
                        accent = RomelRed,
                        onClick = onOpenPriceManagement,
                        modifier = Modifier.weight(1f).testTag("admin_quick_update_price")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.Category,
                        label = "Categories",
                        accent = RomelNavy,
                        onClick = onOpenCategories,
                        modifier = Modifier.weight(1f).testTag("admin_quick_categories")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionTile(
                        icon = Icons.Filled.Inventory,
                        label = "Manage Stock",
                        accent = RomelNavy,
                        onClick = onOpenInventory,
                        modifier = Modifier.weight(1f).testTag("admin_quick_manage_stock")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.Analytics,
                        label = "View Reports",
                        accent = RomelNavy,
                        onClick = onOpenReports,
                        modifier = Modifier.weight(1f).testTag("admin_quick_view_reports")
                    )
                }
            }
        }

        // Administrator Session & Security card
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = CleanWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = RomelNavy, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Admin Session: $adminUsername", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${metrics.unavailableCount} hidden • ${metrics.outOfStockCount} out of stock • ${metrics.totalCategories} categories",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateMuted
                                )
                            }
                        }
                        TextButton(onClick = onOpenSettings) {
                            Text("Settings")
                        }
                    }
                    OutlinedButton(
                        onClick = onLogoutAdmin,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dashboard_logout_admin")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp), tint = RomelRed)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out of Admin Mode", color = RomelRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAllProductsScreen(
    products: List<Product>,
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onEditPrices: (Product) -> Unit,
    onSetAvailability: (Product, ProductAvailability) -> Unit,
    onDeleteProduct: (Product) -> Unit
) {
    BackHandler(onBack = onBack)

    var search by remember { mutableStateOf("") }
    var filterAvail by remember { mutableStateOf<ProductAvailability?>(null) }
    var deletingProduct by remember { mutableStateOf<Product?>(null) }

    val filtered = remember(products, search, filterAvail) {
        val byAvail = if (filterAvail == null) products else products.filter { it.parsedAvailability == filterAvail }
        val q = search.trim()
        if (q.isBlank()) byAvail else byAvail.filter { MainViewModel.matchScore(it, q) > 0 }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CleanWhite)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search all products by name or code...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("admin_all_products_search")
                )
                Button(
                    onClick = onAddProduct,
                    colors = ButtonDefaults.buttonColors(containerColor = RomelRed, contentColor = CleanWhite),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(52.dp).testTag("admin_all_products_add")
                ) {
                    Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterAvail == null,
                    onClick = { filterAvail = null },
                    label = { Text("All (${products.size})") }
                )
                ProductAvailability.entries.forEach { avail ->
                    FilterChip(
                        selected = filterAvail == avail,
                        onClick = { filterAvail = avail },
                        label = { Text(avail.label) }
                    )
                }
            }
            Text(
                text = "Showing ${filtered.size} of ${products.size} catalog products",
                style = MaterialTheme.typography.labelSmall,
                color = SlateMuted
            )
        }
        HorizontalDivider()

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.id }) { prod ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = CleanWhite),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().testTag("admin_product_row_${prod.code}")
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProductImage(prod, modifier = Modifier.size(56.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${prod.code} • ${prod.category}", style = MaterialTheme.typography.labelSmall, color = SlateMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = formatEtb(prod.effectiveUnitPrice),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (prod.effectiveUnitPrice != null) RomelRed else SlateMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                    StockBadge(prod)
                                }
                            }
                        }

                        // Manage product availability inline
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Availability:", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                            ProductAvailability.entries.forEach { avail ->
                                FilterChip(
                                    selected = prod.parsedAvailability == avail,
                                    onClick = {
                                        if (prod.parsedAvailability != avail) {
                                            onSetAvailability(prod, avail)
                                        }
                                    },
                                    label = { Text(avail.label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { onEditProduct(prod) },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("admin_edit_${prod.code}")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { onEditPrices(prod) },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("admin_price_${prod.code}")
                            ) {
                                Icon(Icons.Filled.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Price", maxLines = 1)
                            }
                            IconButton(
                                onClick = { deletingProduct = prod },
                                modifier = Modifier.size(48.dp).testTag("admin_delete_${prod.code}")
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete ${prod.code}", tint = StatusRose)
                            }
                        }
                    }
                }
            }
        }
    }

    deletingProduct?.let { prod ->
        AlertDialog(
            onDismissRequest = { deletingProduct = null },
            title = { Text("Delete Product?") },
            text = {
                Text("Permanently delete \"${prod.name}\" (${prod.code}) from Firestore? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = RomelRed),
                    onClick = {
                        onDeleteProduct(prod)
                        deletingProduct = null
                    },
                    modifier = Modifier.testTag("confirm_delete_product_button")
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingProduct = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AdminKpiCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = CleanWhite),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 10.dp)
        ) {
            Surface(color = LightBlueSurface, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = label, tint = RomelNavy, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = RomelText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = CleanWhite),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .height(84.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(10.dp)
        ) {
            Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = RomelText)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    existing: Product?,
    categories: List<CategoryItem>,
    onBack: () -> Unit,
    onSave: (Product, isNew: Boolean, pickedImageUri: Uri?) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val isNew = existing == null
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var code by remember(existing?.id) { mutableStateOf(existing?.code ?: "") }
    var category by remember(existing?.id) { mutableStateOf(existing?.category ?: categories.firstOrNull()?.name ?: "") }
    var series by remember(existing?.id) { mutableStateOf(existing?.series ?: "") }
    var priceText by remember(existing?.id) { mutableStateOf(existing?.sellingPrice?.toString() ?: "") }
    var stockText by remember(existing?.id) { mutableStateOf(existing?.stockQuantity?.toString() ?: "") }
    var description by remember(existing?.id) { mutableStateOf(existing?.description ?: "") }
    var specs by remember(existing?.id) { mutableStateOf(existing?.specifications ?: "") }
    var unit by remember(existing?.id) { mutableStateOf(existing?.unit ?: "PCS") }
    var boxQty by remember(existing?.id) { mutableStateOf((existing?.piecesPerBox ?: 10L).toString()) }
    var cartonQty by remember(existing?.id) { mutableStateOf((existing?.piecesPerCarton ?: 100L).toString()) }
    var lowThresh by remember(existing?.id) { mutableStateOf((existing?.lowStockThreshold ?: 10L).toString()) }
    var availability by remember(existing?.id) { mutableStateOf(existing?.parsedAvailability ?: ProductAvailability.AVAILABLE) }
    var needsReview by remember(existing?.id) { mutableStateOf(existing?.needsReview ?: false) }
    var reviewNote by remember(existing?.id) { mutableStateOf(existing?.reviewNote ?: "") }
    var pickedUri by remember(existing?.id) { mutableStateOf<Uri?>(null) }
    var catExpanded by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = CleanWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Upload Image Tile matching reference bottom-left screen
                    Surface(
                        color = LightBlueSurface,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.5.dp, RomelNavy.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .size(116.dp)
                            .clickable {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("add_product_upload_image")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            val previewData: Any? = pickedUri
                                ?: existing?.customImageUri?.takeIf { it.isNotBlank() }
                                ?: existing?.imageAsset?.takeIf { it.isNotBlank() }?.let { "file:///android_asset/$it" }
                            if (previewData != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(previewData).size(300, 300).build(),
                                    contentDescription = "Selected product image",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().padding(8.dp).clip(MaterialTheme.shapes.small)
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.PhotoCamera, contentDescription = "Upload Image", tint = SlateMuted, modifier = Modifier.size(30.dp))
                                    Spacer(Modifier.height(6.dp))
                                    Text("Upload Image", style = MaterialTheme.typography.labelSmall, color = RomelNavy)
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Product Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_product_name")
                        )
                        OutlinedTextField(
                            value = code,
                            onValueChange = { if (isNew) code = it },
                            enabled = isNew,
                            label = { Text("Product Code *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_product_code")
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    category = cat.name
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price (ETB)") },
                        placeholder = { Text("Price not set") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_product_price")
                    )
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it.filter { c -> c.isDigit() } },
                        label = { Text("Stock Quantity") },
                        placeholder = { Text("Not set") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_product_stock")
                    )
                }

                OutlinedTextField(
                    value = series,
                    onValueChange = { series = it },
                    label = { Text("Series") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = specs,
                    onValueChange = { specs = it },
                    label = { Text("Technical Specifications (use | between specs)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = boxQty,
                        onValueChange = { boxQty = it.filter { c -> c.isDigit() } },
                        label = { Text("PCS/Box") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cartonQty,
                        onValueChange = { cartonQty = it.filter { c -> c.isDigit() } },
                        label = { Text("PCS/Carton") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = lowThresh,
                    onValueChange = { lowThresh = it.filter { c -> c.isDigit() } },
                    label = { Text("Low Stock Alert Threshold") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Product Availability", style = MaterialTheme.typography.labelLarge, color = RomelText)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProductAvailability.entries.forEach { avail ->
                        FilterChip(
                            selected = availability == avail,
                            onClick = { availability = avail },
                            label = { Text(avail.label) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = needsReview, onCheckedChange = { needsReview = it })
                    Text("Flag for Admin Review", style = MaterialTheme.typography.bodySmall)
                }
                if (needsReview) {
                    OutlinedTextField(
                        value = reviewNote,
                        onValueChange = { reviewNote = it },
                        label = { Text("Review Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    enabled = code.isNotBlank() && name.isNotBlank(),
                    onClick = {
                        val base = existing ?: Product()
                        val parsedPrice = priceText.trim().toDoubleOrNull()?.takeIf { it >= 0.0 }
                        val parsedStock = stockText.trim().toLongOrNull()?.takeIf { it >= 0L }
                        onSave(
                            base.copy(
                                code = code.trim(),
                                name = name.trim(),
                                category = category.ifBlank { "Uncategorized" },
                                series = series.ifBlank { category },
                                description = description.trim(),
                                specifications = specs.trim(),
                                unit = unit.trim().ifBlank { "PCS" },
                                piecesPerBox = (boxQty.toLongOrNull() ?: 10L).coerceAtLeast(1L),
                                piecesPerCarton = (cartonQty.toLongOrNull() ?: 100L).coerceAtLeast(1L),
                                lowStockThreshold = (lowThresh.toLongOrNull() ?: 10L).coerceAtLeast(0L),
                                sellingPrice = parsedPrice ?: base.sellingPrice,
                                stockQuantity = if (availability == ProductAvailability.OUT_OF_STOCK && parsedStock == null) 0L else (parsedStock ?: base.stockQuantity),
                                availabilityStatus = availability.name,
                                needsReview = needsReview,
                                reviewNote = reviewNote.takeIf { needsReview && it.isNotBlank() }
                            ),
                            isNew,
                            pickedUri
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RomelRed, contentColor = CleanWhite),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_product_button")
                ) {
                    Text("Save Product", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private enum class PriceFilterMode(val label: String) {
    ALL("All"),
    NOT_SET("Price Not Set"),
    PRICED("Priced"),
    DISCOUNTED("Discounted")
}

@Composable
fun PriceManagementScreen(
    role: UserRole,
    products: List<Product>,
    movements: List<StockMovement>,
    updatingProductId: String? = null,
    onBack: () -> Unit,
    onQuickUpdateSellingPrice: (Product, Double?) -> Unit,
    onOpenFullPriceDialog: (Product) -> Unit
) {
    BackHandler(onBack = onBack)

    if (role != UserRole.ADMIN) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = "Admin Only", tint = RomelRed, modifier = Modifier.size(48.dp))
                Text("Admin Role Required", style = MaterialTheme.typography.titleLarge, color = RomelNavy, fontWeight = FontWeight.Bold)
                Text(
                    "Updating product prices in Firestore is strictly restricted to ADMIN accounts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateMuted
                )
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = RomelNavy)) {
                    Text("Back")
                }
            }
        }
        return
    }

    var search by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(PriceFilterMode.ALL) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val pricedCount = remember(products) { products.count { it.sellingPrice != null } }
    val filtered = remember(products, search, filterMode) {
        val byMode = when (filterMode) {
            PriceFilterMode.ALL -> products
            PriceFilterMode.NOT_SET -> products.filter { it.sellingPrice == null }
            PriceFilterMode.PRICED -> products.filter { it.sellingPrice != null }
            PriceFilterMode.DISCOUNTED -> products.filter { it.discountPrice != null }
        }
        val q = search.trim()
        if (q.isBlank()) byMode else byMode.filter { MainViewModel.matchScore(it, q) > 0 }
    }
    val priceHistory = remember(movements) {
        movements.filter { it.movementType == MovementType.PRICE_UPDATE.name }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header strip with search, status summary, filter chips, and Price History pill
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CleanWhite)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search code, name, or category...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("price_mgmt_search")
                )
                Button(
                    onClick = { showHistoryDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RomelNavy, contentColor = CleanWhite),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(52.dp).testTag("price_history_button")
                ) {
                    Icon(Icons.Filled.History, contentDescription = "Price History", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("History")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PriceFilterMode.entries.forEach { mode ->
                    FilterChip(
                        selected = filterMode == mode,
                        onClick = { filterMode = mode },
                        label = { Text(mode.label) }
                    )
                }
            }

            Text(
                text = "Admin Firestore Pricing • $pricedCount/${products.size} products priced (${filtered.size} shown)",
                style = MaterialTheme.typography.labelSmall,
                color = SlateMuted
            )
        }
        HorizontalDivider()

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.id }) { prod ->
                var newPriceInput by remember(prod.id, prod.sellingPrice) {
                    mutableStateOf(prod.sellingPrice?.let { String.format(Locale.US, "%.2f", it) } ?: "")
                }
                val parsed = remember(newPriceInput, prod.discountPrice) {
                    val base = PriceValidator.validateSinglePrice(newPriceInput, "Unit price")
                    val crossError = if (
                        base.error == null &&
                        base.value != null &&
                        prod.discountPrice != null &&
                        prod.discountPrice > base.value
                    ) {
                        "Below discount (${formatEtb(prod.discountPrice)}). Use Tiers to adjust both."
                    } else null
                    base.value to (base.error ?: crossError)
                }
                val parsedValue = parsed.first
                val validationError = parsed.second
                val isSaving = updatingProductId == prod.id
                val hasChanged = parsedValue != prod.sellingPrice

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = CleanWhite),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProductImage(prod, modifier = Modifier.size(52.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(prod.code, style = MaterialTheme.typography.labelMedium, color = RomelNavy)
                                    Text(
                                        text = formatEtb(prod.sellingPrice),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (prod.sellingPrice != null) RomelRed else SlateMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (prod.wholesalePrice != null || prod.retailPrice != null || prod.discountPrice != null) {
                                    Text(
                                        text = buildString {
                                            prod.wholesalePrice?.let { append("W: ${formatEtb(it)}  ") }
                                            prod.retailPrice?.let { append("R: ${formatEtb(it)}  ") }
                                            prod.discountPrice?.let { append("D: ${formatEtb(it)}") }
                                        }.trim(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { onOpenFullPriceDialog(prod) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(48.dp).testTag("full_tiers_${prod.code}")
                            ) {
                                Icon(Icons.Filled.AttachMoney, contentDescription = "All price tiers", modifier = Modifier.size(16.dp), tint = RomelNavy)
                                Text("Tiers", color = RomelNavy, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newPriceInput,
                                onValueChange = { newPriceInput = it },
                                label = { Text("Unit Selling Price (ETB)") },
                                placeholder = { Text("Price not set") },
                                isError = validationError != null,
                                supportingText = validationError?.let { err -> { Text(err) } },
                                trailingIcon = {
                                    if (newPriceInput.isNotEmpty()) {
                                        IconButton(onClick = { newPriceInput = "" }) {
                                            Icon(Icons.Filled.Clear, contentDescription = "Clear price input")
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("price_input_${prod.code}")
                            )
                            Button(
                                enabled = !isSaving && validationError == null && hasChanged,
                                onClick = {
                                    onQuickUpdateSellingPrice(prod, parsedValue)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RomelRed, contentColor = CleanWhite),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .height(52.dp)
                                    .testTag("update_price_${prod.code}")
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        color = CleanWhite,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(if (parsedValue == null && prod.sellingPrice != null) "Clear" else "Update")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHistoryDialog) {
        val fmt = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.US) }
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Admin Price Change History") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (priceHistory.isEmpty()) {
                        Text("No price updates recorded yet.", style = MaterialTheme.typography.bodySmall, color = SlateMuted)
                    } else {
                        priceHistory.take(40).forEach { mov ->
                            Surface(color = LightBlueSurface, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("${mov.productCode} • ${mov.productName}", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = "${formatEtb(mov.previousPrice)} → ${formatEtb(mov.newPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RomelRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "By ${mov.performedByName} • ${mov.createdAt?.toDate()?.let { fmt.format(it) } ?: "Now"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateMuted
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) { Text("Close") }
            }
        )
    }
}
