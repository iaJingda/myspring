package org.myspring.aop.framework.adapter;

public class UnknownAdviceTypeException  extends IllegalArgumentException  {

    public UnknownAdviceTypeException(Object advice) {
        super("Advice object [" + advice + "] is neither a supported subinterface of " +
                "[org.aopalliance.aop.Advice] nor an [org.springframework.aop.Advisor]");
    }

    /**
     * Create a new UnknownAdviceTypeException with the given message.
     * @param message the message text
     */
    public UnknownAdviceTypeException(String message) {
        super(message);
    }

}
