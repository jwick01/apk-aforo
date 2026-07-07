# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

**Aditivos y Émboladas** (package `com.hansbarrera.aditivosaforo`) is a native Android app
(Kotlin + Jetpack Compose, Material 3) for field calculations on shotcrete pumping
operations. It works fully offline — the app deliberately requests no Internet permission.
The UI strings, code comments, and domain vocabulary are in **Spanish**; keep new code and
comments consistent with that (variable/function names, `strings.xml` keys, commit-worthy
comments should stay in Spanish to match the existing codebase).

The app has two halves:
1. **Calculators** (pump output, additive/accelerant dosing, display verification,
   potentiometer position) that replicate formulas from an original Excel spreadsheet
   (sheets `aditivos y envoladas`, `display`, `tablas para generar informe`).
2. **Aforo (gauging) record-keeping**: a form that accumulates inputs/results from the
   calculators plus photos into a record, saved locally and exportable as a `.zip`.

## Build, lint, run

There is no test suite in this repo (no `src/test` or `src/androidTest` directories) and no
linter is configured beyond the Android/Kotlin compiler and R8/ProGuard on release builds.

```bash
./gradlew assembleDebug      # debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # release APK (minified via proguard-rules.pro)
./gradlew build              # full build (compile + assemble both variants)
```

Requires JDK 17 and the Android SDK (compileSdk 34, minSdk 26). CI (`.github/workflows/build-apk.yml`)
runs `./gradlew assembleDebug --no-daemon` on every push to any branch and uploads the debug
APK as a build artifact named `AditivosAforo-debug-apk` — this is the primary way users obtain
a build (no Play Store distribution, no local Android Studio required).

## Architecture

**Single-Activity, no DI framework, no ViewModel/Room/database.** `MainActivity` hosts one
Compose `NavHost` with a bottom `NavigationBar` (Inicio / Aforo / Historial); calculators,
the guided flow ("Guiado"), and "Acerca de" are pushed as additional nav-graph destinations
reached from `HomeScreen`, not from the bottom bar.

State is plain `mutableStateOf`, not a state-management library. The important pieces:

- **`AppState`** (`AppState.kt`) — one instance created in `MainActivity` and passed down
  through every screen's constructor. It is the single shared, in-memory state container for
  the whole app: last-computed values shared between calculators (`rendimientoM3Hr`,
  `caudalAditivoLtsMin`, `cementoKgM3`, ...), the accumulated report data map (`datosInforme`,
  keyed `"Sección - Campo"`), and appearance prefs (`temaOscuro`, `idioma`) persisted directly
  to `SharedPreferences` ("ajustes") inside `AppState` itself — there's no separate settings
  repository.
- **`formResetTrigger`** / **`editarRegistroTrigger`** — counters bumped to force Compose
  screens to re-seed their `rememberSaveable` fields (keyed on these counters) either back to
  defaults (new gauging) or from a loaded historical record (edit mode), since there is no
  ViewModel to just recreate.
- **`calc/Formulas.kt`** — a stateless `object` holding *all* domain math, each function
  annotated in comments with the exact originating Excel cell formula (e.g. `C7=(C4*C5)*C6`).
  This is the auditable source of truth for calculations; when changing a formula, verify it
  against the referenced spreadsheet logic in the comment, not just the Kotlin. Screens never
  compute formulas themselves — they only call into `Formulas`.
- **`data/` package** — persistence and domain data, no framework:
  - `AforoRecord` — the gauging record model, with manual `toJson()`/`fromJson()`. Result keys
    use the `"Sección - Campo"` convention (e.g. `"Aditivo - Litros de aditivo (lts/min)"`);
    `agruparResultados()` (`ResultadosUtil.kt`) splits on `" - "` to regroup them by section for
    display and for the PDF report. When adding a new calculator result field, follow this
    naming convention — other code (PDF export, history display, `AppState.cargarRegistroParaEditar`)
    depends on matching these exact string keys.
  - `AforoRepository` — flat-file storage under `filesDir/aforos/<id>/datos.json` +
    `foto01.jpg, foto02.jpg, ...`, plus a single `borrador_aforo.json` draft file for
    in-progress forms (auto-saved so an in-progress record survives the app being killed).
    Writes go through `writeTextAtomic()` (write to `.tmp`, then rename) to avoid truncated
    JSON from a mid-write kill. `exportZip()`/`importZip()` handle the share/round-trip flow;
    `importZip` sanitizes zip entry names (`esNombreSeguro`) against zip-slip path traversal.
  - `Presets.kt` — hardcoded pump/preset constants taken from the spreadsheet (pump cylinder
    volumes, default fill factor, default additive density, default potentiometer table).
  - `PdfExporter.kt` — builds the shareable PDF report from a record's `resultados` map.
- **`ui/screens/`** — one file per screen/calculator (`RendimientoScreen`, `AditivoScreen`,
  `VerificacionScreen`, `PotenciometroScreen`, `AforoScreen`/`RegistroForm`, `HistorialScreen`,
  `GuiadoScreen`, `HomeScreen`, `AcercaDeScreen`). Each calculator screen follows the same
  shape: read prior values out of `appState.datosInforme` to prefill fields (so re-opening a
  calculator or editing a historical record restores inputs), compute via `Formulas`, render
  results, and offer a "Usar este resultado" / "Guardar estos datos" button that calls
  `appState.registrarDatos(...)` to push values into the shared report map.
  `GuiadoScreen` chains several calculators into one assisted workflow.
- **`ui/components/Common.kt`** — shared widgets reused across all calculator screens:
  `NumberField` (locale-tolerant decimal input, comma or dot, optional +/- step buttons for
  one-handed field use), `formatNumber`, `String.toDoubleOrZero()`, `SectionCard`, `ResultRow`,
  `ResultadosAgrupados`. Reuse these instead of building new input/result widgets.
- **Localization**: `res/values/strings.xml` (Spanish, default) and `res/values-en/strings.xml`
  (English) — `AppState.idioma` overrides the system locale via a wrapped `Context` in
  `MainActivity.AppRoot()`. Any new user-facing string must be added to both files.
- **Theming**: `ui/theme/` — dark/light/"follow system" is user-selectable and stored via
  `AppState.temaOscuro`; dark mode exists specifically for low-light work (e.g. inside tunnels).

## Conventions worth preserving

- No third-party libraries beyond official AndroidX/Jetpack Compose + Material 3 — this is a
  stated security/auditability property of the app, not an accident. Don't add networking,
  analytics, or crash-reporting SDKs.
- No Internet permission and `allowBackup="false"` in `AndroidManifest.xml` are intentional —
  the app must not be able to transmit data off-device or leak it via ADB backup.
- Camera permission is used only for the gauging-record photos; photos live in private app
  storage and only leave the device through the explicit "Exportar y compartir" zip flow via
  the `FileProvider` declared in the manifest (authorities `${applicationId}.fileprovider`,
  paths in `res/xml/file_paths.xml`).
- Result/report map keys follow `"Sección - Campo"` — grep for a key string before renaming
  one, since `AppState`, `AforoRecord`, `ResultadosUtil`, and `PdfExporter` all match on exact
  key text rather than a shared enum/constant.
