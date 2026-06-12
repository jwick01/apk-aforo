package com.hansbarrera.aditivosaforo.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
fun RendimientoScreen(appState: AppState) {
    var tabIndex by rememberSaveable { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Rendimiento de la bomba", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(top = 8.dp))

        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Por émboladas") })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Por tiempo de llenado") })
        }

        Spacer(modifier = Modifier.padding(top = 12.dp))

        if (tabIndex == 0) {
            RendimientoPorEmboladas(appState)
        } else {
            RendimientoPorTiempoLlenado(appState)
        }
    }
}

@Composable
private fun RendimientoPorEmboladas(appState: AppState) {
    var presetIndex by rememberSaveable { mutableStateOf(2) } // Alpha 30 por defecto

    var volumenCilindro by rememberSaveable { mutableStateOf(Presets.bombas[presetIndex].volumenCilindroLts.toString()) }
    var emboladas by rememberSaveable { mutableStateOf("12") }
    var factorLlenado by rememberSaveable { mutableStateOf(Presets.FACTOR_LLENADO_DEFAULT.toString()) }

    SectionCard("Datos de la bomba") {
        Text("Tipo de bomba", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.padding(top = 4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Presets.bombas.forEachIndexed { index, bomba ->
                FilterChip(
                    selected = presetIndex == index,
                    onClick = {
                        presetIndex = index
                        if (bomba.volumenCilindroLts > 0.0) {
                            volumenCilindro = bomba.volumenCilindroLts.toString()
                        }
                    },
                    label = { Text(bomba.nombre) }
                )
            }
        }

        Spacer(modifier = Modifier.padding(top = 8.dp))
        NumberField("Volumen cilindro", volumenCilindro, { volumenCilindro = it }, unit = "lts", step = 0.1, decimals = 2)
        Spacer(modifier = Modifier.padding(top = 8.dp))
        NumberField("Número de émboladas", emboladas, { emboladas = it }, unit = "por minuto", step = 1.0, decimals = 0)
        Spacer(modifier = Modifier.padding(top = 8.dp))
        NumberField("Factor de llenado", factorLlenado, { factorLlenado = it }, step = 0.01, decimals = 2)
    }

    val resultado = remember(volumenCilindro, emboladas, factorLlenado) {
        Formulas.rendimientoPorEmboladas(
            volumenCilindroLts = volumenCilindro.toDoubleOrZero(),
            emboladasPorMin = emboladas.toDoubleOrZero(),
            factorLlenado = factorLlenado.toDoubleOrZero()
        )
    }

    SectionCard("Resultado") {
        ResultRow("Volumen cilindro efectivo", resultado.volumenEfectivoLtsMin, "lts/min")
        ResultRow("Volumen cilindro efectivo", resultado.volumenEfectivoLtsHr, "lts/hr")
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        ResultRow("Rendimiento", resultado.rendimientoM3Hr, "m3/hr")

        Spacer(modifier = Modifier.padding(top = 8.dp))
        Button(
            onClick = {
                appState.rendimientoM3Hr.value = resultado.rendimientoM3Hr
                appState.registrarDatos(mapOf(
                    "Rendimiento (émboladas) - Volumen cilindro (lts)" to volumenCilindro,
                    "Rendimiento (émboladas) - Émboladas por minuto" to emboladas,
                    "Rendimiento (émboladas) - Factor de llenado" to factorLlenado,
                    "Rendimiento de la bomba (m3/hr)" to formatNumber(resultado.rendimientoM3Hr, 6)
                ))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Usar este rendimiento en las otras calculadoras")
        }
        GuardadoIndicator(appState)
    }
}

@Composable
private fun RendimientoPorTiempoLlenado(appState: AppState) {
    var tiempoLlenado by rememberSaveable { mutableStateOf("80") }
    var volumenLlenado by rememberSaveable { mutableStateOf("0.2") }

    SectionCard("Datos de llenado") {
        NumberField("Tiempo de llenado", tiempoLlenado, { tiempoLlenado = it }, unit = "seg", step = 1.0, decimals = 0)
        Spacer(modifier = Modifier.padding(top = 8.dp))
        NumberField("Volumen de llenado", volumenLlenado, { volumenLlenado = it }, unit = "m3", step = 0.01, decimals = 2)
    }

    val rendimiento = remember(tiempoLlenado, volumenLlenado) {
        Formulas.rendimientoPorTiempoLlenado(
            volumenLlenadoM3 = volumenLlenado.toDoubleOrZero(),
            tiempoLlenadoSeg = tiempoLlenado.toDoubleOrZero()
        )
    }

    SectionCard("Resultado") {
        ResultRow("Rendimiento", rendimiento, "m3/hr")

        Spacer(modifier = Modifier.padding(top = 8.dp))
        Button(
            onClick = {
                appState.rendimientoM3Hr.value = rendimiento
                appState.registrarDatos(mapOf(
                    "Rendimiento (tiempo de llenado) - Tiempo de llenado (seg)" to tiempoLlenado,
                    "Rendimiento (tiempo de llenado) - Volumen de llenado (m3)" to volumenLlenado,
                    "Rendimiento de la bomba (m3/hr)" to formatNumber(rendimiento, 6)
                ))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Usar este rendimiento en las otras calculadoras")
        }
        GuardadoIndicator(appState)
    }
}

@Composable
private fun GuardadoIndicator(appState: AppState) {
    val valor = appState.rendimientoM3Hr.value
    if (valor != null) {
        Text(
            "Rendimiento guardado: ${formatNumber(valor)} m3/hr",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
