package com.deehem.gpawhiz.service

import com.deehem.gpawhiz.data.ExchangeRate
import com.deehem.gpawhiz.data.Scholarship
import com.deehem.gpawhiz.data.ScholarshipStatus
import java.text.SimpleDateFormat
import java.util.*

object CurrencyConverter {

    data class CurrencyInfo(
        val code: String,
        val symbol: String,
        val name: String
    )

    val POPULAR_CURRENCIES = listOf(
        CurrencyInfo("NGN", "₦", "Nigerian Naira"),
        CurrencyInfo("USD", "$", "US Dollar"),
        CurrencyInfo("GBP", "£", "British Pound"),
        CurrencyInfo("EUR", "€", "Euro"),
        CurrencyInfo("CAD", "C$", "Canadian Dollar"),
        CurrencyInfo("AUD", "A$", "Australian Dollar"),
        CurrencyInfo("GHS", "GH₵", "Ghanaian Cedi"),
        CurrencyInfo("KES", "KSh", "Kenyan Shilling"),
        CurrencyInfo("ZAR", "R", "South African Rand")
    )

    val DEFAULT_RATES = listOf(
        ExchangeRate("USD", "NGN", 1550.0, "Central Bank reference benchmark"),
        ExchangeRate("GBP", "NGN", 1950.0, "Central Bank reference benchmark"),
        ExchangeRate("EUR", "NGN", 1680.0, "Central Bank reference benchmark"),
        ExchangeRate("CAD", "NGN", 1120.0, "Market reference benchmark"),
        ExchangeRate("AUD", "NGN", 980.0, "Market reference benchmark"),
        ExchangeRate("GHS", "NGN", 102.0, "West Africa regional benchmark"),
        ExchangeRate("KES", "NGN", 12.0, "East Africa regional benchmark"),
        ExchangeRate("ZAR", "NGN", 84.0, "Southern Africa benchmark")
    )

    fun normalizeCurrencyCode(raw: String?): String {
        if (raw.isNullOrBlank()) return "NGN"
        val trimmed = raw.trim()
        return when (trimmed) {
            "₦" -> "NGN"
            "$" -> "USD"
            "£" -> "GBP"
            "€" -> "EUR"
            "C$", "CAD$" -> "CAD"
            "A$", "AUD$" -> "AUD"
            "GH₵" -> "GHS"
            "KSh" -> "KES"
            "R" -> "ZAR"
            else -> trimmed.uppercase()
        }
    }

    fun getCurrencySymbol(code: String): String {
        val clean = normalizeCurrencyCode(code)
        return POPULAR_CURRENCIES.find { it.code == clean }?.symbol ?: clean
    }

    fun getCurrencyName(code: String): String {
        val clean = normalizeCurrencyCode(code)
        return POPULAR_CURRENCIES.find { it.code == clean }?.name ?: clean
    }

    sealed class ConversionResult {
        data class Success(
            val convertedAmount: Double,
            val rateUsed: Double,
            val rateDate: Long,
            val isDirect: Boolean,
            val sourceDescription: String = "",
            val pairLabel: String = ""
        ) : ConversionResult()

        data class SameCurrency(val amount: Double) : ConversionResult()

        data class RateMissing(
            val fromCurrency: String,
            val toCurrency: String
        ) : ConversionResult()

        object InvalidAmount : ConversionResult()
    }

    data class RateUsageInfo(
        val fromCurrency: String,
        val toCurrency: String,
        val rate: Double,
        val lastUpdated: Long,
        val sourceDescription: String = ""
    )

    data class NormalizedFundingResult(
        val totalAmount: Double,
        val targetCurrency: String,
        val includedCount: Int,
        val unconvertedCount: Int,
        val unconvertedCurrencies: List<String>,
        val ratesUsed: List<RateUsageInfo>
    ) {
        val hasMissingRates: Boolean get() = unconvertedCount > 0
    }

