package com.bryantgiorgini.saldaria.ui

import com.bryantgiorgini.saldaria.R
import com.bryantgiorgini.saldaria.viewmodel.InputError
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/** Formato de moneda en euros, locale de España. */
fun BigDecimal.formatCurrency(): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES"))
    return format.format(this)
}

/** Formato de porcentaje con dos decimales, locale de España. */
fun BigDecimal.formatPercentage(): String {
    val format = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-ES"))
    format.minimumFractionDigits = 2
    format.maximumFractionDigits = 2
    return "${format.format(this)} %"
}

/** Convierte los InputError del dominio a su equivalente en string resources */
fun InputError.errorMessageRes(): Int = when (this) {
    InputError.INVALID_FORMAT,
    InputError.INVALID_PREPAYMENT_MONTH_FORMAT,
    InputError.INVALID_PREPAYMENT_AMOUNT_FORMAT -> R.string.error_invalid_inputs
    InputError.INVALID_PRINCIPAL -> R.string.error_domain_principal
    InputError.INVALID_RATE -> R.string.error_domain_rate
    InputError.INVALID_MONTHS -> R.string.error_domain_months
    InputError.INVALID_PREPAYMENT_MONTH -> R.string.error_domain_prepayment_month
    InputError.INVALID_PREPAYMENT_AMOUNT -> R.string.error_domain_prepayment_amount
    InputError.PREPAYMENT_TOO_LATE -> R.string.error_domain_prepayment_too_late
    InputError.INVALID_PREPAYMENT_GENERIC -> R.string.error_invalid_prepayment
}
