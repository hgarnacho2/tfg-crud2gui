package com.crud2gui.statement.extractor;

import com.crud2gui.common.utils.PdfTextExtractor;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Extrae el contenido textual del PDF de un enunciado y permite guardarlo
 * en un fichero de texto plano con extensión {@code .txt}.
 * Delega la extracción genérica en {@link PdfTextExtractor}.
 *
 * @author hgarnacho
 * @version 1.3
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
        return PdfTextExtractor.extractText(pdfPath);
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
        return PdfTextExtractor.extractText(pdfPath, txtPath);
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

        return extractText(pdfPath, PdfTextExtractor.siblingTxtPath(pdfPath));
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