package org.myspring.expression.spel.support;

import org.myspring.core.MethodParameter;
import org.myspring.core.asm.MethodVisitor;
import org.myspring.core.convert.Property;
import org.myspring.core.convert.TypeDescriptor;
import org.myspring.core.util.ReflectionUtils;
import org.myspring.core.util.StringUtils;
import org.myspring.expression.*;
import org.myspring.expression.spel.CodeFlow;
import org.myspring.expression.spel.CompilablePropertyAccessor;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectivePropertyAccessor implements PropertyAccessor {

    private static final Set<Class<?>> ANY_TYPES = Collections.emptySet();

    private static final Set<Class<?>> BOOLEAN_TYPES;

    static {
        Set<Class<?>> booleanTypes = new HashSet<Class<?>>(4);
        booleanTypes.add(Boolean.class);
        booleanTypes.add(Boolean.TYPE);
        BOOLEAN_TYPES = Collections.unmodifiableSet(booleanTypes);
    }


    private final boolean allowWrite;

    private final Map<PropertyCacheKey, InvokerPair> readerCache =
            new ConcurrentHashMap<PropertyCacheKey, InvokerPair>(64);

    private final Map<PropertyCacheKey, Member> writerCache =
            new ConcurrentHashMap<PropertyCacheKey, Member>(64);

    private final Map<PropertyCacheKey, TypeDescriptor> typeDescriptorCache =
            new ConcurrentHashMap<PropertyCacheKey, TypeDescriptor>(64);

    private final Map<Class<?>, Method[]> sortedMethodsCache =
            new ConcurrentHashMap<Class<?>, Method[]>(64);

    private volatile InvokerPair lastReadInvokerPair;

    public ReflectivePropertyAccessor() {
        this.allowWrite = true;
    }

    public ReflectivePropertyAccessor(boolean allowWrite) {
        this.allowWrite = allowWrite;
    }

    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return null;
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
        if (target == null) {
            return false;
        }

        Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
        if (type.isArray() && name.equals("length")) {
            return true;
        }

        PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
        if (this.readerCache.containsKey(cacheKey)) {
            return true;
        }

        Method method = findGetterForProperty(name, type, target);
        if (method != null) {
            // Treat it like a property...
            // The readerCache will only contain gettable properties (let's not worry about setters for now).
            Property property = new Property(type, method, null);
            TypeDescriptor typeDescriptor = new TypeDescriptor(property);
            this.readerCache.put(cacheKey, new InvokerPair(method, typeDescriptor));
            this.typeDescriptorCache.put(cacheKey, typeDescriptor);
            return true;
        }
        else {
            Field field = findField(name, type, target);
            if (field != null) {
                TypeDescriptor typeDescriptor = new TypeDescriptor(field);
                this.readerCache.put(cacheKey, new InvokerPair(field, typeDescriptor));
                this.typeDescriptorCache.put(cacheKey, typeDescriptor);
                return true;
            }
        }

        return false;
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
        if (target == null) {
            throw new AccessException("Cannot read property of null target");
        }
        Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());

        if (type.isArray() && name.equals("length")) {
            if (target instanceof Class) {
                throw new AccessException("Cannot access length on array class itself");
            }
            return new TypedValue(Array.getLength(target));
        }

        PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
        InvokerPair invoker = this.readerCache.get(cacheKey);
        this.lastReadInvokerPair = invoker;

        if (invoker == null || invoker.member instanceof Method) {
            Method method = (Method) (invoker != null ? invoker.member : null);
            if (method == null) {
                method = findGetterForProperty(name, type, target);
                if (method != null) {
                    // Treat it like a property...
                    // The readerCache will only contain gettable properties (let's not worry about setters for now).
                    Property property = new Property(type, method, null);
                    TypeDescriptor typeDescriptor = new TypeDescriptor(property);
                    invoker = new InvokerPair(method, typeDescriptor);
                    this.lastReadInvokerPair = invoker;
                    this.readerCache.put(cacheKey, invoker);
                }
            }
            if (method != null) {
                try {
                    ReflectionUtils.makeAccessible(method);
                    Object value = method.invoke(target);
                    return new TypedValue(value, invoker.typeDescriptor.narrow(value));
                }
                catch (Exception ex) {
                    throw new AccessException("Unable to access property '" + name + "' through getter method", ex);
                }
            }
        }

        if (invoker == null || invoker.member instanceof Field) {
            Field field = (Field) (invoker == null ? null : invoker.member);
            if (field == null) {
                field = findField(name, type, target);
                if (field != null) {
                    invoker = new InvokerPair(field, new TypeDescriptor(field));
                    this.lastReadInvokerPair = invoker;
                    this.readerCache.put(cacheKey, invoker);
                }
            }
            if (field != null) {
                try {
                    ReflectionUtils.makeAccessible(field);
                    Object value = field.get(target);
                    return new TypedValue(value, invoker.typeDescriptor.narrow(value));
                }
                catch (Exception ex) {
                    throw new AccessException("Unable to access field '" + name + "'", ex);
                }
            }
        }

        throw new AccessException("Neither getter method nor field found for property '" + name + "'");
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
        if (!this.allowWrite || target == null) {
            return false;
        }

        Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
        PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
        if (this.writerCache.containsKey(cacheKey)) {
            return true;
        }

        Method method = findSetterForProperty(name, type, target);
        if (method != null) {
            // Treat it like a property
            Property property = new Property(type, null, method);
            TypeDescriptor typeDescriptor = new TypeDescriptor(property);
            this.writerCache.put(cacheKey, method);
            this.typeDescriptorCache.put(cacheKey, typeDescriptor);
            return true;
        }
        else {
            Field field = findField(name, type, target);
            if (field != null) {
                this.writerCache.put(cacheKey, field);
                this.typeDescriptorCache.put(cacheKey, new TypeDescriptor(field));
                return true;
            }
        }

        return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
        if (!this.allowWrite) {
            throw new AccessException("PropertyAccessor for property '" + name +
                    "' on target [" + target + "] does not allow write operations");
        }

        if (target == null) {
            throw new AccessException("Cannot write property on null target");
        }
        Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());

        Object possiblyConvertedNewValue = newValue;
        TypeDescriptor typeDescriptor = getTypeDescriptor(context, target, name);
        if (typeDescriptor != null) {
            try {
                possiblyConvertedNewValue = context.getTypeConverter().convertValue(
                        newValue, TypeDescriptor.forObject(newValue), typeDescriptor);
            }
            catch (EvaluationException evaluationException) {
                throw new AccessException("Type conversion failure", evaluationException);
            }
        }

        PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
        Member cachedMember = this.writerCache.get(cacheKey);

        if (cachedMember == null || cachedMember instanceof Method) {
            Method method = (Method) cachedMember;
            if (method == null) {
                method = findSetterForProperty(name, type, target);
                if (method != null) {
                    cachedMember = method;
                    this.writerCache.put(cacheKey, cachedMember);
                }
            }
            if (method != null) {
                try {
                    ReflectionUtils.makeAccessible(method);
                    method.invoke(target, possiblyConvertedNewValue);
                    return;
                }
                catch (Exception ex) {
                    throw new AccessException("Unable to access property '" + name + "' through setter method", ex);
                }
            }
        }

        if (cachedMember == null || cachedMember instanceof Field) {
            Field field = (Field) cachedMember;
            if (field == null) {
                field = findField(name, type, target);
                if (field != null) {
                    cachedMember = field;
                    this.writerCache.put(cacheKey, cachedMember);
                }
            }
            if (field != null) {
                try {
                    ReflectionUtils.makeAccessible(field);
                    field.set(target, possiblyConvertedNewValue);
                    return;
                }
                catch (Exception ex) {
                    throw new AccessException("Unable to access field '" + name + "'", ex);
                }
            }
        }

        throw new AccessException("Neither setter method nor field found for property '" + name + "'");
    }

    @Deprecated
    public Member getLastReadInvokerPair() {
        InvokerPair lastReadInvoker = this.lastReadInvokerPair;
        return (lastReadInvoker != null ? lastReadInvoker.member : null);
    }

    private TypeDescriptor getTypeDescriptor(EvaluationContext context, Object target, String name) {
        if (target == null) {
            return null;
        }
        Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());

        if (type.isArray() && name.equals("length")) {
            return TypeDescriptor.valueOf(Integer.TYPE);
        }
        PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
        TypeDescriptor typeDescriptor = this.typeDescriptorCache.get(cacheKey);
        if (typeDescriptor == null) {
            // Attempt to populate the cache entry
            try {
                if (canRead(context, target, name) || canWrite(context, target, name)) {
                    typeDescriptor = this.typeDescriptorCache.get(cacheKey);
                }
            }
            catch (AccessException ex) {
                // Continue with null type descriptor
            }
        }
        return typeDescriptor;
    }

    private Method findGetterForProperty(String propertyName, Class<?> clazz, Object target) {
        Method method = findGetterForProperty(propertyName, clazz, target instanceof Class);
        if (method == null && target instanceof Class) {
            method = findGetterForProperty(propertyName, target.getClass(), false);
        }
        return method;
    }

    private Method findSetterForProperty(String propertyName, Class<?> clazz, Object target) {
        Method method = findSetterForProperty(propertyName, clazz, target instanceof Class);
        if (method == null && target instanceof Class) {
            method = findSetterForProperty(propertyName, target.getClass(), false);
        }
        return method;
    }

    protected Method findGetterForProperty(String propertyName, Class<?> clazz, boolean mustBeStatic) {
        Method method = findMethodForProperty(getPropertyMethodSuffixes(propertyName),
                "get", clazz, mustBeStatic, 0, ANY_TYPES);
        if (method == null) {
            method = findMethodForProperty(getPropertyMethodSuffixes(propertyName),
                    "is", clazz, mustBeStatic, 0, BOOLEAN_TYPES);
        }
        return method;
    }

    protected Method findSetterForProperty(String propertyName, Class<?> clazz, boolean mustBeStatic) {
        return findMethodForProperty(getPropertyMethodSuffixes(propertyName),
                "set", clazz, mustBeStatic, 1, ANY_TYPES);
    }

    private Method findMethodForProperty(String[] methodSuffixes, String prefix, Class<?> clazz,
                                         boolean mustBeStatic, int numberOfParams, Set<Class<?>> requiredReturnTypes) {

        Method[] methods = getSortedMethods(clazz);
        for (String methodSuffix : methodSuffixes) {
            for (Method method : methods) {
                if (isCandidateForProperty(method, clazz) && method.getName().equals(prefix + methodSuffix) &&
                        method.getParameterTypes().length == numberOfParams &&
                        (!mustBeStatic || Modifier.isStatic(method.getModifiers())) &&
                        (requiredReturnTypes.isEmpty() || requiredReturnTypes.contains(method.getReturnType()))) {
                    return method;
                }
            }
        }
        return null;
    }

    protected boolean isCandidateForProperty(Method method, Class<?> targetClass) {
        return true;
    }

    /**
     * Return class methods ordered with non-bridge methods appearing higher.
     */
    private Method[] getSortedMethods(Class<?> clazz) {
        Method[] methods = this.sortedMethodsCache.get(clazz);
        if (methods == null) {
            methods = clazz.getMethods();
            Arrays.sort(methods, new Comparator<Method>() {
                @Override
                public int compare(Method o1, Method o2) {
                    return (o1.isBridge() == o2.isBridge()) ? 0 : (o1.isBridge() ? 1 : -1);
                }
            });
            this.sortedMethodsCache.put(clazz, methods);
        }
        return methods;
    }

    protected String[] getPropertyMethodSuffixes(String propertyName) {
        String suffix = getPropertyMethodSuffix(propertyName);
        if (suffix.length() > 0 && Character.isUpperCase(suffix.charAt(0))) {
            return new String[] {suffix};
        }
        return new String[] {suffix, StringUtils.capitalize(suffix)};
    }

    protected String getPropertyMethodSuffix(String propertyName) {
        if (propertyName.length() > 1 && Character.isUpperCase(propertyName.charAt(1))) {
            return propertyName;
        }
        return StringUtils.capitalize(propertyName);
    }

    private Field findField(String name, Class<?> clazz, Object target) {
        Field field = findField(name, clazz, target instanceof Class);
        if (field == null && target instanceof Class) {
            field = findField(name, target.getClass(), false);
        }
        return field;
    }

    protected Field findField(String name, Class<?> clazz, boolean mustBeStatic) {
        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            if (field.getName().equals(name) && (!mustBeStatic || Modifier.isStatic(field.getModifiers()))) {
                return field;
            }
        }
        // We'll search superclasses and implemented interfaces explicitly,
        // although it shouldn't be necessary - however, see SPR-10125.
        if (clazz.getSuperclass() != null) {
            Field field = findField(name, clazz.getSuperclass(), mustBeStatic);
            if (field != null) {
                return field;
            }
        }
        for (Class<?> implementedInterface : clazz.getInterfaces()) {
            Field field = findField(name, implementedInterface, mustBeStatic);
            if (field != null) {
                return field;
            }
        }
        return null;
    }

    public PropertyAccessor createOptimalAccessor(EvaluationContext context, Object target, String name) {
        // Don't be clever for arrays or a null target...
        if (target == null) {
            return this;
        }
        Class<?> clazz = (target instanceof Class ? (Class<?>) target : target.getClass());
        if (clazz.isArray()) {
            return this;
        }

        PropertyCacheKey cacheKey = new PropertyCacheKey(clazz, name, target instanceof Class);
        InvokerPair invocationTarget = this.readerCache.get(cacheKey);

        if (invocationTarget == null || invocationTarget.member instanceof Method) {
            Method method = (Method) (invocationTarget != null ? invocationTarget.member : null);
            if (method == null) {
                method = findGetterForProperty(name, clazz, target);
                if (method != null) {
                    invocationTarget = new InvokerPair(method, new TypeDescriptor(new MethodParameter(method, -1)));
                    ReflectionUtils.makeAccessible(method);
                    this.readerCache.put(cacheKey, invocationTarget);
                }
            }
            if (method != null) {
                return new OptimalPropertyAccessor(invocationTarget);
            }
        }

        if (invocationTarget == null || invocationTarget.member instanceof Field) {
            Field field = (invocationTarget != null ? (Field) invocationTarget.member : null);
            if (field == null) {
                field = findField(name, clazz, target instanceof Class);
                if (field != null) {
                    invocationTarget = new InvokerPair(field, new TypeDescriptor(field));
                    ReflectionUtils.makeAccessible(field);
                    this.readerCache.put(cacheKey, invocationTarget);
                }
            }
            if (field != null) {
                return new OptimalPropertyAccessor(invocationTarget);
            }
        }

        return this;
    }


    //===============================================
    private static class InvokerPair {

        final Member member;

        final TypeDescriptor typeDescriptor;

        public InvokerPair(Member member, TypeDescriptor typeDescriptor) {
            this.member = member;
            this.typeDescriptor = typeDescriptor;
        }
    }

    private static final class PropertyCacheKey implements Comparable<PropertyCacheKey> {

        private final Class<?> clazz;

        private final String property;

        private boolean targetIsClass;

        public PropertyCacheKey(Class<?> clazz, String name, boolean targetIsClass) {
            this.clazz = clazz;
            this.property = name;
            this.targetIsClass = targetIsClass;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof PropertyCacheKey)) {
                return false;
            }
            PropertyCacheKey otherKey = (PropertyCacheKey) other;
            return (this.clazz == otherKey.clazz && this.property.equals(otherKey.property) &&
                    this.targetIsClass == otherKey.targetIsClass);
        }

        @Override
        public int hashCode() {
            return (this.clazz.hashCode() * 29 + this.property.hashCode());
        }

        @Override
        public String toString() {
            return "CacheKey [clazz=" + this.clazz.getName() + ", property=" + this.property + ", " +
                    this.property + ", targetIsClass=" + this.targetIsClass + "]";
        }

        @Override
        public int compareTo(PropertyCacheKey other) {
            int result = this.clazz.getName().compareTo(other.clazz.getName());
            if (result == 0) {
                result = this.property.compareTo(other.property);
            }
            return result;
        }
    }


    public static class OptimalPropertyAccessor implements CompilablePropertyAccessor {

        public final Member member;

        private final TypeDescriptor typeDescriptor;

        private final boolean needsToBeMadeAccessible;

        OptimalPropertyAccessor(InvokerPair target) {
            this.member = target.member;
            this.typeDescriptor = target.typeDescriptor;
            this.needsToBeMadeAccessible = (!Modifier.isPublic(this.member.getModifiers()) ||
                    !Modifier.isPublic(this.member.getDeclaringClass().getModifiers()));
        }

        @Override
        public Class<?>[] getSpecificTargetClasses() {
            throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
        }

        @Override
        public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
            if (target == null) {
                return false;
            }
            Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
            if (type.isArray()) {
                return false;
            }

            if (this.member instanceof Method) {
                Method method = (Method) this.member;
                String getterName = "get" + StringUtils.capitalize(name);
                if (getterName.equals(method.getName())) {
                    return true;
                }
                getterName = "is" + StringUtils.capitalize(name);
                return getterName.equals(method.getName());
            }
            else {
                Field field = (Field) this.member;
                return field.getName().equals(name);
            }
        }

        @Override
        public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
            if (this.member instanceof Method) {
                Method method = (Method) this.member;
                try {
                    if (this.needsToBeMadeAccessible && !method.isAccessible()) {
                        method.setAccessible(true);
                    }
                    Object value = method.invoke(target);
                    return new TypedValue(value, this.typeDescriptor.narrow(value));
                }
                catch (Exception ex) {
                    throw new AccessException("Unable to access property '" + name + "' through getter method", ex);
                }
            }
            else {
                Field field = (Field) this.member;
                try {
                    if (this.needsToBeMadeAccessible && !field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    Object value = field.get(target);
                    return new TypedValue(value, this.typeDescriptor.narrow(value));
                }
                catch (Exception ex) {
                    throw new AccessException("Unable to access field '" + name + "'", ex);
                }
            }
        }

        @Override
        public boolean canWrite(EvaluationContext context, Object target, String name) {
            throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
        }

        @Override
        public void write(EvaluationContext context, Object target, String name, Object newValue) {
            throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
        }

        @Override
        public boolean isCompilable() {
            return (Modifier.isPublic(this.member.getModifiers()) &&
                    Modifier.isPublic(this.member.getDeclaringClass().getModifiers()));
        }

        @Override
        public Class<?> getPropertyType() {
            if (this.member instanceof Method) {
                return ((Method) this.member).getReturnType();
            }
            else {
                return ((Field) this.member).getType();
            }
        }

        @Override
        public void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf) {
            boolean isStatic = Modifier.isStatic(this.member.getModifiers());
            String descriptor = cf.lastDescriptor();
            String classDesc = this.member.getDeclaringClass().getName().replace('.', '/');

            if (!isStatic) {
                if (descriptor == null) {
                    cf.loadTarget(mv);
                }
                if (descriptor == null || !classDesc.equals(descriptor.substring(1))) {
                    mv.visitTypeInsn(CHECKCAST, classDesc);
                }
            }
            else {
                if (descriptor != null) {
                    // A static field/method call will not consume what is on the stack,
                    // it needs to be popped off.
                    mv.visitInsn(POP);
                }
            }

            if (this.member instanceof Method) {
                mv.visitMethodInsn((isStatic ? INVOKESTATIC : INVOKEVIRTUAL), classDesc, this.member.getName(),
                        CodeFlow.createSignatureDescriptor((Method) this.member), false);
            }
            else {
                mv.visitFieldInsn((isStatic ? GETSTATIC : GETFIELD), classDesc, this.member.getName(),
                        CodeFlow.toJvmDescriptor(((Field) this.member).getType()));
            }
        }
    }





}
