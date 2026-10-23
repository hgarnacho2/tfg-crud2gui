# CRUD2GUI: Análisis de modelos y Generador de interfaces

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
todos los módulos del reactor Maven. El JSON externo del filtro no se conecta
automáticamente al escritorio: el filtrado sigue usando sus reglas predeterminadas.
