package com.hansbarrera.aditivosaforo.data

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Almacenamiento local de los registros de aforo: cada registro vive en su
 * propia carpeta dentro de `filesDir/aforos/<id>/`, con un archivo `datos.json`
 * y las fotografías asociadas (foto01.jpg, foto02.jpg, ...).
 *
 * Permite exportar un registro completo (datos + fotos) como un .zip listo
 * para compartir, por ejemplo con la app de Claude para generar el informe.
 */
class AforoRepository(private val context: Context) {

    private val baseDir: File
        get() = File(context.filesDir, "aforos").apply { mkdirs() }

    private fun recordDir(id: String): File = File(baseDir, id).apply { mkdirs() }

    private fun datosFile(id: String): File = File(recordDir(id), "datos.json")

    private val borradorFile: File
        get() = File(context.filesDir, "borrador_aforo.json")

    /**
     * Escribe primero a un archivo temporal y luego lo renombra, para que un
     * cierre a mitad de escritura nunca deje un JSON truncado en la ruta final.
     */
    private fun writeTextAtomic(destino: File, texto: String) {
        val temporal = File(destino.parentFile, destino.name + ".tmp")
        temporal.writeText(texto)
        if (!temporal.renameTo(destino)) {
            destino.delete()
            if (!temporal.renameTo(destino)) {
                destino.writeText(texto)
                temporal.delete()
            }
        }
    }

    /** Guarda el formulario en curso como borrador, para no perderlo si se cierra la app. */
    fun saveDraft(record: AforoRecord) {
        writeTextAtomic(borradorFile, record.toJson().toString(2))
    }

    /** Recupera el borrador guardado, si existe. */
    fun loadDraft(): AforoRecord? {
        val file = borradorFile
        if (!file.exists()) return null
        return try {
            AforoRecord.fromJson(JSONObject(file.readText()))
        } catch (e: Exception) {
            // Borrador corrupto (p. ej. app cerrada a mitad de escritura): se
            // descarta para no bloquear el arranque de la app.
            file.delete()
            null
        }
    }

    /** Elimina el borrador, por ejemplo tras guardar el registro definitivo. */
    fun clearDraft() {
        borradorFile.delete()
    }

    /** IDs de registros existentes, más recientes primero. */
    fun listIds(): List<String> {
        return baseDir.listFiles { f -> f.isDirectory }
            ?.map { it.name }
            ?.sortedDescending()
            ?: emptyList()
    }

    fun load(id: String): AforoRecord? {
        val file = datosFile(id)
        if (!file.exists()) return null
        return try {
            AforoRecord.fromJson(JSONObject(file.readText()))
        } catch (e: Exception) {
            // Registro corrupto: se omite en vez de hacer caer toda la lista.
            null
        }
    }

    fun loadAll(): List<AforoRecord> = listIds().mapNotNull { load(it) }

    fun save(record: AforoRecord) {
        writeTextAtomic(datosFile(record.id), record.toJson().toString(2))
    }

    fun delete(id: String) {
        recordDir(id).deleteRecursively()
    }

    /** Genera un ID correlativo del tipo AFORO-yyyy-MM-dd-NNN para la fecha dada. */
    fun nextId(fecha: String): String {
        val prefix = "AFORO-$fecha"
        val ultimoNumero = listIds()
            .filter { it.startsWith("$prefix-") }
            .mapNotNull { it.substringAfterLast("-").toIntOrNull() }
            .maxOrNull() ?: 0
        val siguiente = ultimoNumero + 1
        return "$prefix-${siguiente.toString().padStart(3, '0')}"
    }

    /** Siguiente nombre de archivo correlativo (foto01.jpg, foto02.jpg, ...) para el registro. */
    fun nextPhotoFile(id: String): File {
        val dir = recordDir(id)
        val ultimoNumero = dir.listFiles { f -> f.name.matches(Regex("foto\\d+\\.jpg")) }
            ?.mapNotNull { Regex("foto(\\d+)\\.jpg").find(it.name)?.groupValues?.get(1)?.toIntOrNull() }
            ?.maxOrNull() ?: 0
        val siguiente = ultimoNumero + 1
        return File(dir, "foto${siguiente.toString().padStart(2, '0')}.jpg")
    }

    fun photoFile(id: String, nombre: String): File = File(recordDir(id), nombre)

    fun deletePhoto(id: String, nombre: String) {
        File(recordDir(id), nombre).delete()
    }

    /**
     * Genera un .zip con `datos.json` y todas las fotografías del registro,
     * listo para compartir mediante un Intent.ACTION_SEND.
     */
    fun exportZip(id: String): File {
        val record = load(id) ?: throw IllegalArgumentException("Registro no encontrado: $id")
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val zipFile = File(exportDir, nombreArchivoExport(record))

        ZipOutputStream(zipFile.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("datos.json"))
            zos.write(record.toJson().toString(2).toByteArray())
            zos.closeEntry()

            record.fotos.forEach { nombreFoto ->
                val foto = photoFile(id, nombreFoto)
                if (foto.exists()) {
                    zos.putNextEntry(ZipEntry(nombreFoto))
                    foto.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        return zipFile
    }

    /** Nombre de archivo legible para el .zip exportado: incluye el ID (con fecha) y el cliente. */
    private fun nombreArchivoExport(record: AforoRecord): String {
        val clienteSanitizado = record.cliente.trim()
            .replace(Regex("[^A-Za-z0-9 _-]"), "")
            .replace(Regex("\\s+"), "_")
        val base = if (clienteSanitizado.isNotBlank()) "${record.id}_$clienteSanitizado" else record.id
        return "$base.zip"
    }

    /** Nombre simple sin rutas: evita que una entrada del zip escriba fuera de su carpeta. */
    private fun esNombreSeguro(nombre: String): Boolean {
        return nombre.isNotBlank() &&
            !nombre.contains('/') &&
            !nombre.contains('\\') &&
            !nombre.contains("..")
    }

    /**
     * Importa un .zip exportado previamente (datos.json + fotos), colocando las
     * fotos en la carpeta del registro y dejando los datos como borrador para
     * que se pueda seguir editando desde "Nuevo registro".
     *
     * Las entradas se copian por streaming a una carpeta temporal (sin cargar
     * todo el zip en memoria) y solo se aceptan nombres de archivo simples,
     * para que un zip manipulado no pueda escribir fuera de su carpeta.
     */
    fun importZip(uri: Uri): AforoRecord {
        val tempDir = File(context.cacheDir, "import_tmp_${System.nanoTime()}").apply { mkdirs() }
        try {
            val abierto = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("No se pudo abrir el archivo")
            abierto.use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && esNombreSeguro(entry.name)) {
                            File(tempDir, entry.name).outputStream().use { salida ->
                                zis.copyTo(salida)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            val datosImportados = File(tempDir, "datos.json")
            if (!datosImportados.exists()) {
                throw IllegalArgumentException("El archivo no contiene datos.json")
            }
            val record = AforoRecord.fromJson(JSONObject(datosImportados.readText()))
            if (!esNombreSeguro(record.id)) {
                throw IllegalArgumentException("ID de registro inválido: ${record.id}")
            }

            val dir = recordDir(record.id)
            record.fotos.filter { esNombreSeguro(it) }.forEach { nombre ->
                val origen = File(tempDir, nombre)
                if (origen.exists()) {
                    origen.copyTo(File(dir, nombre), overwrite = true)
                }
            }

            saveDraft(record)
            return record
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
