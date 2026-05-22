# Contributing to Habitus

¡Gracias por querer contribuir a Habitus! Este documento explica cómo hacerlo de forma completa y ordenada.

---

## Código de conducta

Este proyecto sigue el [Contributor Covenant](https://www.contributor-covenant.org/version/2/1/code_of_conduct/).  
Se espera un trato respetuoso, inclusivo y constructivo en todas las interacciones.

---

## ¿Cómo puedo contribuir?

### Reportar un bug

Usa la plantilla **Bug Report** al crear un Issue. Incluye:
- Versión de Android y modelo del dispositivo
- Pasos exactos para reproducirlo
- Comportamiento esperado vs actual
- Capturas de pantalla si aplica

### Proponer una mejora

Usa la plantilla **Feature Request**. Explica:
- El problema que resuelve
- La solución propuesta
- Alternativas consideradas

### Contribuir código

1. Busca un issue abierto o abre uno nuevo
2. Comenta que lo vas a trabajar para evitar trabajo duplicado
3. Sigue el [flujo de trabajo Git](#flujo-de-trabajo-git)

---

## Flujo de trabajo Git

```
main (producción estable)
  └── develop (integración)
        ├── feature/nombre-descriptivo
        ├── fix/descripcion-del-bug
        ├── refactor/componente-afectado
        ├── chore/tarea-de-mantenimiento
        └── docs/sección-documentada
```

### Pasos

```bash
# Crear rama desde develop (nunca desde main)
git checkout develop
git pull origin develop
git checkout -b feature/mi-nueva-funcionalidad

# Trabajar, hacer commits atómicos
git add .
git commit -m "feat: agregar pantalla de estadísticas mensuales"

# Push y abrir PR hacia develop
git push origin feature/mi-nueva-funcionalidad
```

> Nota: **Nunca hagas push directo a `main` o `develop`.**

---

## Conventional Commits

Todos los commits deben seguir el estándar [Conventional Commits](https://www.conventionalcommits.org/):

```
<tipo>[alcance opcional]: <descripción corta>

[cuerpo opcional]

[pie opcional: BREAKING CHANGE, Closes #xxx]
```

### Tipos permitidos

| Tipo | Cuándo usarlo |
|------|---------------|
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de bug |
| `refactor` | Cambio de código sin cambio de comportamiento |
| `chore` | Tareas de build, dependencias, configuración |
| `docs` | Solo documentación |
| `test` | Agregar o corregir tests |
| `style` | Formato (no lógica) |
| `perf` | Mejora de rendimiento |
| `ci` | Cambios en GitHub Actions |

### Ejemplos válidos

```bash
git commit -m "feat: agregar autenticación con Google Sign-In"
git commit -m "fix: corregir Locale en SimpleDateFormat para fechas ISO"
git commit -m "refactor: migrar HomeFragment a ViewModel + StateFlow"
git commit -m "chore: actualizar Firebase BOM a 34.11.0"
git commit -m "docs: agregar sección de instalación en README"
git commit -m "test: agregar tests unitarios para debeResetearRacha()"
```

---

## Estándares de código

### Antes de hacer commit

```bash
# Verificar formato
./gradlew ktlintCheck

# Corregir formato automáticamente
./gradlew ktlintFormat

# Análisis estático
./gradlew detekt
```

### Reglas generales

- **Kotlin**: Sigue el [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **ViewBinding**: Siempre sobre `findViewById`
- **Corrutinas**: Preferir `viewModelScope` o `viewLifecycleOwner.lifecycleScope` con `repeatOnLifecycle`
- **Firebase**: Toda operación de datos debe pasar por un `Repository`. Los Fragments y ViewModels no llaman a Firebase directamente.
- **Fechas ISO**: Usa siempre `Locale.ROOT` en `SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)` para fechas que van a Firestore como IDs.
- **Result<T>**: Todas las funciones de repositorio retornan `Result<T>` con `.fold(onSuccess, onFailure)`

---

## Tests

Antes de abrir un PR, asegúrate de que los tests pasan:

```bash
./gradlew test               # Tests unitarios
./gradlew connectedAndroidTest  # Tests de instrumentación (requiere dispositivo/emulador)
```

Para nuevas funcionalidades, incluye al menos un test unitario de la función de repositorio o ViewModel asociado.

---

## Pull Requests

1. Abre el PR **hacia `develop`**, nunca hacia `main`
2. Usa la plantilla de PR (se carga automáticamente)
3. Enlaza el issue que resuelve: `Closes #42`
4. Asegúrate de que el CI pase (`audit.yml`)
5. Espera revisión antes de hacer merge

### Checklist antes de pedir revisión

- [ ] El código compila sin errores
- [ ] `ktlintCheck` pasa sin errores
- [ ] `detekt` pasa sin errores
- [ ] Los tests unitarios pasan
- [ ] La funcionalidad fue probada manualmente en un dispositivo/emulador
- [ ] Los commits siguen Conventional Commits
- [ ] La documentación se actualizó si aplica

---

## Estructura del proyecto

```
app/src/main/java/com/ochoastack/habitus/
├── data/           # Repositorios y modelos de dominio
├── ui/             # Activities, Fragments, Adapters, ViewModels
├── utils/          # Helpers de notificaciones
└── worker/         # WorkManager workers
```

---

*¿Tienes alguna duda? Abre un Issue con la etiqueta `question`.*
