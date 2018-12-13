package org.myspring.expression.spel.ast;

import org.myspring.expression.PropertyAccessor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class AstUtils {

    public static List<PropertyAccessor> getPropertyAccessorsToTry(
            Class<?> targetType, List<PropertyAccessor> propertyAccessors) {

        List<PropertyAccessor> specificAccessors = new ArrayList<PropertyAccessor>();
        List<PropertyAccessor> generalAccessors = new ArrayList<PropertyAccessor>();
        for (PropertyAccessor resolver : propertyAccessors) {
            Class<?>[] targets = resolver.getSpecificTargetClasses();
            if (targets == null) {  // generic resolver that says it can be used for any type
                generalAccessors.add(resolver);
            }
            else {
                if (targetType != null) {
                    int pos = 0;
                    for (Class<?> clazz : targets) {
                        if (clazz == targetType) {  // put exact matches on the front to be tried first?
                            specificAccessors.add(pos++, resolver);
                        }
                        else if (clazz.isAssignableFrom(targetType)) {  // put supertype matches at the end of the
                            // specificAccessor list
                            generalAccessors.add(resolver);
                        }
                    }
                }
            }
        }
        List<PropertyAccessor> resolvers = new LinkedList<PropertyAccessor>();
        resolvers.addAll(specificAccessors);
        resolvers.addAll(generalAccessors);
        return resolvers;
    }

}
