package com.bryantgiorgini.saldaria.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

private const val MONEY_SCALE = 2
private val MONTHS_IN_YEAR = BigDecimal(12)
private val ONE_HUNDRED = BigDecimal(100)

/** Extension function: redondea un importe a céntimos (2 decimales, mitad hacia arriba). */
fun BigDecimal.money(): BigDecimal = setScale(MONEY_SCALE, RoundingMode.HALF_UP)

enum class PrepaymentMode {
    /** Mantiene la cuota y el préstamo termina antes. */
    REDUCE_TERM,
    /** Mantiene la fecha final y baja la cuota. */
    REDUCE_PAYMENT,
}

/** Condiciones del préstamo. El tipo se expresa en % anual (TIN), p. ej. 3.00. */
data class LoanTerms(
    val principal: BigDecimal,
    val annualRatePercent: BigDecimal,
    val months: Int,
) {
    init {
        require(principal.signum() > 0) { "El capital debe ser mayor que 0" }
        require(annualRatePercent.signum() >= 0) { "El tipo de interés no puede ser negativo" }
        require(months > 0) { "El plazo debe ser de al menos 1 mes" }
    }
}

/** Amortización anticipada: se aplica justo después de pagar la cuota del mes [afterMonth]. */
data class Prepayment(
    val afterMonth: Int,
    val amount: BigDecimal,
    val mode: PrepaymentMode,
) {
    init {
        require(afterMonth >= 1) { "El mes de la amortización debe ser 1 o mayor" }
        require(amount.signum() > 0) { "El importe a amortizar debe ser mayor que 0" }
    }
}

data class AmortizationRow(
    val number: Int,
    val installment: BigDecimal,
    val interest: BigDecimal,
    val principalPaid: BigDecimal,
    val extraPayment: BigDecimal,
    val balance: BigDecimal,
)

data class AmortizationSchedule(
    val terms: LoanTerms,
    val rows: List<AmortizationRow>,
) {
    val months: Int get() = rows.size
    val totalInterest: BigDecimal get() = rows.sumOf { it.interest }
    val totalPaid: BigDecimal get() = rows.sumOf { it.installment + it.extraPayment }
}

data class PrepaymentComparison(
    val base: AmortizationSchedule,
    val withPrepayment: AmortizationSchedule,
) {
    val interestSaved: BigDecimal get() = base.totalInterest - withPrepayment.totalInterest
    val monthsSaved: Int get() = base.months - withPrepayment.months
}

/** Null safety: devuelve null si el mes no existe en el cuadro. */
fun AmortizationSchedule.rowOrNull(month: Int): AmortizationRow? =
    rows.firstOrNull { it.number == month }

private fun BigDecimal.toMonthlyRate(): BigDecimal =
    divide(ONE_HUNDRED.multiply(MONTHS_IN_YEAR), MathContext.DECIMAL128)

private fun paymentFor(balance: BigDecimal, monthlyRate: BigDecimal, months: Int): BigDecimal {
    if (monthlyRate.signum() == 0) {
        return balance.divide(BigDecimal(months), MONEY_SCALE, RoundingMode.HALF_UP)
    }
    val growth = BigDecimal.ONE.add(monthlyRate).pow(months, MathContext.DECIMAL128)
    return balance
        .multiply(monthlyRate)
        .multiply(growth)
        .divide(growth.subtract(BigDecimal.ONE), MathContext.DECIMAL128)
        .money()
}

/** Cuota mensual del sistema francés. */
fun monthlyPayment(principal: BigDecimal, annualRatePercent: BigDecimal, months: Int): BigDecimal {
    require(principal.signum() > 0) { "El capital debe ser mayor que 0" }
    require(annualRatePercent.signum() >= 0) { "El tipo de interés no puede ser negativo" }
    require(months > 0) { "El plazo debe ser de al menos 1 mes" }
    return paymentFor(principal.money(), annualRatePercent.toMonthlyRate(), months)
}

/** TAE aproximada a partir del TIN (capitalización mensual). No incluye comisiones ni gastos vinculados. */
fun LoanTerms.effectiveAnnualRatePercent(): BigDecimal =
    BigDecimal.ONE.add(annualRatePercent.toMonthlyRate())
        .pow(12, MathContext.DECIMAL128)
        .subtract(BigDecimal.ONE)
        .multiply(ONE_HUNDRED)
        .setScale(MONEY_SCALE, RoundingMode.HALF_UP)

/** Cuadro de amortización, con amortizaciones anticipadas opcionales. */
fun buildSchedule(
    terms: LoanTerms,
    prepayments: List<Prepayment> = emptyList(),
): AmortizationSchedule {
    require(prepayments.all { it.afterMonth < terms.months }) {
        "La amortización anticipada debe ser antes del último mes del préstamo"
    }
    val prepaymentsByMonth = prepayments.groupBy { it.afterMonth }
    val monthlyRate = terms.annualRatePercent.toMonthlyRate()

    var balance = terms.principal.money()
    var payment = paymentFor(balance, monthlyRate, terms.months)
    val rows = mutableListOf<AmortizationRow>()
    var month = 0

    while (balance.signum() > 0) {
        month++
        val interest = balance.multiply(monthlyRate).money()
        // La última cuota (o la que ya cubre todo el capital) se ajusta para dejar el saldo a cero.
        val clearsBalance = month == terms.months || payment - interest >= balance
        val principalPaid = if (clearsBalance) balance else payment - interest
        val installment = principalPaid + interest
        balance -= principalPaid

        var extra = BigDecimal.ZERO.money()
        for (prepayment in prepaymentsByMonth[month].orEmpty()) {
            if (balance.signum() == 0) break
            val applied = prepayment.amount.money().min(balance)
            extra += applied
            balance -= applied
            if (prepayment.mode == PrepaymentMode.REDUCE_PAYMENT && balance.signum() > 0) {
                payment = paymentFor(balance, monthlyRate, terms.months - month)
            }
        }

        rows += AmortizationRow(month, installment, interest, principalPaid, extra, balance)
    }
    return AmortizationSchedule(terms, rows)
}

/** Compara el préstamo sin amortizar con el mismo préstamo con amortizaciones anticipadas. */
fun comparePrepayment(terms: LoanTerms, prepayments: List<Prepayment>): PrepaymentComparison =
    PrepaymentComparison(
        base = buildSchedule(terms),
        withPrepayment = buildSchedule(terms, prepayments),
    )
