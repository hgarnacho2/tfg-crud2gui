package com.crud2gui.javamodelextractor;

import com.crud2gui.model.TypeRef;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.WildcardType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Interpreta referencias de tipo del AST y las convierte al formato {@link TypeRef} del modelo.
 *
 * <p>Para decidir si un nombre designa un tipo del proyecto, consulta el {@link TypeIndex} usando
 * el {@link ResolutionContext} del fichero: paquete, imports y tipos contenedores. Así puede
 * reconocer tipos declarados en otros archivos aunque todavía no se hayan extraído sus miembros.</p>
 *
 * <p>El resultado conserva el nombre simple, los argumentos genéricos y las dimensiones de array.
 * Solo asigna un identificador cualificado y marca {@code projectType} cuando encuentra el tipo en
 * el índice; las referencias externas, desconocidas o ambiguas se mantienen como referencias no
 * pertenecientes al proyecto. No compila el código ni resuelve clases externas mediante el
 * classpath: utiliza reglas explícitas para identificar los tipos propios del proyecto.</p>
 */
final class TypeResolver {
    /** Índice completo de los tipos declarados en el proyecto analizado. */
    private final TypeIndex index;

    /**
     * Crea un resolutor que reutiliza el índice construido antes de extraer los miembros.
     *
     * @param index registro de todos los tipos del proyecto, incluidos los tipos anidados
     */
    TypeResolver(TypeIndex index) {
        this.index = index;
    }

    /**
     * Convierte un tipo del AST de JavaParser en un {{@link TypeRef}} del modelo, es decir,
     * un dato que describe un tipo del código fuente dentro de ProjectModel. Por ejemplo, 
     * para un atributo Persona directora, el TypeRef guarda el nombre Persona y, si el índice 
     * la identifica como tipo del proyecto, también su identificador cualificado y la 
     * marca projectType.
     *
     * <p>Resuelve recursivamente los argumentos genéricos y el componente de los arrays. Para un
     * comodín, conserva la referencia a su cota superior o inferior; si no tiene cota, conserva
     * {@code ?}. Los tipos que no se identifican en el índice (por ejemplo, {@code int} o una clase
     * de una biblioteca) se representan por su nombre simple, pero sin identificador de proyecto.</p>
     *
     * @param type tipo tal como aparece en el codigo
     * @param ctx contexto del fichero donde aparece; determina cómo interpretar nombres simples
     * @return referencia con nombre, posible identificador del proyecto, argumentos y dimensiones
     */
    TypeRef resolve(Type type, ResolutionContext ctx) {
        if (type instanceof ArrayType at) {
            // Se resuelve el componente y se registra cada dimensión por separado en TypeRef.
            TypeRef inner = resolve(at.getComponentType(), ctx);
            return new TypeRef(inner.name(), inner.qualifiedName(), inner.projectType(),
                    inner.arguments(), inner.arrayDimensions() + 1);
        }
        if (type instanceof ClassOrInterfaceType ct) {
            // Los argumentos pueden pertenecer al proyecto aunque el tipo contenedor sea externo.
            List<TypeRef> args = new ArrayList<>();
            if (ct.getTypeArguments().isPresent()) {
                for (Type a : ct.getTypeArguments().get()) {
                    args.add(resolve(a, ctx));
                }
            }
            Optional<String> id = resolveName(ct.getNameWithScope(), ctx);
            return new TypeRef(ct.getNameAsString(), id.orElse(null), id.isPresent(), args, 0);
        }
        if (type instanceof WildcardType wt) {
            // TypeRef no guarda la dirección de la cota, solo el tipo de la referencia acotada.
            if (wt.getExtendedType().isPresent()) {
                return resolve(wt.getExtendedType().get(), ctx);
            }
            if (wt.getSuperType().isPresent()) {
                return resolve(wt.getSuperType().get(), ctx);
            }
            return new TypeRef("?", null, false, List.of(), 0);
        }
        // Primitivos, void, var y otros tipos no nominales se conservan como texto.
        return new TypeRef(type.asString(), null, false, List.of(), 0);
    }

    /**
     * Busca en el índice el tipo del proyecto al que se refiere un nombre.
     *
     * <p>Para nombres simples, busca por orden en los tipos contenedores, los imports explícitos,
     * el paquete del fichero y los imports con asterisco. Como último recurso acepta un nombre
     * simple presente una sola vez en todo el proyecto. Para nombres cualificados, primero busca
     * el nombre completo y, si no está indexado, intenta resolver su prefijo como tipo contenedor
     * (por ejemplo, una clase interna).</p>
     *
     * <p>Este método solo confirma pertenencia al proyecto: un resultado vacío significa que no
     * encontró una coincidencia propia inequívoca; no distingue ni resuelve las posibles clases
     * externas.</p>
     *
     * @param name nombre tal como se escribió, simple ({@code Usuario}) o cualificado
     *             ({@code paquete.Usuario} o {@code Exterior.Interior})
     * @param ctx contexto del fichero donde aparece el nombre
     * @return identificador del tipo si está indexado; vacío si no se encuentra en el proyecto
     */
    Optional<String> resolveName(String name, ResolutionContext ctx) {
        if (name.contains(".")) {
            if (index.contains(name)) {
                return Optional.of(name);
            }
            int dot = name.indexOf('.');
            Optional<String> first = resolveName(name.substring(0, dot), ctx);
            if (first.isPresent()) {
                String candidate = first.get() + name.substring(dot);
                if (index.contains(candidate)) {
                    return Optional.of(candidate);
                }
            }
            return Optional.empty();
        }

        // 1. tipos contenedores y sus clases internas
        for (String enclosing : ctx.enclosing()) {
            if (enclosing.substring(enclosing.lastIndexOf('.') + 1).equals(name)) {
                return Optional.of(enclosing);
            }
            if (index.contains(enclosing + "." + name)) {
                return Optional.of(enclosing + "." + name);
            }
        }
        // 2. import explicito (si apunta fuera del proyecto, es externo)
        String imported = ctx.explicitImports().get(name);
        if (imported != null) {
            return index.contains(imported) ? Optional.of(imported) : Optional.empty();
        }
        // 3. mismo paquete
        String samePackage = ctx.packageName().isEmpty() ? name : ctx.packageName() + "." + name;
        if (index.contains(samePackage)) {
            return Optional.of(samePackage);
        }
        // 4. imports con asterisco
        for (String pkg : ctx.wildcardImports()) {
            if (index.contains(pkg + "." + name)) {
                return Optional.of(pkg + "." + name);
            }
        }
        // 5. ultimo recurso: nombre simple unico en todo el proyecto
        List<String> candidates = index.withSimpleName(name);
        if (candidates.size() == 1) {
            return Optional.of(candidates.get(0));
        }
        return Optional.empty();
    }
}
