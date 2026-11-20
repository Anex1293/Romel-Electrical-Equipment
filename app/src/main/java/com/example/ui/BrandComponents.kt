package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SalesOrder
import com.example.model.UserRole
import com.example.model.formatEtb
import com.example.ui.theme.LightBlueSurface
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.RomelNavy
import com.example.ui.theme.RomelNavyDark
import com.example.ui.theme.RomelRed
import com.example.ui.theme.RomelRedSoft
import com.example.ui.theme.RomelText
import com.example.ui.theme.SlateMuted

@Composable
fun RomelWordmarkLogo(
    modifier: Modifier = Modifier,
    darkBackground: Boolean = false,
    titleSize: TextUnit = 26.sp,
    emblemSize: Dp = 24.dp,
    showSubtitle: Boolean = true
) {
    val primaryColor = if (darkBackground) Color.White else RomelNavy
    val subColor = if (darkBackground) Color(0xFFCBD5E1) else RomelText

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "R",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = titleSize,
                color = primaryColor,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.width(1.dp))
            Canvas(modifier = Modifier.size(emblemSize)) {
                val strokeW = size.minDimension * 0.24f
                val r = (size.minDimension - strokeW) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                // Signature red socket-ring 'O' from ROMEL brand mark
                drawArc(
                    color = RomelRed,
                    startAngle = 25f,
                    sweepAngle = 310f,
                    useCenter = false,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                val dotRadius = size.minDimension * 0.085f
                drawCircle(
                    color = RomelRed,
                    radius = dotRadius,
                    center = Offset(center.x - size.minDimension * 0.14f, center.y)
                )
                drawCircle(
                    color = RomelRed,
                    radius = dotRadius,
                    center = Offset(center.x + size.minDimension * 0.14f, center.y)
                )
            }
            Spacer(Modifier.width(1.dp))
            Text(
                text = "MEL",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = titleSize,
                color = primaryColor,
                letterSpacing = 1.sp
            )
        }
        if (showSubtitle) {
            Text(
                text = "ELECTRICAL EQUIPMENT",
                style = MaterialTheme.typography.labelSmall,
                color = subColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp
            )
        }
    }
}

@Composable
fun DualBarChart(
    bars: List<Triple<String, Float, Float>>,
    modifier: Modifier = Modifier,
    navyLabel: String = "Orders / Catalog",
    redLabel: String = "Sales / Demand"
) {
    val maxVal = (bars.maxOfOrNull { maxOf(it.second, it.third) } ?: 1f).coerceAtLeast(1f)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(RomelNavy, navyLabel)
            LegendDot(RomelRed, redLabel)
        }
        Spacer(Modifier.height(12.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            if (bars.isEmpty()) return@Canvas
            val groupCount = bars.size
            val groupWidth = size.width / groupCount
            val barWidth = (groupWidth * 0.28f).coerceIn(10f, 28f)
            val chartHeight = size.height - 16f

            // Subtle baseline grid lines
            val gridColor = Color(0xFFE2E8F3)
            for (i in 0..3) {
                val y = chartHeight * (i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5f
                )
            }

            bars.forEachIndexed { idx, (_, primaryVal, secondaryVal) ->
                val centerX = idx * groupWidth + groupWidth / 2f
                val h1 = ((primaryVal / maxVal).coerceIn(0.04f, 1f)) * chartHeight
                val h2 = ((secondaryVal / maxVal).coerceIn(0.04f, 1f)) * chartHeight

                drawRoundRect(
                    color = RomelNavy,
                    topLeft = Offset(centerX - barWidth - 3f, chartHeight - h1),
                    size = Size(barWidth, h1),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = RomelRed,
                    topLeft = Offset(centerX + 3f, chartHeight - h2),
                    size = Size(barWidth, h2),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            bars.forEach { (label, _, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
    }
}

@Composable
fun RoleAccessShowcaseRow(
    activeRole: UserRole,
    modifier: Modifier = Modifier,
    onSelectPreview: ((UserRole?) -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        RoleTile(
            title = "Admin",
            subtitle = "Full Access",
            accent = RomelRed,
            bg = RomelRedSoft,
            icon = Icons.Filled.Shield,
            selected = activeRole == UserRole.ADMIN,
            onClick = { onSelectPreview?.invoke(null) },
            modifier = Modifier.weight(1f)
        )
        RoleTile(
            title = "Sales Rep",
            subtitle = "View & Order",
            accent = RomelNavy,
            bg = LightBlueSurface,
            icon = Icons.Filled.Badge,
            selected = activeRole == UserRole.SALES_REP,
            onClick = { onSelectPreview?.invoke(UserRole.SALES_REP) },
            modifier = Modifier.weight(1f)
        )
        RoleTile(
            title = "Viewer",
            subtitle = "Browse Only",
            accent = SlateMuted,
            bg = Color(0xFFEDF2F7),
            icon = Icons.Filled.Visibility,
            selected = activeRole == UserRole.VIEWER,
            onClick = { onSelectPreview?.invoke(UserRole.VIEWER) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RoleTile(
    title: String,
    subtitle: String,
    accent: Color,
    bg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = bg,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else accent.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
        ) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = RomelText, maxLines = 1)
        }
    }
}

@Composable
fun QuotationPreviewDialog(
    order: SalesOrder,
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit,
    onPrint: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                RomelWordmarkLogo(titleSize = 18.sp, emblemSize = 16.dp, showSubtitle = false)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (order.orderType == "SALES_ORDER") "Sales Order Preview" else "Official Quotation Preview",
                    style = MaterialTheme.typography.titleMedium,
                    color = RomelNavy
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(color = RomelNavyDark, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("ROMEL ELECTRICAL EQUIPMENT", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        Text("Ref: ${order.orderNumber}  •  Status: ${order.status}", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelSmall)
                        Text("Customer: ${order.customerName.ifBlank { "Walk-in Customer" }}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
                order.parsedItems().forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${item.code} • ${item.name}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${item.quantity} ${item.unit} × ${formatEtb(item.unitPrice)}", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(formatEtb(item.lineTotal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = RomelNavy)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", style = MaterialTheme.typography.bodySmall)
                    Text(formatEtb(order.subtotal), style = MaterialTheme.typography.bodySmall)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Discount", style = MaterialTheme.typography.bodySmall)
                    Text("- ${formatEtb(order.discount)}", style = MaterialTheme.typography.bodySmall, color = RomelRed)
                }
                Surface(color = RomelNavy, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Text(formatEtb(order.totalAmount), color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onExportPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = RomelNavy),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PDF")
                    }
                    Button(
                        onClick = onShareWhatsApp,
                        colors = ButtonDefaults.buttonColors(containerColor = RomelRed),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("WhatsApp")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onPrint, modifier = Modifier.weight(1f).height(48.dp)) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Print")
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text("Close")
                    }
                }
            }
        }
    )
}
