package com.crud2gui.common.utils;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTextExtractorTest {

    @Test
    void extractsTextAndWritesTxt(@TempDir Path tmp) throws Exception {
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

        assertTrue(PdfTextExtractor.extractText(pdf).contains("Hola PDF"));
        Path txt = PdfTextExtractor.extractText(pdf, tmp.resolve("out/sample.txt"));
        assertTrue(Files.readString(txt).contains("Hola PDF"));
    }

    @Test
    void rejectsMissingPdfAndSameOutput(@TempDir Path tmp) {
        assertThrows(IOException.class, () -> PdfTextExtractor.extractText(tmp.resolve("no-existe.pdf")));
        Path pdf = tmp.resolve("a.pdf");
        assertThrows(IllegalArgumentException.class, () -> PdfTextExtractor.extractText(pdf, pdf));
    }

    @Test
    void buildsSiblingTxtPath() {
        assertEquals(Path.of("dir", "enunciado.txt"), PdfTextExtractor.siblingTxtPath(Path.of("dir", "enunciado.pdf")));
    }
}
