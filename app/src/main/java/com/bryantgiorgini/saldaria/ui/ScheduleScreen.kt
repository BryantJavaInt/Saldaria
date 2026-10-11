package com.bryantgiorgini.saldaria.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bryantgiorgini.saldaria.R
import com.bryantgiorgini.saldaria.domain.AmortizationRow
import com.bryantgiorgini.saldaria.ui.theme.SaldariaTheme
import com.bryantgiorgini.saldaria.ui.theme.TableTypography
import com.bryantgiorgini.saldaria.ui.theme.customColors
import com.bryantgiorgini.saldaria.viewmodel.UiState
import java.math.BigDecimal

@Composable
fun ScheduleScreen(
    state: UiState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        val rows = state.prepaymentComparison?.withPrepayment?.rows ?: state.baseSchedule?.rows ?: emptyList()

        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.error_empty_schedule),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AmortizationChartSection(rows = rows)
            
            // Agrupar filas por año (cada 12 meses)
            val rowsByYear = rows.groupBy { (it.number - 1) / 12 + 1 }
            
            // Recordar el estado de expansión de cada año (por defecto el año 1 abierto)
            val expandedYears = rememberSaveable(
                saver = Saver(
                    save = { it.toList() },
                    restore = { mutableStateMapOf(*it.toTypedArray()) }
                )
            ) {
                mutableStateMapOf(1 to true)
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                rowsByYear.forEach { (year, yearRows) ->
                    item {
                        val isExpanded = expandedYears[year] == true
                        val yearInterest = yearRows.sumOf { it.interest }
                        val yearPrincipal = yearRows.sumOf { it.principalPaid + it.extraPayment }
                        val yearBalance = yearRows.last().balance
                        
                        YearHeaderItem(
                            year = year,
                            interest = yearInterest,
                            principal = yearPrincipal,
                            balance = yearBalance,
                            isExpanded = isExpanded,
                            onClick = {
                                expandedYears[year] = !isExpanded
                            }
                        )
                    }
                    
                    if (expandedYears[year] == true) {
                        item {
                            ScheduleHeader()
                        }
                        items(yearRows) { row ->
                            ScheduleRowItem(row = row)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearHeaderItem(
    year: Int,
    interest: BigDecimal,
    principal: BigDecimal,
    balance: BigDecimal,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.schedule_year_header, year),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Icono simulado con texto (▲/▼) para no usar librerías externas
            Text(
                text = if (isExpanded) "▲" else "▼",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.result_label_interest), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)).background(MaterialTheme.customColors.interest))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = interest.formatCurrency(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.result_label_principal), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)).background(MaterialTheme.customColors.capital))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = principal.formatCurrency(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(text = stringResource(R.string.table_header_balance), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = balance.formatCurrency(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun AmortizationChartSection(rows: List<AmortizationRow>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.schedule_chart_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.schedule_chart_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        val firstRow = rows.first()
        val lastRow = rows.last()
        val chartDescription = stringResource(R.string.schedule_chart_content_description)

        AmortizationChart(
            rows = rows,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .semantics {
                    contentDescription = chartDescription
                }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.schedule_chart_legend_first_month),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).background(MaterialTheme.customColors.interest))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.schedule_chart_legend_interest_amount, firstRow.interest.formatCurrency()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.schedule_chart_legend_last_month, lastRow.number),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).background(MaterialTheme.customColors.interest))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.schedule_chart_legend_interest_amount, lastRow.interest.formatCurrency()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AmortizationChart(rows: List<AmortizationRow>, modifier: Modifier = Modifier) {
    val capitalColor = MaterialTheme.customColors.capital
    val interestColor = MaterialTheme.customColors.interest
    val textMeasurer = rememberTextMeasurer()
    val labelInterest = stringResource(R.string.schedule_chart_label_interest)
    val labelCapital = stringResource(R.string.schedule_chart_label_capital)
    val colorInk = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))) {
        val width = size.width
        val height = size.height

        val maxInstallment = rows.maxOfOrNull { it.installment.toFloat() } ?: 1f
        val pointCount = rows.size
        val xStep = if (pointCount > 1) width / (pointCount - 1) else width

        // Fondo (Intereses)
        drawRect(
            color = interestColor,
            size = size
        )

        // Capital (Área inferior superpuesta)
        val capitalPath = Path().apply {
            moveTo(0f, height) // Esquina inferior izquierda

            rows.forEachIndexed { index, row ->
                val x = index * xStep
                // El capital crece, así que mapeamos desde el total de la cuota hacia abajo
                val capitalRatio = row.principalPaid.toFloat() / maxInstallment
                val y = height - (height * capitalRatio)
                lineTo(x, y)
            }

            lineTo(width, height) // Esquina inferior derecha
            close()
        }

        drawPath(
            path = capitalPath,
            color = capitalColor
        )
        
        drawText(
            textMeasurer = textMeasurer,
            text = labelInterest,
            style = TextStyle(color = colorInk, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            topLeft = Offset(16.dp.toPx(), 16.dp.toPx())
        )
        drawText(
            textMeasurer = textMeasurer,
            text = labelCapital,
            style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            topLeft = Offset(16.dp.toPx(), height - 32.dp.toPx())
        )
    }
}

@Composable
private fun ScheduleHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(R.string.table_header_month), modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        Text(stringResource(R.string.table_header_installment), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall)
        Text(stringResource(R.string.table_header_interest), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall)
        Text(stringResource(R.string.table_header_principal), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall)
        Text(stringResource(R.string.table_header_balance), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ScheduleRowItem(row: AmortizationRow) {
    val isPrepayment = row.extraPayment > BigDecimal.ZERO
    val bgColor = if (isPrepayment) MaterialTheme.customColors.activeFill else MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(row.number.toString(), modifier = Modifier.weight(0.5f), color = textColor, style = TableTypography)
            Text(row.installment.formatCurrency(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = textColor, style = TableTypography)
            Text(row.interest.formatCurrency(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = textColor, style = TableTypography)
            Text(row.principalPaid.formatCurrency(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = textColor, style = TableTypography)
            Text(row.balance.formatCurrency(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = textColor, style = TableTypography)
        }
        if (isPrepayment) {
            Text(
                text = stringResource(R.string.table_row_prepayment, row.extraPayment.formatCurrency()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.customColors.capital,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScheduleScreenPreview() {
    SaldariaTheme {
        ScheduleScreen(state = UiState())
    }
}
