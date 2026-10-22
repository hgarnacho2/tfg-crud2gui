# CRUD2GUI Desktop App

Aplicacion JavaFX de escritorio que presenta el estado de autenticacion de CRUD2GUI. La autenticacion la delega al modulo `authenticator` y siempre se completa en un WebView integrado en la misma ventana mediante MSAL4J, PKCE y callback loopback.

Desde la raiz del repositorio, ejecuta:

```bash
modules/desktop-app/script/run.sh
```

Por defecto, el script instala `common-utils`, `authenticator`, `statement-text-extractor` y `desktop-app` antes de arrancar, para usar siempre las versiones actuales. Usa `--no-install` solo cuando los snapshots actuales ya estén instalados en Maven. Opciones:

```bash
modules/desktop-app/script/run.sh --no-install
modules/desktop-app/script/run.sh --debug
modules/desktop-app/script/run.sh --debug --no-install
```

`--debug` muestra la salida `INFO` de Maven y activa el logging de diagnóstico de MSAL.

El punto de entrada es `com.crud2gui.desktop.Crud2GuiDesktopApp`. En Windows el perfil Maven
`windows-renderer` pasa `-Dprism.order=sw` a la JVM de la aplicacion. En VS Code utiliza la
configuracion "CRUD2GUI Desktop" de `.vscode/launch.json`, que aplica la misma opcion solo
en Windows. Para otros arranques manuales en Windows, incluye esa opcion en los argumentos
de la JVM, no en los de la aplicacion.

El renderizado por software evita la ruta de texturas Direct3D (`D3DTexture.getContext`)
que fallo al volver a acceder despues del cierre de sesion; no modifica la autenticacion
y puede aumentar el uso de CPU. La opcion debe aplicarse antes de iniciar JavaFX, por lo
que requiere reiniciar la aplicacion. Maven permite cambiarla con `-Dprism.order=...` o
desactivar el perfil con `-P!windows-renderer`; en VS Code se modifica `vmArgs`.

La ventana muestra si el inicio de sesion se completo y el perfil `Alumno` o `Docente`. Si la cuenta se autentica pero no tiene ninguno de esos roles, la ventana indica que no tiene acceso asignado. No se muestran ni almacenan contrasenas o tokens.

La configuracion de Entra se lee desde las variables `AZURE_*` de `.env.public` o desde variables de entorno/propiedades Java con mayor precedencia. El WebView puede no ofrecer todos los métodos de autenticación, SSO o requisitos MFA/Conditional Access disponibles en un navegador del sistema; valida el flujo con las políticas del tenant.

## Organización de la interfaz

Los paneles de alumno y docente incluyen "Cerrar sesion". Se retira el panel, se vacia la cache
MSAL en memoria y se abre el endpoint de cierre de Microsoft en el WebView integrado. El cierre
remoto solo se considera confirmado cuando Microsoft redirige a `AZURE_REDIRECT_URI`, que debe
estar registrado como URI de redireccion en Entra ID. Tras el retorno se crea un acceso nuevo.
La vista de cierre no incluye un boton para volver al acceso: espera el retorno de Microsoft.
Si falla la red o pasan 120 segundos sin retorno, se vuelve al acceso con un aviso
de que el cierre de Microsoft no pudo confirmarse. No se cierran sesiones de otros navegadores.
Cada inicio de sesion solicita `prompt=login`; Microsoft decide el metodo de autenticacion
(contrasena, passkey, MFA o politicas del tenant).

* `Crud2GuiDesktopApp`: punto de entrada JavaFX y creación del `Stage`.
* `login/LoginView`: controles y estados visuales del login.
* `login/LoginController`: coordina la acción de login y actualiza la vista al completarse `authenticator`.
* `ui/UiComponents`: crea textos secundarios y botones de accion con clases CSS.
* `src/main/resources/styles/app.css`: sustituye los records `TextStyle` y `ButtonStyle`. Define `.text-muted`, `.action-button` y la variante `.login-button`. Tambien establece el cursor de los botones y su estado `:disabled`.

