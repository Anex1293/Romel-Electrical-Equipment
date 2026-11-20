package com.example.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.model.CategoryItem
import com.example.model.Product
import com.example.model.ProductSortOption
import com.example.model.StockAvailabilityFilter
import com.example.model.UnitOfMeasure
import com.example.model.UserRole
import com.example.model.formatEtb
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.LightBlueSurface
import com.example.ui.theme.RomelNavy
import com.example.ui.theme.RomelNavyDark
import com.example.ui.theme.RomelRed
import com.example.ui.theme.RomelText
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberSoft

private data class VisualCategoryTile(
    val title: String,
    val imageFile: String,
    val keywords: List<String>
)

private val homeCategoryTiles = listOf(
    VisualCategoryTile("Switches & Sockets", "rl0602.jpg", listOf("Splendor", "Jewel", "Italian Standard RL08", "Piano", "Heavy Duty", "Waterproof")),
    VisualCategoryTile("Plates", "rl0863e_wh.jpg", listOf("Cover Plates", "Glass Plates", "Metal Plates", "Aluminum Plates", "Stainless Steel Plates")),
    VisualCategoryTile("MCB", "mcb_1p.jpg", listOf("Miniature Circuit Breakers")),
    VisualCategoryTile("LED Lights", "t60_9w.jpg", listOf("LED Bulbs", "Recessed Panel", "Surface Panel", "LED Spotlights")),
    VisualCategoryTile("Distribution Boards", "rl306032.jpg", listOf("Distribution Boards")),
    VisualCategoryTile("Accessories", "rk530.jpg", listOf("Adaptor", "Scatola", "Junction Boxes"))
)

