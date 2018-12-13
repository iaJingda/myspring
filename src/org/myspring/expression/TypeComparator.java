package org.myspring.expression;

public interface TypeComparator {

    boolean canCompare(Object firstObject, Object secondObject);


    int compare(Object firstObject, Object secondObject) throws EvaluationException;

}
