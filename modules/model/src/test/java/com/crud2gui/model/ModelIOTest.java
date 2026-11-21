package com.crud2gui.model;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    private static final String MODELO_VACIO_VALIDO = """
            {
              "schemaVersion": "1.0",
              "expectedManagerId": null,
              "source": {"root": null, "generatedAt": null, "filesParsed": 0, "warnings": []},
              "types": [],
              "relations": [],
              "analysis": null,
              "layout": null
            }
            """;

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

    /** La serializacion incluye los campos nulos requeridos y valida el resultado contra el esquema. */
    @Test
    void serializaYValidaModeloConformeAlEsquema() throws Exception {
        ProjectModel model = ModelIO.fromJson(MODELO_VACIO_VALIDO);

        String json = ModelIO.toJson(model);

        assertDoesNotThrow(() -> ProjectModelSchema.validate(json));
        assertEquals(model, ModelIO.fromJson(json));
    }

    /** Un modelo que incumple el esquema no se serializa ni se escribe parcialmente. */
    @Test
    void rechazaSerializarModeloQueIncumpleElEsquema() throws Exception {
        ProjectModel invalid = new ProjectModel(
                ProjectModel.CURRENT_SCHEMA, null, null, List.of(), List.of(), null, null);
        Path tmp = Files.createTempFile("modelo-invalido", ".json");

        assertThrows(java.io.IOException.class, () -> ModelIO.toJson(invalid));
        assertThrows(java.io.IOException.class, () -> ModelIO.write(invalid, tmp));
        assertEquals(0, Files.size(tmp));
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

    /** Lee y conserva marcas de opcionalidad, manteniendo false para modelos anteriores al campo. */
    @Test
    void conservaOpcionalidadYAdmiteJsonAnterior() throws Exception {
        ProjectModel optionalModel = ModelIO.fromJson("""
                {
                  "schemaVersion":"1.0",
                  "types":[{
                    "id":"Opcional","name":"Opcional","kind":"CLASS","optional":true,
                    "fields":[{"name":"dato","optional":true}],
                    "methods":[{"name":"accion","optional":true}]
                  }],
                  "relations":[]
                }
                """);

        TypeInfo optionalType = optionalModel.types().get(0);
        assertTrue(optionalType.optional());
        assertTrue(optionalType.fields().get(0).optional());
        assertTrue(optionalType.methods().get(0).optional());
        ProjectModel oldModel = ModelIO.fromJson("""
                {"schemaVersion":"1.0","types":[{"id":"Antiguo","kind":"CLASS"}],"relations":[]}
                """);
        assertFalse(oldModel.types().get(0).optional());
    }

    /** Un fichero con otra version mayor del esquema se rechaza. */
    @Test
    void rechazaVersionMayorIncompatible() throws Exception {
        Path tmp = Files.createTempFile("modelo", ".json");
        Files.writeString(tmp, "{\"schemaVersion\":\"2.0\"}");
        assertThrows(java.io.IOException.class, () -> ModelIO.read(tmp));
    }

}
