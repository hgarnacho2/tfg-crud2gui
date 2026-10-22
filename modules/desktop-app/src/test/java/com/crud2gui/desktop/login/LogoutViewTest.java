package com.crud2gui.desktop.login;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogoutViewTest {

    @Test
    void acceptsBrowserNormalizedLoopbackReturn() {
        assertTrue(LogoutView.matchesRedirect(URI.create("http://localhost"),
                URI.create("http://localhost/")));
        assertTrue(LogoutView.matchesRedirect(URI.create("http://localhost"),
                URI.create("http://localhost:80/")));
        assertTrue(LogoutView.matchesRedirect(URI.create("http://localhost:8765/callback"),
                URI.create("http://localhost:8765/callback")));
    }

    @Test
    void rejectsMicrosoftIntermediatePagesAndDifferentCallbacks() {
        URI expected = URI.create("http://localhost");
        for (String location : new String[] {
                "https://login.microsoftonline.com/common/oauth2/v2.0/logout",
                "http://localhost:8765/", "http://localhost/other", "http://localhost/?error=failure",
                "http://localhost/#error", "http://localhost.example/", "http://user@localhost/",
                "https://localhost/", "about:blank", ""}) {
            assertFalse(LogoutView.matchesRedirect(expected, URI.create(location)), location);
        }
    }
}
