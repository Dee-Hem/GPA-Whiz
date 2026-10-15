package com.deehem.gpawhiz.service

import com.deehem.gpawhiz.data.ExchangeRate
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object CurrencyConverter {

    val DEFAULT_RATES = listOf(
        ExchangeRate("USD", "NGN", 1500.0, "Default Market Rate", System.currentTimeMillis()),
        ExchangeRate("GBP", "NGN", 1950.0, "Default Market Rate", System.currentTimeMillis()),
        ExchangeRate("EUR", "NGN", 1650.0, "Default Market Rate", System.currentTimeMillis()),
        ExchangeRate("CAD", "NGN", 1100.0, "Default Market Rate", System.currentTimeMillis())
    )

    fun normalizeCurrencyCode(code: String): String {
        val trimmed = code.trim().uppercase()
        return when (trimmed) {
            "₦" -> "NGN"
            "$" -> "USD"
            "£" -> "GBP"
            "€" -> "EUR"
            else -> if (trimmed.isNotBlank()) trimmed else "NGN"
        }
    }

    fun getCurrencyName(code: String): String = when (normalizeCurrencyCode(code)) {
        "NGN" -> "Nigerian Naira"
        "USD" -> "US Dollar"
        "GBP" -> "British Pound"
        "EUR" -> "Euro"
        "CAD" -> "Canadian Dollar"
        else -> code
    }

    fun format(amount: Double, currency: String): String {
        val cleanCode = normalizeCurrencyCode(currency)
        val symbol = when (cleanCode) {
            "NGN" -> "₦"
            "USD" -> "$"
            "GBP" -> "£"
            "EUR" -> "€"
            "CAD" -> "CA$"
            else -> "$cleanCode "
        }
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }
        return "$symbol${formatter.format(amount)}"
    }

    fun formatConverted(amount: Double, currency: String): String = format(amount, currency)

    fun formatRateValue(rate: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 4
        }
        return formatter.format(rate)
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    sealed class ConversionResult {
        data class Success(
            val originalAmount: Double,
            val originalCurrency: String,
            val convertedAmount: Double,
            val targetCurrency: String,
            val rate: Double,
            val rateDate: Long,
            val pairLabel: String
        ) : ConversionResult() {
            val rateUsed: Double get() = rate
        }

        data class MissingRate(
            val fromCurrency: String,
            val toCurrency: String
        ) : ConversionResult()
    }

    data class RateUsageInfo(
        val fromCurrency: String,
        val toCurrency: String,
        val rate: Double
    )

    data class NormalizedFundingResult(
        val totalAmount: Double,
        val unconvertedCount: Int,
        val unconvertedCurrencies: List<String>,
        val ratesUsed: List<RateUsageInfo>
    )

    fun convert(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        customRates: List<ExchangeRate>
    ): ConversionResult {
        val from = normalizeCurrencyCode(fromCurrency)
        val to = normalizeCurrencyCode(toCurrency)
        if (from == to) {
            return ConversionResult.Success(amount, from, amount, to, 1.0, System.currentTimeMillis(), "1:1")
        }
        val rateList = if (customRates.isNotEmpty()) customRates else DEFAULT_RATES
        val direct = rateList.firstOrNull { it.fromCurrency == from && it.toCurrency == to }
        if (direct != null && direct.rate > 0.0) {
            return ConversionResult.Success(
                amount, from, amount * direct.rate, to, direct.rate, direct.lastUpdated,
                "1 $from = ${formatRateValue(direct.rate)} $to"
            )
        }
        val reverse = rateList.firstOrNull { it.fromCurrency == to && it.toCurrency == from }
        if (reverse != null && reverse.rate > 0.0) {
            val effRate = 1.0 / reverse.rate
            return ConversionResult.Success(
                amount, from, amount * effRate, to, effRate, reverse.lastUpdated,
                "1 $from = ${formatRateValue(effRate)} $to"
            )
        }
        return ConversionResult.MissingRate(from, to)
    }

    fun calculateNormalizedFunding(
        fundingByCurrency: Map<String, Double>,
        targetCurrency: String,
        exchangeRates: List<ExchangeRate>
    ): Double {
        val target = normalizeCurrencyCode(targetCurrency)
        var total = 0.0
        for ((curr, amt) in fundingByCurrency) {
            val result = convert(amt, curr, target, exchangeRates)
            if (result is ConversionResult.Success) {
                total += result.convertedAmount
            }
        }
        return total
    }

    fun calculateNormalizedFunding(
        scholarships: List<com.deehem.gpawhiz.data.Scholarship>,
        targetCurrency: String,
        rates: List<ExchangeRate>,
        awardedOnly: Boolean
    ): NormalizedFundingResult {
        val target = normalizeCurrencyCode(targetCurrency)
        var total = 0.0
        var unconverted = 0
        val unconvertedSet = mutableSetOf<String>()
        val ratesUsed = mutableListOf<RateUsageInfo>()

        for (s in scholarships) {
            val amt = s.effectiveAmount
            val curr = s.effectiveCurrency
            if (amt <= 0.0) continue
            val result = convert(amt, curr, target, rates)
            if (result is ConversionResult.Success) {
                total += result.convertedAmount
                if (curr != target) {
                    ratesUsed.add(RateUsageInfo(curr, target, result.rate))
                }
            } else {
                unconverted++
                unconvertedSet.add(curr)
            }
        }
        return NormalizedFundingResult(
            totalAmount = total,
            unconvertedCount = unconverted,
            unconvertedCurrencies = unconvertedSet.toList(),
            ratesUsed = ratesUsed.distinctBy { "${it.fromCurrency}-${it.toCurrency}" }
        )
    }
}
