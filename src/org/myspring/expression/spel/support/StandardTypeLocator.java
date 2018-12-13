package org.myspring.expression.spel.support;

import org.myspring.core.util.ClassUtils;
import org.myspring.expression.EvaluationException;
import org.myspring.expression.TypeLocator;
import org.myspring.expression.spel.SpelEvaluationException;
import org.myspring.expression.spel.SpelMessage;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class StandardTypeLocator  implements TypeLocator {

    private final ClassLoader classLoader;

    private final List<String> knownPackagePrefixes = new LinkedList<String>();

    public StandardTypeLocator() {
        this(ClassUtils.getDefaultClassLoader());
    }
    public StandardTypeLocator(ClassLoader classLoader) {
        this.classLoader = classLoader;
        // Similar to when writing regular Java code, it only knows about java.lang by default
        registerImport("java.lang");
    }
    public void registerImport(String prefix) {
        this.knownPackagePrefixes.add(prefix);
    }

    public void removeImport(String prefix) {
        this.knownPackagePrefixes.remove(prefix);
    }

    public List<String> getImportPrefixes() {
        return Collections.unmodifiableList(this.knownPackagePrefixes);
    }

    @Override
    public Class<?> findType(String typeName) throws EvaluationException {
        String nameToLookup = typeName;
        try {
            return ClassUtils.forName(nameToLookup, this.classLoader);
        }
        catch (ClassNotFoundException ey) {
            // try any registered prefixes before giving up
        }
        for (String prefix : this.knownPackagePrefixes) {
            try {
                nameToLookup = prefix + '.' + typeName;
                return ClassUtils.forName(nameToLookup, this.classLoader);
            }
            catch (ClassNotFoundException ex) {
                // might be a different prefix
            }
        }
        throw new SpelEvaluationException(SpelMessage.TYPE_NOT_FOUND, typeName);
    }
}
