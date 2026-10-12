package com.crud2gui.statement.extractor;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Extrae el contenido textual de un fichero PDF y permite guardarlo
 * en un fichero de texto plano con extensión {@code .txt}.
 *
 * @author hgarnacho
 * @version 1.2
 * @since 1.0
 */
public final class StatementExtractor {

    private StatementExtractor() {
        // Clase de utilidad: no debe instanciarse.
    }

    /**
     * Extrae el texto de un fichero PDF y lo devuelve como una cadena de caracteres.
     *
     * @param pdfPath ruta del fichero PDF de origen.
     * @return texto extraído del PDF como un {@link String}.
     * @throws IOException si el fichero no existe o no se puede leer.
     */
    public static String extractText(Path pdfPath) throws IOException {
        if (pdfPath == null) {
            throw new IllegalArgumentException("La ruta del PDF no puede ser null.");
        }

        if (!Files.exists(pdfPath) || !Files.isRegularFile(pdfPath) || !Files.isReadable(pdfPath)) {
            throw new IOException("No se puede leer o no existe el fichero PDF: " + pdfPath);
        }

        File file = pdfPath.toFile();

        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    /**
     * Extrae el texto de un fichero PDF y lo guarda en la ruta de salida especificada.
     *
     * @param pdfPath ruta del fichero PDF de origen.
     * @param txtPath ruta del fichero TXT de destino.
     * @return ruta del fichero TXT generado.
     * @throws IOException si ocurre un error de lectura/escritura.
     */
    public static Path extractText(Path pdfPath, Path txtPath) throws IOException {
        if (pdfPath == null) {
            throw new IllegalArgumentException("La ruta del PDF no puede ser null.");
        }

        if (txtPath == null) {
            throw new IllegalArgumentException("La ruta del TXT de salida no puede ser null.");
        }

        Path normalizedPdf = pdfPath.toAbsolutePath().normalize();
        Path normalizedTxt = txtPath.toAbsolutePath().normalize();

        if (normalizedPdf.equals(normalizedTxt)) {
            throw new IllegalArgumentException("El fichero de salida no puede ser el mismo que el PDF de origen.");
        }

        String text = extractText(pdfPath);

        Path parent = normalizedTxt.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(normalizedTxt, text, StandardCharsets.UTF_8);

        return normalizedTxt;
    }

    /**
     * Extrae el texto de un fichero PDF generando automáticamente el TXT en la misma carpeta.
     *
     * @param pdfPath ruta del fichero PDF de origen.
     * @param autoSave parámetro booleano para distinguir esta firma y activar el guardado automático.
     * @return ruta del fichero TXT generado.
     * @throws IOException si ocurre un error de lectura/escritura.
     */
    public static Path extractText(Path pdfPath, boolean autoSave) throws IOException {
        if (pdfPath == null) {
            throw new IllegalArgumentException("La ruta del PDF no puede ser null.");
        }

        if (!autoSave) {
            extractText(pdfPath);
            return pdfPath; // O manejar según convenga si solo se quería omitir
        }

        Path fileName = pdfPath.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("La ruta del PDF debe incluir un nombre de fichero.");
        }

        String name = fileName.toString();
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;

        Path txtPath = pdfPath.resolveSibling(base + ".txt");

        return extractText(pdfPath, txtPath);
    }

    public static void main(String[] args) {
        if (args == null || args.length < 1) {
            System.err.println("Uso: java com.crud2gui.statementextractor.StatementExtractor <fichero.pdf> [salida.txt]");
            System.exit(1);
        }

        try {
            Path pdf = Paths.get(args[0]);
            Path result;

            if (args.length >= 2) {
                Path txt = Paths.get(args[1]);
                result = extractText(pdf, txt);
            } else {
                result = extractText(pdf, true);
            }

            System.out.println("Texto extraído en: " + result.toAbsolutePath());

        } catch (InvalidPathException e) {
            System.err.println("Error: la ruta indicada no es válida. " + e.getMessage());
            System.exit(2);
        } catch (IllegalArgumentException | IOException e) {
            System.err.println("Error procesando el PDF: " + e.getMessage());
            System.exit(2);
        }
    }
}