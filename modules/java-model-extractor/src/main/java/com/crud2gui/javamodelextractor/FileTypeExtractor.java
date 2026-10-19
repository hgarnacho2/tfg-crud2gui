package com.crud2gui.javamodelextractor;

import com.crud2gui.model.AnnotationInfo;
import com.crud2gui.model.ConstructorInfo;
import com.crud2gui.model.EnumConstantInfo;
import com.crud2gui.model.FieldInfo;
import com.crud2gui.model.JavadocInfo;
import com.crud2gui.model.Location;
import com.crud2gui.model.MethodInfo;
import com.crud2gui.model.Modifier;
import com.crud2gui.model.ParameterInfo;
import com.crud2gui.model.TypeInfo;
import com.crud2gui.model.TypeKind;
import com.crud2gui.model.TypeRef;
import com.crud2gui.model.Visibility;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithJavadoc;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.javadoc.JavadocBlockTag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convierte las declaraciones de tipo de un fichero en {@link TypeInfo}, incluidas las clases internas.
 *
 * <p>Aplica las reglas implicitas de Java: los miembros de una interfaz son publicos, sus metodos sin
 * cuerpo son abstractos y sus campos son estaticos y finales; los constructores de un enum son privados.</p>
 */
final class FileTypeExtractor {
    private final TypeResolver resolver;
    private final String file;

    /**
     * Crea un extractor para un fichero.
     *
     * @param resolver     resolutor de nombres de tipo
     * @param relativeFile ruta del fichero relativa a la raiz analizada, usada en las ubicaciones
     */
    FileTypeExtractor(TypeResolver resolver, String relativeFile) {
        this.resolver = resolver;
        this.file = relativeFile;
    }

