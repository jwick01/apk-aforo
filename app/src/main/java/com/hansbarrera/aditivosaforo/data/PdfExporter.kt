package com.hansbarrera.aditivosaforo.data

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import com.hansbarrera.aditivosaforo.R
import java.io.File

/**
 * Genera el informe PDF de un registro de aforo con la API nativa
 * [PdfDocument] (sin dependencias externas): encabezado con técnico/empresa,
 * veredicto de verificación destacado, datos del trabajo, resultados por
 * sección y fotografías con sus notas.
 *
 * Debe invocarse fuera del hilo principal (Dispatchers.IO).
 */
class PdfExporter(private val context: Context) {

    private companion object {
        const val PAGE_W = 595   // A4 en puntos
        const val PAGE_H = 842
        const val MARGIN = 40f
        const val CONTENT_W = 515  // PAGE_W - 2 * MARGIN

        val COLOR_OK = 0xFF2E7D32.toInt()
        val COLOR_WARN = 0xFFF9A825.toInt()
        val COLOR_BAD = 0xFFC62828.toInt()
        val COLOR_NEUTRO = 0xFF757575.toInt()
        val COLOR_TEXTO = 0xFF212121.toInt()
        val COLOR_TEXTO_SUAVE = 0xFF555555.toInt()
        val COLOR_LINEA = 0xFFBDBDBD.toInt()
    }

    private val titlePaint = TextPaint().apply {
        textSize = 18f; typeface = Typeface.DEFAULT_BOLD; color = COLOR_TEXTO; isAntiAlias = true
    }
    private val sectionPaint = TextPaint().apply {
        textSize = 12f; typeface = Typeface.DEFAULT_BOLD; color = COLOR_TEXTO; isAntiAlias = true
    }
    private val bodyPaint = TextPaint().apply {
        textSize = 10f; color = COLOR_TEXTO; isAntiAlias = true
    }
    private val footerPaint = Paint().apply {
        textSize = 8f; color = COLOR_TEXTO_SUAVE; isAntiAlias = true
    }
    private val linePaint = Paint().apply {
        strokeWidth = 1f; color = COLOR_LINEA
    }
    private val fotoPaint = Paint().apply { isFilterBitmap = true }

    fun exportPdf(
        record: AforoRecord,
        tecnico: String,
        empresa: String,
        repository: AforoRepository
    ): File {
        val doc = PdfDocument()
        try {
            val writer = PageWriter(doc)

            // Encabezado
            writer.drawText(context.getString(R.string.label_pdf_titulo), titlePaint)
            writer.espacio(2f)
            if (tecnico.isNotBlank()) {
                writer.drawText(context.getString(R.string.label_pdf_tecnico, tecnico), bodyPaint)
            }
            if (empresa.isNotBlank()) {
                writer.drawText(context.getString(R.string.label_pdf_empresa, empresa), bodyPaint)
            }
            writer.drawText(context.getString(R.string.label_id_registro, record.id), bodyPaint)
            writer.drawText(context.getString(R.string.historial_fecha, record.fecha), bodyPaint)
            writer.divider()
            writer.espacio(6f)

            // Veredicto destacado
            drawVeredicto(writer, record)
            writer.espacio(10f)

            // Datos del trabajo
            writer.drawText(context.getString(R.string.section_datos_trabajo), sectionPaint)
            writer.drawText(context.getString(R.string.historial_cliente, record.cliente), bodyPaint)
            writer.drawText(context.getString(R.string.historial_proyecto, record.proyectoOMina), bodyPaint)
            writer.drawText(context.getString(R.string.historial_lugar, record.lugarAforo), bodyPaint)
            writer.drawText(context.getString(R.string.historial_operador, record.operador), bodyPaint)
            writer.drawText(context.getString(R.string.historial_equipo, record.numeroEquipo), bodyPaint)
            writer.drawText(context.getString(R.string.historial_odometro, record.odometro), bodyPaint)
            if (record.observaciones.isNotBlank()) {
                writer.drawText(context.getString(R.string.historial_observaciones, record.observaciones), bodyPaint)
            }

            // Resultados por sección
            agruparResultados(record.resultados).forEach { (seccion, campos) ->
                writer.ensureSpace(40f)
                writer.espacio(8f)
                writer.drawText(seccion, sectionPaint)
                campos.forEach { (campo, valor) ->
                    writer.drawText("$campo: $valor", bodyPaint)
                }
            }

            // Fotografías con sus notas
            val fotosExistentes = record.fotos.filter { repository.photoFile(record.id, it).exists() }
            if (fotosExistentes.isNotEmpty()) {
                writer.ensureSpace(60f)
                writer.espacio(10f)
                writer.drawText(context.getString(R.string.label_pdf_fotos), sectionPaint)
                fotosExistentes.forEach { nombre ->
                    drawFoto(writer, repository.photoFile(record.id, nombre), record.fotoNotas[nombre] ?: nombre)
                }
            }

            writer.finish()

            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val salida = File(exportDir, repository.nombreBaseExport(record) + ".pdf")
            salida.outputStream().use { doc.writeTo(it) }
            return salida
        } finally {
            doc.close()
        }
    }

