package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.model.SalesOrder
import com.example.model.formatEtb
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfQuotationExporter {

    fun generateOrderPdf(context: Context, order: SalesOrder): File {
        val dir = File(context.cacheDir, "quotations").apply { mkdirs() }
        val file = File(dir, "ROMEL_${order.orderNumber}.pdf")

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 72 DPI
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val navyPaint = Paint().apply { color = Color.rgb(11, 31, 58) }
        val redPaint = Paint().apply { color = Color.rgb(225, 29, 46) }
        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subHeaderPaint = Paint().apply {
            color = Color.rgb(220, 230, 245)
            textSize = 9.5f
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = Color.rgb(90, 107, 132)
            textSize = 9.5f
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(11, 31, 58)
            textSize = 10.5f
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.rgb(11, 31, 58)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(220, 227, 238)
            strokeWidth = 1f
        }

        // Top Navy Banner + Red Accent Bar
        canvas.drawRect(0f, 0f, 595f, 92f, navyPaint)
        canvas.drawRect(0f, 92f, 595f, 96f, redPaint)

        canvas.drawText("ROMEL ELECTRICAL EQUIPMENT", 32f, 38f, headerTextPaint)
        canvas.drawText(
            "Urael, ROMEL Building, Addis Ababa, Ethiopia  |  +251 911 523 493 / +251 115 620 212",
            32f, 56f, subHeaderPaint
        )
        canvas.drawText("www.romelelectrical.com  |  romel@romelelectrical.com", 32f, 72f, subHeaderPaint)

        // Document title & metadata
        val docTitle = if (order.orderType == "SALES_ORDER") "SALES ORDER / INVOICE" else "OFFICIAL QUOTATION"
        canvas.drawText(docTitle, 32f, 126f, Paint(boldPaint).apply { textSize = 15f })

        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
            .format(order.createdAt?.toDate() ?: Date())
        canvas.drawText("Ref: ${order.orderNumber}", 400f, 122f, boldPaint)
        canvas.drawText("Date: $dateStr", 400f, 136f, labelPaint)
        canvas.drawText("Status: ${order.status}", 400f, 150f, labelPaint)

        canvas.drawText("Customer: ${order.customerName.ifBlank { "Walk-in Customer" }}", 32f, 148f, bodyPaint)
        if (order.customerPhone.isNotBlank()) {
            canvas.drawText("Phone: ${order.customerPhone}", 32f, 162f, labelPaint)
        }
        canvas.drawText("Prepared by: ${order.createdByName} (${order.createdByRole})", 32f, 176f, labelPaint)

        // Table Header
        var y = 200f
        canvas.drawRect(32f, y, 563f, y + 24f, Paint().apply { color = Color.rgb(238, 242, 247) })
        canvas.drawText("Code", 40f, y + 16f, boldPaint)
        canvas.drawText("Product", 115f, y + 16f, boldPaint)
        canvas.drawText("Unit Price", 335f, y + 16f, boldPaint)
        canvas.drawText("Qty", 425f, y + 16f, boldPaint)
        canvas.drawText("Total (ETB)", 480f, y + 16f, boldPaint)
        y += 34f

        val items = order.parsedItems()
        for (item in items.take(22)) {
            canvas.drawText(item.code.take(12), 40f, y, bodyPaint)
            canvas.drawText(item.name.take(34), 115f, y, bodyPaint)
            canvas.drawText(String.format(Locale.US, "%,.2f", item.unitPrice), 335f, y, bodyPaint)
            canvas.drawText("${item.quantity} ${item.unit}", 420f, y, bodyPaint)
            canvas.drawText(String.format(Locale.US, "%,.2f", item.lineTotal), 480f, y, boldPaint)
            y += 16f
            canvas.drawLine(32f, y - 4f, 563f, y - 4f, linePaint)
            y += 8f
        }

        y += 12f
        canvas.drawText("Total Products: ${order.totalProducts}  |  Total Qty: ${order.totalQuantity}", 32f, y, labelPaint)
        y += 20f
        canvas.drawText("Subtotal:", 380f, y, bodyPaint)
        canvas.drawText(formatEtb(order.subtotal), 460f, y, boldPaint)
        y += 18f
        canvas.drawText("Discount:", 380f, y, bodyPaint)
        canvas.drawText("- ${formatEtb(order.discount)}", 460f, y, bodyPaint)
        y += 22f
        canvas.drawRect(370f, y - 16f, 563f, y + 8f, navyPaint)
        canvas.drawText("GRAND TOTAL:", 378f, y, Paint(boldPaint).apply { color = Color.WHITE })
        canvas.drawText(formatEtb(order.totalAmount), 465f, y, Paint(boldPaint).apply { color = Color.WHITE })

        if (!order.notes.isNullOrBlank()) {
            y += 32f
            canvas.drawText("Notes: ${order.notes.take(110)}", 32f, y, labelPaint)
        }

        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    fun sharePdf(context: Context, order: SalesOrder, preferWhatsApp: Boolean = false) {
        val file = generateOrderPdf(context, order)
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val summary = buildString {
            appendLine("*ROMEL Electrical Equipment*")
            appendLine("${if (order.orderType == "SALES_ORDER") "Sales Order" else "Quotation"}: ${order.orderNumber}")
            if (order.customerName.isNotBlank()) appendLine("Customer: ${order.customerName}")
            appendLine("Items: ${order.totalProducts} | Qty: ${order.totalQuantity}")
            appendLine("Total: ${formatEtb(order.totalAmount)}")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, summary)
            putExtra(Intent.EXTRA_SUBJECT, "ROMEL ${order.orderNumber}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (preferWhatsApp) {
                setPackage("com.whatsapp")
            }
        }
        try {
            context.startActivity(
                if (preferWhatsApp) intent else Intent.createChooser(intent, "Share ${order.orderNumber} PDF")
            )
        } catch (_: Exception) {
            // Fallback to general chooser if WhatsApp is not installed
            intent.setPackage(null)
            context.startActivity(Intent.createChooser(intent, "Share ${order.orderNumber} PDF"))
        }
    }

    fun printOrder(context: Context, order: SalesOrder) {
        val file = generateOrderPdf(context, order)
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("ROMEL_${order.orderNumber}.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileInputStream(file).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        printManager.print("ROMEL_${order.orderNumber}", adapter, null)
    }
}
