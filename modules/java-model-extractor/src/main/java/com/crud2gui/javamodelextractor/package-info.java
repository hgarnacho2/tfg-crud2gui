/**
 * Analiza codigo fuente Java de una carpeta y construye un {@link com.crud2gui.model.ProjectModel}
 * con los tipos declarados, sus miembros, sus relaciones y el resultado de la deteccion de la clase
 * gestora.
 *
 * <p>{@link com.crud2gui.javamodelextractor.ProjectParser} es la API principal para analizar un
 * proyecto. {@link com.crud2gui.javamodelextractor.Main} ofrece la entrada de linea de comandos y
 * guarda el modelo resultante en JSON.</p>
 *
 * <p>El analisis busca los ficheros fuente, extrae sus tipos y miembros, resuelve referencias entre
 * tipos, deriva relaciones UML y detecta heuristicamente la clase gestora. La busqueda de ficheros,
 * la extraccion y resolucion de tipos, y el analisis de relaciones y de la clase gestora se delegan
 * en clases especializadas del paquete.</p>
 */
package com.crud2gui.javamodelextractor;