@Composable
fun HomeScreen(
    role: UserRole,
    userName: String,
    metrics: DashboardMetrics,
    homeSearchQuery: String,
    featuredProducts: List<Product>,
    wishlistProductIds: Set<String> = emptySet(),
    isCloudSynced: Boolean,
    onHomeSearchQueryChange: (String) -> Unit,
    onSyncCloud: () -> Unit,
    onQuickCodeSearch: (String) -> Unit,
    onNavigateSection: (AppSection) -> Unit,
    onOpenCategoryGroup: (title: String, keywords: List<String>) -> Unit,
    onProductClick: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit = {},
    onOpenCalculator: (Product) -> Unit,
    onEditPrice: (Product) -> Unit,
    onAdjustStock: (Product) -> Unit
) {
    val context = LocalContext.current
    val alertCount = metrics.lowStockCount + metrics.outOfStockCount + metrics.needsReviewCount
    var showAlertsDialog by remember { mutableStateOf(false) }
    val isSearching = homeSearchQuery.isNotBlank()

    val bannerImgReq = remember(context) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/catalog_images/romel_cover_hero.jpg")
            .size(Size(360, 280))
            .build()
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // White Top Brand Header with ROMEL logo & Notification Bell matching reference Home
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.size(48.dp))
                RomelWordmarkLogo(
                    darkBackground = false,
                    titleSize = 24.sp,
                    emblemSize = 22.dp
                )
                IconButton(
                    onClick = {
                        if (role == UserRole.ADMIN) onNavigateSection(AppSection.INVENTORY)
                        else showAlertsDialog = true
                    },
                    modifier = Modifier.size(48.dp).testTag("home_notifications_button")
                ) {
                    if (alertCount > 0) {
                        BadgedBox(badge = { Badge(containerColor = RomelRed) { Text("$alertCount") } }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Catalog Notifications", tint = RomelNavy)
                        }
                    } else {
                        Icon(Icons.Filled.Notifications, contentDescription = "Catalog Notifications", tint = RomelNavy)
                    }
                }
            }
        }

        // Live Real-Time Search Bar filtering Firestore products by name
        item {
            OutlinedTextField(
                value = homeSearchQuery,
                onValueChange = onHomeSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search products by name...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = RomelNavy)
                },
                trailingIcon = {
                    if (isSearching) {
                        IconButton(
                            onClick = { onHomeSearchQueryChange("") },
                            modifier = Modifier.testTag("home_search_clear")
                        ) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search", tint = SlateMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(999.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CleanWhite,
                    unfocusedContainerColor = CleanWhite,
                    focusedBorderColor = RomelNavy,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_bar")
            )
        }

        if (isSearching) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Results (${featuredProducts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = RomelText
                    )
                    TextButton(onClick = { onHomeSearchQueryChange("") }) {
                        Text("Clear Search", color = RomelRed)
                    }
                }
            }
            if (featuredProducts.isEmpty()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = CleanWhite),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(40.dp), tint = SlateMuted)
                            Text("No matching products", style = MaterialTheme.typography.titleMedium, color = RomelText)
                            Text(
                                text = "Try another product name or category (e.g. Switch, Plate, MCB, LED).",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(featuredProducts, key = { it.id }) { prod ->
                    ProductCard(
                        product = prod,
                        role = role,
                        onClick = { onProductClick(prod) },
                        onOpenCalculator = { onOpenCalculator(prod) },
                        onEditPrice = { onEditPrice(prod) },
                        onAdjustStock = { onAdjustStock(prod) },
                        isWishlisted = prod.id in wishlistProductIds,
                        onToggleWishlist = { onToggleWishlist(prod) }
                    )
                }
            }
        } else {

        if (role == UserRole.ADMIN && !isCloudSynced) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusAmberSoft),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = StatusAmber)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sync 2025 Catalog to Cloud", style = MaterialTheme.typography.titleSmall, color = RomelNavy)
                            Text("Push 139 ROMEL items & 21 categories to Firestore.", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = onSyncCloud,
                            colors = ButtonDefaults.buttonColors(containerColor = RomelNavy),
                            modifier = Modifier.height(48.dp)
                        ) { Text("Sync") }
                    }
                }
            }
        }

        // Promotional Navy Banner "Quality Electrical Solutions" matching reference
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp)
                        .background(Brush.horizontalGradient(listOf(RomelNavy, RomelNavyDark)))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val ribbon = Path().apply {
                            moveTo(0f, size.height)
                            cubicTo(size.width * 0.25f, size.height * 0.85f, size.width * 0.55f, size.height * 0.50f, size.width * 0.72f, 0f)
                            lineTo(size.width * 0.80f, 0f)
                            cubicTo(size.width * 0.62f, size.height * 0.55f, size.width * 0.30f, size.height * 0.92f, 0f, size.height)
                            close()
                        }
                        drawPath(ribbon, color = RomelRed.copy(alpha = 0.88f))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Quality Electrical\nSolutions",
                                style = MaterialTheme.typography.headlineSmall,
                                color = CleanWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (role == UserRole.ADMIN) {
                                    "Admin: $userName • ${metrics.totalProducts} SKUs"
                                } else {
                                    "ROMEL 2025 Catalog • ${metrics.totalProducts} SKUs"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        AsyncImage(
                            model = bannerImgReq,
                            contentDescription = "ROMEL 2025 Electrical Equipment",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 122.dp, height = 104.dp)
                                .clip(MaterialTheme.shapes.medium)
                        )
                    }
                }
            }
        }

        // Admin quick shortcut banner to open the Red Admin Dashboard
        if (role == UserRole.ADMIN) {
            item {
                Card(
                    onClick = { onNavigateSection(AppSection.ADMIN_DASHBOARD) },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = RomelRed),
                    modifier = Modifier.fillMaxWidth().testTag("home_admin_dashboard_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = CleanWhite)
                            Column {
                                Text("Admin Dashboard", style = MaterialTheme.typography.titleMedium, color = CleanWhite, fontWeight = FontWeight.Bold)
                                Text("Manage prices, add products, stock & sales charts", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFEE2E4))
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open", tint = CleanWhite)
                    }
                }
            }
        }

        // Categories 3x2 Visual Grid matching reference
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Categories", style = MaterialTheme.typography.titleLarge, color = RomelText)
                TextButton(onClick = { onNavigateSection(AppSection.CATEGORIES) }) {
                    Text("View All >", color = RomelNavy, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                homeCategoryTiles.chunked(3).forEach { rowTiles ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        rowTiles.forEach { tile ->
                            val tileReq = remember(tile.imageFile, context) {
                                ImageRequest.Builder(context)
                                    .data("file:///android_asset/catalog_images/${tile.imageFile}")
                                    .size(Size(240, 240))
                                    .build()
                            }
                            Card(
                                onClick = { onOpenCategoryGroup(tile.title, tile.keywords) },
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = CleanWhite),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.weight(1f).testTag("home_cat_${tile.title}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                ) {
                                    Surface(
                                        color = LightBlueSurface.copy(alpha = 0.65f),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.fillMaxWidth().height(72.dp)
                                    ) {
                                        AsyncImage(
                                            model = tileReq,
                                            contentDescription = tile.title,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize().padding(6.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = tile.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RomelText,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        minLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Model Code Lookup
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Quick Code Lookup", style = MaterialTheme.typography.titleSmall, color = RomelText)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("RL0602", "RL0616", "RL0702", "RL0829-W", "RL2503-W", "RLHD20A", "RK530", "RLGS2003", "RLWT0427").forEach { code ->
                        AssistChip(
                            onClick = { onQuickCodeSearch(code) },
                            label = { Text(code, style = MaterialTheme.typography.labelMedium, color = RomelNavy) }
                        )
                    }
                }
            }
        }

        // Featured Products
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Featured Products", style = MaterialTheme.typography.titleLarge, color = RomelText)
                TextButton(onClick = { onNavigateSection(AppSection.PRODUCTS) }) {
                    Text("Browse All >", color = RomelNavy)
                }
            }
        }

        items(featuredProducts, key = { it.id }) { prod ->
            ProductCard(
                product = prod,
                role = role,
                onClick = { onProductClick(prod) },
                onOpenCalculator = { onOpenCalculator(prod) },
                onEditPrice = { onEditPrice(prod) },
                onAdjustStock = { onAdjustStock(prod) },
                isWishlisted = prod.id in wishlistProductIds,
                onToggleWishlist = { onToggleWishlist(prod) }
            )
        }
        }
    }

    if (showAlertsDialog) {
        AlertDialog(
            onDismissRequest = { showAlertsDialog = false },
            title = { Text("ROMEL Catalog Summary") },
            text = {
                Text("Catalog contains ${metrics.totalProducts} electrical products across ${metrics.totalCategories} categories. Contact sales for pricing on unpriced items.")
            },
            confirmButton = {
                TextButton(onClick = { showAlertsDialog = false }) { Text("OK") }
            }
        )
    }
}

