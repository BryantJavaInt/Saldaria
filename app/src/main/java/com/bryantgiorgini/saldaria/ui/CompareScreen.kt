package com.bryantgiorgini.saldaria.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bryantgiorgini.saldaria.R
import com.bryantgiorgini.saldaria.viewmodel.InputError
import com.bryantgiorgini.saldaria.domain.PrepaymentComparison
import com.bryantgiorgini.saldaria.domain.PrepaymentMode
import com.bryantgiorgini.saldaria.domain.rowOrNull
import com.bryantgiorgini.saldaria.ui.theme.SaldariaTheme
import com.bryantgiorgini.saldaria.ui.errorMessageRes
import com.bryantgiorgini.saldaria.ui.theme.customColors
import com.bryantgiorgini.saldaria.viewmodel.UiState

@Composable
fun CompareScreen(
    state: UiState,
    onMonthChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onModeChange: (PrepaymentMode) -> Unit,
    onCalculate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (state.baseSchedule == null) {
            Text(
                text = stringResource(R.string.error_empty_compare),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        val isMonthError = state.prepaymentError in listOf(InputError.INVALID_PREPAYMENT_MONTH, InputError.PREPAYMENT_TOO_LATE, InputError.INVALID_PREPAYMENT_MONTH_FORMAT, InputError.INVALID_PREPAYMENT_GENERIC, InputError.INVALID_FORMAT)
        val isAmountError = state.prepaymentError in listOf(InputError.INVALID_PREPAYMENT_AMOUNT, InputError.INVALID_PREPAYMENT_AMOUNT_FORMAT, InputError.INVALID_FORMAT)

        // --- ENTRADA DE DATOS ---
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = stringResource(R.string.compare_title_inputs), style = MaterialTheme.typography.titleMedium)
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = state.prepaymentMonthInput,
                    onValueChange = onMonthChange,
                    label = { Text(stringResource(R.string.input_prepayment_month)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    isError = isMonthError,
                    supportingText = if (isMonthError) { state.prepaymentError?.let { { Text(stringResource(it.errorMessageRes())) } } } else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.prepaymentAmountInput,
                    onValueChange = onAmountChange,
                    label = { Text(stringResource(R.string.input_prepayment_amount)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    isError = isAmountError,
                    supportingText = if (isAmountError && !isMonthError) { state.prepaymentError?.let { { Text(stringResource(it.errorMessageRes())) } } } else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )
            }
        }

        // Selector segmentado
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.compare_prepayment_question), style = MaterialTheme.typography.titleMedium)
            
            Row(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                SegmentedButton(
                    text = stringResource(R.string.option_reduce_term),
                    selected = state.prepaymentMode == PrepaymentMode.REDUCE_TERM,
                    onClick = { onModeChange(PrepaymentMode.REDUCE_TERM) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                SegmentedButton(
                    text = stringResource(R.string.option_reduce_payment),
                    selected = state.prepaymentMode == PrepaymentMode.REDUCE_PAYMENT,
                    onClick = { onModeChange(PrepaymentMode.REDUCE_PAYMENT) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Button(
            onClick = onCalculate,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.action_calculate))
        }

        // --- RESULTADOS ---
        if (state.prepaymentComparison != null) {
            val comparison = state.prepaymentComparison
            val preMonth = state.prepaymentMonthInput.toIntOrNull() ?: 1
            
            // Leemos la nueva cuota de la fila que sigue a la amortización
            val nextRowBase = comparison.base.rowOrNull(preMonth + 1)
            val nextRowPrepay = comparison.withPrepayment.rowOrNull(preMonth + 1)
            
            ComparisonColumns(
                comparison = comparison, 
                baseInstallment = nextRowBase?.installment?.formatCurrency() ?: "-",
                newInstallment = nextRowPrepay?.installment?.formatCurrency() ?: "-"
            )
            
            SavingsHighlight(comparison = comparison)
            
            ComparisonChart(comparison = comparison)
        } else {
            Text(
                text = stringResource(R.string.compare_empty_state),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun SegmentedButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) MaterialTheme.customColors.activeFill else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = contentColor, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ComparisonColumns(
    comparison: PrepaymentComparison,
    baseInstallment: String,
    newInstallment: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Columna Base
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.compare_column_base), 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            StatItem(label = "Plazo", value = stringResource(R.string.compare_stat_months, comparison.base.months))
            StatItem(label = "Cuota (tras amort.)", value = baseInstallment)
            StatItem(label = "Intereses totales", value = comparison.base.totalInterest.formatCurrency())
        }
        
        // Línea divisoria
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(150.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Columna Amortización
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.compare_column_prepayment), 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            StatItem(label = "Plazo", value = stringResource(R.string.compare_stat_months, comparison.withPrepayment.months))
            StatItem(label = "Cuota (tras amort.)", value = newInstallment)
            StatItem(label = "Intereses totales", value = comparison.withPrepayment.totalInterest.formatCurrency())
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SavingsHighlight(comparison: PrepaymentComparison) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.compare_savings_title), 
            style = MaterialTheme.typography.titleMedium, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = comparison.interestSaved.formatCurrency(), 
            style = MaterialTheme.typography.displayLarge, 
            color = MaterialTheme.customColors.capital,
            textAlign = TextAlign.Center
        )
        if (comparison.monthsSaved > 0) {
            Text(
                text = stringResource(R.string.compare_savings_months, comparison.monthsSaved), 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ComparisonChart(comparison: PrepaymentComparison) {
    val chartDescription = stringResource(R.string.compare_chart_description)
    val baseLineColor = MaterialTheme.colorScheme.outline
    val prepayLineColor = MaterialTheme.customColors.capital
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics { contentDescription = chartDescription }
        ) {
            val width = size.width
            val height = size.height
            
            val maxBalance = comparison.base.rows.firstOrNull()?.balance?.toFloat() ?: 1f
            val maxMonths = comparison.base.months
            val xStep = if (maxMonths > 1) width / (maxMonths - 1) else width
            
            // Función para dibujar la curva
            fun drawBalancePath(rows: List<com.bryantgiorgini.saldaria.domain.AmortizationRow>, lineColor: androidx.compose.ui.graphics.Color, strokeWidth: Float) {
                val path = Path().apply {
                    // Partimos del saldo inicial en y=0 (arriba)
                    moveTo(0f, 0f)
                    rows.forEachIndexed { index, row ->
                        val x = index * xStep
                        val yRatio = 1f - (row.balance.toFloat() / maxBalance) // 0 es arriba, 1 es abajo
                        lineTo(x, height * yRatio)
                    }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
            
            // Dibujar la curva base (gris)
            drawBalancePath(comparison.base.rows, baseLineColor, 4.dp.toPx())
            
            // Dibujar la curva con amortización (verde)
            drawBalancePath(comparison.withPrepayment.rows, prepayLineColor, 6.dp.toPx())
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Leyenda
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(baseLineColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.compare_column_base), style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.width(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(prepayLineColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.compare_column_prepayment), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CompareScreenPreview() {
    SaldariaTheme {
        CompareScreen(
            state = UiState(),
            onMonthChange = {},
            onAmountChange = {},
            onModeChange = {},
            onCalculate = {}
        )
    }
}
