package com.hansbarrera.aditivosaforo.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.hansbarrera.aditivosaforo.AppState
import com.hansbarrera.aditivosaforo.R
import com.hansbarrera.aditivosaforo.data.AforoRecord
import com.hansbarrera.aditivosaforo.data.AforoRepository
import com.hansbarrera.aditivosaforo.ui.components.SectionCard
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun fechaDeHoy(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

/** Convierte "yyyy-MM-dd" a millis UTC de medianoche, para inicializar el DatePicker. */
private fun fechaAMillisUtc(fecha: String): Long? = try {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.parse(fecha)?.time
} catch (e: Exception) {
    null
}

/** Convierte los millis UTC que entrega el DatePicker de vuelta a "yyyy-MM-dd". */
private fun millisUtcAFecha(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(millis))

/** Agrupa resultados con claves "Sección - Campo" por sección, para mostrarlos ordenados. */
private fun agruparResultados(datos: Map<String, String>): Map<String, List<Pair<String, String>>> {
    val grupos = LinkedHashMap<String, MutableList<Pair<String, String>>>()
    datos.forEach { (clave, valor) ->
        val sep = clave.indexOf(" - ")
        val seccion = if (sep >= 0) clave.substring(0, sep) else "General"
        val campo = if (sep >= 0) clave.substring(sep + 3) else clave
        grupos.getOrPut(seccion) { mutableListOf() }.add(campo to valor)
    }
    return grupos
}

@Composable
private fun ResultadosAgrupados(datos: Map<String, String>) {
    agruparResultados(datos).forEach { (seccion, campos) ->
        Text(seccion, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        campos.forEach { (campo, valor) -> Text("  $campo: $valor") }
        Spacer(modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun AforoScreen(appState: AppState) {
    var tabIndex by rememberSaveable { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.title_aforo), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(top = 8.dp))

        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text(stringResource(R.string.tab_nuevo_registro)) })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text(stringResource(R.string.tab_historial)) })
        }

        Spacer(modifier = Modifier.padding(top = 12.dp))

        if (tabIndex == 0) {
            NuevoRegistroTab(appState)
        } else {
            HistorialTab(appState, onEditar = { tabIndex = 0 })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevoRegistroTab(appState: AppState) {
    val context = LocalContext.current
    val repository = remember { AforoRepository(context) }
    val resetKey = appState.formResetTrigger.value
    val editKey = appState.editarRegistroTrigger.value
    val draft = remember(resetKey, editKey) { repository.loadDraft() }

    var recordId by rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.id?.takeIf { it.isNotBlank() }) }
    var fecha by rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.fecha?.takeIf { it.isNotBlank() } ?: fechaDeHoy()) }
    var cliente by rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.cliente ?: "") }
    var proyectoOMina by rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.proyectoOMina ?: "") }
    var lugarAforo by rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.lugarAforo ?: "") }
    var operador by rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.operador ?: "") }
    var numeroEquipo by rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.numeroEquipo ?: "") }
    var odometro by rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.odometro ?: "") }
    var observaciones by rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.observaciones ?: "") }

    var fotos by rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.fotos ?: emptyList()) }
    var fotoNotas by rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.fotoNotas ?: emptyMap()) }
    var mensaje by rememberSaveable(resetKey, editKey) { mutableStateOf<String?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showConfirmReset by rememberSaveable { mutableStateOf(false) }

    val msgPermisoCamara = stringResource(R.string.msg_permiso_camara)
    val msgRegistroGuardado = stringResource(R.string.msg_registro_guardado)
    val msgGuardaAntesExportar = stringResource(R.string.msg_guarda_antes_exportar)
    val chooserCompartirRegistro = stringResource(R.string.chooser_compartir_registro)

    // Restaura, al entrar a la pantalla, los datos de cálculo guardados en el borrador.
    LaunchedEffect(Unit) {
        draft?.resultados?.takeIf { it.isNotEmpty() }?.let { appState.registrarDatos(it) }
    }

    // Guarda automáticamente como borrador mientras se completa el formulario, para no
    // perder los datos si la app se cierra antes de presionar "Guardar registro".
    LaunchedEffect(
        fecha, cliente, proyectoOMina, lugarAforo, operador, numeroEquipo, odometro, observaciones,
        fotos, fotoNotas, recordId, appState.datosInforme.value
    ) {
        val vacio = recordId == null &&
            fecha == fechaDeHoy() &&
            cliente.isBlank() && proyectoOMina.isBlank() && lugarAforo.isBlank() &&
            operador.isBlank() && numeroEquipo.isBlank() && odometro.isBlank() && observaciones.isBlank() &&
            fotos.isEmpty() && appState.datosInforme.value.isEmpty()

        if (vacio) {
            repository.clearDraft()
        } else {
            repository.saveDraft(
                AforoRecord(
                    id = recordId ?: "",
                    fecha = fecha,
                    cliente = cliente,
                    proyectoOMina = proyectoOMina,
                    lugarAforo = lugarAforo,
                    operador = operador,
                    numeroEquipo = numeroEquipo,
                    odometro = odometro,
                    observaciones = observaciones,
                    resultados = appState.datosInforme.value,
                    fotos = fotos,
                    fotoNotas = fotoNotas
                )
            )
        }
    }

    fun idActual(): String {
        val existente = recordId
        if (existente != null) return existente
        val nuevo = repository.nextId(fecha)
        recordId = nuevo
        return nuevo
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val archivo = pendingCameraFile
        if (exito && archivo != null) {
            fotos = fotos + archivo.name
        } else {
            archivo?.delete()
        }
        pendingCameraFile = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        val archivo = pendingCameraFile
        if (concedido && archivo != null) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
            cameraLauncher.launch(uri)
        } else {
            archivo?.delete()
            pendingCameraFile = null
            if (!concedido) {
                mensaje = msgPermisoCamara
            }
        }
    }

    fun tomarFoto() {
        val id = idActual()
        val archivo = repository.nextPhotoFile(id)
        pendingCameraFile = archivo
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val id = idActual()
            val destino = repository.nextPhotoFile(id)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destino.outputStream().use { output -> input.copyTo(output) }
            }
            fotos = fotos + destino.name
        }
    }

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
                    recordId = null
                    fecha = fechaDeHoy()
                    cliente = ""
                    proyectoOMina = ""
                    lugarAforo = ""
                    operador = ""
                    numeroEquipo = ""
                    odometro = ""
                    observaciones = ""
                    fotos = emptyList()
                    fotoNotas = emptyMap()
                    mensaje = null
                    appState.reiniciarTodo()
                    repository.clearDraft()
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

    SectionCard(stringResource(R.string.section_datos_trabajo)) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = fecha,
                onValueChange = { fecha = it },
                label = { Text(stringResource(R.string.label_fecha)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = { showDatePicker = true }) {
                Text(stringResource(R.string.btn_elegir_fecha))
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = fechaAMillisUtc(fecha))
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis -> fecha = millisUtcAFecha(millis) }
                        showDatePicker = false
                    }) {
                        Text(stringResource(R.string.btn_aceptar))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.btn_cancelar))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = cliente,
            onValueChange = { cliente = it },
            label = { Text(stringResource(R.string.label_cliente)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = proyectoOMina,
            onValueChange = { proyectoOMina = it },
            label = { Text(stringResource(R.string.label_proyecto_mina)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = lugarAforo,
            onValueChange = { lugarAforo = it },
            label = { Text(stringResource(R.string.label_lugar_aforo)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = operador,
            onValueChange = { operador = it },
            label = { Text(stringResource(R.string.label_operador)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = numeroEquipo,
            onValueChange = { numeroEquipo = it },
            label = { Text(stringResource(R.string.label_numero_equipo)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = odometro,
            onValueChange = { odometro = it },
            label = { Text(stringResource(R.string.label_odometro)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = observaciones,
            onValueChange = { observaciones = it },
            label = { Text(stringResource(R.string.label_observaciones)) },
            modifier = Modifier.fillMaxWidth()
        )
    }

    SectionCard(stringResource(R.string.section_resultados_guardados)) {
        val datos = appState.datosInforme.value
        if (datos.isEmpty()) {
            Text(
                stringResource(R.string.msg_no_datos_guardados),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            ResultadosAgrupados(datos)
        }
    }

    SectionCard(stringResource(R.string.section_fotografias)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { tomarFoto() },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.btn_tomar_foto))
            }
            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.btn_cargar_imagen))
            }
        }

        if (fotos.isNotEmpty()) {
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(stringResource(R.string.label_fotos_count, fotos.size), style = MaterialTheme.typography.labelLarge)
            fotos.forEach { nombre ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val id = recordId
                    if (id != null) {
                        FotoThumbnail(repository.photoFile(id, nombre))
                    }
                    Text(nombre, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        recordId?.let { repository.deletePhoto(it, nombre) }
                        fotos = fotos - nombre
                        fotoNotas = fotoNotas - nombre
                    }) {
                        Text(stringResource(R.string.btn_quitar))
                    }
                }
                OutlinedTextField(
                    value = fotoNotas[nombre] ?: "",
                    onValueChange = { fotoNotas = fotoNotas + (nombre to it) },
                    label = { Text(stringResource(R.string.label_nota_foto)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                )
            }
        }
    }

    SectionCard(stringResource(R.string.section_guardar_exportar)) {
        recordId?.let { id ->
            Text(stringResource(R.string.label_id_registro, id), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.padding(top = 8.dp))
        }

        Button(
            onClick = {
                val id = idActual()
                repository.save(
                    AforoRecord(
                        id = id,
                        fecha = fecha,
                        cliente = cliente,
                        proyectoOMina = proyectoOMina,
                        lugarAforo = lugarAforo,
                        operador = operador,
                        numeroEquipo = numeroEquipo,
                        odometro = odometro,
                        observaciones = observaciones,
                        resultados = appState.datosInforme.value,
                        fotos = fotos,
                        fotoNotas = fotoNotas
                    )
                )
                appState.limpiarDatosInforme()
                repository.clearDraft()
                mensaje = String.format(msgRegistroGuardado, id)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_guardar_registro))
        }

        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedButton(
            onClick = {
                val id = recordId
                if (id == null) {
                    mensaje = msgGuardaAntesExportar
                } else {
                    val zip = repository.exportZip(id)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zip)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, chooserCompartirRegistro))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_exportar_compartir))
        }

        mensaje?.let {
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun HistorialTab(appState: AppState, onEditar: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { AforoRepository(context) }
    var registros by remember { mutableStateOf(repository.loadAll()) }
    val chooserCompartirRegistro = stringResource(R.string.chooser_compartir_registro)

    if (registros.isEmpty()) {
        Text(
            stringResource(R.string.msg_no_registros),
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    registros.forEach { record ->
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
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
                        repository.saveDraft(record)
                        appState.cargarRegistroParaEditar(record.resultados)
                        onEditar()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_editar))
                }
                OutlinedButton(
                    onClick = {
                        val zip = repository.exportZip(record.id)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zip)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, chooserCompartirRegistro))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_exportar))
                }
                OutlinedButton(
                    onClick = {
                        repository.delete(record.id)
                        registros = repository.loadAll()
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

@Composable
private fun FotoThumbnail(file: File) {
    if (!file.exists()) return
    val bitmap = remember(file.path, file.lastModified()) { decodeSampledBitmap(file, 160) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    }
}

private fun decodeSampledBitmap(file: File, reqSize: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (bounds.outWidth / sample > reqSize || bounds.outHeight / sample > reqSize) {
        sample *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(file.path, options)
}
