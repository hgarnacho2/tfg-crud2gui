package com.crud2gui.desktop;

import com.crud2gui.common.env.DotEnv;

import java.util.Locale;

/** Selección del navegador usado para los flujos interactivos de Entra ID. */
public enum AuthenticationBrowserMode {
    WEBVIEW,
    SYSTEM;

    public static AuthenticationBrowserMode fromEnvironment() {
        return parse(DotEnv.getStringVar("AZURE_AUTH_BROWSER", "webview"));
    }

    static AuthenticationBrowserMode parse(String value) {
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "webview" -> WEBVIEW;
            case "system" -> SYSTEM;
            default -> throw new IllegalArgumentException(
                    "AZURE_AUTH_BROWSER debe ser 'webview' o 'system'.");
        };
    }
}