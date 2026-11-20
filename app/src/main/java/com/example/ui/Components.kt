package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size as CoilSize
import com.example.model.CategoryItem
import com.example.model.Product
import com.example.model.UnitOfMeasure
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
import com.example.ui.theme.StatusBlueSoft
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusEmeraldSoft
import com.example.ui.theme.StatusRose
import com.example.ui.theme.StatusRoseSoft

@Composable
fun ProductImage(
    product: Product,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val rawUri: Any? = remember(product.customImageUri, product.imageAsset) {
        when {
            !product.customImageUri.isNullOrBlank() -> product.customImageUri
            product.imageAsset.isNotBlank() -> "file:///android_asset/${product.imageAsset}"
            else -> null
        }
    }
    val model = remember(rawUri, context) {
        rawUri?.let {
            ImageRequest.Builder(context)
                .data(it)
                .size(CoilSize(480, 480))
                .crossfade(true)
                .build()
        }
    }
    Surface(
        color = LightBlueSurface.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = "${product.name} (${product.code})",
                contentScale = contentScale,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Filled.ElectricalServices,
                    contentDescription = product.name,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun StockBadge(product: Product, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when {
        product.stockQuantity == null -> Triple(
            LightBlueSurface,
            SlateMuted,
            "Stock not set"
        )
        product.stockQuantity <= 0L -> Triple(StatusRoseSoft, StatusRose, "Out of Stock")
        product.stockQuantity <= product.lowStockThreshold -> Triple(
            StatusAmberSoft,
            StatusAmber,
            "Low (${product.stockQuantity} ${product.unit})"
        )
        else -> Triple(StatusEmeraldSoft, StatusEmerald, "In Stock")
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    role: UserRole,
    onClick: () -> Unit,
    onOpenCalculator: () -> Unit,
    onEditPrice: (() -> Unit)? = null,
    onAdjustStock: (() -> Unit)? = null,
    isWishlisted: Boolean = false,
    onToggleWishlist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = CleanWhite),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("product_card_${product.code}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                ProductImage(
                    product = product,
                    modifier = Modifier.size(88.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = LightBlueSurface,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = product.code,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RomelNavy,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            if (product.needsReview) {
                                Surface(
                                    color = StatusAmberSoft,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Review",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusAmber,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (onToggleWishlist != null) {
                            IconButton(
                                onClick = onToggleWishlist,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("wishlist_toggle_${product.code}")
                            ) {
                                Icon(
                                    imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = if (isWishlisted) {
                                        "Remove ${product.name} from wishlist"
                                    } else {
                                        "Save ${product.name} to wishlist"
                                    },
                                    tint = if (isWishlisted) RomelRed else SlateMuted
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = RomelText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (product.specifications.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = product.specifications,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatEtb(product.effectiveUnitPrice),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (product.effectiveUnitPrice != null) RomelRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StockBadge(product)
                        Text(
                            text = product.stockQuantity?.let { "$it ${product.unit}" } ?: product.unit,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (role == UserRole.ADMIN && onEditPrice != null) {
                        IconButton(
                            onClick = onEditPrice,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("quick_price_${product.code}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AttachMoney,
                                contentDescription = "Edit price for ${product.code}",
                                tint = RomelNavy
                            )
                        }
                    }
                    if (role == UserRole.ADMIN && onAdjustStock != null) {
                        IconButton(
                            onClick = onAdjustStock,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("quick_stock_${product.code}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Inventory,
                                contentDescription = "Adjust stock for ${product.code}",
                                tint = RomelNavy
                            )
                        }
                    }
                    Button(
                        onClick = onOpenCalculator,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RomelRed, contentColor = CleanWhite),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("calc_add_${product.code}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Calculate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Calc")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuantityPriceCalculatorSheet(
    product: Product,
    role: UserRole,
    onDismiss: () -> Unit,
    onAddToOrder: (unit: String, multiplier: Long, unitPrice: Double, quantity: Long, discount: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val canEditPrice = role == UserRole.ADMIN || role == UserRole.SALES_REP

    var selectedUnit by remember(product.id) { mutableStateOf(UnitOfMeasure.fromRaw(product.unit)) }
    var customMultiplierText by remember(product.id) { mutableStateOf("1") }
    var unitPriceText by remember(product.id) {
        mutableStateOf(product.effectiveUnitPrice?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "")
    }
    var quantityText by remember(product.id) { mutableStateOf("1") }
    var discountText by remember(product.id) { mutableStateOf("") }

    val multiplier = when (selectedUnit) {
        UnitOfMeasure.PCS -> 1L
        UnitOfMeasure.BOX -> product.piecesPerBox.coerceAtLeast(1L)
        UnitOfMeasure.CARTON -> product.piecesPerCarton.coerceAtLeast(1L)
        UnitOfMeasure.SET, UnitOfMeasure.PACK, UnitOfMeasure.OTHER ->
            (customMultiplierText.toLongOrNull() ?: 1L).coerceAtLeast(1L)
    }

    val unitPrice = unitPriceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val quantity = (quantityText.toLongOrNull() ?: 0L).coerceAtLeast(0L)
    val subtotal = unitPrice * quantity
    val discount = (discountText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val total = (subtotal - discount).coerceAtLeast(0.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProductImage(product = product, modifier = Modifier.size(64.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Product: ${product.code}",
                        style = MaterialTheme.typography.labelMedium,
                        color = RomelRed
                    )
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    StockBadge(product)
                }
            }

            Spacer(Modifier.height(14.dp))

            Text("Unit of Measurement", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UnitOfMeasure.entries.forEach { u ->
                    val label = when (u) {
                        UnitOfMeasure.BOX -> "Box (${product.piecesPerBox} PCS)"
                        UnitOfMeasure.CARTON -> "Carton (${product.piecesPerCarton} PCS)"
                        else -> u.label
                    }
                    FilterChip(
                        selected = selectedUnit == u,
                        onClick = {
                            selectedUnit = u
                            val newMult = when (u) {
                                UnitOfMeasure.PCS -> 1L
                                UnitOfMeasure.BOX -> product.piecesPerBox
                                UnitOfMeasure.CARTON -> product.piecesPerCarton
                                else -> customMultiplierText.toLongOrNull() ?: 1L
                            }
                            product.effectiveUnitPrice?.let { base ->
                                unitPriceText = String.format(java.util.Locale.US, "%.2f", base * newMult)
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }

            if (selectedUnit in listOf(UnitOfMeasure.SET, UnitOfMeasure.PACK, UnitOfMeasure.OTHER)) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customMultiplierText,
                    onValueChange = { customMultiplierText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Pieces per ${selectedUnit.label}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = unitPriceText,
                onValueChange = { if (canEditPrice) unitPriceText = it },
                enabled = canEditPrice,
                label = {
                    Text(
                        if (product.effectiveUnitPrice == null && !canEditPrice)
                            "Unit Price (Price not set)"
                        else "Unit Price (ETB per ${selectedUnit.label})"
                    )
                },
                placeholder = { Text("Price not set") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_unit_price_input")
            )

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        val next = (quantity - 1L).coerceAtLeast(1L)
                        quantityText = next.toString()
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                }
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Quantity (${selectedUnit.label})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calc_quantity_input")
                )
                OutlinedButton(
                    onClick = {
                        val next = quantity + 1L
                        quantityText = next.toString()
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = discountText,
                onValueChange = { if (canEditPrice) discountText = it },
                enabled = canEditPrice,
                label = { Text("Discount (Optional ETB)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_discount_input")
            )

            Spacer(Modifier.height(14.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Pieces", style = MaterialTheme.typography.bodySmall)
                        Text("${quantity * multiplier} PCS", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal (Unit × Qty)", style = MaterialTheme.typography.bodyMedium)
                        Text(formatEtb(subtotal), style = MaterialTheme.typography.titleSmall)
                    }
                    if (discount > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Discount", style = MaterialTheme.typography.bodyMedium)
                            Text("- ${formatEtb(discount)}", color = RomelRed, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Price", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = formatEtb(total),
                            style = MaterialTheme.typography.titleLarge,
                            color = RomelNavy,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val safeQty = quantity.coerceAtLeast(1L)
                    onAddToOrder(selectedUnit.label, multiplier, unitPrice, safeQty, discount)
                    onDismiss()
                },
                enabled = quantity >= 1L,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = RomelRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("calc_add_to_order_button")
            ) {
                Text("Add to Order / Quotation", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun AdminPriceEditorDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (selling: Double?, wholesale: Double?, retail: Double?, discount: Double?) -> Unit
) {
    var selling by remember(product.id) { mutableStateOf(product.sellingPrice?.toString() ?: "") }
    var wholesale by remember(product.id) { mutableStateOf(product.wholesalePrice?.toString() ?: "") }
    var retail by remember(product.id) { mutableStateOf(product.retailPrice?.toString() ?: "") }
    var discount by remember(product.id) { mutableStateOf(product.discountPrice?.toString() ?: "") }

    val validation = remember(selling, wholesale, retail, discount) {
        com.example.model.PriceValidator.validateAll(
            sellingRaw = selling,
            wholesaleRaw = wholesale,
            retailRaw = retail,
            discountRaw = discount
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ETB Prices • ${product.code}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(product.name, style = MaterialTheme.typography.bodySmall)
                Text(
                    "Enter positive ETB amounts, or leave blank for \"Price not set\".",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = selling,
                    onValueChange = { selling = it },
                    label = { Text("Unit Selling Price (ETB)") },
                    isError = validation.sellingError != null,
                    supportingText = validation.sellingError?.let { err -> { Text(err) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_selling_price_input")
                )
                OutlinedTextField(
                    value = wholesale,
                    onValueChange = { wholesale = it },
                    label = { Text("Wholesale Price (ETB)") },
                    isError = validation.wholesaleError != null,
                    supportingText = validation.wholesaleError?.let { err -> { Text(err) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_wholesale_price_input")
                )
                OutlinedTextField(
                    value = retail,
                    onValueChange = { retail = it },
                    label = { Text("Retail Price (ETB)") },
                    isError = validation.retailError != null,
                    supportingText = validation.retailError?.let { err -> { Text(err) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_retail_price_input")
                )
                OutlinedTextField(
                    value = discount,
                    onValueChange = { discount = it },
                    label = { Text("Discount / Promo Price (ETB)") },
                    isError = validation.discountError != null,
                    supportingText = validation.discountError?.let { err -> { Text(err) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_discount_price_input")
                )
            }
        },
        confirmButton = {
            Button(
                enabled = validation.isValid,
                onClick = {
                    onSave(
                        validation.sellingPrice,
                        validation.wholesalePrice,
                        validation.retailPrice,
                        validation.discountPrice
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RomelRed),
                modifier = Modifier.testTag("save_prices_button")
            ) {
                Text("Save to Firestore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminStockAdjustDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (delta: Long, reason: String) -> Unit
) {
    var isAdd by remember { mutableStateOf(true) }
    var qtyText by remember { mutableStateOf("10") }
    var reason by remember { mutableStateOf("Warehouse Restock") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Stock • ${product.code}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Current stock: ${product.stockQuantity?.toString() ?: "Stock not set"} ${product.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isAdd,
                        onClick = {
                            isAdd = true
                            reason = "Incoming Warehouse Stock"
                        },
                        label = { Text("+ Add Stock") }
                    )
                    FilterChip(
                        selected = !isAdd,
                        onClick = {
                            isAdd = false
                            reason = "Manual Stock Reduction"
                        },
                        label = { Text("- Reduce Stock") }
                    )
                }
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Quantity (${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("stock_adjust_qty_input")
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason / Reference") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = (qtyText.toLongOrNull() ?: 0L).coerceAtLeast(1L)
                    onConfirm(if (isAdd) qty else -qty, reason)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_stock_adjust_button")
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddEditProductDialog(
    existing: Product?,
    categories: List<CategoryItem>,
    onDismiss: () -> Unit,
    onSave: (Product, isNew: Boolean, pickedImageUri: Uri?) -> Unit
) {
    val isNew = existing == null
    var code by remember { mutableStateOf(existing?.code ?: "") }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: categories.firstOrNull()?.name ?: "") }
    var series by remember { mutableStateOf(existing?.series ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var specs by remember { mutableStateOf(existing?.specifications ?: "") }
    var unit by remember { mutableStateOf(existing?.unit ?: "PCS") }
    var boxQty by remember { mutableStateOf((existing?.piecesPerBox ?: 10L).toString()) }
    var cartonQty by remember { mutableStateOf((existing?.piecesPerCarton ?: 100L).toString()) }
    var lowThresh by remember { mutableStateOf((existing?.lowStockThreshold ?: 10L).toString()) }
    var needsReview by remember { mutableStateOf(existing?.needsReview ?: false) }
    var reviewNote by remember { mutableStateOf(existing?.reviewNote ?: "") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var catExpanded by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add New Product" else "Edit ${existing?.code}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (isNew) code = it },
                    enabled = isNew,
                    label = { Text("Product Code *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_product_code")
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_product_name")
                )
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
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
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
                    label = { Text("Technical Specifications") },
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
                        value = lowThresh,
                        onValueChange = { lowThresh = it.filter { c -> c.isDigit() } },
                        label = { Text("Low Alert") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = boxQty,
                        onValueChange = { boxQty = it.filter { c -> c.isDigit() } },
                        label = { Text("PCS / Box") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cartonQty,
                        onValueChange = { cartonQty = it.filter { c -> c.isDigit() } },
                        label = { Text("PCS / Carton") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (pickedUri != null) "Custom Photo Selected" else "Upload Product Photo")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = needsReview, onCheckedChange = { needsReview = it })
                    Text("Flag as Needs Review", style = MaterialTheme.typography.bodySmall)
                }
                if (needsReview) {
                    OutlinedTextField(
                        value = reviewNote,
                        onValueChange = { reviewNote = it },
                        label = { Text("Review Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = code.isNotBlank() && name.isNotBlank(),
                onClick = {
                    val base = existing ?: Product()
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
                            needsReview = needsReview,
                            reviewNote = reviewNote.takeIf { needsReview && it.isNotBlank() }
                        ),
                        isNew,
                        pickedUri
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text(if (isNew) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SimpleBarChart(
    items: List<Pair<String, Float>>,
    barColor: Color,
    valueFormatter: (Float) -> String,
    modifier: Modifier = Modifier
) {
    val maxVal = (items.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (label, value) ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = valueFormatter(value),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(3.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                ) {
                    val trackColor = Color(0xFFE2E8F0)
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset.Zero,
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    val fillW = (value / maxVal).coerceIn(0f, 1f) * size.width
                    if (fillW > 0f) {
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset.Zero,
                            size = Size(fillW, size.height),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
            }
        }
    }
}
