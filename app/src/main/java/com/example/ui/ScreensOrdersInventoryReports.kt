package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.data.PdfQuotationExporter
import com.example.model.MovementType
import com.example.model.OrderLineItem
import com.example.model.OrderStatus
import com.example.model.OrderType
import com.example.model.Product
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
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusEmeraldSoft
import com.example.ui.theme.StatusRose
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MyOrdersScreen(
    role: UserRole,
    cartItems: List<OrderLineItem>,
    customerName: String,
    customerPhone: String,
    orderType: OrderType,
    orderDiscount: String,
    orderNotes: String,
    allowAdminBackorder: Boolean,
    orders: List<SalesOrder>,
    onCustomerNameChange: (String) -> Unit,
    onCustomerPhoneChange: (String) -> Unit,
    onOrderTypeChange: (OrderType) -> Unit,
    onOrderDiscountChange: (String) -> Unit,
    onOrderNotesChange: (String) -> Unit,
    onBackorderToggle: (Boolean) -> Unit,
    onUpdateItem: (index: Int, unitPrice: Double?, quantity: Long?) -> Unit,
    onRemoveItem: (index: Int) -> Unit,
    onClearCart: () -> Unit,
    onSubmitOrder: (confirmSale: Boolean) -> Unit,
    onBuildPreviewOrder: () -> SalesOrder,
    onBrowseCatalog: () -> Unit,
    onOpenOrderHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val canEditPrice = role == UserRole.ADMIN || role == UserRole.SALES_REP
    val subtotal = cartItems.sumOf { it.lineTotal }
    val discountVal = (orderDiscount.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val finalTotal = (subtotal - discountVal).coerceAtLeast(0.0)
    val totalQty = cartItems.sumOf { it.quantity }
    var previewOrder by remember { mutableStateOf<SalesOrder?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            val (roleTitle, roleSubtitle, badgeBg, badgeFg) = when (role) {
                UserRole.ADMIN -> listOf(
                    "Admin Order & Stock Control",
                    "Full access to pricing, quotes, sales confirmation & stock deduction",
                    StatusEmeraldSoft,
                    StatusEmerald
                )
                UserRole.SALES_REP -> listOf(
                    "Sales Quotation Mode",
                    "Create quotations, confirm sales orders & deduct stock",
                    LightBlueSurface,
                    RomelNavy
                )
                UserRole.VIEWER -> listOf(
                    "Order & Quotation Builder",
                    "Build your product list, save quotations, and export or share PDFs anytime",
                    LightBlueSurface,
                    RomelNavy
                )
            }
            Surface(
                color = badgeBg as Color,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = roleTitle as String,
                        style = MaterialTheme.typography.labelLarge,
                        color = badgeFg as Color,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = roleSubtitle as String,
                        style = MaterialTheme.typography.bodySmall,
                        color = RomelText
                    )
                }
            }
        }

        if (cartItems.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = CleanWhite),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(44.dp), tint = RomelNavy)
                        Spacer(Modifier.height(10.dp))
                        Text("Your order list is empty", style = MaterialTheme.typography.titleMedium, color = RomelText)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Select any product to set quantity and add it to an order or quotation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onBrowseCatalog,
                            colors = ButtonDefaults.buttonColors(containerColor = RomelRed, contentColor = CleanWhite),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Browse Catalog")
                        }
                    }
                }
            }
        } else {
            // Item cards matching reference My Order screen
            itemsIndexed(cartItems, key = { idx, item -> "${item.productId}_${item.unit}_$idx" }) { index, item ->
                var priceInput by remember(item.unitPrice) {
                    mutableStateOf(String.format(Locale.US, "%.2f", item.unitPrice))
                }
                val thumbModel = remember(item.customImageUri, item.imageAsset, context) {
                    val src = item.customImageUri?.takeIf { it.isNotBlank() }
                        ?: item.imageAsset.takeIf { it.isNotBlank() }?.let { "file:///android_asset/$it" }
                    src?.let { ImageRequest.Builder(context).data(it).size(Size(180, 180)).build() }
                }

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = CleanWhite),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = LightBlueSurface,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.size(56.dp)
                            ) {
                                if (thumbModel != null) {
                                    AsyncImage(
                                        model = thumbModel,
                                        contentDescription = item.name,
                                        modifier = Modifier.fillMaxSize().padding(4.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.code, style = MaterialTheme.typography.labelLarge, color = RomelNavy, fontWeight = FontWeight.Bold)
                                Text(item.name, style = MaterialTheme.typography.bodySmall, color = RomelText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${formatEtb(item.unitPrice)} / ${item.unit} • ${item.totalPieces} PCS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RomelRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            // Quantity stepper - qty +
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (item.quantity > 1) onUpdateItem(index, null, item.quantity - 1) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = RomelNavy)
                                }
                                Text(
                                    text = "${item.quantity}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { onUpdateItem(index, null, item.quantity + 1) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase", tint = RomelNavy)
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = priceInput,
                                onValueChange = { raw ->
                                    if (canEditPrice) {
                                        priceInput = raw
                                        raw.toDoubleOrNull()?.let { onUpdateItem(index, it, null) }
                                    }
                                },
                                enabled = canEditPrice,
                                label = { Text("Unit ETB") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatEtb(item.lineTotal),
                                style = MaterialTheme.typography.titleMedium,
                                color = RomelNavy,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { onRemoveItem(index) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove ${item.code}", tint = StatusRose)
                            }
                        }
                    }
                }
            }

            // Customer & Summary Card matching reference
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = CleanWhite),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Customer & Order Summary", style = MaterialTheme.typography.titleMedium, color = RomelText)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OrderType.entries.forEach { t ->
                                FilterChip(
                                    selected = orderType == t,
                                    onClick = { onOrderTypeChange(t) },
                                    label = { Text(t.label) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = onCustomerNameChange,
                            label = { Text("Customer / Company Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("order_customer_name")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customerPhone,
                                onValueChange = onCustomerPhoneChange,
                                label = { Text("Phone (+251...)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = orderDiscount,
                                onValueChange = { if (canEditPrice) onOrderDiscountChange(it) },
                                enabled = canEditPrice,
                                label = { Text("Discount ETB") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = orderNotes,
                            onValueChange = onOrderNotesChange,
                            label = { Text("Order Notes / Delivery Info") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (role == UserRole.ADMIN) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = allowAdminBackorder, onCheckedChange = onBackorderToggle)
                                Text("Admin Override: Allow backorder if stock is low", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        HorizontalDivider()

                        SummaryLine("Total Products", "${cartItems.size}")
                        SummaryLine("Total Quantity", "$totalQty")
                        SummaryLine("Subtotal", formatEtb(subtotal))
                        SummaryLine("Discount", "- ${formatEtb(discountVal)}")

                        // Full-width Navy Total Amount Banner matching reference
                        Surface(
                            color = RomelNavy,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Amount", style = MaterialTheme.typography.titleMedium, color = CleanWhite)
                                Text(
                                    text = formatEtb(finalTotal),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = CleanWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Save Order (outlined Navy) + Generate Quotation (solid Red) matching reference
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { onSubmitOrder(false) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, RomelNavy),
                                modifier = Modifier.weight(1f).height(50.dp).testTag("save_quotation_button")
                            ) {
                                Text("Save Order", color = RomelNavy, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { previewOrder = onBuildPreviewOrder() },
                                colors = ButtonDefaults.buttonColors(containerColor = RomelRed, contentColor = CleanWhite),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(50.dp).testTag("generate_quotation_button")
                            ) {
                                Text("Generate Quotation", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        if (canEditPrice) {
                            Button(
                                onClick = { onSubmitOrder(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = RomelNavy, contentColor = CleanWhite),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("confirm_sale_button")
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Confirm Sale & Deduct Stock", fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { PdfQuotationExporter.sharePdf(context, onBuildPreviewOrder(), preferWhatsApp = false) },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("export_pdf_button")
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("PDF")
                            }
                            OutlinedButton(
                                onClick = { PdfQuotationExporter.printOrder(context, onBuildPreviewOrder()) },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("print_order_button")
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Print")
                            }
                            OutlinedButton(
                                onClick = { PdfQuotationExporter.sharePdf(context, onBuildPreviewOrder(), preferWhatsApp = true) },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("whatsapp_share_button")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("WhatsApp")
                            }
                        }

                        OutlinedButton(
                            onClick = onClearCart,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Clear Current List", color = StatusRose)
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Orders & Quotations (${orders.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = RomelText,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onOpenOrderHistory,
                    modifier = Modifier.height(48.dp).testTag("open_order_history_button")
                ) {
                    Text("Order History")
                }
            }
        }

        items(orders, key = { it.id }) { order ->
            val dateFmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US) }
            val isConfirmed = order.status == OrderStatus.CONFIRMED.name
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = CleanWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(order.orderNumber, style = MaterialTheme.typography.titleMedium, color = RomelNavy, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${order.customerName.ifBlank { "Walk-in Customer" }} • ${order.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "Just now"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateMuted
                            )
                        }
                        Surface(
                            color = if (isConfirmed) StatusEmeraldSoft else StatusAmberSoft,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = order.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isConfirmed) StatusEmerald else StatusAmber,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${order.totalProducts} products (${order.totalQuantity} units)", style = MaterialTheme.typography.bodySmall)
                        Text(formatEtb(order.totalAmount), style = MaterialTheme.typography.titleSmall, color = RomelRed, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { previewOrder = order },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Preview")
                        }
                        OutlinedButton(
                            onClick = { PdfQuotationExporter.sharePdf(context, order, preferWhatsApp = false) },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PDF")
                        }
                        OutlinedButton(
                            onClick = { PdfQuotationExporter.printOrder(context, order) },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Print")
                        }
                    }
                }
            }
        }
    }

    previewOrder?.let { ord ->
        QuotationPreviewDialog(
            order = ord,
            onDismiss = { previewOrder = null },
            onExportPdf = { PdfQuotationExporter.sharePdf(context, ord, preferWhatsApp = false) },
            onPrint = { PdfQuotationExporter.printOrder(context, ord) },
            onShareWhatsApp = { PdfQuotationExporter.sharePdf(context, ord, preferWhatsApp = true) }
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = SlateMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = RomelText, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun InventoryScreen(
    products: List<Product>,
    movements: List<StockMovement>,
    onAddProduct: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onEditPrices: (Product) -> Unit,
    onAdjustStock: (Product) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    val tabs = listOf("All Stock", "Low & Out", "Needs Review", "History")

    val filtered = remember(products, selectedTab, search) {
        val base = when (selectedTab) {
            1 -> products.filter { it.stockIsLow || it.stockIsOut }
            2 -> products.filter { it.needsReview }
            else -> products
        }
        val q = search.trim()
        if (q.isBlank()) base else base.filter { MainViewModel.matchScore(it, q) > 0 }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Inventory Control", style = MaterialTheme.typography.titleLarge, color = RomelText)
            Button(
                onClick = onAddProduct,
                colors = ButtonDefaults.buttonColors(containerColor = RomelRed, contentColor = CleanWhite),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp).testTag("inventory_add_product_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Product")
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }

        if (selectedTab != 3) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Filter inventory by code or name...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ProductImage(prod, modifier = Modifier.size(56.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${prod.code} • ${prod.name}", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = formatEtb(prod.effectiveUnitPrice),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (prod.effectiveUnitPrice != null) RomelRed else SlateMuted,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    StockBadge(prod)
                                }
                            }
                            if (prod.needsReview) {
                                Text(
                                    text = prod.reviewNote ?: "Flagged for Admin review",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusAmber
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { onEditPrices(prod) },
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(Icons.Filled.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Price")
                                }
                                OutlinedButton(
                                    onClick = { onAdjustStock(prod) },
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(Icons.Filled.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Stock")
                                }
                                OutlinedButton(
                                    onClick = { onEditProduct(prod) },
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Edit")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val fmt = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.US) }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (movements.isEmpty()) {
                    item {
                        Text(
                            "No stock or price movements recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateMuted
                        )
                    }
                }
                items(movements, key = { it.id }) { mov ->
                    val type = MovementType.fromRaw(mov.movementType)
                    val accent = when (type) {
                        MovementType.STOCK_IN, MovementType.IMPORT -> StatusEmerald
                        MovementType.STOCK_OUT, MovementType.SALE -> StatusRose
                        MovementType.PRICE_UPDATE -> StatusBlue
                    }
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = CleanWhite),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${mov.productCode} • ${type.label}", style = MaterialTheme.typography.titleSmall, color = accent)
                                Text(mov.productName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${mov.reason} • By ${mov.performedByName} • ${mov.createdAt?.toDate()?.let { fmt.format(it) } ?: "Now"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateMuted
                                )
                            }
                            if (mov.movementType == MovementType.PRICE_UPDATE.name) {
                                Text(
                                    text = mov.newPrice?.let { formatEtb(it) } ?: "Price cleared",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                val sign = if (mov.quantityDelta >= 0) "+" else ""
                                Text(
                                    text = "$sign${mov.quantityDelta}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = accent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsScreen(metrics: DashboardMetrics) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Business & Sales Analytics", style = MaterialTheme.typography.headlineSmall, color = RomelText)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiTile("Today's Sales", formatEtb(metrics.todaysSalesEtb), StatusEmerald, Modifier.weight(1f))
                KpiTile("Monthly Sales", formatEtb(metrics.monthlySalesEtb), RomelNavy, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiTile("Total Orders", "${metrics.totalOrders} (${metrics.confirmedOrdersCount} sold)", RomelNavy, Modifier.weight(1f))
                KpiTile("Inventory Value", formatEtb(metrics.totalInventoryValueEtb), StatusEmerald, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiTile("Registered SKUs", "${metrics.totalProducts}", RomelNavy, Modifier.weight(1f))
                KpiTile("Low Stock", "${metrics.lowStockCount}", StatusAmber, Modifier.weight(1f))
                KpiTile("Out of Stock", "${metrics.outOfStockCount}", StatusRose, Modifier.weight(1f))
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = CleanWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Best-Selling Products (Units Sold)", style = MaterialTheme.typography.titleMedium)
                    if (metrics.bestSellers.isEmpty()) {
                        Text(
                            "Confirm sales orders to populate best-selling product analytics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                    } else {
                        SimpleBarChart(
                            items = metrics.bestSellers.map { "${it.code} • ${it.name}" to it.totalUnitsSold.toFloat() },
                            barColor = RomelRed,
                            valueFormatter = { "${it.toLong()} PCS" }
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = CleanWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Catalog Distribution by Category (Top 10)", style = MaterialTheme.typography.titleMedium)
                    SimpleBarChart(
                        items = metrics.categoryStats
                            .sortedByDescending { it.productCount }
                            .take(10)
                            .map { it.name to it.productCount.toFloat() },
                        barColor = RomelNavy,
                        valueFormatter = { "${it.toInt()} items" }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    adminSession: AdminSessionState,
    isImporting: Boolean,
    onOpenAdminLogin: () -> Unit,
    onLogoutAdmin: () -> Unit,
    onSyncCatalog: () -> Unit,
    onImportCsv: (Uri) -> Unit,
    onImportPdf: (Uri) -> Unit,
    onOpenAdminDashboard: () -> Unit = {},
    onOpenAllProducts: () -> Unit = {},
    onOpenAddProduct: () -> Unit = {},
    onOpenPriceManagement: () -> Unit = {},
    onOpenInventory: () -> Unit = {},
    onOpenReports: () -> Unit = {},
    onOpenCategories: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenOrderHistory: () -> Unit = {}
) {
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportCsv(uri)
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportPdf(uri)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, color = RomelText)
        }

        // Admin Access Card (Settings Only — hidden from customer launch flow)
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = CleanWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!adminSession.isAdminLoggedIn) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = RomelNavy,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Administrator Access", style = MaterialTheme.typography.titleMedium, color = RomelText)
                                Text(
                                    "Store administrators can sign in here to manage products, prices, categories, and availability.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateMuted
                                )
                            }
                        }
                        Button(
                            onClick = onOpenAdminLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = RomelNavy, contentColor = CleanWhite),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("settings_admin_login_button")
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Admin Login", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Administrator Session", style = MaterialTheme.typography.titleMedium, color = RomelText)
                                Text(
                                    text = "Signed in as ${adminSession.adminUsername ?: "Administrator"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateMuted
                                )
                            }
                            Surface(color = StatusEmeraldSoft, shape = RoundedCornerShape(999.dp)) {
                                Text(
                                    text = "Admin Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusEmerald,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Button(
                            onClick = onOpenAdminDashboard,
                            colors = ButtonDefaults.buttonColors(containerColor = RomelRed, contentColor = CleanWhite),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("settings_open_admin_dashboard")
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open Admin Dashboard", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onLogoutAdmin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("admin_logout_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp), tint = RomelRed)
                            Spacer(Modifier.width(8.dp))
                            Text("Sign Out of Admin", color = RomelRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (adminSession.isAdminLoggedIn) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = CleanWhite),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Admin Workspace", style = MaterialTheme.typography.titleMedium, color = RomelRed)
                        Text(
                            "Restricted catalog management tools verified by Firestore security rules.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = onOpenAdminDashboard,
                                colors = ButtonDefaults.buttonColors(containerColor = RomelRed),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("settings_admin_dashboard")
                            ) { Text("Dashboard", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            OutlinedButton(
                                onClick = onOpenAllProducts,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("settings_all_products")
                            ) { Text("All Products", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onOpenAddProduct,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("settings_add_product")
                            ) { Text("Add Product", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            OutlinedButton(
                                onClick = onOpenCategories,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("settings_categories_admin")
                            ) { Text("Categories", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onOpenPriceManagement,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("settings_price_mgmt")
                            ) { Text("Prices", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            OutlinedButton(
                                onClick = onOpenInventory,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("settings_inventory")
                            ) { Text("Inventory", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            OutlinedButton(
                                onClick = onOpenReports,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("settings_reports")
                            ) { Text("Reports", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = CleanWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Catalog Quick Access", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onOpenCategories,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("All Categories", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        OutlinedButton(
                            onClick = onOpenSearch,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Product Search", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    OutlinedButton(
                        onClick = onOpenOrderHistory,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_order_history")
                    ) { Text("View My Order History") }
                }
            }
        }

        if (adminSession.isAdminLoggedIn) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = CleanWhite),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Product Import & Cloud Catalog", style = MaterialTheme.typography.titleMedium)
                        if (isImporting) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("Processing catalog import...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Button(
                            onClick = onSyncCatalog,
                            enabled = !isImporting,
                            colors = ButtonDefaults.buttonColors(containerColor = RomelNavy),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sync_catalog_button")
                        ) {
                            Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sync 2025 ROMEL Catalog to Cloud")
                        }
                        OutlinedButton(
                            onClick = { csvLauncher.launch(arrayOf("text/*", "text/csv", "application/vnd.ms-excel")) },
                            enabled = !isImporting,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("import_csv_button")
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Import Products from CSV / Sheet")
                        }
                        OutlinedButton(
                            onClick = { pdfLauncher.launch(arrayOf("application/pdf")) },
                            enabled = !isImporting,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("import_pdf_ai_button")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp), tint = RomelRed)
                            Spacer(Modifier.width(8.dp))
                            Text("Import Additional PDF Catalog (Gemini AI)")
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = LightBlueSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("ROMEL Electrical Equipment", style = MaterialTheme.typography.titleSmall, color = RomelNavy)
                    Text("Urael, ROMEL Building, Addis Ababa, Ethiopia", style = MaterialTheme.typography.bodySmall)
                    Text("Tel: +251 911 523 493 / +251 115 620 212", style = MaterialTheme.typography.bodySmall)
                    Text("Web: www.romelelectrical.com • romel@romelelectrical.com", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
