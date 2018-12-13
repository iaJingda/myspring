package org.myspring.context.expression;

import org.myspring.beans.factory.BeanFactory;
import org.myspring.expression.AccessException;
import org.myspring.expression.EvaluationContext;
import org.myspring.expression.PropertyAccessor;
import org.myspring.expression.TypedValue;

public class BeanFactoryAccessor implements PropertyAccessor {

    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class<?>[] {BeanFactory.class};
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
        return (((BeanFactory) target).containsBean(name));
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
        return new TypedValue(((BeanFactory) target).getBean(name));
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
        return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
        throw new AccessException("Beans in a BeanFactory are read-only");
    }

}
