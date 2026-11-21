# Java Model Extractor

Analiza fuentes Java y produce un `ProjectModel` JSON. La comparación con el modelo esperado del enunciado pertenece al módulo `model-validator`.

## JavaParser y el arbol de sintaxis abstracta (AST)

**JavaParser** es una biblioteca que lee codigo fuente Java y lo transforma en objetos que
representan su estructura sintactica. Se incorpora mediante la dependencia Maven
`com.github.javaparser:javaparser-core`, declarada en el [pom.xml](./pom.xml).
El paquete `com.github.javaparser` lo definen los autores de la biblioteca: no es codigo propio
de CRUD2GUI ni implica conectarse a GitHub o enviar alli los fuentes.

El resultado del analisis es un **AST** (*Abstract Syntax Tree*, arbol de sintaxis abstracta).
Es una representacion jerarquica del programa: un fichero contiene declaraciones de tipos, una
clase contiene atributos y metodos, y el cuerpo de un metodo contiene sentencias y expresiones.
Permite consultar esos elementos como objetos, en vez de buscar patrones en el texto.

Algunos nodos de JavaParser utilizados en este modulo son:

| Tipo de nodo | Elemento representado |
| --- | --- |
| `CompilationUnit` | Unidad de compilacion de un fichero, con su paquete, imports y tipos. |
| `ClassOrInterfaceDeclaration` | Declaracion de una clase o interfaz. |
| `FieldDeclaration` / `VariableDeclarator` | Declaracion de atributos y cada variable declarada en ella. |
| `MethodDeclaration` / `ConstructorDeclaration` | Metodo o constructor, con sus parametros y cuerpo cuando existe. |
| `EnumDeclaration` / `RecordDeclaration` | Declaracion de un enum o un record. |

`Node` es la clase base de los nodos. Las interfaces `NodeWithAnnotations`, `NodeWithJavadoc`
y `NodeWithModifiers` permiten consultar propiedades comunes de distintos nodos.

En CRUD2GUI, [ProjectParser](./src/main/java/com/crud2gui/javamodelextractor/ProjectParser.java)
configura JavaParser para Java 21 y UTF-8, obtiene los AST e indexa los tipos antes de extraer sus
miembros. El AST es una estructura de la biblioteca; `ProjectModel` es nuestro modelo de intercambio,
construido a partir de ella y serializable a JSON. No se guarda el AST completo en ese JSON.

Este proceso es **analisis estatico**: no ejecuta las clases analizadas, no es un LLM y no sustituye
al compilador ni demuestra que el programa funcione correctamente.

## Validacion del JSON del modelo

`ModelIO.toJson` valida el JSON serializado con el contrato de
`ProjectModelSchema`. `ModelIO.write` utiliza esa misma serializacion antes de escribir el fichero.
Por tanto, tanto la salida de linea de comandos como la extraccion de codigo desde el escritorio
se validan antes de guardar o devolver el JSON.

La respuesta JSON del LLM tambien se valida con `ProjectModelSchema` antes de deserializarla. Para
cumplir las propiedades requeridas por el esquema, aunque sean nullable, la serializacion incluye
los valores `null` en vez de omitirlos.

La validacion comprueba la estructura, los tipos JSON y los valores enumerados; no determina si el
analisis es semanticamente correcto ni compara el resultado con el modelo esperado. Esa comparacion
corresponde a `model-validator`. Las operaciones de lectura `ModelIO.read` y `ModelIO.fromJson`
mantienen la comprobacion de compatibilidad de la version mayor, pero no realizan por si solas la
validacion completa del esquema.

## Paquetes y clases

El codigo de produccion de `src/main/java` se organiza en un unico paquete, sin subpaquetes:
`com.crud2gui.javamodelextractor`.

