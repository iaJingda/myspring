package org.myspring.context;

public interface MessageSourceResolvable {

    String[] getCodes();

    Object[] getArguments();

    String getDefaultMessage();
}
