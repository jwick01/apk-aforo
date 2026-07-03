package com.hansbarrera.aditivosaforo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun fechaDeHoy(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

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

/**
 * Estado del formulario de registro de aforo, compartido entre la pestaña
 * "Aforo" (formulario libre) y el asistente guiado. Se crea con
 * [rememberRegistroForm], que es además el ÚNICO dueño del autoguardado del
 * borrador (los dos anfitriones nunca están compuestos a la vez).
 */
class RegistroFormState(
    val recordId: MutableState<String?>,
    val fecha: MutableState<String>,
    val cliente: MutableState<String>,
    val proyectoOMina: MutableState<String>,
    val lugarAforo: MutableState<String>,
    val operador: MutableState<String>,
    val numeroEquipo: MutableState<String>,
    val odometro: MutableState<String>,
    val observaciones: MutableState<String>,
    val fotos: MutableState<List<String>>,
    val fotoNotas: MutableState<Map<String, String>>,
    val mensaje: MutableState<String?>
) {
    /** Devuelve el ID del registro, asignando uno nuevo si aún no existe. */
    fun idActual(repository: AforoRepository): String {
        val existente = recordId.value
        if (existente != null) return existente
        val nuevo = repository.nextId(fecha.value)
        recordId.value = nuevo
        return nuevo
    }

    fun buildSnapshot(id: String, resultados: Map<String, String>): AforoRecord = AforoRecord(
        id = id,
        fecha = fecha.value,
        cliente = cliente.value,
        proyectoOMina = proyectoOMina.value,
        lugarAforo = lugarAforo.value,
        operador = operador.value,
        numeroEquipo = numeroEquipo.value,
        odometro = odometro.value,
        observaciones = observaciones.value,
        resultados = resultados,
        fotos = fotos.value,
        fotoNotas = fotoNotas.value
    )

    /**
     * Guarda el registro definitivo. Solo tras un guardado exitoso limpia el
     * informe y el borrador; si la escritura falla, la excepción se propaga y
     * los datos siguen en el formulario. Devuelve el registro guardado.
     */
    suspend fun guardarRegistro(repository: AforoRepository, appState: AppState): AforoRecord {
        val id = idActual(repository)
        val snapshot = buildSnapshot(id, appState.datosInforme.value)
        withContext(Dispatchers.IO) { repository.save(snapshot) }
        appState.limpiarDatosInforme()
        withContext(Dispatchers.IO) { repository.clearDraft() }
        return snapshot
    }

    /** Borra todo para comenzar un nuevo aforo (formulario + calculadoras + borrador). */
    fun reset(appState: AppState, repository: AforoRepository) {
        recordId.value = null
        fecha.value = fechaDeHoy()
        cliente.value = ""
        proyectoOMina.value = ""
        lugarAforo.value = ""
        operador.value = ""
        numeroEquipo.value = ""
        odometro.value = ""
        observaciones.value = ""
        fotos.value = emptyList()
        fotoNotas.value = emptyMap()
        mensaje.value = null
        appState.reiniciarTodo()
        repository.clearDraft()
    }
}

/**
 * Crea el estado del formulario a partir del borrador guardado y posee la
 * siembra de resultados y el autoguardado. Debe llamarse UNA sola vez por
 * pantalla anfitriona.
 */
@Composable
fun rememberRegistroForm(appState: AppState, repository: AforoRepository): RegistroFormState {
    val resetKey = appState.formResetTrigger.value
    val editKey = appState.editarRegistroTrigger.value
    val draft = remember(resetKey, editKey) { repository.loadDraft() }

    val recordId = rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.id?.takeIf { it.isNotBlank() }) }
    val fecha = rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.fecha?.takeIf { it.isNotBlank() } ?: fechaDeHoy()) }
    val cliente = rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.cliente ?: "") }
    val proyectoOMina = rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.proyectoOMina ?: "") }
    val lugarAforo = rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.lugarAforo ?: "") }
    val operador = rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.operador ?: "") }
    val numeroEquipo = rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.numeroEquipo ?: "") }
    val odometro = rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.odometro ?: "") }
    val observaciones = rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.observaciones ?: "") }
    val fotos = rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.fotos ?: emptyList()) }
    val fotoNotas = rememberSaveable(resetKey, editKey) { mutableStateOf(draft?.fotoNotas ?: emptyMap()) }
    val mensaje = rememberSaveable(resetKey, editKey) { mutableStateOf<String?>(null) }

    val state = remember(resetKey, editKey) {
        RegistroFormState(
            recordId, fecha, cliente, proyectoOMina, lugarAforo, operador,
            numeroEquipo, odometro, observaciones, fotos, fotoNotas, mensaje
        )
    }

    // Restaura los datos de cálculo guardados en el borrador.
    LaunchedEffect(resetKey, editKey) {
        draft?.resultados?.takeIf { it.isNotEmpty() }?.let { appState.registrarDatos(it) }
    }

    // Guarda automáticamente como borrador mientras se completa el formulario, para no
    // perder los datos si la app se cierra antes de presionar "Guardar registro".
    LaunchedEffect(
        fecha.value, cliente.value, proyectoOMina.value, lugarAforo.value, operador.value,
        numeroEquipo.value, odometro.value, observaciones.value, fotos.value, fotoNotas.value,
        recordId.value, appState.datosInforme.value
    ) {
        val vacio = recordId.value == null &&
            fecha.value == fechaDeHoy() &&
            cliente.value.isBlank() && proyectoOMina.value.isBlank() && lugarAforo.value.isBlank() &&
            operador.value.isBlank() && numeroEquipo.value.isBlank() && odometro.value.isBlank() &&
            observaciones.value.isBlank() &&
            fotos.value.isEmpty() && appState.datosInforme.value.isEmpty()

        if (vacio) {
            withContext(Dispatchers.IO) { repository.clearDraft() }
        } else {
            val snapshot = state.buildSnapshot(recordId.value ?: "", appState.datosInforme.value)
            withContext(Dispatchers.IO) { repository.saveDraft(snapshot) }
        }
    }

    return state
}

