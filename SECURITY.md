# Política de Seguridad y Privacidad

La seguridad y protección de datos son pilares fundamentales en el diseño de HabitusApp. Este documento detalla nuestro modelo de seguridad actual y cómo reportar vulnerabilidades de manera responsable.

## Alcance

La presente política cubre exclusivamente las versiones del software contenidas en la rama `main` y en los Releases oficiales de este repositorio. Las áreas soportadas incluyen:

- El código fuente de la aplicación Android.
- La configuración de seguridad de las reglas de Firebase Firestore y Firebase Auth.
- La configuración del pipeline de Integración Continua (GitHub Actions).
- El sistema de manejo e ignorado de credenciales criptográficas y secretos.

Cualquier vulnerabilidad que afecte la infraestructura de Google Firebase subyacente queda fuera de nuestro alcance y debe reportarse a Google directamente.

## Procedimiento de Reporte de Vulnerabilidades

**Por favor, NO abras un Issue público si descubres un problema de seguridad crítico.**

La divulgación pública prematura podría poner en riesgo los datos de los usuarios. En su lugar, utiliza uno de los siguientes métodos seguros:

1. **GitHub Private Vulnerability Reporting**: Si está habilitado en este repositorio, utiliza la pestaña "Security" > "Advisories" > "Report a vulnerability" para enviar tu hallazgo en un entorno cifrado.
2. **Email Directo**: Envía un correo (placeholder) describiendo la vulnerabilidad, los pasos para reproducirla y el impacto potencial.

### Tiempo de Respuesta Esperado
Nos esforzamos por reconocer cualquier reporte en un plazo de **48 a 72 horas**. Te mantendremos informado sobre el progreso hacia la corrección y emisión de un parche.

## Prácticas de Seguridad Actuales

Nuestra arquitectura incluye mecanismos defensivos aplicados por defecto:

1. **Manejo de Secretos (Zero-Exposure)**: Los archivos sensibles como `google-services.json` y el keystore `habitus-release.jks` se encuentran en el `.gitignore` por defecto y jamás son commiteados. Los entornos de compilación de CI/CD los reciben bajo demanda vía GitHub Secrets encriptados.
2. **Restricción de Firestore**: Las `firestore.rules` imponen una política de autenticación estricta: ninguna solicitud externa puede leer o escribir datos. Los usuarios autenticados operan dentro de un silo (`uid == request.auth.uid`), impidiendo el acceso cruzado a datos de terceros.
3. **Ofuscación y Optimización de Bytecode**: La aplicación de producción se empaqueta utilizando **R8** (ProGuard moderno) para ofuscar nombres de clases y funciones, mitigando la efectividad de los esfuerzos de ingeniería inversa.
4. **Prevención de Fugas de Datos Físicas**: Se ha establecido el flag `android:allowBackup="false"` en el `AndroidManifest.xml` para prevenir que respaldos inseguros en la nube extraigan la base de datos de preferencias locales del usuario a entornos no controlados.

## Descargo de Responsabilidad

HabitusApp nace como un proyecto de desarrollo personal. Aunque hemos implementado estándares y mitigaciones de seguridad rigurosas y profesionales, **este software no ha sido auditado por una firma independiente de seguridad informática**. 

Se proporciona "tal cual", sin garantías de idoneidad para entornos críticos, corporativos o médicos. Su uso para almacenar información extremadamente sensible queda bajo la responsabilidad exclusiva del usuario.
