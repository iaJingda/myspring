package org.myspring.expression.spel.ast;

import org.myspring.expression.TypedValue;
import org.myspring.expression.spel.*;

public abstract class Literal extends SpelNodeImpl {

    private final String originalValue;


    public Literal(String originalValue, int pos) {
        super(pos);
        this.originalValue = originalValue;
    }

    public final String getOriginalValue() {
        return this.originalValue;
    }

    @Override
    public final TypedValue getValueInternal(ExpressionState state) throws SpelEvaluationException {
        return getLiteralValue();
    }

    @Override
    public String toString() {
        return getLiteralValue().getValue().toString();
    }

    @Override
    public String toStringAST() {
        return toString();
    }


    public abstract TypedValue getLiteralValue();

    public static Literal getIntLiteral(String numberToken, int pos, int radix) {
        try {
            int value = Integer.parseInt(numberToken, radix);
            return new IntLiteral(numberToken, pos, value);
        }
        catch (NumberFormatException ex) {
            throw new InternalParseException(new SpelParseException(pos>>16, ex, SpelMessage.NOT_AN_INTEGER, numberToken));
        }
    }

    public static Literal getLongLiteral(String numberToken, int pos, int radix) {
        try {
            long value = Long.parseLong(numberToken, radix);
            return new LongLiteral(numberToken, pos, value);
        }
        catch (NumberFormatException ex) {
            throw new InternalParseException(new SpelParseException(pos>>16, ex, SpelMessage.NOT_A_LONG, numberToken));
        }
    }

    public static Literal getRealLiteral(String numberToken, int pos, boolean isFloat) {
        try {
            if (isFloat) {
                float value = Float.parseFloat(numberToken);
                return new FloatLiteral(numberToken, pos, value);
            }
            else {
                double value = Double.parseDouble(numberToken);
                return new RealLiteral(numberToken, pos, value);
            }
        }
        catch (NumberFormatException ex) {
            throw new InternalParseException(new SpelParseException(pos>>16, ex, SpelMessage.NOT_A_REAL, numberToken));
        }
    }
}
