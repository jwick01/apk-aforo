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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hansbarrera.aditivosaforo.AppState
import com.hansbarrera.aditivosaforo.R
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
        Text(stringResource(R.string.title_rendimiento), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(top = 8.dp))

        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text(stringResource(R.string.tab_por_emboladas)) })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text(stringResource(R.string.tab_por_tiempo_llenado)) })
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
    val resetKey = appState.formResetTrigger.value
    val editKey = appState.editarRegistroTrigger.value
    val datos = appState.datosInforme.value
    var presetIndex by rememberSaveable(resetKey, editKey) {
        val nombreGuardado = datos["Rendimiento (émboladas) - Equipo (tipo de bomba)"]
        val indiceGuardado = nombreGuardado?.let { nombre -> Presets.bombas.indexOfFirst { it.nombre == nombre } }
        mutableStateOf(indiceGuardado?.takeIf { it >= 0 } ?: 2) // Alpha 30 por defecto
    }

    var volumenCilindro by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Rendimiento (émboladas) - Volumen cilindro (lts)"] ?: Presets.bombas[presetIndex].volumenCilindroLts.toString())
    }
    var emboladas by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Rendimiento (émboladas) - Émboladas por minuto"] ?: "12")
    }
    var factorLlenado by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Rendimiento (émboladas) - Factor de llenado"] ?: Presets.FACTOR_LLENADO_DEFAULT.toString())
    }

    SectionCard(stringResource(R.string.section_datos_bomba)) {
        Text(stringResource(R.string.label_tipo_bomba), style = MaterialTheme.typography.labelLarge)
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
        NumberField(stringResource(R.string.label_volumen_cilindro), volumenCilindro, { volumenCilindro = it }, unit = "lts", step = 0.1, decimals = 2)
        Spacer(modifier = Modifier.padding(top = 8.dp))
        NumberField(stringResource(R.string.label_numero_emboladas), emboladas, { emboladas = it }, unit = stringResource(R.string.unit_por_minuto), step = 1.0, decimals = 0)
        Spacer(modifier = Modifier.padding(top = 8.dp))
        NumberField(stringResource(R.string.label_factor_llenado), factorLlenado, { factorLlenado = it }, step = 0.01, decimals = 2)
    }

    val resultado = remember(volumenCilindro, emboladas, factorLlenado) {
        Formulas.rendimientoPorEmboladas(
            volumenCilindroLts = volumenCilindro.toDoubleOrZero(),
            emboladasPorMin = emboladas.toDoubleOrZero(),
            factorLlenado = factorLlenado.toDoubleOrZero()
        )
    }

    // Registra automáticamente los datos ingresados, para que queden disponibles
    // en el informe aunque no se presione el botón de "usar este rendimiento".
    LaunchedEffect(volumenCilindro, emboladas, factorLlenado, presetIndex, resultado.rendimientoM3Hr) {
        appState.registrarDatos(mapOf(
            "Rendimiento (émboladas) - Equipo (tipo de bomba)" to Presets.bombas[presetIndex].nombre,
            "Rendimiento (émboladas) - Volumen cilindro (lts)" to volumenCilindro,
            "Rendimiento (émboladas) - Émboladas por minuto" to emboladas,
            "Rendimiento (émboladas) - Factor de llenado" to factorLlenado,
            "Rendimiento de la bomba (m3/hr)" to formatNumber(resultado.rendimientoM3Hr, 1)
        ))
    }

    SectionCard(stringResource(R.string.result_title)) {
        Text(
            stringResource(R.string.result_equipo_seleccionado, Presets.bombas[presetIndex].nombre),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.padding(top = 4.dp))
        ResultRow(stringResource(R.string.result_volumen_cilindro_efectivo), resultado.volumenEfectivoLtsMin, "lts/min")
        ResultRow(stringResource(R.string.result_volumen_cilindro_efectivo), resultado.volumenEfectivoLtsHr, "lts/hr")
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        ResultRow(stringResource(R.string.result_rendimiento), resultado.rendimientoM3Hr, "m3/hr")

        Spacer(modifier = Modifier.padding(top = 8.dp))
        Button(
            onClick = {
                appState.rendimientoM3Hr.value = resultado.rendimientoM3Hr
                appState.registrarDatos(mapOf(
                    "Rendimiento (émboladas) - Equipo (tipo de bomba)" to Presets.bombas[presetIndex].nombre,
                    "Rendimiento (émboladas) - Volumen cilindro (lts)" to volumenCilindro,
                    "Rendimiento (émboladas) - Émboladas por minuto" to emboladas,
                    "Rendimiento (émboladas) - Factor de llenado" to factorLlenado,
                    "Rendimiento de la bomba (m3/hr)" to formatNumber(resultado.rendimientoM3Hr, 1)
                ))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.usar_rendimiento_otras))
        }
        GuardadoIndicator(appState)
    }
}

