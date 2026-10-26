package com.crud2gui.statementanalyzer.client;

import java.io.IOException;

/** Metadatos y factoría ligera de un proveedor de modelos de lenguaje. */
public interface ModelClientProvider {

    /** @return identificador estable del proveedor */
    String id();

    /** @return nombre visible en la interfaz */
    String displayName();

    /**
     * Crea el cliente configurado para el usuario actual.
     *
     * @return cliente del proveedor
     * @throws IOException si falta o no se puede leer su configuración
     */
    ModelClient createClient() throws IOException;
}