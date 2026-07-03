package com.hansbarrera.aditivosaforo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.hansbarrera.aditivosaforo.calc.Formulas
import com.hansbarrera.aditivosaforo.calc.NivelVeredicto
import com.hansbarrera.aditivosaforo.ui.theme.VerdictBad
import com.hansbarrera.aditivosaforo.ui.theme.VerdictOk
import com.hansbarrera.aditivosaforo.ui.theme.VerdictWarn

/**
 * Barra horizontal de desviación: escala [minPct]..[maxPct] con banda verde
 * (±tolerancia), banda ámbar (±2·tolerancia) y una aguja en la desviación
 * medida (acotada a la escala). Sin desviación válida, la aguja queda al centro.
 */
@Composable
fun DeviationGauge(
    desviacionPct: Double?,
    toleranciaPct: Double,
    modifier: Modifier = Modifier,
    minPct: Double = -20.0,
    maxPct: Double = 20.0
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val tickColor = MaterialTheme.colorScheme.outline
    val needleColor = when {
        desviacionPct == null || !desviacionPct.isFinite() -> tickColor
        else -> when (Formulas.nivelVeredicto(desviacionPct, toleranciaPct)) {
            NivelVeredicto.OK -> VerdictOk
            NivelVeredicto.ADVERTENCIA -> VerdictWarn
            else -> VerdictBad
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .padding(horizontal = 2.dp)
        ) {
            val rango = maxPct - minPct
            fun xDe(pct: Double): Float {
                val acotado = pct.coerceIn(minPct, maxPct)
                return (((acotado - minPct) / rango) * size.width).toFloat()
            }

            val radio = CornerRadius(size.height / 2f, size.height / 2f)
            drawRoundRect(color = trackColor, cornerRadius = radio)

            if (toleranciaPct > 0.0) {
                val ambarIni = xDe(-toleranciaPct * 2.0)
                val ambarFin = xDe(toleranciaPct * 2.0)
                drawRect(
                    color = VerdictWarn.copy(alpha = 0.30f),
                    topLeft = Offset(ambarIni, 0f),
                    size = Size(ambarFin - ambarIni, size.height)
                )
                val verdeIni = xDe(-toleranciaPct)
                val verdeFin = xDe(toleranciaPct)
                drawRect(
                    color = VerdictOk.copy(alpha = 0.35f),
                    topLeft = Offset(verdeIni, 0f),
                    size = Size(verdeFin - verdeIni, size.height)
                )
            }

            // Marca del cero.
            val xCero = xDe(0.0)
            drawLine(
                color = tickColor,
                start = Offset(xCero, 0f),
                end = Offset(xCero, size.height),
                strokeWidth = 2f
            )

            // Aguja en la desviación medida (al centro si no hay dato).
            val xAguja = if (desviacionPct != null && desviacionPct.isFinite()) xDe(desviacionPct) else xCero
            drawLine(
                color = needleColor,
                start = Offset(xAguja, 0f),
                end = Offset(xAguja, size.height),
                strokeWidth = 8f
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, start = 2.dp, end = 2.dp)
        ) {
            Text(
                "${formatNumber(minPct, 0)}%",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f)
            )
            Text(
                "0",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                "+${formatNumber(maxPct, 0)}%",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}