    /**
     * Extrae un tipo y, a continuacion, sus tipos internos, y los anade a la lista de salida.
     *
     * @param td          declaracion a extraer
     * @param enclosingId id del tipo contenedor, o {@code null} si es de primer nivel
     * @param base        contexto del fichero (paquete e imports)
     * @param out         lista donde se anaden los tipos extraidos, el externo antes que los internos
     */
    void extract(TypeDeclaration<?> td, String enclosingId, ResolutionContext base, List<TypeInfo> out) {
        String name = td.getNameAsString();
        String id = enclosingId != null ? enclosingId + "." + name
                : (base.packageName().isEmpty() ? name : base.packageName() + "." + name);
        List<String> enclosing = new ArrayList<>();
        enclosing.add(id);
        enclosing.addAll(base.enclosing());
        ResolutionContext ctx = base.withEnclosing(enclosing);

        TypeKind kind;
        TypeRef superclass = null;
        List<TypeRef> interfaces = new ArrayList<>();
        List<String> typeParams = new ArrayList<>();
        List<EnumConstantInfo> constants = new ArrayList<>();
        List<FieldInfo> fields = new ArrayList<>();

        if (td instanceof ClassOrInterfaceDeclaration c) {
            kind = c.isInterface() ? TypeKind.INTERFACE : TypeKind.CLASS;
            for (TypeParameter tp : c.getTypeParameters()) typeParams.add(tp.getNameAsString());
            if (c.isInterface()) {
                for (ClassOrInterfaceType t : c.getExtendedTypes()) interfaces.add(resolver.resolve(t, ctx));
            } else {
                if (!c.getExtendedTypes().isEmpty()) {
                    superclass = resolver.resolve(c.getExtendedTypes().get(0), ctx);
                }
                for (ClassOrInterfaceType t : c.getImplementedTypes()) interfaces.add(resolver.resolve(t, ctx));
            }
        } else if (td instanceof EnumDeclaration e) {
            kind = TypeKind.ENUM;
            for (ClassOrInterfaceType t : e.getImplementedTypes()) interfaces.add(resolver.resolve(t, ctx));
            for (EnumConstantDeclaration k : e.getEntries()) {
                constants.add(new EnumConstantInfo(k.getNameAsString(), javadoc(k)));
            }
        } else if (td instanceof RecordDeclaration r) {
            kind = TypeKind.RECORD;
            for (TypeParameter tp : r.getTypeParameters()) typeParams.add(tp.getNameAsString());
            for (ClassOrInterfaceType t : r.getImplementedTypes()) interfaces.add(resolver.resolve(t, ctx));
            for (Parameter p : r.getParameters()) {
                fields.add(new FieldInfo(p.getNameAsString(), resolver.resolve(p.getType(), ctx),
                        Visibility.PRIVATE, List.of(Modifier.FINAL), false,
                        annotations(p), null, loc(p)));
            }
        } else if (td instanceof AnnotationDeclaration) {
            kind = TypeKind.ANNOTATION;
        } else {
            return; // declaracion desconocida
        }

        boolean inInterface = kind == TypeKind.INTERFACE || kind == TypeKind.ANNOTATION;
        List<ConstructorInfo> constructors = new ArrayList<>();
        List<MethodInfo> methods = new ArrayList<>();
        List<TypeDeclaration<?>> nested = new ArrayList<>();
        boolean hasMain = false;

        for (BodyDeclaration<?> member : td.getMembers()) {
            if (member instanceof FieldDeclaration f) {
                Visibility vis = visibility(f, inInterface ? Visibility.PUBLIC : Visibility.PACKAGE);
                List<Modifier> mods = modifiers(f);
                if (inInterface) {
                    if (!mods.contains(Modifier.STATIC)) mods.add(Modifier.STATIC);
                    if (!mods.contains(Modifier.FINAL)) mods.add(Modifier.FINAL);
                }
                for (VariableDeclarator v : f.getVariables()) {
                    fields.add(new FieldInfo(v.getNameAsString(), resolver.resolve(v.getType(), ctx),
                            vis, mods, v.getInitializer().isPresent(), annotations(f), javadoc(f), loc(v)));
                }
            } else if (member instanceof ConstructorDeclaration k) {
                Visibility vis = visibility(k, kind == TypeKind.ENUM ? Visibility.PRIVATE : Visibility.PACKAGE);
                constructors.add(new ConstructorInfo(vis, parameters(k.getParameters(), ctx),
                        thrown(k.getThrownExceptions(), ctx), annotations(k), javadoc(k), loc(k)));
            } else if (member instanceof MethodDeclaration m) {
                Visibility vis = visibility(m, inInterface ? Visibility.PUBLIC : Visibility.PACKAGE);
                List<Modifier> mods = modifiers(m);
                boolean hasBody = m.getBody().isPresent();
                if (inInterface && !hasBody && !mods.contains(Modifier.ABSTRACT)) mods.add(Modifier.ABSTRACT);
                if (isMain(m)) hasMain = true;
                methods.add(new MethodInfo(m.getNameAsString(), resolver.resolve(m.getType(), ctx), vis, mods,
                        parameters(m.getParameters(), ctx), thrown(m.getThrownExceptions(), ctx),
                        annotations(m), hasBody, javadoc(m), loc(m)));
            } else if (member instanceof AnnotationMemberDeclaration am) {
                methods.add(new MethodInfo(am.getNameAsString(), resolver.resolve(am.getType(), ctx),
                        Visibility.PUBLIC, List.of(Modifier.ABSTRACT), List.of(), List.of(),
                        annotations(am), false, javadoc(am), loc(am)));
            } else if (member instanceof TypeDeclaration<?> inner) {
                nested.add(inner);
            }
        }

        out.add(new TypeInfo(id, name, base.packageName(), kind,
                visibility(td, Visibility.PACKAGE), modifiers(td),
                enclosingId, superclass, interfaces, typeParams, annotations(td),
                javadoc(td), loc(td), hasMain, fields, constructors, methods, constants));

        for (TypeDeclaration<?> inner : nested) {
            extract(inner, id, base, out);
        }
    }

    // ---------- ayudas ----------

    /**
     * Convierte los parametros de un metodo o constructor.
     *
     * @param params parametros de JavaParser
     * @param ctx    contexto de resolucion
     * @return los parametros del metamodelo, en el mismo orden
     */
    private List<ParameterInfo> parameters(List<Parameter> params, ResolutionContext ctx) {
        List<ParameterInfo> out = new ArrayList<>();
        for (Parameter p : params) {
            out.add(new ParameterInfo(p.getNameAsString(), resolver.resolve(p.getType(), ctx), p.isVarArgs()));
        }
        return out;
    }

