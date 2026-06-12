package com.hansbarrera.aditivosaforo.ui.screens

import android.content.Intent
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.hansbarrera.aditivosaforo.AppState
import com.hansbarrera.aditivosaforo.data.AforoRecord
import com.hansbarrera.aditivosaforo.data.AforoRepository
import com.hansbarrera.aditivosaforo.ui.components.SectionCard
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun fechaDeHoy(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

@Composable
fun AforoScreen(appState: AppState) {
    var tabIndex by rememberSaveable { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Registro de aforo", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(top = 8.dp))

        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Nuevo registro") })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Historial") })
        }

        Spacer(modifier = Modifier.padding(top = 12.dp))

        if (tabIndex == 0) {
            NuevoRegistroTab(appState)
        } else {
            HistorialTab()
        }
    }
}

@Composable
private fun NuevoRegistroTab(appState: AppState) {
    val context = LocalContext.current
    val repository = remember { AforoRepository(context) }
    val draft = remember { repository.loadDraft() }

    var recordId by rememberSaveable { mutableStateOf(draft?.id?.takeIf { it.isNotBlank() }) }
    var fecha by rememberSaveable { mutableStateOf(draft?.fecha?.takeIf { it.isNotBlank() } ?: fechaDeHoy()) }
    var cliente by rememberSaveable { mutableStateOf(draft?.cliente ?: "") }
    var proyectoOMina by rememberSaveable { mutableStateOf(draft?.proyectoOMina ?: "") }
    var lugarAforo by rememberSaveable { mutableStateOf(draft?.lugarAforo ?: "") }
    var operador by rememberSaveable { mutableStateOf(draft?.operador ?: "") }
    var numeroEquipo by rememberSaveable { mutableStateOf(draft?.numeroEquipo ?: "") }
    var odometro by rememberSaveable { mutableStateOf(draft?.odometro ?: "") }
    var observaciones by rememberSaveable { mutableStateOf(draft?.observaciones ?: "") }

    var fotos by rememberSaveable { mutableStateOf(draft?.fotos ?: emptyList()) }
    var fotoNotas by rememberSaveable { mutableStateOf(draft?.fotoNotas ?: emptyMap()) }
    var mensaje by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

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

    SectionCard("Datos del trabajo") {
        OutlinedTextField(
            value = fecha,
            onValueChange = { fecha = it },
            label = { Text("Fecha (aaaa-mm-dd)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = cliente,
            onValueChange = { cliente = it },
            label = { Text("Cliente") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = proyectoOMina,
            onValueChange = { proyectoOMina = it },
            label = { Text("Proyecto o mina") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = lugarAforo,
            onValueChange = { lugarAforo = it },
            label = { Text("Lugar del aforo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = operador,
            onValueChange = { operador = it },
            label = { Text("Operador") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = numeroEquipo,
            onValueChange = { numeroEquipo = it },
            label = { Text("Número de equipo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = odometro,
            onValueChange = { odometro = it },
            label = { Text("Odómetro") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = observaciones,
            onValueChange = { observaciones = it },
            label = { Text("Observaciones") },
            modifier = Modifier.fillMaxWidth()
        )
    }

    SectionCard("Resultados guardados") {
        val datos = appState.datosInforme.value
        if (datos.isEmpty()) {
            Text(
                "Aún no hay datos guardados. Usa los botones \"Usar este resultado...\" o " +
                    "\"Guardar estos datos...\" en las otras pestañas para traerlos aquí.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            datos.forEach { (clave, valor) -> Text("$clave: $valor") }
        }
    }

    SectionCard("Fotografías") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val id = idActual()
                    val archivo = repository.nextPhotoFile(id)
                    pendingCameraFile = archivo
                    val uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", archivo
                    )
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Tomar foto")
            }
            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Cargar imagen")
            }
        }

        if (fotos.isNotEmpty()) {
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text("Fotos (${fotos.size}):", style = MaterialTheme.typography.labelLarge)
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
                        Text("Quitar")
                    }
                }
                OutlinedTextField(
                    value = fotoNotas[nombre] ?: "",
                    onValueChange = { fotoNotas = fotoNotas + (nombre to it) },
                    label = { Text("Nota / pie de foto") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                )
            }
        }
    }

    SectionCard("Guardar y exportar") {
        recordId?.let { id ->
            Text("ID del registro: $id", style = MaterialTheme.typography.labelLarge)
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
                mensaje = "Registro guardado: $id"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar registro")
        }

        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedButton(
            onClick = {
                val id = recordId
                if (id == null) {
                    mensaje = "Guarda el registro antes de exportarlo."
                } else {
                    val zip = repository.exportZip(id)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zip)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir registro de aforo"))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Exportar y compartir")
        }

        Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedButton(
            onClick = {
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
                appState.limpiarDatosInforme()
                repository.clearDraft()
                mensaje = null
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Limpiar formulario (nuevo registro)")
        }

        mensaje?.let {
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun HistorialTab() {
    val context = LocalContext.current
    val repository = remember { AforoRepository(context) }
    var registros by remember { mutableStateOf(repository.loadAll()) }

    if (registros.isEmpty()) {
        Text(
            "Aún no hay registros guardados. Crea uno en la pestaña \"Nuevo registro\".",
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    registros.forEach { record ->
        SectionCard(record.id) {
            Text("Fecha: ${record.fecha}")
            Text("Cliente: ${record.cliente}")
            Text("Proyecto o mina: ${record.proyectoOMina}")
            Text("Lugar del aforo: ${record.lugarAforo}")
            Text("Operador: ${record.operador}")
            Text("Número de equipo: ${record.numeroEquipo}")
            Text("Odómetro: ${record.odometro}")
            if (record.observaciones.isNotBlank()) {
                Text("Observaciones: ${record.observaciones}")
            }

            if (record.resultados.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Resultados:", style = MaterialTheme.typography.labelLarge)
                record.resultados.forEach { (clave, valor) -> Text("$clave: $valor") }
            }

            if (record.fotos.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("${record.fotos.size} foto(s)", style = MaterialTheme.typography.labelLarge)
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
                        val zip = repository.exportZip(record.id)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zip)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartir registro de aforo"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Exportar")
                }
                OutlinedButton(
                    onClick = {
                        repository.delete(record.id)
                        registros = repository.loadAll()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Eliminar")
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
