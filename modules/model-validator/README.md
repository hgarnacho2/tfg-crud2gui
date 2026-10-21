# Model Validator

Compara un modelo esperado derivado del enunciado con el modelo extraido de la implementacion Java. Depende del esquema comun `crud2gui-model`, no del extractor ni del cliente LLM.

Desde la raiz del repositorio:

```bash
modules/model-validator/script/run.sh \
  modules/statement-analyzer/output/gemini-gemini-3.5-flash-lite/model-esperado.json \
  modules/java-model-extractor/output/model_yyyyMMdd_HHmmss.json
```

En cada ejecución, el script y `ModelComparisonMain` leen `modules/model-validator/conf/validation-options.json`; así la selección persiste sin repetir opciones en la línea de comandos. El archivo actual valida las declaraciones de clases y omite interfaces, enums, modificadores abstractos, relaciones, atributos, constructores, métodos y operaciones. Para una excepción puntual se puede pasar `--config=fichero.json`. La configuración es parcial: las claves omitidas permanecen activadas. El ejemplo `validation-options.example.json` muestra otra combinación:

```json
{
  "types": {
    "classes": true,
    "interfaces": true,
    "enums": false
  },
  "relationships": true,
  "attributes": false
}
```

Las opciones disponibles son `types.classes`, `types.interfaces`, `types.enums`, `types.records`, `types.annotations`, `types.abstractness`, `relationships`, `attributes`, `constructors`, `methods`, `enumConstants` y `collectionOperations`. `types.abstractness` controla solo la comprobación del modificador `ABSTRACT`; `types.classes` controla la presencia, tipo, visibilidad y modificadores de las clases. La configuración efectiva queda registrada en `validationOptions` dentro del informe.

El primer archivo es el modelo esperado; el segundo, el modelo de la solucion Java. Las clases del modelo extraido que contienen un metodo `main` se identifican con el hallazgo informativo `LAUNCHER_EXCLUDED` y se excluyen de la validacion de dominio y del CRUD; si tambien aparecen en el esperado, no producen un falso `MISSING_CLASS`. Se ignoran asimismo dependencias de uso y metadatos de ejecucion; las clases y atributos de dominio no contemplados en el modelo esperado invalidan la comparacion. Las discrepancias entre `ASSOCIATION`, `AGGREGATION` y `COMPOSITION` se reportan como decisiones pendientes (`PENDING_CONFIRMATION`), no como errores estructurales. Otros desacuerdos de tipo de relacion, como herencia frente a asociacion, siguen invalidando la comparacion. `VALID` indica que no hay incumplimientos estructurales.

Al comprobar métodos requeridos, también se consideran los métodos heredados de superclases e interfaces cuando son accesibles desde el tipo.

Los getters y setters públicos que coinciden en nombre y tipo con un atributo se reconocen como accesores implícitos y no se marcan como métodos adicionales. Un atributo privado esperado sin getter público genera una advertencia `MISSING_GETTER`.

Para cada atributo privado esperado de tipo colección (`List`, `Set`, `Collection` y equivalentes), el validador requiere operaciones públicas de alta y baja. El método de alta debe aceptar un elemento de la colección; el de baja puede aceptar el elemento o un identificador como `id` o `email`. Para colecciones no privadas, `source.warnings` puede indicar operaciones con `OPERACIONES_LISTA: Clase.atributo(add,remove)`. No se valida el estado por defecto.

El informe incluye `managerSelection`, separado de `result` y `valid`: compara el `expectedManagerId` del enunciado con la candidata detectada, y muestra alternativas, confianza, ambigüedades, override manual y `requiresConfirmation`. Si el enunciado es ambiguo, el analizador deja `expectedManagerId` en `null` y añade `AMBIGUO: Gestora: ...`; el informe usa `AMBIGUOUS_EXPECTATION` y solicita confirmación. Estas discrepancias no invalidan el contrato estructural; una elección manual se proporciona durante la extracción con `--manager=id`.

Además de mostrar el resultado y los hallazgos en consola, el script guarda un informe por ejecución en `modules/model-validator/output/validation_<modelo>_<fecha>_<hora>.json`; conserva los informes anteriores y añade un sufijo si ya existe un informe con el mismo nombre. El nombre del modelo se obtiene del fichero esperado. El informe incluye las rutas de entrada, `valid` (validez estructural), `readyForCrud`, `relationshipDecisions`, `crudConfirmations`, la selección de gestora, el recuento por severidad y todos los hallazgos. `crudConfirmations` enumera los elementos que bloquean el CRUD y cuándo resolverlos: las relaciones (`RELATIONSHIP_KIND`) se determinan durante `CRUD_GENERATION`; la selección de gestora, si es ambigua o no fiable, se revisa en `BEFORE_CRUD_GENERATION`. `readyForCrud` solo es `true` cuando la estructura es válida y no quedan confirmaciones pendientes. Al ejecutar directamente `ModelComparisonMain`, se puede indicar una ruta de informe como tercer argumento; si se omite, aplica el mismo patrón en `output/` del directorio actual.

Los elementos opcionales deben aparecer en el modelo esperado y marcarse en `source.warnings`: `OPCIONAL: Clase`, `OPCIONAL: Clase.atributo` o `OPCIONAL: Clase.metodo(Tipo)`. Si están declarados como opcionales, pueden faltar en la implementación; si aparecen, se validan normalmente. Para un campo cuyo concepto y tipo son obligatorios pero cuyo nombre no se fija, se puede añadir `NOMBRE_ATRIBUTO_NO_ESPECIFICADO: Clase.nombreCanonico`; el validador acepta un único campo compatible con otro nombre y lo informa como `INFO`, sin relajar la comprobación del tipo ni de sus accesores.

Por defecto no instala dependencias; usa `--install` para instalarlas antes de comparar.