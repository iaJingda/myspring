/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.myspring.core.cglib.transform.impl;


import org.myspring.core.cglib.core.ClassGenerator;
import org.myspring.core.cglib.core.DefaultGeneratorStrategy;
import org.myspring.core.cglib.core.TypeUtils;
import org.myspring.core.cglib.transform.ClassTransformer;
import org.myspring.core.cglib.transform.MethodFilter;
import org.myspring.core.cglib.transform.MethodFilterTransformer;
import org.myspring.core.cglib.transform.TransformingClassGenerator;

public class UndeclaredThrowableStrategy extends DefaultGeneratorStrategy {
    

    private Class wrapper;

	/**
     * Create a new instance of this strategy.
     * @param wrapper a class which extends either directly or
     * indirectly from <code>Throwable</code> and which has at least one
     * constructor that takes a single argument of type
     * <code>Throwable</code>, for example
     * <code>java.lang.reflect.UndeclaredThrowableException.class</code>
     */
    public UndeclaredThrowableStrategy(Class wrapper) {
       this.wrapper = wrapper;
    }
    
    private static final MethodFilter TRANSFORM_FILTER = new MethodFilter() {
        public boolean accept(int access, String name, String desc, String signature, String[] exceptions) {
            return !TypeUtils.isPrivate(access) && name.indexOf('$') < 0;
        }
    };

    protected ClassGenerator transform(ClassGenerator cg) throws Exception {
    	 ClassTransformer tr = new UndeclaredThrowableTransformer(wrapper);
         tr = new MethodFilterTransformer(TRANSFORM_FILTER, tr);
        return new TransformingClassGenerator(cg, tr);
    }
}

