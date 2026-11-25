package com.crud2gui.javamodelextractor;

import java.nio.file.Files;
import java.nio.file.Path;

import com.crud2gui.common.env.DotEnv;
import com.crud2gui.common.filesystem.OutputDirectoryResolver;

/**
 * Calcula la carpeta y el nombre predeterminados para guardar modelos extraidos de codigo Java.
 *
 * <p>La linea de comandos y la interfaz de escritorio comparten esta clase para proponer destinos
 * coherentes. Sus metodos solo calculan rutas o nombres: no crean carpetas, no escriben archivos
 * y no comprueban si el nombre ya existe. Cada consumidor se encarga de esas operaciones.</p>
 */
public final class JavaModelOutput {

    /** Impide instanciar esta clase de utilidades, que solo contiene metodos estaticos. */
    private JavaModelOutput() { }

    /**
     * Resuelve la carpeta predeterminada para los modelos de codigo Java.
     *
     * <p>Consulta {@code JAVA_MODEL_OUTPUT_DIR} mediante {@link DotEnv}: primero la variable de
     * entorno, despues la propiedad Java y, por ultimo, los archivos de configuracion del
     * proyecto. Al combinar {@code .env.public} y {@code .env.private}, el privado prevalece.</p>
     *
     * <p>Busca la raiz del proyecto ascendiendo desde el directorio de trabajo hasta encontrar
     * {@code modules/java-model-extractor/pom.xml}. Una ruta configurada relativa se resuelve
     * respecto a esa raiz; si no se encuentra, respecto al directorio de trabajo. Una ruta
     * absoluta configurada se conserva. Sin configuracion, usa
     * {@code modules/java-model-extractor/output} en la raiz o {@code output} en el directorio
     * de trabajo cuando no se encuentra la raiz.</p>
     *
     * @return ruta absoluta normalizada de la carpeta de salida, sin crearla
     * @throws java.nio.file.InvalidPathException si la ruta configurada no es valida
     */
    public static Path defaultDirectory() {
        // Resuelve la carpeta de salida predeterminada usando OutputDirectoryResolver
        return OutputDirectoryResolver.resolve(Path.of(""),
                root -> Files.isRegularFile(root.resolve("modules/java-model-extractor/pom.xml")),
                Path.of("modules/java-model-extractor/output"), DotEnv.getStringVar("JAVA_MODEL_OUTPUT_DIR", null));
    }

    /**
     * Construye el nombre JSON predeterminado a partir del ultimo componente de la carpeta fuente.
     *
     * <p>Primero convierte la ruta en absoluta y la normaliza. Por ejemplo, para
     * {@code data/practica-2023/} devuelve {@code modelo-practica-2023.json}. Si la ruta
     * normalizada no tiene nombre, como la raiz de un sistema de archivos, usa
     * {@code modelo-codigo.json}.</p>
     *
     * <p>Este metodo no comprueba que la carpeta exista ni anade sufijos para evitar
     * sobrescrituras; solo propone el nombre base.</p>
     *
     * @param sourceFolder ruta de la carpeta de codigo fuente; no puede ser {@code null}
     * @return nombre del archivo JSON, sin carpeta de destino
     * @throws NullPointerException si {@code sourceFolder} es {@code null}
     */
    public static String fileName(Path sourceFolder) {
        Path folderName = sourceFolder.toAbsolutePath().normalize().getFileName();
        return "modelo-" + (folderName == null ? "codigo" : folderName) + ".json";
    }
}
