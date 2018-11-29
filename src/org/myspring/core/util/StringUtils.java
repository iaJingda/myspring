package org.myspring.core.util;

public  abstract class StringUtils {

    public static boolean hasLength(String str) {
        return (str != null && !str.isEmpty());
    }
}
