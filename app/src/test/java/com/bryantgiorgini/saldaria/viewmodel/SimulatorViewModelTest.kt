package com.bryantgiorgini.saldaria.viewmodel

import com.bryantgiorgini.saldaria.domain.PrepaymentMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class SimulatorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `calculate - entradas validas actualiza baseSchedule`() {
        val viewModel = SimulatorViewModel()
        
        viewModel.updatePrincipal("100000")
        viewModel.updateRate("3.00")
        viewModel.updateYears("25")
        
        viewModel.calculate()
        
        val state = viewModel.uiState.value
        assertNull(state.inputError)
        assertNotNull(state.baseSchedule)
        assertEquals(300, state.baseSchedule!!.months)
    }

    @Test
    fun `calculate - entradas no validas muestran error`() {
        val viewModel = SimulatorViewModel()
        
        viewModel.updatePrincipal("texto")
        viewModel.updateRate("3.00")
        viewModel.updateYears("25")
        
        viewModel.calculate()
        
        val state = viewModel.uiState.value
        assertNotNull(state.inputError)
        assertEquals(InputError.INVALID_FORMAT, state.inputError)
        assertNull(state.baseSchedule)
    }

    @Test
    fun `calculate - excepciones de dominio se capturan como error de UI`() {
        val viewModel = SimulatorViewModel()
        
        viewModel.updatePrincipal("0")
        viewModel.updateRate("3.00")
        viewModel.updateYears("25")
        
        viewModel.calculate()
        
        val state = viewModel.uiState.value
        assertNotNull(state.inputError)
        assertEquals(InputError.INVALID_PRINCIPAL, state.inputError)
        assertNull(state.baseSchedule)
    }
    
    @Test
    fun `calculatePrepayment - actualiza comparacion con datos validos`() {
        val viewModel = SimulatorViewModel()
        
        viewModel.updatePrincipal("100000")
        viewModel.updateRate("3.00")
        viewModel.updateYears("25")
        
        viewModel.updatePrepaymentMonth("12")
        viewModel.updatePrepaymentAmount("20000")
        viewModel.updatePrepaymentMode(PrepaymentMode.REDUCE_TERM)
        
        viewModel.calculatePrepayment()
        
        val state = viewModel.uiState.value
        assertNull(state.prepaymentError)
        assertNotNull(state.prepaymentComparison)
        
        val comparison = state.prepaymentComparison!!
        assertEquals(222, comparison.withPrepayment.months)
    }

    @Test
    fun `calculate - normaliza coma y punto para decimales`() {
        val viewModelDot = SimulatorViewModel()
        viewModelDot.updatePrincipal("100000")
        viewModelDot.updateRate("2.65")
        viewModelDot.updateYears("25")
        viewModelDot.calculate()

        val viewModelComma = SimulatorViewModel()
        viewModelComma.updatePrincipal("100000")
        viewModelComma.updateRate("2,65")
        viewModelComma.updateYears("25")
        viewModelComma.calculate()

        assertEquals(
            viewModelDot.uiState.value.baseSchedule?.totalInterest,
            viewModelComma.uiState.value.baseSchedule?.totalInterest
        )
        assertNotNull(viewModelComma.uiState.value.baseSchedule)
    }

    @Test
    fun `calculatePrepayment - muestra error con datos fuera de plazo`() {
        val viewModel = SimulatorViewModel()
        
        viewModel.updatePrincipal("100000")
        viewModel.updateRate("3.00")
        viewModel.updateYears("25") // 300 meses
        
        // Amortización en el mes 300, dominio lanzará excepción
        viewModel.updatePrepaymentMonth("300") 
        viewModel.updatePrepaymentAmount("20000")
        
        viewModel.calculatePrepayment()
        
        val state = viewModel.uiState.value
        assertNotNull(state.prepaymentError)
        assertEquals(InputError.PREPAYMENT_TOO_LATE, state.prepaymentError)
        assertNull(state.prepaymentComparison)
    }
}
