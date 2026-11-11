package com.crud2gui.desktop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticationBrowserModeTest {

    @Test
    void parsesSupportedModesWithoutCaseSensitivity() {
        assertEquals(AuthenticationBrowserMode.WEBVIEW, AuthenticationBrowserMode.parse("webview"));
        assertEquals(AuthenticationBrowserMode.SYSTEM, AuthenticationBrowserMode.parse("SYSTEM"));
    }

    @Test
    void rejectsUnsupportedModes() {
        assertThrows(IllegalArgumentException.class, () -> AuthenticationBrowserMode.parse("embedded"));
    }
}