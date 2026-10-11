package com.bryantgiorgini.saldaria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bryantgiorgini.saldaria.data.EcbRepository
import com.bryantgiorgini.saldaria.domain.AmortizationSchedule
import com.bryantgiorgini.saldaria.domain.LoanTerms
import com.bryantgiorgini.saldaria.domain.Prepayment
import com.bryantgiorgini.saldaria.domain.PrepaymentComparison
import com.bryantgiorgini.saldaria.domain.PrepaymentMode
import com.bryantgiorgini.saldaria.domain.buildSchedule
import com.bryantgiorgini.saldaria.domain.comparePrepayment
import com.bryantgiorgini.saldaria.domain.effectiveAnnualRatePercent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

private const val TAG = "Saldaria"

sealed interface EcbUiState {
    object Loading : EcbUiState
    data class Success(val rate: BigDecimal) : EcbUiState
    data class Error(val message: String?) : EcbUiState
}

enum class InputError {
    INVALID_FORMAT,
    INVALID_PRINCIPAL,
    INVALID_RATE,
    INVALID_MONTHS,
    INVALID_PREPAYMENT_MONTH,
    INVALID_PREPAYMENT_AMOUNT,
    PREPAYMENT_TOO_LATE,
    INVALID_PREPAYMENT_GENERIC,
    INVALID_PREPAYMENT_MONTH_FORMAT,
    INVALID_PREPAYMENT_AMOUNT_FORMAT
}

data class UiState(
    val principalInput: String = "100000",
    val rateInput: String = "3.00",
    val yearsInput: String = "25",
    val prepaymentMonthInput: String = "",
    val prepaymentAmountInput: String = "",
    val prepaymentMode: PrepaymentMode = PrepaymentMode.REDUCE_TERM,
    
    val baseSchedule: AmortizationSchedule? = null,
    val prepaymentComparison: PrepaymentComparison? = null,
    val effectiveRate: BigDecimal? = null,
    
    val inputError: InputError? = null,
    val prepaymentError: InputError? = null,
    
    val ecbState: EcbUiState = EcbUiState.Loading
)

