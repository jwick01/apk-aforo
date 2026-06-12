package com.hansbarrera.aditivosaforo.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
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
        return AforoRecord.fromJson(JSONObject(file.readText()))
    }

    fun loadAll(): List<AforoRecord> = listIds().mapNotNull { load(it) }

    fun save(record: AforoRecord) {
        datosFile(record.id).writeText(record.toJson().toString(2))
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
        val zipFile = File(exportDir, "$id.zip")

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
}
