# Security Policy

## Versiones soportadas

| Versión | Soporte de seguridad |
|---|---|
| 1.0.0   | Activo |

## Reportar una vulnerabilidad

**No uses Issues públicos para reportar vulnerabilidades de seguridad.**

Si descubres una vulnerabilidad de seguridad en Habitus, por favor:

1. **Envía un reporte privado** usando [GitHub Private Security Advisories](https://github.com/Ochoa-Stack/HabitusApp/security/advisories/new)
2. Incluye la siguiente información:
   - Descripción del problema
   - Pasos para reproducirlo
   - Impacto potencial (datos expuestos, usuarios afectados)
   - Versión afectada

### Tiempo de respuesta esperado

| Etapa | Tiempo objetivo |
|-------|-----------------|
| Confirmación de recepción | 48 horas |
| Evaluación inicial | 5 días hábiles |
| Parche y release | 30 días (vulnerabilidades críticas: 7 días) |

## Scope de seguridad

### En scope (reportar)
- Exposición de datos de usuario en Firebase
- Bypass de autenticación
- Escalamiento de privilegios entre usuarios
- Exposición de API keys en el repositorio
- Vulnerabilidades de SSRF o inyección en Firestore rules

### Fuera de scope (no reportar)
- Ataques que requieren acceso físico al dispositivo
- Problemas en versiones no soportadas
- Vulnerabilidades en dependencias sin PoC de explotación real

## Contacto

Para dudas generales de seguridad (no vulnerabilidades activas), abre un Issue con la etiqueta `security`.

---

*Este proyecto maneja datos personales (hábitos, estadísticas de usuario). Tomamos la seguridad seriamente.*
