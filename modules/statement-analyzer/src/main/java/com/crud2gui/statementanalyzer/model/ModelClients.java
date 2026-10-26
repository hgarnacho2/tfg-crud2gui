package com.crud2gui.statementanalyzer.model;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;

import com.crud2gui.statementanalyzer.client.ModelClient;
import com.crud2gui.statementanalyzer.client.ModelClientProvider;

/** Descubre proveedores y crea sus clientes. */
public final class ModelClients {

    private ModelClients() { }

    /**
     * Devuelve los proveedores registrados en el classpath.
     *
     * @return proveedores ordenados por nombre visible
     */
    public static List<ModelClientProvider> providers() {
        return ServiceLoader.load(ModelClientProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparing(ModelClientProvider::displayName))
                .toList();
    }

    /**
     * Busca un proveedor por su identificador, sin distinguir mayusculas.
     *
     * @param provider identificador corto del proveedor
     * @return proveedor registrado
     * @throws IllegalArgumentException si el proveedor no existe
     */
    public static ModelClientProvider provider(String provider) {
        String name = provider == null ? "" : provider.strip().toLowerCase(Locale.ROOT);
        List<ModelClientProvider> providers = providers();
        return providers.stream()
                .filter(candidate -> candidate.id().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Proveedor desconocido: '" + provider
                        + "' (disponibles: " + providers.stream().map(ModelClientProvider::id)
                                .collect(java.util.stream.Collectors.joining(", ")) + ")"));
    }

    /**
     * Crea el cliente de un proveedor con la configuracion del entorno.
     *
     * @param provider identificador corto del proveedor
     * @return el cliente
     * @throws IOException si falta la configuracion necesaria (por ejemplo la API key)
     * @throws IllegalArgumentException si el proveedor no existe
     */
    public static ModelClient forProvider(String provider) throws IOException {
        return provider(provider).createClient();
    }
}
