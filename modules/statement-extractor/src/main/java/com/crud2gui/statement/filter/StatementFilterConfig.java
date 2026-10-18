package com.crud2gui.statement.filter;

import java.util.List;
import java.util.Objects;

/**
 * Configuración independiente de la interfaz para decidir cómo tratar las
 * secciones de un enunciado.
 *
 * <p>Los títulos se comparan sin distinguir mayúsculas, tildes ni numeración
 * inicial. Las secciones que no tengan una regla explícita se incluyen por
 * defecto. Debe definirse al menos una regla {@link SectionAction#START} y una
 * {@link SectionAction#STOP}.</p>
 *
 * @param sectionRules reglas de título y acción, en el orden en que se evalúan
 * @param maxConsecutiveBlankLines máximo de líneas vacías consecutivas que se conservan
 * @param removePageNumbers indica si se descartan las líneas que solo contienen números
 */
public record StatementFilterConfig(
        List<SectionRule> sectionRules,
        int maxConsecutiveBlankLines,
        boolean removePageNumbers) {

    /**
     * Valida y copia la configuración para impedir modificaciones posteriores
     * de la lista de reglas.
     *
     * @throws NullPointerException si la lista de reglas o alguna acción es nula
     * @throws IllegalArgumentException si el máximo es negativo o faltan reglas START/STOP
     */
    public StatementFilterConfig {
        sectionRules = List.copyOf(Objects.requireNonNull(sectionRules, "sectionRules"));
        if (maxConsecutiveBlankLines < 0) {
            throw new IllegalArgumentException("maxConsecutiveBlankLines no puede ser negativo");
        }

        boolean hasStart = false;
        boolean hasStop = false;
        for (SectionRule rule : sectionRules) {
            if (rule.action() == SectionAction.START) {
                hasStart = true;
            } else if (rule.action() == SectionAction.STOP) {
                hasStop = true;
            }
        }
        if (!hasStart || !hasStop) {
            throw new IllegalArgumentException("Debe existir al menos una regla START y una STOP");
        }
    }

    /**
     * Crea la configuración predeterminada para los enunciados UNED de ejemplo.
     *
     * @return reglas para iniciar en «Introducción», ignorar «Requisitos previos»
     *         y detenerse en «Fechas y normas de entrega»
     */
    public static StatementFilterConfig defaults() {
        return new StatementFilterConfig(List.of(
                new SectionRule("Introducción", SectionAction.START),
                new SectionRule("Requisitos previos", SectionAction.IGNORE),
                new SectionRule("Fechas y normas de entrega", SectionAction.STOP)),
                StatementFilter.MAX_CONSECUTIVE_BLANK_LINES,
                true);
    }

    /**
     * Título de sección y acción que debe aplicar el filtro al encontrarlo.
     *
     * @param heading título que identifica la sección, sin numeración
     * @param action acción a aplicar al encontrar el título
     */
    public record SectionRule(String heading, SectionAction action) {
        public SectionRule {
            if (heading == null || heading.isBlank()) {
                throw new IllegalArgumentException("El título de sección no puede estar vacío");
            }
            Objects.requireNonNull(action, "action");
        }
    }

    /** Acción que el filtro aplica al encontrar el título de una regla. */
    public enum SectionAction {
        /** Empieza a conservar texto e incluye el título encontrado. */
        START,
        /** Conserva el título y el contenido de la sección. */
        INCLUDE,
        /** Omite el título y el contenido hasta la siguiente sección reconocida. */
        IGNORE,
        /** Termina el cuerpo conservado sin incluir este título. */
        STOP
    }
}