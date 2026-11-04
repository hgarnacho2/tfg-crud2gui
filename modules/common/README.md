# Common

Librería Maven con utilidades compartidas por los módulos de CRUD2GUI. Actualmente ofrece configuración desde ficheros `.env` y extracción de texto de PDF; no contiene interfaz gráfica.

## DotEnv

`com.crud2gui.common.utils.DotEnv` lee ficheros UTF-8 con entradas `CLAVE=valor`. Admite líneas vacías, comentarios que empiezan por `#`, el prefijo `export` y comillas simples o dobles alrededor del valor. No interpreta sustituciones de variables ni comentarios al final de una línea.

Para resolver una variable, `getStringVar` y `getBooleanVar` aplican esta precedencia:

1. Variable de entorno del sistema.
2. Propiedad Java (`-DCLAVE=valor`).
3. `.env.public`.
4. `.env.private`.
5. Valor predeterminado recibido por el método.

Al cargar varios ficheros con `load`, se procesan en el orden indicado y las claves posteriores sobrescriben las anteriores. Los ficheros inexistentes se ignoran. `loadDefaultsIn` carga `.env.public` y `.env.private` desde un directorio; `loadProjectDefaults` busca la configuración en los recursos del classpath o en el proyecto Maven a partir de `user.dir`.

```java
import com.crud2gui.common.utils.DotEnv;

import java.util.Map;

String clientId = DotEnv.getStringVar("AZURE_CLIENT_ID", "");
boolean debug = DotEnv.getBooleanVar("DOTENV_DEBUG", false);
Map<String, String> config = DotEnv.loadProjectDefaults();
```

La depuración se activa con `DOTENV_DEBUG=true` y se evalúa una vez por proceso. No guardes secretos en `.env.public`; usa `.env.private`, excluido de Git.

## PDF

`com.crud2gui.common.utils.PdfTextExtractor` extrae el texto digital de un PDF ordenándolo por posición. No realiza OCR, por lo que un PDF compuesto solo por imágenes no produce texto mediante este extractor.

```java
import com.crud2gui.common.utils.PdfTextExtractor;

import java.nio.file.Path;

Path pdfPath = Path.of("documento.pdf");
String text = PdfTextExtractor.extractText(pdfPath);
Path txt = PdfTextExtractor.extractText(pdfPath, Path.of("salida/documento.txt"));
Path sibling = PdfTextExtractor.siblingTxtPath(pdfPath);
```

La variante que recibe una salida crea las carpetas necesarias, escribe UTF-8 y devuelve la ruta absoluta normalizada. Las rutas de entrada y salida, una vez normalizadas, deben ser distintas. PDFBox es una dependencia opcional de `common`: cada módulo que use `PdfTextExtractor` debe declarar PDFBox en su propio `pom.xml`.

## Estructura

```text
common/
├── pom.xml
├── script/
│   ├── install.sh
│   ├── test.sh
│   └── verify.sh
└── src/
    ├── main/
    │   └── java/com/crud2gui/common/utils/
    │       ├── DotEnv.java
    │       └── PdfTextExtractor.java
    └── test/
        └── java/com/crud2gui/common/utils/
            ├── DotEnvTest.java
            └── PdfTextExtractorTest.java
```

Desde la raíz del repositorio:

```bash
modules/common/script/test.sh
modules/common/script/verify.sh
modules/common/script/install.sh
```