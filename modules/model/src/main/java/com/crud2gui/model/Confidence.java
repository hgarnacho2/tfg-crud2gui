package com.crud2gui.model;

/**
 * Fiabilidad de un resultado obtenido mediante heuristicas.
 */
public enum Confidence {
    /** Resultado claro y bien respaldado por la evidencia. */
    HIGH,
    /** Resultado probable, con algun indicio contradictorio o ambiguo. */
    MEDIUM,
    /** Resultado dudoso; conviene revisarlo manualmente. */
    LOW
}
