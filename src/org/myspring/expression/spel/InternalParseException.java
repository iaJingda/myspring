package org.myspring.expression.spel;

public class InternalParseException extends RuntimeException  {

    public InternalParseException(SpelParseException cause) {
        super(cause);
    }

    @Override
    public SpelParseException getCause() {
        return (SpelParseException) super.getCause();
    }
}
