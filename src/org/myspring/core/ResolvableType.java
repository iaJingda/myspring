package org.myspring.core;

import org.myspring.core.util.Assert;
import org.myspring.core.util.ClassUtils;
import org.myspring.core.util.ConcurrentReferenceHashMap;
import org.myspring.core.SerializableTypeWrapper.TypeProvider;
import org.myspring.core.SerializableTypeWrapper.FieldTypeProvider;
import org.myspring.core.util.ObjectUtils;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

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


    public static ResolvableType forField(Field field) {
        Assert.notNull(field, "Field must not be null");
        return forType(null, new FieldTypeProvider(field), null);
    }


    public boolean isArray() {
        if (this == NONE) {
            return false;
        }
        return ((this.type instanceof Class && ((Class<?>) this.type).isArray()) ||
                this.type instanceof GenericArrayType || resolveType().isArray());
    }

    public ResolvableType asMap() {
        return as(Map.class);
    }
    public ResolvableType asCollection() {
        return as(Collection.class);
    }
    public Type getType() {
        return SerializableTypeWrapper.unwrap(this.type);
    }

    public boolean hasUnresolvableGenerics() {
        if (this == NONE) {
            return false;
        }
        ResolvableType[] generics = getGenerics();
        for (ResolvableType generic : generics) {
            if (generic.isUnresolvableTypeVariable() || generic.isWildcardWithoutBounds()) {
                return true;
            }
        }
        Class<?> resolved = resolve();
        if (resolved != null) {
            for (Type genericInterface : resolved.getGenericInterfaces()) {
                if (genericInterface instanceof Class) {
                    if (forClass((Class<?>) genericInterface).hasGenerics()) {
                        return true;
                    }
                }
            }
            return getSuperType().hasUnresolvableGenerics();
        }
        return false;
    }
    private boolean isWildcardWithoutBounds() {
        if (this.type instanceof WildcardType) {
            WildcardType wt = (WildcardType) this.type;
            if (wt.getLowerBounds().length == 0) {
                Type[] upperBounds = wt.getUpperBounds();
                if (upperBounds.length == 0 || (upperBounds.length == 1 && Object.class == upperBounds[0])) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isUnresolvableTypeVariable() {
        if (this.type instanceof TypeVariable) {
            if (this.variableResolver == null) {
                return true;
            }
            TypeVariable<?> variable = (TypeVariable<?>) this.type;
            ResolvableType resolved = this.variableResolver.resolveVariable(variable);
            if (resolved == null || resolved.isUnresolvableTypeVariable()) {
                return true;
            }
        }
        return false;
    }


    public boolean isAssignableFrom(ResolvableType other) {
        return isAssignableFrom(other, null);
    }

    private boolean isAssignableFrom(ResolvableType other, Map<Type, Type> matchedBefore) {
        Assert.notNull(other, "ResolvableType must not be null");

        // If we cannot resolve types, we are not assignable
        if (this == NONE || other == NONE) {
            return false;
        }

        // Deal with array by delegating to the component type
        if (isArray()) {
            return (other.isArray() && getComponentType().isAssignableFrom(other.getComponentType()));
        }

        if (matchedBefore != null && matchedBefore.get(this.type) == other.type) {
            return true;
        }

        // Deal with wildcard bounds
        WildcardBounds ourBounds = WildcardBounds.get(this);
        WildcardBounds typeBounds = WildcardBounds.get(other);

        // In the form X is assignable to <? extends Number>
        if (typeBounds != null) {
            return (ourBounds != null && ourBounds.isSameKind(typeBounds) &&
                    ourBounds.isAssignableFrom(typeBounds.getBounds()));
        }

        // In the form <? extends Number> is assignable to X...
        if (ourBounds != null) {
            return ourBounds.isAssignableFrom(other);
        }

        // Main assignability check about to follow
        boolean exactMatch = (matchedBefore != null);  // We're checking nested generic variables now...
        boolean checkGenerics = true;
        Class<?> ourResolved = null;
        if (this.type instanceof TypeVariable) {
            TypeVariable<?> variable = (TypeVariable<?>) this.type;
            // Try default variable resolution
            if (this.variableResolver != null) {
                ResolvableType resolved = this.variableResolver.resolveVariable(variable);
                if (resolved != null) {
                    ourResolved = resolved.resolve();
                }
            }
            if (ourResolved == null) {
                // Try variable resolution against target type
                if (other.variableResolver != null) {
                    ResolvableType resolved = other.variableResolver.resolveVariable(variable);
                    if (resolved != null) {
                        ourResolved = resolved.resolve();
                        checkGenerics = false;
                    }
                }
            }
            if (ourResolved == null) {
                // Unresolved type variable, potentially nested -> never insist on exact match
                exactMatch = false;
            }
        }
        if (ourResolved == null) {
            ourResolved = resolve(Object.class);
        }
        Class<?> otherResolved = other.resolve(Object.class);

        // We need an exact type match for generics
        // List<CharSequence> is not assignable from List<String>
        if (exactMatch ? !ourResolved.equals(otherResolved) : !ClassUtils.isAssignable(ourResolved, otherResolved)) {
            return false;
        }

        if (checkGenerics) {
            // Recursively check each generic
            ResolvableType[] ourGenerics = getGenerics();
            ResolvableType[] typeGenerics = other.as(ourResolved).getGenerics();
            if (ourGenerics.length != typeGenerics.length) {
                return false;
            }
            if (matchedBefore == null) {
                matchedBefore = new IdentityHashMap<Type, Type>(1);
            }
            matchedBefore.put(this.type, other.type);
            for (int i = 0; i < ourGenerics.length; i++) {
                if (!ourGenerics[i].isAssignableFrom(typeGenerics[i], matchedBefore)) {
                    return false;
                }
            }
        }

        return true;
    }


    public static ResolvableType forMethodReturnType(Method method, Class<?> implementationClass) {
        Assert.notNull(method, "Method must not be null");
        MethodParameter methodParameter = new MethodParameter(method, -1);
        methodParameter.setContainingClass(implementationClass);
        return forMethodParameter(methodParameter);
    }

    public static ResolvableType forMethodParameter(MethodParameter methodParameter) {
        return forMethodParameter(methodParameter, (Type) null);
    }

    public static ResolvableType forMethodParameter(MethodParameter methodParameter, Type targetType) {
        Assert.notNull(methodParameter, "MethodParameter must not be null");
        ResolvableType owner = forType(methodParameter.getContainingClass()).as(methodParameter.getDeclaringClass());
        return forType(targetType, new SerializableTypeWrapper.MethodParameterTypeProvider(methodParameter), owner.asVariableResolver()).
                getNested(methodParameter.getNestingLevel(), methodParameter.typeIndexesPerLevel);
    }


    public ResolvableType getNested(int nestingLevel, Map<Integer, Integer> typeIndexesPerLevel) {
        ResolvableType result = this;
        for (int i = 2; i <= nestingLevel; i++) {
            if (result.isArray()) {
                result = result.getComponentType();
            }
            else {
                // Handle derived types
                while (result != ResolvableType.NONE && !result.hasGenerics()) {
                    result = result.getSuperType();
                }
                Integer index = (typeIndexesPerLevel != null ? typeIndexesPerLevel.get(i) : null);
                index = (index == null ? result.getGenerics().length - 1 : index);
                result = result.getGeneric(index);
            }
        }
        return result;
    }

    public ResolvableType getGeneric(int... indexes) {
        ResolvableType[] generics = getGenerics();
        if (indexes == null || indexes.length == 0) {
            return (generics.length == 0 ? NONE : generics[0]);
        }
        ResolvableType generic = this;
        for (int index : indexes) {
            generics = generic.getGenerics();
            if (index < 0 || index >= generics.length) {
                return NONE;
            }
            generic = generics[index];
        }
        return generic;
    }
    public boolean hasGenerics() {
        return (getGenerics().length > 0);
    }

    public ResolvableType[] getGenerics() {
        if (this == NONE) {
            return EMPTY_TYPES_ARRAY;
        }
        if (this.generics == null) {
            if (this.type instanceof Class) {
                Class<?> typeClass = (Class<?>) this.type;
                this.generics = forTypes(SerializableTypeWrapper.forTypeParameters(typeClass), this.variableResolver);
            }
            else if (this.type instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) this.type).getActualTypeArguments();
                ResolvableType[] generics = new ResolvableType[actualTypeArguments.length];
                for (int i = 0; i < actualTypeArguments.length; i++) {
                    generics[i] = forType(actualTypeArguments[i], this.variableResolver);
                }
                this.generics = generics;
            }
            else {
                this.generics = resolveType().getGenerics();
            }
        }
        return this.generics;
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
    public ResolvableType as(Class<?> type) {
        if (this == NONE) {
            return NONE;
        }
        if (ObjectUtils.nullSafeEquals(resolve(), type)) {
            return this;
        }
        for (ResolvableType interfaceType : getInterfaces()) {
            ResolvableType interfaceAsType = interfaceType.as(type);
            if (interfaceAsType != NONE) {
                return interfaceAsType;
            }
        }
        return getSuperType().as(type);
    }

    public ResolvableType[] getInterfaces() {
        Class<?> resolved = resolve();
        if (resolved == null || ObjectUtils.isEmpty(resolved.getGenericInterfaces())) {
            return EMPTY_TYPES_ARRAY;
        }
        if (this.interfaces == null) {
            this.interfaces = forTypes(SerializableTypeWrapper.forGenericInterfaces(resolved), asVariableResolver());
        }
        return this.interfaces;
    }

    public ResolvableType getSuperType() {
        Class<?> resolved = resolve();
        if (resolved == null || resolved.getGenericSuperclass() == null) {
            return NONE;
        }
        if (this.superType == null) {
            this.superType = forType(SerializableTypeWrapper.forGenericSuperclass(resolved), asVariableResolver());
        }
        return this.superType;
    }


    static void resolveMethodParameter(MethodParameter methodParameter) {
        Assert.notNull(methodParameter, "MethodParameter must not be null");
        ResolvableType owner = forType(methodParameter.getContainingClass()).as(methodParameter.getDeclaringClass());
        methodParameter.setParameterType(
                forType(null, new SerializableTypeWrapper.MethodParameterTypeProvider(methodParameter), owner.asVariableResolver()).resolve());
    }


    private static class WildcardBounds {

        private final Kind kind;

        private final ResolvableType[] bounds;

        /**
         * Internal constructor to create a new {@link WildcardBounds} instance.
         * @param kind the kind of bounds
         * @param bounds the bounds
         * @see #get(ResolvableType)
         */
        public WildcardBounds(Kind kind, ResolvableType[] bounds) {
            this.kind = kind;
            this.bounds = bounds;
        }

        /**
         * Return {@code true} if this bounds is the same kind as the specified bounds.
         */
        public boolean isSameKind(WildcardBounds bounds) {
            return this.kind == bounds.kind;
        }

        /**
         * Return {@code true} if this bounds is assignable to all the specified types.
         * @param types the types to test against
         * @return {@code true} if this bounds is assignable to all types
         */
        public boolean isAssignableFrom(ResolvableType... types) {
            for (ResolvableType bound : this.bounds) {
                for (ResolvableType type : types) {
                    if (!isAssignable(bound, type)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean isAssignable(ResolvableType source, ResolvableType from) {
            return (this.kind == Kind.UPPER ? source.isAssignableFrom(from) : from.isAssignableFrom(source));
        }

        /**
         * Return the underlying bounds.
         */
        public ResolvableType[] getBounds() {
            return this.bounds;
        }

        /**
         * Get a {@link WildcardBounds} instance for the specified type, returning
         * {@code null} if the specified type cannot be resolved to a {@link WildcardType}.
         * @param type the source type
         * @return a {@link WildcardBounds} instance or {@code null}
         */
        public static WildcardBounds get(ResolvableType type) {
            ResolvableType resolveToWildcard = type;
            while (!(resolveToWildcard.getType() instanceof WildcardType)) {
                if (resolveToWildcard == NONE) {
                    return null;
                }
                resolveToWildcard = resolveToWildcard.resolveType();
            }
            WildcardType wildcardType = (WildcardType) resolveToWildcard.type;
            Kind boundsType = (wildcardType.getLowerBounds().length > 0 ? Kind.LOWER : Kind.UPPER);
            Type[] bounds = (boundsType == Kind.UPPER ? wildcardType.getUpperBounds() : wildcardType.getLowerBounds());
            ResolvableType[] resolvableBounds = new ResolvableType[bounds.length];
            for (int i = 0; i < bounds.length; i++) {
                resolvableBounds[i] = ResolvableType.forType(bounds[i], type.variableResolver);
            }
            return new WildcardBounds(boundsType, resolvableBounds);
        }

        /**
         * The various kinds of bounds.
         */
        enum Kind {UPPER, LOWER}
    }
}
