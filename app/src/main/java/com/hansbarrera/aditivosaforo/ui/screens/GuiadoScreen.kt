package com.hansbarrera.aditivosaforo.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.hansbarrera.aditivosaforo.AppState
import com.hansbarrera.aditivosaforo.R
import com.hansbarrera.aditivosaforo.data.AforoRecord
import com.hansbarrera.aditivosaforo.data.AforoRepository
import com.hansbarrera.aditivosaforo.data.PdfExporter
import com.hansbarrera.aditivosaforo.ui.components.ResultadosAgrupados
import com.hansbarrera.aditivosaforo.ui.components.SectionCard
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TOTAL_PASOS = 7
private const val PASO_POTENCIOMETRO = 3
private const val PASO_FOTOS = 5

/**
 * Asistente de aforo paso a paso: sigue el orden real del trabajo en terreno
 * (datos → rendimiento → aditivo → potenciómetro → verificación → fotos →
 * resumen). Reutiliza las pantallas de las calculadoras tal cual: el paso les
 * da una altura acotada (weight), por lo que su scroll propio es válido.
 */
@Composable
fun GuiadoScreen(appState: AppState, onFinalizar: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { AforoRepository(context) }
    val form = rememberRegistroForm(appState, repository)
    var paso by rememberSaveable { mutableStateOf(0) }
    val stateHolder = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    var guardadoId by rememberSaveable { mutableStateOf<String?>(null) }
    var registroGuardado by remember { mutableStateOf<AforoRecord?>(null) }
    var exportando by remember { mutableStateOf(false) }
    var exportandoPdf by remember { mutableStateOf(false) }

    val msgErrorGuardar = stringResource(R.string.msg_error_guardar)
    val msgErrorExportar = stringResource(R.string.msg_error_exportar)
    val msgErrorPdf = stringResource(R.string.msg_error_pdf)
    val msgGuardadoGuiado = stringResource(R.string.msg_registro_guardado_guiado)
    val chooserCompartirRegistro = stringResource(R.string.chooser_compartir_registro)
    val chooserCompartirPdf = stringResource(R.string.chooser_compartir_pdf)

    // Al salir del asistente, fuerza a la pestaña Aforo (si quedó restaurada en
    // memoria) a releer el borrador que el asistente estuvo modificando.
    DisposableEffect(Unit) {
        onDispose { appState.editarRegistroTrigger.value++ }
    }

    BackHandler(enabled = paso > 0) { paso-- }

    val titulos = listOf(
        R.string.section_datos_trabajo,
        R.string.title_rendimiento,
        R.string.title_aditivo,
        R.string.title_potenciometro,
        R.string.title_verificacion,
        R.string.section_fotografias,
        R.string.title_resumen
    )

    fun compartirArchivo(archivo: File, mime: String, titulo: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, titulo))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.title_guiado), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(top = 8.dp))
        LinearProgressIndicator(
            progress = { (paso + 1) / TOTAL_PASOS.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(top = 6.dp))
        Text(
            stringResource(R.string.label_paso_de, paso + 1, TOTAL_PASOS) + " · " + stringResource(titulos[paso]),
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))

        Box(modifier = Modifier.weight(1f)) {
            stateHolder.SaveableStateProvider(paso) {
                when (paso) {
                    0 -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        DatosTrabajoSection(form)
                    }
                    1 -> RendimientoScreen(appState)
                    2 -> AditivoScreen(appState)
                    PASO_POTENCIOMETRO -> PotenciometroScreen(appState)
                    4 -> VerificacionScreen(appState)
                    PASO_FOTOS -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        FotosSection(form, repository)
                    }
                    else -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        ResumenPaso(
                            appState = appState,
                            form = form,
                            registroGuardado = registroGuardado,
                            guardadoId = guardadoId,
                            exportando = exportando,
                            exportandoPdf = exportandoPdf,
                            onGuardar = {
                                scope.launch {
                                    try {
                                        val guardado = form.guardarRegistro(repository, appState)
                                        registroGuardado = guardado
                                        guardadoId = guardado.id
                                        form.mensaje.value = String.format(msgGuardadoGuiado, guardado.id)
                                    } catch (e: Exception) {
                                        form.mensaje.value = msgErrorGuardar
                                    }
                                }
                            },
                            onExportarZip = { id ->
                                exportando = true
                                scope.launch {
                                    try {
                                        val zip = withContext(Dispatchers.IO) { repository.exportZip(id) }
                                        compartirArchivo(zip, "application/zip", chooserCompartirRegistro)
                                    } catch (e: Exception) {
                                        form.mensaje.value = msgErrorExportar
                                    } finally {
                                        exportando = false
                                    }
                                }
                            },
                            onExportarPdf = { id ->
                                exportandoPdf = true
                                scope.launch {
                                    try {
                                        val pdf = withContext(Dispatchers.IO) {
                                            val record = repository.load(id)
                                                ?: throw IllegalStateException("Registro no encontrado: $id")
                                            PdfExporter(context).exportPdf(
                                                record = record,
                                                tecnico = appState.tecnico.value,
                                                empresa = appState.empresa.value,
                                                repository = repository
                                            )
                                        }
                                        compartirArchivo(pdf, "application/pdf", chooserCompartirPdf)
                                    } catch (e: Exception) {
                                        form.mensaje.value = msgErrorPdf
                                    } finally {
                                        exportandoPdf = false
                                    }
                                }
                            },
                            onFinalizar = onFinalizar
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                enabled = paso > 0,
                onClick = { paso-- },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.btn_anterior))
            }
            if (paso == PASO_POTENCIOMETRO || paso == PASO_FOTOS) {
                TextButton(onClick = { paso++ }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.btn_omitir))
                }
            }
            if (paso < TOTAL_PASOS - 1) {
                Button(onClick = { paso++ }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.btn_siguiente))
                }
            }
        }
    }
}

