package org.myspring.expression;

import org.myspring.core.convert.TypeDescriptor;

import java.util.List;

public interface MethodResolver {

    MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
                           List<TypeDescriptor> argumentTypes) throws AccessException;
}
