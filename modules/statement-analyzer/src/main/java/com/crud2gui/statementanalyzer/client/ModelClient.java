package com.crud2gui.statementanalyzer.client;

import java.io.IOException;
import java.util.List;

import com.crud2gui.statementanalyzer.model.ModelInfo;
import com.crud2gui.statementanalyzer.model.GenerationOptions;
import com.crud2gui.statementanalyzer.model.ModelResponse;

/**
 * Cliente de un modelo de lenguaje: sabe listar los modelos que ofrece y analizar un enunciado.
 *
 * <p>Cada proveedor (Gemini remoto, Ollama local, ...) implementa esta interfaz. El cliente trabaja
 * con textos ya leidos; la lectura de ficheros y la validacion del JSON devuelto son responsabilidad
 * de {@link StatementAnalyzer}, de modo que cambiar de proveedor no cambia nada mas.</p>
 */
public interface ModelClient {

    /**
     * Identificador corto del proveedor, por ejemplo {@code gemini} u {@code ollama}.
     *
     * @return nombre del proveedor en minusculas
     */
    String provider();

    /**
     * Consulta al proveedor los modelos disponibles para generar texto.
     *
     * @return modelos disponibles, con el identificador que hay que pasar a {@code generate}
     * @throws IOException          si el proveedor no responde o responde con un error
     * @throws InterruptedException si el hilo se interrumpe durante la espera
     */
    List<ModelInfo> listModels() throws IOException, InterruptedException;

    /**
     * Envia el prompt y el enunciado al modelo y devuelve el texto que genera.
     *
     * @param model     identificador del modelo, tal y como lo devuelve {@link #listModels()}
     * @param prompt    consulta que explica al modelo que debe hacer
     * @param statement texto plano del enunciado de la practica
     * @param options   parametros de generacion
     * @return la respuesta del modelo con sus metadatos
     * @throws IOException          si la peticion falla o la respuesta no se puede interpretar
     * @throws InterruptedException si el hilo se interrumpe durante la espera
     */
    ModelResponse generate(String model, String prompt, String statement, GenerationOptions options)
            throws IOException, InterruptedException;

    /**
     * Igual que {@link #generate(String, String, String, GenerationOptions)} con los parametros por defecto.
     *
     * @param model     identificador del modelo
     * @param prompt    consulta para el modelo
     * @param statement texto del enunciado
     * @return la respuesta del modelo
     * @throws IOException          si la peticion falla
     * @throws InterruptedException si el hilo se interrumpe
     */
    default ModelResponse generate(String model, String prompt, String statement)
            throws IOException, InterruptedException {
        return generate(model, prompt, statement, GenerationOptions.defaults());
    }
}
