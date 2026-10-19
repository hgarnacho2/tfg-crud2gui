# Java Model Extractor

Analiza fuentes Java y produce un `ProjectModel` JSON. La comparación con el modelo esperado del enunciado pertenece al módulo `model-validator`.

## Scripts

* `script/run.sh`: extrae el modelo de una carpeta Java.
* `script/test.sh`: ejecuta las pruebas de `model` y `java-model-extractor`.
* `script/verify.sh`: ejecuta `clean verify` para ambos modulos.
* `script/install.sh`: instala ambos modulos en el repositorio local de Maven.