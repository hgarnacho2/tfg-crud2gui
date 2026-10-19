package fixtures.universidad;

import java.util.ArrayList;
import java.util.List;

class University {
    private final List<Faculty> faculties = new ArrayList<>();
    void addFaculty(Faculty faculty) { faculties.add(faculty); }
}

class Faculty {
    private final List<Student> students = new ArrayList<>();
    void enroll(Student student) { students.add(student); }
}

class Student { }