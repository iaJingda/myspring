package org.myspring.expression;

public class ParseException extends ExpressionException  {

    public ParseException(String expressionString, int position, String message) {
        super(expressionString, position, message);
    }

    public ParseException(int position, String message, Throwable cause) {
        super(position, message, cause);
    }

    public ParseException(int position, String message) {
        super(position, message);
    }

}
