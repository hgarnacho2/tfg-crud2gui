# CRUD2GUI: Análisis de modelos y Generador de interfaces

## Estructura de las secciones

Las secciones de docente y alumno heredan de `com.crud2gui.desktop.ui.Section`.
Esta base comparte el espaciado, los márgenes y la cabecera, con o sin botón de
acción. La cabecera ocupa el primer hijo y cada sección añade después su contenido.
También proporciona un logger protegido que identifica a la clase concreta.
Comparte los estados CSS de error, selección y finalización, la actualización de
mensajes y la ejecución auxiliar de tareas con callbacks en JavaFX y limpieza final.
Cada sección conserva el procesamiento específico y decide qué controles bloquear,
qué panel actualizar y qué mensajes presentar.

`UiComponents.fileContentPane(id, title, pathSupplier, errorMessage)` crea un visor
desplegable reutilizable de archivos UTF-8, sin interpretar su formato. Resuelve la
ruta y recarga el contenido en cada apertura; muestra y registra los errores de
lectura y permite recuperarse en la siguiente apertura. Utiliza las clases CSS
genéricas `file-content`, `file-content-text`, `file-content-path` y `status-message`.
Los identificadores se derivan del ID recibido, por lo que cada sección puede
incorporar varios visores sin acoplar sus estilos al dominio del enunciado.

## Ejemplo portable para Windows

Requisitos de empaquetado: Windows, Maven y un JDK 21 completo con `jpackage`.
`JAVA_HOME`, si está definido, debe apuntar al mismo JDK que utiliza Maven.

Desde la raíz del repositorio:

```powershell
powershell -ExecutionPolicy Bypass -File .\script\package-windows.ps1
```

El script compila la interfaz y sus dependencias (sin ejecutar tests) y genera
una carpeta fechada dentro de `dist`. También admite `-OutputDirectory`.
No crea un instalador ni necesita WiX. La distribución incluye Java:

```text
CRUD2GUI\
  CRUD2GUI.exe
  app\
    config\
      .env.public
    ... JAR de escritorio y dependencias
  runtime\
    ... Java incluido
```

Edita `app\config\.env.public` con el identificador de cliente, tenant, URI de
retorno y scopes de tu aplicación de Entra ID. La URI debe coincidir con la
registrada en Entra ID. El script no copia los archivos de entorno del proyecto
ni sus secretos. Las variables de entorno y propiedades Java explícitas tienen
prioridad sobre esta configuración. Opcionalmente se puede crear un archivo
local `app\config\.env.private`; no lo compartas.

Ejecuta `CRUD2GUI.exe` con doble clic. Este ejemplo abre también una consola para
ver errores de arranque. Copia **toda la carpeta CRUD2GUI**, no solo el ejecutable.
El resultado es para Windows y la arquitectura del JDK utilizado.

Solo se incluyen los módulos de los que depende actualmente `desktop-app`; no
todos los módulos del reactor Maven. El escritorio carga las reglas del JSON
externo en cada filtrado; si falta o es inválido, la operación falla sin generar
un resultado ni utilizar reglas predeterminadas como alternativa.

En el panel «Filtrar texto», el desplegable «Ver statement-filter-rules.json» permite
consultar el archivo en modo de solo lectura. Se vuelve a leer al desplegarlo.
La ruta se obtiene de `STATEMENT_FILTER_CONFIG_PATH` (variable de entorno,
propiedad Java o configuración de entorno del proyecto). Si no se define, se busca
`modules/statement-text-extractor/config/statement-filter-rules.json` desde el directorio
de ejecución y sus antecesores. Para la distribución portable, copia el JSON
externo e indica su ruta absoluta; no se incluye automáticamente.
Si el archivo no puede leerse, el desplegable muestra un error.
La consulta y el filtrado utilizan la misma ruta. Tras editar el archivo,
vuelve a desplegarlo para consultar su contenido actualizado.
