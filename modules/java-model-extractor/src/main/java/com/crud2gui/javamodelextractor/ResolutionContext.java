package com.crud2gui.javamodelextractor;

import java.util.List;
import java.util.Map;

/**
 * Datos de un fichero necesarios para resolver los nombres de tipo que aparecen en el.
 *
 * @param packageName     paquete del fichero; cadena vacia en el paquete por defecto
 * @param explicitImports imports explicitos: nombre simple a nombre completo ({@code import a.b.C})
 * @param wildcardImports paquetes importados con asterisco ({@code import a.b.*})
 * @param enclosing       ids de los tipos que contienen el codigo, del mas interno al mas externo
 */
record ResolutionContext(String packageName, Map<String, String> explicitImports,
                         List<String> wildcardImports, List<String> enclosing) {

    /**
     * Devuelve una copia del contexto con otra cadena de tipos contenedores.
     *
     * @param newEnclosing nuevos ids contenedores, del mas interno al mas externo
     * @return el contexto actualizado; el original no se modifica
     */
    ResolutionContext withEnclosing(List<String> newEnclosing) {
        return new ResolutionContext(packageName, explicitImports, wildcardImports, newEnclosing);
    }
}
