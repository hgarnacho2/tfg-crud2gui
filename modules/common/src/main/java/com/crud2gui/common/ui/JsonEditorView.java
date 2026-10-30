package com.crud2gui.common.ui;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Visor/editor de JSON para JavaFX.
 *
 * Uso:
 *   JsonEditorView.show(Path.of("config.json"));                 // abre una ventana nueva
 *   Parent vista = JsonEditorView.createView(Path.of("a.json")); // para incrustarlo en tu escena
 *   JsonEditorView editor = new JsonEditorView(path);            // para incrustarlo y leer el JSON editado
 *   JsonEditorView visor = new JsonEditorView(path, false);      // solo lectura (sin inspector ni guardado)
 *
 * Requiere jackson-databind y javafx-controls (dependencias opcionales del módulo common).
 */
public final class JsonEditorView {

    // ───────────────────────────── API pública ─────────────────────────────

    /** Abre el editor en una ventana nueva. */
    public static void show(Path path) {
        try {
            JsonEditorView view = new JsonEditorView(path);
            Stage stage = new Stage();
            stage.setTitle("Editor JSON · " + path.getFileName());
            stage.setScene(new Scene(view.root, 1000, 640));
            stage.setOnCloseRequest(ev -> {
                if (view.dirty.get() && !view.confirmDiscard()) ev.consume();
            });
            stage.show();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Devuelve el editor como nodo para incrustarlo donde quieras. */
    public static Parent createView(Path path) {
        try {
            return new JsonEditorView(path).root;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Crea un editor sobre el fichero indicado. */
    public JsonEditorView(Path path) throws IOException {
        this(path, true);
    }

    /**
     * Crea un editor o un visor de solo lectura sobre el fichero indicado.
     *
     * @param editable {@code false} oculta el inspector, «Recargar» y «Guardar» y bloquea la pestaña de texto
     */
    public JsonEditorView(Path path, boolean editable) throws IOException {
        this.path = path;
        this.editable = editable;
        buildUi();
        load();
    }

    /** Indica si el JSON se puede modificar. */
    public boolean isEditable() {
        return editable;
    }

    /** Nodo raíz del editor, para incrustarlo en otra escena. */
    public Parent getView() {
        return root;
    }

    /** Indica si hay cambios sin guardar en el fichero original. */
    public ReadOnlyBooleanProperty dirtyProperty() {
        return dirty;
    }

    /** Muestra u oculta el botón «Guardar» que sobrescribe el fichero original (siempre oculto en solo lectura). */
    public void setSaveVisible(boolean visible) {
        toggle(saveBtn, visible && editable);
    }

    /**
     * Devuelve el JSON editado (formateado) tomando como origen la pestaña activa.
     *
     * @throws IOException si el texto no es JSON válido
     * @throws IllegalStateException si el árbol contiene claves duplicadas o números no válidos
     */
    public String currentJson() throws IOException {
        return pretty(currentNode());
    }

    // ───────────────────────────── Modelo ─────────────────────────────

    enum Kind { OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL }

    /** Nodo editable del árbol (mutable, se convierte a JsonNode al guardar). */
    static final class Entry {
        String key;
        Kind kind;
        String value; // solo para tipos primitivos

        Entry(String key, Kind kind, String value) {
            this.key = key;
            this.kind = kind;
            this.value = value;
        }
    }

    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

    private final Path path;
    private final boolean editable;
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private final BooleanProperty dirty = new SimpleBooleanProperty(false);
    private boolean loading;      // evita bucles al rellenar el inspector
    private boolean syncing;      // evita bucles al sincronizar árbol <-> texto
    private boolean textDirty;    // el texto se editó a mano y el árbol está desfasado

    private final BorderPane root = new BorderPane();
    private final TreeView<Entry> tree = new TreeView<>();
    private final TextArea textArea = new TextArea();
    private final Label title = new Label();
    private final Label status = new Label();
    private final Button saveBtn = new Button("Guardar");
    private TabPane tabs;
    private Tab treeTab, textTab;

    // Inspector
    private final VBox inspector = new VBox(12);
    private final TextField keyField = new TextField();
    private final ComboBox<Kind> typeBox = new ComboBox<>();
    private final TextArea valueArea = new TextArea();
    private final CheckBox boolBox = new CheckBox();
    private VBox valueBox;
    private final Button addBtn = new Button("＋ Hijo");
    private final Button dupBtn = new Button("Duplicar");
    private final Button upBtn = new Button("↑");
    private final Button downBtn = new Button("↓");
    private final Button delBtn = new Button("Eliminar");

    // ───────────────────────────── UI ─────────────────────────────

    private void buildUi() {
        root.getStyleClass().add("json-editor");
        root.getStylesheets().add(JsonEditorView.class.getResource("json-editor.css").toExternalForm());

        // Barra superior
        title.getStyleClass().add("title");
        title.textProperty().bind(Bindings.when(dirty)
                .then(path.getFileName() + "  ●")
                .otherwise(path.getFileName().toString()));
        Label subtitle = new Label(path.toAbsolutePath().toString());
        subtitle.getStyleClass().add("subtitle");
        VBox titles = new VBox(2, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button expandBtn = new Button("Expandir");
        Button collapseBtn = new Button("Contraer");
        Button reloadBtn = new Button("Recargar");
        saveBtn.getStyleClass().add("primary");
        saveBtn.disableProperty().bind(dirty.not());

        expandBtn.setOnAction(e -> setExpandedAll(tree.getRoot(), true));
        collapseBtn.setOnAction(e -> {
            setExpandedAll(tree.getRoot(), false);
            tree.getRoot().setExpanded(true);
        });
        reloadBtn.setOnAction(e -> reload());
        saveBtn.setOnAction(e -> save());

        HBox bar = new HBox(8, titles, spacer, expandBtn, collapseBtn, reloadBtn, saveBtn);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);

        // Árbol
        tree.setShowRoot(true);
        tree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Entry e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                TreeItem<Entry> ti = getTreeItem();
                String kind = "kind-" + e.kind.name().toLowerCase();

                Label badge = new Label(badge(e.kind));
                badge.getStyleClass().addAll("badge", kind);
                Label key = new Label(displayKey(ti));
                key.getStyleClass().add("key");
                HBox box = new HBox(8, badge, key);
                box.setAlignment(Pos.CENTER_LEFT);

                Label value = new Label(preview(ti));
                value.getStyleClass().addAll("value", kind);
                if (isContainer(e.kind)) {
                    value.getStyleClass().add("muted");
                    box.getChildren().add(value);
                } else {
                    Label colon = new Label(":");
                    colon.getStyleClass().add("muted");
                    box.getChildren().addAll(colon, value);
                }
                setText(null);
                setGraphic(box);
            }
        });
        tree.getSelectionModel().selectedItemProperty().addListener((o, a, sel) -> loadInspector(sel));

        // Inspector
        buildInspector();
        SplitPane split = new SplitPane(tree, inspector);
        split.setDividerPositions(0.62);

        // Pestaña de texto
        textArea.getStyleClass().add("code");
        textArea.textProperty().addListener((o, a, b) -> {
            if (!syncing) {
                textDirty = true;
                dirty.set(true);
            }
        });

        treeTab = new Tab("Árbol", split);
        textTab = new Tab("Texto", textArea);
        treeTab.setClosable(false);
        textTab.setClosable(false);
        tabs = new TabPane(treeTab, textTab);
        tabs.getSelectionModel().selectedItemProperty().addListener((o, old, now) -> onTabChange(old, now));

        // Barra de estado
        status.getStyleClass().add("status");
        HBox statusBar = new HBox(status);
        statusBar.getStyleClass().add("statusbar");

        root.setTop(bar);
        root.setCenter(tabs);
        root.setBottom(statusBar);

        if (!editable) {
            split.getItems().remove(inspector);
            textArea.setEditable(false);
            toggle(reloadBtn, false);
            toggle(saveBtn, false);
        }

        // Ctrl/Cmd + S
        KeyCodeCombination ctrlS = new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN);
        root.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ctrlS.match(ev) && saveBtn.isVisible()) {
                save();
                ev.consume();
            }
        });
    }

    private void buildInspector() {
        inspector.getStyleClass().add("inspector");
        inspector.setPadding(new Insets(16));
        inspector.setMinWidth(260);

        Label header = new Label("Inspector");
        header.getStyleClass().add("panel-title");

        typeBox.getItems().addAll(Kind.values());
        typeBox.setMaxWidth(Double.MAX_VALUE);
        valueArea.setWrapText(true);
        valueArea.setPrefRowCount(5);
        valueArea.getStyleClass().add("code");
        delBtn.getStyleClass().add("danger");

        valueBox = field("Valor", valueArea, boolBox);
        FlowPane actions = new FlowPane(8, 8, addBtn, dupBtn, upBtn, downBtn, delBtn);

        inspector.getChildren().addAll(header, field("Clave", keyField), field("Tipo", typeBox), valueBox, actions);
        inspector.setDisable(true);

        // Listeners (edición en vivo)
        keyField.textProperty().addListener((o, a, text) -> {
            TreeItem<Entry> sel = selected();
            if (loading || sel == null || !inObject(sel)) return;
            sel.getValue().key = text;
            touched();
        });
        typeBox.valueProperty().addListener((o, a, kind) -> {
            TreeItem<Entry> sel = selected();
            if (loading || sel == null || kind == null) return;
            changeType(sel, kind);
        });
        valueArea.textProperty().addListener((o, a, text) -> {
            TreeItem<Entry> sel = selected();
            if (loading || sel == null) return;
            sel.getValue().value = text;
            validate(sel.getValue());
            touched();
        });
        boolBox.selectedProperty().addListener((o, a, b) -> {
            TreeItem<Entry> sel = selected();
            if (loading || sel == null) return;
            sel.getValue().value = Boolean.toString(b);
            boolBox.setText(Boolean.toString(b));
            touched();
        });

        addBtn.setOnAction(e -> addChild());
        dupBtn.setOnAction(e -> duplicate());
        upBtn.setOnAction(e -> move(-1));
        downBtn.setOnAction(e -> move(1));
        delBtn.setOnAction(e -> delete());
    }

    private static VBox field(String label, Node... nodes) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        VBox box = new VBox(4, l);
        box.getChildren().addAll(nodes);
        return box;
    }

    private void loadInspector(TreeItem<Entry> sel) {
        loading = true;
        try {
            inspector.setDisable(sel == null);
            if (sel == null) return;
            Entry e = sel.getValue();
            TreeItem<Entry> p = sel.getParent();

            boolean editableKey = inObject(sel);
            keyField.setText(p == null ? "raíz" : editableKey ? e.key : "[" + p.getChildren().indexOf(sel) + "]");
            keyField.setDisable(!editableKey);
            typeBox.setValue(e.kind);

            boolean isBool = e.kind == Kind.BOOLEAN;
            boolean isText = e.kind == Kind.STRING || e.kind == Kind.NUMBER;
            valueArea.setText(e.value);
            boolBox.setSelected(Boolean.parseBoolean(e.value));
            boolBox.setText(Boolean.toString(Boolean.parseBoolean(e.value)));
            toggle(valueArea, isText);
            toggle(boolBox, isBool);
            toggle(valueBox, isText || isBool);
            validate(e);

            addBtn.setDisable(!isContainer(e.kind));
            dupBtn.setDisable(p == null);
            upBtn.setDisable(p == null);
            downBtn.setDisable(p == null);
            delBtn.setDisable(p == null);
        } finally {
            loading = false;
        }
    }

    private static void toggle(Node n, boolean visible) {
        n.setVisible(visible);
        n.setManaged(visible);
    }

    private void validate(Entry e) {
        boolean bad = e.kind == Kind.NUMBER && !isValidNumber(e.value);
        valueArea.pseudoClassStateChanged(INVALID, bad);
    }

    // ───────────────────────────── Operaciones de edición ─────────────────────────────

    private void changeType(TreeItem<Entry> sel, Kind newKind) {
        Entry e = sel.getValue();
        Kind old = e.kind;
        if (old == newKind) return;
        e.kind = newKind;

        if (!isContainer(newKind)) {
            sel.getChildren().clear(); // un primitivo no tiene hijos
        }
        switch (newKind) {
            case STRING -> { if (isContainer(old)) e.value = ""; }
            case NUMBER -> { if (!isValidNumber(e.value)) e.value = "0"; }
            case BOOLEAN -> e.value = Boolean.toString(Boolean.parseBoolean(e.value));
            default -> e.value = "";
        }
        loadInspector(sel);
        touched();
    }

    private void addChild() {
        TreeItem<Entry> sel = selected();
        if (sel == null || !isContainer(sel.getValue().kind)) return;
        boolean array = sel.getValue().kind == Kind.ARRAY;
        TreeItem<Entry> child = new TreeItem<>(new Entry(array ? "" : uniqueKey(sel, "nuevaClave"), Kind.STRING, ""));
        sel.getChildren().add(child);
        sel.setExpanded(true);
        tree.getSelectionModel().select(child);
        touched();
        if (!array) keyField.requestFocus();
    }

    private void duplicate() {
        TreeItem<Entry> sel = selected();
        if (sel == null || sel.getParent() == null) return;
        try {
            TreeItem<Entry> parent = sel.getParent();
            String key = inObject(sel) ? uniqueKey(parent, sel.getValue().key + "_copia") : "";
            TreeItem<Entry> copy = build(key, toJson(sel), 2);
            parent.getChildren().add(parent.getChildren().indexOf(sel) + 1, copy);
            tree.getSelectionModel().select(copy);
            touched();
        } catch (RuntimeException ex) {
            error("No se pudo duplicar", ex);
        }
    }

    private void move(int delta) {
        TreeItem<Entry> sel = selected();
        if (sel == null || sel.getParent() == null) return;
        var siblings = sel.getParent().getChildren();
        int i = siblings.indexOf(sel), j = i + delta;
        if (j < 0 || j >= siblings.size()) return;
        siblings.remove(sel);
        siblings.add(j, sel);
        tree.getSelectionModel().select(sel);
        touched();
    }

    private void delete() {
        TreeItem<Entry> sel = selected();
        if (sel == null || sel.getParent() == null) return;
        TreeItem<Entry> parent = sel.getParent();
        parent.getChildren().remove(sel);
        tree.getSelectionModel().select(parent);
        touched();
    }

    private void touched() {
        dirty.set(true);
        tree.refresh();
    }

    private String uniqueKey(TreeItem<Entry> parent, String base) {
        String key = base;
        int n = 2;
        while (hasKey(parent, key)) key = base + n++;
        return key;
    }

    private boolean hasKey(TreeItem<Entry> parent, String key) {
        return parent.getChildren().stream().anyMatch(c -> key.equals(c.getValue().key));
    }

    // ───────────────────────────── Carga, guardado y sincronización ─────────────────────────────

    private void load() throws IOException {
        JsonNode json = mapper.readTree(path.toFile());
        if (json == null || json.isMissingNode()) json = mapper.createObjectNode();
        replaceTree(json);
        dirty.set(false);
        textDirty = false;
        setStatus("Cargado " + path.getFileName());
    }

    private void replaceTree(JsonNode json) {
        TreeItem<Entry> r = build("raíz", json, 0);
        tree.setRoot(r);
        tree.getSelectionModel().select(r);
    }

    private void reload() {
        if (dirty.get() && !confirmDiscard()) return;
        try {
            syncing = true;
            tabs.getSelectionModel().select(treeTab);
            load();
        } catch (IOException ex) {
            error("No se pudo recargar el archivo", ex);
        } finally {
            syncing = false;
        }
    }

    private boolean save() {
        try {
            Files.writeString(path, currentJson() + System.lineSeparator());
            dirty.set(false);
            setStatus("Guardado a las " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            return true;
        } catch (Exception ex) {
            error("No se pudo guardar", ex);
            return false;
        }
    }

    private JsonNode currentNode() throws IOException {
        boolean fromText = tabs.getSelectionModel().getSelectedItem() == textTab;
        return fromText ? parse(textArea.getText()) : toJson(tree.getRoot());
    }

    /** Mantiene sincronizados la pestaña de árbol y la de texto. */
    private void onTabChange(Tab old, Tab now) {
        if (syncing) return;
        try {
            if (now == textTab) {
                String text = pretty(toJson(tree.getRoot()));
                syncing = true;
                textArea.setText(text);
                textDirty = false;
            } else if (now == treeTab && textDirty) {
                replaceTree(parse(textArea.getText()));
                textDirty = false;
            }
        } catch (Exception ex) {
            Platform.runLater(() -> {
                syncing = true;
                tabs.getSelectionModel().select(old);
                syncing = false;
                error("JSON no válido", ex);
            });
        } finally {
            syncing = false;
        }
    }

    private boolean confirmDiscard() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Hay cambios sin guardar. ¿Descartarlos?",
                ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Cambios sin guardar");
        return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    private void error(String header, Exception ex) {
        Alert a = new Alert(Alert.AlertType.ERROR, String.valueOf(ex.getMessage()));
        a.setHeaderText(header);
        a.showAndWait();
    }

    private void setStatus(String msg) {
        status.setText(msg);
    }

    // ───────────────────────────── Conversión JsonNode <-> TreeItem ─────────────────────────────

    private TreeItem<Entry> build(String key, JsonNode n, int depth) {
        Kind kind = kindOf(n);
        String value = isContainer(kind) || kind == Kind.NULL ? "" : n.asText();
        TreeItem<Entry> item = new TreeItem<>(new Entry(key, kind, value));
        item.setExpanded(depth < 2);
        if (n.isObject()) {
            n.fields().forEachRemaining(f -> item.getChildren().add(build(f.getKey(), f.getValue(), depth + 1)));
        } else if (n.isArray()) {
            int i = 0;
            for (JsonNode c : n) item.getChildren().add(build(String.valueOf(i++), c, depth + 1));
        }
        return item;
    }

    private JsonNode toJson(TreeItem<Entry> item) {
        Entry e = item.getValue();
        JsonNodeFactory f = JsonNodeFactory.instance;
        switch (e.kind) {
            case OBJECT: {
                ObjectNode o = mapper.createObjectNode();
                for (TreeItem<Entry> c : item.getChildren()) {
                    String k = c.getValue().key;
                    if (o.has(k)) throw new IllegalStateException("Clave duplicada: \"" + k + "\"");
                    o.set(k, toJson(c));
                }
                return o;
            }
            case ARRAY: {
                ArrayNode a = mapper.createArrayNode();
                for (TreeItem<Entry> c : item.getChildren()) a.add(toJson(c));
                return a;
            }
            case STRING:
                return f.textNode(e.value);
            case NUMBER:
                if (!isValidNumber(e.value)) {
                    throw new IllegalStateException("Número no válido en \"" + e.key + "\": " + e.value);
                }
                return number(e.value);
            case BOOLEAN:
                return f.booleanNode(Boolean.parseBoolean(e.value));
            default:
                return f.nullNode();
        }
    }

    private JsonNode parse(String text) throws IOException {
        JsonNode n = mapper.readTree(text);
        if (n == null || n.isMissingNode()) throw new IllegalStateException("El JSON está vacío");
        return n;
    }

    private String pretty(JsonNode n) throws IOException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(n);
    }

    private static JsonNode number(String raw) {
        String v = raw.trim();
        JsonNodeFactory f = JsonNodeFactory.instance;
        if (v.matches("-?\\d+")) {
            BigInteger bi = new BigInteger(v);
            return bi.bitLength() < 64 ? f.numberNode(bi.longValue()) : f.numberNode(bi);
        }
        return f.numberNode(new BigDecimal(v));
    }

    private static boolean isValidNumber(String s) {
        try {
            new BigDecimal(s.trim());
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    // ───────────────────────────── Utilidades ─────────────────────────────

    private TreeItem<Entry> selected() {
        return tree.getSelectionModel().getSelectedItem();
    }

    private static boolean inObject(TreeItem<Entry> item) {
        return item.getParent() != null && item.getParent().getValue().kind == Kind.OBJECT;
    }

    private static boolean isContainer(Kind k) {
        return k == Kind.OBJECT || k == Kind.ARRAY;
    }

    private static Kind kindOf(JsonNode n) {
        if (n.isObject()) return Kind.OBJECT;
        if (n.isArray()) return Kind.ARRAY;
        if (n.isTextual()) return Kind.STRING;
        if (n.isNumber()) return Kind.NUMBER;
        if (n.isBoolean()) return Kind.BOOLEAN;
        return Kind.NULL;
    }

    private static String badge(Kind k) {
        return switch (k) {
            case OBJECT -> "{ }";
            case ARRAY -> "[ ]";
            case STRING -> "Aa";
            case NUMBER -> "#";
            case BOOLEAN -> "✓";
            case NULL -> "∅";
        };
    }

    private static String displayKey(TreeItem<Entry> ti) {
        TreeItem<Entry> p = ti.getParent();
        if (p == null) return "raíz";
        if (p.getValue().kind == Kind.ARRAY) return "[" + p.getChildren().indexOf(ti) + "]";
        return ti.getValue().key;
    }

    private static String preview(TreeItem<Entry> ti) {
        Entry e = ti.getValue();
        int n = ti.getChildren().size();
        return switch (e.kind) {
            case OBJECT -> n + (n == 1 ? " clave" : " claves");
            case ARRAY -> n + (n == 1 ? " elemento" : " elementos");
            case STRING -> "\"" + (e.value.length() > 80 ? e.value.substring(0, 80) + "…" : e.value) + "\"";
            case NUMBER, BOOLEAN -> e.value;
            case NULL -> "null";
        };
    }

    private static void setExpandedAll(TreeItem<?> item, boolean expanded) {
        if (item == null) return;
        item.setExpanded(expanded);
        item.getChildren().forEach(c -> setExpandedAll(c, expanded));
    }
}
