package com.crud2gui.common.utils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Extrae el contenido textual de cualquier fichero PDF y permite guardarlo en un fichero de texto UTF-8.
 * Requiere pdfbox (dependencia opcional del módulo common).
 */
public final class PdfTextExtractor {

    private PdfTextExtractor() {
    }

    /**
     * Extrae el texto de un fichero PDF ordenado por posición.
     *
     * @param pdfPath ruta del fichero PDF de origen.
     * @return texto extraído del PDF.
     * @throws IOException si el fichero no existe o no se puede leer.
     */
    public static String extractText(Path pdfPath) throws IOException {
        if (pdfPath == null) {
            throw new IllegalArgumentException("La ruta del PDF no puede ser null.");
        }
        if (!Files.isRegularFile(pdfPath) || !Files.isReadable(pdfPath)) {
            throw new IOException("No se puede leer o no existe el fichero PDF: " + pdfPath);
        }
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    /**
     * Extrae el texto de un fichero PDF y lo guarda en la ruta indicada, creando las carpetas necesarias.
     *
     * @param pdfPath ruta del fichero PDF de origen.
     * @param txtPath ruta del fichero de texto de destino.
     * @return ruta absoluta y normalizada del fichero generado.
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
     * Devuelve la ruta {@code .txt} hermana del PDF (mismo nombre base, misma carpeta).
     *
     * @param pdfPath ruta del fichero PDF.
     * @return ruta del fichero de texto correspondiente.
     */
    public static Path siblingTxtPath(Path pdfPath) {
        if (pdfPath == null || pdfPath.getFileName() == null) {
            throw new IllegalArgumentException("La ruta del PDF debe incluir un nombre de fichero.");
        }
        String name = pdfPath.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        return pdfPath.resolveSibling(base + ".txt");
    }
}
