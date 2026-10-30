# Common Module

El módulo `common` es una librería transversal que proporciona utilidades compartidas y reutilizables en los diferentes componentes del proyecto multi-módulo `TFG_CRUD2GUI`. Se organiza en dos paquetes:

* **`com.crud2gui.common.utils`**: utilidades sin interfaz gráfica.
  * `DotEnv`: gestor de variables de entorno mediante ficheros `.env` públicos y privados.
  * `PdfTextExtractor`: extracción genérica del texto de un PDF (PDFBox) y guardado en `.txt`.
* **`com.crud2gui.common.ui`**: componentes JavaFX reutilizables.
  * `JsonEditorView`: visor/editor de ficheros JSON (árbol + texto).
  * `JavaCodeViewer`: visor de solo lectura de ficheros de código con resaltado de sintaxis Java (RichTextFX).

PDFBox, JavaFX, Jackson y RichTextFX se declaran como dependencias **opcionales**: no se propagan, de modo que cada módulo que use `PdfTextExtractor`, `JsonEditorView` o `JavaCodeViewer` debe declararlas en su propio `pom.xml`.

## 📂 Estructura del Módulo

```text
common/
├── pom.xml
├── README.md
├── script/
└── src/
    ├── main/
    │   ├── java/com/crud2gui/common/
    │   │   ├── ui/
    │   │   │   ├── JavaCodeViewer.java
    │   │   │   └── JsonEditorView.java
    │   │   └── utils/
    │   │       ├── DotEnv.java
    │   │       └── PdfTextExtractor.java
    │   └── resources/com/crud2gui/common/ui/
    │       ├── java-code.css
    │       └── json-editor.css
    └── test/java/com/crud2gui/common/utils/
        ├── DotEnvTest.java
        └── PdfTextExtractorTest.java
```

## ⚙️ La Clase `DotEnv`

La clase `DotEnv` ofrece un lector minimalista y eficiente de configuraciones basadas en clave-valor. Está diseñada con un constructor privado para impedir su instanciación y expone métodos estáticos orientados a facilitar la gestión centralizada de entornos.

### Características principales:
* **Soporte para comentarios y líneas vacías:** Omite automáticamente las líneas que comiencen por `#` o estén vacías.
* **Compatibilidad con prefijos `export`:** Limpia prefijos comunes de scripts de shell.
* **Eliminación de BOM:** Maneja correctamente archivos codificados en UTF-8 con marcas de orden de bytes (BOM).
* **Limpieza de comillas:** Remueve comillas dobles o simples envolventes en los valores.

## 🔄 Estrategia de Configuración: `env.public` y `env.private` en la Raíz

Para separar la configuración general o por defecto (versionada en el control de cambios) de los secretos o claves privadas locales, el sistema implementa un mecanismo de **carga secuencial y mezcla opcional** de múltiples ficheros de entorno ubicados de forma transversal en la **raíz del proyecto multi-módulo**.

### Mecánica de funcionamiento:
1. **Ubicación en la raíz:** Los ficheros se sitúan en el directorio raíz del proyecto (`TFG_CRUD2GUI/`), permitiendo que cualquier submódulo (como `statement-analyzer`) los comparta de forma unificada.
2. **Opcionalidad del fichero privado:** Si el fichero privado (`env.private`) no existe en el disco, el sistema no lanza ninguna excepción; simplemente omite su lectura de forma transparente.
3. **Orden de precedencia y sobreescritura:** Los ficheros se procesan en orden secuencial. Las claves definidas en los ficheros posteriores (**`env.private`**) **sobrescriben** a las de los anteriores (**`env.public`**).

* **`env.public`**: Contiene configuraciones genéricas (como URLs por defecto, timeouts, o variables compartidas). **Se versiona en Git**.
* **`env.private`**: Contiene credenciales personales o tokens de API (ej. `GEMINI_API_KEY`). **No se versiona** (debe añadirse al `.gitignore`).

---

## 💻 Ejemplos de Uso

### 1. Detección automática en la raíz (`loadProjectDefaults`)
El método recomendado para la aplicación. Detecta automáticamente si se está ejecutando desde la raíz o desde dentro de un submódulo (subiendo un nivel si es necesario) para cargar y fusionar `env.public` y `env.private`:

```java
import com.crud2gui.common.utils.DotEnv;
import java.io.IOException;
import java.util.Map;

// Carga automática buscando en la raíz del proyecto multi-módulo
Map<String, String> env = DotEnv.loadProjectDefaults();
String apiKey = env.get("GEMINI_API_KEY");
```

### 2. Carga en un directorio específico (`loadDefaultsIn`)
Permite especificar de forma explícita el directorio base donde se encuentran los ficheros:

```java
Map<String, String> env = DotEnv.loadDefaultsIn(Path.of("/ruta/al/directorio"));
```

### 3. Carga secuencial personalizada (`load`)
Puedes combinar cualquier cantidad de rutas de manera explícita:

```java
Map<String, String> env = DotEnv.load(
    Path.of("env.public"),
    Path.of("env.private.local")
);
```

---

## 🛠️ Instalación y Publicación Local

Al tratarse de un submódulo Maven independiente, para que el resto de submódulos puedan resolverlo como dependencia, debes instalarlo en tu repositorio local ejecutando el script provisto:

```bash
cd modules/common/script/
./install.sh
```

O bien mediante Maven directamente desde la raíz del módulo:

```bash
mvn clean install
```