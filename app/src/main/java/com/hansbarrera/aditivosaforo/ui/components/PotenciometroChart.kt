package com.hansbarrera.aditivosaforo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
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

private const val ALTO_GRAFICO_DP = 200

/**
 * Gráfico de la curva de calibración del potenciómetro: eje X = posición del
 * potenciómetro, eje Y = acelerante en kg/min. Los valores de los ejes se
 * recalculan dinámicamente a partir de la tabla de calibración ingresada, y
 * se resalta el punto objetivo (acelerante objetivo / posición interpolada)
 * si se indica.
 */
@Composable
fun PotenciometroCurveChart(
    tabla: List<FilaPotenciometro>,
    objetivo: Double?,
    posicion: Double?,
    modifier: Modifier = Modifier
) {
    val puntos = tabla.filter { it.aceleranteKgMin > 0.0 }.sortedBy { it.potenciometro }

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

    val minX = puntos.first().potenciometro
    val maxX = puntos.last().potenciometro
    val minY = puntos.minOf { it.aceleranteKgMin }
    val maxY = puntos.maxOf { it.aceleranteKgMin }

    Column(modifier = modifier) {
        Text(
            "Eje Y: Acelerante (kg/min)  ·  Eje X: Posición del potenciómetro",
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.padding(top = 4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            // Etiquetas dinámicas del eje Y (acelerante kg/min): máximo arriba, mínimo abajo.
            Column(
                modifier = Modifier
                    .height(ALTO_GRAFICO_DP.dp)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatNumber(maxY, 2), style = MaterialTheme.typography.labelSmall)
                Text(formatNumber((maxY + minY) / 2.0, 2), style = MaterialTheme.typography.labelSmall)
                Text(formatNumber(minY, 2), style = MaterialTheme.typography.labelSmall)
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(ALTO_GRAFICO_DP.dp)
                    .padding(8.dp)
            ) {
                val rangoX = (maxX - minX).takeIf { it > 0.0 } ?: 1.0
                val rangoY = (maxY - minY).takeIf { it > 0.0 } ?: 1.0

                fun toOffset(pot: Double, ace: Double): Offset {
                    val x = ((pot - minX) / rangoX) * size.width
                    val y = size.height - ((ace - minY) / rangoY) * size.height
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
                    val p1 = toOffset(puntos[i].potenciometro, puntos[i].aceleranteKgMin)
                    val p2 = toOffset(puntos[i + 1].potenciometro, puntos[i + 1].aceleranteKgMin)
                    drawLine(lineColor, p1, p2, strokeWidth = 4f, cap = StrokeCap.Round)
                }

                // Puntos de la tabla.
                puntos.forEach {
                    drawCircle(lineColor, radius = 6f, center = toOffset(it.potenciometro, it.aceleranteKgMin))
                }

                // Punto objetivo (interpolado o extrapolado).
                if (objetivo != null && posicion != null) {
                    val raw = toOffset(posicion, objetivo)
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
        }

        // Etiquetas dinámicas del eje X (posición del potenciómetro): mínimo, medio y máximo.
        Row(modifier = Modifier.fillMaxWidth().padding(start = 28.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatNumber(minX, 2), style = MaterialTheme.typography.labelSmall)
            Text(formatNumber((minX + maxX) / 2.0, 2), style = MaterialTheme.typography.labelSmall)
            Text(formatNumber(maxX, 2), style = MaterialTheme.typography.labelSmall)
        }

        if (objetivo != null && posicion != null) {
            Spacer(modifier = Modifier.padding(top = 4.dp))
            Text(
                "● Objetivo: posición ${formatNumber(posicion, 2)} para ${formatNumber(objetivo, 2)} kg/min",
                style = MaterialTheme.typography.labelSmall,
                color = targetColor
            )
        }
    }
}
