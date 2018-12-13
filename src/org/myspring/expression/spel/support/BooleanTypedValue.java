package org.myspring.expression.spel.support;

import org.myspring.expression.TypedValue;

public class BooleanTypedValue extends TypedValue {
    public static final BooleanTypedValue TRUE = new BooleanTypedValue(true);

    public static final BooleanTypedValue FALSE = new BooleanTypedValue(false);


    private BooleanTypedValue(boolean b) {
        super(b);
    }


    public static BooleanTypedValue forValue(boolean b) {
        return (b ? TRUE : FALSE);
    }

}
