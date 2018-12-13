package org.myspring.expression.spel;

import org.myspring.expression.EvaluationException;

public class SpelEvaluationException extends EvaluationException {

    private final SpelMessage message;

    private final Object[] inserts;


    public SpelEvaluationException(SpelMessage message, Object... inserts) {
        super(message.formatMessage(inserts));
        this.message = message;
        this.inserts = inserts;
    }

    public SpelEvaluationException(int position, SpelMessage message, Object... inserts) {
        super(position, message.formatMessage(inserts));
        this.message = message;
        this.inserts = inserts;
    }

    public SpelEvaluationException(int position, Throwable cause, SpelMessage message, Object... inserts) {
        super(position, message.formatMessage(inserts), cause);
        this.message = message;
        this.inserts = inserts;
    }

    public SpelEvaluationException(Throwable cause, SpelMessage message, Object... inserts) {
        super(message.formatMessage(inserts), cause);
        this.message = message;
        this.inserts = inserts;
    }


    /**
     * Set the position in the related expression which gave rise to this exception.
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Return the message code.
     */
    public SpelMessage getMessageCode() {
        return this.message;
    }

    /**
     * Return the message inserts.
     */
    public Object[] getInserts() {
        return this.inserts;
    }
}
