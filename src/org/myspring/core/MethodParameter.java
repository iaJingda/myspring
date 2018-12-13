package org.myspring.core;

import org.myspring.core.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;

public class MethodParameter {
    private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

    private final int parameterIndex;
    private final Method method;
    private int nestingLevel = 1;
    Map<Integer, Integer> typeIndexesPerLevel;

    private volatile Class<?> containingClass;

    private volatile Class<?> parameterType;

    private volatile Type genericParameterType;

    private volatile Annotation[] parameterAnnotations;

    private volatile ParameterNameDiscoverer parameterNameDiscoverer;

    private volatile String parameterName;

    private volatile MethodParameter nestedMethodParameter;

    private final Constructor<?> constructor;

    public int getNestingLevel() {
        return this.nestingLevel;
    }


    public MethodParameter(Method method, int parameterIndex) {
        this(method, parameterIndex, 1);
    }

    public MethodParameter(Method method, int parameterIndex, int nestingLevel) {
        Assert.notNull(method, "Method must not be null");
        this.method = method;
        this.parameterIndex = parameterIndex;
        this.nestingLevel = nestingLevel;
        this.constructor = null;
    }
    public MethodParameter(MethodParameter original) {
        Assert.notNull(original, "Original must not be null");
        this.method = original.method;
        this.constructor = original.constructor;
        this.parameterIndex = original.parameterIndex;
        this.nestingLevel = original.nestingLevel;
        this.typeIndexesPerLevel = original.typeIndexesPerLevel;
        this.containingClass = original.containingClass;
        this.parameterType = original.parameterType;
        this.genericParameterType = original.genericParameterType;
        this.parameterAnnotations = original.parameterAnnotations;
        this.parameterNameDiscoverer = original.parameterNameDiscoverer;
        this.parameterName = original.parameterName;
    }

    public MethodParameter(Constructor<?> constructor, int parameterIndex) {
        this(constructor, parameterIndex, 1);
    }

    public MethodParameter(Constructor<?> constructor, int parameterIndex, int nestingLevel) {
        Assert.notNull(constructor, "Constructor must not be null");
        this.constructor = constructor;
        this.parameterIndex = parameterIndex;
        this.nestingLevel = nestingLevel;
        this.method = null;
    }

    public <A extends Annotation> A getParameterAnnotation(Class<A> annotationType) {
        Annotation[] anns = getParameterAnnotations();
        for (Annotation ann : anns) {
            if (annotationType.isInstance(ann)) {
                return (A) ann;
            }
        }
        return null;
    }


    public Class<?> getNestedParameterType() {
        if (this.nestingLevel > 1) {
            Type type = getGenericParameterType();
            for (int i = 2; i <= this.nestingLevel; i++) {
                if (type instanceof ParameterizedType) {
                    Type[] args = ((ParameterizedType) type).getActualTypeArguments();
                    Integer index = getTypeIndexForLevel(i);
                    type = args[index != null ? index : args.length - 1];
                }
                // TODO: Object.class if unresolvable
            }
            if (type instanceof Class) {
                return (Class<?>) type;
            }
            else if (type instanceof ParameterizedType) {
                Type arg = ((ParameterizedType) type).getRawType();
                if (arg instanceof Class) {
                    return (Class<?>) arg;
                }
            }
            return Object.class;
        }
        else {
            return getParameterType();
        }
    }
    public Integer getTypeIndexForLevel(int nestingLevel) {
        return getTypeIndexesPerLevel().get(nestingLevel);
    }

    private Map<Integer, Integer> getTypeIndexesPerLevel() {
        if (this.typeIndexesPerLevel == null) {
            this.typeIndexesPerLevel = new HashMap<Integer, Integer>(4);
        }
        return this.typeIndexesPerLevel;
    }

    public Annotation[] getMethodAnnotations() {
        return adaptAnnotationArray(getAnnotatedElement().getAnnotations());
    }
    protected Annotation[] adaptAnnotationArray(Annotation[] annotations) {
        return annotations;
    }

    public AnnotatedElement getAnnotatedElement() {
        // NOTE: no ternary expression to retain JDK <8 compatibility even when using
        // the JDK 8 compiler (potentially selecting java.lang.reflect.Executable
        // as common type, with that new base class not available on older JDKs)
        if (this.method != null) {
            return this.method;
        }
        else {
            return this.constructor;
        }
    }

