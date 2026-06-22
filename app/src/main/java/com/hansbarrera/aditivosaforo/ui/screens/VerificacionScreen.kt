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
import com.hansbarrera.aditivosaforo.ui.components.NumberField
import com.hansbarrera.aditivosaforo.ui.components.ResultRow
import com.hansbarrera.aditivosaforo.ui.components.SectionCard
import com.hansbarrera.aditivosaforo.ui.components.formatNumber
import com.hansbarrera.aditivosaforo.ui.components.toDoubleOrZero

@Composable
fun VerificacionScreen(appState: AppState) {
    val resetKey = appState.formResetTrigger.value
    val editKey = appState.editarRegistroTrigger.value
    val datos = appState.datosInforme.value
    var rendimientoCalculado by rememberSaveable(resetKey, editKey) { mutableStateOf("") }
    var rendimientoDisplay by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Verificación - Rendimiento según display (m3/hr)"] ?: "")
    }
    var aditivoCalculado by rememberSaveable(resetKey, editKey) { mutableStateOf("") }
    var aditivoDisplay by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Verificación - Aditivo según display (lts/min)"] ?: "")
    }
    var porcentajeCalculado by rememberSaveable(resetKey, editKey) { mutableStateOf("") }
    var porcentajeDisplay by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Verificación - Porcentaje según display"] ?: "")
    }

    // Completa automáticamente los valores calculados si ya fueron calculados en otra pestaña.
    LaunchedEffect(appState.rendimientoM3Hr.value) {
        val guardadoAuto = appState.rendimientoM3Hr.value
        if (rendimientoCalculado.isBlank() && guardadoAuto != null) {
            rendimientoCalculado = formatNumber(guardadoAuto, 6)
        }
    }
    LaunchedEffect(appState.caudalAditivoLtsMin.value) {
        val guardadoAuto = appState.caudalAditivoLtsMin.value
        if (aditivoCalculado.isBlank() && guardadoAuto != null) {
            aditivoCalculado = formatNumber(guardadoAuto, 6)
        }
    }
    LaunchedEffect(appState.porcentajeAditivoCalculado.value) {
        val guardadoAuto = appState.porcentajeAditivoCalculado.value
        if (porcentajeCalculado.isBlank() && guardadoAuto != null) {
            porcentajeCalculado = formatNumber(guardadoAuto, 6)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.title_verificacion), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(top = 12.dp))

        SectionCard(stringResource(R.string.section_caudal_hormigon)) {
            NumberField(stringResource(R.string.label_rendimiento_calculado), rendimientoCalculado, { rendimientoCalculado = it }, unit = "m3/hr", step = 0.1, decimals = 2)
            val guardadoRend = appState.rendimientoM3Hr.value
            if (guardadoRend != null) {
                Spacer(modifier = Modifier.padding(top = 6.dp))
                OutlinedButton(
                    onClick = { rendimientoCalculado = formatNumber(guardadoRend, 6) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.btn_usar_valor_guardado_m3hr, formatNumber(guardadoRend)))
                }
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField(stringResource(R.string.label_rendimiento_display), rendimientoDisplay, { rendimientoDisplay = it }, unit = "m3/hr", step = 0.1, decimals = 2)
        }

        SectionCard(stringResource(R.string.section_caudal_aditivo)) {
            NumberField(stringResource(R.string.label_caudal_aditivo_calculado), aditivoCalculado, { aditivoCalculado = it }, unit = "lts/min", step = 0.1, decimals = 2)
            val guardadoAditivo = appState.caudalAditivoLtsMin.value
            if (guardadoAditivo != null) {
                Spacer(modifier = Modifier.padding(top = 6.dp))
                OutlinedButton(
                    onClick = { aditivoCalculado = formatNumber(guardadoAditivo, 6) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.btn_usar_valor_guardado_ltsmin, formatNumber(guardadoAditivo)))
                }
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField(stringResource(R.string.label_aditivo_display), aditivoDisplay, { aditivoDisplay = it }, unit = "lts/min", step = 0.1, decimals = 2)
        }

        SectionCard(stringResource(R.string.porcentaje_aditivo_label)) {
            NumberField(stringResource(R.string.label_porcentaje_calculado), porcentajeCalculado, { porcentajeCalculado = it }, unit = stringResource(R.string.unit_fraccion_008), step = 0.005, decimals = 3)
            val guardadoPorcentaje = appState.porcentajeAditivoCalculado.value
            if (guardadoPorcentaje != null) {
                Spacer(modifier = Modifier.padding(top = 6.dp))
                OutlinedButton(
                    onClick = { porcentajeCalculado = formatNumber(guardadoPorcentaje, 6) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.btn_usar_valor_guardado, formatNumber(guardadoPorcentaje)))
                }
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField(stringResource(R.string.label_porcentaje_display), porcentajeDisplay, { porcentajeDisplay = it }, unit = stringResource(R.string.unit_fraccion), step = 0.005, decimals = 3)
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

        // Registra automáticamente los datos ingresados, para que queden disponibles
        // en el informe aunque no se presione el botón "Guardar estos datos...".
        LaunchedEffect(
            rendimientoCalculado, rendimientoDisplay,
            aditivoCalculado, aditivoDisplay,
            porcentajeCalculado, porcentajeDisplay,
            resultado
        ) {
            appState.registrarDatos(mapOf(
                "Verificación - Rendimiento según display (m3/hr)" to rendimientoDisplay,
                "Verificación - Aditivo según display (lts/min)" to aditivoDisplay,
                "Verificación - Porcentaje según display" to porcentajeDisplay,
                "Verificación - Desviación caudal de hormigón (%)" to formatNumber(resultado.desviacionCaudalHormigon * 100.0, 1),
                "Verificación - Desviación caudal de aditivo (%)" to formatNumber(resultado.desviacionCaudalAditivo * 100.0, 1),
                "Verificación - Desviación porcentaje de aditivo (%)" to formatNumber(resultado.desviacionPorcentajeAditivo * 100.0, 1)
            ))
        }

        SectionCard(stringResource(R.string.section_desviaciones)) {
            ResultRow(stringResource(R.string.section_caudal_hormigon), resultado.desviacionCaudalHormigon * 100.0, "%", decimals = 1)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            ResultRow(stringResource(R.string.section_caudal_aditivo), resultado.desviacionCaudalAditivo * 100.0, "%", decimals = 1)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            ResultRow(stringResource(R.string.porcentaje_aditivo_label), resultado.desviacionPorcentajeAditivo * 100.0, "%", decimals = 1)

            Spacer(modifier = Modifier.padding(top = 8.dp))
            OutlinedButton(
                onClick = {
                    appState.registrarDatos(mapOf(
                        "Verificación - Rendimiento según display (m3/hr)" to rendimientoDisplay,
                        "Verificación - Aditivo según display (lts/min)" to aditivoDisplay,
                        "Verificación - Porcentaje según display" to porcentajeDisplay,
                        "Verificación - Desviación caudal de hormigón (%)" to formatNumber(resultado.desviacionCaudalHormigon * 100.0, 1),
                        "Verificación - Desviación caudal de aditivo (%)" to formatNumber(resultado.desviacionCaudalAditivo * 100.0, 1),
                        "Verificación - Desviación porcentaje de aditivo (%)" to formatNumber(resultado.desviacionPorcentajeAditivo * 100.0, 1)
                    ))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_guardar_datos_aforo))
            }
        }
    }
}
