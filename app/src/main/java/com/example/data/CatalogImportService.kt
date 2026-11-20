package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import com.example.model.Product
import com.example.model.sanitizeFirestoreId
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

private const val TAG = "CatalogImportService"

class CatalogImportService(private val context: Context) {

    private val extractionSchema = Schema.obj(
        properties = mapOf(
            "products" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "code" to Schema.string(description = "Unique product model/code"),
                        "name" to Schema.string(description = "Product name"),
                        "category" to Schema.string(description = "Product category"),
                        "series" to Schema.string(description = "Product series"),
                        "description" to Schema.string(description = "Short product description"),
                        "specifications" to Schema.string(description = "Technical specifications"),
                        "catalogPage" to Schema.integer(description = "Catalog page number"),
                        "unclear" to Schema.boolean(description = "True if any detail is ambiguous or illegible")
                    )
                )
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = extractionSchema
            }
        )
    }

    suspend fun parseCsvUri(uri: Uri): List<Product> = withContext(Dispatchers.IO) {
        val products = mutableListOf<Product>()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                val lines = reader.readLines().filter { it.isNotBlank() }
                if (lines.isEmpty()) return@use
                val delimiter = if (lines.first().contains('\t')) '\t' else ','
                val startIndex = if (lines.first().lowercase().contains("code")) 1 else 0
                for (i in startIndex until lines.size) {
                    val cols = splitCsvLine(lines[i], delimiter)
                    if (cols.isEmpty()) continue
                    val code = cols.getOrNull(0)?.trim().orEmpty()
                    val name = cols.getOrNull(1)?.trim().orEmpty()
                    val category = cols.getOrNull(2)?.trim().orEmpty().ifBlank { "Uncategorized" }
                    val series = cols.getOrNull(3)?.trim().orEmpty().ifBlank { category }
                    val desc = cols.getOrNull(4)?.trim().orEmpty()
                    val specs = cols.getOrNull(5)?.trim().orEmpty()
                    val price = cols.getOrNull(6)?.trim()?.toDoubleOrNull()
                    val stock = cols.getOrNull(7)?.trim()?.toLongOrNull()
                    val unit = cols.getOrNull(8)?.trim().orEmpty().ifBlank { "PCS" }
                    val page = cols.getOrNull(9)?.trim()?.toLongOrNull() ?: 0L

                    val unclear = code.isBlank() || name.isBlank() || category == "Uncategorized"
                    val safeCode = code.ifBlank { "IMP-$i" }
                    products.add(
                        Product(
                            id = sanitizeFirestoreId(safeCode),
                            code = safeCode,
                            name = name.ifBlank { "Unnamed Imported Item ($safeCode)" },
                            category = category,
                            series = series,
                            description = desc,
                            specifications = specs,
                            catalogPage = page,
                            imageAsset = "",
                            unit = unit,
                            sellingPrice = price,
                            stockQuantity = stock,
                            needsReview = unclear,
                            reviewNote = if (unclear) "Missing code, name, or category in CSV row ${i + 1}." else null
                        )
                    )
                }
            }
        }
        products
    }

    suspend fun extractProductsFromPdfUri(uri: Uri, maxPages: Int = 5): Result<List<Product>> =
        withContext(Dispatchers.IO) {
            try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: return@withContext Result.failure(IllegalStateException("Unable to open PDF file."))
                val extracted = mutableListOf<Product>()
                pfd.use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        val pageLimit = minOf(renderer.pageCount, maxPages)
                        for (pageIndex in 0 until pageLimit) {
                            renderer.openPage(pageIndex).use { page ->
                                val bmp = Bitmap.createBitmap(
                                    (page.width * 1.5f).toInt().coerceAtMost(1400),
                                    (page.height * 1.5f).toInt().coerceAtMost(1900),
                                    Bitmap.Config.ARGB_8888
                                )
                                bmp.eraseColor(Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                val response = withTimeout(60_000L) {
                                    model.generateContent(
                                        content {
                                            image(bmp)
                                            text(
                                                "Extract every electrical product from this ROMEL catalog page (page ${pageIndex + 1}). " +
                                                    "Do NOT guess prices or stock quantities. Set unclear=true if any code, name, or specification is ambiguous."
                                            )
                                        }
                                    )
                                }
                                bmp.recycle()
                                val jsonStr = response.text
                                    ?: throw IllegalStateException("AI returned empty response for page ${pageIndex + 1}.")
                                val root = JSONObject(jsonStr)
                                val arr = root.optJSONArray("products")
                                if (arr != null) {
                                    for (j in 0 until arr.length()) {
                                        val obj = arr.getJSONObject(j)
                                        val code = obj.optString("code").trim()
                                        val name = obj.optString("name").trim()
                                        if (code.isBlank() && name.isBlank()) continue
                                        val unclear = obj.optBoolean("unclear", false) || code.isBlank() || name.isBlank()
                                        val safeCode = code.ifBlank { "PDF-P${pageIndex + 1}-${j + 1}" }
                                        extracted.add(
                                            Product(
                                                id = sanitizeFirestoreId(safeCode),
                                                code = safeCode,
                                                name = name.ifBlank { "Review Item ($safeCode)" },
                                                category = obj.optString("category").ifBlank { "Uncategorized" },
                                                series = obj.optString("series").ifBlank { "General" },
                                                description = obj.optString("description"),
                                                specifications = obj.optString("specifications"),
                                                catalogPage = obj.optLong("catalogPage", (pageIndex + 1).toLong()),
                                                imageAsset = "",
                                                needsReview = unclear,
                                                reviewNote = if (unclear) "Extracted from PDF page ${pageIndex + 1}; verify details before publishing." else null
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Result.success(extracted)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "PDF AI extraction timed out", e)
                Result.failure(Exception("PDF extraction timed out after 60 seconds. Please try a smaller PDF."))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "PDF AI extraction failed", e)
                Result.failure(e)
            }
        }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }
}
