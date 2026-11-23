package com.crud2gui.model;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectModelSchemaTest {

    private static final String VALID_EMPTY_MODEL = """
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

    @Test
    void acceptsCompleteModelJson() throws Exception {
        assertDoesNotThrow(() -> ProjectModelSchema.validate(VALID_EMPTY_MODEL));
        assertTrue(ProjectModelSchema.jsonSchema().contains("CRUD2GUI ProjectModel 1.0"));
    }

    @Test
    void rejectsMissingRequiredProperty() {
        String invalid = """
                {"schemaVersion":"1.0","source":{},"types":[],"relations":[],"analysis":null,"layout":null}
                """;

        assertThrows(IOException.class, () -> ProjectModelSchema.validate(invalid));
    }

    @Test
    void rejectsWrongPropertyType() {
        String invalid = VALID_EMPTY_MODEL.replace("\"expectedManagerId\": null", "\"expectedManagerId\": 7");

        assertThrows(IOException.class, () -> ProjectModelSchema.validate(invalid));
    }

    @Test
    void acceptsStructuralEvidenceAndRejectsRemovedDependencyFields() throws Exception {
        String relation = """
                {"kind":"ASSOCIATION","from":"A","to":"B","via":"b","multiplicity":"1",
                 "confidence":"HIGH","evidence":{"createdWithNew":false,"assignedFromParameter":true,
                 "initializedInline":false,"exposedBySetter":false}}
                """;
        String model = VALID_EMPTY_MODEL.replace("\"relations\": []", "\"relations\": [" + relation + "]");
        assertDoesNotThrow(() -> ProjectModelSchema.validate(model));
        assertThrows(IOException.class, () -> ProjectModelSchema.validate(
                model.replace("\"ASSOCIATION\"", "\"DEPENDENCY\"")));
        assertThrows(IOException.class, () -> ProjectModelSchema.validate(
                model.replace("\"exposedBySetter\":false", "\"exposedBySetter\":false,\"usedIn\":null")));
    }
}