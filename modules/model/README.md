# Módulo `model`

## Propósito

Este módulo define el metamodelo común de CRUD2GUI: la representación de los tipos Java de un proyecto, sus miembros y relaciones, junto con los resultados del análisis y la posición de los elementos en el diagrama. Es un contrato de datos inmutable, independiente del parser y de la interfaz gráfica, que comparten los módulos que generan, validan o muestran modelos.

La raíz es `ProjectModel`. `ModelIO` permite leer y escribir esa estructura en JSON usando un esquema versionado. El extractor Java produce modelos con estas clases; el validador y la aplicación de escritorio los consumen.

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
| `RelationKind` | Clasifica relaciones UML: herencia, realización, composición, agregación, asociación o dependencia. |
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

Las colecciones del metamodelo se normalizan como colecciones inmutables. Al guardar JSON se omiten los valores nulos; al leerlo se ignoran propiedades desconocidas, siempre que la versión mayor del esquema sea compatible.