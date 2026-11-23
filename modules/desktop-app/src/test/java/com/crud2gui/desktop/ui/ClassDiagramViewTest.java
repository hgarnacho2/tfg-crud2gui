package com.crud2gui.desktop.ui;

import com.crud2gui.model.Relation;
import com.crud2gui.model.RelationKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassDiagramViewTest {

    @Test
    void labelsInheritanceInSpanishAndPreservesRelationDetails() {
        assertEquals("Herencia", ClassDiagramView.relationLabel(
                new Relation(RelationKind.INHERITANCE, "child", "parent", null, null, null, null)));
        assertEquals("productos  0..*", ClassDiagramView.relationLabel(
                new Relation(RelationKind.ASSOCIATION, "owner", "product", "productos", "0..*", null, null)));
    }
}
