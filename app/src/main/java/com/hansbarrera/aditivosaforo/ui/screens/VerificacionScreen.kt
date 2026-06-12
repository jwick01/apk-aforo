package com.hansbarrera.aditivosaforo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.hansbarrera.aditivosaforo.ui.components.NumberField
import com.hansbarrera.aditivosaforo.ui.components.ResultRow
import com.hansbarrera.aditivosaforo.ui.components.SectionCard
import com.hansbarrera.aditivosaforo.ui.components.formatNumber
import com.hansbarrera.aditivosaforo.ui.components.toDoubleOrZero

@Composable
fun VerificacionScreen(appState: AppState) {
    var rendimientoCalculado by rememberSaveable { mutableStateOf("") }
    var rendimientoDisplay by rememberSaveable { mutableStateOf("") }
    var aditivoCalculado by rememberSaveable { mutableStateOf("") }
    var aditivoDisplay by rememberSaveable { mutableStateOf("") }
    var porcentajeCalculado by rememberSaveable { mutableStateOf("") }
    var porcentajeDisplay by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Verificación contra el display", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(top = 12.dp))

        SectionCard("Caudal de hormigón") {
            NumberField("Rendimiento calculado", rendimientoCalculado, { rendimientoCalculado = it }, unit = "m3/hr", step = 0.1, decimals = 2)
            val guardadoRend = appState.rendimientoM3Hr.value
            if (guardadoRend != null) {
                Spacer(modifier = Modifier.padding(top = 6.dp))
                OutlinedButton(
                    onClick = { rendimientoCalculado = formatNumber(guardadoRend, 6) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Usar valor guardado (${formatNumber(guardadoRend)} m3/hr)")
                }
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField("Rendimiento según display", rendimientoDisplay, { rendimientoDisplay = it }, unit = "m3/hr", step = 0.1, decimals = 2)
        }

        SectionCard("Caudal de aditivo") {
            NumberField("Caudal de aditivo calculado", aditivoCalculado, { aditivoCalculado = it }, unit = "lts/min", step = 0.1, decimals = 2)
            val guardadoAditivo = appState.caudalAditivoLtsMin.value
            if (guardadoAditivo != null) {
                Spacer(modifier = Modifier.padding(top = 6.dp))
                OutlinedButton(
                    onClick = { aditivoCalculado = formatNumber(guardadoAditivo, 6) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Usar valor guardado (${formatNumber(guardadoAditivo)} lts/min)")
                }
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField("Aditivo según display", aditivoDisplay, { aditivoDisplay = it }, unit = "lts/min", step = 0.1, decimals = 2)
        }

        SectionCard("Porcentaje de aditivo") {
            NumberField("Porcentaje calculado", porcentajeCalculado, { porcentajeCalculado = it }, unit = "fracción, ej. 0.08 = 8%", step = 0.005, decimals = 3)
            val guardadoPorcentaje = appState.porcentajeAditivoCalculado.value
            if (guardadoPorcentaje != null) {
                Spacer(modifier = Modifier.padding(top = 6.dp))
                OutlinedButton(
                    onClick = { porcentajeCalculado = formatNumber(guardadoPorcentaje, 6) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Usar valor guardado (${formatNumber(guardadoPorcentaje)})")
                }
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField("Porcentaje según muestra del display", porcentajeDisplay, { porcentajeDisplay = it }, unit = "fracción", step = 0.005, decimals = 3)
        }

        val resultado = remember(
            rendimientoCalculado, rendimientoDisplay,
            aditivoCalculado, aditivoDisplay,
            porcentajeCalculado, porcentajeDisplay
        ) {
            Formulas.desviaciones(
                rendimientoCalculadoM3Hr = rendimientoCalculado.toDoubleOrZero(),
                rendimientoDisplayM3Hr = rendimientoDisplay.toDoubleOrZero(),
                caudalAditivoCalculadoLtsMin = aditivoCalculado.toDoubleOrZero(),
                aditivoDisplayLtsMin = aditivoDisplay.toDoubleOrZero(),
                porcentajeAditivoCalculado = porcentajeCalculado.toDoubleOrZero(),
                porcentajeAditivoDisplay = porcentajeDisplay.toDoubleOrZero()
            )
        }

        SectionCard("Desviaciones") {
            ResultRow("Caudal de hormigón", resultado.desviacionCaudalHormigon * 100.0, "%", decimals = 2)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            ResultRow("Caudal de aditivo", resultado.desviacionCaudalAditivo * 100.0, "%", decimals = 2)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            ResultRow("Porcentaje de aditivo", resultado.desviacionPorcentajeAditivo * 100.0, "%", decimals = 2)
        }
    }
}
