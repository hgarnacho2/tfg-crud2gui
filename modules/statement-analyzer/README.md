# statementAnalyzer

Clientes de modelos de lenguaje que, a partir del **enunciado de una practica** (TXT) y de un **prompt**,
devuelven un JSON con el modelo de clases. Ese JSON se compara mas adelante con el que el `parser`
extrae del codigo Java real.

Proveedores actuales: **Gemini** (remoto) y **Ollama** (local, p. ej. `qwen3:8b`).

El modelo esperado incluye `expectedManagerId` cuando el enunciado designa explícitamente una clase gestora; si no, el campo es `null`. Esta expectativa se mantiene separada de `analysis`, que corresponde a la detección del código.
El prompt de referencia `src/test/resources/prompts/prompt-v5.txt` incluye ese campo
tambien en su ejemplo JSON y exige que coincida con un id de `types` cuando no sea
`null`. El visor lo utiliza para resaltar la gestora y organizar el diagrama.
Tras actualizar el prompt, vuelve a seleccionarlo y genera de nuevo el modelo:
los JSON anteriores no se modifican automaticamente.

## Estructura

| Clase | Papel |
|---|---|
| `ModelClient` | Interfaz: `provider()`, `listModels()`, `generate(modelo, prompt, enunciado, opciones)` |
| `GeminiClient` | `GET /v1beta/models` (paginado) y `POST /v1beta/models/{modelo}:generateContent` |
| `OllamaClient` | `GET /api/tags` y `POST /api/chat` |
| `StatementAnalyzer` | Lee los dos ficheros, llama al cliente y exige un objeto JSON valido |
| `StatementAnalyzerMain` | Linea de comandos (`list` y `analyze`) |
| `ModelClients` | Crea el cliente a partir del nombre del proveedor |

Para anadir otro proveedor basta con implementar `ModelClient` y registrarlo en `ModelClients`.

## Configuracion

| Variable | Uso | Por defecto |
|---|---|---|
| `GEMINI_API_KEY` | Clave de Gemini (tambien se lee de un fichero `.env` del directorio actual) | obligatoria |
| `OLLAMA_BASE_URL` | URL base de Ollama, sin `/api/...` | `http://localhost:11434` |
| `OLLAMA_NUM_CTX` | Ventana de contexto en tokens | `32768` |
| `STATEMENT_ANALYZER_GEMINI_MODEL` | Modelo predeterminado para Gemini en el script, configurado en el `.env.public` raíz | `gemini-3.5-flash-lite` |
| `STATEMENT_ANALYZER_OLLAMA_MODEL` | Modelo predeterminado para Ollama en el script, configurado en el `.env.public` raíz | `qwen3:8b` |
| `STATEMENT_ANALYZER_GROQ_MODEL` | Modelo predeterminado para Groq en el script, configurado en el `.env.public` raíz | sin valor predeterminado |

Los valores no secretos del analizador se centralizan en el `.env.public` de la raíz del repositorio. Las claves API permanecen en `.env.private` o en variables exportadas del sistema.

La precedencia para elegir el modelo es:

1. Segundo argumento del script, si se proporciona.
2. Variable exportada en el entorno del proceso (`STATEMENT_ANALYZER_GEMINI_MODEL`, `STATEMENT_ANALYZER_OLLAMA_MODEL` o `STATEMENT_ANALYZER_GROQ_MODEL`). También se aceptan `GEMINI_MODEL`, `OLLAMA_MODEL` y `GROQ_MODEL` por compatibilidad.
3. Valor correspondiente en el `.env.public` raíz.
4. Valor predeterminado del script para Gemini u Ollama. Groq requiere un modelo explícito o `GROQ_MODEL`.

Si una variable exportada y la misma variable de `.env.public` tienen valores distintos, prevalece la exportada. Para cambiar el modelo habitualmente, edita el `.env.public` raíz; para una ejecución puntual, pasa el modelo como segundo argumento.

## Uso

Este modulo debe estar listado en el `pom.xml` raiz (`<module>statementAnalyzer</module>`).

```bash
cd statementAnalyzer

# modelos disponibles
mvn -q compile exec:java -Dexec.args="list gemini"
mvn -q compile exec:java -Dexec.args="list ollama"

# analizar un enunciado (la salida por defecto es output/<proveedor>-<modelo>.json)
mvn -q compile exec:java -Dexec.args="analyze gemini gemini-3.5-flash-lite src/test/resources/prompt.txt src/test/resources/statement.txt"
mvn -q compile exec:java -Dexec.args="analyze ollama qwen3:8b src/test/resources/prompt.txt src/test/resources/statement.txt"
```

El script `script/run-analyze-statement.sh` aplica la misma configuración de modelos y permite sobrescribirla con su segundo argumento:

```bash
./script/run-analyze-statement.sh gemini gemini-2.5-flash
./script/run-analyze-statement.sh ollama qwen3:8b
```

Sin una ruta de salida explicita, los resultados se guardan en `output/<proveedor>-<modelo>/`, por ejemplo
`output/gemini-gemini-3.5-flash-lite/`.

Opciones: `--temperature=0.1` y `--thinking=low|none` (`none` no envia `thinkingConfig`).

## Tests

```bash
mvn test                      # unitarios, sin red: servidor HTTP falso
mvn test -Dllm.tests=true     # ademas, contra Gemini y Ollama reales (consumen cuota / tiempo)
```

Las pruebas reales usan `src/test/resources/prompt.txt` y `statement.txt` y dejan el resultado en `output/`.
Eligen modelo con `GEMINI_MODEL` y `OLLAMA_MODEL`; si falta la clave, Ollama no responde o el modelo no
esta disponible, se omiten en vez de fallar.

## Detalles que conviene conocer

* **No hay que escapar nada a mano.** El escapado de `prompt_esc` / `statement_esc` de Postman lo hace Jackson.
* **El ":" de `modelo:generateContent`** se concatena en la URL; nunca se usa `URI.resolve`, que lo leeria como esquema.
  El identificador del modelo se valida (`gemini-x` o `models/gemini-x`) para que no pueda alterar la ruta.
* **`thinkingLevel`** es de la serie Gemini 3. Los Gemini 2.5 usan `thinkingBudget` y rechazan `thinkingLevel`
  con un 400: para ellos, `--thinking=none`.
* **Ollama recorta el prompt en silencio** si no cabe en el contexto. Por eso se fija `num_ctx`, y el resultado
  informa de los tokens de entrada para detectarlo. El prompt + enunciado de ejemplo son ~65.000 caracteres.
* **Reintentos**: 3 intentos con espera exponencial ante 429/500/502/503/504. No se reintentan 4xx ni timeouts.
* Si la respuesta no es un objeto JSON valido (o trae texto antes o despues) se guarda tal cual en
  `<salida>.invalid.txt` para no perder la llamada.