    /**
     * Convierte las excepciones declaradas con {@code throws}.
     *
     * @param types tipos de excepcion de JavaParser
     * @param ctx   contexto de resolucion
     * @return las referencias de tipo, en el mismo orden
     */
    private List<TypeRef> thrown(List<? extends Type> types, ResolutionContext ctx) {
        List<TypeRef> out = new ArrayList<>();
        for (Type t : types) out.add(resolver.resolve(t, ctx));
        return out;
    }

    /**
     * Indica si un metodo es un punto de entrada {@code public static void main(String[])}.
     *
     * @param m metodo a comprobar
     * @return {@code true} si es estatico, se llama {@code main} y recibe un unico {@code String[]} o {@code String...}
     */
    private static boolean isMain(MethodDeclaration m) {
        if (!m.getNameAsString().equals("main") || !m.isStatic() || m.getParameters().size() != 1) {
            return false;
        }
        Parameter p = m.getParameter(0);
        String type = p.getType().asString();
        return type.equals("String[]") || (p.isVarArgs() && type.equals("String"));
    }

    /**
     * Obtiene la visibilidad de un elemento.
     *
     * @param n          elemento con modificadores
     * @param whenImplicit visibilidad a usar cuando no se escribio ninguna
     * @return la visibilidad explicita, o {@code whenImplicit} si no hay
     */
    private static Visibility visibility(NodeWithModifiers<?> n, Visibility whenImplicit) {
        switch (n.getAccessSpecifier()) {
            case PUBLIC: return Visibility.PUBLIC;
            case PROTECTED: return Visibility.PROTECTED;
            case PRIVATE: return Visibility.PRIVATE;
            default: return whenImplicit;
        }
    }

    /**
     * Obtiene los modificadores de un elemento, sin la visibilidad.
     *
     * @param n elemento con modificadores
     * @return lista modificable de modificadores reconocidos; se descartan los que expresa {@link Visibility}
     */
    private static List<Modifier> modifiers(NodeWithModifiers<?> n) {
        List<Modifier> out = new ArrayList<>();
        n.getModifiers().forEach(m -> {
            try {
                out.add(Modifier.valueOf(m.getKeyword().name()));
            } catch (IllegalArgumentException ignored) {
                // PUBLIC, PRIVATE, etc. se expresan como visibilidad
            }
        });
        return out;
    }

    /**
     * Obtiene las anotaciones de un elemento.
     *
     * @param n elemento anotable
     * @return las anotaciones, por nombre
     */
    private static List<AnnotationInfo> annotations(NodeWithAnnotations<?> n) {
        List<AnnotationInfo> out = new ArrayList<>();
        n.getAnnotations().forEach(a -> out.add(new AnnotationInfo(a.getNameAsString())));
        return out;
    }

    /**
     * Descompone el Javadoc de un elemento en descripcion y etiquetas.
     *
     * @param n elemento documentable
     * @return la documentacion, o {@code null} si el elemento no tiene comentario Javadoc
     */
    private static JavadocInfo javadoc(NodeWithJavadoc<?> n) {
        return n.getJavadoc().map(j -> {
            Map<String, List<String>> tags = new LinkedHashMap<>();
            for (JavadocBlockTag t : j.getBlockTags()) {
                String content = t.getContent().toText().trim();
                String text = t.getName().map(nm -> (nm + " " + content).trim()).orElse(content);
                tags.computeIfAbsent(t.getTagName(), k -> new ArrayList<>()).add(text);
            }
            return new JavadocInfo(j.getDescription().toText().trim(), tags);
        }).orElse(null);
    }

    /**
     * Obtiene la posicion de un nodo en el fichero.
     *
     * @param n nodo de JavaParser
     * @return fichero y linea de inicio; la linea es 0 si se desconoce
     */
    private Location loc(Node n) {
        return new Location(file, n.getBegin().map(p -> p.line).orElse(0));
    }
}
