package com.hansbarrera.aditivosaforo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import com.hansbarrera.aditivosaforo.calc.Formulas
import com.hansbarrera.aditivosaforo.data.Presets
import com.hansbarrera.aditivosaforo.ui.components.NumberField
import com.hansbarrera.aditivosaforo.ui.components.ResultRow
import com.hansbarrera.aditivosaforo.ui.components.SectionCard
import com.hansbarrera.aditivosaforo.ui.components.formatNumber
import com.hansbarrera.aditivosaforo.ui.components.toDoubleOrZero

@Composable
fun AditivoScreen(appState: AppState) {
    var rendimiento by rememberSaveable { mutableStateOf("") }
    var dosisCemento by rememberSaveable { mutableStateOf("400") }
    var porcentajeAditivo by rememberSaveable { mutableStateOf("0.08") }
    var densidadAditivo by rememberSaveable { mutableStateOf(Presets.DENSIDAD_ADITIVO_DEFAULT.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Dosis de aditivo / acelerante", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(top = 12.dp))

        SectionCard("Datos") {
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
            NumberField("Dosis de cemento", dosisCemento, { dosisCemento = it }, unit = "kg/m3", step = 5.0, decimals = 0)
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField("Porcentaje de aditivo", porcentajeAditivo, { porcentajeAditivo = it }, unit = "fracción, ej. 0.08 = 8%", step = 0.005, decimals = 3)
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField("Densidad del aditivo", densidadAditivo, { densidadAditivo = it }, unit = "kg/lt", step = 0.05, decimals = 2)
        }

        val resultado = remember(rendimiento, dosisCemento, porcentajeAditivo, densidadAditivo) {
            Formulas.dosisAditivo(
                rendimientoM3Hr = rendimiento.toDoubleOrZero(),
                dosisCementoKg = dosisCemento.toDoubleOrZero(),
                porcentajeAditivo = porcentajeAditivo.toDoubleOrZero(),
                densidadAditivo = densidadAditivo.toDoubleOrZero()
            )
        }

        SectionCard("Resultado") {
            ResultRow("Kilos de aditivo requerido", resultado.kilosAditivoKgMin, "kg/min")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            ResultRow("Litros de aditivo", resultado.litrosAditivoLtsMin, "lts/min")

            Spacer(modifier = Modifier.padding(top = 8.dp))
            Button(
                onClick = {
                    appState.caudalAditivoLtsMin.value = resultado.litrosAditivoLtsMin
                    appState.porcentajeAditivoCalculado.value = porcentajeAditivo.toDoubleOrZero()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Usar estos resultados en Verificación")
            }
        }
    }
}
