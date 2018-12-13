package org.myspring.expression.spel.ast;

import org.myspring.core.util.Assert;
import org.myspring.core.util.ClassUtils;
import org.myspring.core.util.ObjectUtils;
import org.myspring.expression.EvaluationException;
import org.myspring.expression.TypedValue;
import org.myspring.expression.spel.ExpressionState;
import org.myspring.expression.spel.SpelEvaluationException;
import org.myspring.expression.spel.SpelMessage;

import java.lang.reflect.Array;
import java.util.*;

public class Selection extends SpelNodeImpl {

    public static final int ALL = 0; // ?[]

    public static final int FIRST = 1; // ^[]

    public static final int LAST = 2; // $[]

    private final int variant;

    private final boolean nullSafe;


    public Selection(boolean nullSafe, int variant, int pos, SpelNodeImpl expression) {
        super(pos, expression);
        Assert.notNull(expression, "Expression must not be null");
        this.nullSafe = nullSafe;
        this.variant = variant;
    }


    @Override
    public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
        return getValueRef(state).getValue();
    }

    @Override
    protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
        TypedValue op = state.getActiveContextObject();
        Object operand = op.getValue();
        SpelNodeImpl selectionCriteria = this.children[0];

        if (operand instanceof Map) {
            Map<?, ?> mapdata = (Map<?, ?>) operand;
            // TODO don't lose generic info for the new map
            Map<Object, Object> result = new HashMap<Object, Object>();
            Object lastKey = null;

            for (Map.Entry<?, ?> entry : mapdata.entrySet()) {
                try {
                    TypedValue kvPair = new TypedValue(entry);
                    state.pushActiveContextObject(kvPair);
                    state.enterScope();
                    Object val = selectionCriteria.getValueInternal(state).getValue();
                    if (val instanceof Boolean) {
                        if ((Boolean) val) {
                            if (this.variant == FIRST) {
                                result.put(entry.getKey(), entry.getValue());
                                return new ValueRef.TypedValueHolderValueRef(new TypedValue(result), this);
                            }
                            result.put(entry.getKey(), entry.getValue());
                            lastKey = entry.getKey();
                        }
                    }
                    else {
                        throw new SpelEvaluationException(selectionCriteria.getStartPosition(),
                                SpelMessage.RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);
                    }
                }
                finally {
                    state.popActiveContextObject();
                    state.exitScope();
                }
            }

            if ((this.variant == FIRST || this.variant == LAST) && result.isEmpty()) {
                return new ValueRef.TypedValueHolderValueRef(new TypedValue(null), this);
            }

            if (this.variant == LAST) {
                Map<Object, Object> resultMap = new HashMap<Object, Object>();
                Object lastValue = result.get(lastKey);
                resultMap.put(lastKey,lastValue);
                return new ValueRef.TypedValueHolderValueRef(new TypedValue(resultMap),this);
            }

            return new ValueRef.TypedValueHolderValueRef(new TypedValue(result),this);
        }

        if (operand instanceof Iterable || ObjectUtils.isArray(operand)) {
            Iterable<?> data = (operand instanceof Iterable ?
                    (Iterable<?>) operand : Arrays.asList(ObjectUtils.toObjectArray(operand)));

            List<Object> result = new ArrayList<Object>();
            int index = 0;
            for (Object element : data) {
                try {
                    state.pushActiveContextObject(new TypedValue(element));
                    state.enterScope("index", index);
                    Object val = selectionCriteria.getValueInternal(state).getValue();
                    if (val instanceof Boolean) {
                        if ((Boolean) val) {
                            if (this.variant == FIRST) {
                                return new ValueRef.TypedValueHolderValueRef(new TypedValue(element), this);
                            }
                            result.add(element);
                        }
                    }
                    else {
                        throw new SpelEvaluationException(selectionCriteria.getStartPosition(),
                                SpelMessage.RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);
                    }
                    index++;
                }
                finally {
                    state.exitScope();
                    state.popActiveContextObject();
                }
            }

            if ((this.variant == FIRST || this.variant == LAST) && result.isEmpty()) {
                return ValueRef.NullValueRef.INSTANCE;
            }

            if (this.variant == LAST) {
                return new ValueRef.TypedValueHolderValueRef(new TypedValue(result.get(result.size() - 1)), this);
            }

            if (operand instanceof Iterable) {
                return new ValueRef.TypedValueHolderValueRef(new TypedValue(result), this);
            }

            Class<?> elementType = ClassUtils.resolvePrimitiveIfNecessary(
                    op.getTypeDescriptor().getElementTypeDescriptor().getType());
            Object resultArray = Array.newInstance(elementType, result.size());
            System.arraycopy(result.toArray(), 0, resultArray, 0, result.size());
            return new ValueRef.TypedValueHolderValueRef(new TypedValue(resultArray), this);
        }
        if (operand == null) {
            if (this.nullSafe) {
                return ValueRef.NullValueRef.INSTANCE;
            }
            throw new SpelEvaluationException(getStartPosition(), SpelMessage.INVALID_TYPE_FOR_SELECTION, "null");
        }
        throw new SpelEvaluationException(getStartPosition(), SpelMessage.INVALID_TYPE_FOR_SELECTION,
                operand.getClass().getName());
    }

    @Override
    public String toStringAST() {
        StringBuilder sb = new StringBuilder();
        switch (this.variant) {
            case ALL:
                sb.append("?[");
                break;
            case FIRST:
                sb.append("^[");
                break;
            case LAST:
                sb.append("$[");
                break;
        }
        return sb.append(getChild(0).toStringAST()).append("]").toString();
    }

}