* [Main](./src/main/java/com/crud2gui/javamodelextractor/Main.java): entrada de linea de comandos.
* [ProjectParser](./src/main/java/com/crud2gui/javamodelextractor/ProjectParser.java): orquesta el analisis del proyecto.
* [FolderScanner](./src/main/java/com/crud2gui/javamodelextractor/FolderScanner.java): localiza los ficheros Java que se analizaran.
* [FileTypeExtractor](./src/main/java/com/crud2gui/javamodelextractor/FileTypeExtractor.java): extrae los tipos y miembros declarados en cada fichero.
* [TypeIndex](./src/main/java/com/crud2gui/javamodelextractor/TypeIndex.java): registra los tipos declarados para buscarlos por nombre.
* [ResolutionContext](./src/main/java/com/crud2gui/javamodelextractor/ResolutionContext.java): guarda el paquete, imports y tipos contenedores necesarios para resolver nombres.
* [TypeResolver](./src/main/java/com/crud2gui/javamodelextractor/TypeResolver.java): resuelve referencias de tipos y las convierte al metamodelo.
* [RelationAnalyzer](./src/main/java/com/crud2gui/javamodelextractor/RelationAnalyzer.java): deriva relaciones UML de las declaraciones y usos.
* [ManagerDetector](./src/main/java/com/crud2gui/javamodelextractor/ManagerDetector.java): puntua candidatas y detecta la clase gestora.
* [ManagerDetectorConfig](./src/main/java/com/crud2gui/javamodelextractor/ManagerDetectorConfig.java): carga la configuracion del detector de clase gestora.

### FolderScanner

`FolderScanner` busca recursivamente ficheros con extension `.java` bajo una carpeta raiz. No requiere
una estructura de directorios concreta para encontrarlos. Devuelve las rutas ordenadas, de forma que
el resultado sea determinista.

* `findJavaFiles(root)` comprueba que la ruta exista como carpeta; si no, lanza `IOException`.
* Omite las carpetas ocultas (nombres que empiezan por `.`) y, por defecto, las llamadas `target`,
  `build`, `out`, `bin` y `node_modules`. El constructor que recibe un `Set<String>` permite
  sustituir esa lista por exclusiones personalizadas; las carpetas ocultas se omiten siempre.
* Omite `module-info.java` y `package-info.java`, ademas de cualquier fichero cuyo nombre no termine
  exactamente en `.java`.
* Si no puede visitar un fichero o una carpeta, continua el recorrido y omite esa ruta.
* `DEFAULT_EXCLUDED_FOLDER_NAMES` y `DEFAULT_EXCLUDED_FILE_NAMES` exponen las listas
  predeterminadas. `DEFAULT_EXCLUDED` se conserva como alias obsoleto de la lista de carpetas.

### FileTypeExtractor

[FileTypeExtractor](./src/main/java/com/crud2gui/javamodelextractor/FileTypeExtractor.java)
convierte las declaraciones del AST en los datos del modelo de CRUD2GUI. Recibe una declaracion
`TypeDeclaration<?>`, el contexto del fichero y un `TypeResolver`; no lee ni parsea el fuente
por su cuenta, ya que esa fase corresponde a `ProjectParser`.

* Reconoce clases, interfaces, enums, records y tipos de anotacion.
* Construye un `TypeInfo` con nombre, identificador, visibilidad, modificadores, superclase,
  interfaces y parametros genericos.
* Convierte atributos, constructores y metodos en `FieldInfo`, `ConstructorInfo` y `MethodInfo`,
  incluyendo tipos, parametros, excepciones declaradas y presencia de cuerpo.
* Conserva anotaciones, Javadoc y ubicaciones en el fuente, cuando estan disponibles.
* Aplica reglas implicitas de Java, como los campos `public static final` de las interfaces,
  y recorre los tipos anidados.
* Delega la resolucion de referencias de tipos en `TypeResolver`. Las relaciones UML se derivan
  despues en `RelationAnalyzer`, no en esta clase.

