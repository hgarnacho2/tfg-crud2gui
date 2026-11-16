package com.crud2gui.desktop.workflow;

/**
 * Representa el estado de preparación para operaciones CRUD del último informe de validación.
 * 
 * Cada estado indica si un modelo ha sido validado correctamente y si está listo
 * para ser utilizado en operaciones de crear, leer, actualizar y eliminar (CRUD).
 * 
 * Los estados se presentan con un nombre descriptivo para mostrar al usuario
 * y un símbolo visual que representa su estado.
 */
public enum ValidationStatus {
    /** El modelo aún no ha sido validado. Símbolo: – */
    NOT_VALIDATED("Pendiente de validación", "–"),
    
    /** El modelo no es válido y contiene errores que impiden su uso. Símbolo: × */
    INVALID("Modelo no válido", "×"),
    
    /** El modelo es válido pero está pendiente de revisión antes de usar para CRUD. Símbolo: ! */
    PENDING_REVIEW("Pendiente de revisión", "!"),
    
    /** El modelo es válido y está listo para ser utilizado en operaciones CRUD. Símbolo: ✓ */
    READY_FOR_CRUD("Listo para CRUD", "✓"),
    
    /** El estado del modelo no se puede determinar o no está disponible. Símbolo: ? */
    UNKNOWN("Estado no disponible", "?");

    /** Nombre descriptivo del estado para mostrar en la interfaz de usuario. */
    private final String displayName;
    
    /** Símbolo visual que representa visualmente el estado. */
    private final String symbol;

    /**
     * Construye un estado de validación.
     * 
     * @param displayName el nombre descriptivo a mostrar al usuario
     * @param symbol el símbolo visual que representa el estado
     */
    ValidationStatus(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    /**
     * Obtiene el nombre descriptivo del estado.
     * 
     * @return el nombre a mostrar en la interfaz de usuario
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Obtiene el símbolo visual del estado.
     * 
     * @return el símbolo que representa visualmente el estado
     */
    public String symbol() {
        return symbol;
    }

    /**
     * Determina el estado de validación basado en los resultados del informe.
     * 
     * Retorna {@link #INVALID} si el modelo no es válido.
     * Retorna {@link #READY_FOR_CRUD} si es válido y está listo para CRUD.
     * Retorna {@link #PENDING_REVIEW} si es válido pero pendiente de revisión.
     * 
     * @param valid {@code true} si el modelo pasó la validación; {@code false} en caso contrario
     * @param readyForCrud {@code true} si el modelo está listo para CRUD; {@code false} en caso contrario
     * @return el estado de validación correspondiente
     */
    public static ValidationStatus fromReport(boolean valid, boolean readyForCrud) {
        if (!valid) {
            return INVALID;
        }
        return readyForCrud ? READY_FOR_CRUD : PENDING_REVIEW;
    }
}