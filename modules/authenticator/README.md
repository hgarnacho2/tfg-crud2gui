# CRUD2GUI Authenticator

Modulo reutilizable para autenticar una aplicacion de escritorio Java contra Microsoft Entra ID. Usa MSAL4J como cliente publico, inicia sesion en el navegador predeterminado y recibe el retorno por loopback con el flujo interactivo de codigo de autorizacion y PKCE.

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

Estos valores identifican la aplicacion y el tenant, no son credenciales. No configures contrasenas de usuario ni client secret. La autenticacion de cada persona se realiza en el navegador.

## Uso desde la GUI

```java
AuthenticatorConfig config = AuthenticatorConfig.fromEnvironment();
EntraIdAuthenticator authenticator = new EntraIdAuthenticator(config);

authenticator.authenticate().thenAccept(user -> {
    if (user.isDocente()) {
        // Mostrar funciones de docente.
    } else if (user.isAlumno()) {
        // Mostrar funciones de alumno.
    }
});
```

`authenticate()` devuelve un `CompletableFuture`; la GUI debe consumirlo de forma asíncrona para no bloquear el hilo visual. `AuthenticatedUser` expone identificador, nombre, tenant, roles y expiración, pero no conserva tokens ni credenciales.

## Prueba interactiva

Desde la raiz del repositorio:

```bash
modules/authenticator/script/run.sh
```

Se abrira el navegador del sistema. Inicia sesion ahi con el usuario que quieras probar; la consola mostrara el nombre y los roles recibidos. No se deben pasar contrasenas como argumentos ni almacenarlas en archivos de configuracion. Esta prueba requiere una sesion real de Entra y no forma parte de los tests unitarios.

El flujo interactivo espera el callback local durante un tiempo limitado. Si falla, confirma que `http://localhost` esta registrado en **Authentication > Mobile and desktop applications** y que el navegador regreso a esa URL. Para habilitar logs de diagnostico de MSAL sin activar logging de datos personales, ejecuta `AUTHENTICATOR_DEBUG=true modules/authenticator/script/run.sh`. No compartas la URL de callback ni sus parametros.

## Tests

```bash
modules/authenticator/script/test.sh
```

Las pruebas no abren el navegador ni necesitan acceso a Entra.