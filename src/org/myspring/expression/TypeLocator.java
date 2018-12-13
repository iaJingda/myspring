package org.myspring.expression;

public interface TypeLocator {

    Class<?> findType(String typeName) throws EvaluationException;

}
