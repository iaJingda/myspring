package org.myspring.expression.spel.support;

import org.myspring.core.convert.TypeDescriptor;
import org.myspring.core.util.Assert;
import org.myspring.expression.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StandardEvaluationContext implements EvaluationContext {

    private TypedValue rootObject;

    private List<ConstructorResolver> constructorResolvers;

    private List<MethodResolver> methodResolvers;

    private BeanResolver beanResolver;

    private ReflectiveMethodResolver reflectiveMethodResolver;

    private List<PropertyAccessor> propertyAccessors;

    private TypeLocator typeLocator;

    private TypeConverter typeConverter;

    private TypeComparator typeComparator = new StandardTypeComparator();

    private OperatorOverloader operatorOverloader = new StandardOperatorOverloader();

    private final Map<String, Object> variables = new HashMap<String, Object>();

    public StandardEvaluationContext() {
        setRootObject(null);
    }


    public StandardEvaluationContext(Object rootObject) {
        setRootObject(rootObject);
    }


    public void setRootObject(Object rootObject, TypeDescriptor typeDescriptor) {
        this.rootObject = new TypedValue(rootObject, typeDescriptor);
    }

    public void setRootObject(Object rootObject) {
        this.rootObject = (rootObject != null ? new TypedValue(rootObject) : TypedValue.NULL);
    }

    @Override
    public TypedValue getRootObject() {
        return this.rootObject;
    }

    public void setPropertyAccessors(List<PropertyAccessor> propertyAccessors) {
        this.propertyAccessors = propertyAccessors;
    }

    @Override
    public List<PropertyAccessor> getPropertyAccessors() {
        ensurePropertyAccessorsInitialized();
        return this.propertyAccessors;
    }

    public void addPropertyAccessor(PropertyAccessor accessor) {
        ensurePropertyAccessorsInitialized();
        this.propertyAccessors.add(this.propertyAccessors.size() - 1, accessor);
    }

    public boolean removePropertyAccessor(PropertyAccessor accessor) {
        return this.propertyAccessors.remove(accessor);
    }

    public void setConstructorResolvers(List<ConstructorResolver> constructorResolvers) {
        this.constructorResolvers = constructorResolvers;
    }

    @Override
    public List<ConstructorResolver> getConstructorResolvers() {
        ensureConstructorResolversInitialized();
        return this.constructorResolvers;
    }

    public void addConstructorResolver(ConstructorResolver resolver) {
        ensureConstructorResolversInitialized();
        this.constructorResolvers.add(this.constructorResolvers.size() - 1, resolver);
    }

    public boolean removeConstructorResolver(ConstructorResolver resolver) {
        ensureConstructorResolversInitialized();
        return this.constructorResolvers.remove(resolver);
    }
    public void setMethodResolvers(List<MethodResolver> methodResolvers) {
        this.methodResolvers = methodResolvers;
    }

    @Override
    public List<MethodResolver> getMethodResolvers() {
        ensureMethodResolversInitialized();
        return this.methodResolvers;
    }

    public void addMethodResolver(MethodResolver resolver) {
        ensureMethodResolversInitialized();
        this.methodResolvers.add(this.methodResolvers.size() - 1, resolver);
    }

    public boolean removeMethodResolver(MethodResolver methodResolver) {
        ensureMethodResolversInitialized();
        return this.methodResolvers.remove(methodResolver);
    }

    public void setBeanResolver(BeanResolver beanResolver) {
        this.beanResolver = beanResolver;
    }

    @Override
    public BeanResolver getBeanResolver() {
        return this.beanResolver;
    }

    public void setTypeLocator(TypeLocator typeLocator) {
        Assert.notNull(typeLocator, "TypeLocator must not be null");
        this.typeLocator = typeLocator;
    }

    @Override
    public TypeLocator getTypeLocator() {
        if (this.typeLocator == null) {
            this.typeLocator = new StandardTypeLocator();
        }
        return this.typeLocator;
    }


    public void setTypeConverter(TypeConverter typeConverter) {
        Assert.notNull(typeConverter, "TypeConverter must not be null");
        this.typeConverter = typeConverter;
    }

    @Override
    public TypeConverter getTypeConverter() {
        if (this.typeConverter == null) {
            this.typeConverter = new StandardTypeConverter();
        }
        return this.typeConverter;
    }

    public void setTypeComparator(TypeComparator typeComparator) {
        Assert.notNull(typeComparator, "TypeComparator must not be null");
        this.typeComparator = typeComparator;
    }

    @Override
    public TypeComparator getTypeComparator() {
        return this.typeComparator;
    }

    public void setOperatorOverloader(OperatorOverloader operatorOverloader) {
        Assert.notNull(operatorOverloader, "OperatorOverloader must not be null");
        this.operatorOverloader = operatorOverloader;
    }

    @Override
    public OperatorOverloader getOperatorOverloader() {
        return this.operatorOverloader;
    }

    @Override
    public void setVariable(String name, Object value) {
        this.variables.put(name, value);
    }

    public void setVariables(Map<String,Object> variables) {
        this.variables.putAll(variables);
    }

    public void registerFunction(String name, Method method) {
        this.variables.put(name, method);
    }

    @Override
    public Object lookupVariable(String name) {
        return this.variables.get(name);
    }

    public void registerMethodFilter(Class<?> type, MethodFilter filter) throws IllegalStateException {
        ensureMethodResolversInitialized();
        if (this.reflectiveMethodResolver != null) {
            this.reflectiveMethodResolver.registerMethodFilter(type, filter);
        }
        else {
            throw new IllegalStateException("Method filter cannot be set as the reflective method resolver is not in use");
        }
    }


    private void ensurePropertyAccessorsInitialized() {
        if (this.propertyAccessors == null) {
            initializePropertyAccessors();
        }
    }

    private synchronized void initializePropertyAccessors() {
        if (this.propertyAccessors == null) {
            List<PropertyAccessor> defaultAccessors = new ArrayList<PropertyAccessor>();
            defaultAccessors.add(new ReflectivePropertyAccessor());
            this.propertyAccessors = defaultAccessors;
        }
    }

    private void ensureConstructorResolversInitialized() {
        if (this.constructorResolvers == null) {
            initializeConstructorResolvers();
        }
    }

    private synchronized void initializeConstructorResolvers() {
        if (this.constructorResolvers == null) {
            List<ConstructorResolver> defaultResolvers = new ArrayList<ConstructorResolver>();
            defaultResolvers.add(new ReflectiveConstructorResolver());
            this.constructorResolvers = defaultResolvers;
        }
    }

    private void ensureMethodResolversInitialized() {
        if (this.methodResolvers == null) {
            initializeMethodResolvers();
        }
    }

    private synchronized void initializeMethodResolvers() {
        if (this.methodResolvers == null) {
            List<MethodResolver> defaultResolvers = new ArrayList<MethodResolver>();
            this.reflectiveMethodResolver = new ReflectiveMethodResolver();
            defaultResolvers.add(this.reflectiveMethodResolver);
            this.methodResolvers = defaultResolvers;
        }
    }
}
