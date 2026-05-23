# Arquitectura del Sistema

Este documento describe a detalle los patrones arquitectónicos, las decisiones de diseño y las prácticas de ingeniería implementadas en Habitus para asegurar escalabilidad, mantenibilidad y un rendimiento óptimo.

## Patrón Arquitectónico: MVVM + Repository + Hilt DI

HabitusApp sigue una arquitectura basada en capas fuertemente inspirada en la [Guía de Arquitectura de Aplicaciones](https://developer.android.com/jetpack/guide) oficial de Android:

1. **Capa de Presentación**: Desacopla la lógica de visualización del manejo del estado.
2. **Capa de Dominio / Datos**: Centraliza el acceso a datos locales y remotos como única fuente de verdad.
3. **Inyección de Dependencias**: Administrada enteramente por **Dagger Hilt** para proveer instancias automáticas en tiempo de compilación.

### Flujo de Datos Unidireccional

La UI nunca solicita datos directamente a las fuentes; en su lugar, reacciona a los cambios de estado:
- El `Repository` procesa las lecturas de Firestore y las devuelve envueltas en la clase `Result<T>`.
- El `ViewModel` captura estos resultados y actualiza un `StateFlow` (ej. `_uiState.value = UiState.Success(data)`).
- Los `Fragments` o `Activities` observan el `StateFlow` de manera segura a través de `repeatOnLifecycle(Lifecycle.State.STARTED)`. Esto asegura que las actualizaciones de UI se detengan automáticamente cuando la app está en segundo plano, previniendo crashes y ahorrando batería.

## Gestión de Concurrencia y Trabajo Asíncrono

Para no bloquear el hilo principal (Main Thread), la concurrencia es manejada a través de:

- **Kotlin Coroutines**: Todas las lecturas y escrituras de bases de datos son funciones `suspend` ejecutadas bajo dispatchers dedicados (ej. `Dispatchers.IO`).
- **`viewModelScope`**: El ciclo de vida de las corrutinas en los ViewModels está atado a su `viewModelScope`. Si un usuario sale de la pantalla antes de que finalice una red, el job se cancela automáticamente.
- **`WorkManager`**: Tareas que requieren ejecución garantizada sin depender del ciclo de vida de la UI (como el cómputo del resumen semanal y el envío de notificaciones locales) se aíslan en workers dedicados (ej. `WeeklySummaryWorker`), inyectados vía `@HiltWorker`.

## Seguridad y Privacidad

- **Aislamiento de Datos en Firestore**: Se aplican `firestore.rules` estrictas que previenen que cualquier usuario sin autenticar pueda acceder a la base de datos, y asegura que un usuario autenticado solo pueda leer o alterar documentos donde `uid == request.auth.uid`.
- **Gestión de Secretos**: No hay claves sensibles expuestas en el código fuente. El archivo `google-services.json` y el keystore criptográfico `.jks` se inyectan únicamente en el pipeline de CI/CD vía GitHub Secrets.
- **Ofuscación y Optimización**: Se emplea **R8/ProGuard** en las compilaciones de release (a través de `minifyEnabled = true`) para ofuscar el código contra ingeniería inversa y eliminar el código no utilizado.

## Registro de Decisiones Técnicas (ADR)

### ¿Por qué KSP en lugar de KAPT?
KAPT (Kotlin Annotation Processing Tool) dependía de compatibilidad con Java Stubs, haciéndolo ineficiente y propenso a fallos con las nuevas versiones de metadatos de Kotlin. Al transicionar a **KSP (Kotlin Symbol Processing)** nativo:
- Los tiempos de compilación mejoraron sustancialmente.
- Desaparecieron los errores crípticos de metadatos `2.2.0` frente a compiladores desactualizados de Hilt y AndroidX.

### ¿Por qué mantener AGP 8.7.3?
Aunque existen versiones preliminares de Android Gradle Plugin 9.x, AGP 8.7.3 se fijó como el pilar estable debido a su plena compatibilidad testeada con KSP, Kotlin 2.0.21, y la suite de compiladores de DataBinding/ViewBinding actuales sin forzar bypasses o flags incubadoras.

### ¿Por qué estandarizar `Locale.ROOT` en formateadores?
Históricamente, los ID de documentos en las colecciones de completación utilizaban fechas generadas por `SimpleDateFormat` con `Locale.getDefault()`. Esto significaba que, si un usuario cambiaba el idioma de su dispositivo de inglés a español, la estructura de la fecha generada mutaba (ej: abreviaturas diferentes de días), rompiendo la trazabilidad de las métricas en la base de datos NoSQL. Al fijar `Locale.ROOT`, se garantiza un identificador ISO universal `yyyy-MM-dd` determinista y resistente al idioma del sistema operativo cliente.
