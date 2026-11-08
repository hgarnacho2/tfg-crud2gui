package com.crud2gui.desktop.student.sections;

import com.crud2gui.desktop.student.StudentWorkflowContext;

/** Sección del alumno para validar los modelos de código y del enunciado. */
public final class ModelValidationSection extends com.crud2gui.desktop.teacher.sections.ModelValidationSection {

    public ModelValidationSection(StudentWorkflowContext workflow) {
        super("Validar modelos", workflow);
    }
}
