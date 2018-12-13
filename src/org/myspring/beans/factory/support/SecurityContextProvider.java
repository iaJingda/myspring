package org.myspring.beans.factory.support;

import java.security.AccessControlContext;

public interface SecurityContextProvider {
    AccessControlContext getAccessControlContext();

}
