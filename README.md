# Habitus

Aplicación Android para el seguimiento de hábitos — completación diaria, gestión de rachas, balance cognitivo, resúmenes semanales y diario de reflexión construida con Kotlin y Firebase.

[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen)](https://android-arsenal.com/api?level=24)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **Estado:** Proyecto de portafolio; no publicado en Google Play Store.
> Fork de un proyecto grupal universitario. Diseñe y construi un ~80% del proyecto (incluyendo orginal y post-fork), cubriendo tanto las capas de frontend como de backend.
> Repositorio: [github.com/Ochoa-Stack/HabitusApp](https://github.com/Ochoa-Stack/HabitusApp)

---

## Resumen (Overview)

Habitus es una aplicación nativa de Android para el seguimiento de hábitos. Los usuarios se autentican con correo/contraseña o Google Sign In, crean hábitos con frecuencia personalizada, tipo cognitivo y configuración de días de gracia, los completan diariamente mediante una transacción atómica de Firestore y siguen su progreso a través de un calendario mensual construido con datos reales de Firestore. La aplicación incluye un diario de reflexión por hábito, un modo de enfoque para la ejecución diaria sin distracciones y un resumen semanal automático entregado a través de WorkManager.

La mayoría de las aplicaciones de seguimiento de hábitos informan un progreso binario sin contexto. Habitus distingue los tipos de actividad en cinco categorías cognitivas, protege las rachas con días de gracia configurables sin inflar el porcentaje de cumplimiento real y requiere una reflexión escrita antes de que cierre el día. El calendario mensual renderiza cuatro estados de día distintos a partir de datos de Firestore en vivo, no mediante simulación local.

El código base refleja una serie de decisiones de ingeniería deliberadas: ViewBinding con un patrón `_binding` nullable en todos los Fragments previene NullPointerExceptions durante las transiciones del ciclo de vida; `viewLifecycleOwner.lifecycleScope` limita las corrutinas al ciclo de vida de la vista, no del Fragment; `runTransaction` en `completarHabito` garantiza una operación atómica de lectura-cálculo-escritura en entornos multidispositivo; las lecturas paralelas mediante `coroutineScope + async/awaitAll` reemplazan las llamadas secuenciales de Firestore en las consultas de resumen; WorkManager utiliza `ExistingPeriodicWorkPolicy.KEEP` para evitar trabajadores duplicados al reiniciar; R8 y ProGuard están activos en las versiones de lanzamiento.

---

## Stack Tecnológico

- **Kotlin** - lenguaje único de la aplicación; corrutinas con `suspend/await` para todas las operaciones asíncronas.
- **XML Views + ViewBinding** - sistema de vistas completo sin Jetpack Compose; patrón de binding nullable aplicado en todos los Fragments.
- **Firebase Authentication** - correo/contraseña y Google Sig In mediante la API de CredentialManager; SHA-1 vinculado al proyecto.
- **Cloud Firestore** - base de datos de documentos NoSQL; las reglas de seguridad aplican `request.auth.uid == resource.data.uid` en todas las colecciones y subcolecciones.
- **WorkManager** - `CoroutineWorker` para el resumen semanal (horario fijo) y el recordatorio diario (configurable por el usuario); las políticas de unicidad evitan trabajadores duplicados.
- **Navigation Component** - gráfico de navegación de una sola actividad con `BottomNavigationView`; back stack gestionado de forma declarativa.
- **Material Design 3** - `CardView`, `ChipGroup`, `SwitchMaterial`, `MaterialAlertDialog`, `Snackbar`.
- **R8 / ProGuard** - activo en release; las reglas preservan los campos del modelo de Firestore, los objetivos de navegación y los Workers.

> Sin Jetpack Compose. Sin ViewModels. Repositorios instanciados directamente en la UI; decisión arquitectónica tomada durante la fase académica original, mantenida post-fork para dar continuidad.

---

## Arquitectura

### Patrón

MVVM con Repositorios. Cada operación de datos fluye a través de un repositorio que devuelve un `Result<T>`. La UI consume los resultados con `.fold(onSuccess, onFailure)`. Ninguna operación toca Firebase directamente desde un Fragment o Activity.

```
UI (Activity / Fragment)
        │
        │  llama a función suspend
        ▼
  Repositorio (HabitRepository, CategoryRepository, FirebaseAuthRepository)
        │
        │  Kotlin Coroutines; suspend + await
        ▼
  Firebase SDK (Firestore / Auth)
```

### Estructura del Proyecto

```
com.ochoastack.habitus/
├── data/               # Habit, Reflexion, ResumenSemanal,
│                         TipoCognitivo, EstadisticasUsuario,
│                         HabitRepository, CategoryRepository,
│                         FirebaseAuthRepository
├── ui/                 # Activities, Fragments, Adapters,
│                         HabitFormManager, MonthCalendarAdapter
├── utils/              # NotificationHelper
├── worker/             # ReminderWorker, WeeklySummaryWorker,
│                         DailyReminderWorker
└── HabitusApp.kt       # Clase Application: inicialización del
                          canal de notificaciones y WorkManager
```

### Estructura de Firestore

```
usuarios/{uid}
  → name, email, createdAt

categorias/{id}
  → uid, name, color, isDefault, createdAt

habitos/{id}
  → uid, name, frequency, weekDays[], categoryId,
    cognitiveType, graceDays, streak, percentage,
    totalCompletions, archived, lastCompletion, createdAt

habitos/{id}/completaciones/{yyyy-MM-dd}
  → date, timestamp

habitos/{id}/reflexiones/{yyyy-MM-dd}
  → date, text, timestamp
```

### Seguridad

- Reglas de Firestore: `request.auth.uid == resource.data.uid` aplicado en todas las colecciones y subcolecciones, incluyendo `completaciones` y `reflexiones`.
- `completarHabito`: `runTransaction` garantiza lectura-cálculo-escritura atómica; el lambda es totalmente síncrono; sin llamadas a `await` dentro del bloque de transacción.
- Guardia contra doble completación: se verifica la existencia del documento de la subcolección antes de abrir la transacción.
- `allowBackup=false`: todos los datos del usuario viven en Firestore; no hay extracción local mediante el backup de Android.
- Google Sign-In: API de CredentialManager con `SHA-1` registrado en Firebase Console.
- R8 activo en release con reglas de ProGuard que preservan los nombres de los campos del modelo de Firestore.
- Workers: se verifica `FirebaseAuth.currentUser` antes de que se ejecute cualquier operación de red.

---

## Funcionalidades (Features)

- **Autenticación** - registro e inicio de sesión con correo/contraseña; Google Sign-In mediante la API de CredentialManager; onboarding en el primer uso.
- **Gestión de hábitos** - crear, editar y eliminar hábitos; frecuencia diaria o personalizada por día de la semana; categorías con colores personalizados; tipo cognitivo (Físico, Mental, Social, Creativo, Descanso); días de gracia (0, 1 o 2); archivar y restaurar sin perder el historial; eliminación permanente desde la pantalla de detalle.
- **Seguimiento diario** - completación en un solo sentido con transacción atómica de Firestore; guardia contra doble completación; indicador circular de completado en la lista sincronizado desde Firestore al cargar.
- **Progreso** - calendario mensual con cuatro estados de día (completado, hoy, fallado, no aplicable) construido con datos reales de Firestore; porcentaje de cumplimiento de 30 días con los días programados como denominador; racha con lógica de días de gracia y reinicio automático al cargar.
- **Diario de reflexión** - se desbloquea tras la completación diaria; límite de 280 caracteres; deshabilitado tras el envío hasta el día siguiente; historial de las últimas 5 entradas por hábito.
- **Modo enfoque** - pantalla inmersiva que muestra solo los hábitos de hoy; completar sin navegación adicional; indicador de tipo cognitivo por ítem.
- **Estadísticas** - estadísticas de inicio en tiempo real (hábitos activos, completados hoy, racha máxima); barras de balance cognitivo por tipo; mensaje motivacional basado en la racha y el porcentaje semanal.
- **Resumen semanal** - entrega automática por WorkManager cada domingo a las 8pm; accesible manualmente desde el Inicio; porcentaje, detalle de completaciones, mejor racha y mensaje contextual.
- **Recordatorio diario** - hora configurable por el usuario mediante `TimePickerDialog`; persistido en SharedPreferences; programado con WorkManager; se cancela al desactivarlo.
- **Modo oscuro** - cobertura completa de `values-night/`; cero lógica de Kotlin; 15 colores y 11 atributos de tema Material3 mapeados a la paleta terracota.

---

## Desarrollo Local

**Requisitos previos**
- Android Studio Ladybug o posterior
- JDK 17+
- Android SDK API 24-36
- Un proyecto de Firebase con Authentication y Firestore habilitados

**Instalación**

1. Clonar el repositorio:
```bash
git clone https://github.com/Ochoa-Stack/HabitusApp.git
```

2. Configuración de Firebase:
   - Habilita los proveedores de Authentication: Correo/Contraseña y Google.
   - Crea una base de datos Firestore en modo producción.
   - Registra una app de Android con el paquete `com.ochoastack.habitus`.
   - Registra el `SHA-1` de tu máquina en Firebase Console.
   - Descarga `google-services.json` y colócalo en `app/`.

3. Desplegar reglas de seguridad de Firestore:
```bash
firebase deploy --only firestore:rules
```

4. Abrir el proyecto en Android Studio.
   La sincronización de Gradle se ejecuta automáticamente.

5. Ejecutar en un emulador (API 24+) o dispositivo físico.

**Nota sobre Google Sign-In:**
La huella `SHA-1` de tu almacén de claves de depuración (debug keystore) debe estar registrada en Firebase Console. Obténla con:
```bash
./gradlew signingReport
```

**Nota sobre índices de Firestore:**
La consulta `whereEqualTo("uid").whereEqualTo("archivado", false)` requiere un índice compuesto en la colección `habitos`. Créalo en Firebase Console → Firestore → Índices → Compuesto: campos `uid` (Ascendente) + `archivado` (Ascendente).

---

## Limitaciones Actuales

- No publicado en Google Play Store.
- Sin ViewModels: repositorios instanciados directamente en la UI (heredado de la arquitectura académica original).
- Sin suite de pruebas automatizadas (unitarias o instrumentadas).
- Patrón N+1 de Firestore mitigado con lecturas asíncronas paralelas, no eliminado — Firestore no admite joins.
- Sin soporte offline: todas las operaciones de datos requieren una conexión activa.

---

## Origen

Desarrollado originalmente como un proyecto integrador universitario de Ingeniería de Software. El desarrollo post-fork, auditoría de arquitectura, refuerzo de seguridad y todas las funcionalidades principales; fue realizado por [Elias Ochoa](https://github.com/Ochoa-Stack).

Repositorio original del equipo: [github.com/DjRober/HabitTrackerApp](https://github.com/DjRober/HabitTrackerApp)

---

## Licencia

Distribuido bajo la Licencia MIT. Consulta [LICENSE](LICENSE) para más detalles.
