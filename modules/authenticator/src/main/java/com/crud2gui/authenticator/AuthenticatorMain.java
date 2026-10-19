package com.crud2gui.authenticator;

import java.util.concurrent.ExecutionException;

/** Lanzador de prueba para validar el inicio de sesion con la cuenta Entra del desarrollador. */
public final class AuthenticatorMain {

    private AuthenticatorMain() { }

    public static void main(String[] args) throws Exception {
        AuthenticatorConfig config = AuthenticatorConfig.fromEnvironment();
        EntraIdAuthenticator authenticator = new EntraIdAuthenticator(config);

        System.out.println("Se abrira el navegador para iniciar sesion en Microsoft Entra ID.");
        AuthenticatedUser user;
        try {
            user = authenticator.authenticate().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            System.err.println("No se pudo completar el inicio de sesion: " + cause.getMessage());
            System.err.println("Si MSAL indica que no recibio authorization code, confirma en Entra ID:");
            System.err.println("- Authentication > Mobile and desktop applications > redirect URI http://localhost.");
            System.err.println("- Que el navegador termino el inicio de sesion y volvio a localhost.");
            System.err.println("No compartas la URL final del navegador ni sus parametros.");
            System.exit(1);
            return;
        }

        System.out.println("Autenticacion completada.");
        System.out.println("Nombre: " + valueOrFallback(user.displayName(), "no disponible"));
        System.out.println("Usuario: " + valueOrFallback(user.username(), "no disponible"));
        System.out.println("Tenant: " + valueOrFallback(user.tenantId(), "no disponible"));
        System.out.println("Roles: " + (user.roles().isEmpty() ? "ninguno" : String.join(", ", user.roles())));
        System.out.println("Rol alumno: " + user.isAlumno());
        System.out.println("Rol docente: " + user.isDocente());
        if (user.roles().isEmpty()) {
            System.out.println("AVISO: comprueba la asignacion del rol en Enterprise applications > Users and groups.");
        }
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
