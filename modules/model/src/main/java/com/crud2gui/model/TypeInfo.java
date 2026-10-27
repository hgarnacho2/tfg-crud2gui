package com.crud2gui.model;

import java.util.List;

/**
 * Clase, interfaz, enum, record o anotacion del proyecto analizado. Es un nodo del diagrama de clases.
 *
 * @param id             nombre completo, unico en el proyecto; es la clave usada por las relaciones y el layout.
 *                       Las clases internas usan el id del contenedor mas su nombre
 * @param name           nombre simple
 * @param packageName    paquete; cadena vacia en el paquete por defecto
 * @param kind           clase, interfaz, enum, record o anotacion
 * @param visibility     visibilidad del tipo
 * @param modifiers      modificadores (abstract, static, final...)
 * @param enclosingType  id del tipo contenedor si es una clase interna; {@code null} si es de primer nivel
 * @param superclass     superclase declarada con {@code extends}; {@code null} si no hay
 * @param interfaces     interfaces implementadas; en una interfaz, las que extiende
 * @param typeParameters nombres de los parametros de tipo ({@code T}, {@code K}, {@code V})
 * @param annotations    anotaciones del tipo
 * @param javadoc        documentacion, o {@code null} si no tiene
 * @param location       posicion en el codigo fuente
 * @param hasMainMethod  {@code true} si declara {@code public static void main(String[])}
 * @param fields         atributos (en un record, sus componentes)
 * @param constructors   constructores declarados
 * @param methods        metodos declarados
 * @param enumConstants  constantes, solo para enums
 * @param optional       {@code true} si el enunciado marca este tipo como opcional
 */
public record TypeInfo(String id, String name, String packageName, TypeKind kind,
                       Visibility visibility, List<Modifier> modifiers,
                       String enclosingType, TypeRef superclass, List<TypeRef> interfaces,
                       List<String> typeParameters, List<AnnotationInfo> annotations,
                       JavadocInfo javadoc, Location location, boolean hasMainMethod,
                       List<FieldInfo> fields, List<ConstructorInfo> constructors,
                       List<MethodInfo> methods, List<EnumConstantInfo> enumConstants,
                       boolean optional) {

    /** Mantiene compatibilidad con llamadas que no especifican opcionalidad. */
    public TypeInfo(String id, String name, String packageName, TypeKind kind,
                    Visibility visibility, List<Modifier> modifiers,
                    String enclosingType, TypeRef superclass, List<TypeRef> interfaces,
                    List<String> typeParameters, List<AnnotationInfo> annotations,
                    JavadocInfo javadoc, Location location, boolean hasMainMethod,
                    List<FieldInfo> fields, List<ConstructorInfo> constructors,
                    List<MethodInfo> methods, List<EnumConstantInfo> enumConstants) {
        this(id, name, packageName, kind, visibility, modifiers, enclosingType, superclass, interfaces,
                typeParameters, annotations, javadoc, location, hasMainMethod, fields, constructors,
                methods, enumConstants, false);
    }

    /**
     * Normaliza las listas: {@code null} se convierte en una lista vacia inmutable.
     */
    public TypeInfo {
        modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
        interfaces = interfaces == null ? List.of() : List.copyOf(interfaces);
        typeParameters = typeParameters == null ? List.of() : List.copyOf(typeParameters);
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
        fields = fields == null ? List.of() : List.copyOf(fields);
        constructors = constructors == null ? List.of() : List.copyOf(constructors);
        methods = methods == null ? List.of() : List.copyOf(methods);
        enumConstants = enumConstants == null ? List.of() : List.copyOf(enumConstants);
    }
}
