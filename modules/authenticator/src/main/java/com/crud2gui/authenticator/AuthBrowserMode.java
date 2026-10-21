package com.crud2gui.authenticator;

/** Interfaz usada para completar el inicio de sesión interactivo. */
public enum AuthBrowserMode {
    SYSTEM,
    EMBEDDED;

    public static AuthBrowserMode parse(String value) {
        if (value == null || value.isBlank() || value.strip().equalsIgnoreCase("system")) {
            return SYSTEM;
        }
        if (value.strip().equalsIgnoreCase("embedded")) {
            return EMBEDDED;
        }
        throw new IllegalArgumentException("AZURE_AUTH_BROWSER_MODE debe ser 'system' o 'embedded'.");
    }
}