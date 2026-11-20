package com.example.model

import kotlin.math.round

data class ParsedPriceField(
    val value: Double?,
    val error: String?
)

data class ValidatedProductPrices(
    val sellingPrice: Double?,
    val wholesalePrice: Double?,
    val retailPrice: Double?,
    val discountPrice: Double?,
    val sellingError: String? = null,
    val wholesaleError: String? = null,
    val retailError: String? = null,
    val discountError: String? = null
) {
    val isValid: Boolean
        get() = sellingError == null &&
            wholesaleError == null &&
            retailError == null &&
            discountError == null

    val firstError: String?
        get() = sellingError ?: wholesaleError ?: retailError ?: discountError
}

object PriceValidator {
    const val MAX_PRICE_ETB = 10_000_000.0

    fun validateAdminCredentialsInput(username: String, password: String): String? {
        val u = username.trim()
        val p = password
        if (u.length < 3) return "Admin username must be at least 3 characters."
        if (u.length > 64) return "Admin username must be 64 characters or fewer."
        if (!u.matches(Regex("^[a-zA-Z0-9._\\-@]+$"))) {
            return "Admin username may only contain letters, numbers, '.', '_', '-', or '@'."
        }
        if (p.length < 6) return "Admin password must be at least 6 characters."
        if (p.length > 128) return "Admin password must be 128 characters or fewer."
        return null
    }

    fun validateSinglePrice(raw: String, fieldLabel: String = "Price"): ParsedPriceField {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return ParsedPriceField(value = null, error = null)
        }
        val parsed = trimmed.toDoubleOrNull()
            ?: return ParsedPriceField(null, "$fieldLabel must be a valid number.")
        if (!parsed.isFinite()) {
            return ParsedPriceField(null, "$fieldLabel must be a finite number.")
        }
        if (parsed <= 0.0) {
            return ParsedPriceField(null, "$fieldLabel must be greater than 0 ETB (blank to clear).")
        }
        if (parsed > MAX_PRICE_ETB) {
            return ParsedPriceField(null, "$fieldLabel cannot exceed 10,000,000 ETB.")
        }
        val rounded = round(parsed * 100.0) / 100.0
        return ParsedPriceField(value = rounded, error = null)
    }

    fun validateAll(
        sellingRaw: String,
        wholesaleRaw: String,
        retailRaw: String,
        discountRaw: String
    ): ValidatedProductPrices {
        val sell = validateSinglePrice(sellingRaw, "Selling price")
        val whole = validateSinglePrice(wholesaleRaw, "Wholesale price")
        val retail = validateSinglePrice(retailRaw, "Retail price")
        val disc = validateSinglePrice(discountRaw, "Discount price")

        var discountError = disc.error
        if (discountError == null && disc.value != null && sell.value != null && disc.value > sell.value) {
            discountError = "Discount price cannot exceed selling price."
        }
        if (discountError == null && disc.value != null && sell.value == null && retail.value != null && disc.value > retail.value) {
            discountError = "Discount price cannot exceed retail price."
        }

        return ValidatedProductPrices(
            sellingPrice = sell.value,
            wholesalePrice = whole.value,
            retailPrice = retail.value,
            discountPrice = disc.value,
            sellingError = sell.error,
            wholesaleError = whole.error,
            retailError = retail.error,
            discountError = discountError
        )
    }

    fun validateValues(
        selling: Double?,
        wholesale: Double?,
        retail: Double?,
        discount: Double?
    ): String? {
        listOf(
            "Selling price" to selling,
            "Wholesale price" to wholesale,
            "Retail price" to retail,
            "Discount price" to discount
        ).forEach { (label, v) ->
            if (v != null) {
                if (!v.isFinite()) return "$label must be a finite number."
                if (v <= 0.0) return "$label must be greater than 0 ETB."
                if (v > MAX_PRICE_ETB) return "$label cannot exceed 10,000,000 ETB."
            }
        }
        if (discount != null && selling != null && discount > selling) {
            return "Discount price cannot exceed selling price."
        }
        if (discount != null && selling == null && retail != null && discount > retail) {
            return "Discount price cannot exceed retail price."
        }
        return null
    }
}
