# Model Validator

Compara un modelo esperado derivado del enunciado con el modelo extraido de la implementacion Java. Depende del esquema comun `crud2gui-model`, no del extractor ni del cliente LLM.

Desde la raiz del repositorio:

```bash
modules/model-validator/script/compare.sh \
  modules/statement-analyzer/output/gemini-gemini-3.5-flash-lite/model-esperado.json \
  modules/java-model-extractor/output/model_yyyyMMdd_HHmmss.json
```

El primer archivo es el modelo esperado; el segundo, el modelo de la solucion Java. La comparacion ignora lanzadores, dependencias de uso y metadatos de ejecucion; permite miembros adicionales y reporta la ambiguedad composicion/agregacion como advertencia. Devuelve `VALID` si no hay incumplimientos bloqueantes.