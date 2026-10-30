package com.crud2gui.common.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Visor de solo lectura para ficheros Java con resaltado de sintaxis.
 *
 * Uso:
 *   JavaCodeViewer viewer = new JavaCodeViewer();
 *   viewer.showFile(Path.of("Alojamiento.java"));
 *
 * Los ficheros .java se colorean; el resto de extensiones admitidas (por defecto .txt y .md) se
 * muestran sin colores. Las extensiones admitidas se configuran en el constructor o con
 * {@link #setAllowedExtensions(Collection)}.
 * Requiere RichTextFX y javafx-controls (dependencias opcionales del módulo common).
 */
public final class JavaCodeViewer extends BorderPane {

    private static final long MAX_BYTES = 2L * 1024 * 1024; // 2 MB

    /** Extensiones que se muestran si no se indican otras. */
    public static final Set<String> DEFAULT_EXTENSIONS = Set.of("java", "txt", "md");

    // ───────────── Léxico de Java ─────────────

    private static final String[] KEYWORDS = {
            "abstract", "assert", "break", "case", "catch", "class", "const", "continue",
            "default", "do", "else", "enum", "extends", "final", "finally", "for", "goto",
            "if", "implements", "import", "instanceof", "interface", "native", "new",
            "package", "private", "protected", "public", "return", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "volatile", "while"
    };
    private static final String[] PRIMITIVES = {
            "boolean", "byte", "char", "double", "float", "int", "long", "short", "void"
    };

    /** Orden de los grupos = prioridad. Comentarios y cadenas primero para que
     *  una palabra reservada dentro de ellos no se coloree. */
    private static final String[] GROUPS = {
            "COMMENT", "STRING", "CHAR", "ANNOTATION", "KEYWORD", "TYPE", "LITERAL", "NUMBER", "CLASSNAME"
    };

    private static final Pattern PATTERN = Pattern.compile(
            "(?<COMMENT>//[^\\n]*|/\\*[\\s\\S]*?\\*/)"
          + "|(?<STRING>\"\"\"[\\s\\S]*?\"\"\"|\"([^\"\\\\\\n]|\\\\.)*\")"
          + "|(?<CHAR>'([^'\\\\\\n]|\\\\.)+')"
          + "|(?<ANNOTATION>@[A-Za-z_][A-Za-z0-9_]*)"
          + "|(?<KEYWORD>\\b(" + String.join("|", KEYWORDS) + ")\\b)"
          + "|(?<TYPE>\\b(" + String.join("|", PRIMITIVES) + ")\\b)"
          + "|(?<LITERAL>\\b(true|false|null)\\b)"
          + "|(?<NUMBER>\\b0[xX][0-9a-fA-F_]+\\b|\\b\\d[\\d_]*(\\.\\d+)?([eE][+-]?\\d+)?[fFdDlL]?\\b)"
          + "|(?<CLASSNAME>\\b[A-Z][A-Za-z0-9_]*\\b)");

    // ───────────── Componentes ─────────────

    private final CodeArea codeArea = new CodeArea();
    private final VirtualizedScrollPane<CodeArea> scroll = new VirtualizedScrollPane<>(codeArea);
    private final Label placeholder = new Label("Selecciona un fichero del árbol para ver su contenido");
    private final Label chip = new Label();
    private final Label fileName = new Label();
    private final Label info = new Label();
    private Set<String> allowedExtensions = DEFAULT_EXTENSIONS;

    /** Crea el visor con las extensiones por defecto ({@link #DEFAULT_EXTENSIONS}). */
    public JavaCodeViewer() {
        this(DEFAULT_EXTENSIONS);
    }

    /** Crea el visor limitado a las extensiones indicadas (sin punto, p. ej. "java"). */
    public JavaCodeViewer(Collection<String> extensions) {
        setAllowedExtensions(extensions);
        getStyleClass().add("java-code");
        getStylesheets().add(JavaCodeViewer.class.getResource("java-code.css").toExternalForm());

        chip.getStyleClass().add("chip");
        fileName.getStyleClass().add("file-name");
        info.getStyleClass().add("file-info");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, chip, fileName, spacer, info);
        header.getStyleClass().add("code-header");
        header.setAlignment(Pos.CENTER_LEFT);

        codeArea.setEditable(false);          // solo lectura, pero se puede seleccionar y copiar
        codeArea.setWrapText(false);
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        placeholder.getStyleClass().add("placeholder");
        StackPane body = new StackPane(scroll, placeholder);
        body.getStyleClass().add("code-body");

        setTop(header);
        setCenter(body);
        clear();
    }

    // ───────────── API pública ─────────────

    /** Limita los ficheros que se muestran a las extensiones indicadas (con o sin punto, sin distinguir mayúsculas). */
    public void setAllowedExtensions(Collection<String> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            throw new IllegalArgumentException("Debe indicarse al menos una extensión.");
        }
        Set<String> normalized = new TreeSet<>();
        for (String extension : extensions) {
            String value = extension == null ? "" : extension.trim().toLowerCase(Locale.ROOT);
            if (value.startsWith(".")) value = value.substring(1);
            if (value.isEmpty()) throw new IllegalArgumentException("Extensión vacía.");
            normalized.add(value);
        }
        allowedExtensions = Collections.unmodifiableSet(normalized);
    }

    /** Extensiones admitidas, en minúsculas y sin punto. */
    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    /** Indica si el fichero tiene una extensión admitida por el visor. */
    public boolean isSupported(Path file) {
        if (file == null || file.getFileName() == null) return false;
        String name = file.getFileName().toString();
        int i = name.lastIndexOf('.');
        return i >= 0 && allowedExtensions.contains(name.substring(i + 1).toLowerCase(Locale.ROOT));
    }

    /** Muestra el contenido del fichero; si es .java lo colorea. */
    public void showFile(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            clear();
            return;
        }
        String name = file.getFileName().toString();
        chip.setText(extensionOf(name));
        fileName.setText(name);
        setContentVisible(true);

        if (!isSupported(file)) {
            codeArea.replaceText("Tipo de fichero no admitido en el visor. Extensiones permitidas: ."
                    + String.join(", .", allowedExtensions));
            codeArea.clearStyle(0, codeArea.getLength());
            info.setText("");
            return;
        }

        try {
            if (Files.size(file) > MAX_BYTES) {
                codeArea.replaceText("El fichero es demasiado grande para previsualizarlo (más de 2 MB).");
                codeArea.clearStyle(0, codeArea.getLength());
                info.setText("");
                return;
            }
            Decoded decoded = read(file);
            String text = normalize(decoded.text());

            codeArea.replaceText(text);
            if (!text.isEmpty()) {
                if (name.toLowerCase(Locale.ROOT).endsWith(".java")) {
                    codeArea.setStyleSpans(0, highlight(text));
                } else {
                    codeArea.clearStyle(0, text.length());
                }
            }
            info.setText(codeArea.getParagraphs().size() + " líneas · " + decoded.charset());
            codeArea.moveTo(0);
            Platform.runLater(() -> codeArea.showParagraphAtTop(0));
        } catch (IOException ex) {
            codeArea.replaceText("No se pudo leer el fichero: " + ex.getMessage());
            codeArea.clearStyle(0, codeArea.getLength());
            info.setText("");
        }
    }

    /** Vacía el visor y muestra el mensaje inicial. */
    public void clear() {
        codeArea.clear();
        chip.setText("");
        fileName.setText("");
        info.setText("");
        setContentVisible(false);
    }

    /** Texto mostrado actualmente (vacío si no hay fichero). */
    public String getText() {
        return codeArea.getText();
    }

    /** Nombre del fichero mostrado actualmente (vacío si no hay fichero). */
    public String getFileName() {
        return fileName.getText();
    }

    // ───────────── Resaltado ─────────────

    private static StyleSpans<Collection<String>> highlight(String text) {
        Matcher m = PATTERN.matcher(text);
        StyleSpansBuilder<Collection<String>> spans = new StyleSpansBuilder<>();
        int last = 0;
        while (m.find()) {
            String style = null;
            for (String g : GROUPS) {
                if (m.group(g) != null) {
                    style = "tok-" + g.toLowerCase(Locale.ROOT);
                    break;
                }
            }
            spans.add(Collections.emptyList(), m.start() - last);
            spans.add(Collections.singleton(style), m.end() - m.start());
            last = m.end();
        }
        spans.add(Collections.emptyList(), text.length() - last);
        return spans.create();
    }

    // ───────────── Utilidades ─────────────

    private void setContentVisible(boolean visible) {
        scroll.setVisible(visible);
        placeholder.setVisible(!visible);
        chip.setVisible(visible);
        chip.setManaged(visible);
    }

    private record Decoded(String text, String charset) { }

    /** Lee como UTF-8 y, si no es válido, como Windows-1252 (habitual en ficheros antiguos). */
    private static Decoded read(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        try {
            CharBuffer cb = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return new Decoded(cb.toString(), "UTF-8");
        } catch (CharacterCodingException e) {
            return new Decoded(new String(bytes, Charset.forName("windows-1252")), "Windows-1252");
        }
    }

    private static String normalize(String s) {
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1); // BOM
        return s.replace("\r\n", "\n").replace('\r', '\n').replace("\t", "    ");
    }

    private static String extensionOf(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 || i == name.length() - 1 ? "FICHERO" : name.substring(i + 1).toUpperCase(Locale.ROOT);
    }
}
