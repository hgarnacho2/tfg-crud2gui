# CRUD2GUI Desktop App

Aplicacion JavaFX de escritorio que presenta el estado de autenticacion de CRUD2GUI. La autenticacion la delega al modulo `authenticator` y siempre se completa en un WebView integrado en la misma ventana mediante MSAL4J, PKCE y callback loopback.

Desde la raiz del repositorio, ejecuta:

```bash
modules/desktop-app/script/run.sh
```

Por defecto, el script instala `common-utils`, `authenticator` y `desktop-app` antes de arrancar, para usar siempre las versiones actuales del flujo embebido. Usa `--no-install` solo cuando los snapshots actuales ya estén instalados en Maven. Opciones:

```bash
modules/desktop-app/script/run.sh --no-install
modules/desktop-app/script/run.sh --debug
modules/desktop-app/script/run.sh --debug --no-install
```

`--debug` muestra la salida `INFO` de Maven y activa el logging de diagnóstico de MSAL.

La ventana muestra si el inicio de sesion se completo y el perfil `Alumno` o `Docente`. Si la cuenta se autentica pero no tiene ninguno de esos roles, la ventana indica que no tiene acceso asignado. No se muestran ni almacenan contrasenas o tokens.

La configuracion de Entra se lee desde las variables `AZURE_*` de `.env.public` o desde variables de entorno/propiedades Java con mayor precedencia. El WebView puede no ofrecer todos los métodos de autenticación, SSO o requisitos MFA/Conditional Access disponibles en un navegador del sistema; valida el flujo con las políticas del tenant.

## Organización de la interfaz

* `Crud2GuiDesktopApp`: punto de entrada JavaFX y creación del `Stage`.
* `login/LoginView`: controles y estados visuales del login.
* `login/LoginController`: coordina la acción de login y actualiza la vista al completarse `authenticator`.

Las siguientes áreas funcionales pueden añadirse como vistas y controladores separados sin mezclar su navegación con la lógica de autenticación.