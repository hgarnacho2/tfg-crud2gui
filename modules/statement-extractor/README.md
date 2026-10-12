# Módulo `statement-extractor`

Convierte el PDF de un enunciado de práctica en **texto plano** y lo **filtra** para quedarse solo con la parte útil (sin portada, índice, normas de entrega, etc.). El resultado está pensado para enviarse después a un LLM que extraiga las clases necesarias para resolver la práctica.

```
enunciado.pdf ──▶ enunciado.txt ──▶ enunciado-filtrado.txt ──▶ (análisis con LLM)
       PdfToTxtConverter          StatementFilter
```

## Requisitos

| Herramienta | Versión |
|---|---|
| Java | 17 o superior |
| Maven | 3.x |
| Bash | Git Bash en Windows (para el script `run.sh`) |

Dependencias del módulo (se descargan solas la primera vez, hace falta internet):

* [Apache PDFBox](https://pdfbox.apache.org/) 3.0.3: extracción de texto.
* JUnit Jupiter 5.10.2: tests.

---

## ⚙️ Configuración y Entorno (`DotEnv`)

El módulo incorpora un gestor de configuración jerárquico a través de la clase `DotEnv`. Busca automáticamente los ficheros de entorno (**`.env.public`** y **`.env.private`**) subiendo de forma inteligente por los directorios padres hasta la **raíz del proyecto**, garantizando que funcione independientemente de la ruta desde donde se ejecute.

### Variables de configuración disponibles (en `.env.public`):
* **`FILTER_STATEMENT_TEXT`** (`true` / `false`): Activa o desactiva el filtrado automático de texto innecesario durante la extracción del enunciado.
* **`DOTENV_DEBUG`** (`true` / `false`): Activa o desactiva de forma parametrizada las trazas de depuración detalladas de la carga de configuración en la consola (también controlable mediante variable de entorno del S.O. o propiedad del sistema `-DDOTENV_DEBUG=true`).

---

## Estructura

```
statement-extractor/
├── pom.xml
├── script/
│   └── run.sh                      # lanza la conversión + filtrado
├── output/                         # resultados generados (no versionar)
└── src/
    ├── main/
    │   ├── java/com/crud2gui/statement/
    │   │   ├── extractor/          # punto de entrada y lógica de extracción
    │   │   ├── filter/             # limpieza del enunciado
    │   │   └── package-info.java
    │   └── resources/
    └── test/
        ├── java/com/crud2gui/statement/
        └── resources/              # PDFs de ejemplo (enunciado_practica_23.pdf, ...)
```

## Uso rápido

Desde `modules/statement-extractor/` (o ejecutando el script desde la ruta correspondiente):

```bash
./script/run.sh "ruta/al/enunciado_practica_23.pdf"
```

Genera en `output/enunciado_practica_23/`:

* `enunciado_practica_23.txt`: texto completo extraído del PDF.
* `enunciado_practica_23-filtrado.txt`: texto del enunciado ya filtrado (según la configuración de `.env.public`).

### El script `run.sh`

Procesa **un único PDF por ejecución**.

```bash
./script/run.sh <pdf>
```

`<pdf>` puede ser:

| Argumento | Dónde se busca |
|---|---|
| Solo el nombre (`enunciado_practica_23` o `enunciado_practica_23.pdf`) | Tal cual en el directorio actual y, si no existe, en `src/test/resources` |
| Una ruta relativa o absoluta (`../x/enunciado.pdf`, `/c/Users/.../x.pdf`) | Exactamente esa ruta |
| Una ruta de Windows (`"C:/Users/.../x.pdf"`) | Se convierte automáticamente con `cygpath` |

Notas:
* Las rutas con espacios o con `\` van **entre comillas**.
* Para actualizar cambios entre módulos en Maven debes ejecutar siempre un `mvn clean install` desde la raíz del proyecto.

---

## Clases principales

### `PdfToTxtConverter`
Extrae el texto de un PDF local con PDFBox (`PDFTextStripper`, ordenando por posición) y lo guarda como UTF-8.

```java
String texto = PdfToTxtConverter.extractText(Path.of("enunciado.pdf"));           // solo el String
PdfToTxtConverter.convert(Path.of("enunciado.pdf"));                            // -> enunciado.txt (junto al PDF)
PdfToTxtConverter.convert(Path.of("enunciado.pdf"), Path.of("out/enunciado.txt"));
```

### `StatementFilter`
Limpia el texto de un enunciado descartando portadas, índices, requisitos previos e introduciendo límites por secciones, normalizando caracteres especiales a Unicode NFC.

```java
StatementFilter.Result r = StatementFilter.filter(texto);
r.text();                    // texto filtrado
r.structureRecognized();     // false si no se reconoció la estructura
```

---

## Tests

Desde el módulo o la raíz:

```bash
mvn test
```

Los informes quedan en `target/surefire-reports/`.

---

## Usar el módulo desde otro módulo

```xml
<dependency>
    <groupId>com.crud2gui</groupId>
    <artifactId>statement-extractor</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```