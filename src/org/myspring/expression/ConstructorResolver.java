package org.myspring.expression;

import org.myspring.core.convert.TypeDescriptor;

import java.util.List;

public interface ConstructorResolver {

    ConstructorExecutor resolve(EvaluationContext context, String typeName, List<TypeDescriptor> argumentTypes)
            throws AccessException;


}
