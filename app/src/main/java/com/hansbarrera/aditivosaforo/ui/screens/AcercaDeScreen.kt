package com.hansbarrera.aditivosaforo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hansbarrera.aditivosaforo.BuildConfig
import com.hansbarrera.aditivosaforo.ui.components.SectionCard

@Composable
fun AcercaDeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Acerca de", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(top = 12.dp))

        SectionCard("Aditivos y Émboladas") {
            Text("Versión ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(
                "Calculadora de campo para operaciones de shotcrete: rendimiento de la bomba, " +
                    "dosis de aditivo/acelerante, verificación contra el display, posición del " +
                    "potenciómetro del dosificador y registro de aforos con fotografías.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.padding(top = 12.dp))
            Text("Hecho por Hans Barrera", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        SectionCard("Privacidad y seguridad") {
            Text(
                "• La app funciona 100% sin conexión: no requiere ni solicita permiso de Internet.\n" +
                    "• No incluye anuncios, rastreadores ni librerías de terceros fuera de las oficiales de Android.\n" +
                    "• No envía, recibe ni almacena datos fuera de este dispositivo.\n" +
                    "• El permiso de cámara se usa solo para tomar fotos del registro de aforo; " +
                    "las fotos se guardan localmente y solo se comparten si tú lo haces explícitamente " +
                    "con la opción \"Exportar y compartir\".\n" +
                    "• El código fuente completo está disponible para revisión.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
