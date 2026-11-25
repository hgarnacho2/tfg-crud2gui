# Módulo `model`

## Propósito

Este módulo define el metamodelo común de CRUD2GUI: la representación de los tipos Java de un proyecto, sus miembros y relaciones, junto con los resultados del análisis y la posición de los elementos en el diagrama. Es un contrato de datos inmutable, independiente del parser y de la interfaz gráfica, que comparten los módulos que generan, validan o muestran modelos.

La raíz es `ProjectModel`. `ModelIO` permite leer y escribir esa estructura en JSON usando un esquema versionado. `ProjectModelSchema` publica el JSON Schema canónico y permite validar respuestas estrictas del LLM antes de deserializarlas. La aplicación inserta el esquema en el prompt donde aparece `{{PROJECT_MODEL_JSON_SCHEMA}}`; si no hay marcador, lo añade al final. El extractor Java produce modelos con estas clases; el validador y la aplicación de escritorio los consumen.

## Contrato JSON y validación

El contrato canónico está en [project-model-v1.schema.json](src/main/resources/com/crud2gui/model/project-model-v1.schema.json). Describe el objeto raíz `ProjectModel`, sus propiedades obligatorias (`schemaVersion`, `expectedManagerId`, `source`, `types`, `relations`, `analysis` y `layout`), los tipos de datos y las restricciones de sus componentes. También define los objetos reutilizables del modelo y los valores permitidos para enumeraciones como `TypeKind`, `RelationKind`, `Visibility` y `Confidence`. Las propiedades no declaradas se rechazan durante la validación.

El esquema está escrito según **JSON Schema Draft 7**. La propiedad `$schema` indica al validador qué versión de la especificación debe interpretar; no es la versión del modelo CRUD2GUI. Esta última se declara por separado en `schemaVersion`, cuyo valor actual es `"1.0"`.

En la serialización, `ModelIO.toJson()` convierte el `ProjectModel` a JSON con Jackson y llama a `ProjectModelSchema.validate()` antes de devolver la cadena. El esquema se carga como Draft 7 y se reutiliza para validar. Si el JSON incumple el contrato, se lanza una `IOException`; `ModelIO.write()` y los llamadores solo pueden escribirlo si esa validación termina correctamente. Esta validación comprueba la estructura y las restricciones declaradas, no que la solución Java compile ni que las relaciones sean semánticamente correctas.

Al leer JSON mediante `ModelIO.read()` o `ModelIO.fromJson()`, se deserializa el modelo y se comprueba la compatibilidad de la versión mayor de `schemaVersion`. La deserialización ignora propiedades desconocidas; en cambio, la validación explícita con `ProjectModelSchema` rechaza propiedades no declaradas.

## Clases

### Modelo del proyecto y tipos

| Clase | Responsabilidad |
| --- | --- |
| `ProjectModel` | Raíz del modelo: versión del esquema, origen, tipos, relaciones, análisis y disposición del diagrama. |
| `TypeInfo` | Describe una clase, interfaz, enum, record o anotación, incluidos sus miembros y su ubicación en el código. |
| `TypeKind` | Clasifica las declaraciones Java representadas por `TypeInfo`. |
| `TypeRef` | Representa una referencia a tipo, con nombre, genéricos, pertenencia al proyecto y dimensiones de array. |
| `Visibility` | Visibilidad de un tipo o miembro, también utilizable para mostrar la notación UML. |
| `Modifier` | Modificadores Java distintos de la visibilidad, como `abstract`, `static` o `final`. |

### Miembros y documentación

| Clase | Responsabilidad |
| --- | --- |
| `FieldInfo` | Atributo de un tipo, con tipo, visibilidad, modificadores y metadatos. |
| `ConstructorInfo` | Constructor, sus parámetros, excepciones declaradas y metadatos. |
| `MethodInfo` | Método, con tipo de retorno, parámetros, modificadores y metadatos. |
| `ParameterInfo` | Parámetro de método o constructor, incluidos los parámetros varargs. |
| `EnumConstantInfo` | Constante de una enumeración. |
| `AnnotationInfo` | Anotación aplicada a un tipo o miembro. |
| `JavadocInfo` | Javadoc separado en descripción y etiquetas. |
| `Location` | Archivo y línea de origen de un elemento. |

### Relaciones y resultados del análisis

| Clase | Responsabilidad |
| --- | --- |
| `Relation` | Relación entre dos tipos, con multiplicidad, confianza y evidencia. |
| `RelationKind` | Clasifica relaciones UML: herencia, realización, composición, agregación o asociación. |
| `Evidence` | Hechos del código que respaldan la clasificación de una relación. |
| `Confidence` | Nivel de confianza de un resultado heurístico. |
| `Analysis` | Resultados calculados, como la clase gestora principal, candidatas alternativas y puntos de entrada. |
| `ManagerCandidate` | Candidata a clase gestora, con puntuación, confianza y motivos. |
| `SourceInfo` | Origen de la generación, número de archivos analizados y avisos. |

### Diagrama y JSON

| Clase | Responsabilidad |
| --- | --- |
| `Layout` | Posiciones de los tipos en el diagrama, conservadas aparte de los datos extraídos. |
| `Point` | Coordenadas horizontal y vertical de un elemento del diagrama. |
| `ModelIO` | Serializa y deserializa `ProjectModel` en JSON y comprueba la compatibilidad de la versión mayor del esquema. |
| `ProjectModelSchema` | Expone el JSON Schema Draft 7 de `ProjectModel` 1.0 y valida la estructura y los tipos de una respuesta JSON. |

Las colecciones del metamodelo se normalizan como colecciones inmutables. `ModelIO` incluye los campos nulos requeridos por el esquema e ignora propiedades desconocidas al leer modelos existentes. En cambio, `ProjectModelSchema` exige todas las propiedades del contrato y rechaza propiedades desconocidas al validar un JSON.

## Scripts

Los scripts se pueden ejecutar desde cualquier directorio y se sitúan en la raíz del módulo antes de invocar Maven:

* `script/install.sh`: ejecuta `mvn clean install`. Primero elimina los artefactos anteriores de
  `target`; después compila, ejecuta las pruebas, empaqueta el módulo e instala el JAR y su POM en el
  repositorio local de Maven para que otros módulos puedan usarlos.
* `script/test.sh`: ejecuta `mvn test`. Compila el código principal y el de pruebas y ejecuta las
  pruebas automatizadas del módulo. No limpia `target` ni instala el JAR en el repositorio local.
* `script/verify.sh`: ejecuta `mvn clean verify`. Limpia `target`, vuelve a compilar y probar el
  módulo y ejecuta las fases de verificación del ciclo Maven. Comprueba el módulo de forma más
  completa que `test.sh`, pero no instala el artefacto en el repositorio local.