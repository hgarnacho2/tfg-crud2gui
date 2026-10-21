# CRUD2GUI Desktop App

Aplicacion JavaFX de escritorio que presenta el estado de autenticacion de CRUD2GUI. La autenticacion la delega al modulo `authenticator`; `AZURE_AUTH_BROWSER_MODE` permite elegir entre el navegador del sistema (`system`, predeterminado) y un WebView integrado en la ventana (`embedded`). Ambos modos usan el flujo interactivo de MSAL4J con PKCE y callback loopback.

Desde la raiz del repositorio, ejecuta:

```bash
modules/desktop-app/script/run.sh
```

Por defecto, el script omite la instalación y oculta los mensajes informativos; requiere que las dependencias estén instaladas en Maven. Usa `--install` para instalar `common-utils`, `authenticator` y `desktop-app`. Opciones:

```bash
modules/desktop-app/script/run.sh --install
modules/desktop-app/script/run.sh --no-install
modules/desktop-app/script/run.sh --debug
modules/desktop-app/script/run.sh --debug --no-install
```

`--debug` muestra la salida `INFO` de Maven y activa el logging de diagnóstico de MSAL.

La ventana muestra si el inicio de sesion se completo y el perfil `Alumno` o `Docente`. Si la cuenta se autentica pero no tiene ninguno de esos roles, la ventana indica que no tiene acceso asignado. No se muestran ni almacenan contrasenas o tokens.

La configuracion de Entra se lee desde las variables `AZURE_*` de `.env.public` o desde variables de entorno/propiedades Java con mayor precedencia. Para probar el WebView, configura `AZURE_AUTH_BROWSER_MODE=embedded`; para volver al navegador del sistema, usa `AZURE_AUTH_BROWSER_MODE=system`. El comportamiento de MFA y Conditional Access en WebView debe verificarse con las politicas del tenant; el modo system mantiene la compatibilidad mas amplia.

## Organización de la interfaz

* `Crud2GuiDesktopApp`: punto de entrada JavaFX y creación del `Stage`.
* `login/LoginView`: controles y estados visuales del login.
* `login/LoginController`: coordina la acción de login y actualiza la vista al completarse `authenticator`.

Las siguientes áreas funcionales pueden añadirse como vistas y controladores separados sin mezclar su navegación con la lógica de autenticación.