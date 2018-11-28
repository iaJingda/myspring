package org.myspring.core;

public abstract  class NestedRuntimeException extends  RuntimeException {

    private static final  long serialVersionUID = 555L;

    static {

        NestedExceptionUtils.class.getName();
    }

    public NestedRuntimeException(String msg){
        super(msg);
    }

    public NestedRuntimeException(String msg,Throwable cause){
        super(msg,cause);
    }

    public String getMessage(){
        return NestedExceptionUtils.buildMessage(super.getMessage(),getCause());
    }

    public Throwable getRootCause(){
        Throwable rootCause = null;
        Throwable cause = getCause();
        while(cause!=null && cause !=rootCause){
            rootCause = cause;
            cause= cause.getCause();
        }

        return rootCause;
    }
}
