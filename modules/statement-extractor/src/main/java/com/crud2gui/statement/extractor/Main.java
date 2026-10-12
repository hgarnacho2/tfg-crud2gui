package com.crud2gui.statement.extractor;

import com.crud2gui.common.DotEnv;
import com.crud2gui.statement.filter.StatementFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Punto de entrada principal para el módulo {@code statement-extractor}.
 * <p>
 * Su función es procesar un fichero PDF de enunciado, extraer su contenido textual
 * en un fichero de texto plano y, de manera opcional según la configuración,
 * aplicar un filtrado estructural para limpiar portadas e índices.
 * </p>
 *
 * @author hgarnacho
 * @version 2.1
 * @since 1.0
 */
public class Main {

    /** Valor por defecto para la activación del filtrado si no se especifica lo contrario. */
    private static final String FILTER_ENABLED_NAME = "FILTER_STATEMENT_TEXT";
    private static final boolean DEFAULT_FILTER_ENABLED_VALUE = false;

    /**
     * Método ejecutable principal (Entry Point).
     *
     * @param args argumentos de la línea de comandos:
     *             <ol>
     *                 <li>Ruta al fichero PDF de origen (obligatorio).</li>
     *                 <li>Ruta opcional para el texto completo de salida.</li>
     *                 <li>Ruta opcional para el texto filtrado de salida.</li>
     *             </ol>
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

            // Resolución centralizada de la configuración de filtrado a través de DotEnv
            boolean runFilter = DotEnv.getBooleanVar(FILTER_ENABLED_NAME, DEFAULT_FILTER_ENABLED_VALUE);

            if (runFilter) {
                System.out.println("2/2 Filtrando el enunciado (activado)");
                StatementFilter.Result result = StatementFilter.filterFile(rawTxt, filteredTxt);
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

    /**
     * Genera una nueva ruta de tipo {@link Path} hermana a partir de un fichero existente,
     * reemplazando o añadiendo un sufijo antes de la extensión.
     *
     * @param path   ruta del fichero original.
     * @param suffix el sufijo que se desea añadir (por ejemplo, {@code ".txt"} o {@code "-filtrado.txt"}).
     * @return un objeto {@link Path} con el nuevo nombre de fichero en el mismo directorio.
     */
    private static Path withSuffix(Path path, String suffix) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;
        return path.resolveSibling(base + suffix);
    }
}