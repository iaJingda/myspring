package org.myspring.core.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.myspring.core.BridgeMethodResolver;
import org.myspring.core.util.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public abstract class AnnotationUtils {

    public static final String VALUE = "value";

    private static final String REPEATABLE_CLASS_NAME = "java.lang.annotation.Repeatable";

    private static final Map<AnnotationCacheKey, Annotation> findAnnotationCache =
            new ConcurrentReferenceHashMap<AnnotationCacheKey, Annotation>(256);

    private static final Map<AnnotationCacheKey, Boolean> metaPresentCache =
            new ConcurrentReferenceHashMap<AnnotationCacheKey, Boolean>(256);

    private static final Map<Class<?>, Boolean> annotatedInterfaceCache =
            new ConcurrentReferenceHashMap<Class<?>, Boolean>(256);

    private static final Map<Class<? extends Annotation>, Boolean> synthesizableCache =
            new ConcurrentReferenceHashMap<Class<? extends Annotation>, Boolean>(256);

    private static final Map<Class<? extends Annotation>, Map<String, List<String>>> attributeAliasesCache =
            new ConcurrentReferenceHashMap<Class<? extends Annotation>, Map<String, List<String>>>(256);

    private static final Map<Class<? extends Annotation>, List<Method>> attributeMethodsCache =
            new ConcurrentReferenceHashMap<Class<? extends Annotation>, List<Method>>(256);

    private static final Map<Method, AliasDescriptor> aliasDescriptorCache =
            new ConcurrentReferenceHashMap<Method, AliasDescriptor>(256);

    private static transient Log logger;

    public static <A extends Annotation> A getAnnotation(Annotation annotation, Class<A> annotationType) {
        if (annotationType.isInstance(annotation)) {
            return synthesizeAnnotation((A) annotation);
        }
        Class<? extends Annotation> annotatedElement = annotation.annotationType();
        try {
            return synthesizeAnnotation(annotatedElement.getAnnotation(annotationType), annotatedElement);
        }
        catch (Throwable ex) {
            handleIntrospectionFailure(annotatedElement, ex);
            return null;
        }
    }

    public static <A extends Annotation> A getAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
        try {
            A annotation = annotatedElement.getAnnotation(annotationType);
            if (annotation == null) {
                for (Annotation metaAnn : annotatedElement.getAnnotations()) {
                    annotation = metaAnn.annotationType().getAnnotation(annotationType);
                    if (annotation != null) {
                        break;
                    }
                }
            }
            return synthesizeAnnotation(annotation, annotatedElement);
        }
        catch (Throwable ex) {
            handleIntrospectionFailure(annotatedElement, ex);
            return null;
        }
    }

    public static <A extends Annotation> A getAnnotation(Method method, Class<A> annotationType) {
        Method resolvedMethod = BridgeMethodResolver.findBridgedMethod(method);
        return getAnnotation((AnnotatedElement) resolvedMethod, annotationType);
    }

    public static Annotation[] getAnnotations(AnnotatedElement annotatedElement) {
        try {
            return synthesizeAnnotationArray(annotatedElement.getAnnotations(), annotatedElement);
        }
        catch (Throwable ex) {
            handleIntrospectionFailure(annotatedElement, ex);
            return null;
        }
    }

    public static Annotation[] getAnnotations(Method method) {
        try {
            return synthesizeAnnotationArray(BridgeMethodResolver.findBridgedMethod(method).getAnnotations(), method);
        }
        catch (Throwable ex) {
            handleIntrospectionFailure(method, ex);
            return null;
        }
    }
    @Deprecated
    public static <A extends Annotation> Set<A> getRepeatableAnnotation(Method method,
                                                                        Class<? extends Annotation> containerAnnotationType, Class<A> annotationType) {

        return getRepeatableAnnotations(method, annotationType, containerAnnotationType);
    }

    @Deprecated
    public static <A extends Annotation> Set<A> getRepeatableAnnotation(AnnotatedElement annotatedElement,
                                                                        Class<? extends Annotation> containerAnnotationType, Class<A> annotationType) {

        return getRepeatableAnnotations(annotatedElement, annotationType, containerAnnotationType);
    }

    public static <A extends Annotation> Set<A> getRepeatableAnnotations(AnnotatedElement annotatedElement,
                                                                         Class<A> annotationType) {

        return getRepeatableAnnotations(annotatedElement, annotationType, null);
    }
    public static <A extends Annotation> Set<A> getRepeatableAnnotations(AnnotatedElement annotatedElement,
                                                                         Class<A> annotationType, Class<? extends Annotation> containerAnnotationType) {

        Set<A> annotations = getDeclaredRepeatableAnnotations(annotatedElement, annotationType, containerAnnotationType);
        if (!annotations.isEmpty()) {
            return annotations;
        }

        if (annotatedElement instanceof Class) {
            Class<?> superclass = ((Class<?>) annotatedElement).getSuperclass();
            if (superclass != null && Object.class != superclass) {
                return getRepeatableAnnotations(superclass, annotationType, containerAnnotationType);
            }
        }

        return getRepeatableAnnotations(annotatedElement, annotationType, containerAnnotationType, false);
    }

    public static <A extends Annotation> Set<A> getDeclaredRepeatableAnnotations(AnnotatedElement annotatedElement,
                                                                                 Class<A> annotationType) {

        return getDeclaredRepeatableAnnotations(annotatedElement, annotationType, null);
    }

    public static <A extends Annotation> Set<A> getDeclaredRepeatableAnnotations(AnnotatedElement annotatedElement,
                                                                                 Class<A> annotationType, Class<? extends Annotation> containerAnnotationType) {

        return getRepeatableAnnotations(annotatedElement, annotationType, containerAnnotationType, true);
    }

    private static <A extends Annotation> Set<A> getRepeatableAnnotations(AnnotatedElement annotatedElement,
                                                                          Class<A> annotationType, Class<? extends Annotation> containerAnnotationType, boolean declaredMode) {

        Assert.notNull(annotatedElement, "AnnotatedElement must not be null");
        Assert.notNull(annotationType, "Annotation type must not be null");

        try {
            if (annotatedElement instanceof Method) {
                annotatedElement = BridgeMethodResolver.findBridgedMethod((Method) annotatedElement);
            }
            return new AnnotationCollector<A>(annotationType, containerAnnotationType, declaredMode).getResult(annotatedElement);
        }
        catch (Throwable ex) {
            handleIntrospectionFailure(annotatedElement, ex);
            return Collections.emptySet();
        }
    }

    public static <A extends Annotation> A findAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
        Assert.notNull(annotatedElement, "AnnotatedElement must not be null");
        if (annotationType == null) {
            return null;
        }

        // Do NOT store result in the findAnnotationCache since doing so could break
        // findAnnotation(Class, Class) and findAnnotation(Method, Class).
        A ann = findAnnotation(annotatedElement, annotationType, new HashSet<Annotation>());
        return synthesizeAnnotation(ann, annotatedElement);
    }

    private static <A extends Annotation> A findAnnotation(
            AnnotatedElement annotatedElement, Class<A> annotationType, Set<Annotation> visited) {
        try {
            Annotation[] anns = annotatedElement.getDeclaredAnnotations();
            for (Annotation ann : anns) {
                if (ann.annotationType() == annotationType) {
                    return (A) ann;
                }
            }
            for (Annotation ann : anns) {
                if (!isInJavaLangAnnotationPackage(ann) && visited.add(ann)) {
                    A annotation = findAnnotation((AnnotatedElement) ann.annotationType(), annotationType, visited);
                    if (annotation != null) {
                        return annotation;
                    }
                }
            }
        }
        catch (Throwable ex) {
            handleIntrospectionFailure(annotatedElement, ex);
        }
        return null;
    }

    public static <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType) {
        Assert.notNull(method, "Method must not be null");
        if (annotationType == null) {
            return null;
        }

        AnnotationCacheKey cacheKey = new AnnotationCacheKey(method, annotationType);
        A result = (A) findAnnotationCache.get(cacheKey);

        if (result == null) {
            Method resolvedMethod = BridgeMethodResolver.findBridgedMethod(method);
            result = findAnnotation((AnnotatedElement) resolvedMethod, annotationType);
            if (result == null) {
                result = searchOnInterfaces(method, annotationType, method.getDeclaringClass().getInterfaces());
            }

            Class<?> clazz = method.getDeclaringClass();
            while (result == null) {
                clazz = clazz.getSuperclass();
                if (clazz == null || Object.class == clazz) {
                    break;
                }
                try {
                    Method equivalentMethod = clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
                    Method resolvedEquivalentMethod = BridgeMethodResolver.findBridgedMethod(equivalentMethod);
                    result = findAnnotation((AnnotatedElement) resolvedEquivalentMethod, annotationType);
                }
                catch (NoSuchMethodException ex) {
                    // No equivalent method found
                }
                if (result == null) {
                    result = searchOnInterfaces(method, annotationType, clazz.getInterfaces());
                }
            }

            if (result != null) {
                result = synthesizeAnnotation(result, method);
                findAnnotationCache.put(cacheKey, result);
            }
        }

        return result;
    }

    private static <A extends Annotation> A searchOnInterfaces(Method method, Class<A> annotationType, Class<?>... ifcs) {
        A annotation = null;
        for (Class<?> ifc : ifcs) {
            if (isInterfaceWithAnnotatedMethods(ifc)) {
                try {
                    Method equivalentMethod = ifc.getMethod(method.getName(), method.getParameterTypes());
                    annotation = getAnnotation(equivalentMethod, annotationType);
                }
                catch (NoSuchMethodException ex) {
                    // Skip this interface - it doesn't have the method...
                }
                if (annotation != null) {
                    break;
                }
            }
        }
        return annotation;
    }

    static boolean isInterfaceWithAnnotatedMethods(Class<?> ifc) {
        Boolean found = annotatedInterfaceCache.get(ifc);
        if (found != null) {
            return found;
        }
        found = Boolean.FALSE;
        for (Method ifcMethod : ifc.getMethods()) {
            try {
                if (ifcMethod.getAnnotations().length > 0) {
                    found = Boolean.TRUE;
                    break;
                }
            }
            catch (Throwable ex) {
                handleIntrospectionFailure(ifcMethod, ex);
            }
        }
        annotatedInterfaceCache.put(ifc, found);
        return found;
    }
    public static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType) {
        return findAnnotation(clazz, annotationType, true);
    }

    private static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType, boolean synthesize) {
        Assert.notNull(clazz, "Class must not be null");
        if (annotationType == null) {
            return null;
        }

        AnnotationCacheKey cacheKey = new AnnotationCacheKey(clazz, annotationType);
        A result = (A) findAnnotationCache.get(cacheKey);
        if (result == null) {
            result = findAnnotation(clazz, annotationType, new HashSet<Annotation>());
            if (result != null && synthesize) {
                result = synthesizeAnnotation(result, clazz);
                findAnnotationCache.put(cacheKey, result);
            }
        }
        return result;
    }

    private static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType, Set<Annotation> visited) {
        try {
            Annotation[] anns = clazz.getDeclaredAnnotations();
            for (Annotation ann : anns) {
                if (ann.annotationType() == annotationType) {
                    return (A) ann;
                }
            }
            for (Annotation ann : anns) {
                if (!isInJavaLangAnnotationPackage(ann) && visited.add(ann)) {
                    A annotation = findAnnotation(ann.annotationType(), annotationType, visited);
                    if (annotation != null) {
                        return annotation;
                    }
                }
            }
        }
        catch (Throwable ex) {
            handleIntrospectionFailure(clazz, ex);
            return null;
        }

        for (Class<?> ifc : clazz.getInterfaces()) {
            A annotation = findAnnotation(ifc, annotationType, visited);
            if (annotation != null) {
                return annotation;
            }
        }

        Class<?> superclass = clazz.getSuperclass();
        if (superclass == null || Object.class == superclass) {
            return null;
        }
        return findAnnotation(superclass, annotationType, visited);
    }

    public static Class<?> findAnnotationDeclaringClass(Class<? extends Annotation> annotationType, Class<?> clazz) {
        Assert.notNull(annotationType, "Annotation type must not be null");
        if (clazz == null || Object.class == clazz) {
            return null;
        }
        if (isAnnotationDeclaredLocally(annotationType, clazz)) {
            return clazz;
        }
        return findAnnotationDeclaringClass(annotationType, clazz.getSuperclass());
    }

    public static Class<?> findAnnotationDeclaringClassForTypes(List<Class<? extends Annotation>> annotationTypes, Class<?> clazz) {
        Assert.notEmpty(annotationTypes, "List of annotation types must not be empty");
        if (clazz == null || Object.class == clazz) {
            return null;
        }
        for (Class<? extends Annotation> annotationType : annotationTypes) {
            if (isAnnotationDeclaredLocally(annotationType, clazz)) {
                return clazz;
            }
        }
        return findAnnotationDeclaringClassForTypes(annotationTypes, clazz.getSuperclass());
    }
    public static boolean isAnnotationDeclaredLocally(Class<? extends Annotation> annotationType, Class<?> clazz) {
        Assert.notNull(annotationType, "Annotation type must not be null");
        Assert.notNull(clazz, "Class must not be null");
        try {
            for (Annotation ann : clazz.getDeclaredAnnotations()) {
                if (ann.annotationType() == annotationType) {
                    return true;
                }
            }
        }
        catch (Throwable ex) {
            handleIntrospectionFailure(clazz, ex);
        }
        return false;
    }
    public static boolean isAnnotationInherited(Class<? extends Annotation> annotationType, Class<?> clazz) {
        Assert.notNull(annotationType, "Annotation type must not be null");
        Assert.notNull(clazz, "Class must not be null");
        return (clazz.isAnnotationPresent(annotationType) && !isAnnotationDeclaredLocally(annotationType, clazz));
    }
    public static boolean isAnnotationMetaPresent(Class<? extends Annotation> annotationType,
                                                  Class<? extends Annotation> metaAnnotationType) {

        Assert.notNull(annotationType, "Annotation type must not be null");
        if (metaAnnotationType == null) {
            return false;
        }

        AnnotationCacheKey cacheKey = new AnnotationCacheKey(annotationType, metaAnnotationType);
        Boolean metaPresent = metaPresentCache.get(cacheKey);
        if (metaPresent != null) {
            return metaPresent;
        }
        metaPresent = Boolean.FALSE;
        if (findAnnotation(annotationType, metaAnnotationType, false) != null) {
            metaPresent = Boolean.TRUE;
        }
        metaPresentCache.put(cacheKey, metaPresent);
        return metaPresent;
    }

    public static boolean isInJavaLangAnnotationPackage(Annotation annotation) {
        return (annotation != null && isInJavaLangAnnotationPackage(annotation.annotationType()));
    }

    static boolean isInJavaLangAnnotationPackage(Class<? extends Annotation> annotationType) {
        return (annotationType != null && isInJavaLangAnnotationPackage(annotationType.getName()));
    }

    public static boolean isInJavaLangAnnotationPackage(String annotationType) {
        return (annotationType != null && annotationType.startsWith("java.lang.annotation"));
    }
    public static void validateAnnotation(Annotation annotation) {
        for (Method method : getAttributeMethods(annotation.annotationType())) {
            Class<?> returnType = method.getReturnType();
            if (returnType == Class.class || returnType == Class[].class) {
                try {
                    method.invoke(annotation);
                }
                catch (Throwable ex) {
                    throw new IllegalStateException("Could not obtain annotation attribute value for " + method, ex);
                }
            }
        }
    }

    public static Map<String, Object> getAnnotationAttributes(Annotation annotation) {
        return getAnnotationAttributes(null, annotation);
    }

    public static Map<String, Object> getAnnotationAttributes(Annotation annotation, boolean classValuesAsString) {
        return getAnnotationAttributes(annotation, classValuesAsString, false);
    }

    public static AnnotationAttributes getAnnotationAttributes(Annotation annotation, boolean classValuesAsString,
                                                               boolean nestedAnnotationsAsMap) {

        return getAnnotationAttributes(null, annotation, classValuesAsString, nestedAnnotationsAsMap);
    }

    public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement annotatedElement, Annotation annotation) {
        return getAnnotationAttributes(annotatedElement, annotation, false, false);
    }

    public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement annotatedElement,
                                                               Annotation annotation, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

        return getAnnotationAttributes(
                (Object) annotatedElement, annotation, classValuesAsString, nestedAnnotationsAsMap);
    }

    private static AnnotationAttributes getAnnotationAttributes(Object annotatedElement,
                                                                Annotation annotation, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

        AnnotationAttributes attributes =
                retrieveAnnotationAttributes(annotatedElement, annotation, classValuesAsString, nestedAnnotationsAsMap);
        postProcessAnnotationAttributes(annotatedElement, attributes, classValuesAsString, nestedAnnotationsAsMap);
        return attributes;
    }

    static AnnotationAttributes retrieveAnnotationAttributes(Object annotatedElement, Annotation annotation,
                                                             boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

        Class<? extends Annotation> annotationType = annotation.annotationType();
        AnnotationAttributes attributes = new AnnotationAttributes(annotationType);

        for (Method method : getAttributeMethods(annotationType)) {
            try {
                Object attributeValue = method.invoke(annotation);
                Object defaultValue = method.getDefaultValue();
                if (defaultValue != null && ObjectUtils.nullSafeEquals(attributeValue, defaultValue)) {
                    attributeValue = new DefaultValueHolder(defaultValue);
                }
                attributes.put(method.getName(),
                        adaptValue(annotatedElement, attributeValue, classValuesAsString, nestedAnnotationsAsMap));
            }
            catch (Throwable ex) {
                if (ex instanceof InvocationTargetException) {
                    Throwable targetException = ((InvocationTargetException) ex).getTargetException();
                    rethrowAnnotationConfigurationException(targetException);
                }
                throw new IllegalStateException("Could not obtain annotation attribute value for " + method, ex);
            }
        }

        return attributes;
    }

    static Object adaptValue(Object annotatedElement, Object value, boolean classValuesAsString,
                             boolean nestedAnnotationsAsMap) {

        if (classValuesAsString) {
            if (value instanceof Class) {
                return ((Class<?>) value).getName();
            }
            else if (value instanceof Class[]) {
                Class<?>[] clazzArray = (Class<?>[]) value;
                String[] classNames = new String[clazzArray.length];
                for (int i = 0; i < clazzArray.length; i++) {
                    classNames[i] = clazzArray[i].getName();
                }
                return classNames;
            }
        }

        if (value instanceof Annotation) {
            Annotation annotation = (Annotation) value;
            if (nestedAnnotationsAsMap) {
                return getAnnotationAttributes(annotatedElement, annotation, classValuesAsString, true);
            }
            else {
                return synthesizeAnnotation(annotation, annotatedElement);
            }
        }

        if (value instanceof Annotation[]) {
            Annotation[] annotations = (Annotation[]) value;
            if (nestedAnnotationsAsMap) {
                AnnotationAttributes[] mappedAnnotations = new AnnotationAttributes[annotations.length];
                for (int i = 0; i < annotations.length; i++) {
                    mappedAnnotations[i] =
                            getAnnotationAttributes(annotatedElement, annotations[i], classValuesAsString, true);
                }
                return mappedAnnotations;
            }
            else {
                return synthesizeAnnotationArray(annotations, annotatedElement);
            }
        }

        // Fallback
        return value;
    }

    public static void registerDefaultValues(AnnotationAttributes attributes) {
        // Only do defaults scanning for public annotations; we'd run into
        // IllegalAccessExceptions otherwise, and we don't want to mess with
        // accessibility in a SecurityManager environment.
        Class<? extends Annotation> annotationType = attributes.annotationType();
        if (annotationType != null && Modifier.isPublic(annotationType.getModifiers())) {
            // Check declared default values of attributes in the annotation type.
            for (Method annotationAttribute : getAttributeMethods(annotationType)) {
                String attributeName = annotationAttribute.getName();
                Object defaultValue = annotationAttribute.getDefaultValue();
                if (defaultValue != null && !attributes.containsKey(attributeName)) {
                    if (defaultValue instanceof Annotation) {
                        defaultValue = getAnnotationAttributes((Annotation) defaultValue, false, true);
                    }
                    else if (defaultValue instanceof Annotation[]) {
                        Annotation[] realAnnotations = (Annotation[]) defaultValue;
                        AnnotationAttributes[] mappedAnnotations = new AnnotationAttributes[realAnnotations.length];
                        for (int i = 0; i < realAnnotations.length; i++) {
                            mappedAnnotations[i] = getAnnotationAttributes(realAnnotations[i], false, true);
                        }
                        defaultValue = mappedAnnotations;
                    }
                    attributes.put(attributeName, new DefaultValueHolder(defaultValue));
                }
            }
        }
    }

    public static void postProcessAnnotationAttributes(Object annotatedElement,
                                                       AnnotationAttributes attributes, boolean classValuesAsString) {

        postProcessAnnotationAttributes(annotatedElement, attributes, classValuesAsString, false);
    }

    static void postProcessAnnotationAttributes(Object annotatedElement,
                                                AnnotationAttributes attributes, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

        // Abort?
        if (attributes == null) {
            return;
        }

        Class<? extends Annotation> annotationType = attributes.annotationType();

        // Track which attribute values have already been replaced so that we can short
        // circuit the search algorithms.
        Set<String> valuesAlreadyReplaced = new HashSet<String>();

        if (!attributes.validated) {
            // Validate @AliasFor configuration
            Map<String, List<String>> aliasMap = getAttributeAliasMap(annotationType);
            for (String attributeName : aliasMap.keySet()) {
                if (valuesAlreadyReplaced.contains(attributeName)) {
                    continue;
                }
                Object value = attributes.get(attributeName);
                boolean valuePresent = (value != null && !(value instanceof DefaultValueHolder));
                for (String aliasedAttributeName : aliasMap.get(attributeName)) {
                    if (valuesAlreadyReplaced.contains(aliasedAttributeName)) {
                        continue;
                    }
                    Object aliasedValue = attributes.get(aliasedAttributeName);
                    boolean aliasPresent = (aliasedValue != null && !(aliasedValue instanceof DefaultValueHolder));
                    // Something to validate or replace with an alias?
                    if (valuePresent || aliasPresent) {
                        if (valuePresent && aliasPresent) {
                            // Since annotation attributes can be arrays, we must use ObjectUtils.nullSafeEquals().
                            if (!ObjectUtils.nullSafeEquals(value, aliasedValue)) {
                                String elementAsString =
                                        (annotatedElement != null ? annotatedElement.toString() : "unknown element");
                                throw new AnnotationConfigurationException(String.format(
                                        "In AnnotationAttributes for annotation [%s] declared on %s, " +
                                                "attribute '%s' and its alias '%s' are declared with values of [%s] and [%s], " +
                                                "but only one is permitted.", annotationType.getName(), elementAsString,
                                        attributeName, aliasedAttributeName, ObjectUtils.nullSafeToString(value),
                                        ObjectUtils.nullSafeToString(aliasedValue)));
                            }
                        }
                        else if (aliasPresent) {
                            // Replace value with aliasedValue
                            attributes.put(attributeName,
                                    adaptValue(annotatedElement, aliasedValue, classValuesAsString, nestedAnnotationsAsMap));
                            valuesAlreadyReplaced.add(attributeName);
                        }
                        else {
                            // Replace aliasedValue with value
                            attributes.put(aliasedAttributeName,
                                    adaptValue(annotatedElement, value, classValuesAsString, nestedAnnotationsAsMap));
                            valuesAlreadyReplaced.add(aliasedAttributeName);
                        }
                    }
                }
            }
            attributes.validated = true;
        }

        // Replace any remaining placeholders with actual default values
        for (String attributeName : attributes.keySet()) {
            if (valuesAlreadyReplaced.contains(attributeName)) {
                continue;
            }
            Object value = attributes.get(attributeName);
            if (value instanceof DefaultValueHolder) {
                value = ((DefaultValueHolder) value).defaultValue;
                attributes.put(attributeName,
                        adaptValue(annotatedElement, value, classValuesAsString, nestedAnnotationsAsMap));
            }
        }
    }

    public static Object getValue(Annotation annotation) {
        return getValue(annotation, VALUE);
    }

    public static Object getValue(Annotation annotation, String attributeName) {
        if (annotation == null || !StringUtils.hasText(attributeName)) {
            return null;
        }
        try {
            Method method = annotation.annotationType().getDeclaredMethod(attributeName);
            ReflectionUtils.makeAccessible(method);
            return method.invoke(annotation);
        }
        catch (NoSuchMethodException ex) {
            return null;
        }
        catch (InvocationTargetException ex) {
            rethrowAnnotationConfigurationException(ex.getTargetException());
            throw new IllegalStateException(
                    "Could not obtain value for annotation attribute '" + attributeName + "' in " + annotation, ex);
        }
        catch (Throwable ex) {
            handleIntrospectionFailure(annotation.getClass(), ex);
            return null;
        }
    }

    public static Object getDefaultValue(Annotation annotation) {
        return getDefaultValue(annotation, VALUE);
    }

    public static Object getDefaultValue(Annotation annotation, String attributeName) {
        if (annotation == null) {
            return null;
        }
        return getDefaultValue(annotation.annotationType(), attributeName);
    }

    public static Object getDefaultValue(Class<? extends Annotation> annotationType) {
        return getDefaultValue(annotationType, VALUE);
    }

    public static Object getDefaultValue(Class<? extends Annotation> annotationType, String attributeName) {
        if (annotationType == null || !StringUtils.hasText(attributeName)) {
            return null;
        }
        try {
            return annotationType.getDeclaredMethod(attributeName).getDefaultValue();
        }
        catch (Throwable ex) {
            handleIntrospectionFailure(annotationType, ex);
            return null;
        }
    }

    static <A extends Annotation> A synthesizeAnnotation(A annotation) {
        return synthesizeAnnotation(annotation, null);
    }

    public static <A extends Annotation> A synthesizeAnnotation(A annotation, AnnotatedElement annotatedElement) {
        return synthesizeAnnotation(annotation, (Object) annotatedElement);
    }

    static <A extends Annotation> A synthesizeAnnotation(A annotation, Object annotatedElement) {
        if (annotation == null) {
            return null;
        }
        if (annotation instanceof SynthesizedAnnotation) {
            return annotation;
        }

        Class<? extends Annotation> annotationType = annotation.annotationType();
        if (!isSynthesizable(annotationType)) {
            return annotation;
        }

        DefaultAnnotationAttributeExtractor attributeExtractor =
                new DefaultAnnotationAttributeExtractor(annotation, annotatedElement);
        InvocationHandler handler = new SynthesizedAnnotationInvocationHandler(attributeExtractor);

        // Can always expose Spring's SynthesizedAnnotation marker since we explicitly check for a
        // synthesizable annotation before (which needs to declare @AliasFor from the same package)
        Class<?>[] exposedInterfaces = new Class<?>[] {annotationType, SynthesizedAnnotation.class};
        return (A) Proxy.newProxyInstance(annotation.getClass().getClassLoader(), exposedInterfaces, handler);
    }

    public static <A extends Annotation> A synthesizeAnnotation(Map<String, Object> attributes,
                                                                Class<A> annotationType, AnnotatedElement annotatedElement) {

        Assert.notNull(annotationType, "'annotationType' must not be null");
        if (attributes == null) {
            return null;
        }

        MapAnnotationAttributeExtractor attributeExtractor =
                new MapAnnotationAttributeExtractor(attributes, annotationType, annotatedElement);
        InvocationHandler handler = new SynthesizedAnnotationInvocationHandler(attributeExtractor);
        Class<?>[] exposedInterfaces = (canExposeSynthesizedMarker(annotationType) ?
                new Class<?>[] {annotationType, SynthesizedAnnotation.class} : new Class<?>[] {annotationType});
        return (A) Proxy.newProxyInstance(annotationType.getClassLoader(), exposedInterfaces, handler);
    }


    public static <A extends Annotation> A synthesizeAnnotation(Class<A> annotationType) {
        return synthesizeAnnotation(Collections.<String, Object> emptyMap(), annotationType, null);
    }

    static Annotation[] synthesizeAnnotationArray(Annotation[] annotations, Object annotatedElement) {
        if (annotations == null) {
            return null;
        }

        Annotation[] synthesized = (Annotation[]) Array.newInstance(
                annotations.getClass().getComponentType(), annotations.length);
        for (int i = 0; i < annotations.length; i++) {
            synthesized[i] = synthesizeAnnotation(annotations[i], annotatedElement);
        }
        return synthesized;
    }

    static <A extends Annotation> A[] synthesizeAnnotationArray(Map<String, Object>[] maps, Class<A> annotationType) {
        Assert.notNull(annotationType, "'annotationType' must not be null");
        if (maps == null) {
            return null;
        }

        A[] synthesized = (A[]) Array.newInstance(annotationType, maps.length);
        for (int i = 0; i < maps.length; i++) {
            synthesized[i] = synthesizeAnnotation(maps[i], annotationType, null);
        }
        return synthesized;
    }

    static Map<String, List<String>> getAttributeAliasMap(Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> map = attributeAliasesCache.get(annotationType);
        if (map != null) {
            return map;
        }

        map = new LinkedHashMap<String, List<String>>();
        for (Method attribute : getAttributeMethods(annotationType)) {
            List<String> aliasNames = getAttributeAliasNames(attribute);
            if (!aliasNames.isEmpty()) {
                map.put(attribute.getName(), aliasNames);
            }
        }

        attributeAliasesCache.put(annotationType, map);
        return map;
    }

    private static boolean canExposeSynthesizedMarker(Class<? extends Annotation> annotationType) {
        try {
            return (Class.forName(SynthesizedAnnotation.class.getName(), false, annotationType.getClassLoader()) ==
                    SynthesizedAnnotation.class);
        }
        catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private static boolean isSynthesizable(Class<? extends Annotation> annotationType) {
        Boolean synthesizable = synthesizableCache.get(annotationType);
        if (synthesizable != null) {
            return synthesizable;
        }

        synthesizable = Boolean.FALSE;
        for (Method attribute : getAttributeMethods(annotationType)) {
            if (!getAttributeAliasNames(attribute).isEmpty()) {
                synthesizable = Boolean.TRUE;
                break;
            }
            Class<?> returnType = attribute.getReturnType();
            if (Annotation[].class.isAssignableFrom(returnType)) {
                Class<? extends Annotation> nestedAnnotationType =
                        (Class<? extends Annotation>) returnType.getComponentType();
                if (isSynthesizable(nestedAnnotationType)) {
                    synthesizable = Boolean.TRUE;
                    break;
                }
            }
            else if (Annotation.class.isAssignableFrom(returnType)) {
                Class<? extends Annotation> nestedAnnotationType = (Class<? extends Annotation>) returnType;
                if (isSynthesizable(nestedAnnotationType)) {
                    synthesizable = Boolean.TRUE;
                    break;
                }
            }
        }

        synthesizableCache.put(annotationType, synthesizable);
        return synthesizable;
    }

    static List<String> getAttributeAliasNames(Method attribute) {
        Assert.notNull(attribute, "attribute must not be null");
        AliasDescriptor descriptor = AliasDescriptor.from(attribute);
        return (descriptor != null ? descriptor.getAttributeAliasNames() : Collections.<String> emptyList());
    }

    static String getAttributeOverrideName(Method attribute, Class<? extends Annotation> metaAnnotationType) {
        Assert.notNull(attribute, "attribute must not be null");
        Assert.notNull(metaAnnotationType, "metaAnnotationType must not be null");
        Assert.isTrue(Annotation.class != metaAnnotationType,
                "metaAnnotationType must not be [java.lang.annotation.Annotation]");

        AliasDescriptor descriptor = AliasDescriptor.from(attribute);
        return (descriptor != null ? descriptor.getAttributeOverrideName(metaAnnotationType) : null);
    }

    static List<Method> getAttributeMethods(Class<? extends Annotation> annotationType) {
        List<Method> methods = attributeMethodsCache.get(annotationType);
        if (methods != null) {
            return methods;
        }

        methods = new ArrayList<Method>();
        for (Method method : annotationType.getDeclaredMethods()) {
            if (isAttributeMethod(method)) {
                ReflectionUtils.makeAccessible(method);
                methods.add(method);
            }
        }

        attributeMethodsCache.put(annotationType, methods);
        return methods;
    }
    static Annotation getAnnotation(AnnotatedElement element, String annotationName) {
        for (Annotation annotation : element.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationName)) {
                return annotation;
            }
        }
        return null;
    }

    static boolean isAttributeMethod(Method method) {
        return (method != null && method.getParameterTypes().length == 0 && method.getReturnType() != void.class);
    }

    static boolean isAnnotationTypeMethod(Method method) {
        return (method != null && method.getName().equals("annotationType") && method.getParameterTypes().length == 0);
    }

    static Class<? extends Annotation> resolveContainerAnnotationType(Class<? extends Annotation> annotationType) {
        try {
            Annotation repeatable = getAnnotation(annotationType, REPEATABLE_CLASS_NAME);
            if (repeatable != null) {
                Object value = getValue(repeatable);
                return (Class<? extends Annotation>) value;
            }
        }
        catch (Exception ex) {
            handleIntrospectionFailure(annotationType, ex);
        }
        return null;
    }

    static void rethrowAnnotationConfigurationException(Throwable ex) {
        if (ex instanceof AnnotationConfigurationException) {
            throw (AnnotationConfigurationException) ex;
        }
    }

    static void handleIntrospectionFailure(AnnotatedElement element, Throwable ex) {
        rethrowAnnotationConfigurationException(ex);

        Log loggerToUse = logger;
        if (loggerToUse == null) {
            loggerToUse = LogFactory.getLog(AnnotationUtils.class);
            logger = loggerToUse;
        }
        if (element instanceof Class && Annotation.class.isAssignableFrom((Class<?>) element)) {
            // Meta-annotation or (default) value lookup on an annotation type
            if (loggerToUse.isDebugEnabled()) {
                loggerToUse.debug("Failed to meta-introspect annotation " + element + ": " + ex);
            }
        }
        else {
            // Direct annotation lookup on regular Class, Method, Field
            if (loggerToUse.isInfoEnabled()) {
                loggerToUse.info("Failed to introspect annotations on " + element + ": " + ex);
            }
        }
    }

    public static void clearCache() {
        findAnnotationCache.clear();
        metaPresentCache.clear();
        annotatedInterfaceCache.clear();
        synthesizableCache.clear();
        attributeAliasesCache.clear();
        attributeMethodsCache.clear();
        aliasDescriptorCache.clear();
    }


    //=====================================================================================
    private static final class AnnotationCacheKey implements Comparable<AnnotationCacheKey> {

        private final AnnotatedElement element;

        private final Class<? extends Annotation> annotationType;

        public AnnotationCacheKey(AnnotatedElement element, Class<? extends Annotation> annotationType) {
            this.element = element;
            this.annotationType = annotationType;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof AnnotationCacheKey)) {
                return false;
            }
            AnnotationCacheKey otherKey = (AnnotationCacheKey) other;
            return (this.element.equals(otherKey.element) && this.annotationType.equals(otherKey.annotationType));
        }

        @Override
        public int hashCode() {
            return (this.element.hashCode() * 29 + this.annotationType.hashCode());
        }

        @Override
        public String toString() {
            return "@" + this.annotationType + " on " + this.element;
        }

        @Override
        public int compareTo(AnnotationCacheKey other) {
            int result = this.element.toString().compareTo(other.element.toString());
            if (result == 0) {
                result = this.annotationType.getName().compareTo(other.annotationType.getName());
            }
            return result;
        }
    }

    private static class AnnotationCollector<A extends Annotation> {

        private final Class<A> annotationType;

        private final Class<? extends Annotation> containerAnnotationType;

        private final boolean declaredMode;

        private final Set<AnnotatedElement> visited = new HashSet<AnnotatedElement>();

        private final Set<A> result = new LinkedHashSet<A>();

        AnnotationCollector(Class<A> annotationType, Class<? extends Annotation> containerAnnotationType, boolean declaredMode) {
            this.annotationType = annotationType;
            this.containerAnnotationType = (containerAnnotationType != null ? containerAnnotationType :
                    resolveContainerAnnotationType(annotationType));
            this.declaredMode = declaredMode;
        }

        Set<A> getResult(AnnotatedElement element) {
            process(element);
            return Collections.unmodifiableSet(this.result);
        }

        @SuppressWarnings("unchecked")
        private void process(AnnotatedElement element) {
            if (this.visited.add(element)) {
                try {
                    Annotation[] annotations = (this.declaredMode ? element.getDeclaredAnnotations() : element.getAnnotations());
                    for (Annotation ann : annotations) {
                        Class<? extends Annotation> currentAnnotationType = ann.annotationType();
                        if (ObjectUtils.nullSafeEquals(this.annotationType, currentAnnotationType)) {
                            this.result.add(synthesizeAnnotation((A) ann, element));
                        }
                        else if (ObjectUtils.nullSafeEquals(this.containerAnnotationType, currentAnnotationType)) {
                            this.result.addAll(getValue(element, ann));
                        }
                        else if (!isInJavaLangAnnotationPackage(currentAnnotationType)) {
                            process(currentAnnotationType);
                        }
                    }
                }
                catch (Throwable ex) {
                    handleIntrospectionFailure(element, ex);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private List<A> getValue(AnnotatedElement element, Annotation annotation) {
            try {
                List<A> synthesizedAnnotations = new ArrayList<A>();
                for (A anno : (A[]) AnnotationUtils.getValue(annotation)) {
                    synthesizedAnnotations.add(synthesizeAnnotation(anno, element));
                }
                return synthesizedAnnotations;
            }
            catch (Throwable ex) {
                handleIntrospectionFailure(element, ex);
            }
            // Unable to read value from repeating annotation container -> ignore it.
            return Collections.emptyList();
        }
    }

    private static class AliasDescriptor {

        private final Method sourceAttribute;

        private final Class<? extends Annotation> sourceAnnotationType;

        private final String sourceAttributeName;

        private final Method aliasedAttribute;

        private final Class<? extends Annotation> aliasedAnnotationType;

        private final String aliasedAttributeName;

        private final boolean isAliasPair;

        /**
         * Create an {@code AliasDescriptor} <em>from</em> the declaration
         * of {@code @AliasFor} on the supplied annotation attribute and
         * validate the configuration of {@code @AliasFor}.
         * @param attribute the annotation attribute that is annotated with
         * {@code @AliasFor}
         * @return an alias descriptor, or {@code null} if the attribute
         * is not annotated with {@code @AliasFor}
         * @see #validateAgainst
         */
        public static AliasDescriptor from(Method attribute) {
            AliasDescriptor descriptor = aliasDescriptorCache.get(attribute);
            if (descriptor != null) {
                return descriptor;
            }

            AliasFor aliasFor = attribute.getAnnotation(AliasFor.class);
            if (aliasFor == null) {
                return null;
            }

            descriptor = new AliasDescriptor(attribute, aliasFor);
            descriptor.validate();
            aliasDescriptorCache.put(attribute, descriptor);
            return descriptor;
        }

        @SuppressWarnings("unchecked")
        private AliasDescriptor(Method sourceAttribute, AliasFor aliasFor) {
            Class<?> declaringClass = sourceAttribute.getDeclaringClass();
            Assert.isTrue(declaringClass.isAnnotation(), "sourceAttribute must be from an annotation");

            this.sourceAttribute = sourceAttribute;
            this.sourceAnnotationType = (Class<? extends Annotation>) declaringClass;
            this.sourceAttributeName = sourceAttribute.getName();

            this.aliasedAnnotationType = (Annotation.class == aliasFor.annotation() ?
                    this.sourceAnnotationType : aliasFor.annotation());
            this.aliasedAttributeName = getAliasedAttributeName(aliasFor, sourceAttribute);
            if (this.aliasedAnnotationType == this.sourceAnnotationType &&
                    this.aliasedAttributeName.equals(this.sourceAttributeName)) {
                String msg = String.format("@AliasFor declaration on attribute '%s' in annotation [%s] points to " +
                                "itself. Specify 'annotation' to point to a same-named attribute on a meta-annotation.",
                        sourceAttribute.getName(), declaringClass.getName());
                throw new AnnotationConfigurationException(msg);
            }
            try {
                this.aliasedAttribute = this.aliasedAnnotationType.getDeclaredMethod(this.aliasedAttributeName);
            }
            catch (NoSuchMethodException ex) {
                String msg = String.format(
                        "Attribute '%s' in annotation [%s] is declared as an @AliasFor nonexistent attribute '%s' in annotation [%s].",
                        this.sourceAttributeName, this.sourceAnnotationType.getName(), this.aliasedAttributeName,
                        this.aliasedAnnotationType.getName());
                throw new AnnotationConfigurationException(msg, ex);
            }

            this.isAliasPair = (this.sourceAnnotationType == this.aliasedAnnotationType);
        }

        private void validate() {
            // Target annotation is not meta-present?
            if (!this.isAliasPair && !isAnnotationMetaPresent(this.sourceAnnotationType, this.aliasedAnnotationType)) {
                String msg = String.format("@AliasFor declaration on attribute '%s' in annotation [%s] declares " +
                                "an alias for attribute '%s' in meta-annotation [%s] which is not meta-present.",
                        this.sourceAttributeName, this.sourceAnnotationType.getName(), this.aliasedAttributeName,
                        this.aliasedAnnotationType.getName());
                throw new AnnotationConfigurationException(msg);
            }

            if (this.isAliasPair) {
                AliasFor mirrorAliasFor = this.aliasedAttribute.getAnnotation(AliasFor.class);
                if (mirrorAliasFor == null) {
                    String msg = String.format("Attribute '%s' in annotation [%s] must be declared as an @AliasFor [%s].",
                            this.aliasedAttributeName, this.sourceAnnotationType.getName(), this.sourceAttributeName);
                    throw new AnnotationConfigurationException(msg);
                }

                String mirrorAliasedAttributeName = getAliasedAttributeName(mirrorAliasFor, this.aliasedAttribute);
                if (!this.sourceAttributeName.equals(mirrorAliasedAttributeName)) {
                    String msg = String.format("Attribute '%s' in annotation [%s] must be declared as an @AliasFor [%s], not [%s].",
                            this.aliasedAttributeName, this.sourceAnnotationType.getName(), this.sourceAttributeName,
                            mirrorAliasedAttributeName);
                    throw new AnnotationConfigurationException(msg);
                }
            }

            Class<?> returnType = this.sourceAttribute.getReturnType();
            Class<?> aliasedReturnType = this.aliasedAttribute.getReturnType();
            if (returnType != aliasedReturnType &&
                    (!aliasedReturnType.isArray() || returnType != aliasedReturnType.getComponentType())) {
                String msg = String.format("Misconfigured aliases: attribute '%s' in annotation [%s] " +
                                "and attribute '%s' in annotation [%s] must declare the same return type.",
                        this.sourceAttributeName, this.sourceAnnotationType.getName(), this.aliasedAttributeName,
                        this.aliasedAnnotationType.getName());
                throw new AnnotationConfigurationException(msg);
            }

            if (this.isAliasPair) {
                validateDefaultValueConfiguration(this.aliasedAttribute);
            }
        }

        private void validateDefaultValueConfiguration(Method aliasedAttribute) {
            Assert.notNull(aliasedAttribute, "aliasedAttribute must not be null");
            Object defaultValue = this.sourceAttribute.getDefaultValue();
            Object aliasedDefaultValue = aliasedAttribute.getDefaultValue();

            if (defaultValue == null || aliasedDefaultValue == null) {
                String msg = String.format("Misconfigured aliases: attribute '%s' in annotation [%s] " +
                                "and attribute '%s' in annotation [%s] must declare default values.",
                        this.sourceAttributeName, this.sourceAnnotationType.getName(), aliasedAttribute.getName(),
                        aliasedAttribute.getDeclaringClass().getName());
                throw new AnnotationConfigurationException(msg);
            }

            if (!ObjectUtils.nullSafeEquals(defaultValue, aliasedDefaultValue)) {
                String msg = String.format("Misconfigured aliases: attribute '%s' in annotation [%s] " +
                                "and attribute '%s' in annotation [%s] must declare the same default value.",
                        this.sourceAttributeName, this.sourceAnnotationType.getName(), aliasedAttribute.getName(),
                        aliasedAttribute.getDeclaringClass().getName());
                throw new AnnotationConfigurationException(msg);
            }
        }

        /**
         * Validate this descriptor against the supplied descriptor.
         * <p>This method only validates the configuration of default values
         * for the two descriptors, since other aspects of the descriptors
         * are validated when they are created.
         */
        private void validateAgainst(AliasDescriptor otherDescriptor) {
            validateDefaultValueConfiguration(otherDescriptor.sourceAttribute);
        }

        /**
         * Determine if this descriptor represents an explicit override for
         * an attribute in the supplied {@code metaAnnotationType}.
         * @see #isAliasFor
         */
        private boolean isOverrideFor(Class<? extends Annotation> metaAnnotationType) {
            return (this.aliasedAnnotationType == metaAnnotationType);
        }

        /**
         * Determine if this descriptor and the supplied descriptor both
         * effectively represent aliases for the same attribute in the same
         * target annotation, either explicitly or implicitly.
         * <p>This method searches the attribute override hierarchy, beginning
         * with this descriptor, in order to detect implicit and transitively
         * implicit aliases.
         * @return {@code true} if this descriptor and the supplied descriptor
         * effectively alias the same annotation attribute
         * @see #isOverrideFor
         */
        private boolean isAliasFor(AliasDescriptor otherDescriptor) {
            for (AliasDescriptor lhs = this; lhs != null; lhs = lhs.getAttributeOverrideDescriptor()) {
                for (AliasDescriptor rhs = otherDescriptor; rhs != null; rhs = rhs.getAttributeOverrideDescriptor()) {
                    if (lhs.aliasedAttribute.equals(rhs.aliasedAttribute)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public List<String> getAttributeAliasNames() {
            // Explicit alias pair?
            if (this.isAliasPair) {
                return Collections.singletonList(this.aliasedAttributeName);
            }

            // Else: search for implicit aliases
            List<String> aliases = new ArrayList<String>();
            for (AliasDescriptor otherDescriptor : getOtherDescriptors()) {
                if (this.isAliasFor(otherDescriptor)) {
                    this.validateAgainst(otherDescriptor);
                    aliases.add(otherDescriptor.sourceAttributeName);
                }
            }
            return aliases;
        }

        private List<AliasDescriptor> getOtherDescriptors() {
            List<AliasDescriptor> otherDescriptors = new ArrayList<AliasDescriptor>();
            for (Method currentAttribute : getAttributeMethods(this.sourceAnnotationType)) {
                if (!this.sourceAttribute.equals(currentAttribute)) {
                    AliasDescriptor otherDescriptor = AliasDescriptor.from(currentAttribute);
                    if (otherDescriptor != null) {
                        otherDescriptors.add(otherDescriptor);
                    }
                }
            }
            return otherDescriptors;
        }

        public String getAttributeOverrideName(Class<? extends Annotation> metaAnnotationType) {
            Assert.notNull(metaAnnotationType, "metaAnnotationType must not be null");
            Assert.isTrue(Annotation.class != metaAnnotationType,
                    "metaAnnotationType must not be [java.lang.annotation.Annotation]");

            // Search the attribute override hierarchy, starting with the current attribute
            for (AliasDescriptor desc = this; desc != null; desc = desc.getAttributeOverrideDescriptor()) {
                if (desc.isOverrideFor(metaAnnotationType)) {
                    return desc.aliasedAttributeName;
                }
            }

            // Else: explicit attribute override for a different meta-annotation
            return null;
        }

        private AliasDescriptor getAttributeOverrideDescriptor() {
            if (this.isAliasPair) {
                return null;
            }
            return AliasDescriptor.from(this.aliasedAttribute);
        }


        private String getAliasedAttributeName(AliasFor aliasFor, Method attribute) {
            String attributeName = aliasFor.attribute();
            String value = aliasFor.value();
            boolean attributeDeclared = StringUtils.hasText(attributeName);
            boolean valueDeclared = StringUtils.hasText(value);

            // Ensure user did not declare both 'value' and 'attribute' in @AliasFor
            if (attributeDeclared && valueDeclared) {
                String msg = String.format("In @AliasFor declared on attribute '%s' in annotation [%s], attribute 'attribute' " +
                                "and its alias 'value' are present with values of [%s] and [%s], but only one is permitted.",
                        attribute.getName(), attribute.getDeclaringClass().getName(), attributeName, value);
                throw new AnnotationConfigurationException(msg);
            }

            // Either explicit attribute name or pointing to same-named attribute by default
            attributeName = (attributeDeclared ? attributeName : value);
            return (StringUtils.hasText(attributeName) ? attributeName.trim() : attribute.getName());
        }

        @Override
        public String toString() {
            return String.format("%s: @%s(%s) is an alias for @%s(%s)", getClass().getSimpleName(),
                    this.sourceAnnotationType.getSimpleName(), this.sourceAttributeName,
                    this.aliasedAnnotationType.getSimpleName(), this.aliasedAttributeName);
        }
    }

    private static class DefaultValueHolder {

        final Object defaultValue;

        public DefaultValueHolder(Object defaultValue) {
            this.defaultValue = defaultValue;
        }
    }
}