/** Sección "Datos del trabajo": fecha con selector, cliente, proyecto, etc. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatosTrabajoSection(state: RegistroFormState) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    SectionCard(stringResource(R.string.section_datos_trabajo)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.fecha.value,
                onValueChange = { state.fecha.value = it },
                label = { Text(stringResource(R.string.label_fecha)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = { showDatePicker = true }) {
                Text(stringResource(R.string.btn_elegir_fecha))
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = fechaAMillisUtc(state.fecha.value))
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis -> state.fecha.value = millisUtcAFecha(millis) }
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

        CampoTexto(state.cliente, R.string.label_cliente)
        CampoTexto(state.proyectoOMina, R.string.label_proyecto_mina)
        CampoTexto(state.lugarAforo, R.string.label_lugar_aforo)
        CampoTexto(state.operador, R.string.label_operador)
        CampoTexto(state.numeroEquipo, R.string.label_numero_equipo)
        CampoTexto(state.odometro, R.string.label_odometro)
        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = state.observaciones.value,
            onValueChange = { state.observaciones.value = it },
            label = { Text(stringResource(R.string.label_observaciones)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CampoTexto(valor: MutableState<String>, etiqueta: Int) {
    Spacer(modifier = Modifier.padding(top = 8.dp))
    OutlinedTextField(
        value = valor.value,
        onValueChange = { valor.value = it },
        label = { Text(stringResource(etiqueta)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Sección "Fotografías": cámara, galería, miniaturas y notas por foto. */
@Composable
fun FotosSection(state: RegistroFormState, repository: AforoRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Solo la ruta (String) es Saveable: así la foto no se pierde si el sistema
    // mata la app mientras la cámara está abierta en primer plano.
    var pendingCameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    val msgPermisoCamara = stringResource(R.string.msg_permiso_camara)

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val archivo = pendingCameraPath?.let { File(it) }
        if (exito && archivo != null) {
            state.fotos.value = state.fotos.value + archivo.name
        } else {
            archivo?.delete()
        }
        pendingCameraPath = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        val archivo = pendingCameraPath?.let { File(it) }
        if (concedido && archivo != null) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
            cameraLauncher.launch(uri)
        } else {
            archivo?.delete()
            pendingCameraPath = null
            if (!concedido) {
                state.mensaje.value = msgPermisoCamara
            }
        }
    }

    fun tomarFoto() {
        val id = state.idActual(repository)
        val archivo = repository.nextPhotoFile(id)
        pendingCameraPath = archivo.absolutePath
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val id = state.idActual(repository)
            val destino = repository.nextPhotoFile(id)
            scope.launch {
                val copiada = try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            destino.outputStream().use { output -> input.copyTo(output) }
                        } != null
                    }
                } catch (e: Exception) {
                    false
                }
                if (copiada) {
                    state.fotos.value = state.fotos.value + destino.name
                } else {
                    destino.delete()
                }
            }
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

        val fotos = state.fotos.value
        if (fotos.isNotEmpty()) {
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(stringResource(R.string.label_fotos_count, fotos.size), style = MaterialTheme.typography.labelLarge)
            fotos.forEach { nombre ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val id = state.recordId.value
                    if (id != null) {
                        FotoThumbnail(repository.photoFile(id, nombre))
                    }
                    Text(nombre, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        state.recordId.value?.let { repository.deletePhoto(it, nombre) }
                        state.fotos.value = state.fotos.value - nombre
                        state.fotoNotas.value = state.fotoNotas.value - nombre
                    }) {
                        Text(stringResource(R.string.btn_quitar))
                    }
                }
                OutlinedTextField(
                    value = state.fotoNotas.value[nombre] ?: "",
                    onValueChange = { state.fotoNotas.value = state.fotoNotas.value + (nombre to it) },
                    label = { Text(stringResource(R.string.label_nota_foto)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
internal fun FotoThumbnail(file: File) {
    if (!file.exists()) return
    val bitmapState by produceState<Bitmap?>(initialValue = null, file.path, file.lastModified()) {
        value = withContext(Dispatchers.IO) { decodeSampledBitmap(file, 160) }
    }
    val bitmap = bitmapState
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