@Composable
fun KpiTile(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = CleanWhite),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ProductsAndSearchScreen(
    isSearchFocused: Boolean,
    role: UserRole,
    query: String,
    suggestions: List<Product>,
    products: List<Product>,
    categories: List<CategoryItem>,
    seriesList: List<String>,
    selectedCategory: String?,
    categoryGroupTitle: String?,
    selectedSeries: String?,
    availabilityFilter: StockAvailabilityFilter,
    minPrice: String,
    maxPrice: String,
    sortOption: ProductSortOption,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onClearCategoryGroup: () -> Unit,
    onSeriesChange: (String?) -> Unit,
    onAvailabilityChange: (StockAvailabilityFilter) -> Unit,
    onPriceRangeChange: (String, String) -> Unit,
    onSortChange: (ProductSortOption) -> Unit,
    onClearFilters: () -> Unit,
    onProductClick: (Product) -> Unit,
    wishlistProductIds: Set<String> = emptySet(),
    onToggleWishlist: (Product) -> Unit = {},
    onOpenCalculator: (Product) -> Unit,
    onEditPrice: (Product) -> Unit,
    onAdjustStock: (Product) -> Unit
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CleanWhite)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        if (isSearchFocused) "Enter code (RL0602, RK530, RLWT0427), name, specs..."
                        else "Filter catalog by code, name, specs..."
                    )
                },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = RomelNavy) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_search_input")
            )

            if (query.isNotBlank() && suggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.forEach { sug ->
                        AssistChip(
                            onClick = { onProductClick(sug) },
                            label = {
                                Text(
                                    "${sug.code} • ${sug.name.take(22)}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                OutlinedButton(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier.height(48.dp).testTag("open_filters_button")
                ) {
                    Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Filters")
                }

                Box {
                    OutlinedButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.height(48.dp).testTag("open_sort_button")
                    ) {
                        Icon(Icons.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(sortOption.label)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        ProductSortOption.entries.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label) },
                                onClick = {
                                    onSortChange(opt)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                if (categoryGroupTitle != null) {
                    FilterChip(
                        selected = true,
                        onClick = onClearCategoryGroup,
                        label = { Text(categoryGroupTitle) },
                        trailingIcon = { Icon(Icons.Filled.Clear, contentDescription = "Remove", modifier = Modifier.size(16.dp)) }
                    )
                }
                if (selectedCategory != null) {
                    FilterChip(
                        selected = true,
                        onClick = { onCategoryChange(null) },
                        label = { Text(selectedCategory) },
                        trailingIcon = { Icon(Icons.Filled.Clear, contentDescription = "Remove", modifier = Modifier.size(16.dp)) }
                    )
                }
                if (availabilityFilter != StockAvailabilityFilter.ALL) {
                    FilterChip(
                        selected = true,
                        onClick = { onAvailabilityChange(StockAvailabilityFilter.ALL) },
                        label = { Text(availabilityFilter.label) },
                        trailingIcon = { Icon(Icons.Filled.Clear, contentDescription = "Remove", modifier = Modifier.size(16.dp)) }
                    )
                }
                if (selectedCategory != null || categoryGroupTitle != null || selectedSeries != null || query.isNotBlank() || availabilityFilter != StockAvailabilityFilter.ALL || minPrice.isNotBlank() || maxPrice.isNotBlank()) {
                    TextButton(onClick = onClearFilters) {
                        Text("Reset", color = RomelRed)
                    }
                }
            }

            Text(
                text = "${products.size} matching products",
                style = MaterialTheme.typography.labelSmall,
                color = SlateMuted
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (products.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(48.dp), tint = SlateMuted)
                    Spacer(Modifier.height(10.dp))
                    Text("No products match your search", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Try searching a partial code like RL06, RL08, RK530, or clear filters.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onClearFilters,
                        colors = ButtonDefaults.buttonColors(containerColor = RomelRed)
                    ) { Text("Clear Filters") }
                }
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val twoColumns = maxWidth >= 680.dp
                val rows = if (twoColumns) products.chunked(2) else products.map { listOf(it) }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(rows, key = { chunk -> chunk.joinToString("_") { it.id } }) { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowItems.forEach { prod ->
                                ProductCard(
                                    product = prod,
                                    role = role,
                                    onClick = { onProductClick(prod) },
                                    onOpenCalculator = { onOpenCalculator(prod) },
                                    onEditPrice = { onEditPrice(prod) },
                                    onAdjustStock = { onAdjustStock(prod) },
                                    isWishlisted = prod.id in wishlistProductIds,
                                    onToggleWishlist = { onToggleWishlist(prod) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (twoColumns && rowItems.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        var minInput by remember { mutableStateOf(minPrice) }
        var maxInput by remember { mutableStateOf(maxPrice) }
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Catalog") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Availability", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StockAvailabilityFilter.entries.forEach { f ->
                            FilterChip(
                                selected = availabilityFilter == f,
                                onClick = { onAvailabilityChange(f) },
                                label = { Text(f.label) }
                            )
                        }
                    }

                    Text("Category", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { onCategoryChange(null) },
                            label = { Text("All Categories") }
                        )
                        categories.forEach { c ->
                            FilterChip(
                                selected = selectedCategory == c.name,
                                onClick = { onCategoryChange(c.name) },
                                label = { Text(c.name) }
                            )
                        }
                    }

                    Text("Series", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedSeries == null,
                            onClick = { onSeriesChange(null) },
                            label = { Text("All Series") }
                        )
                        seriesList.forEach { s ->
                            FilterChip(
                                selected = selectedSeries == s,
                                onClick = { onSeriesChange(s) },
                                label = { Text(s) }
                            )
                        }
                    }

                    Text("Price Range (ETB)", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minInput,
                            onValueChange = { minInput = it },
                            label = { Text("Min ETB") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxInput,
                            onValueChange = { maxInput = it },
                            label = { Text("Max ETB") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPriceRangeChange(minInput, maxInput)
                        showFilterDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RomelRed)
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onClearFilters()
                    showFilterDialog = false
                }) { Text("Reset All") }
            }
        )
    }
}

@Composable
fun CategoriesScreen(
    role: UserRole,
    categories: List<CategoryItem>,
    products: List<Product>,
    onSelectCategory: (String) -> Unit,
    onCreateCategory: (name: String, desc: String) -> Unit,
    onUpdateCategory: (category: CategoryItem, newName: String, newDesc: String) -> Unit = { _, _, _ -> },
    onDeleteCategory: (category: CategoryItem) -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryItem?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (role == UserRole.ADMIN) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${categories.size} Product Categories", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RomelRed),
                    modifier = Modifier.height(48.dp).testTag("add_category_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Category")
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(categories, key = { it.id }) { cat ->
                val count = products.count { it.category.equals(cat.name, ignoreCase = true) }
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = CleanWhite),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCategory(cat.name) }
                        .testTag("category_card_${cat.sortOrder}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = LightBlueSurface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${cat.sortOrder}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = RomelNavy,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.name, style = MaterialTheme.typography.titleMedium, color = RomelText)
                            if (cat.description.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = cat.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "$count items",
                                style = MaterialTheme.typography.labelSmall,
                                color = RomelRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (role == UserRole.ADMIN) {
                            IconButton(
                                onClick = { editingCategory = cat },
                                modifier = Modifier.size(48.dp).testTag("edit_category_${cat.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit ${cat.name}",
                                    tint = RomelNavy
                                )
                            }
                            IconButton(
                                onClick = { deletingCategory = cat },
                                modifier = Modifier.size(48.dp).testTag("delete_category_${cat.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete ${cat.name}",
                                    tint = RomelRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Product Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_category_name_input")
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth().testTag("new_category_desc_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = RomelRed),
                    onClick = {
                        onCreateCategory(name, desc)
                        showAddDialog = false
                    },
                    modifier = Modifier.testTag("confirm_create_category_button")
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    editingCategory?.let { cat ->
        var editName by remember(cat.id) { mutableStateOf(cat.name) }
        var editDesc by remember(cat.id) { mutableStateOf(cat.description) }
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Edit Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_category_name_input")
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_category_desc_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = editName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = RomelNavy),
                    onClick = {
                        onUpdateCategory(cat, editName, editDesc)
                        editingCategory = null
                    },
                    modifier = Modifier.testTag("confirm_edit_category_button")
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) { Text("Cancel") }
            }
        )
    }

    deletingCategory?.let { cat ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Delete Category?") },
            text = {
                Text("Remove category \"${cat.name}\" from the cloud catalog? Existing products remain intact and can be reassigned.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = RomelRed),
                    onClick = {
                        onDeleteCategory(cat)
                        deletingCategory = null
                    },
                    modifier = Modifier.testTag("confirm_delete_category_button")
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProductDetailScreen(
    product: Product,
    similarProducts: List<Product>,
    role: UserRole,
    isWishlisted: Boolean = false,
    onToggleWishlist: () -> Unit = {},
    onBack: () -> Unit,
    onAddDirectToCart: (unit: String, multiplier: Long, unitPrice: Double, quantity: Long) -> Unit,
    onOpenCalculator: () -> Unit,
    onEditPrices: () -> Unit,
    onAdjustStock: () -> Unit,
    onEditProduct: () -> Unit,
    onSelectSimilar: (Product) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var quantity by remember(product.id) { mutableLongStateOf(1L) }
    var selectedUnit by remember(product.id) { mutableStateOf(UnitOfMeasure.fromRaw(product.unit)) }
    val multiplier = when (selectedUnit) {
        UnitOfMeasure.BOX -> product.piecesPerBox.coerceAtLeast(1L)
        UnitOfMeasure.CARTON -> product.piecesPerCarton.coerceAtLeast(1L)
        else -> 1L
    }
    val unitPrice = (product.effectiveUnitPrice ?: 0.0) * multiplier

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Inline Back & Share/Admin Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("detail_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RomelNavy)
                }
                Text(product.code, style = MaterialTheme.typography.titleMedium, color = RomelNavy, fontWeight = FontWeight.Bold)
            }
            Row {
                IconButton(
                    onClick = onToggleWishlist,
                    modifier = Modifier.size(48.dp).testTag("detail_wishlist_button")
                ) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isWishlisted) "Remove from wishlist" else "Save to wishlist",
                        tint = if (isWishlisted) RomelRed else RomelNavy
                    )
                }
                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "ROMEL Electrical Equipment\n${product.name} (${product.code})\nCategory: ${product.category}\nPrice: ${formatEtb(product.effectiveUnitPrice)}\nSpecs: ${product.specifications}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share ${product.code}"))
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share product", tint = RomelNavy)
                }
                if (role == UserRole.ADMIN) {
                    IconButton(onClick = onEditProduct, modifier = Modifier.size(48.dp).testTag("detail_edit_product")) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit product", tint = RomelNavy)
                    }
                }
            }
        }

        // Large White Product Image Showcase matching reference
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = CleanWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ProductImage(
                product = product,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(235.dp)
                    .padding(10.dp)
            )
        }

        // Product Title & Series
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(product.name, style = MaterialTheme.typography.headlineSmall, color = RomelText, fontWeight = FontWeight.Bold)
            Text("${product.code}  •  ${product.series} (Page ${product.catalogPage})", style = MaterialTheme.typography.bodyMedium, color = SlateMuted)
            Text(product.category, style = MaterialTheme.typography.labelLarge, color = RomelNavy)
        }

        // Prominent Red Price + Green Stock Pill matching reference Product Details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatEtb(product.effectiveUnitPrice),
                style = MaterialTheme.typography.headlineMedium,
                color = if (product.effectiveUnitPrice != null) RomelRed else SlateMuted,
                fontWeight = FontWeight.Bold
            )
            StockBadge(product)
        }

        if (product.needsReview) {
            Surface(
                color = StatusAmberSoft,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = "Review needed", tint = StatusAmber)
                    Text(
                        text = product.reviewNote ?: "Flagged for administrator verification against physical catalog.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RomelNavy
                    )
                }
            }
        }

        // Description & Bulleted Technical Specifications matching reference
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = CleanWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Description", style = MaterialTheme.typography.titleMedium, color = RomelText)
                Text(
                    text = product.description.ifBlank { "ROMEL 2025 commercial catalog item." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateMuted
                )
                if (product.specifications.isNotBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Technical Specifications", style = MaterialTheme.typography.titleMedium, color = RomelText)
                    product.specifications.split("|").map { it.trim() }.filter { it.isNotBlank() }.forEach { specLine ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("•", color = RomelRed, fontWeight = FontWeight.Bold)
                            Text(specLine, style = MaterialTheme.typography.bodySmall, color = RomelText)
                        }
                    }
                }
            }
        }

        // Full ETB Price Tiers & Packaging Card
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = CleanWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pricing Tiers & Packaging", style = MaterialTheme.typography.titleMedium)
                    if (role == UserRole.ADMIN) {
                        Row {
                            TextButton(onClick = onEditPrices) {
                                Icon(Icons.Filled.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Prices")
                            }
                            TextButton(onClick = onAdjustStock) {
                                Icon(Icons.Filled.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Stock")
                            }
                        }
                    }
                }
                PriceRow("Unit Selling Price", formatEtb(product.sellingPrice))
                PriceRow("Wholesale Price", formatEtb(product.wholesalePrice))
                PriceRow("Retail Price", formatEtb(product.retailPrice))
                PriceRow("Discount Price", formatEtb(product.discountPrice))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                PriceRow("Packaging", "1 Box = ${product.piecesPerBox} PCS  •  1 Carton = ${product.piecesPerCarton} PCS")
                PriceRow("Available Stock", product.stockQuantity?.let { "$it ${product.unit}" } ?: "Stock not set")
            }
        }

        // Quantity Stepper + Unit Selector + Add to Cart matching reference Product Details
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = CleanWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quantity", style = MaterialTheme.typography.titleMedium, color = RomelText)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = { if (quantity > 1L) quantity-- },
                            shape = RoundedCornerShape(10.dp),
                            color = LightBlueSurface,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", tint = RomelNavy)
                            }
                        }
                        Text(
                            text = "$quantity",
                            style = MaterialTheme.typography.titleLarge,
                            color = RomelText,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Surface(
                            onClick = { quantity++ },
                            shape = RoundedCornerShape(10.dp),
                            color = LightBlueSurface,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase quantity", tint = RomelNavy)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(UnitOfMeasure.PCS, UnitOfMeasure.BOX, UnitOfMeasure.CARTON, UnitOfMeasure.SET, UnitOfMeasure.PACK).forEach { u ->
                        FilterChip(
                            selected = selectedUnit == u,
                            onClick = { selectedUnit = u },
                            label = { Text(u.label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Estimated Subtotal", style = MaterialTheme.typography.bodySmall, color = SlateMuted)
                    Text(
                        text = if (product.effectiveUnitPrice != null) formatEtb(unitPrice * quantity) else "Price not set",
                        style = MaterialTheme.typography.titleMedium,
                        color = RomelNavy,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { onAddDirectToCart(selectedUnit.label, multiplier, unitPrice, quantity) },
                    colors = ButtonDefaults.buttonColors(containerColor = RomelRed, contentColor = CleanWhite),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("detail_add_to_cart_button")
                ) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add to Cart", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenCalculator,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("detail_calculate_button")
                ) {
                    Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open Custom Price & Discount Calculator")
                }
            }
        }

        if (similarProducts.isNotEmpty()) {
            Text("More in ${product.category}", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(similarProducts, key = { it.id }) { sim ->
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = CleanWhite),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .width(170.dp)
                            .clickable { onSelectSimilar(sim) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            ProductImage(sim, modifier = Modifier.fillMaxWidth().height(90.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(sim.code, style = MaterialTheme.typography.labelMedium, color = RomelNavy)
                            Text(sim.name, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = formatEtb(sim.effectiveUnitPrice),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (sim.effectiveUnitPrice != null) RomelRed else SlateMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = SlateMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = RomelText, fontWeight = FontWeight.Medium)
    }
}
