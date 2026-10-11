package com.bryantgiorgini.saldaria.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class AmortizationCalculatorTest {

    private fun bd(value: String): BigDecimal = BigDecimal(value).setScale(2, RoundingMode.HALF_UP)
    
    private val defaultPrincipal = bd("100000.00")
    private val defaultRate = bd("3.00")
    private val defaultMonths = 300
    private val defaultTerms = LoanTerms(defaultPrincipal, defaultRate, defaultMonths)

    @Test
    fun `cuota 474,21`() {
        val payment = monthlyPayment(defaultPrincipal, defaultRate, defaultMonths)
        assertEquals(bd("474.21"), payment)
    }

    @Test
    fun `fila 1 = 474,21, 250,00, 224,21, saldo 99775,79`() {
        val schedule = buildSchedule(defaultTerms)
        val row1 = schedule.rowOrNull(1)
        assertNotNull(row1)
        assertEquals(bd("474.21"), row1!!.installment)
        assertEquals(bd("250.00"), row1.interest)
        assertEquals(bd("224.21"), row1.principalPaid)
        assertEquals(bd("0.00"), row1.extraPayment)
        assertEquals(bd("99775.79"), row1.balance)
    }

    @Test
    fun `300 filas con saldo final 0,00 y ultima cuota 474,70`() {
        val schedule = buildSchedule(defaultTerms)
        assertEquals(300, schedule.months)
        val row300 = schedule.rowOrNull(300)
        assertNotNull(row300)
        assertEquals(bd("0.00"), row300!!.balance)
        assertEquals(bd("474.70"), row300.installment)
    }
    
    @Test
    fun `totalInterest 42263,49 y totalPaid 142263,49`() {
        val schedule = buildSchedule(defaultTerms)
        assertEquals(bd("42263.49"), schedule.totalInterest)
        assertEquals(bd("142263.49"), schedule.totalPaid)
    }

    @Test
    fun `tipo 0 por ciento (12000 a 12 meses, cuota 1000,00, intereses 0,00)`() {
        val zeroRateTerms = LoanTerms(bd("12000.00"), bd("0.00"), 12)
        val schedule = buildSchedule(zeroRateTerms)
        assertEquals(bd("1000.00"), monthlyPayment(zeroRateTerms.principal, zeroRateTerms.annualRatePercent, zeroRateTerms.months))
        assertEquals(12, schedule.months)
        assertEquals(bd("0.00"), schedule.totalInterest)
        assertEquals(bd("12000.00"), schedule.totalPaid)
    }

    @Test
    fun `todos los importes con escala 2`() {
        val schedule = buildSchedule(defaultTerms)
        for (row in schedule.rows) {
            assertEquals(2, row.installment.scale())
            assertEquals(2, row.interest.scale())
            assertEquals(2, row.principalPaid.scale())
            assertEquals(2, row.extraPayment.scale())
            assertEquals(2, row.balance.scale())
        }
    }

    @Test
    fun `REDUCE_TERM con 20000 en el mes 12, 222 meses e interestSaved 17207,90`() {
        val prepayments = listOf(Prepayment(12, bd("20000.00"), PrepaymentMode.REDUCE_TERM))
        val comparison = comparePrepayment(defaultTerms, prepayments)
        val prepaySchedule = comparison.withPrepayment

        assertEquals(222, prepaySchedule.months)
        assertEquals(bd("17207.90"), comparison.interestSaved)
    }

    @Test
    fun `REDUCE_PAYMENT, 300 meses, cuota 376,71 en el mes 13 e interestSaved 8080,77`() {
        val prepayments = listOf(Prepayment(12, bd("20000.00"), PrepaymentMode.REDUCE_PAYMENT))
        val comparison = comparePrepayment(defaultTerms, prepayments)
        val prepaySchedule = comparison.withPrepayment

        assertEquals(300, prepaySchedule.months)
        val row13 = prepaySchedule.rowOrNull(13)!!
        assertEquals(bd("376.71"), row13.installment)
        assertEquals(bd("8080.77"), comparison.interestSaved)
    }

    @Test
    fun `amortizacion mayor que el saldo liquida el prestamo`() {
        val prepayments = listOf(Prepayment(12, bd("500000.00"), PrepaymentMode.REDUCE_TERM))
        val schedule = buildSchedule(defaultTerms, prepayments)
        
        assertEquals(12, schedule.months)
        val lastRow = schedule.rowOrNull(12)!!
        assertEquals(bd("0.00"), lastRow.balance)
        // La amortización aplicada es exactamente lo que quedaba de saldo
        assertEquals(bd("97272.18"), lastRow.extraPayment)
    }

    @Test
    fun `TAE 3,04`() {
        assertEquals(bd("3.04"), defaultTerms.effectiveAnnualRatePercent())
    }

    @Test
    fun `rowOrNull(300) no es null y rowOrNull(301) y rowOrNull(0) si`() {
        val schedule = buildSchedule(defaultTerms)
        assertNotNull(schedule.rowOrNull(300))
        assertNull(schedule.rowOrNull(301))
        assertNull(schedule.rowOrNull(0))
    }

    @Test
    fun `IllegalArgumentException con capital 0, tipo negativo, plazo 0, Prepayment con mes 0, con importe 0 y en el mes 300`() {
        assertThrows(IllegalArgumentException::class.java) {
            LoanTerms(bd("0.00"), bd("3.00"), 300)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LoanTerms(bd("100000.00"), bd("-1.00"), 300)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LoanTerms(bd("100000.00"), bd("3.00"), 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Prepayment(0, bd("1000.00"), PrepaymentMode.REDUCE_TERM)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Prepayment(12, bd("0.00"), PrepaymentMode.REDUCE_TERM)
        }
        assertThrows(IllegalArgumentException::class.java) {
            buildSchedule(defaultTerms, listOf(Prepayment(300, bd("1000.00"), PrepaymentMode.REDUCE_TERM)))
        }
    }
}
