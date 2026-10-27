package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.model.MethodInfo;
import com.crud2gui.model.Modifier;
import com.crud2gui.model.ProjectModel;
import com.crud2gui.model.Relation;
import com.crud2gui.model.RelationKind;
import com.crud2gui.model.TypeInfo;
import com.crud2gui.model.TypeKind;
import com.crud2gui.model.TypeRef;
import com.crud2gui.model.Visibility;
import javafx.scene.Cursor;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Vista desplazable del diagrama UML del modelo inferido. */
public final class ClassDiagramView extends Pane {

    private static final double CARD_WIDTH = 370;
    private static final double COLUMN_GAP = 180;
    private static final double ROW_GAP = 90;
    private static final double CANVAS_MARGIN = 120;

    private final Canvas relationships = new Canvas();
    private final Map<String, VBox> cards = new LinkedHashMap<>();
    private final Map<String, Point2D> centers = new HashMap<>();
    private final Map<String, Point2D> draggedCenters = new HashMap<>();
    private final Map<String, Integer> layers = new HashMap<>();
    private ProjectModel model;
    private List<TypeInfo> diagramTypes = List.of();
    private List<Relation> diagramRelations = List.of();
    private String managerTypeId;

    public ClassDiagramView() {
        getStyleClass().add("uml-diagram");
        getChildren().add(relationships);
        setMinSize(1000, 700);
        setPrefSize(1500, 1000);
    }

    /** Sustituye el modelo dibujado y reconstruye sus tarjetas y relaciones. */
    public void setModel(ProjectModel model) {
        String detectedManagerId = model == null || model.analysis() == null
                || model.analysis().managerClass() == null
                ? null : model.analysis().managerClass().id();
        String selectedManagerId = model == null || model.expectedManagerId() == null
                ? detectedManagerId : model.expectedManagerId();
        setModel(model, selectedManagerId);
    }

    /** Sustituye el modelo dibujado y fija la clase gestora que debe resaltarse. */
    public void setModel(ProjectModel model, String selectedManagerId) {
        this.model = model;
        cards.clear();
        centers.clear();
        draggedCenters.clear();
        layers.clear();
        getChildren().setAll(relationships);
        diagramTypes = model == null ? List.of() : model.types().stream()
                .filter(type -> !type.hasMainMethod())
                .toList();
        Set<String> visibleTypeIds = diagramTypes.stream()
                .map(TypeInfo::id)
                .collect(java.util.stream.Collectors.toSet());
        diagramRelations = filterDiagramRelations(model, visibleTypeIds);
        managerTypeId = visibleTypeIds.contains(selectedManagerId) ? selectedManagerId : null;
        if (diagramTypes.isEmpty()) {
            setMinSize(1000, 700);
            setPrefSize(1500, 1000);
            requestLayout();
            return;
        }

        layers.putAll(calculateLayers(diagramTypes, diagramRelations, managerTypeId));
        int cardIndex = 0;
        for (TypeInfo type : orderedTypes()) {
            boolean manager = Objects.equals(type.id(), managerTypeId);
            VBox card = createCard(type, manager);
            card.setId("uml-class-card-" + cardIndex++);
            enableDragging(card, type.id());
            cards.put(type.id(), card);
            getChildren().add(card);
        }
        updateCanvasSize();
        requestLayout();
    }

    public ProjectModel getModel() {
        return model;
    }

    static List<Relation> filterDiagramRelations(ProjectModel model, Set<String> visibleTypeIds) {
        if (model == null) {
            return List.of();
        }
        Map<String, TypeInfo> typesById = model.types().stream()
                .collect(java.util.stream.Collectors.toMap(TypeInfo::id, type -> type));
        return model.relations().stream()
                .filter(relation -> visibleTypeIds.contains(relation.from())
                        && visibleTypeIds.contains(relation.to()))
                .filter(relation -> {
                    TypeInfo from = typesById.get(relation.from());
                    TypeInfo to = typesById.get(relation.to());
                    return relation.kind() != RelationKind.DEPENDENCY
                            || (from.kind() != TypeKind.ENUM && to.kind() != TypeKind.ENUM);
                })
                .toList();
    }

