package org.myspring.expression.spel.ast;

import org.myspring.core.convert.TypeDescriptor;
import org.myspring.core.util.ClassUtils;

import java.util.List;

public class FormatHelper {
    public static String formatMethodForMessage(String name, List<TypeDescriptor> argumentTypes) {
        StringBuilder sb = new StringBuilder(name);
        sb.append("(");
        for (int i = 0; i < argumentTypes.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            TypeDescriptor typeDescriptor = argumentTypes.get(i);
            if (typeDescriptor != null) {
                sb.append(formatClassNameForMessage(typeDescriptor.getType()));
            }
            else {
                sb.append(formatClassNameForMessage(null));
            }
        }
        sb.append(")");
        return sb.toString();
    }
    public static String formatClassNameForMessage(Class<?> clazz) {
        return (clazz != null ? ClassUtils.getQualifiedName(clazz) : "null");
    }
}
