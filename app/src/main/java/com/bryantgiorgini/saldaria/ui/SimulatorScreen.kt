package com.bryantgiorgini.saldaria.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bryantgiorgini.saldaria.R
import com.bryantgiorgini.saldaria.domain.AmortizationSchedule
import com.bryantgiorgini.saldaria.ui.theme.SaldariaTheme
import com.bryantgiorgini.saldaria.ui.theme.customColors
import com.bryantgiorgini.saldaria.viewmodel.EcbUiState
import com.bryantgiorgini.saldaria.viewmodel.UiState
import java.math.BigDecimal

@Composable
fun SimulatorScreen(
    state: UiState,
    onPrincipalChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onYearsChange: (String) -> Unit,
    onCalculate: () -> Unit,
    onRetryEcb: () -> Unit,
    onUseEcbRate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (state.baseSchedule != null) {
            SummaryHeader(state.baseSchedule, state.effectiveRate)
        } else {
            Text(
                text = stringResource(R.string.simulator_empty_state),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
            )
        }

        InputsSection(
            state = state,
            onPrincipalChange = onPrincipalChange,
            onRateChange = onRateChange,
            onYearsChange = onYearsChange,
            onCalculate = onCalculate,
            onRetryEcb = onRetryEcb,
            onUseEcbRate = onUseEcbRate
        )
    }
}

@Composable
private fun SummaryHeader(schedule: AmortizationSchedule, apr: BigDecimal?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tu cuota mensual
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = stringResource(R.string.result_monthly_payment),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val firstRow = schedule.rows.firstOrNull()
            Text(
                text = firstRow?.installment?.formatCurrency() ?: "0,00 €",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Barra partida
        ProportionalBar(
            principal = schedule.terms.principal,
            interest = schedule.totalInterest,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        )

        // Leyendas de la barra
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            LegendItem(
                label = stringResource(R.string.result_label_principal),
                value = schedule.terms.principal.formatCurrency(),
                color = MaterialTheme.customColors.capital,
                modifier = Modifier.weight(1f)
            )
            LegendItem(
                label = stringResource(R.string.result_label_interest),
                value = schedule.totalInterest.formatCurrency(),
                color = MaterialTheme.customColors.interest,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Total pagado y TAE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.result_total_paid),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = schedule.totalPaid.formatCurrency(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        apr?.let {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.result_apr),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = it.formatPercentage(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color))
        Spacer(modifier = Modifier.size(8.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ProportionalBar(principal: BigDecimal, interest: BigDecimal, modifier: Modifier = Modifier) {
    val total = principal + interest
    val principalRatio = if (total > BigDecimal.ZERO) (principal.toFloat() / total.toFloat()) else 0.5f
    val capitalColor = MaterialTheme.customColors.capital
    val interestColor = MaterialTheme.customColors.interest
    val description = "Barra de proporción: ${(principalRatio * 100).toInt()}% capital, ${((1f - principalRatio) * 100).toInt()}% intereses"
    
    Canvas(modifier = modifier
        .clip(RoundedCornerShape(percent = 50))
        .semantics { contentDescription = description }) {
        val width = size.width
        val height = size.height
        val principalWidth = width * principalRatio
        
        drawRect(color = capitalColor, size = androidx.compose.ui.geometry.Size(principalWidth, height))
        drawRect(
            color = interestColor, 
            topLeft = androidx.compose.ui.geometry.Offset(principalWidth, 0f),
            size = androidx.compose.ui.geometry.Size(width - principalWidth, height)
        )
    }
}

@Composable
private fun InputsSection(
    state: UiState,
    onPrincipalChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onYearsChange: (String) -> Unit,
    onCalculate: () -> Unit,
    onRetryEcb: () -> Unit,
    onUseEcbRate: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.principalInput,
            onValueChange = onPrincipalChange,
            label = { Text(stringResource(R.string.input_principal)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            isError = state.inputError != null,
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = state.rateInput,
                onValueChange = onRateChange,
                label = { Text(stringResource(R.string.input_rate)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                isError = state.inputError != null,
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = state.yearsInput,
                onValueChange = onYearsChange,
                label = { Text(stringResource(R.string.input_years)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                isError = state.inputError != null,
                supportingText = state.inputError?.let { { Text(stringResource(it.errorMessageRes())) } },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }
        
        EcbInlineView(ecbState = state.ecbState, onRetry = onRetryEcb, onUseRate = onUseEcbRate)

        Button(
            onClick = onCalculate,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.action_calculate))
        }
    }
}

@Composable
private fun EcbInlineView(ecbState: EcbUiState, onRetry: () -> Unit, onUseRate: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (ecbState) {
            is EcbUiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.ecb_rate_loading), style = MaterialTheme.typography.bodySmall)
                }
            }
            is EcbUiState.Success -> {
                Text(
                    text = stringResource(R.string.ecb_rate_success, ecbState.rate.formatPercentage()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onUseRate,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.customColors.capital)
                ) {
                    Text(
                        text = stringResource(R.string.ecb_action_use),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            is EcbUiState.Error -> {
                Text(
                    text = ecbState.message ?: stringResource(R.string.error_unknown),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.ecb_action_retry))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SimulatorScreenPreview() {
    SaldariaTheme {
        SimulatorScreen(
            state = UiState(),
            onPrincipalChange = {},
            onRateChange = {},
            onYearsChange = {},
            onCalculate = {},
            onRetryEcb = {},
            onUseEcbRate = {}
        )
    }
}
