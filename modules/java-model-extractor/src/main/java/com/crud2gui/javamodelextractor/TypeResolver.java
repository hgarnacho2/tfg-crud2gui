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
 * Convierte tipos de JavaParser en {@link TypeRef} y decide si pertenecen al proyecto.
 *
 * <p>No usa el symbol solver de JavaParser: resuelve los nombres con reglas simples y explicables
 * contra el {@link TypeIndex}, sin necesitar el classpath del proyecto analizado.</p>
 */
final class TypeResolver {
    private final TypeIndex index;

    /**
     * Crea un resolutor.
     *
     * @param index registro de los tipos declarados en el proyecto
     */
    TypeResolver(TypeIndex index) {
        this.index = index;
    }

    /**
     * Convierte un tipo de JavaParser en una referencia del metamodelo, resolviendo genericos y arrays.
     *
     * @param type tipo tal como aparece en el codigo
     * @param ctx  contexto del fichero donde aparece
     * @return la referencia resuelta; los comodines devuelven su cota, o {@code ?} si no tienen
     */
    TypeRef resolve(Type type, ResolutionContext ctx) {
        if (type instanceof ArrayType at) {
            TypeRef inner = resolve(at.getComponentType(), ctx);
            return new TypeRef(inner.name(), inner.qualifiedName(), inner.projectType(),
                    inner.arguments(), inner.arrayDimensions() + 1);
        }
        if (type instanceof ClassOrInterfaceType ct) {
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
            if (wt.getExtendedType().isPresent()) {
                return resolve(wt.getExtendedType().get(), ctx);
            }
            if (wt.getSuperType().isPresent()) {
                return resolve(wt.getSuperType().get(), ctx);
            }
            return new TypeRef("?", null, false, List.of(), 0);
        }
        // primitivos, void, var y otros
        return new TypeRef(type.asString(), null, false, List.of(), 0);
    }

    /**
     * Busca el tipo del proyecto al que se refiere un nombre. Las reglas se aplican por orden:
     * tipos contenedores y clases internas, import explicito, mismo paquete, imports con asterisco
     * y, por ultimo, nombre simple unico en todo el proyecto.
     *
     * @param name nombre tal como se escribio, simple ({@code Usuario}) o calificado ({@code Outer.Inner})
     * @param ctx  contexto del fichero donde aparece
     * @return el id del tipo del proyecto, o vacio si el nombre es externo o no se puede resolver
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
