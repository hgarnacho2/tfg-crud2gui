# CRUD2GUI Desktop App

Aplicacion JavaFX de escritorio que presenta el estado de autenticacion de CRUD2GUI. La autenticacion la delega al modulo `authenticator` y siempre se completa en un WebView integrado en la misma ventana mediante MSAL4J, PKCE y callback loopback.

Desde la raiz del repositorio, ejecuta:

```bash
modules/desktop-app/script/run.sh
```

Por defecto, el script compila e instala `desktop-app` y todas sus dependencias del reactor
(incluidos el modelo y los extractores) antes de arrancar, para usar las versiones actuales.
No ejecuta `clean`: en Windows puede fallar al borrar archivos de `target` que otro proceso
tenga abiertos. Los errores de compilacion o instalacion detienen el arranque.
Usa `--no-install` solo cuando los snapshots actuales ya estén instalados en Maven. Opciones:

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
Las tarjetas tienen una altura uniforme de 76 px. En las tres secciones con pasos,
el flujo conserva su ancho preferido, alineado a la izquierda, sin estirarse en
ventanas grandes. Cada tarjeta tiene un ancho preferido de 260 px; los flujos con
mas pasos ocupan el ancho correspondiente a su numero de tarjetas.

Inicialmente solo esta disponible el primero. Elegir un PDF habilita el segundo paso,
pero mantiene abierto el primero para previsualizar el documento. El visor local usa PDFBox
en segundo plano, con anterior/siguiente, contador de paginas y zoom del 50 al 150 %.
Renderiza solo una pagina, limita la resolucion y cierra el archivo tras cada carga.
La altura del visor se ajusta a la hoja completa y al zoom; si supera la ventana,
se utiliza el desplazamiento general de la seccion.
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
muestra progreso. Los errores permiten reintentar. No se realiza OCR ni se envian documentos
a servicios externos. El estado se conserva al navegar por el
dashboard. El modulo extractor hereda Java 21 del proyecto padre.

La carpeta de salida predeterminada es la del PDF y se puede cambiar antes de cada
operacion. Para `enunciado.pdf`, el paso 2 guarda `enunciado.txt` y el paso 3 guarda
`enunciado-filtrado.txt`, ambos en UTF-8. El destino se muestra durante el proceso y
la ruta generada aparece solo en el mensaje final inferior, tras guardar
correctamente. Los archivos existentes requieren confirmacion antes de sobrescribir;
cancelar conserva los resultados. Cambiar la carpeta no mueve archivos anteriores;
seleccionar otro PDF restablece el destino a su carpeta. No se guarda un TXT vacio si
el PDF no contiene texto. Si el filtro no reconoce la estructura, guarda el original
en el archivo `-filtrado.txt` con un aviso explicito.
La configuracion de salida aparece dentro de los pasos 2 y 3, justo despues de su
subtitulo; el paso 1 se dedica al PDF. El boton secundario «Cambiar carpeta de salida»
permite modificar el destino. Debajo del mensaje final, «Abrir carpeta de salida»
abre ese destino en el explorador del sistema y comunica los errores de apertura.

### Visor del texto del enunciado

Los pasos de Enunciado, Modelo desde enunciado y Modelo desde codigo muestran su estado
al pie del panel. Al completar un paso, el mensaje confirma la accion e indica
«Puedes continuar con el paso X: Nombre del paso». El ultimo paso indica «Flujo completado».
Los detalles de archivos generados, avisos y estadisticas se conservan debajo de esa indicacion.

En «Enunciado >> Modelo», el visor inferior permanece oculto y no ocupa espacio hasta
que se selecciona y carga correctamente un fichero de texto. Cancelar o un error de
lectura conserva la previsualizacion anterior, si la habia.
El panel inicial queda compacto y alineado arriba; al cargar el texto aprovecha la
altura disponible para el visor.
El paso «Seleccionar prompt» sigue el mismo comportamiento: visor oculto y panel
compacto hasta cargar el fichero correctamente. Entonces el visor ocupa la altura
disponible y la etiqueta inferior confirma que se puede continuar con el paso 3:
Seleccionar LLM. Cancelar o un error de lectura conserva el prompt anterior, si lo habia.
Si el LLM devuelve una respuesta con JSON incorrecto o incompatible con el modelo,
el visor muestra el texto original sin modificarlo. La etiqueta inferior conserva
el error y el paso del diagrama permanece bloqueado; esa respuesta no se guarda
como un modelo valido. Se puede volver a pulsar «Obtener modelo» para reintentar.

### Seleccion de codigo del docente

El panel docente comienza en «Enunciado»; las secciones «Resumen», «Practicas», «Revisiones» y «Modelos»
se han retirado de su navegacion. El panel del estudiante no cambia.

En «Codigo >> Modelo», una tarjeta compacta indica inicialmente que no hay carpeta
seleccionada. El arbol y su estado de carga permanecen ocultos hasta seleccionar una
carpeta valida; cancelar el selector conserva el estado anterior.
El panel de seleccion tambien permanece compacto hasta seleccionar la carpeta.
El arbol de la carpeta seleccionada ocupa la altura disponible
y permite desplazarse con la rueda y las barras. Las carpetas permanecen expandidas;
los elementos no se pueden seleccionar, abrir ni plegar.
Los ficheros con extension `.java` se resaltan en verde y negrita.
Tras cargar el contenido, un mensaje verde confirma la seleccion e indica que se puede
continuar al paso 2: Obtener modelo.
El paso «Obtener modelo» muestra la tarjeta de destino JSON y permite cambiar o abrir
la carpeta de salida. Guarda cada resultado con fecha y hora, sin sobrescribir los
anteriores; por defecto utiliza `modules/java-model-extractor/output`.
El panel permanece compacto y alineado arriba hasta obtener y guardar el modelo.
Entonces el visor JSON ocupa la altura disponible y el estado inferior indica la
ruta guardada y que se puede continuar con el diagrama. Si falla la escritura, el
paso no se completa y se muestra el error.
Las insignias TXT y FOLDER de las secciones de modelos usan el mismo estilo gris
que la insignia PDF.

En ambos flujos de modelos, el visor del diagrama conserva la altura completa del
modelo, sin ajustarla a la pantalla ni recortar las clases. Si supera la ventana,
se utiliza el desplazamiento vertical general de la pagina, tambien con la rueda
sobre el diagrama. El visor mantiene su desplazamiento horizontal cuando es necesario.