    private List<TypeInfo> orderedTypes() {
        return diagramTypes.stream()
                .sorted(Comparator.comparingInt((TypeInfo type) -> layers.getOrDefault(type.id(), 0))
                        .thenComparing(type -> type.name() == null ? type.id() : type.name(),
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static Map<String, Integer> calculateLayers(
            List<TypeInfo> types, List<Relation> relations, String managerId) {
        Map<String, List<String>> adjacent = new LinkedHashMap<>();
        for (TypeInfo type : types) {
            adjacent.put(type.id(), new ArrayList<>());
        }
        for (Relation relation : relations) {
            if (relation.from() == null || relation.to() == null
                    || relation.kind() == RelationKind.DEPENDENCY
                    || !adjacent.containsKey(relation.from()) || !adjacent.containsKey(relation.to())) {
                continue;
            }
            adjacent.get(relation.from()).add(relation.to());
            adjacent.get(relation.to()).add(relation.from());
        }

        Map<String, Integer> result = new HashMap<>();
        if (managerId != null) {
            result.put(managerId, 0);
            propagateLayers(new ArrayList<>(List.of(managerId)), adjacent, result);
        }
        for (TypeInfo type : types) {
            if (!result.containsKey(type.id())) {
                result.put(type.id(), 0);
                propagateLayers(new ArrayList<>(List.of(type.id())), adjacent, result);
            }
        }
        return result;
    }

    private static void propagateLayers(List<String> queue, Map<String, List<String>> adjacent,
                                        Map<String, Integer> layers) {
        ArrayDeque<String> pending = new ArrayDeque<>(queue);
        while (!pending.isEmpty()) {
            String source = pending.removeFirst();
            for (String target : adjacent.getOrDefault(source, List.of())) {
                if (!layers.containsKey(target)) {
                    layers.put(target, layers.get(source) + 1);
                    pending.addLast(target);
                }
            }
        }
    }

    private VBox createCard(TypeInfo type, boolean manager) {
        VBox card = new VBox(0);
        card.getStyleClass().addAll("uml-class-card", "uml-kind-" + type.kind().name().toLowerCase());
        if (manager) {
            card.getStyleClass().add("uml-manager-card");
        }
        card.setMinWidth(CARD_WIDTH);
        card.setPrefWidth(CARD_WIDTH);
        card.setMaxWidth(CARD_WIDTH);

        HBox header = new HBox(12);
        header.getStyleClass().add("uml-class-header");
        VBox title = new VBox(4);
        HBox.setHgrow(title, Priority.ALWAYS);
        Label stereotype = new Label(stereotype(type));
        stereotype.getStyleClass().add("uml-stereotype");
        Label name = new Label(type.name() == null ? type.id() : type.name());
        name.getStyleClass().add("uml-class-name");
        if (type.modifiers().contains(Modifier.ABSTRACT)) {
            name.getStyleClass().add("uml-abstract-name");
        }
        title.getChildren().addAll(stereotype, name);
        if (type.packageName() != null && !type.packageName().isBlank()) {
            Label packageName = new Label(type.packageName());
            packageName.getStyleClass().add("uml-package-name");
            title.getChildren().add(packageName);
        }
        header.getChildren().add(title);
        if (manager) {
            Label badge = new Label("GESTORA");
            badge.getStyleClass().add("uml-manager-badge");
            header.getChildren().add(badge);
        }
        card.getChildren().add(header);

        if (type.kind() == TypeKind.ENUM) {
            card.getChildren().add(section("Valores", type.enumConstants().stream()
                    .map(constant -> constant.name()).toList()));
        }
        card.getChildren().add(section("Atributos", type.fields().stream()
                .map(field -> visibilitySymbol(field.visibility()) + modifierPrefix(field.modifiers())
                        + field.name() + " : " + display(field.type()) + optionalSuffix(field.optional()))
                .toList()));
        card.getChildren().add(new Separator());
        card.getChildren().add(section("Constructores", constructorLines(type)));
        card.getChildren().add(new Separator());
        card.getChildren().add(section("Métodos", methodLines(type.methods())));
        return card;
    }

    private static String stereotype(TypeInfo type) {
        String optionalPrefix = type.optional() ? "<<opcional>> " : "";
        if (type.modifiers().contains(Modifier.ABSTRACT)) {
            return optionalPrefix + "<<abstract class>>";
        }
        return optionalPrefix + switch (type.kind()) {
            case INTERFACE -> "<<interface>>";
            case ENUM -> "<<enumeration>>";
            case RECORD -> "<<record>>";
            case ANNOTATION -> "<<annotation>>";
            case CLASS -> "<<class>>";
        };
    }

    private static VBox section(String titleText, List<String> lines) {
        VBox section = new VBox(5);
        section.getStyleClass().add("uml-class-section");
        Label title = new Label(titleText);
        title.getStyleClass().add("uml-section-title");
        section.getChildren().add(title);
        if (lines.isEmpty()) {
            Label empty = new Label("-");
            empty.getStyleClass().add("uml-member-empty");
            section.getChildren().add(empty);
        } else {
            for (String line : lines) {
                Label member = new Label(line);
                member.getStyleClass().add("uml-member-line");
                member.setWrapText(true);
                member.setMaxWidth(Double.MAX_VALUE);
                section.getChildren().add(member);
            }
        }
        return section;
    }

    private static List<String> constructorLines(TypeInfo type) {
        return type.constructors().stream().map(constructor ->
                visibilitySymbol(constructor.visibility())
                        + (type.name() == null ? type.id() : type.name())
                        + "(" + parameters(constructor.parameters()) + ")")
                .toList();
    }

    private static List<String> methodLines(List<MethodInfo> methods) {
        return methods.stream().map(method -> visibilitySymbol(method.visibility())
                + modifierPrefix(method.modifiers()) + method.name()
                + "(" + parameters(method.parameters()) + ") : " + display(method.returnType())
                + optionalSuffix(method.optional())).toList();
    }

    private static String optionalSuffix(boolean optional) {
        return optional ? " «opcional»" : "";
    }

    private static String parameters(List<com.crud2gui.model.ParameterInfo> parameters) {
        return parameters.stream().map(parameter -> display(parameter.type())
                        + (parameter.varargs() ? "..." : "")
                        + (parameter.name() == null || parameter.name().isBlank() ? "" : " " + parameter.name()))
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String display(TypeRef type) {
        return type == null ? "?" : type.display();
    }

    private static String modifierPrefix(List<Modifier> modifiers) {
        return modifiers.stream().map(modifier -> modifier.name().toLowerCase() + " ")
                .collect(java.util.stream.Collectors.joining());
    }

    private static String visibilitySymbol(Visibility visibility) {
        if (visibility == null) {
            return "? ";
        }
        return switch (visibility) {
            case PUBLIC -> "+ ";
            case PROTECTED -> "# ";
            case PACKAGE -> "~ ";
            case PRIVATE -> "- ";
        };
    }

    private void updateCanvasSize() {
        Map<Integer, List<String>> columns = idsByLayer();
        double maxColumnHeight = columns.values().stream()
            .mapToDouble(this::columnHeight)
            .max().orElse(0);
        double width = Math.max(1200,
            CANVAS_MARGIN * 2 + columns.size() * CARD_WIDTH + Math.max(0, columns.size() - 1) * COLUMN_GAP);
        double height = Math.max(850, CANVAS_MARGIN * 2 + maxColumnHeight);
        setMinSize(width, height);
        setPrefSize(width, height);
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth() > 0 ? getWidth() : getPrefWidth();
        double height = getHeight() > 0 ? getHeight() : getPrefHeight();
        relationships.setWidth(width);
        relationships.setHeight(height);
        if (model == null || cards.isEmpty()) {
            return;
        }

        centers.clear();
        Map<Integer, List<String>> columns = idsByLayer();
        for (Map.Entry<Integer, List<String>> column : columns.entrySet()) {
            double totalHeight = columnHeight(column.getValue());
            double y = (height - totalHeight) / 2;
            double x = CANVAS_MARGIN + CARD_WIDTH / 2
                    + column.getKey() * (CARD_WIDTH + COLUMN_GAP);
            for (String id : column.getValue()) {
                VBox card = cards.get(id);
                double cardHeight = cardHeight(card);
                Point2D defaultCenter = new Point2D(x, y + cardHeight / 2);
                Point2D selectedCenter = draggedCenters.getOrDefault(id, defaultCenter);
                Point2D positionedCenter = placeCard(card, selectedCenter);
                centers.put(id, positionedCenter);
                y += cardHeight + ROW_GAP;
            }
        }
        drawRelationships(width, height);
    }

    private Map<Integer, List<String>> idsByLayer() {
        Map<Integer, List<String>> columns = new java.util.TreeMap<>();
        for (TypeInfo type : orderedTypes()) {
            columns.computeIfAbsent(layers.getOrDefault(type.id(), 0), ignored -> new ArrayList<>())
                    .add(type.id());
        }
        return columns;
    }

    private double columnHeight(List<String> ids) {
        double height = 0;
        for (String id : ids) {
            height += cardHeight(cards.get(id));
        }
        return height + Math.max(0, ids.size() - 1) * ROW_GAP;
    }

    private static Point2D placeCard(VBox card, Point2D center) {
        card.applyCss();
        double cardHeight = cardHeight(card);
        double x = center.getX() - CARD_WIDTH / 2;
        double y = center.getY() - cardHeight / 2;
        card.resize(CARD_WIDTH, cardHeight);
        card.relocate(x, y);
        return new Point2D(x + CARD_WIDTH / 2, y + cardHeight / 2);
    }

    private static double cardHeight(VBox card) {
        if (card == null) {
            return 420;
        }
        card.applyCss();
        return Math.max(320, card.prefHeight(CARD_WIDTH));
    }

    private void enableDragging(VBox card, String typeId) {
        double[] dragStart = new double[4];
        card.setCursor(Cursor.OPEN_HAND);
        card.setOnMousePressed(event -> {
            dragStart[0] = event.getSceneX();
            dragStart[1] = event.getSceneY();
            dragStart[2] = card.getLayoutX();
            dragStart[3] = card.getLayoutY();
            card.setCursor(Cursor.CLOSED_HAND);
            event.consume();
        });
        card.setOnMouseDragged(event -> {
            double maxX = Math.max(CANVAS_MARGIN, getWidth() - CARD_WIDTH - CANVAS_MARGIN);
            double maxY = Math.max(CANVAS_MARGIN, getHeight() - card.getHeight() - CANVAS_MARGIN);
            double x = clamp(dragStart[2] + event.getSceneX() - dragStart[0], CANVAS_MARGIN, maxX);
            double y = clamp(dragStart[3] + event.getSceneY() - dragStart[1], CANVAS_MARGIN, maxY);
            card.relocate(x, y);
            Point2D center = new Point2D(x + CARD_WIDTH / 2, y + card.getHeight() / 2);
            centers.put(typeId, center);
            draggedCenters.put(typeId, center);
            drawRelationships(getWidth(), getHeight());
            event.consume();
        });
        card.setOnMouseReleased(event -> card.setCursor(Cursor.OPEN_HAND));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private void drawRelationships(double width, double height) {
        GraphicsContext graphics = relationships.getGraphicsContext2D();
        graphics.clearRect(0, 0, width, height);
        graphics.setFont(Font.font("Segoe UI", 12));
        for (Relation relation : diagramRelations) {
            Point2D fromCenter = centers.get(relation.from());
            Point2D toCenter = centers.get(relation.to());
            VBox fromCard = cards.get(relation.from());
            VBox toCard = cards.get(relation.to());
            if (fromCenter == null || toCenter == null || fromCard == null || toCard == null) {
                continue;
            }
            drawRelation(graphics, relation, fromCenter, toCenter, fromCard.getHeight(), toCard.getHeight());
        }
    }

    private static void drawRelation(GraphicsContext graphics, Relation relation,
                                     Point2D fromCenter, Point2D toCenter,
                                     double fromHeight, double toHeight) {
        double dx = toCenter.getX() - fromCenter.getX();
        double dy = toCenter.getY() - fromCenter.getY();
        double distance = Math.max(1, Math.hypot(dx, dy));
        double unitX = dx / distance;
        double unitY = dy / distance;
        double startScale = boundaryScale(unitX, unitY, CARD_WIDTH / 2, fromHeight / 2);
        double endScale = boundaryScale(unitX, unitY, CARD_WIDTH / 2, toHeight / 2);
        double startX = fromCenter.getX() + unitX * startScale;
        double startY = fromCenter.getY() + unitY * startScale;
        double endX = toCenter.getX() - unitX * endScale;
        double endY = toCenter.getY() - unitY * endScale;

        Color color = relationColor(relation.kind());
        graphics.setStroke(color);
        graphics.setFill(color);
        graphics.setLineWidth(relation.kind() == RelationKind.COMPOSITION ? 2.2 : 1.8);
        if (relation.kind() == RelationKind.DEPENDENCY || relation.kind() == RelationKind.REALIZATION) {
            graphics.setLineDashes(8, 6);
        } else {
            graphics.setLineDashes();
        }
        if (relation.kind() == RelationKind.COMPOSITION || relation.kind() == RelationKind.AGGREGATION) {
            drawDiamond(graphics, startX, startY, unitX, unitY,
                    relation.kind() == RelationKind.COMPOSITION);
        }
        graphics.strokeLine(startX, startY, endX, endY);
        graphics.setLineDashes();
        if (relation.kind() == RelationKind.INHERITANCE || relation.kind() == RelationKind.REALIZATION) {
            drawTriangle(graphics, endX, endY, unitX, unitY);
        } else if (relation.kind() == RelationKind.DEPENDENCY) {
            drawOpenArrow(graphics, endX, endY, unitX, unitY);
        }
        drawRelationLabel(graphics, relation, fromCenter, toCenter, color);
    }

    private static double boundaryScale(double unitX, double unitY, double halfWidth, double halfHeight) {
        double xScale = Math.abs(unitX) < 0.0001 ? Double.POSITIVE_INFINITY : halfWidth / Math.abs(unitX);
        double yScale = Math.abs(unitY) < 0.0001 ? Double.POSITIVE_INFINITY : halfHeight / Math.abs(unitY);
        return Math.min(xScale, yScale);
    }

    private static void drawDiamond(GraphicsContext graphics, double x, double y,
                                    double unitX, double unitY, boolean filled) {
        double perpendicularX = -unitY;
        double perpendicularY = unitX;
        double length = 15;
        double halfWidth = 8;
        double[] pointsX = {x, x - unitX * length + perpendicularX * halfWidth,
                x - unitX * length * 2, x - unitX * length - perpendicularX * halfWidth};
        double[] pointsY = {y, y - unitY * length + perpendicularY * halfWidth,
                y - unitY * length * 2, y - unitY * length - perpendicularY * halfWidth};
        if (filled) {
            graphics.fillPolygon(pointsX, pointsY, 4);
        } else {
            graphics.setFill(Color.WHITE);
            graphics.fillPolygon(pointsX, pointsY, 4);
            graphics.strokePolygon(pointsX, pointsY, 4);
        }
    }

    private static void drawTriangle(GraphicsContext graphics, double x, double y,
                                     double unitX, double unitY) {
        double perpendicularX = -unitY;
        double perpendicularY = unitX;
        double length = 18;
        double halfWidth = 10;
        double[] pointsX = {x, x - unitX * length + perpendicularX * halfWidth,
                x - unitX * length - perpendicularX * halfWidth};
        double[] pointsY = {y, y - unitY * length + perpendicularY * halfWidth,
                y - unitY * length - perpendicularY * halfWidth};
        graphics.setFill(Color.WHITE);
        graphics.fillPolygon(pointsX, pointsY, 3);
        graphics.strokePolygon(pointsX, pointsY, 3);
    }

    private static void drawOpenArrow(GraphicsContext graphics, double x, double y,
                                      double unitX, double unitY) {
        double perpendicularX = -unitY;
        double perpendicularY = unitX;
        double length = 13;
        double halfWidth = 7;
        graphics.strokeLine(x, y, x - unitX * length + perpendicularX * halfWidth,
                y - unitY * length + perpendicularY * halfWidth);
        graphics.strokeLine(x, y, x - unitX * length - perpendicularX * halfWidth,
                y - unitY * length - perpendicularY * halfWidth);
    }

    private static void drawRelationLabel(GraphicsContext graphics, Relation relation,
                                          Point2D from, Point2D to, Color color) {
        String label = relation.via() == null ? relation.kind().name().toLowerCase() : relation.via();
        if (relation.multiplicity() != null && !relation.multiplicity().isBlank()) {
            label += "  " + relation.multiplicity();
        }
        double x = (from.getX() + to.getX()) / 2;
        double y = (from.getY() + to.getY()) / 2;
        double labelWidth = label.length() * 7.2 + 14;
        graphics.setFill(Color.rgb(255, 255, 255, 0.94));
        graphics.fillRoundRect(x - labelWidth / 2, y - 12, labelWidth, 22, 8, 8);
        graphics.setFill(color);
        graphics.fillText(label, x - labelWidth / 2 + 7, y + 4);
    }

    private static Color relationColor(RelationKind kind) {
        return switch (kind) {
            case INHERITANCE, REALIZATION -> Color.web("#334e68");
            case COMPOSITION -> Color.web("#087e65");
            case AGGREGATION -> Color.web("#568c7c");
            case ASSOCIATION -> Color.web("#60766f");
            case DEPENDENCY -> Color.web("#718096");
        };
    }
}