    /**
     * Converts an amount from one currency to another using local rates only.
     * Offline-first: Checks direct pair, inverse pair, or cross-rate via common base.
     */
    fun convert(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        rates: List<ExchangeRate>,
        depth: Int = 0
    ): ConversionResult {
        if (amount <= 0.0) return ConversionResult.InvalidAmount
        if (depth > 1) return ConversionResult.RateMissing(fromCurrency, toCurrency)

        val fromClean = normalizeCurrencyCode(fromCurrency)
        val toClean = normalizeCurrencyCode(toCurrency)

        if (fromClean == toClean) {
            return ConversionResult.SameCurrency(amount)
        }

        // 1. Check direct rate: fromClean -> toClean
        val direct = rates.find {
            normalizeCurrencyCode(it.fromCurrency) == fromClean &&
            normalizeCurrencyCode(it.toCurrency) == toClean &&
            it.rate > 0.0
        }
        if (direct != null) {
            return ConversionResult.Success(
                convertedAmount = amount * direct.rate,
                rateUsed = direct.rate,
                rateDate = direct.lastUpdated,
                isDirect = true,
                sourceDescription = direct.sourceDescription,
                pairLabel = "1 $fromClean = ${formatRateValue(direct.rate)} $toClean"
            )
        }

        // 2. Check inverse rate: toClean -> fromClean
        val inverse = rates.find {
            normalizeCurrencyCode(it.fromCurrency) == toClean &&
            normalizeCurrencyCode(it.toCurrency) == fromClean &&
            it.rate > 0.0
        }
        if (inverse != null) {
            val effRate = 1.0 / inverse.rate
            return ConversionResult.Success(
                convertedAmount = amount / inverse.rate,
                rateUsed = effRate,
                rateDate = inverse.lastUpdated,
                isDirect = false,
                sourceDescription = inverse.sourceDescription,
                pairLabel = "1 $fromClean = ${formatRateValue(effRate)} $toClean (Inverse of 1 $toClean = ${formatRateValue(inverse.rate)} $fromClean)"
            )
        }

        // 3. Check cross rate via intermediate currency (e.g. NGN or USD)
        // Only attempt cross-rate if depth is 0
        if (depth == 0) {
            val intermediateCurrencies = listOf("NGN", "USD", "EUR", "GBP").filter { it != fromClean && it != toClean }
            for (intermediate in intermediateCurrencies) {
                // Pass depth + 1 to sub-calls to prevent infinite recursion
                val fromToInter = convert(1.0, fromClean, intermediate, rates, depth + 1)
                val interToTarget = convert(1.0, intermediate, toClean, rates, depth + 1)
                if (fromToInter is ConversionResult.Success && interToTarget is ConversionResult.Success) {
                    val crossRate = fromToInter.rateUsed * interToTarget.rateUsed
                    val date = maxOf(fromToInter.rateDate, interToTarget.rateDate)
                    return ConversionResult.Success(
                        convertedAmount = amount * crossRate,
                        rateUsed = crossRate,
                        rateDate = date,
                        isDirect = false,
                        sourceDescription = "Derived via $intermediate",
                        pairLabel = "1 $fromClean ≈ ${formatRateValue(crossRate)} $toClean (via $intermediate)"
                    )
                }
            }
        }

        return ConversionResult.RateMissing(fromClean, toClean)
    }

    /**
     * Compute normalized sum for a list of scholarships into target default currency.
     */
    fun calculateNormalizedFunding(
        scholarships: List<Scholarship>,
        targetCurrency: String,
        rates: List<ExchangeRate>,
        awardedOnly: Boolean = true
    ): NormalizedFundingResult {
        val targetClean = normalizeCurrencyCode(targetCurrency)
        var total = 0.0
        var included = 0
        var unconverted = 0
        val unconvertedCurrs = mutableSetOf<String>()
        val ratesApplied = mutableMapOf<String, RateUsageInfo>()

        for (s in scholarships) {
            val isAwarded = s.status == ScholarshipStatus.AWARDED || s.outcome.equals("Awarded", ignoreCase = true)
            if (awardedOnly && !isAwarded) continue

            val amt = s.effectiveAmount
            if (amt <= 0.0) continue

            val curr = s.effectiveCurrency
            val result = convert(amt, curr, targetClean, rates)

            when (result) {
                is ConversionResult.SameCurrency -> {
                    total += result.amount
                    included++
                }
                is ConversionResult.Success -> {
                    total += result.convertedAmount
                    included++
                    val key = "$curr->$targetClean"
                    if (!ratesApplied.containsKey(key)) {
                        ratesApplied[key] = RateUsageInfo(
                            fromCurrency = curr,
                            toCurrency = targetClean,
                            rate = result.rateUsed,
                            lastUpdated = result.rateDate,
                            sourceDescription = result.sourceDescription
                        )
                    }
                }
                is ConversionResult.RateMissing -> {
                    unconverted++
                    unconvertedCurrs.add(curr)
                }
                is ConversionResult.InvalidAmount -> {
                    // Ignore non-positive
                }
            }
        }

        return NormalizedFundingResult(
            totalAmount = total,
            targetCurrency = targetClean,
            includedCount = included,
            unconvertedCount = unconverted,
            unconvertedCurrencies = unconvertedCurrs.toList(),
            ratesUsed = ratesApplied.values.toList()
        )
    }

    /**
     * Format currency amount according to requirements:
     * - Symbols & ISO code: e.g. "₦500,000 NGN", "$200 USD"
     * - Sensible decimal precision (no unnecessary .00)
     */
    fun format(
        amount: Double,
        currency: String,
        includeIso: Boolean = true
    ): String {
        if (amount <= 0.0) return "0 ${normalizeCurrencyCode(currency)}"
        val clean = normalizeCurrencyCode(currency)
        val symbol = getCurrencySymbol(clean)

        val numStr = if (amount % 1.0 == 0.0) {
            "%,.0f".format(Locale.US, amount)
        } else {
            "%,.2f".format(Locale.US, amount)
        }

        return if (includeIso) {
            "$symbol$numStr $clean"
        } else {
            "$symbol$numStr"
        }
    }

    fun formatConverted(
        amount: Double,
        currency: String,
        includeIso: Boolean = true
    ): String {
        return "≈ ${format(amount, currency, includeIso)}"
    }

    fun formatRateValue(rate: Double): String {
        return if (rate % 1.0 == 0.0) {
            "%,.0f".format(Locale.US, rate)
        } else if (rate >= 100) {
            "%,.2f".format(Locale.US, rate)
        } else if (rate >= 1) {
            "%.4f".format(Locale.US, rate)
        } else {
            "%.6f".format(Locale.US, rate)
        }
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0L) return "Unknown"
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
