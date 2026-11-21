public class LanzadorEtapa2_2 {
    public static void main(String[] args) {
        System.out.println("-------------------------------");
        System.out.println("Creamos la plataforma de cursos");
        System.out.println("-------------------------------");
        PlataformaCursos gestor = new PlataformaCursos("Edubit");
        System.out.println("--------------------");
        System.out.println("Creamos instructores");
        System.out.println("--------------------");
        Instructor instructor1 = new Instructor("Marta Iglesias Roca","marta.iglesias@correo.es");
        Instructor instructor2 = new Instructor("Raúl Ortega Pons","raulortega@mimail.com");
        Instructor instructor3 = new Instructor("Nuria Campos Vega","nuriacampos@otromail.com");
        System.out.println("---------------------");
        System.out.println("Añadimos instructores");
        System.out.println("---------------------");
        gestor.addInstructor(instructor1);
        gestor.addInstructor(instructor2);
        gestor.addInstructor(instructor3);
        System.out.println("--------------");
        System.out.println("Creamos cursos");
        System.out.println("--------------");
        Curso curso1 = new Curso(1,"Introducción a Java desde cero",49,
                "Programación para principiantes. Incluye ejercicios.",T_Estado.ABIERTO);
        Curso curso2 = new Curso(2,"Diseño de bases de datos con SQL",79.9,
                "Modelado relacional y consultas SQL. Proyecto final.",T_Estado.ABIERTO);
        Curso curso3 = new Curso(3,"Fotografía digital para principiantes",35,
                "Domina la cámara y la composición. Edición en directo.",T_Estado.ABIERTO);
        Curso curso4 = new Curso(4,"Marketing digital y redes sociales",59.5,
                "Estrategias para pymes y emprendedores. Con plantillas.",T_Estado.ABIERTO);
        Curso curso5 = new Curso(5,"Python aplicado a la ciencia de datos",89,
                "Análisis de datos con pandas. Proyecto final real.",T_Estado.ABIERTO);
        System.out.println("--------------------------------------");
        System.out.println("Añadimos los cursos a los instructores");
        System.out.println("--------------------------------------");
        gestor.addCurso2Instructor(curso1,"marta.iglesias@correo.es");
        gestor.addCurso2Instructor(curso2,"raulortega@mimail.com");
        gestor.addCurso2Instructor(curso3,"nuriacampos@otromail.com");
        gestor.addCurso2Instructor(curso4,"marta.iglesias@correo.es");
        gestor.addCurso2Instructor(curso5,"nuriacampos@otromail.com");
        System.out.println("------------------------------");
        System.out.println("Imprimimos la info de un curso");
        System.out.println("------------------------------");
        curso2.print();
        System.out.println("-----------------------------------");
        System.out.println("Imprimimos la info de un instructor");
        System.out.println("-----------------------------------");
        instructor3.print();
        System.out.println("---------------------------------------------");
        System.out.println("Eliminamos un instructor a partir de su email");
        System.out.println("---------------------------------------------");
        gestor.removeInstructor("nuriacampos@otromail.com");
        System.out.println("-----------------------------------");
        System.out.println("Imprimimos la lista de instructores");
        System.out.println("-----------------------------------");
        gestor.printAllInstructores();
        System.out.println("----------------------------------------------");
        System.out.println("Imprimimos el total de cursos en la plataforma");
        System.out.println("----------------------------------------------");
        gestor.printAllCursos();
        System.out.println("-----------------------------------");
        System.out.println("Eliminamos un curso a un instructor");
        System.out.println("-----------------------------------");
        gestor.removeCurso2Instructor(1,instructor1.getEmail());
        gestor.printCursosByInstructor(instructor1.getEmail());
        System.out.println("---------------------------");
        System.out.println("Imprimimos todos los cursos");
        System.out.println("---------------------------");
        gestor.printAllCursos();
        // **************
        // PARTE OPCIONAL
        // **************
        curso2.setEstado(T_Estado.CERRADO);
        System.out.println("------------------------------");
        System.out.println("Imprimimos los cursos abiertos");
        System.out.println("------------------------------");
        gestor.printCursosByState(T_Estado.ABIERTO);
        System.out.println("------------------------------");
        System.out.println("Imprimimos los cursos cerrados");
        System.out.println("------------------------------");
        gestor.printCursosByState(T_Estado.CERRADO);
        System.out.println("-----------------------------------------------");
        System.out.println("Imprimimos los cursos abiertos de un instructor");
        System.out.println("-----------------------------------------------");
        gestor.printCursosByInstructor(instructor1.getEmail(),T_Estado.ABIERTO);
        System.out.println("---------------------------------------------------------");
        System.out.println("Imprimimos los cursos que contienen la palabra \"proyecto\"");
        System.out.println("---------------------------------------------------------");
        gestor.searchCursosByKeyword("proyecto");
    }
}
