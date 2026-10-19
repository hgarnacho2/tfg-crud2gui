package com.crud2gui.javamodelextractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuracion inmutable del detector de clase gestora.
 *
 * <p>Los valores base se leen de {@code manager-analysis.json}. Una configuracion externa puede
 * reemplazar solo algunas claves; las restantes conservan sus valores predeterminados.</p>
 */
final class ManagerDetectorConfig {
    /** Lector JSON compartido para cargar recursos y ficheros de configuracion. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Peso por cada fraccion de tipos del dominio alcanzables desde la candidata. */
    final double reachabilityWeight;
    /** Peso por ser raiz de un componente del grafo de atributos. */
    final double rootWeight;
    /** Peso por cada atributo de coleccion que contiene tipos del proyecto. */
    final double collectionWeight;
    /** Peso por cada coleccion del dominio usada desde los metodos de la candidata. */
    final double collectionUsageWeight;
    /** Peso por instanciarse desde un punto de entrada o una prueba. */
    final double entryPointWeight;
    /** Peso menor para coincidencias lexicas, usadas solo como desempate. */
    final double lexicalWeight;
    /** Diferencia minima de puntuacion para asignar confianza alta. */
    final double highConfidenceGap;
    /** Diferencia minima de puntuacion para asignar confianza media. */
    final double mediumConfidenceGap;
    /** Puntuacion minima que debe tener una candidata para alcanzar confianza alta. */
    final double highConfidenceMinimumScore;
    /** Terminos que se buscan en nombres de clases como indicio lexico. */
    final List<String> managerNameTerms;
    /** Terminos que se buscan en nombres de metodos como indicio lexico. */
    final List<String> managerMethodTerms;

    /**
     * Construye la configuracion a partir de un nodo JSON ya completado con valores base.
     *
     * @param root objeto JSON con secciones {@code weights}, {@code confidence} y terminos lexicos
     */
    private ManagerDetectorConfig(JsonNode root) {
        JsonNode weights = root.path("weights");
        JsonNode confidence = root.path("confidence");
        reachabilityWeight = number(weights, "reachability");
        rootWeight = number(weights, "root");
        collectionWeight = number(weights, "collections");
        collectionUsageWeight = number(weights, "collectionUsage");
        entryPointWeight = number(weights, "entryPoint");
        lexicalWeight = number(weights, "lexical");
        highConfidenceGap = number(confidence, "highGap");
        mediumConfidenceGap = number(confidence, "mediumGap");
        highConfidenceMinimumScore = number(confidence, "highMinimumScore");
        managerNameTerms = strings(root.path("managerNameTerms"));
        managerMethodTerms = strings(root.path("managerMethodTerms"));
    }

    /**
     * Carga la configuracion predeterminada incluida en los recursos del parser.
     *
     * @return configuracion predeterminada inmutable
     * @throws IOException si el recurso no existe o contiene JSON invalido
     */
    static ManagerDetectorConfig loadDefault() throws IOException {
        try (InputStream stream = ManagerDetectorConfig.class.getResourceAsStream("/manager-analysis.json")) {
            if (stream == null) throw new IOException("No se encuentra manager-analysis.json en el classpath");
            return new ManagerDetectorConfig(MAPPER.readTree(stream));
        }
    }

    /**
     * Carga una configuracion externa y completa las claves omitidas con los valores por defecto.
     *
     * <p>Los objetos JSON se combinan recursivamente. Los valores escalares y las listas externas
     * reemplazan por completo sus valores predeterminados.</p>
     *
     * @param path ruta del fichero JSON externo
     * @return configuracion resultante e inmutable
     * @throws IOException si no se puede leer el fichero, el JSON es invalido o falta el recurso base
     */
    static ManagerDetectorConfig load(Path path) throws IOException {
        return new ManagerDetectorConfig(merge(defaults(), MAPPER.readTree(path.toFile())));
    }

    /**
     * Lee el objeto JSON de configuracion incluido como recurso del modulo.
     *
     * @return arbol JSON con los valores predeterminados
     * @throws IOException si falta el recurso o su contenido no es JSON valido
     */
    private static JsonNode defaults() throws IOException {
        try (InputStream stream = ManagerDetectorConfig.class.getResourceAsStream("/manager-analysis.json")) {
            if (stream == null) throw new IOException("No se encuentra manager-analysis.json en el classpath");
            return MAPPER.readTree(stream);
        }
    }

    /**
     * Combina dos nodos JSON: mezcla objetos por clave y reemplaza los demas tipos con el override.
     *
     * @param defaults valores base
     * @param overrides valores externos que tienen prioridad
     * @return arbol combinado; los nodos de entrada no se modifican
     */
    private static JsonNode merge(JsonNode defaults, JsonNode overrides) {
        if (!defaults.isObject() || !overrides.isObject()) return overrides;
        ObjectNode result = defaults.deepCopy();
        overrides.fields().forEachRemaining(field -> {
            JsonNode defaultValue = defaults.get(field.getKey());
            JsonNode mergedValue = defaultValue == null ? field.getValue()
                    : merge(defaultValue, field.getValue());
            result.set(field.getKey(), mergedValue);
        });
        return result;
    }

    /**
     * Lee un valor numerico o devuelve cero si la clave no existe o no es numerica.
     *
     * @param node objeto JSON que contiene el valor
     * @param field nombre de la clave a leer
     * @return valor decimal configurado, o cero
     */
    private static double number(JsonNode node, String field) {
        return node.path(field).isNumber() ? node.path(field).doubleValue() : 0.0;
    }

    /**
     * Extrae una lista JSON de cadenas, descarta valores vacios y normaliza a minusculas.
     *
     * @param node nodo JSON que se espera que sea un array
     * @return lista inmutable de terminos; vacia si el nodo no es un array
     */
    private static List<String> strings(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(value -> {
                if (value.isTextual() && !value.asText().isBlank()) {
                    values.add(value.asText().toLowerCase(java.util.Locale.ROOT));
                }
            });
        }
        return List.copyOf(values);
    }
}