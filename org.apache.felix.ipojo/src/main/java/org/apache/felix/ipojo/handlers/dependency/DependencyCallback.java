/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.ipojo.handlers.dependency;

import java.lang.reflect.InvocationTargetException;

import org.apache.felix.ipojo.Callback;


/**
 * This class allwos the creation of callback when service dependency arrives or disappear.
 * @author escoffie
 *
 */
public class DependencyCallback {

	/**
	 * Bind method (called when a service arrives).
	 */
	public static final int BIND = 0;

	/**
	 * Unbind method (called when a service disappears).
	 */
	public static final int UNBIND = 1;

	/**
	 * Is the method a bind method or an unbind method ?
	 */
	private int m_methodType;

    /**
     * Callback object.
     */
    private Callback m_callback;


    /**
     * Constructor.
     * @param dep : the dependency attached to this depednency callback
     * @param method : the method to call
     * @param methodType : is the method to call a bind method or an unbind method
     * @param isStatic : is the method to call static ?
     */
    public DependencyCallback(Dependency dep, String method, int methodType, boolean isStatic) {
    	m_methodType = methodType;
        m_callback = new Callback(method, isStatic, dep.getDependencyHandler().getComponentManager());
    }

    /**
     * @return the method type.
     */
    public int getMethodType() { return m_methodType; }

    /**
     * Call the callback method.
     * @throws NoSuchMethodException : Method is not found in the class
     * @throws InvocationTargetException : The method is not static
     * @throws IllegalAccessException : The method can not be invoked
     */
    protected void call() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    	m_callback.call();
    }

    /**
     * Call the callback method.
     * @throws NoSuchMethodException : Method is not found in the class
     * @throws InvocationTargetException : The method is not static
     * @throws IllegalAccessException : The method can not be invoked
     */
    protected void call(Object[] arg) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    	m_callback.call(arg);
    }
}