@Composable
private fun RendimientoPorTiempoLlenado(appState: AppState) {
    val resetKey = appState.formResetTrigger.value
    val editKey = appState.editarRegistroTrigger.value
    val datos = appState.datosInforme.value
    var tiempoLlenado by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Rendimiento (tiempo de llenado) - Tiempo de llenado (seg)"] ?: "80")
    }
    var volumenLlenado by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Rendimiento (tiempo de llenado) - Volumen de llenado (m3)"] ?: "0.2")
    }

    SectionCard(stringResource(R.string.section_datos_llenado)) {
        NumberField(stringResource(R.string.label_tiempo_llenado), tiempoLlenado, { tiempoLlenado = it }, unit = "seg", step = 1.0, decimals = 0)
        Spacer(modifier = Modifier.padding(top = 8.dp))
        NumberField(stringResource(R.string.label_volumen_llenado), volumenLlenado, { volumenLlenado = it }, unit = "m3", step = 0.01, decimals = 2)
    }

    val rendimiento = remember(tiempoLlenado, volumenLlenado) {
        Formulas.rendimientoPorTiempoLlenado(
            volumenLlenadoM3 = volumenLlenado.toDoubleOrZero(),
            tiempoLlenadoSeg = tiempoLlenado.toDoubleOrZero()
        )
    }

    // Registra automáticamente los datos ingresados, para que queden disponibles
    // en el informe aunque no se presione el botón de "usar este rendimiento".
    LaunchedEffect(tiempoLlenado, volumenLlenado, rendimiento) {
        appState.registrarDatos(mapOf(
            "Rendimiento (tiempo de llenado) - Tiempo de llenado (seg)" to tiempoLlenado,
            "Rendimiento (tiempo de llenado) - Volumen de llenado (m3)" to volumenLlenado,
            "Rendimiento de la bomba (m3/hr)" to formatNumber(rendimiento, 1)
        ))
    }

    SectionCard(stringResource(R.string.result_title)) {
        ResultRow(stringResource(R.string.result_rendimiento), rendimiento, "m3/hr")

        Spacer(modifier = Modifier.padding(top = 8.dp))
        Button(
            onClick = {
                appState.rendimientoM3Hr.value = rendimiento
                appState.registrarDatos(mapOf(
                    "Rendimiento (tiempo de llenado) - Tiempo de llenado (seg)" to tiempoLlenado,
                    "Rendimiento (tiempo de llenado) - Volumen de llenado (m3)" to volumenLlenado,
                    "Rendimiento de la bomba (m3/hr)" to formatNumber(rendimiento, 1)
                ))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.usar_rendimiento_otras))
        }
        GuardadoIndicator(appState)
    }
}

@Composable
private fun GuardadoIndicator(appState: AppState) {
    val valor = appState.rendimientoM3Hr.value
    if (valor != null) {
        Text(
            stringResource(R.string.guardado_rendimiento, formatNumber(valor)),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