    public Annotation[] getParameterAnnotations() {
        Annotation[] paramAnns = this.parameterAnnotations;
        if (paramAnns == null) {
            Annotation[][] annotationArray = (this.method != null ?
                    this.method.getParameterAnnotations() : this.constructor.getParameterAnnotations());
            int index = this.parameterIndex;
            if (this.constructor != null && this.constructor.getDeclaringClass().isMemberClass() &&
                    !Modifier.isStatic(this.constructor.getDeclaringClass().getModifiers()) &&
                    annotationArray.length == this.constructor.getParameterTypes().length - 1) {
                // Bug in javac in JDK <9: annotation array excludes enclosing instance parameter
                // for inner classes, so access it with the actual parameter index lowered by 1
                index = this.parameterIndex - 1;
            }
            paramAnns = (index >= 0 && index < annotationArray.length ?
                    adaptAnnotationArray(annotationArray[index]) : EMPTY_ANNOTATION_ARRAY);
            this.parameterAnnotations = paramAnns;
        }
        return paramAnns;
    }
    public static MethodParameter forMethodOrConstructor(Object methodOrConstructor, int parameterIndex) {
        if (methodOrConstructor instanceof Method) {
            return new MethodParameter((Method) methodOrConstructor, parameterIndex);
        }
        else if (methodOrConstructor instanceof Constructor) {
            return new MethodParameter((Constructor<?>) methodOrConstructor, parameterIndex);
        }
        else {
            throw new IllegalArgumentException(
                    "Given object [" + methodOrConstructor + "] is neither a Method nor a Constructor");
        }
    }


    public Class<?> getParameterType() {
        Class<?> paramType = this.parameterType;
        if (paramType == null) {
            if (this.parameterIndex < 0) {
                Method method = getMethod();
                paramType = (method != null ? method.getReturnType() : void.class);
            }
            else {
                paramType = (this.method != null ?
                        this.method.getParameterTypes()[this.parameterIndex] :
                        this.constructor.getParameterTypes()[this.parameterIndex]);
            }
            this.parameterType = paramType;
        }
        return paramType;
    }

    public Method getMethod() {
        return this.method;
    }

    void setContainingClass(Class<?> containingClass) {
        this.containingClass = containingClass;
    }

    public Class<?> getContainingClass() {
        return (this.containingClass != null ? this.containingClass : getDeclaringClass());
    }

    public Class<?> getDeclaringClass() {
        return getMember().getDeclaringClass();
    }
    public Constructor<?> getConstructor() {
        return this.constructor;
    }
    public int getParameterIndex() {
        return this.parameterIndex;
    }

    void setParameterType(Class<?> parameterType) {
        this.parameterType = parameterType;
    }


    public Member getMember() {
        // NOTE: no ternary expression to retain JDK <8 compatibility even when using
        // the JDK 8 compiler (potentially selecting java.lang.reflect.Executable
        // as common type, with that new base class not available on older JDKs)
        if (this.method != null) {
            return this.method;
        }
        else {
            return this.constructor;
        }
    }
    public Type getGenericParameterType() {
        Type paramType = this.genericParameterType;
        if (paramType == null) {
            if (this.parameterIndex < 0) {
                Method method = getMethod();
                paramType = (method != null ? method.getGenericReturnType() : void.class);
            }
            else {
                Type[] genericParameterTypes = (this.method != null ?
                        this.method.getGenericParameterTypes() : this.constructor.getGenericParameterTypes());
                int index = this.parameterIndex;
                if (this.constructor != null && this.constructor.getDeclaringClass().isMemberClass() &&
                        !Modifier.isStatic(this.constructor.getDeclaringClass().getModifiers()) &&
                        genericParameterTypes.length == this.constructor.getParameterTypes().length - 1) {
                    // Bug in javac: type array excludes enclosing instance parameter
                    // for inner classes with at least one generic constructor parameter,
                    // so access it with the actual parameter index lowered by 1
                    index = this.parameterIndex - 1;
                }
                paramType = (index >= 0 && index < genericParameterTypes.length ?
                        genericParameterTypes[index] : getParameterType());
            }
            this.genericParameterType = paramType;
        }
        return paramType;
    }

    public void increaseNestingLevel() {
        this.nestingLevel++;
    }

    public String getParameterName() {
        ParameterNameDiscoverer discoverer = this.parameterNameDiscoverer;
        if (discoverer != null) {
            String[] parameterNames = (this.method != null ?
                    discoverer.getParameterNames(this.method) : discoverer.getParameterNames(this.constructor));
            if (parameterNames != null) {
                this.parameterName = parameterNames[this.parameterIndex];
            }
            this.parameterNameDiscoverer = null;
        }
        return this.parameterName;
    }
    public void initParameterNameDiscovery(ParameterNameDiscoverer parameterNameDiscoverer) {
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }
}
