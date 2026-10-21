#!/usr/bin/env bash
set -e

# Resolver la raíz del módulo y del repositorio.
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd -- "$PROJECT_DIR/../.." && pwd)"
cd "$PROJECT_DIR"

# Las variables ya exportadas prevalecen sobre los valores del .env.public raíz.
PROCESS_GEMINI_MODEL="${STATEMENT_ANALYZER_GEMINI_MODEL:-${GEMINI_MODEL:-}}"
PROCESS_OLLAMA_MODEL="${STATEMENT_ANALYZER_OLLAMA_MODEL:-${OLLAMA_MODEL:-}}"
PROCESS_GROQ_MODEL="${STATEMENT_ANALYZER_GROQ_MODEL:-${GROQ_MODEL:-}}"

if [ -f "$REPO_ROOT/.env.public" ]; then
    while IFS= read -r line || [ -n "$line" ]; do
        line=${line%$'\r'}
        case "$line" in
            STATEMENT_ANALYZER_GEMINI_MODEL=*|STATEMENT_ANALYZER_OLLAMA_MODEL=*|STATEMENT_ANALYZER_GROQ_MODEL=*)
                key=${line%%=*}
                value=${line#*=}
                case "$value" in
                    \"*\") value=${value#\"}; value=${value%\"} ;;
                    \'*\') value=${value#\'}; value=${value%\'} ;;
                esac
                printf -v "$key" '%s' "$value"
                export "$key"
                ;;
        esac
    done < "$REPO_ROOT/.env.public"
fi

if [ -n "$PROCESS_GEMINI_MODEL" ]; then
    STATEMENT_ANALYZER_GEMINI_MODEL="$PROCESS_GEMINI_MODEL"
fi
if [ -n "$PROCESS_OLLAMA_MODEL" ]; then
    STATEMENT_ANALYZER_OLLAMA_MODEL="$PROCESS_OLLAMA_MODEL"
fi
if [ -n "$PROCESS_GROQ_MODEL" ]; then
    STATEMENT_ANALYZER_GROQ_MODEL="$PROCESS_GROQ_MODEL"
fi

# Gestión de argumentos con mensajes informativos para valores por defecto
if [ -z "${1:-}" ]; then
    echo "No se ha indicado proveedor, se usará 'gemini' por defecto."
    PROVIDER="gemini"
else
    PROVIDER="$1"
fi

PROVIDER_KEY="$(printf '%s' "$PROVIDER" | tr '[:upper:]' '[:lower:]')"
if [ -n "${2:-}" ]; then
    MODEL="$2"
    echo "Se usará el modelo indicado como parámetro: '$MODEL'."
else
    case "$PROVIDER_KEY" in
        gemini)
            MODEL_ENV_NAME="STATEMENT_ANALYZER_GEMINI_MODEL"
            CONFIGURED_MODEL="${STATEMENT_ANALYZER_GEMINI_MODEL:-}"
            DEFAULT_MODEL="gemini-3.5-flash-lite"
            ;;
        ollama)
            MODEL_ENV_NAME="STATEMENT_ANALYZER_OLLAMA_MODEL"
            CONFIGURED_MODEL="${STATEMENT_ANALYZER_OLLAMA_MODEL:-}"
            DEFAULT_MODEL="qwen3:8b"
            ;;
        groq)
            MODEL_ENV_NAME="STATEMENT_ANALYZER_GROQ_MODEL"
            CONFIGURED_MODEL="${STATEMENT_ANALYZER_GROQ_MODEL:-}"
            DEFAULT_MODEL=""
            ;;
        *)
            echo "Proveedor no reconocido: '$PROVIDER'. Use gemini, ollama o groq." >&2
            exit 2
            ;;
    esac

    if [ -n "$CONFIGURED_MODEL" ]; then
        MODEL="$CONFIGURED_MODEL"
        echo "No se ha indicado modelo; se usará '$MODEL' desde $MODEL_ENV_NAME."
    elif [ -n "$DEFAULT_MODEL" ]; then
        MODEL="$DEFAULT_MODEL"
        echo "No se ha indicado modelo ni $MODEL_ENV_NAME; se usará '$MODEL' por defecto."
    else
        echo "No se ha indicado modelo; define $MODEL_ENV_NAME en el .env.public raíz o pásalo como segundo parámetro." >&2
        exit 2
    fi
fi

if [ -z "${3:-}" ]; then
    echo "No se ha indicado prompt, se usará 'src/test/resources/prompts/prompt-v4.txt' por defecto."
    PROMPT="src/test/resources/prompts/prompt-v4.txt"
else
    PROMPT="$3"
fi

if [ -z "${4:-}" ]; then
    echo "No se ha indicado enunciado, se usará 'src/test/resources/statements/enunciado_practica_23/enunciado_practica_23-filtrado.txt' por defecto."
    STATEMENT="src/test/resources/statements/enunciado_practica_23/enunciado_practica_23-filtrado.txt"
else
    STATEMENT="$4"
fi

OUTPUT="${5:-}"

echo "=== Analizando enunciado con $PROVIDER ($MODEL) ==="

if [ -n "$OUTPUT" ]; then
    mvn -q compile exec:java -Dexec.args="analyze $PROVIDER $MODEL $PROMPT $STATEMENT $OUTPUT"
else
    mvn -q compile exec:java -Dexec.args="analyze $PROVIDER $MODEL $PROMPT $STATEMENT"
fi

echo "=== Análisis completado con éxito ==="