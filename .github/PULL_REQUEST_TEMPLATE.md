---
name: Pull Request
about: Describe los cambios de este PR
---

## Descripción

<!-- Explica brevemente qué hace este PR y por qué es necesario -->

## Issue relacionado

<!-- Enlaza el issue que resuelve (si aplica): -->
Closes #

## Tipo de cambio

- [ ] Bug fix (cambio que corrige un error sin romper funcionalidad existente)
- [ ] Nueva funcionalidad (cambio que agrega funcionalidad sin romper la existente)
- [ ] Refactor (cambio de código que no afecta comportamiento externo)
- [ ] Documentación (solo cambios en docs)
- [ ] Chore (build, dependencias, CI/CD)
- [ ] Test (agrega o corrige tests)
- [ ] Breaking change (cambio que rompe funcionalidad existente)

## Checklist

- [ ] El código compila sin errores (`./gradlew assembleDebug`)
- [ ] `ktlintCheck` pasa sin errores (`./gradlew ktlintCheck`)
- [ ] `detekt` pasa sin errores (`./gradlew detekt`)
- [ ] Los tests unitarios pasan (`./gradlew test`)
- [ ] La funcionalidad fue probada manualmente en dispositivo/emulador
- [ ] Los commits siguen el estándar **Conventional Commits**
- [ ] El PR apunta a `develop`, no a `main`

## Capturas / Video (si aplica)

<!-- Capturas de pantalla o video demostrando el cambio visual o funcional -->

## Notas adicionales

<!-- Decisiones de diseño, deuda técnica conocida, cosas a revisar con cuidado, etc. -->
