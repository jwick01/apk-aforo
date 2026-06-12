package com.hansbarrera.aditivosaforo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.hansbarrera.aditivosaforo.calc.FilaPotenciometro

/**
 * Gráfico de la curva de calibración del potenciómetro (acelerante kg/min en X,
 * posición del potenciómetro en Y), con el punto objetivo resaltado si se indica.
 */
@Composable
fun PotenciometroCurveChart(
    tabla: List<FilaPotenciometro>,
    objetivo: Double?,
    posicion: Double?,
    modifier: Modifier = Modifier
) {
    val puntos = tabla.filter { it.aceleranteKgMin > 0.0 }.sortedBy { it.aceleranteKgMin }

    if (puntos.size < 2) {
        Text(
            "Agrega al menos dos filas con valores distintos en la tabla para ver la curva.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val targetColor = MaterialTheme.colorScheme.error

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(8.dp)
        ) {
            val minX = puntos.first().aceleranteKgMin
            val maxX = puntos.last().aceleranteKgMin
            val minY = puntos.minOf { it.potenciometro }
            val maxY = puntos.maxOf { it.potenciometro }

            val rangoX = (maxX - minX).takeIf { it > 0.0 } ?: 1.0
            val rangoY = (maxY - minY).takeIf { it > 0.0 } ?: 1.0

            fun toOffset(ace: Double, pot: Double): Offset {
                val x = ((ace - minX) / rangoX) * size.width
                val y = size.height - ((pot - minY) / rangoY) * size.height
                return Offset(x.toFloat(), y.toFloat())
            }

            // Cuadrícula de referencia.
            for (i in 0..4) {
                val y = size.height * i / 4
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                val x = size.width * i / 4
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            }

            // Curva de calibración.
            for (i in 0 until puntos.size - 1) {
                val p1 = toOffset(puntos[i].aceleranteKgMin, puntos[i].potenciometro)
                val p2 = toOffset(puntos[i + 1].aceleranteKgMin, puntos[i + 1].potenciometro)
                drawLine(lineColor, p1, p2, strokeWidth = 4f, cap = StrokeCap.Round)
            }

            // Puntos de la tabla.
            puntos.forEach {
                drawCircle(lineColor, radius = 6f, center = toOffset(it.aceleranteKgMin, it.potenciometro))
            }

            // Punto objetivo (interpolado o extrapolado).
            if (objetivo != null && posicion != null) {
                val raw = toOffset(objetivo, posicion)
                val target = Offset(
                    raw.x.coerceIn(0f, size.width),
                    raw.y.coerceIn(0f, size.height)
                )
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                drawLine(targetColor, Offset(target.x, target.y), Offset(target.x, size.height), strokeWidth = 2f, pathEffect = dash)
                drawLine(targetColor, Offset(target.x, target.y), Offset(0f, target.y), strokeWidth = 2f, pathEffect = dash)
                drawCircle(targetColor, radius = 8f, center = target)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatNumber(puntos.first().aceleranteKgMin, 2), style = MaterialTheme.typography.labelSmall)
            Text("Acelerante (kg/min)", style = MaterialTheme.typography.labelMedium)
            Text(formatNumber(puntos.last().aceleranteKgMin, 2), style = MaterialTheme.typography.labelSmall)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Potenciómetro: ${formatNumber(puntos.minOf { it.potenciometro }, 1)} a ${formatNumber(puntos.maxOf { it.potenciometro }, 1)}",
                style = MaterialTheme.typography.labelSmall
            )
            if (objetivo != null && posicion != null) {
                Text(
                    "● Objetivo: ${formatNumber(posicion, 2)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = targetColor
                )
            }
        }
    }
}
