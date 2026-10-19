package com.crud2gui.statement.extractor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfToTxtConverterTest {

    @Test
    void convertsPdfToTxt(@TempDir Path tmp) throws Exception {
        Path pdf = tmp.resolve("sample.pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Hola PDF");
                cs.endText();
            }
            doc.save(pdf.toFile());
        }

        // Llamada corregida utilizando la sobrecarga con el booleano para el guardado automático
        Path txt = StatementExtractor.extractText(pdf, true);

        assertTrue(Files.exists(txt));
        assertTrue(Files.readString(txt).contains("Hola PDF"));
    }

    @Test
    void failsWhenPdfDoesNotExist(@TempDir Path tmp) {
        assertThrows(IOException.class,
                () -> StatementExtractor.extractText(tmp.resolve("no-existe.pdf")));
    }
}