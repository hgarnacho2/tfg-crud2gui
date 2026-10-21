package com.crud2gui.model;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import com.crud2gui.model.ModelIO;
import com.crud2gui.model.ProjectModel;
import com.crud2gui.model.RelationKind;
import com.crud2gui.model.TypeInfo;

/**
 * Pruebas de lectura y escritura del metamodelo en JSON.
 */
class ModelIOTest {

    private static final Path EJEMPLO = Path.of("src/test/resources/ejemplo-plataforma.json");

    /** El JSON de ejemplo se lee y sus datos clave se reconstruyen correctamente. */
    @Test
    void leeElEjemplo() throws Exception {
        ProjectModel m = ModelIO.read(EJEMPLO);
        assertEquals(1, m.types().size());
        TypeInfo t = m.types().get(0);
        assertEquals("List<Usuario>", t.fields().get(0).type().display());
        assertEquals(RelationKind.AGGREGATION, m.relations().get(0).kind());
        assertEquals("com.tienda.PlataformaCompraVenta", m.analysis().managerClass().id());
        assertTrue(m.expectedManagerId() == null);
    }

    /** Escribir y volver a leer devuelve un modelo igual al original. */
    @Test
    void idaYVueltaConservaElModelo() throws Exception {
        ProjectModel model = ModelIO.read(EJEMPLO);
        ProjectModel original = new ProjectModel(model.schemaVersion(), "com.tienda.PlataformaCompraVenta",
            model.source(), model.types(), model.relations(), model.analysis(), model.layout());
        Path tmp = Files.createTempFile("modelo", ".json");
        ModelIO.write(original, tmp);
        assertEquals(original, ModelIO.read(tmp));
    }

    /** El JSON tambien se puede reconstruir directamente desde memoria. */
    @Test
    void leeJsonEnMemoria() throws Exception {
        ProjectModel model = ModelIO.fromJson("""
                {"schemaVersion":"1.0","types":[],"relations":[]}
                """);

        assertEquals(ProjectModel.CURRENT_SCHEMA, model.schemaVersion());
        assertTrue(model.types().isEmpty());
        assertTrue(model.relations().isEmpty());
    }

    /** Un fichero con otra version mayor del esquema se rechaza. */
    @Test
    void rechazaVersionMayorIncompatible() throws Exception {
        Path tmp = Files.createTempFile("modelo", ".json");
        Files.writeString(tmp, "{\"schemaVersion\":\"2.0\"}");
        assertThrows(java.io.IOException.class, () -> ModelIO.read(tmp));
    }
}
