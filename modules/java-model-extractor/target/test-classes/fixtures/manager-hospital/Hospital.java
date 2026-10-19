package fixtures.hospital;

import java.util.HashMap;
import java.util.Map;

class Hospital {
    private final Map<String, Paciente> pacientes = new HashMap<>();

    void registrar(Paciente paciente) { pacientes.put(paciente.getId(), paciente); }
    Paciente buscar(String id) { return pacientes.get(id); }
}

class Paciente {
    String getId() { return "id"; }
}