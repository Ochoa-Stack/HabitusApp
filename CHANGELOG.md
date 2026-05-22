# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

Formato basado en [Keep a Changelog](https://keepachangelog.com).
Versionado basado en [Semantic Versioning](https://semver.org).

---

## [Sin publicar]

### En progreso
- Documentación técnica

---

## [1.0.0] — 2026-05-12

> Todo lo que sigue fue diseñado e implementado individualmente por Elias Ochoa después del fork.
> Repositorio académico original: github.com/DjRober/HabitTrackerApp

### Seguridad
- Reglas de Firestore actualizadas: subcolección `reflexiones` protegida con validación de `uid`, a la par que `completaciones`
- `completarHabito` migrado a `runTransaction` de Firestore para garantizar atomicidad lectura-cálculo-escritura en entornos multi-dispositivo
- R8/ProGuard activado en release con reglas explícitas para Firebase, Navigation Component, WorkManager y modelos de datos
- `allowBackup=false` en AndroidManifest: datos gestionados exclusivamente en Firestore, sin extracción local
- `backup_rules.xml` y `data_extraction_rules.xml` con exclusión completa de todos los dominios de datos
- Guard de sesión en Workers: verificación de `FirebaseAuth.currentUser` antes de ejecutar operaciones

### Arquitectura y estabilidad
- `viewLifecycleOwner.lifecycleScope` en todos los Fragments con patrón `_binding` nullable para eliminar NPE en cambios de ciclo de vida
- Lecturas de subcolecciones de Firestore migradas a `coroutineScope + async/awaitAll` para paralelizar consultas y eliminar lecturas secuenciales
- `HabitAdapter` migrado a `ListAdapter` con `DiffUtil.ItemCallback` para actualizaciones eficientes
- `onFailure` vacíos reemplazados con feedback explícito al usuario en todas las pantallas principales
- Touch targets de elementos interactivos llevados a mínimo 48dp en todos los layouts

### Calidad de código
- Migración completa de package name: `com.example.habittrackerapp` → `com.ochoastack.habitus` en `applicationId`, `namespace`, 39 archivos Kotlin y estructura física de directorios
- 24 strings hardcodeadas migradas a `strings.xml`
- Colores de tipos cognitivos declarados en `values/colors.xml` y `values-night/colors.xml` para soporte tema-aware en modo oscuro
- Eliminación de directorios huérfanos sin uso (`logic/`, `di/`, `presentation/`)
- `.gitignore` raíz con cobertura completa para Android: builds, IDE, secretos, sistema operativo
- `settings.gradle.kts`: `rootProject.name = "HabitusApp"`

### Features
- Recordatorio diario personalizable: selector de hora con `TimePickerDialog`, persistencia en `SharedPreferences`, programación con `WorkManager` (`DailyReminderWorker`) con política `REPLACE`
- Eliminación permanente de hábito desde `HabitDetailActivity` con diálogo de confirmación
- Estados de reflexión: bloqueado hasta completar el hábito del día, activo, enviado (no editable hasta el día siguiente)
- Indicador circular de completado en `HabitAdapter` sincronizado con Firestore en carga paralela
- `ProgressBar` en `WeeklySummaryActivity` durante carga de datos
- Política de privacidad y pantalla Acerca de actualizadas con datos reales de la app

---

## [0.9.0] — 2026

### Agregado
- Tipos cognitivos por hábito: Físico, Mental, Social, Creativo, Descanso con colores diferenciados
- Balance cognitivo visual en Home: barras de progreso por tipo generadas dinámicamente
- Diario de reflexión por hábito: texto libre de 280 caracteres guardado en subcolección `reflexiones` de Firestore, historial de las últimas 5 entradas
- Modo enfoque: pantalla inmersiva con solo los hábitos del día actual, completar sin navegación adicional, indicador de tipo cognitivo por ítem
- `HabitFormManager` extendido con selector de tipo cognitivo mediante `ChipGroup`

### Corregido
- Crash al cambiar tabs rápidamente: `lifecycleScope` reemplazado por `viewLifecycleOwner.lifecycleScope` en todos los Fragments con guards de `_binding`
- Flickering de estadísticas en Home y Perfil: valores inicializados con `—` y carga explícita solo en primera creación de la vista

---

## [0.8.0] — 2026

### Agregado
- Días de gracia configurables por hábito (0, 1 o 2): el algoritmo de racha tolera días programados fallados sin romper la racha, sin afectar el porcentaje real
- Resumen semanal automático con `WorkManager`: `WeeklySummaryWorker` ejecuta cada domingo a las 8pm, calcula porcentaje, mejor racha y hábito más descuidado
- `WeeklySummaryActivity`: pantalla accesible desde Home con porcentaje, detalle de completaciones, mejor racha y mensaje contextual según rendimiento
- Home inteligente: mensaje motivacional dinámico basado en racha activa y porcentaje semanal real
- `obtenerPorcentajeSemana()` y `obtenerResumenSemanal()` como métodos separados en `HabitRepository`

---

## [0.7.0] — 2026

### Agregado
- Calendario mensual navegable en detalle de hábito: cuatro estados por día (completado, hoy, fallado, no aplicable) consultados desde Firestore
- `MonthCalendarAdapter` con `GridLayoutManager` de 7 columnas y celdas vacías para alineación de semana
- Archivar hábitos con swipe en la lista principal: `Snackbar` con opción de deshacer
- `ArchivedHabitsActivity`: lista de hábitos archivados con restaurar y eliminar permanente
- `obtenerHabitosArchivados()`, `archivarHabito()` y `restaurarHabito()` en `HabitRepository`

---

## [0.6.0] — 2026

### Agregado
- Google Sign-In con `CredentialManager` API: `FirebaseAuthRepository.iniciarSesionConGoogle()` con manejo de `GetCredentialException`
- Ícono de lanzador personalizado: hoja en blanco sobre fondo terracota en `ic_launcher_foreground.xml` e `ic_launcher_background.xml`
- Modo oscuro completo: `values-night/colors.xml` con 15 colores y `values-night/themes.xml` con 11 atributos Material3 mapeados a la paleta terracota
- `HabitFormManager.kt`: lógica compartida entre `CreateHabitActivity` y `EditHabitActivity` que elimina duplicación de chips, frecuencia y validación
- `NotificationsActivity` con `WorkManager` real: `PeriodicWorkRequest`, `NotificationHelper.kt` y `ReminderWorker.kt`
- `EstadisticasUsuario.kt` reemplaza el `Triple` anónimo en el repositorio

### Corregido
- Historial de 7 días reemplazado por consulta real a Firestore (reemplazó simulación con datos fijos)
- Porcentaje de cumplimiento corregido: denominador es días programados según `diasSemana`, no días transcurridos
- Racha con reset automático al cargar la lista
- `ProfileFragment` migrado para usar `FirebaseAuthRepository.obtenerNombreUsuario()` en lugar de acceso directo a Firestore
- Guard anti-doble-completación: subcolección `completaciones/{yyyy-MM-dd}` verificada antes de registrar una nueva completación
- Fix `BottomNavigationView`: pill de selección corregido con atributos nocturno en `themes.xml`

---

## [0.1.0] — 2025

> Entrega del equipo académico original.
> Autores: Arleth Caballero, Edgar Torres, Roberto Perez, Elias Ochoa.
> Base del proyecto antes del fork independiente.

### Estado inicial
- Autenticación con email y contraseña
- Onboarding en primer uso
- CRUD básico de hábitos con frecuencia y días de semana
- Categorías personalizadas con color
- Estadísticas básicas en Home y Perfil
- Notificaciones con `WorkManager` (configuración inicial)
- Modo oscuro parcial
- Navigation Component + `BottomNavigationView`
- Firebase Authentication + Cloud Firestore integrados
