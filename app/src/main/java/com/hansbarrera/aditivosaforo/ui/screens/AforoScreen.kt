package com.hansbarrera.aditivosaforo.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.hansbarrera.aditivosaforo.AppState
import com.hansbarrera.aditivosaforo.R
import com.hansbarrera.aditivosaforo.data.AforoRepository
import com.hansbarrera.aditivosaforo.data.PdfExporter
import com.hansbarrera.aditivosaforo.ui.components.SectionCard
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pestaña "Aforo": formulario libre del registro (datos del trabajo, fotos,
 * guardar y exportar). El historial vive en su propia pestaña y el asistente
 * guiado en la ruta "guiado"; los tres comparten [rememberRegistroForm].
 */
@Composable
fun AforoScreen(appState: AppState) {
    val context = LocalContext.current
    val repository = remember { AforoRepository(context) }
    val form = rememberRegistroForm(appState, repository)
    var showConfirmReset by rememberSaveable { mutableStateOf(false) }
    var exportando by remember { mutableStateOf(false) }
    var exportandoPdf by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val msgRegistroGuardado = stringResource(R.string.msg_registro_guardado)
    val msgGuardaAntesExportar = stringResource(R.string.msg_guarda_antes_exportar)
    val msgErrorGuardar = stringResource(R.string.msg_error_guardar)
    val msgErrorExportar = stringResource(R.string.msg_error_exportar)
    val msgErrorPdf = stringResource(R.string.msg_error_pdf)
    val chooserCompartirRegistro = stringResource(R.string.chooser_compartir_registro)
    val chooserCompartirPdf = stringResource(R.string.chooser_compartir_pdf)

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
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.title_aforo), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(top = 12.dp))

        OutlinedButton(
            onClick = { showConfirmReset = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_nuevo_aforo))
        }
        Spacer(modifier = Modifier.padding(top = 12.dp))

        if (showConfirmReset) {
            AlertDialog(
                onDismissRequest = { showConfirmReset = false },
                title = { Text(stringResource(R.string.dialog_confirmar_nuevo_aforo_titulo)) },
                text = { Text(stringResource(R.string.dialog_confirmar_nuevo_aforo_mensaje)) },
                confirmButton = {
                    TextButton(onClick = {
                        form.reset(appState, repository)
                        showConfirmReset = false
                    }) {
                        Text(stringResource(R.string.btn_si_borrar_todo))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmReset = false }) {
                        Text(stringResource(R.string.btn_cancelar))
                    }
                }
            )
        }

        DatosTrabajoSection(form)
        FotosSection(form, repository)

        SectionCard(stringResource(R.string.section_guardar_exportar)) {
            form.recordId.value?.let { id ->
                Text(stringResource(R.string.label_id_registro, id), style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.padding(top = 8.dp))
            }

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val guardado = form.guardarRegistro(repository, appState)
                            form.mensaje.value = String.format(msgRegistroGuardado, guardado.id)
                        } catch (e: Exception) {
                            form.mensaje.value = msgErrorGuardar
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_guardar_registro))
            }

            Spacer(modifier = Modifier.padding(top = 8.dp))
            OutlinedButton(
                enabled = !exportando,
                onClick = {
                    val id = form.recordId.value
                    if (id == null) {
                        form.mensaje.value = msgGuardaAntesExportar
                    } else {
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
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(if (exportando) R.string.msg_exportando else R.string.btn_exportar_compartir))
            }

            Spacer(modifier = Modifier.padding(top = 8.dp))
            OutlinedButton(
                enabled = !exportandoPdf,
                onClick = {
                    val id = form.recordId.value
                    if (id == null) {
                        form.mensaje.value = msgGuardaAntesExportar
                    } else {
                        exportandoPdf = true
                        scope.launch {
                            try {
                                val pdf = withContext(Dispatchers.IO) {
                                    val record = repository.load(id)
                                        ?: throw IllegalArgumentException("Registro no encontrado: $id")
                                    PdfExporter(context).exportPdf(
                                        record = record,
                                        tecnico = appState.tecnico.value,
                                        empresa = appState.empresa.value,
                                        repository = repository
                                    )
                                }
                                compartirArchivo(pdf, "application/pdf", chooserCompartirPdf)
                            } catch (e: IllegalArgumentException) {
                                form.mensaje.value = msgGuardaAntesExportar
                            } catch (e: Exception) {
                                form.mensaje.value = msgErrorPdf
                            } finally {
                                exportandoPdf = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(if (exportandoPdf) R.string.msg_generando_pdf else R.string.btn_exportar_pdf))
            }

            form.mensaje.value?.let {
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
