package com.crud2gui.statement.extractor;

import com.crud2gui.common.DotEnv;
import com.crud2gui.statement.filter.StatementFilter;
import com.crud2gui.statement.filter.StatementFilterConfig;
import com.crud2gui.statement.filter.StatementFilterConfigJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Punto de entrada principal del módulo {@code statement-extractor}.
 * Extrae el texto de un PDF y, si está activado, lo filtra con las reglas del
 * fichero JSON indicado por {@code STATEMENT_FILTER_CONFIG_PATH}.
 */
public class Main {

    private static final String FILTER_ENABLED_NAME = "FILTER_STATEMENT_TEXT";
    private static final boolean DEFAULT_FILTER_ENABLED_VALUE = false;
    private static final String FILTER_CONFIG_PATH_NAME = "STATEMENT_FILTER_CONFIG_PATH";
    private static final String DEFAULT_FILTER_CONFIG_PATH =
            "modules/statement-text-extractor/config/statement-filter.json";

    /**
     * Procesa un fichero PDF y genera el texto extraído y, opcionalmente, el
     * texto filtrado.
     *
     * @param args ruta del PDF, ruta opcional para el texto extraído y ruta
     *             opcional para el texto filtrado
     */
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 3) {
            System.err.println("Uso: Main <fichero.pdf> [texto-completo.txt] [texto-filtrado.txt]");
            System.exit(2);
        }

        try {
            Path pdf = Paths.get(args[0]);
            Path rawTxt = (args.length >= 2) ? Paths.get(args[1]) : withSuffix(pdf, ".txt");
            Path filteredTxt = (args.length == 3) ? Paths.get(args[2]) : withSuffix(rawTxt, "-filtrado.txt");

            System.out.println("1/2 Extrayendo texto de " + pdf.toAbsolutePath());
            StatementExtractor.extractText(pdf, rawTxt);
            System.out.println("    -> " + rawTxt.toAbsolutePath() + " (" + Files.size(rawTxt) + " bytes)");

            boolean runFilter = DotEnv.getBooleanVar(FILTER_ENABLED_NAME, DEFAULT_FILTER_ENABLED_VALUE);
            if (runFilter) {
                String configuredPath = DotEnv.getStringVar(
                        FILTER_CONFIG_PATH_NAME, DEFAULT_FILTER_CONFIG_PATH);
                Path configPath = resolveConfigPath(configuredPath);
                StatementFilterConfig config = StatementFilterConfigJson.read(configPath);

                System.out.println("2/2 Filtrando el enunciado (activado)");
                System.out.println("    Configuración: " + configPath);
                StatementFilter.Result result = StatementFilter.filterFile(rawTxt, filteredTxt, config);
                if (!result.structureRecognized()) {
                    System.err.println("AVISO: no se reconoció la estructura del enunciado; "
                            + "el fichero filtrado es una copia del texto original.");
                }
                System.out.println("    -> " + filteredTxt.toAbsolutePath() + " (" + Files.size(filteredTxt) + " bytes)");
            } else {
                System.out.println("2/2 Filtrado omitido por configuración.");
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static Path resolveConfigPath(String configuredPath) throws IOException {
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IOException("La ruta de configuración JSON está vacía.");
        }

        Path path = Paths.get(configuredPath);
        if (path.isAbsolute()) {
            return path;
        }

        Path directory = Paths.get("").toAbsolutePath().normalize();
        while (directory != null) {
            Path candidate = directory.resolve(path).normalize();
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            directory = directory.getParent();
        }

        throw new IOException("No se encuentra el fichero de configuración JSON: " + configuredPath);
    }

    /**
     * Genera una ruta hermana reemplazando o añadiendo un sufijo antes de la
     * extensión del fichero.
     *
     * @param path ruta base
     * @param suffix sufijo que se añadirá
     * @return ruta resultante en el mismo directorio
     */
    private static Path withSuffix(Path path, String suffix) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;
        return path.resolveSibling(base + suffix);
    }
}