/** Paso final: resumen de lo registrado, guardado y exportación. */
@Composable
private fun ResumenPaso(
    appState: AppState,
    form: RegistroFormState,
    registroGuardado: AforoRecord?,
    guardadoId: String?,
    exportando: Boolean,
    exportandoPdf: Boolean,
    onGuardar: () -> Unit,
    onExportarZip: (String) -> Unit,
    onExportarPdf: (String) -> Unit,
    onFinalizar: () -> Unit
) {
    SectionCard(stringResource(R.string.title_resumen)) {
        form.recordId.value?.let { id ->
            Text(stringResource(R.string.label_id_registro, id), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.padding(top = 4.dp))
        }
        Text(stringResource(R.string.historial_fecha, form.fecha.value))
        if (form.cliente.value.isNotBlank()) {
            Text(stringResource(R.string.historial_cliente, form.cliente.value))
        }
        if (form.fotos.value.isNotEmpty()) {
            Text(stringResource(R.string.historial_fotos_count, form.fotos.value.size))
        }
        Spacer(modifier = Modifier.padding(top = 8.dp))

        // Antes de guardar se muestran los datos acumulados; después, los del
        // registro guardado (guardar limpia el acumulado).
        val resultados = registroGuardado?.resultados ?: appState.datosInforme.value
        if (resultados.isNotEmpty()) {
            ResultadosAgrupados(resultados)
        }

        Spacer(modifier = Modifier.padding(top = 8.dp))
        Button(
            enabled = guardadoId == null,
            onClick = onGuardar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_guardar_registro))
        }

        if (guardadoId != null) {
            Spacer(modifier = Modifier.padding(top = 8.dp))
            OutlinedButton(
                enabled = !exportandoPdf,
                onClick = { onExportarPdf(guardadoId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(if (exportandoPdf) R.string.msg_generando_pdf else R.string.btn_exportar_pdf))
            }
            Spacer(modifier = Modifier.padding(top = 4.dp))
            OutlinedButton(
                enabled = !exportando,
                onClick = { onExportarZip(guardadoId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(if (exportando) R.string.msg_exportando else R.string.btn_exportar_compartir))
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Button(onClick = onFinalizar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.btn_finalizar))
            }
        }

        form.mensaje.value?.let {
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
