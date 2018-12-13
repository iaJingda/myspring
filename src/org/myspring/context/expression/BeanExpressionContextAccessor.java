package org.myspring.context.expression;

import org.myspring.beans.factory.config.BeanExpressionContext;
import org.myspring.expression.AccessException;
import org.myspring.expression.EvaluationContext;
import org.myspring.expression.PropertyAccessor;
import org.myspring.expression.TypedValue;

public class BeanExpressionContextAccessor implements PropertyAccessor {

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
        return ((BeanExpressionContext) target).containsObject(name);
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
        return new TypedValue(((BeanExpressionContext) target).getObject(name));
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
        return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
        throw new AccessException("Beans in a BeanFactory are read-only");
    }

    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class<?>[] {BeanExpressionContext.class};
    }
}
