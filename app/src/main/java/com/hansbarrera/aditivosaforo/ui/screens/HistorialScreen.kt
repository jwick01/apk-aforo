package com.hansbarrera.aditivosaforo.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

@Composable
fun HistorialScreen(appState: AppState, onEditarRegistro: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { AforoRepository(context) }
    // null = cargando; la lectura de todos los registros se hace fuera del hilo principal.
    var registros by remember { mutableStateOf<List<AforoRecord>?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var exportandoId by remember { mutableStateOf<String?>(null) }
    var exportandoPdfId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val chooserCompartirRegistro = stringResource(R.string.chooser_compartir_registro)
    val chooserCompartirPdf = stringResource(R.string.chooser_compartir_pdf)
    val msgErrorImportar = stringResource(R.string.msg_error_importar)
    val msgErrorExportar = stringResource(R.string.msg_error_exportar)
    val msgErrorPdf = stringResource(R.string.msg_error_pdf)

    LaunchedEffect(reloadKey) {
        registros = withContext(Dispatchers.IO) { repository.loadAll() }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val record = withContext(Dispatchers.IO) { repository.importZip(uri) }
                    appState.cargarRegistroParaEditar(record.resultados)
                    reloadKey++
                    onEditarRegistro()
                } catch (e: Exception) {
                    mensaje = msgErrorImportar
                }
            }
        }
    }

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
        Text(stringResource(R.string.title_historial), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(top = 12.dp))

        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_importar_informe))
        }
        mensaje?.let {
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.padding(top = 12.dp))

        val lista = registros
        if (lista == null) {
            Text(
                stringResource(R.string.msg_cargando_registros),
                style = MaterialTheme.typography.bodyMedium
            )
            return@Column
        }
        if (lista.isEmpty()) {
            Text(
                stringResource(R.string.msg_no_registros),
                style = MaterialTheme.typography.bodyMedium
            )
            return@Column
        }

        lista.forEach { record ->
            SectionCard(record.id) {
                Text(stringResource(R.string.historial_fecha, record.fecha))
                Text(stringResource(R.string.historial_cliente, record.cliente))
                Text(stringResource(R.string.historial_proyecto, record.proyectoOMina))
                Text(stringResource(R.string.historial_lugar, record.lugarAforo))
                Text(stringResource(R.string.historial_operador, record.operador))
                Text(stringResource(R.string.historial_equipo, record.numeroEquipo))
                Text(stringResource(R.string.historial_odometro, record.odometro))
                if (record.observaciones.isNotBlank()) {
                    Text(stringResource(R.string.historial_observaciones, record.observaciones))
                }

                if (record.resultados.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(stringResource(R.string.historial_resultados), style = MaterialTheme.typography.labelLarge)
                    ResultadosAgrupados(record.resultados)
                }

                if (record.fotos.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(stringResource(R.string.historial_fotos_count, record.fotos.size), style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                    record.fotos.forEach { nombre ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FotoThumbnail(repository.photoFile(record.id, nombre))
                            val nota = record.fotoNotas[nombre]
                            Text(
                                if (nota.isNullOrBlank()) nombre else "$nombre: $nota",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(top = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { repository.saveDraft(record) }
                                appState.cargarRegistroParaEditar(record.resultados)
                                onEditarRegistro()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.btn_editar))
                    }
                    OutlinedButton(
                        enabled = exportandoId == null,
                        onClick = {
                            exportandoId = record.id
                            scope.launch {
                                try {
                                    val zip = withContext(Dispatchers.IO) { repository.exportZip(record.id) }
                                    compartirArchivo(zip, "application/zip", chooserCompartirRegistro)
                                } catch (e: Exception) {
                                    mensaje = msgErrorExportar
                                } finally {
                                    exportandoId = null
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(if (exportandoId == record.id) R.string.msg_exportando else R.string.btn_exportar))
                    }
                }
                Spacer(modifier = Modifier.padding(top = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        enabled = exportandoPdfId == null,
                        onClick = {
                            exportandoPdfId = record.id
                            scope.launch {
                                try {
                                    val pdf = withContext(Dispatchers.IO) {
                                        PdfExporter(context).exportPdf(
                                            record = record,
                                            tecnico = appState.tecnico.value,
                                            empresa = appState.empresa.value,
                                            repository = repository
                                        )
                                    }
                                    compartirArchivo(pdf, "application/pdf", chooserCompartirPdf)
                                } catch (e: Exception) {
                                    mensaje = msgErrorPdf
                                } finally {
                                    exportandoPdfId = null
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(if (exportandoPdfId == record.id) R.string.msg_generando_pdf else R.string.btn_exportar_pdf))
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { repository.delete(record.id) }
                                reloadKey++
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.btn_eliminar))
                    }
                }
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
        }
    }
}
