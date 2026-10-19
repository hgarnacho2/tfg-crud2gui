# Módulo `statement-text-extractor`

Convierte el PDF de un enunciado de práctica en **texto plano** y lo **filtra** para quedarse solo con la parte útil (sin portada, índice, normas de entrega, etc.). El resultado está pensado para enviarse después a un LLM que extraiga las clases necesarias para resolver la práctica.

```
enunciado.pdf ──▶ enunciado.txt ──▶ enunciado-filtrado.txt ──▶ (análisis con LLM)
       StatementExtractor           StatementFilter
```

## Requisitos

| Herramienta | Versión |
|---|---|
| Java | 17 o superior |
| Maven | 3.x |
| Bash | Git Bash en Windows (para el script `run.sh`) |

Dependencias del módulo (se descargan solas la primera vez, hace falta internet):

* [Apache PDFBox](https://pdfbox.apache.org/) 3.0.3: extracción de texto.
* Jackson Databind 2.18.2: lectura y escritura de la configuración JSON del filtro.
* JUnit Jupiter 5.10.2: tests.
* `common-utils`: librería transversal para la gestión centralizada de configuración.

---

## ⚙️ Configuración y Entorno (`DotEnv`)

El módulo incorpora un gestor de configuración jerárquico a través de la clase `DotEnv` (provista por `common-utils`). Busca automáticamente los ficheros de entorno (**`.env.public`** y **`.env.private`**) subiendo de forma inteligente por los directorios padres hasta la **raíz del proyecto**, garantizando que funcione independientemente de la ruta desde donde se ejecute.

### Variables de configuración disponibles (en `.env.public`):
* **`FILTER_STATEMENT_TEXT`** (`true` / `false`): Activa o desactiva el paso de filtrado durante la ejecución de `Main`.
* **`STATEMENT_FILTER_CONFIG_PATH`**: Ruta al JSON de reglas. Por defecto apunta a `modules/statement-text-extractor/config/statement-filter.json`, relativa a la raíz del proyecto. Puede sustituirse con una variable de entorno o una propiedad Java `-DSTATEMENT_FILTER_CONFIG_PATH=...`.
* **`DOTENV_DEBUG`** (`true` / `false`): Activa o desactiva de forma parametrizada las trazas de depuración detalladas de la carga de configuración en la consola (también controlable mediante variable de entorno del S.O. o propiedad del sistema `-DDOTENV_DEBUG=true`).

`Main` carga el JSON indicado cuando el filtrado está activado. Para cambiar las reglas habituales basta con editar el fichero JSON; no es necesario modificar código Java.

---

## Estructura

```
statement-text-extractor/
├── pom.xml
├── script/
│   └── run.sh                      # lanza la conversión + filtrado
├── output/                         # resultados generados (no versionar)
└── src/
    ├── main/
    │   ├── java/com/crud2gui/statement/
    │   │   ├── extractor/          # contiene StatementExtractor
    │   │   ├── filter/             # contiene StatementFilter
    │   │   └── package-info.java
    │   └── resources/
    └── test/
        ├── java/com/crud2gui/statement/
        └── resources/              # PDFs de ejemplo (enunciado_practica_23.pdf, ...)
```

## Uso rápido

Desde `modules/statement-text-extractor/` (o ejecutando el script desde la ruta correspondiente):

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

Notas:
* Las rutas con espacios o con `\` van **entre comillas**.
* Para actualizar cambios entre módulos en Maven debes ejecutar siempre un `mvn clean install` desde la raíz del proyecto.

---

## Clases principales

### `StatementExtractor`
Clase principal de entrada encargada de coordinar la lectura del PDF local utilizando PDFBox y de orquestar el proceso de guardado en texto plano.

### `StatementFilter`
Filtra el texto con reglas de sección configurables. Los títulos se comparan sin distinguir mayúsculas, tildes ni numeración inicial. Debe encontrarse una regla `START` y otra `STOP`; si falta alguna, el texto original se devuelve con `structureRecognized()` en `false`.

```java
StatementFilter.Result r = StatementFilter.filter(texto);
r.text();                    // texto filtrado
r.structureRecognized();     // false si no se reconoció la estructura
```

### `StatementFilterConfig`
Modelo de configuración independiente de la interfaz. Cada regla asocia un título de sección con una acción:

* `START`: comienza el cuerpo que se filtrará e incluye ese título.
* `INCLUDE`: incluye ese título y su sección.
* `IGNORE`: omite esa sección hasta el siguiente encabezado reconocido.
* `STOP`: termina el cuerpo filtrado sin incluir ese título.

Las secciones sin regla explícita se incluyen por defecto. `StatementFilterConfig.defaults()` proporciona las reglas usadas para los ejemplos UNED.

### Configuración JSON
El fichero predeterminado se encuentra en `config/statement-filter.json`. Sus reglas asocian un título con una acción; los títulos se comparan sin distinguir mayúsculas, tildes ni numeración inicial. Ejemplo:

```json
{
    "sectionRules": [
        { "heading": "Introducción", "action": "START" },
        { "heading": "Requisitos previos", "action": "IGNORE" },
        { "heading": "Fechas y normas de entrega", "action": "STOP" }
    ],
    "maxConsecutiveBlankLines": 2,
    "removePageNumbers": true
}
```

`Main` carga el JSON automáticamente al ejecutar `run.sh` si `FILTER_STATEMENT_TEXT=true`. Desde otro componente Java también se puede cargar la configuración explícitamente:

```java
Path configPath = Path.of("statement-filter.json");
StatementFilterConfig config = StatementFilterConfigJson.read(configPath);
StatementFilter.Result result = StatementFilter.filterFile(input, output, config);
```

Para guardar cambios desde otro componente Java, se puede llamar a `StatementFilterConfigJson.write(config, configPath)`. La interfaz gráfica futura podrá editar este mismo modelo y persistirlo con esa utilidad.

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
    <artifactId>statement-text-extractor</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```