La hoja CSS se carga una vez en la escena y se mantiene al cambiar de vista. Para crear una variante visual, anade una clase CSS y asignala con `getStyleClass().add(...)`. Propiedades como `maxWidth`, `defaultButton` y las acciones siguen configurandose en Java. Los estilos especificos restantes de las vistas no se han migrado.

Los paneles de alumno y docente comparten la tarjeta de cuenta de `UiComponents`, con avatar,
nombre, perfil y cierre de sesion, sin un encabezado redundante. El perfil y el boton usan
texto de 12px de alto contraste. La cuenta permanece al pie cuando hay espacio; con poca
altura, la barra lateral permite desplazarse para acceder a la navegacion y a la tarjeta
sin superponerlas.

Las siguientes áreas funcionales pueden añadirse como vistas y controladores separados sin mezclar su navegación con la lógica de autenticación.

### Dashboards y paginas

`StudentDashboardView` y `TeacherDashboardView` componen la barra lateral y el area central.
Los enums privados `StudentDashboardView.StudentSection` y `TeacherDashboardView.TeacherSection` definen las opciones del menu;
`UiComponents.dashboardSidebar` crea sus enlaces y sincroniza la seleccion con el dashboard,
sin construir paginas.

La misma factoria construye toda la barra para ambos perfiles: contenedor, cabecera con
logo UNED, menu y tarjeta de cuenta. Los dashboards solo proporcionan el nombre del panel,
la tarjeta de cuenta y las opciones de navegacion. El layout y los estados visuales del
menu se definen en CSS, sin duplicar su construccion en los dashboards.

Cada perfil tiene una carpeta `sections` con una clase por pagina. Cada pagina construye
su propia cabecera mediante `UiComponents.sectionHeader` y su contenido. Los componentes
visuales compartidos de un perfil permanecen en un helper de ese perfil, no en el dashboard.
Las paginas se crean al visitarlas por primera vez y se conservan al navegar. Las acciones
que cambian de pagina reciben callbacks del dashboard; no crean otros dashboards.
Los datos de practicas del docente se comparten entre resumen y practicas para reflejar
los borradores nuevos sin descartar las instancias de las paginas.

### Enunciados del docente

`teacher/sections/StatementSection` muestra tres tarjetas numeradas conectadas por flechas:
**Seleccionar PDF → Extraer texto → Filtrar texto**. Las tarjetas abren el panel del paso
sin ejecutar su accion y resaltan el paso activo y los completados.
Las tarjetas tienen una altura uniforme de 76 px y el flujo un ancho maximo de 860 px,
para mantener una cabecera compacta incluso en ventanas grandes.

Inicialmente solo esta disponible el primero. Elegir un PDF habilita el segundo paso,
pero mantiene abierto el primero para previsualizar el documento. El visor local usa PDFBox
en segundo plano, con anterior/siguiente, contador de paginas y zoom del 50 al 150 %.
Renderiza solo una pagina, limita la resolucion y cierra el archivo tras cada carga.
No permite seleccionar texto. Los errores del visor se muestran sin bloquear el flujo.
El boton del segundo paso usa `StatementExtractor.extractText(Path)` en un hilo auxiliar y muestra
el texto de solo lectura. Una extraccion correcta habilita el tercer paso, sin ocultar
el resultado extraido. El tercer panel permite ejecutar `StatementFilter.filter(String)`
con las reglas predeterminadas del modulo: inicio en Introduccion, omision de Requisitos
previos y fin en Fechas y normas de entrega. No modifica el texto original.
Si no se reconoce la estructura, muestra un aviso y el original sin marcar el filtrado
como completado. Ambos resultados se pueden seleccionar y copiar.

Se puede volver a los pasos habilitados sin perder datos. Elegir otro PDF invalida la
extraccion y el filtrado; repetir la extraccion invalida el filtro. Cancelar el selector
conserva el estado. Durante el procesamiento se bloquean acciones y navegacion, y se
muestra progreso. Los errores permiten reintentar. No se realiza OCR, no se crean archivos
TXT ni se envian documentos a servicios externos. El estado se conserva al navegar por el
dashboard. El modulo extractor hereda Java 21 del proyecto padre.