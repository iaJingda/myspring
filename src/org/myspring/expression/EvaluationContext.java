package org.myspring.expression;

import java.util.List;

public interface EvaluationContext {

    TypedValue getRootObject();

    List<PropertyAccessor> getPropertyAccessors();

    List<ConstructorResolver> getConstructorResolvers();

    List<MethodResolver> getMethodResolvers();

    BeanResolver getBeanResolver();

    TypeLocator getTypeLocator();

    TypeConverter getTypeConverter();

    TypeComparator getTypeComparator();

    OperatorOverloader getOperatorOverloader();

    void setVariable(String name, Object value);

    Object lookupVariable(String name);

}
