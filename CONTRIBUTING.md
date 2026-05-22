# Guía de Contribución

¡Gracias por tu interés en contribuir a Habitus! Para mantener la calidad, seguridad y consistencia del proyecto, seguimos una serie de lineamientos estrictos inspirados en las mejores prácticas de la industria Open Source.

Por favor, lee este documento antes de abrir un *Pull Request* o un *Issue*.

## Flujo de Trabajo en Git

Adoptamos un modelo estructurado para las ramas de desarrollo:

- **`main`**: Rama de producción. Todo código aquí es estable, seguro y ha sido lanzado mediante GitHub Releases.
- **`develop`**: Rama de integración. Aquí confluyen todas las nuevas características antes de estabilizarse.
- **Ramas de trabajo**: Deben desprenderse siempre de `develop` y usar los siguientes prefijos semánticos:
  - `feat/`: Nuevas características (ej: `feat/dark-mode`).
  - `fix/`: Resolución de errores (ej: `fix/notification-crash`).
  - `docs/`: Actualizaciones a la documentación (ej: `docs/project-documentation`).
  - `refactor/`: Refactorizaciones que no alteran la funcionalidad observable.
  - `chore/`: Mantenimiento, dependencias o tooling (ej: `chore/update-gradle`).

El ciclo estándar de contribución es:
`develop` → rama de trabajo (`feat/xyz`) → Pull Request a `develop` → (al estabilizar) Merge a `main`.

## Convención de Commits

Utilizamos el estándar [Conventional Commits](https://www.conventionalcommits.org/). El formato del mensaje debe ser:
```text
<tipo>: <descripción corta en inglés y minúsculas>

[Opcional: Descripción detallada de por qué se hizo el cambio y cómo funciona]
```
Tipos permitidos: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`.

**Ejemplo correcto:**
```text
feat: add local notifications via WorkManager

Implemented WeeklySummaryWorker to trigger local notifications every Sunday assessing the user's progress.
```

## Estándares de Código y Arquitectura

1. **Linting y Análisis Estático**: El código debe pasar limpio por **ktlint** y **detekt**. Verifica esto localmente corriendo `./gradlew ktlintCheck detekt` antes de hacer push.
2. **Convención de Comentarios**: Se exige el uso de la **primera persona del plural**, con descripciones directas, claras y pragmáticas en español.
   - Correcto: `// Sincronizamos las completaciones con Firebase para evitar desfaces en la racha`
   - Incorrecto: `// TODO: sync fb` o `// Firebase syncer method`
3. **Estructura de Paquetes**: Se mantiene división estricta por responsabilidades: `di` (Hilt), `ui` (Activities/Fragments/ViewModels), `data` (Repositories/Models) y `worker`.

## 🚀 Proceso de Pull Request (PR)

Para fusionar código en `develop`, tu PR debe cumplir el siguiente checklist de calidad:

- [ ] Has rellenado nuestra [Plantilla de Pull Request](.github/PULL_REQUEST_TEMPLATE.md) al abrir el PR.
- [ ] Código compilable sin advertencias (`./gradlew assembleDebug` termina con `BUILD SUCCESSFUL`).
- [ ] Pasa las comprobaciones de CI automatizadas (GitHub Actions).
- [ ] Cumple con las convenciones de commit y la nomenclatura de ramas.
- [ ] **NO INCLUYE** secretos como `google-services.json` ni archivos `.jks`.

**Merge Strategy**: Se utilizará `Merge pull request` normal sin `Fast-Forward` (`--no-ff`) para mantener el registro visual de la integración de la funcionalidad en el árbol de commits.

## 🐞 Reporte de Bugs y Sugerencias (Issues)

Si encuentras un bug o tienes una idea para mejorar la app, te invitamos a usar el sistema de Issues de GitHub usando nuestras plantillas predefinidas:

- **Para reportar un error**: Utiliza nuestra [Plantilla de Bug Report](.github/ISSUE_TEMPLATE/bug_report.md).
- **Para sugerir una mejora**: Utiliza nuestra [Plantilla de Feature Request](.github/ISSUE_TEMPLATE/feature_request.md).

Al crear tu issue, asegúrate de proporcionar todos los detalles solicitados en la plantilla (pasos para reproducir, entorno, comportamiento esperado, etc.).

*(Por favor, no incluyas capturas de pantalla de código, en su lugar usa bloques de código Markdown).*
