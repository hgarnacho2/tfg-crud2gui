package com.crud2gui.statement.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Filtra el texto extraído de un enunciado mediante reglas configurables para
 * reconocer y tratar sus secciones. La configuración predeterminada se adapta
 * a los enunciados UNED de ejemplo del proyecto.
 *
 * <p>Los títulos se comparan sin distinguir mayúsculas, tildes ni numeración
 * inicial. El filtro conserva las secciones no configuradas por defecto,
 * elimina números de página si así se indica y limita las líneas en blanco
 * consecutivas.</p>
 *
 * <p>Para considerar reconocida la estructura, el texto debe contener una regla
 * {@link StatementFilterConfig.SectionAction#START START} seguida de una regla
 * {@link StatementFilterConfig.SectionAction#STOP STOP}. Si falta cualquiera,
 * devuelve el texto original con {@code structureRecognized = false}.</p>
 *
 * @see StatementFilterConfig
 * @see StatementFilterConfigJson
 */
public class StatementFilter {

    /**
     * Resultado del filtrado.
     *
     * @param text texto filtrado, o el original si no se reconoció la estructura
     * @param structureRecognized {@code true} si se encontraron los límites configurados
     */
    public record Result(String text, boolean structureRecognized) {
    }

    /** Máximo de líneas en blanco seguidas que se conservan. */
    public static final int MAX_CONSECUTIVE_BLANK_LINES = 2;

    private static final int FLAGS = Pattern.UNICODE_CHARACTER_CLASS;
    private static final Pattern PAGE_NUMBER = Pattern.compile("^[0-9]+$");
    private static final Pattern PAGE_HEADER =
            Pattern.compile("^Fundamentos de Inform.tica[.] Pr.ctica Obligatoria", FLAGS);
    private static final Pattern SECTION_NUMBER =
            Pattern.compile("^[0-9]+([.][0-9]+)*[.]?\\s*", FLAGS);
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+", FLAGS);

    private StatementFilter() {
    }

    /** Filtra el texto usando {@link StatementFilterConfig#defaults()}. */
    public static Result filter(String rawText) {
        return filter(rawText, StatementFilterConfig.defaults());
    }

    /**
     * Filtra el texto usando las reglas y opciones indicadas.
     *
     * <p>Si no se encuentran las reglas de inicio y fin, se devuelve el texto
     * original y el resultado indica que la estructura no fue reconocida.</p>
     *
     * @param rawText texto extraído del enunciado
     * @param config reglas y opciones del filtrado
     * @return texto filtrado y estado de reconocimiento de estructura
     * @throws NullPointerException si {@code rawText} o {@code config} es {@code null}
     */
    public static Result filter(String rawText, StatementFilterConfig config) {
        Objects.requireNonNull(config, "config");
        // Los PDF generados con LaTeX suelen dar "ı" + acento combinado en lugar de "í".
        String text = Normalizer.normalize(
                rawText.replace("\u0131\u0301", "\u00ED"), Normalizer.Form.NFC);
        List<String> kept = new ArrayList<>();

        boolean inBody = false;
        boolean endRecognized = false;
        boolean includeSection = false;

        for (String line : text.split("\\R", -1)) {
            String trimmed = line.strip();
            StatementFilterConfig.SectionRule rule = findRule(trimmed, config.sectionRules());

            if (!inBody) {
                if (rule != null && rule.action() == StatementFilterConfig.SectionAction.START) {
                    inBody = true;
                    includeSection = true;
                    kept.add(line);
                }
                continue;
            }

            if (rule != null) {
                switch (rule.action()) {
                    case START, INCLUDE -> {
                        includeSection = true;
                        kept.add(line);
                        continue;
                    }
                    case IGNORE -> {
                        includeSection = false;
                        continue;
                    }
                    case STOP -> {
                        endRecognized = true;
                        break;
                    }
                }
                if (endRecognized) {
                    break;
                }
            }

            // Una sección numerada sin regla propia vuelve al comportamiento seguro de incluirla.
            if (isNumberedHeading(trimmed)) {
                includeSection = true;
            }
            if (!includeSection) {
                continue;
            }
            if (config.removePageNumbers() && PAGE_NUMBER.matcher(trimmed).matches()) {
                continue;
            }
            if (PAGE_HEADER.matcher(trimmed).find()) {
                continue;
            }
            kept.add(line);
        }

        String result = String.join("\n", collapseBlankLines(kept, config.maxConsecutiveBlankLines()));
        if (!inBody || !endRecognized || result.isBlank()) {
            return new Result(rawText, false);
        }
        return new Result(result + "\n", true);
    }

    private static StatementFilterConfig.SectionRule findRule(
            String line, List<StatementFilterConfig.SectionRule> rules) {
        String heading = key(removeNumbering(line));
        return rules.stream()
                .filter(rule -> key(rule.heading()).equals(heading))
                .findFirst()
                .orElse(null);
    }

    private static boolean isNumberedHeading(String line) {
        return SECTION_NUMBER.matcher(line).find();
    }

    /**
     * Filtra un fichero TXT con la configuración predeterminada y escribe el
     * resultado en {@code output}.
     *
     * @param input fichero de texto de entrada en UTF-8
     * @param output ruta donde se escribirá el resultado
     * @return resultado del filtrado
     * @throws IOException si no se puede leer la entrada o escribir la salida
     */
    public static Result filterFile(Path input, Path output) throws IOException {
        return filterFile(input, output, StatementFilterConfig.defaults());
    }

    /**
     * Filtra un fichero TXT con una configuración explícita.
     *
     * @param input fichero de texto de entrada en UTF-8
     * @param output ruta donde se escribirá el resultado
     * @param config reglas y opciones del filtrado
     * @return resultado del filtrado
     * @throws IOException si la entrada no existe o no se puede leer o escribir
     * @throws NullPointerException si {@code config} es {@code null}
     */
    public static Result filterFile(Path input, Path output, StatementFilterConfig config) throws IOException {
        if (!Files.isRegularFile(input)) {
            throw new IOException("No existe el fichero TXT: " + input);
        }
        String raw = Files.readString(input, StandardCharsets.UTF_8);
        Result result = filter(raw, config);

        if (output.toAbsolutePath().getParent() != null) {
            Files.createDirectories(output.toAbsolutePath().getParent());
        }
        Files.writeString(output, result.text(), StandardCharsets.UTF_8);
        return result;
    }

    /**
     * Filtra un fichero TXT con la configuración predeterminada y escribe el
     * resultado junto a la entrada con el sufijo {@code -filtrado}.
     *
     * @param input fichero de texto de entrada en UTF-8
     * @return resultado del filtrado
     * @throws IOException si la entrada no existe o no se puede leer o escribir
     */
    public static Result filterFile(Path input) throws IOException {
        return filterFile(input, StatementFilterConfig.defaults());
    }

    /**
     * Filtra un fichero TXT con una configuración explícita y escribe el
     * resultado junto a la entrada con el sufijo {@code -filtrado}.
     *
     * @param input fichero de texto de entrada en UTF-8
     * @param config reglas y opciones del filtrado
     * @return resultado del filtrado
     * @throws IOException si la entrada no existe o no se puede leer o escribir
     * @throws NullPointerException si {@code config} es {@code null}
     */
    public static Result filterFile(Path input, StatementFilterConfig config) throws IOException {
        String name = input.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;
        return filterFile(input, input.resolveSibling(base + "-filtrado.txt"), config);
    }

    /**
     * Deja como mucho {@link #MAX_CONSECUTIVE_BLANK_LINES} líneas en blanco
     * seguidas (las líneas con solo espacios se normalizan a vacías) y quita
     * las líneas en blanco del final.
     */
    private static List<String> collapseBlankLines(List<String> lines, int maxConsecutiveBlankLines) {
        List<String> out = new ArrayList<>();
        int blankRun = 0;
        for (String line : lines) {
            if (isBlank(line)) {
                blankRun++;
                if (blankRun <= maxConsecutiveBlankLines) {
                    out.add("");
                }
            } else {
                blankRun = 0;
                out.add(line);
            }
        }
        while (!out.isEmpty() && out.get(out.size() - 1).isEmpty()) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    /** Línea vacía o solo con espacios (incluidos los espacios no separables). */
    private static boolean isBlank(String line) {
        return line.replace('\u00A0', ' ').isBlank();
    }

    /** Quita la numeración del título ("1.2. Requisitos previos" -> "Requisitos previos"). */
    private static String removeNumbering(String line) {
        return SECTION_NUMBER.matcher(line).replaceFirst("");
    }

    /** Clave de comparación: sin tildes, en minúsculas y con espacios simples. */
    private static String key(String text) {
        String noAccents = DIACRITICS.matcher(
                Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        return MULTI_SPACE.matcher(noAccents.strip().toLowerCase(Locale.ROOT)).replaceAll(" ");
    }

    /**
     * Filtra un fichero TXT desde la línea de comandos.
     *
     * @param args ruta de entrada y, opcionalmente, ruta de salida
     */
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Uso: java com.crud2gui.pdf.StatementFilter <enunciado.txt> [salida-filtrada.txt]");
            System.exit(2);
        }
        try {
            Path input = Paths.get(args[0]);
            Path output = (args.length == 2)
                    ? Paths.get(args[1])
                    : input.resolveSibling(stripExtension(input.getFileName().toString()) + "-filtrado.txt");

            Result result = filterFile(input, output);
            if (!result.structureRecognized()) {
                System.err.println("AVISO: no se reconoció la estructura del enunciado; se conserva el TXT original.");
            }
            System.out.printf("Texto del enunciado: %d -> %d bytes%n",
                    Files.size(input), Files.size(output));
            System.out.println("Resultado: " + output.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot > 0) ? name.substring(0, dot) : name;
    }
}
