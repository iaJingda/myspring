package org.myspring.core;

import org.myspring.core.util.ConcurrentReferenceHashMap;
import org.myspring.core.SerializableTypeWrapper.TypeProvider;
import org.myspring.core.util.ObjectUtils;

import java.io.Serializable;
import java.lang.reflect.*;

public class ResolvableType  implements Serializable {

    public static final ResolvableType NONE = new ResolvableType(null, null, null, 0);

    private static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];

    private static final ConcurrentReferenceHashMap<ResolvableType, ResolvableType> cache =
            new ConcurrentReferenceHashMap<ResolvableType, ResolvableType>(256);


    /**
     * The underlying Java type being managed (only ever {@code null} for {@link #NONE}).
     */
    private final Type type;

    /**
     * Optional provider for the type.
     */
    private final TypeProvider typeProvider;

    /**
     * The {@code VariableResolver} to use or {@code null} if no resolver is available.
     */
    private final VariableResolver variableResolver;

    /**
     * The component type for an array or {@code null} if the type should be deduced.
     */
    private final ResolvableType componentType;

    /**
     * Copy of the resolved value.
     */
    private final Class<?> resolved;

    private final Integer hash;

    private ResolvableType superType;

    private ResolvableType[] interfaces;

    private ResolvableType[] generics;

    private ResolvableType(Class<?> clazz) {
        this.resolved = (clazz != null ? clazz : Object.class);
        this.type = this.resolved;
        this.typeProvider = null;
        this.variableResolver = null;
        this.componentType = null;
        this.hash = null;
    }
    private ResolvableType(Type type, TypeProvider typeProvider, VariableResolver variableResolver) {
        this.type = type;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.componentType = null;
        this.resolved = null;
        this.hash = calculateHashCode();
    }

    private ResolvableType(Type type, TypeProvider typeProvider, VariableResolver variableResolver, Integer hash) {
        this.type = type;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.componentType = null;
        this.resolved = resolveClass();
        this.hash = hash;
    }

    private ResolvableType(
            Type type, TypeProvider typeProvider, VariableResolver variableResolver, ResolvableType componentType) {

        this.type = type;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.componentType = componentType;
        this.resolved = resolveClass();
        this.hash = null;
    }


    public static ResolvableType forClass(Class<?> clazz) {
        return new ResolvableType(clazz);
    }

    private Class<?> resolveClass() {
        if (this.type instanceof Class || this.type == null) {
            return (Class<?>) this.type;
        }
        if (this.type instanceof GenericArrayType) {
            Class<?> resolvedComponent = getComponentType().resolve();
            return (resolvedComponent != null ? Array.newInstance(resolvedComponent, 0).getClass() : null);
        }
        return resolveType().resolve();
    }
    public Class<?> resolve(Class<?> fallback) {
        return (this.resolved != null ? this.resolved : fallback);
    }
    public Class<?> resolve() {
        return resolve(null);
    }

    public ResolvableType getComponentType() {
        if (this == NONE) {
            return NONE;
        }
        if (this.componentType != null) {
            return this.componentType;
        }
        if (this.type instanceof Class) {
            Class<?> componentType = ((Class<?>) this.type).getComponentType();
            return forType(componentType, this.variableResolver);
        }
        if (this.type instanceof GenericArrayType) {
            return forType(((GenericArrayType) this.type).getGenericComponentType(), this.variableResolver);
        }
        return resolveType().getComponentType();
    }

    ResolvableType resolveType() {
        if (this.type instanceof ParameterizedType) {
            return forType(((ParameterizedType) this.type).getRawType(), this.variableResolver);
        }
        if (this.type instanceof WildcardType) {
            Type resolved = resolveBounds(((WildcardType) this.type).getUpperBounds());
            if (resolved == null) {
                resolved = resolveBounds(((WildcardType) this.type).getLowerBounds());
            }
            return forType(resolved, this.variableResolver);
        }
        if (this.type instanceof TypeVariable) {
            TypeVariable<?> variable = (TypeVariable<?>) this.type;
            // Try default variable resolution
            if (this.variableResolver != null) {
                ResolvableType resolved = this.variableResolver.resolveVariable(variable);
                if (resolved != null) {
                    return resolved;
                }
            }
            // Fallback to bounds
            return forType(resolveBounds(variable.getBounds()), this.variableResolver);
        }
        return NONE;
    }

    private Type resolveBounds(Type[] bounds) {
        if (ObjectUtils.isEmpty(bounds) || Object.class == bounds[0]) {
            return null;
        }
        return bounds[0];
    }

    private static ResolvableType[] forTypes(Type[] types, VariableResolver owner) {
        ResolvableType[] result = new ResolvableType[types.length];
        for (int i = 0; i < types.length; i++) {
            result[i] = forType(types[i], owner);
        }
        return result;
    }
    public static ResolvableType forType(Type type) {
        return forType(type, null, null);
    }

    public static ResolvableType forType(Type type, ResolvableType owner) {
        VariableResolver variableResolver = null;
        if (owner != null) {
            variableResolver = owner.asVariableResolver();
        }
        return forType(type, variableResolver);
    }

    VariableResolver asVariableResolver() {
        if (this == NONE) {
            return null;
        }
        return new DefaultVariableResolver();
    }

    public static ResolvableType forType(ParameterizedTypeReference<?> typeReference) {
        return forType(typeReference.getType(), null, null);
    }
    static ResolvableType forType(Type type, VariableResolver variableResolver) {
        return forType(type, null, variableResolver);
    }

    static ResolvableType forType(Type type, TypeProvider typeProvider, VariableResolver variableResolver) {
        if (type == null && typeProvider != null) {
            type = SerializableTypeWrapper.forTypeProvider(typeProvider);
        }
        if (type == null) {
            return NONE;
        }

        // For simple Class references, build the wrapper right away -
        // no expensive resolution necessary, so not worth caching...
        if (type instanceof Class) {
            return new ResolvableType(type, typeProvider, variableResolver, (ResolvableType) null);
        }

        // Purge empty entries on access since we don't have a clean-up thread or the like.
        cache.purgeUnreferencedEntries();

        // Check the cache - we may have a ResolvableType which has been resolved before...
        ResolvableType key = new ResolvableType(type, typeProvider, variableResolver);
        ResolvableType resolvableType = cache.get(key);
        if (resolvableType == null) {
            resolvableType = new ResolvableType(type, typeProvider, variableResolver, key.hash);
            cache.put(resolvableType, resolvableType);
        }
        return resolvableType;
    }


    private ResolvableType resolveVariable(TypeVariable<?> variable) {
        if (this.type instanceof TypeVariable) {
            return resolveType().resolveVariable(variable);
        }
        if (this.type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) this.type;
            TypeVariable<?>[] variables = resolve().getTypeParameters();
            for (int i = 0; i < variables.length; i++) {
                if (ObjectUtils.nullSafeEquals(variables[i].getName(), variable.getName())) {
                    Type actualType = parameterizedType.getActualTypeArguments()[i];
                    return forType(actualType, this.variableResolver);
                }
            }
            if (parameterizedType.getOwnerType() != null) {
                return forType(parameterizedType.getOwnerType(), this.variableResolver).resolveVariable(variable);
            }
        }
        if (this.variableResolver != null) {
            return this.variableResolver.resolveVariable(variable);
        }
        return null;
    }



    @Override
    public int hashCode() {
        return (this.hash != null ? this.hash : calculateHashCode());
    }

    private int calculateHashCode() {
        int hashCode = ObjectUtils.nullSafeHashCode(this.type);
        if (this.typeProvider != null) {
            hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.typeProvider.getType());
        }
        if (this.variableResolver != null) {
            hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.variableResolver.getSource());
        }
        if (this.componentType != null) {
            hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.componentType);
        }
        return hashCode;
    }



    interface VariableResolver extends Serializable {

        /**
         * Return the source of the resolver (used for hashCode and equals).
         */
        Object getSource();

        /**
         * Resolve the specified variable.
         * @param variable the variable to resolve
         * @return the resolved variable, or {@code null} if not found
         */
        ResolvableType resolveVariable(TypeVariable<?> variable);
    }

    private class DefaultVariableResolver implements VariableResolver {

        @Override
        public ResolvableType resolveVariable(TypeVariable<?> variable) {
            return ResolvableType.this.resolveVariable(variable);
        }

        @Override
        public Object getSource() {
            return ResolvableType.this;
        }
    }
}
