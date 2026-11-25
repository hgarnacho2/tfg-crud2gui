package com.crud2gui.common.filesystem;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Calcula directorios de salida a partir de una configuracion y de la ubicacion de un proyecto.
 *
 * <p>Esta utilidad no lee variables de entorno ni crea carpetas o archivos: el componente que
 * llama proporciona la configuracion y se encarga de utilizar la ruta devuelta.</p>
 */
public final class OutputDirectoryResolver {

    private OutputDirectoryResolver() { }

    /**
     * Calcula una ruta de salida a partir de cuatro argumentos: directorio inicial, criterio
     * de raiz, destino predeterminado del proyecto y ruta configurada.
     *
     * <p>Convierte el directorio inicial en una ruta absoluta normalizada y comprueba ese
     * directorio y sus antecesores con {@code isProjectRoot}, hasta encontrar la raiz o llegar
     * al principio del sistema de archivos. El criterio es un unico argumento de tipo
     * {@link Predicate}; puede expresarse como una lambda que compruebe la existencia de un
     * archivo caracteristico del proyecto.</p>
     *
     * <p>Si se proporciona una ruta configurada absoluta, se utiliza directamente. Si es
     * relativa, se resuelve respecto a la raiz encontrada o, si no hay raiz, respecto al
     * directorio inicial. Si no se proporciona configuracion, se usa {@code projectDefault}
     * relativo a la raiz; cuando no se encuentra la raiz se usa {@code output} en el directorio
     * inicial.</p>
     *
     * <p>Por ejemplo, el extractor Java pasa {@code Path.of("")} para comenzar en el directorio
     * de trabajo, una lambda que busca {@code modules/java-model-extractor/pom.xml},
     * {@code modules/java-model-extractor/output} como destino predeterminado y el valor de
     * {@code JAVA_MODEL_OUTPUT_DIR} como configuracion. La llamada incluye los cuatro
     * argumentos, aunque el ultimo puede ser {@code null}.</p>
     *
     * @param workingDirectory directorio desde el que comienza la busqueda; no puede ser {@code null}
     * @param isProjectRoot criterio para reconocer la raiz del proyecto; no puede ser {@code null}
     * @param projectDefault destino predeterminado relativo a la raiz; no puede ser {@code null}
     * @param configuredPath ruta configurada, absoluta o relativa; {@code null} si no se configura
     * @return ruta absoluta normalizada de la carpeta, sin crearla
     * @throws NullPointerException si alguno de los tres primeros argumentos es {@code null}
     * @throws java.nio.file.InvalidPathException si la ruta configurada no es valida
     */
    public static Path resolve(Path workingDirectory, Predicate<Path> isProjectRoot,
                               Path projectDefault, String configuredPath) {
        Path start = Objects.requireNonNull(workingDirectory, "workingDirectory").toAbsolutePath().normalize();
        Objects.requireNonNull(isProjectRoot, "isProjectRoot");
        Objects.requireNonNull(projectDefault, "projectDefault");
        Path root = start;
        while (root != null && !isProjectRoot.test(root)) {
            root = root.getParent();
        }
        Path base = root == null ? start : root;
        Path destination = configuredPath != null ? Path.of(configuredPath)
                : root == null ? Path.of("output") : projectDefault;
        return base.resolve(destination).normalize();
    }
}