    private fun drawVeredicto(writer: PageWriter, record: AforoRecord) {
        val veredicto = record.resultados["Verificación - Veredicto"]
        val desviacion = record.resultados["Verificación - Desviación (%)"]
        val color = when (veredicto) {
            "dentro de tolerancia" -> COLOR_OK
            "desviación moderada" -> COLOR_WARN
            "fuera de tolerancia" -> COLOR_BAD
            else -> COLOR_NEUTRO
        }
        val texto = veredicto?.takeIf { it.isNotBlank() && it != "sin datos" }
            ?: context.getString(R.string.label_pdf_sin_verificacion)

        val alto = 66f
        writer.ensureSpace(alto + 8f)
        val rect = RectF(MARGIN, writer.y, PAGE_W - MARGIN, writer.y + alto)
        val fondo = Paint().apply {
            this.color = (color and 0x00FFFFFF) or 0x1A000000
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val borde = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        writer.canvas.drawRoundRect(rect, 8f, 8f, fondo)
        writer.canvas.drawRoundRect(rect, 8f, 8f, borde)

        val etiquetaPaint = TextPaint().apply {
            textSize = 9f; this.color = COLOR_TEXTO_SUAVE; isAntiAlias = true; textAlign = Paint.Align.CENTER
        }
        val veredictoPaint = TextPaint().apply {
            textSize = 14f; typeface = Typeface.DEFAULT_BOLD; this.color = color
            isAntiAlias = true; textAlign = Paint.Align.CENTER
        }
        val centro = PAGE_W / 2f
        writer.canvas.drawText(context.getString(R.string.label_pdf_veredicto), centro, writer.y + 18f, etiquetaPaint)
        writer.canvas.drawText(texto.uppercase(), centro, writer.y + 40f, veredictoPaint)
        if (!desviacion.isNullOrBlank() && desviacion != "—") {
            val desviacionPaint = TextPaint().apply {
                textSize = 11f; this.color = color; isAntiAlias = true; textAlign = Paint.Align.CENTER
            }
            writer.canvas.drawText(
                context.getString(R.string.label_pdf_desviacion, desviacion),
                centro,
                writer.y + 58f,
                desviacionPaint
            )
        }
        writer.y += alto
    }

    private fun drawFoto(writer: PageWriter, archivo: File, nota: String) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(archivo.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return

        var sample = 1
        while (bounds.outWidth / sample > CONTENT_W * 2) {
            sample *= 2
        }
        val bitmap = BitmapFactory.decodeFile(archivo.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return

        try {
            // Hasta dos fotos por página: cada una ocupa como máximo media página.
            val maxAlto = (PAGE_H - 2 * MARGIN) / 2f - 30f
            val escala = minOf(CONTENT_W.toFloat() / bitmap.width, maxAlto / bitmap.height)
            val ancho = bitmap.width * escala
            val alto = bitmap.height * escala
            writer.ensureSpace(alto + 30f)
            writer.espacio(8f)
            val destino = RectF(MARGIN, writer.y, MARGIN + ancho, writer.y + alto)
            writer.canvas.drawBitmap(bitmap, null, destino, fotoPaint)
            writer.y += alto + 4f
        } finally {
            bitmap.recycle()
        }
        writer.drawText(nota, bodyPaint)
    }

    /**
     * Escritor con cursor vertical: crea páginas A4 a medida que el contenido
     * las va llenando y numera cada una al pie.
     */
    private inner class PageWriter(private val doc: PdfDocument) {
        private var pageNum = 0
        private var page: PdfDocument.Page? = null
        var y = 0f
        val canvas: Canvas get() = page!!.canvas

        init {
            nuevaPagina()
        }

        fun nuevaPagina() {
            cerrarPaginaActual()
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
            y = MARGIN
        }

        private fun cerrarPaginaActual() {
            page?.let {
                it.canvas.drawText(context.getString(R.string.label_pdf_pagina, pageNum), MARGIN, PAGE_H - 16f, footerPaint)
                doc.finishPage(it)
                page = null
            }
        }

        fun ensureSpace(alto: Float) {
            if (y + alto > PAGE_H - MARGIN) nuevaPagina()
        }

        fun espacio(alto: Float) {
            y += alto
        }

        fun drawText(texto: String, paint: TextPaint) {
            val layout = StaticLayout.Builder.obtain(texto, 0, texto.length, paint, CONTENT_W).build()
            ensureSpace(layout.height + 3f)
            canvas.save()
            canvas.translate(MARGIN, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + 3f
        }

        fun divider() {
            ensureSpace(12f)
            canvas.drawLine(MARGIN, y + 5f, PAGE_W - MARGIN, y + 5f, linePaint)
            y += 12f
        }

        fun finish() {
            cerrarPaginaActual()
        }
    }
}
