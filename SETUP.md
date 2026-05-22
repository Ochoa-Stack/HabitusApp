# Guía de Entorno (Setup) ⚙️

Esta guía te ayudará a configurar el entorno de desarrollo local para HabitusApp desde cero.

## 📋 Requisitos Previos

Asegúrate de contar con las siguientes herramientas instaladas antes de comenzar:

- **Java Development Kit (JDK) 17 o superior** (Requerido por AGP 8.7+).
- **Android Studio Hedgehog (2023.1.1) o superior** (Para compatibilidad total con Kotlin 2.0+).
- **Git** para control de versiones.
- **Cuenta de Google** para configurar el proyecto de Firebase asociado.

---

## 🛠️ Clonación y Configuración Local

1. **Clona el repositorio** en tu máquina local:
   ```bash
   git clone https://github.com/TU_USUARIO/HabitTrackerApp.git
   cd HabitTrackerApp
   ```

2. **Abre el proyecto** en Android Studio. Espera a que la fase inicial de `Gradle Sync` termine, aunque probablemente falle si te falta el archivo de Firebase (ver la siguiente sección).

---

## 🔥 Configuración de Firebase

HabitusApp no puede compilar sin conectarse a tu propio proyecto de Firebase. 

1. Ve a la [Consola de Firebase](https://console.firebase.google.com/).
2. Crea un nuevo proyecto llamado `HabitusApp` (o como prefieras).
3. Dentro del proyecto, añade una aplicación Android con el Application ID: `com.ochoastack.habitus`.
4. **Descarga el archivo `google-services.json`**.
5. Mueve el archivo descargado directamente a la carpeta `app/` de tu proyecto:
   ```text
   HabitTrackerApp/
   ├── app/
   │   └── google-services.json   <-- ¡Aquí!
   ...
   ```
6. En la consola de Firebase, activa los siguientes servicios:
   - **Authentication**: Habilita el proveedor de **Correo electrónico y contraseña**.
   - **Firestore Database**: Crea una base de datos en modo producción. Se configurará usando las reglas propias de la aplicación más adelante.

---

## 🔐 Configuración para CI/CD (GitHub Secrets)

Si deseas utilizar los flujos automatizados (`audit.yml` y `release.yml`) incluidos en el repositorio, necesitas proveer los secretos necesarios a GitHub:

En tu repositorio de GitHub, navega a **Settings > Secrets and variables > Actions > New repository secret** y añade:

| Nombre del Secreto | Propósito | Formato / Comando sugerido |
|--------------------|-----------|---------------------------|
| `GOOGLE_SERVICES_JSON` | Otorga a los runners acceso a Firebase | `base64 app/google-services.json` |
| `KEYSTORE_B64` | Archivo JKS para firmar la versión de Release | `base64 habitus-release.jks` |
| `KEYSTORE_PASSWORD` | Contraseña general de tu keystore | Texto plano (ej. `mypassword`) |
| `KEY_ALIAS` | Alias de tu clave de firma | Texto plano (ej. `habitus_key`) |
| `KEY_PASSWORD` | Contraseña asociada al Alias | Texto plano |

---

## 💻 Comandos Útiles de Build y Test

Si trabajas desde la terminal, puedes usar el wrapper de Gradle (`gradlew`) para realizar tareas cotidianas:

- **Limpiar y Compilar APK de Debug**:
  ```bash
  ./gradlew clean assembleDebug
  ```
- **Correr Tests Unitarios Locales**:
  ```bash
  ./gradlew test
  ```
- **Validar estándares de formato de código (ktlint)**:
  ```bash
  ./gradlew ktlintCheck
  ```

---

## 🐛 Solución de Errores Comunes

### Error: `PROJECT_SOFT_DELETED` en Firebase
- **Causa**: Significa que el proyecto de Firebase asociado al `google-services.json` fue eliminado en la nube, pero tu app aún intenta consultarlo.
- **Solución**: Descarga el `google-services.json` actualizado de un proyecto de Firebase activo y reemplaza el archivo en la carpeta `app/`.

### Error: `Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.2.0, expected version is 2.0.0.`
- **Causa**: Alguna dependencia transitiva en Gradle está bajando artefactos compilados con Kotlin 2.1+, incompatibles con el entorno bloqueado actual.
- **Solución**: No actualices `firebaseBom` a versiones superiores a `33.4.0` ni subas arbitrariamente versiones de `androidx.hilt`. El archivo `gradle/libs.versions.toml` está fijado por una razón.

### Las Notificaciones Locales no llegan
- **Causa**: Desde Android 13 (API 33), el permiso de notificaciones `POST_NOTIFICATIONS` no se otorga por defecto; debe ser solicitado explícitamente en tiempo de ejecución.
- **Solución**: Asegúrate de que el flujo de UI dispare el _launcher_ de permisos y que el dispositivo no tenga la aplicación restringida en segundo plano desde las configuraciones de batería.
