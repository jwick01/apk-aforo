package com.hansbarrera.aditivosaforo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hansbarrera.aditivosaforo.AppState
import com.hansbarrera.aditivosaforo.calc.FilaPotenciometro
import com.hansbarrera.aditivosaforo.calc.Formulas
import com.hansbarrera.aditivosaforo.data.Presets
import com.hansbarrera.aditivosaforo.ui.components.NumberField
import com.hansbarrera.aditivosaforo.ui.components.PotenciometroCurveChart
import com.hansbarrera.aditivosaforo.ui.components.ResultRow
import com.hansbarrera.aditivosaforo.ui.components.SectionCard
import com.hansbarrera.aditivosaforo.ui.components.formatNumber
import com.hansbarrera.aditivosaforo.ui.components.toDoubleOrZero

@Composable
fun PotenciometroScreen(appState: AppState) {
    var rendimiento by rememberSaveable { mutableStateOf("") }
    var cementoPorM3 by rememberSaveable { mutableStateOf("420") }
    var porcentajeAcelerante by rememberSaveable { mutableStateOf("0.08") }
    var objetivoManual by rememberSaveable { mutableStateOf("") }

    val filasPotenciometro = remember {
        Presets.tablaPotenciometroDefault.map {
            mutableStateOf(it.first.toString()) to mutableStateOf(it.second.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Posición del potenciómetro", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(top = 12.dp))

        SectionCard("Acelerante requerido (según diseño)") {
            NumberField("Rendimiento de la bomba", rendimiento, { rendimiento = it }, unit = "m3/hr", step = 0.1, decimals = 2)
            val guardado = appState.rendimientoM3Hr.value
            if (guardado != null) {
                Spacer(modifier = Modifier.padding(top = 6.dp))
                OutlinedButton(
                    onClick = { rendimiento = formatNumber(guardado, 6) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Usar rendimiento guardado (${formatNumber(guardado)} m3/hr)")
                }
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField("Cantidad de cemento por m3", cementoPorM3, { cementoPorM3 = it }, unit = "kg/m3", step = 5.0, decimals = 0)
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField("Porcentaje de acelerante requerido", porcentajeAcelerante, { porcentajeAcelerante = it }, unit = "fracción, ej. 0.08 = 8%", step = 0.005, decimals = 3)
        }

        val aceleranteCalculado = remember(rendimiento, cementoPorM3, porcentajeAcelerante) {
            Formulas.aceleranteRequeridoKgMin(
                rendimientoM3Hr = rendimiento.toDoubleOrZero(),
                cementoKgM3 = cementoPorM3.toDoubleOrZero(),
                porcentajeAceleranteRequerido = porcentajeAcelerante.toDoubleOrZero()
            )
        }

        // Registra automáticamente los datos ingresados, para que queden disponibles
        // en el informe aunque no se presione el botón "Usar este valor como objetivo...".
        LaunchedEffect(cementoPorM3, porcentajeAcelerante, aceleranteCalculado) {
            appState.registrarDatos(mapOf(
                "Potenciómetro - Cantidad de cemento (kg/m3)" to cementoPorM3,
                "Potenciómetro - Porcentaje de acelerante requerido" to porcentajeAcelerante,
                "Potenciómetro - Acelerante requerido (kg/min)" to formatNumber(aceleranteCalculado, 6)
            ))
        }

        SectionCard("Resultado") {
            ResultRow("Acelerante requerido", aceleranteCalculado, "kg/min")
            Spacer(modifier = Modifier.padding(top = 8.dp))
            OutlinedButton(
                onClick = {
                    objetivoManual = formatNumber(aceleranteCalculado, 6)
                    appState.registrarDatos(mapOf(
                        "Potenciómetro - Cantidad de cemento (kg/m3)" to cementoPorM3,
                        "Potenciómetro - Porcentaje de acelerante requerido" to porcentajeAcelerante,
                        "Potenciómetro - Acelerante requerido (kg/min)" to formatNumber(aceleranteCalculado, 6)
                    ))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Usar este valor como objetivo del potenciómetro")
            }
        }

        SectionCard("Tabla de calibración del potenciómetro") {
            Text(
                "Edita los valores reales medidos en el equipo (potenciómetro vs. acelerante en kg/min).",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.padding(top = 8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Potenciómetro", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Text("Acelerante (kg/min)", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.padding(top = 4.dp))

            filasPotenciometro.forEach { (pot, ace) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    NumberField("", pot.value, { pot.value = it }, modifier = Modifier.weight(1f))
                    NumberField("", ace.value, { ace.value = it }, modifier = Modifier.weight(1f))
                }
            }
        }

        SectionCard("Posición interpolada") {
            NumberField("Acelerante objetivo", objetivoManual, { objetivoManual = it }, unit = "kg/min", step = 0.1, decimals = 2)

            val tabla = remember(filasPotenciometro.map { it.first.value to it.second.value }) {
                filasPotenciometro.map { (pot, ace) ->
                    FilaPotenciometro(
                        potenciometro = pot.value.toDoubleOrZero(),
                        aceleranteKgMin = ace.value.toDoubleOrZero()
                    )
                }
            }

            val posicion = remember(tabla, objetivoManual) {
                Formulas.potenciometroInterpolado(tabla, objetivoManual.toDoubleOrZero())
            }

            val tablaTexto = remember(tabla) {
                tabla.filter { it.aceleranteKgMin > 0.0 }
                    .sortedBy { it.potenciometro }
                    .joinToString("; ") { "${formatNumber(it.potenciometro, 2)} -> ${formatNumber(it.aceleranteKgMin, 2)} kg/min" }
            }

            // Registra automáticamente la tabla, el objetivo y la posición interpolada,
            // para que queden disponibles en el informe aunque no se presione el botón.
            LaunchedEffect(tablaTexto, objetivoManual, posicion) {
                val datos = mutableMapOf(
                    "Potenciómetro - Tabla de calibración" to tablaTexto,
                    "Potenciómetro - Acelerante objetivo (kg/min)" to objetivoManual
                )
                if (posicion != null) {
                    datos["Potenciómetro - Posición interpolada"] = formatNumber(posicion, 6)
                }
                appState.registrarDatos(datos)
            }

            Spacer(modifier = Modifier.padding(top = 8.dp))
            if (posicion != null) {
                ResultRow("Posición del potenciómetro", posicion, "", decimals = 2)
                Spacer(modifier = Modifier.padding(top = 8.dp))
                OutlinedButton(
                    onClick = {
                        appState.registrarDatos(mapOf(
                            "Potenciómetro - Tabla de calibración" to tablaTexto,
                            "Potenciómetro - Acelerante objetivo (kg/min)" to objetivoManual,
                            "Potenciómetro - Posición interpolada" to formatNumber(posicion, 6)
                        ))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Usar esta posición en el registro de aforo")
                }
            } else {
                Text(
                    "Ingresa al menos dos filas de la tabla con valores de acelerante distintos.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.padding(top = 12.dp))
            PotenciometroCurveChart(
                tabla = tabla,
                objetivo = objetivoManual.toDoubleOrZero().takeIf { it > 0.0 },
                posicion = posicion
            )
        }
    }
}
