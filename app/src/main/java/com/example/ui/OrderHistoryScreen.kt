package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PdfQuotationExporter
import com.example.model.OrderStatus
import com.example.model.OrderType
import com.example.model.SalesOrder
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
import com.example.ui.theme.StatusBlueSoft
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusEmeraldSoft
import com.example.ui.theme.StatusRose
import com.example.ui.theme.StatusRoseSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val ORDER_SEARCH_DATE_PATTERNS = listOf(
    "dd MMM yyyy, HH:mm",
    "dd MMM yyyy",
    "d MMM yyyy",
    "MMM dd yyyy",
    "MMM d, yyyy",
    "MMMM yyyy",
    "MMMM dd, yyyy",
    "yyyy-MM-dd",
    "dd/MM/yyyy",
    "MM/dd/yyyy"
)

internal fun formatOrderSearchDates(
    date: Date,
    timeZone: TimeZone = TimeZone.getDefault()
): List<String> = ORDER_SEARCH_DATE_PATTERNS.map { pattern ->
    SimpleDateFormat(pattern, Locale.US).apply {
        this.timeZone = timeZone
    }.format(date).lowercase(Locale.US)
}

fun filterOrderHistory(
    orders: List<SalesOrder>,
    query: String,
    selectedStatus: OrderStatus? = null,
    timeZone: TimeZone = TimeZone.getDefault()
): List<SalesOrder> {
    val q = query.trim().lowercase(Locale.US)
    return orders.filter { ord ->
        val statusMatches = selectedStatus == null || ord.status == selectedStatus.name
        val queryMatches = if (q.isEmpty()) {
            true
        } else {
            val dateMatch = listOfNotNull(ord.createdAt?.toDate(), ord.updatedAt?.toDate())
                .any { date ->
                    formatOrderSearchDates(date, timeZone).any { formatted ->
                        formatted.contains(q)
                    }
                }

            ord.id.lowercase(Locale.US).contains(q) ||
                ord.orderNumber.lowercase(Locale.US).contains(q) ||
                ord.customerName.lowercase(Locale.US).contains(q) ||
                ord.status.lowercase(Locale.US).contains(q) ||
                ord.orderType.lowercase(Locale.US).contains(q) ||
                dateMatch ||
                ord.parsedItems().any { item ->
                    item.code.lowercase(Locale.US).contains(q) ||
                        item.name.lowercase(Locale.US).contains(q)
                }
        }
        statusMatches && queryMatches
    }
}

@Composable
fun OrderHistoryScreen(
    orders: List<SalesOrder>,
    onBrowseCatalog: () -> Unit
) {
    val context = LocalContext.current
    var selectedStatus by remember { mutableStateOf<OrderStatus?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var previewOrder by remember { mutableStateOf<SalesOrder?>(null) }
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US) }

    val confirmedTotal = remember(orders) {
        orders.filter { it.status == OrderStatus.CONFIRMED.name }.sumOf { it.totalAmount }
    }
    val totalAllOrdersEtb = remember(orders) {
        orders.sumOf { it.totalAmount }
    }

    val filteredOrders = remember(orders, selectedStatus, searchQuery) {
        filterOrderHistory(
            orders = orders,
            query = searchQuery,
            selectedStatus = selectedStatus
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top search bar, KPI summary & status filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CleanWhite)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search by order ID or date (e.g. SO-1002, 19 Nov 2026)...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search orders by ID or date", tint = RomelNavy)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.testTag("order_history_search_clear")
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
                    .testTag("order_history_search_input")
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                KpiTile(
                    title = "Total Orders",
                    value = "${orders.size}",
                    accent = RomelNavy,
                    modifier = Modifier.weight(1f)
                )
                KpiTile(
                    title = "Confirmed Purchases",
                    value = formatEtb(confirmedTotal),
                    accent = StatusEmerald,
                    modifier = Modifier.weight(1f)
                )
            }

            if (orders.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { selectedStatus = null },
                        label = { Text("All (${orders.size})") }
                    )
                    OrderStatus.entries.forEach { st ->
                        val count = orders.count { it.status == st.name }
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = if (selectedStatus == st) null else st },
                            label = { Text("${st.label} ($count)") },
                            modifier = Modifier.testTag("order_history_filter_${st.name.lowercase()}")
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = CleanWhite),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = LightBlueSurface,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = RomelNavy,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = "No Past Purchases Yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = RomelNavy,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your saved quotations and confirmed ROMEL orders from Firestore will appear here with status and ETB total cost.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onBrowseCatalog,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RomelRed,
                                contentColor = CleanWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("order_history_browse_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Browse Catalog", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = SlateMuted
                    )
                    Text(
                        text = "No orders match the current filter",
                        style = MaterialTheme.typography.titleMedium,
                        color = RomelText,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            selectedStatus = null
                            searchQuery = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RomelNavy),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Reset Filters")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("order_history_list")
            ) {
                item {
                    Text(
                        text = "Showing ${filteredOrders.size} of ${orders.size} orders • Volume ${formatEtb(totalAllOrdersEtb)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateMuted
                    )
                }
                items(filteredOrders, key = { it.id }) { order ->
                    OrderHistoryCard(
                        order = order,
                        dateFmt = dateFmt,
                        onPreview = { previewOrder = order },
                        onSharePdf = { PdfQuotationExporter.sharePdf(context, order, preferWhatsApp = false) },
                        onPrint = { PdfQuotationExporter.printOrder(context, order) }
                    )
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
private fun OrderHistoryCard(
    order: SalesOrder,
    dateFmt: SimpleDateFormat,
    onPreview: () -> Unit,
    onSharePdf: () -> Unit,
    onPrint: () -> Unit
) {
    val parsedStatus = remember(order.status) { OrderStatus.fromRaw(order.status) }
    val parsedType = remember(order.orderType) { OrderType.fromRaw(order.orderType) }
    val (statusBg, statusFg) = when (parsedStatus) {
        OrderStatus.CONFIRMED -> StatusEmeraldSoft to StatusEmerald
        OrderStatus.QUOTATION -> StatusBlueSoft to StatusBlue
        OrderStatus.DRAFT -> StatusAmberSoft to StatusAmber
        OrderStatus.CANCELLED -> StatusRoseSoft to StatusRose
    }
    val lineItems = remember(order.items) { order.parsedItems() }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = CleanWhite),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_history_card_${order.orderNumber}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = order.orderNumber,
                            style = MaterialTheme.typography.titleMedium,
                            color = RomelNavy,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = LightBlueSurface,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = parsedType.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = RomelNavy,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${order.customerName.ifBlank { "Walk-in Customer" }} • ${
                            order.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "Syncing..."
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = parsedStatus.label.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusFg,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (lineItems.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    lineItems.take(3).forEach { line ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${line.code} • ${line.name} (${line.quantity} ${line.unit})",
                                style = MaterialTheme.typography.bodySmall,
                                color = RomelText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = formatEtb(line.lineTotal),
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (lineItems.size > 3) {
                        Text(
                            text = "+${lineItems.size - 3} more item(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = RomelNavy
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${order.totalProducts} products • ${order.totalQuantity} units",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateMuted
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Cost",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateMuted
                    )
                    Text(
                        text = formatEtb(order.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        color = RomelRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Preview", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onSharePdf,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onPrint,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Print", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
