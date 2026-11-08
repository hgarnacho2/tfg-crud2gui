package com.crud2gui.desktop.workflow;

import java.nio.file.Path;

/**
 * Estado que comparten los flujos de extracción de código y validación de modelos.
 *
 * <p>Las implementaciones conservan las rutas seleccionadas y determinan qué modelos
 * dependientes deben invalidarse cuando cambia una entrada. Las rutas que devuelven
 * los métodos de consulta solo se consideran disponibles mientras apunten a archivos
 * regulares y legibles.</p>
 */
public interface WorkflowContext {

    /**
     * Registra la carpeta de código fuente activa.
     *
     * <p>Al cambiar la carpeta, la implementación puede invalidar modelos generados
     * o selecciones que dependan de la carpeta anterior.</p>
     *
     * @param path carpeta fuente, o {@code null} para quitarla
     */
    void selectCodeFolder(Path path);

    /**
     * Obtiene la carpeta de código fuente seleccionada y guardada.
     *
     * @return ruta de la carpeta guardada, o {@code null} si no hay ninguna disponible
     */
    Path getSelectedCodeFolder();

    /**
     * Registra el modelo JSON obtenido al analizar código fuente.
     * Si no hay una selección Java manual, queda disponible para la validación.
     *
     * @param path ruta del modelo generado, o {@code null} para quitar el modelo generado
     */
    void recordCodeModel(Path path);

    /**
     * Obtiene el modelo Java seleccionado para la validación o su sugerencia disponible.
     *
     * @return ruta absoluta de un archivo regular y legible, o {@code null} si no hay uno
     */
    Path getSelectedJavaModel();

    /**
     * Indica si el modelo Java se controla mediante una selección manual.
     *
     * @return {@code true} si la selección manual prevalece sobre la sugerencia generada
     */
    boolean isJavaModelManual();

    /**
     * Guarda la selección manual del modelo Java para la validación.
     *
     * <p>Usar {@code null} elimina la ruta seleccionada, pero mantiene el estado manual
     * para que una sugerencia automática no la sustituya.</p>
     *
     * @param path archivo JSON seleccionado, o {@code null} para quitar la selección
     */
    void selectJavaModel(Path path);

    /**
     * Obtiene el modelo del enunciado seleccionado para la validación o su sugerencia disponible.
     *
     * @return ruta absoluta de un archivo regular y legible, o {@code null} si no hay uno
     */
    Path getSelectedInferredModel();

    /**
     * Indica si el modelo del enunciado se controla mediante una selección manual.
     *
     * @return {@code true} si la selección manual prevalece sobre la sugerencia disponible
     */
    boolean isInferredModelManual();

    /**
     * Guarda la selección manual del modelo del enunciado para la validación.
     *
     * <p>Usar {@code null} elimina la ruta seleccionada, pero mantiene el estado manual
     * para que una sugerencia automática no la sustituya.</p>
     *
     * @param path archivo JSON seleccionado, o {@code null} para quitar la selección
     */
    void selectInferredModel(Path path);

    /** Registra el JSON generado por la última validación, o {@code null} para invalidarlo. */
    void recordValidationReport(Path path);

    /** @return informe de validación disponible o {@code null} si todavía no se ha generado */
    Path getValidationReport();
}