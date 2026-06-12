# Aditivos y Émboladas

App Android (Kotlin + Jetpack Compose) para cálculos de campo de operaciones
de shotcrete, hecha por **Hans Barrera**. Funciona **100% sin conexión**
(no pide permiso de Internet).

## Calculadoras incluidas

1. **Rendimiento de la bomba**
   - Por émboladas: a partir del volumen del cilindro (con presets Alpha 15/20/30/40,
     TK-40 y Putzmeister 1007), número de émboladas por minuto y factor de llenado.
   - Por tiempo de llenado: a partir del tiempo y volumen de llenado.
2. **Dosis de aditivo / acelerante**: kg/min y lts/min requeridos según el
   rendimiento de la bomba, la dosis de cemento, el % de aditivo y su densidad.
3. **Verificación contra el display**: compara los valores calculados con las
   lecturas reales del equipo y muestra las desviaciones (%).
4. **Posición del potenciómetro**: calcula el acelerante requerido según diseño
   e interpola la posición del potenciómetro a partir de una tabla de
   calibración editable, con gráfico de la curva.

Las fórmulas replican exactamente las de la hoja de cálculo original
(`aditivos y envoladas`, `display`, `tablas para generar informe`).

## Registro de aforo

La pestaña **Aforo** permite guardar, para cada trabajo, los datos generales
(fecha, cliente, proyecto o mina, lugar del aforo, operador, número de equipo,
odómetro y observaciones), un resumen de los resultados calculados en las
otras pestañas (botones "Usar este resultado...") y fotografías tomadas con
la cámara o cargadas desde la galería.

Cada registro se guarda localmente en el dispositivo con un ID correlativo
(`AFORO-aaaa-mm-dd-NNN`) y sus fotos numeradas de forma correlativa
(`foto01.jpg`, `foto02.jpg`, ...). Desde el **Historial** se puede revisar,
eliminar o **exportar** un registro: la exportación genera un `.zip` con
`datos.json` y las fotos, y abre el selector de "Compartir" de Android para
enviarlo, por ejemplo, a la app de Claude para generar el informe.

## Cómo obtener el APK

Este proyecto se compila automáticamente con **GitHub Actions** en cada push
(no requiere tener Android Studio instalado):

1. Ve a la pestaña **Actions** del repositorio.
2. Abre la ejecución más reciente del workflow **Build APK**.
3. Descarga el artefacto `AditivosAforo-debug-apk` (es un .zip que contiene
   `app-debug.apk`).
4. Copia el `.apk` a tu teléfono e instálalo (puede que debas habilitar
   "Instalar apps de orígenes desconocidos" para el navegador o gestor de
   archivos que uses).

### Compilar localmente (opcional)

Con Android Studio (versión 2024.x o superior) o con la línea de comandos,
siempre que tengas el SDK de Android instalado:

```bash
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Seguridad

- **Sin permiso de Internet**: la app no puede enviar ni recibir datos por red.
- **Permiso de cámara**: solo se usa para tomar fotos del registro de aforo;
  las fotos se guardan en el almacenamiento privado de la app y solo salen
  del dispositivo si el usuario usa explícitamente "Exportar y compartir".
- **Sin librerías de terceros**: solo dependencias oficiales de AndroidX/Jetpack
  Compose y Material 3.
- **`allowBackup="false"`**: evita que los datos de la app salgan del
  dispositivo mediante copias de seguridad ADB.
- **Código abierto y auditable**: todo el cálculo está en
  `app/src/main/java/com/hansbarrera/aditivosaforo/calc/Formulas.kt`.
- El APK generado por el workflow está firmado con la clave de depuración
  (debug), suficiente para instalación personal. Si quieres distribuirlo o
  subirlo a una tienda, genera tu propia clave de firma de release.
