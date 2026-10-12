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
import java.util.regex.Pattern;

/**
 * Limpia el texto de un enunciado de práctica para quedarse solo con lo útil
 * al analizarlo (por ejemplo, con un LLM): elimina portada e índice, números
 * de página, cabeceras repetidas, la sección "Requisitos previos" y todo lo
 * posterior a "FECHAS Y NORMAS DE ENTREGA". Además, colapsa las líneas en
 * blanco consecutivas (como máximo {@value #MAX_CONSECUTIVE_BLANK_LINES}) y
 * elimina las que sobran al final.
 *
 * Los marcadores de inicio ("Introducción") y fin ("Fechas y normas de entrega")
 * se reconocen sin distinguir mayúsculas ni tildes.
 *
 * Si no reconoce la estructura del enunciado, devuelve el texto original.
 *
 * Uso desde línea de comandos:
 *   java com.crud2gui.pdfstatenment.StatementFilter enunciado.txt [salida-filtrada.txt]
 */
public class StatementFilter {

    /** Resultado del filtrado. */
    public record Result(String text, boolean structureRecognized) {
    }

    // Marcadores: se comparan sin distinguir mayúsculas, tildes ni espacios repetidos
    // ("INTRODUCCIÓN", "Introducción" e "introduccion" valen igual).
    private static final String START_MARKER = "introduccion";
    private static final String END_MARKER = "fechas y normas de entrega";
    private static final String SKIPPED_SECTION = "requisitos previos";

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
    private static final Pattern NUMBERED_HEADING =
            Pattern.compile("^[0-9]+([.][0-9]+)*[.]?\\s+", FLAGS);

    private StatementFilter() {
    }

    /**
     * Filtra el texto del enunciado. Si no se encuentra el inicio del cuerpo
     * o el resultado queda vacío, devuelve el texto original con
     * {@code structureRecognized = false}.
     */
    public static Result filter(String rawText) {
        // Los PDF generados con LaTeX suelen dar "ı" + acento combinado en lugar de "í".
        String text = Normalizer.normalize(
                rawText.replace("\u0131\u0301", "\u00ED"), Normalizer.Form.NFC);
        List<String> kept = new ArrayList<>();

        boolean inBody = false;
        boolean skipSection = false;

        for (String line : text.split("\\R", -1)) {
            String trimmed = line.strip();

            if (!inBody) {
                if (key(trimmed).equals(START_MARKER)) {
                    inBody = true;
                    kept.add(line);
                }
                continue;
            }

            if (key(removeNumbering(trimmed)).equals(END_MARKER)) {
                break;
            }
            if (PAGE_NUMBER.matcher(trimmed).matches()) {
                continue;
            }
            if (PAGE_HEADER.matcher(trimmed).find()) {
                continue;
            }
            if (key(removeNumbering(trimmed)).equals(SKIPPED_SECTION)) {
                skipSection = true;
                continue;
            }
            if (skipSection && NUMBERED_HEADING.matcher(trimmed).find()) {
                skipSection = false;
            }
            if (skipSection) {
                continue;
            }
            kept.add(line);
        }

        String result = String.join("\n", collapseBlankLines(kept));
        if (!inBody || result.isBlank()) {
            return new Result(rawText, false);
        }
        return new Result(result + "\n", true);
    }

    /**
     * Filtra un fichero TXT y escribe el resultado en {@code output}.
     * Devuelve el resultado del filtrado (con el aviso de si se reconoció
     * la estructura).
     */
    public static Result filterFile(Path input, Path output) throws IOException {
        if (!Files.isRegularFile(input)) {
            throw new IOException("No existe el fichero TXT: " + input);
        }
        String raw = Files.readString(input, StandardCharsets.UTF_8);
        Result result = filter(raw);

        if (output.toAbsolutePath().getParent() != null) {
            Files.createDirectories(output.toAbsolutePath().getParent());
        }
        Files.writeString(output, result.text(), StandardCharsets.UTF_8);
        return result;
    }

    /** Igual que {@link #filterFile(Path, Path)}, con salida {@code <nombre>-filtrado.txt}. */
    public static Result filterFile(Path input) throws IOException {
        String name = input.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;
        return filterFile(input, input.resolveSibling(base + "-filtrado.txt"));
    }

    /**
     * Deja como mucho {@link #MAX_CONSECUTIVE_BLANK_LINES} líneas en blanco
     * seguidas (las líneas con solo espacios se normalizan a vacías) y quita
     * las líneas en blanco del final.
     */
    private static List<String> collapseBlankLines(List<String> lines) {
        List<String> out = new ArrayList<>();
        int blankRun = 0;
        for (String line : lines) {
            if (isBlank(line)) {
                blankRun++;
                if (blankRun <= MAX_CONSECUTIVE_BLANK_LINES) {
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
