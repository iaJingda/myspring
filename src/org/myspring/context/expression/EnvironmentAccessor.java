package org.myspring.context.expression;

import org.myspring.core.env.Environment;
import org.myspring.expression.AccessException;
import org.myspring.expression.EvaluationContext;
import org.myspring.expression.PropertyAccessor;
import org.myspring.expression.TypedValue;

public class EnvironmentAccessor implements PropertyAccessor {
    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class<?>[] {Environment.class};
    }


    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
        return true;
    }

    /**
     * Access the given target object by resolving the given property name against the given target
     * environment.
     */
    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
        return new TypedValue(((Environment) target).getProperty(name));
    }

    /**
     * Read-only: returns {@code false}.
     */
    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
        return false;
    }

    /**
     * Read-only: no-op.
     */
    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
    }

}