Por ejemplo, de un atributo `private String nombre` obtiene su nombre, tipo y visibilidad;
de `public String getNombre()` obtiene la firma y si tiene cuerpo, pero no ejecuta el getter.
La [memoria del proyecto](../../doc/PFG-CRUD2GUI-Memoria.md#721-localización-lectura-e-indexación-de-los-tipos)
incluye el ejemplo completo de `Persona` y una representacion simplificada de su AST.

## Deteccion de la clase gestora

El recurso `src/main/resources/manager-analysis.json` configura la heuristica que puntua y ordena
las posibles clases gestoras. No determina que tipos se extraen: ajusta como se elige la candidata
principal entre las clases y records elegibles.

Solo pueden competir clases o records concretos que tengan atributos tipados con tipos del proyecto
y una relacion con alguno de ellos. Se excluyen tipos abstractos, interfaces, enums, clases con `main`,
clases de prueba y utilidades estaticas.

La puntuacion es una suma ponderada, no una probabilidad:

```text
score =
    reachability * fraccion_de_tipos_del_dominio_alcanzables
  + root * indicador_de_raiz
  + collections * min(numero_de_colecciones_de_dominio, 2)
  + collectionUsage * min(numero_de_colecciones_de_dominio_usadas, 2)
  + entryPoint * indicador_de_instanciacion_desde_main_o_prueba
```

La seleccion se basa en indicios estructurales; los nombres de clases y metodos no afectan a la
puntuacion.

### `weights`

Cada valor multiplica un indicio de la candidata:

| Clave | Valor predeterminado | Significado |
| --- | ---: | --- |
| `reachability` | `10.0` | Multiplica la fraccion de otros tipos concretos del dominio que son alcanzables desde la candidata mediante relaciones entre atributos. Las relaciones de implementacion permiten que una referencia a una interfaz alcance sus implementaciones; parametros y variables locales no cuentan. |
| `root` | `7.0` | Se suma si la candidata pertenece a un componente raiz del grafo de atributos: ningun otro tipo la contiene como atributo. Los ciclos se consideran como un componente. |
| `collections` | `4.0` | Se multiplica por el numero de atributos no estaticos que son colecciones o arrays con tipos del proyecto, hasta un maximo de dos atributos. |
| `collectionUsage` | `3.0` | Se multiplica por el numero de esas colecciones que los metodos de la candidata usan, hasta un maximo de dos. Se reconocen operaciones conocidas de coleccion, iteraciones `for-each` y alias locales directos del campo. |
| `entryPoint` | `1.0` | Se suma una vez si la candidata se instancia directamente con `new` desde un metodo `main` o de prueba. |

### `confidence`

Estos valores clasifican la confianza de cada candidata a partir de su puntuacion y de la distancia
con las candidatas vecinas en el orden de puntuacion:

| Clave | Valor predeterminado | Significado |
| --- | ---: | --- |
| `highGap` | `4.0` | Diferencia minima con la candidata vecina para obtener confianza alta. Ademas, la puntuacion debe alcanzar `highMinimumScore`. |
| `mediumGap` | `1.5` | Diferencia minima para obtener confianza media. Si solo hay una candidata, su puntuacion se compara con este umbral. |
| `highMinimumScore` | `8.0` | Puntuacion minima que debe tener una candidata para obtener confianza alta. |

En concreto, la confianza es alta cuando la diferencia requerida es al menos `highGap` y la
puntuacion es al menos `highMinimumScore`; es media cuando la diferencia alcanza `mediumGap`
(o, si solo hay una candidata, su puntuacion alcanza ese umbral); en los demas casos es baja.
Con una sola candidata, la diferencia se considera igual a su propia puntuacion.

### Configuracion alternativa

Por defecto se carga `manager-analysis.json` desde los recursos del modulo. Se puede indicar otro
fichero con `--config=ruta/al/archivo.json`. La configuracion externa se combina recursivamente con
la predeterminada: las claves omitidas conservan el valor predeterminado y los valores individuales
proporcionados reemplazan los predeterminados. Un `--manager=id` valido selecciona
manualmente la clase gestora y prevalece sobre la seleccion heuristica.

## Scripts

* `script/run.sh`: extrae el modelo de una carpeta Java. Por defecto no instala dependencias, que deben estar disponibles en el repositorio local de Maven; añade `--install` para instalarlas antes del análisis.
* `script/test.sh`: ejecuta las pruebas de `model` y `java-model-extractor`.
* `script/verify.sh`: ejecuta `clean verify` para ambos modulos.
* `script/install.sh`: instala ambos modulos en el repositorio local de Maven.