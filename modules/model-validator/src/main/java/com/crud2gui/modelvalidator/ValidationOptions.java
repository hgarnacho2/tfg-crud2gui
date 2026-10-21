package com.crud2gui.modelvalidator;

import com.fasterxml.jackson.databind.JsonNode;
import com.crud2gui.model.TypeKind;

/** Configura las categorías estructurales que debe comprobar el validador. */
public record ValidationOptions(boolean classes, boolean interfaces, boolean enums, boolean records,
                                boolean annotations, boolean abstractness, boolean relationships,
                                boolean attributes, boolean constructors, boolean methods,
                                boolean enumConstants, boolean collectionOperations) {

    public static ValidationOptions defaults() {
        return new ValidationOptions(true, true, true, true, true, true,
                true, true, true, true, true, true);
    }

    public boolean validatesType(TypeKind kind) {
        if (kind == null) return true;
        return switch (kind) {
            case CLASS -> classes;
            case INTERFACE -> interfaces;
            case ENUM -> enums;
            case RECORD -> records;
            case ANNOTATION -> annotations;
        };
    }

    public static ValidationOptions fromJson(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("La configuracion de validacion debe ser un objeto JSON.");
        }
        JsonNode types = root.get("types");
        if (types != null && !types.isObject()) {
            throw new IllegalArgumentException("La propiedad 'types' debe ser un objeto JSON.");
        }

        ValidationOptions defaults = defaults();
        return new ValidationOptions(
                booleanValue(types, "classes", defaults.classes),
                booleanValue(types, "interfaces", defaults.interfaces),
                booleanValue(types, "enums", defaults.enums),
                booleanValue(types, "records", defaults.records),
                booleanValue(types, "annotations", defaults.annotations),
                booleanValue(types, "abstractness", defaults.abstractness),
                booleanValue(root, "relationships", defaults.relationships),
                booleanValue(root, "attributes", defaults.attributes),
                booleanValue(root, "constructors", defaults.constructors),
                booleanValue(root, "methods", defaults.methods),
                booleanValue(root, "enumConstants", defaults.enumConstants),
                booleanValue(root, "collectionOperations", defaults.collectionOperations));
    }

    private static boolean booleanValue(JsonNode object, String name, boolean defaultValue) {
        if (object == null || !object.has(name)) return defaultValue;
        JsonNode value = object.get(name);
        if (!value.isBoolean()) {
            throw new IllegalArgumentException("La opcion '" + name + "' debe ser booleana.");
        }
        return value.asBoolean();
    }
}