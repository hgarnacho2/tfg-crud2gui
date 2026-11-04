# CRUD2GUI Authenticator

Biblioteca Maven que autentica clientes públicos de escritorio contra Microsoft Entra ID mediante MSAL4J. La interfaz anfitriona proporciona la acción que presenta la autorización; actualmente `desktop-app` la carga en un `WebView` JavaFX. El flujo interactivo usa authorization code con PKCE y un callback loopback.

Este módulo no es una aplicación ejecutable y no depende directamente de JavaFX. No incluye credenciales de usuario ni `client secret`.

## 📂 Estructura del módulo

```text
authenticator/
├── pom.xml
├── README.md
├── script/
│   ├── install.sh
│   ├── test.sh
│   └── verify.sh
└── src/
    ├── main/
    │   ├── java/com/crud2gui/authenticator/entraid/
    │   │   ├── AuthenticatedUser.java
    │   │   ├── AuthenticatorConfig.java
    │   │   ├── EntraIdAuthenticator.java
    │   │   └── IdTokenClaims.java
    │   └── resources/templates/
    │       ├── auth_error_page.html
    │       └── auth_success_page.html
    └── test/java/com/crud2gui/authenticator/entraid/
        ├── AuthenticatorConfigTest.java
        ├── AuthPageTemplatesTest.java
        ├── EntraIdAuthenticatorTest.java
        └── IdTokenClaimsTest.java
```

## Configuración de Entra

En el registro de aplicación, configura la plataforma **Mobile and desktop applications** con el redirect URI loopback que vaya a usar la aplicación, por defecto `http://localhost`. Registra los roles de aplicación `alumno` y `docente` y asígnalos a los usuarios en **Enterprise applications > Users and groups**.

El registro debe ser un cliente público; no necesita `client secret`. El WebView integrado puede no admitir todas las opciones de SSO, MFA o Conditional Access del navegador del sistema. Comprueba el flujo con las políticas del tenant.

## Configuración

`AuthenticatorConfig.fromEnvironment()` lee las variables con esta precedencia: entorno del sistema, propiedades Java `-D`, `.env.private` y `.env.public`. Si no se indican scopes o redirect URI, aplica los valores predeterminados.

| Variable | Descripción | Predeterminado |
|---|---|---|
| `AZURE_CLIENT_ID` | Application (client) ID del registro | Obligatoria |
| `AZURE_TENANT_ID` | ID o dominio del tenant | Obligatoria |
| `AZURE_REDIRECT_URI` | Callback HTTP loopback registrado | `http://localhost` |
| `AZURE_SCOPES` | Scopes separados por espacios o comas | `openid profile email` |

El client ID y el tenant ID identifican la aplicación y el tenant; no son secretos. No configures contraseñas de usuario ni un client secret.

## API

Los tipos de Entra están en `com.crud2gui.authenticator.entraid` y la identidad devuelta es `AuthenticatedUser`.

```java
import com.crud2gui.authenticator.entraid.AuthenticatorConfig;
import com.crud2gui.authenticator.entraid.EntraIdAuthenticator;

AuthenticatorConfig config = AuthenticatorConfig.fromEnvironment();
EntraIdAuthenticator authenticator =
        new EntraIdAuthenticator(config, loginView::showEmbeddedBrowser);

authenticator.authenticate().thenAccept(user -> {
    if (user.isDocente()) {
        // Mostrar funciones de docente.
    } else if (user.isAlumno()) {
        // Mostrar funciones de alumno.
    }
});
```

`OpenBrowserAction` permite que la interfaz anfitriona decida cómo presentar la URL de autorización. `authenticate()` es asíncrono y devuelve `CompletableFuture<AuthenticatedUser>`; no bloquees el hilo de interfaz. `AuthenticatedUser` contiene claims de identidad y roles, no el token. MSAL gestiona la caché de tokens en memoria.

`clearAccounts()` elimina las cuentas de la caché MSAL en memoria. `logoutUri()` construye el endpoint de cierre de sesión de Entra, pero no abre el navegador ni cierra por sí solo la sesión web; la aplicación anfitriona debe completar el retorno al callback.

Las páginas locales que MSAL muestra al completar o fallar el callback están en `src/main/resources/templates/auth_success_page.html` y `src/main/resources/templates/auth_error_page.html`. Son páginas de resultado; la página de inicio de sesión sigue alojada por Microsoft.

## Pruebas

```bash
modules/authenticator/script/test.sh
```

Las pruebas son unitarias: no abren el navegador ni autentican contra Entra. El test de claims usa un JWT sintético para comprobar la extracción del payload; la autenticidad del token corresponde a MSAL.