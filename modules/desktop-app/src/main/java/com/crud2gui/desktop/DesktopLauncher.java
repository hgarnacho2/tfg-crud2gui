package com.crud2gui.desktop;

import com.crud2gui.common.DotEnv;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

/** Punto de entrada del paquete portable, independiente del lanzador especial de JavaFX. */
public final class DesktopLauncher {

    private DesktopLauncher() { }

    /**
     * Carga la configuración externa junto al JAR y arranca la interfaz.
     * Las variables de entorno y propiedades Java explícitas conservan su prioridad.
     *
     * @param args argumentos de la aplicación
     * @throws IOException si no se puede leer la configuración
     * @throws URISyntaxException si la ubicación del lanzador no es una URI válida
     */
    public static void main(String[] args) throws IOException, URISyntaxException {
        Path applicationDirectory = Path.of(DesktopLauncher.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getParent();
        Path config = applicationDirectory.resolve("config");
        DotEnv.load(config.resolve(".env.public"), config.resolve(".env.private"))
                .forEach((key, value) -> System.getProperties().putIfAbsent(key, value));
        Crud2GuiDesktopApp.main(args);
    }
}
