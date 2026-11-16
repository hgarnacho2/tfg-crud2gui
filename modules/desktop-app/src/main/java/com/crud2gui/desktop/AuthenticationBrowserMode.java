package com.crud2gui.desktop;

import com.crud2gui.common.env.DotEnv;

import java.util.Locale;

/**
 * Selección del navegador usado para los flujos interactivos de autenticación en Entra ID.
 * 
 * Determina si la página de inicio de sesión de Microsoft se presenta dentro de la aplicación
 * (embebida en un componente JavaFX) o en el navegador predeterminado del sistema operativo.
 * Cada modo tiene implicaciones diferentes para la compatibilidad con SSO, MFA y políticas
 * de acceso condicional.
 */
public enum AuthenticationBrowserMode {
    /**
     * Modo embebido: la página de Microsoft se carga dentro de un {@code WebView} JavaFX.
     * 
     * <p><b>Características:</b>
     * <ul>
     *   <li>La autenticación permanece dentro de la ventana de CRUD2GUI.</li>
     *   <li>La experiencia es más integrada, sin cambiar de aplicación.</li>
     *   <li>Puede tener limitaciones con inicio de sesión único (SSO), autenticación multifactor (MFA)
     *       y políticas de acceso condicional del tenant.</li>
     *   <li>La validación de certificados SSL y el soporte de plugins del navegador
     *       dependen de la implementación de WebView en la JVM.</li>
     * </ul>
     * </p>
     */
    WEBVIEW,
    
    /**
     * Modo del sistema: la página de Microsoft se abre en el navegador predeterminado del SO.
     * 
     * <p><b>Características:</b>
     * <ul>
     *   <li>Utiliza el navegador nativo del usuario, con todas sus capacidades y configuración.</li>
     *   <li>Ofrece mejor compatibilidad con SSO, MFA y políticas de acceso condicional.</li>
     *   <li>El usuario puede aprovechar gestor de contraseñas, certificados cliente y extensiones del navegador.</li>
     *   <li>No permite observar ni confirmar remotamente que el navegador completó el cierre de sesión.</li>
     *   <li>El cierre de sesión en CRUD2GUI no puede garantizar que la sesión de Microsoft en el navegador
     *       también haya terminado.</li>
     * </ul>
     * </p>
     * 
     * <p><b>Aplicabilidad:</b>
     * Recomendado para entornos institucionales o de producción donde se requiere soporte
     * completo de características de seguridad avanzadas (MFA, acceso condicional).
     * </p>
     */
    SYSTEM;

    /**
     * Obtiene el modo de autenticación configurado en las variables de entorno.
     * 
     * <p>Consulta la variable {@code AZURE_AUTH_BROWSER}. Si no está definida,
     * utiliza {@code "webview"} como valor predeterminado.</p>
     * 
     * @return el modo configurado, {@link #WEBVIEW} por defecto
     * @throws IllegalArgumentException si el valor no es {@code "webview"} ni {@code "system"}
     */
    public static AuthenticationBrowserMode fromEnvironment() {
        return parse(DotEnv.getStringVar("AZURE_AUTH_BROWSER", "webview"));
    }

    /**
     * Analiza una cadena de texto y retorna el modo correspondiente.
     * 
     * <p>La cadena se normaliza: se eliminan espacios y se convierte a minúsculas
     * antes de hacer la comparación.</p>
     * 
     * @param value cadena con el nombre del modo ({@code "webview"} o {@code "system"})
     * @return el modo analizado
     * @throws IllegalArgumentException si el valor no se reconoce
     * @throws NullPointerException si value es {@code null}
     */
    static AuthenticationBrowserMode parse(String value) {
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "webview" -> WEBVIEW;
            case "system" -> SYSTEM;
            default -> throw new IllegalArgumentException(
                    "AZURE_AUTH_BROWSER debe ser 'webview' o 'system'.");
        };
    }
}