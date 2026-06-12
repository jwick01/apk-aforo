package com.hansbarrera.aditivosaforo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
            NumberField("Rendimiento de la bomba", rendimiento, { rendimiento = it }, unit = "m3/hr")
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
            NumberField("Cantidad de cemento por m3", cementoPorM3, { cementoPorM3 = it }, unit = "kg/m3")
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField("Porcentaje de acelerante requerido", porcentajeAcelerante, { porcentajeAcelerante = it }, unit = "fracción, ej. 0.08 = 8%")
        }

        val aceleranteCalculado = remember(rendimiento, cementoPorM3, porcentajeAcelerante) {
            Formulas.aceleranteRequeridoKgMin(
                rendimientoM3Hr = rendimiento.toDoubleOrZero(),
                cementoKgM3 = cementoPorM3.toDoubleOrZero(),
                porcentajeAceleranteRequerido = porcentajeAcelerante.toDoubleOrZero()
            )
        }

        SectionCard("Resultado") {
            ResultRow("Acelerante requerido", aceleranteCalculado, "kg/min")
            Spacer(modifier = Modifier.padding(top = 8.dp))
            OutlinedButton(
                onClick = { objetivoManual = formatNumber(aceleranteCalculado, 6) },
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
            NumberField("Acelerante objetivo", objetivoManual, { objetivoManual = it }, unit = "kg/min")

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

            Spacer(modifier = Modifier.padding(top = 8.dp))
            if (posicion != null) {
                ResultRow("Posición del potenciómetro", posicion, "", decimals = 2)
            } else {
                Text(
                    "Ingresa al menos dos filas de la tabla con valores de acelerante distintos.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
