package org.myspring.expression.spel.ast;

import org.myspring.expression.EvaluationException;
import org.myspring.expression.TypedValue;
import org.myspring.expression.spel.ExpressionState;
import org.myspring.expression.spel.SpelNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class InlineMap extends SpelNodeImpl {

    private TypedValue constant = null;


    public InlineMap(int pos, SpelNodeImpl... args) {
        super(pos, args);
        checkIfConstant();
    }

    private void checkIfConstant() {
        boolean isConstant = true;
        for (int c = 0, max = getChildCount(); c < max; c++) {
            SpelNode child = getChild(c);
            if (!(child instanceof Literal)) {
                if (child instanceof InlineList) {
                    InlineList inlineList = (InlineList) child;
                    if (!inlineList.isConstant()) {
                        isConstant = false;
                        break;
                    }
                }
                else if (child instanceof InlineMap) {
                    InlineMap inlineMap = (InlineMap) child;
                    if (!inlineMap.isConstant()) {
                        isConstant = false;
                        break;
                    }
                }
                else if (!((c%2)==0 && (child instanceof PropertyOrFieldReference))) {
                    isConstant = false;
                    break;
                }
            }
        }
        if (isConstant) {
            Map<Object,Object> constantMap = new LinkedHashMap<Object,Object>();
            int childCount = getChildCount();
            for (int c = 0; c < childCount; c++) {
                SpelNode keyChild = getChild(c++);
                SpelNode valueChild = getChild(c);
                Object key = null;
                Object value = null;
                if (keyChild instanceof Literal) {
                    key = ((Literal) keyChild).getLiteralValue().getValue();
                }
                else if (keyChild instanceof PropertyOrFieldReference) {
                    key = ((PropertyOrFieldReference) keyChild).getName();
                }
                else {
                    return;
                }
                if (valueChild instanceof Literal) {
                    value = ((Literal) valueChild).getLiteralValue().getValue();
                }
                else if (valueChild instanceof InlineList) {
                    value = ((InlineList) valueChild).getConstantValue();
                }
                else if (valueChild instanceof InlineMap) {
                    value = ((InlineMap) valueChild).getConstantValue();
                }
                constantMap.put(key, value);
            }
            this.constant = new TypedValue(Collections.unmodifiableMap(constantMap));
        }
    }

    @Override
    public TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException {
        if (this.constant != null) {
            return this.constant;
        }
        else {
            Map<Object, Object> returnValue = new LinkedHashMap<Object, Object>();
            int childcount = getChildCount();
            for (int c = 0; c < childcount; c++) {
                // TODO allow for key being PropertyOrFieldReference like Indexer on maps
                SpelNode keyChild = getChild(c++);
                Object key = null;
                if (keyChild instanceof PropertyOrFieldReference) {
                    PropertyOrFieldReference reference = (PropertyOrFieldReference) keyChild;
                    key = reference.getName();
                }
                else {
                    key = keyChild.getValue(expressionState);
                }
                Object value = getChild(c).getValue(expressionState);
                returnValue.put(key,  value);
            }
            return new TypedValue(returnValue);
        }
    }

    @Override
    public String toStringAST() {
        StringBuilder sb = new StringBuilder("{");
        int count = getChildCount();
        for (int c = 0; c < count; c++) {
            if (c > 0) {
                sb.append(",");
            }
            sb.append(getChild(c++).toStringAST());
            sb.append(":");
            sb.append(getChild(c).toStringAST());
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * @return whether this list is a constant value
     */
    public boolean isConstant() {
        return this.constant != null;
    }

    @SuppressWarnings("unchecked")
    public Map<Object,Object> getConstantValue() {
        return (Map<Object,Object>) this.constant.getValue();
    }

}
