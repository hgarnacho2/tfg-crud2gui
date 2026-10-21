# CRUD2GUI Authenticator

Modulo reutilizable para autenticar una aplicacion de escritorio Java contra Microsoft Entra ID. Usa MSAL4J como cliente publico; la app JavaFX presenta el login en un WebView integrado y recibe el retorno por loopback con el flujo interactivo de codigo de autorizacion y PKCE.

## Registro de Entra

En el registro `CRUD2GUI`, configura la plataforma **Mobile and desktop applications** con el redirect URI `http://localhost`. La aplicacion es un cliente publico: no necesita ni debe incluir un client secret.

Los roles de aplicacion se leen del claim `roles` del ID token. Asigna los roles de aplicacion `alumno` y `docente` a los usuarios desde **Enterprise applications > Users and groups**. La autoridad se limita al tenant indicado por `AZURE_TENANT_ID`.

El `client ID` identifica la aplicacion durante OAuth. El object ID del registro no se usa en el flujo de autenticacion.

## Configuracion

El modulo utiliza `DotEnv`; la precedencia es variable de entorno del sistema, propiedad Java `-D`, `.env.private` y `.env.public`.

| Variable | Uso |
|---|---|
| `AZURE_CLIENT_ID` | Application (client) ID del registro Entra |
| `AZURE_TENANT_ID` | ID o dominio del tenant |
| `AZURE_REDIRECT_URI` | URI loopback registrada para la app de escritorio; por defecto `http://localhost` |
| `AZURE_SCOPES` | Scopes separados por espacios o comas; por defecto `openid profile email` |

Estos valores identifican la aplicacion y el tenant, no son credenciales. No configures contrasenas de usuario ni client secret. La autenticacion de cada persona se presenta dentro de la ventana JavaFX.

## Uso desde la GUI

```java
AuthenticatorConfig config = AuthenticatorConfig.fromEnvironment();
EntraIdAuthenticator authenticator = new EntraIdAuthenticator(config, loginView::showEmbeddedBrowser);

authenticator.authenticate().thenAccept(user -> {
    if (user.isDocente()) {
        // Mostrar funciones de docente.
    } else if (user.isAlumno()) {
        // Mostrar funciones de alumno.
    }
});
```

`authenticate()` devuelve un `CompletableFuture`; la GUI debe consumirlo de forma asíncrona para no bloquear el hilo visual. `AuthenticatedUser` expone identificador, nombre, tenant, roles y expiración, pero no conserva tokens ni credenciales.

Las páginas locales que MSAL muestra al terminar o fallar el callback están personalizadas en español y se pueden editar en `src/main/resources/templates/auth_success_page.html` y `src/main/resources/templates/auth_error_page.html`. No reemplazan ni modifican la página de inicio de sesión alojada por Microsoft. MSAL conserva el flujo de authorization code con PKCE y el callback loopback; la app JavaFX inyecta siempre su WebView mediante `OpenBrowserAction`. El WebView puede no ofrecer todos los métodos de autenticación, SSO o requisitos MFA/Conditional Access; valida el flujo con las políticas del tenant.

El flujo interactivo espera el callback local durante un tiempo limitado. Si falla, confirma que `http://localhost` esta registrado en **Authentication > Mobile and desktop applications**. No compartas la URL de callback ni sus parametros.

## Tests

```bash
modules/authenticator/script/test.sh
```

Las pruebas no abren el navegador ni necesitan acceso a Entra.