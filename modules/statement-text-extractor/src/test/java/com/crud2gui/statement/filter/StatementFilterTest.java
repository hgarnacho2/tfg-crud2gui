package com.crud2gui.statement.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementFilterTest {

    private static final String SAMPLE = String.join("\r\n",
            "Universidad Nacional de Educación a Distancia",
            "FUNDAMENTOS DE INFORMÁTICA",
            "ÍNDICE GENERAL",
            "INTRODUCCIÓN ..................... 2",
            "FECHAS Y NORMAS DE ENTREGA ....... 9",
            "INTRODUCCIÓN",
            "La práctica se realizará en Java.",
            "2",
            "Fundamentos de Informática. Práctica Obligatoria 2023/2024",
            "1.2. Requisitos previos",
            "Debe conocer los bucles.",
            "1.3. Descripción del sistema",
            "Existe una clase Biblioteca con libros.",
            "FECHAS Y NORMAS DE ENTREGA",
            "Entrega el 1 de junio.");

    @Test
    void removesCoverIndexPageNumbersHeadersPrerequisitesAndTail() {
        StatementFilter.Result r = StatementFilter.filter(SAMPLE);

        assertTrue(r.structureRecognized());
        String t = r.text();
        assertTrue(t.startsWith("INTRODUCCIÓN"));
        assertTrue(t.contains("La práctica se realizará en Java."));
        assertTrue(t.contains("1.3. Descripción del sistema"));
        assertTrue(t.contains("Existe una clase Biblioteca con libros."));

        assertFalse(t.contains("Universidad Nacional"));
        assertFalse(t.contains("ÍNDICE GENERAL"));
        assertFalse(t.contains("Práctica Obligatoria"));
        assertFalse(t.contains("Requisitos previos"));
        assertFalse(t.contains("bucles"));
        assertFalse(t.contains("Entrega el 1 de junio"));
        assertFalse(t.contains("\r"));
    }

    @Test
    void returnsOriginalWhenStructureIsNotRecognized() {
        String other = "Un texto cualquiera\nsin la estructura esperada";
        StatementFilter.Result r = StatementFilter.filter(other);

        assertFalse(r.structureRecognized());
        assertEquals(other, r.text());
    }

    @Test
    void returnsOriginalWhenEndSectionIsMissing() {
        String text = "Introducción\nContenido sin marcador de fin";

        StatementFilter.Result result = StatementFilter.filter(text);

        assertFalse(result.structureRecognized());
        assertEquals(text, result.text());
    }

    @Test
    void appliesConfiguredSectionActions() {
        StatementFilterConfig config = new StatementFilterConfig(List.of(
                new StatementFilterConfig.SectionRule("Resumen", StatementFilterConfig.SectionAction.START),
                new StatementFilterConfig.SectionRule("Notas internas", StatementFilterConfig.SectionAction.IGNORE),
                new StatementFilterConfig.SectionRule("Modelo", StatementFilterConfig.SectionAction.INCLUDE),
                new StatementFilterConfig.SectionRule("Anexo", StatementFilterConfig.SectionAction.STOP)),
                2,
                true);
        String text = String.join("\n",
                "Portada",
                "Resumen",
                "Texto inicial",
                "Notas internas",
                "No debe aparecer",
                "1. Modelo",
                "Debe aparecer",
                "Anexo",
                "No debe aparecer tampoco");

        StatementFilter.Result result = StatementFilter.filter(text, config);

        assertTrue(result.structureRecognized());
        assertTrue(result.text().contains("Texto inicial"));
        assertTrue(result.text().contains("Debe aparecer"));
        assertFalse(result.text().contains("No debe aparecer"));
        assertFalse(result.text().contains("Portada"));
        assertFalse(result.text().contains("Anexo"));
    }

    @Test
    void savesAndLoadsConfigurationAsJson(@TempDir Path tmp) throws Exception {
        StatementFilterConfig config = StatementFilterConfig.defaults();
        Path jsonFile = tmp.resolve("settings/statement-filter.json");

        StatementFilterConfigJson.write(config, jsonFile);

        assertTrue(Files.exists(jsonFile));
        assertTrue(Files.readString(jsonFile).contains("\"sectionRules\""));
        assertEquals(config, StatementFilterConfigJson.read(jsonFile));
    }

    @Test
    void filtersFileAndWritesOutput(@TempDir Path tmp) throws Exception {
        Path in = tmp.resolve("enunciado.txt");
        Files.writeString(in, SAMPLE);

        StatementFilter.Result r = StatementFilter.filterFile(in);

        Path out = tmp.resolve("enunciado-filtrado.txt");
        assertTrue(r.structureRecognized());
        assertTrue(Files.exists(out));
        assertTrue(Files.readString(out).contains("Existe una clase Biblioteca"));
    }

    @Test
    void collapsesConsecutiveBlankLinesToAtMostTwo() {
        String text = String.join("\n",
                "INTRODUCCIÓN",
                "uno",
                "",
                "dos",
                "",
                "",
                "tres",
                "",
                "   ",
                "\u00A0",
                "",
                "cuatro",
                "",
                "",
                "",
                "Fechas y normas de entrega");

        String result = StatementFilter.filter(text).text();

        assertEquals(String.join("\n",
                "INTRODUCCIÓN",
                "uno",
                "",
                "dos",
                "",
                "",
                "tres",
                "",
                "",
                "cuatro") + "\n", result);
    }

    @Test
    void recognizesMarkersRegardlessOfCaseAndAccents() {
        // Formato de otros cursos: títulos en minúsculas, "ı" + acento combinado y numeración en el final
        String text = String.join("\n",
                "Índice general",
                "1. Introducción 2",
                "5. Fechas y normas de entrega 18",
                "1",
                "Introducci\u00F3n",
                "Los objetos se relacionan entre s\u0131\u0301 de varias formas.",
                "2.1.2. Requisitos previos",
                "Estudiar los temas 4, 5 y 6.",
                "2.1.3. Implementación de la clase Pueblo",
                "Cada pueblo tiene un nombre.",
                "5. Fechas y normas de entrega",
                "Entrega el 8 de mayo.");

        StatementFilter.Result r = StatementFilter.filter(text);

        assertTrue(r.structureRecognized());
        String t = r.text();
        assertTrue(t.startsWith("Introducción"));
        assertTrue(t.contains("entre sí de varias formas"));
        assertTrue(t.contains("Cada pueblo tiene un nombre."));
        assertFalse(t.contains("Índice general"));
        assertFalse(t.contains("Estudiar los temas"));
        assertFalse(t.contains("Entrega el 8 de mayo"));
    }
}