class SimulatorViewModel(
    private val ecbRepository: EcbRepository = EcbRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        fetchEcbRate()
    }

    fun fetchEcbRate() {
        _uiState.update { it.copy(ecbState = EcbUiState.Loading) }
        viewModelScope.launch {
            val result = ecbRepository.getLatestRate()
            result.onSuccess { rate ->
                _uiState.update { it.copy(ecbState = EcbUiState.Success(rate)) }
            }.onFailure { e ->
                Log.e(TAG, "Error fetching ECB rate", e)
                _uiState.update { it.copy(ecbState = EcbUiState.Error(e.message)) }
            }
        }
    }

    fun useEcbRateAsTin() {
        val currentState = _uiState.value.ecbState
        if (currentState is EcbUiState.Success) {
            updateRate(currentState.rate.toString())
        }
    }

    fun updatePrincipal(value: String) = _uiState.update { it.copy(principalInput = value) }
    fun updateRate(value: String) = _uiState.update { it.copy(rateInput = value) }
    fun updateYears(value: String) = _uiState.update { it.copy(yearsInput = value) }
    
    fun updatePrepaymentMonth(value: String) = _uiState.update { it.copy(prepaymentMonthInput = value) }
    fun updatePrepaymentAmount(value: String) = _uiState.update { it.copy(prepaymentAmountInput = value) }
    fun updatePrepaymentMode(mode: PrepaymentMode) = _uiState.update { it.copy(prepaymentMode = mode) }

    fun calculate() {
        val state = _uiState.value
        val principal = state.principalInput.replace(",", ".").toBigDecimalOrNull()
        val rate = state.rateInput.replace(",", ".").toBigDecimalOrNull()
        val years = state.yearsInput.replace(",", ".").toIntOrNull()

        if (principal == null || rate == null || years == null) {
            _uiState.update { it.copy(inputError = InputError.INVALID_FORMAT, baseSchedule = null, prepaymentComparison = null, effectiveRate = null) }
            return
        }

        if (principal <= BigDecimal.ZERO) {
            _uiState.update { it.copy(inputError = InputError.INVALID_PRINCIPAL, baseSchedule = null, prepaymentComparison = null, effectiveRate = null) }
            return
        }
        if (rate < BigDecimal.ZERO) {
            _uiState.update { it.copy(inputError = InputError.INVALID_RATE, baseSchedule = null, prepaymentComparison = null, effectiveRate = null) }
            return
        }
        if (years <= 0) {
            _uiState.update { it.copy(inputError = InputError.INVALID_MONTHS, baseSchedule = null, prepaymentComparison = null, effectiveRate = null) }
            return
        }

        try {
            Log.d(TAG, "Calculando préstamo: Capital=$principal, TIN=$rate%, Plazo=$years años")
            val months = years * 12
            val terms = LoanTerms(principal, rate, months)
            val schedule = buildSchedule(terms)
            
            // Recalcular la comparación si hay datos válidos de amortización anticipada
            val comparison = tryCalculateComparison(terms, state)

            Log.d(TAG, "Cálculo finalizado: ${schedule.months} meses, Cuota mensual=${schedule.rows.firstOrNull()?.installment}")
            
            _uiState.update { 
                it.copy(
                    baseSchedule = schedule,
                    effectiveRate = terms.effectiveAnnualRatePercent(),
                    prepaymentComparison = comparison,
                    inputError = null,
                    prepaymentError = null
                )
            }
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(inputError = InputError.INVALID_FORMAT, baseSchedule = null, prepaymentComparison = null, effectiveRate = null) }
        }
    }
    
    fun calculatePrepayment() {
        calculate() 
        val state = _uiState.value
        
        val prepayMonth = state.prepaymentMonthInput.replace(",", ".").toIntOrNull()
        val prepayAmount = state.prepaymentAmountInput.replace(",", ".").toBigDecimalOrNull()
        
        if (prepayMonth == null) {
            _uiState.update { it.copy(prepaymentError = InputError.INVALID_PREPAYMENT_MONTH_FORMAT, prepaymentComparison = null) }
            return
        }
        if (prepayAmount == null) {
            _uiState.update { it.copy(prepaymentError = InputError.INVALID_PREPAYMENT_AMOUNT_FORMAT, prepaymentComparison = null) }
            return
        }
        if (prepayMonth < 1) {
            _uiState.update { it.copy(prepaymentError = InputError.INVALID_PREPAYMENT_MONTH, prepaymentComparison = null) }
            return
        }
        if (prepayAmount <= BigDecimal.ZERO) {
            _uiState.update { it.copy(prepaymentError = InputError.INVALID_PREPAYMENT_AMOUNT, prepaymentComparison = null) }
            return
        }
        
        val baseMonths = state.baseSchedule?.months ?: ((state.yearsInput.replace(",", ".").toIntOrNull() ?: 0) * 12)
        if (prepayMonth >= baseMonths) {
            _uiState.update { it.copy(prepaymentError = InputError.PREPAYMENT_TOO_LATE, prepaymentComparison = null) }
            return
        }

        if (state.baseSchedule != null && state.prepaymentComparison == null) {
            _uiState.update { it.copy(prepaymentError = InputError.INVALID_PREPAYMENT_GENERIC, prepaymentComparison = null) }
        }
    }

    private fun tryCalculateComparison(terms: LoanTerms, state: UiState): PrepaymentComparison? {
        val prepayMonth = state.prepaymentMonthInput.toIntOrNull()
        val prepayAmount = state.prepaymentAmountInput.toBigDecimalOrNull()
        
        if (prepayMonth == null || prepayAmount == null) return null
        if (prepayMonth < 1 || prepayAmount <= BigDecimal.ZERO || prepayMonth >= terms.months) return null
        
        return try {
            val prepayment = Prepayment(prepayMonth, prepayAmount, state.prepaymentMode)
            comparePrepayment(terms, listOf(prepayment))
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
