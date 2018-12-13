package org.myspring.expression;

import java.lang.reflect.Method;
import java.util.List;

public interface MethodFilter {

    List<Method> filter(List<Method> methods);

}
