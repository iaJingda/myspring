package org.myspring.expression.spel.support;

import org.myspring.core.util.ReflectionUtils;
import org.myspring.expression.AccessException;
import org.myspring.expression.ConstructorExecutor;
import org.myspring.expression.EvaluationContext;
import org.myspring.expression.TypedValue;

import java.lang.reflect.Constructor;

public class ReflectiveConstructorExecutor implements ConstructorExecutor {

    private final Constructor<?> ctor;

    private final Integer varargsPosition;


    public ReflectiveConstructorExecutor(Constructor<?> ctor) {
        this.ctor = ctor;
        if (ctor.isVarArgs()) {
            Class<?>[] paramTypes = ctor.getParameterTypes();
            this.varargsPosition = paramTypes.length - 1;
        }
        else {
            this.varargsPosition = null;
        }
    }

    @Override
    public TypedValue execute(EvaluationContext context, Object... arguments) throws AccessException {
        try {
            if (arguments != null) {
                ReflectionHelper.convertArguments(context.getTypeConverter(), arguments, this.ctor, this.varargsPosition);
            }
            if (this.ctor.isVarArgs()) {
                arguments = ReflectionHelper.setupArgumentsForVarargsInvocation(this.ctor.getParameterTypes(), arguments);
            }
            ReflectionUtils.makeAccessible(this.ctor);
            return new TypedValue(this.ctor.newInstance(arguments));
        }
        catch (Exception ex) {
            throw new AccessException("Problem invoking constructor: " + this.ctor, ex);
        }
    }

    public Constructor<?> getConstructor() {
        return this.ctor;
    }
}
