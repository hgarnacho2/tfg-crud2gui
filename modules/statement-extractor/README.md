# Módulo `pdf`

Convierte el PDF de un enunciado de práctica en **texto plano** y lo **filtra** para quedarse solo con la parte útil (sin portada, índice, normas de entrega, etc.). El resultado está pensado para enviarse después a un LLM que extraiga las clases necesarias para resolver la práctica.

```
enunciado.pdf ──▶ enunciado.txt ──▶ enunciado-filtrado.txt ──▶ (análisis con LLM)
          PdfToTxtConverter      StatementFilter
```

## Requisitos

| Herramienta | Versión |
|---|---|
| Java | 17 o superior |
| Maven | 3.x |
| Bash | Git Bash en Windows (para el script `pdf-a-txt.sh`) |

Dependencias del módulo (se descargan solas la primera vez, hace falta internet):

- [Apache PDFBox](https://pdfbox.apache.org/) 3.0.3: extracción de texto.
- JUnit Jupiter 5.10.2: tests.

## Estructura

```
pdf/
├── pom.xml
├── scripts/
│   └── pdf-a-txt.sh                  # lanza la conversión + filtrado
├── output/                           # resultados generados (no versionar)
└── src/
    ├── main/
    │   ├── java/com/crud2gui/pdf/
    │   │   ├── Main.java             # punto de entrada: convierte y filtra
    │   │   ├── PdfToTxtConverter.java# PDF -> TXT
    │   │   ├── StatementFilter.java  # limpieza del enunciado
    │   │   └── package-info.java
    │   └── resources/
    └── test/
        ├── java/com/crud2gui/pdf/
        │   ├── PdfToTxtConverterTest.java
        │   └── StatementFilterTest.java
        └── resources/                # PDFs de ejemplo (enunciado_practica_23.pdf, ...)
```

## Uso rápido

Desde `pdf/scripts` (o desde cualquier carpeta, el script calcula sus propias rutas):

```bash
./pdf-a-txt.sh enunciado_practica_23
```

Genera en `pdf/output/enunciado_practica_23/`:

- `enunciado_practica_23.txt`: texto completo extraído del PDF.
- `enunciado_practica_23-filtrado.txt`: texto del enunciado ya filtrado.

### El script `pdf-a-txt.sh`

Procesa **un único PDF por ejecución**.

```bash
./pdf-a-txt.sh <pdf>
```

`<pdf>` puede ser:

| Argumento | Dónde se busca |
|---|---|
| Solo el nombre (`enunciado_practica_23` o `enunciado_practica_23.pdf`) | Tal cual en el directorio actual y, si no existe, en `src/test/resources` |
| Una ruta relativa o absoluta (`../x/enunciado.pdf`, `/c/Users/.../x.pdf`) | Exactamente esa ruta |
| Una ruta de Windows (`"C:/Users/.../x.pdf"`) | Se convierte automáticamente con `cygpath` |

Notas:

- Las rutas con espacios o con `\` van **entre comillas**.
- Si hay un fichero con el mismo nombre en el directorio actual, tiene prioridad sobre el de `src/test/resources`.
- La carpeta de salida es siempre `pdf/output/<nombre>/`. Se puede cambiar con la variable `PDF_OUTPUT_DIR`:

  ```bash
  PDF_OUTPUT_DIR=/c/mis_resultados ./pdf-a-txt.sh enunciado.pdf
  ```

- Internamente ejecuta `mvn compile exec:java` con la clase `com.crud2gui.pdf.Main`. El `compile` es necesario porque `exec:java` por sí solo no compila.

### Lanzar `Main` directamente

```bash
# Desde pdf/
mvn compile exec:java \
  -Dexec.mainClass=com.crud2gui.pdf.Main \
  -Dexec.args="ruta/enunciado.pdf"
```

Argumentos de `Main`:

| Argumentos | Resultado |
|---|---|
| `enunciado.pdf` | Genera `enunciado.txt` y `enunciado-filtrado.txt` junto al PDF |
| `enunciado.pdf completo.txt` | Genera `completo.txt` y `completo-filtrado.txt` |
| `enunciado.pdf completo.txt filtrado.txt` | Usa exactamente esos dos nombres |

Códigos de salida: `0` correcto, `1` error de lectura/escritura, `2` argumentos incorrectos.

## Clases

### `PdfToTxtConverter`

Extrae el texto de un PDF local con PDFBox (`PDFTextStripper`, ordenando por posición) y lo guarda como UTF-8.

```java
String texto = PdfToTxtConverter.extractText(Path.of("enunciado.pdf"));          // solo el String
PdfToTxtConverter.convert(Path.of("enunciado.pdf"));                             // -> enunciado.txt (junto al PDF)
PdfToTxtConverter.convert(Path.of("enunciado.pdf"), Path.of("out/enunciado.txt"));
```

Lanza `IOException` si el PDF no existe.

### `StatementFilter`

Limpia el texto de un enunciado. Reglas, en orden:

1. Descarta todo lo anterior a la línea `Introducción` (portada e índice).
2. Se detiene al llegar a `Fechas y normas de entrega` (admite numeración previa, como `5. Fechas...`).
3. Elimina las líneas que son solo un número (números de página).
4. Elimina las cabeceras repetidas `Fundamentos de Informática. Práctica Obligatoria...`.
5. Elimina la sección **Requisitos previos**, hasta el siguiente título numerado.
6. Colapsa las líneas en blanco: como máximo **2 seguidas** (`MAX_CONSECUTIVE_BLANK_LINES`) y ninguna al final.

Los marcadores se comparan **sin distinguir mayúsculas ni tildes** (`INTRODUCCIÓN`, `Introducción` e `introduccion` valen igual). Además normaliza el texto a Unicode NFC, corrige el `ı` + acento combinado que generan los PDF de LaTeX (`sı́` → `sí`) y devuelve los saltos de línea como `\n`.

```java
StatementFilter.Result r = StatementFilter.filter(texto);
r.text();                      // texto filtrado
r.structureRecognized();       // false si no se reconoció la estructura

StatementFilter.filterFile(Path.of("enunciado.txt"));                              // -> enunciado-filtrado.txt
StatementFilter.filterFile(Path.of("enunciado.txt"), Path.of("out/limpio.txt"));
```

**Si no reconoce la estructura** (no encuentra `INTRODUCCIÓN`) o el resultado queda vacío, devuelve el texto original sin cambios y `structureRecognized` es `false`. `Main` muestra un aviso en ese caso.

### `Main`

Encadena las dos fases: convierte el PDF y filtra el TXT resultante, mostrando por pantalla las rutas y los tamaños.

## Tests

Desde `pdf/`:

```bash
mvn test
mvn test -Dtest=StatementFilterTest
mvn test -Dtest=StatementFilterTest#collapsesConsecutiveBlankLinesToAtMostTwo
```

Los informes quedan en `target/surefire-reports/`.

| Test | Entrada |
|---|---|
| `PdfToTxtConverterTest` | PDF **sintético** generado por el propio test con PDFBox (en una carpeta temporal) |
| `StatementFilterTest` | Texto escrito dentro del test; uno escribe un TXT temporal |

Ningún test usa los PDFs de `src/test/resources`: esos están para pruebas manuales con el script.

## Usar el módulo desde otro módulo

```xml
<dependency>
    <groupId>com.crud2gui</groupId>
    <artifactId>pdf</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

(El `groupId` debe coincidir con el del `pom.xml` de `pdf`.) Para que Maven lo construya junto al resto, el `pom.xml` raíz debe incluir `<module>pdf</module>`.

## Limitaciones

- **Solo PDFs con texto.** Si el PDF es un escaneo (imágenes), PDFBox no extrae nada y haría falta OCR.
- **El filtro está pensado para los enunciados de Fundamentos de Informática (UNED).** Con otros formatos puede no reconocer la estructura y devolver el texto sin filtrar. Los marcadores están como constantes en `StatementFilter` (`START_MARKER`, `END_MARKER`, `SKIPPED_SECTION`). Los títulos de sección que se repiten como cabecera de página (por ejemplo `Etapa 1` o `Diseño de la práctica: Diagrama de clases`) no se eliminan.
- La extracción depende de cómo esté construido el PDF: el orden del texto, las tablas y las columnas pueden salir distintos a como se ven.

## Siguiente paso del flujo

El fichero `*-filtrado.txt` es la entrada del análisis con LLM (por ejemplo, el script `analizar-practica-gemini.sh`), que le pide al modelo las clases necesarias para resolver la práctica.

## Solución de problemas

| Síntoma | Causa probable |
|---|---|
| `ClassNotFoundException` / error al ejecutar `com.crud2gui.pdf.Main` | Las líneas `package` de los `.java` no coinciden con las carpetas, o quedan clases antiguas en `target/`. Revisa con `grep -rn "^package" src` y ejecuta `mvn clean` |
| `cannot find symbol` al compilar los tests | Algún test sigue con el paquete antiguo |
| TXT vacío o casi vacío | El PDF es un escaneo (sin texto) |
| Aviso "no se reconoció la estructura del enunciado" | El texto no tiene una línea que sea solo `Introducción` (en cualquier combinación de mayúsculas y tildes) |
| `mvn: command not found` | Maven no está instalado o no está en el `PATH` |
| `Permission denied` al lanzar el script | Ejecútalo con `bash scripts/pdf-a-txt.sh ...` |
