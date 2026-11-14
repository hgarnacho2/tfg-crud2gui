package com.crud2gui.desktop.workflow;

/** Estado de preparación para CRUD del último informe de validación. */
public enum ValidationStatus {
    NOT_VALIDATED("Pendiente de validación", "–"),
    INVALID("Modelo no válido", "×"),
    PENDING_REVIEW("Pendiente de revisión", "!"),
    READY_FOR_CRUD("Listo para CRUD", "✓"),
    UNKNOWN("Estado no disponible", "?");

    private final String displayName;
    private final String symbol;

    ValidationStatus(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String displayName() {
        return displayName;
    }

    public String symbol() {
        return symbol;
    }

    public static ValidationStatus fromReport(boolean valid, boolean readyForCrud) {
        if (!valid) {
            return INVALID;
        }
        return readyForCrud ? READY_FOR_CRUD : PENDING_REVIEW;
    }
}