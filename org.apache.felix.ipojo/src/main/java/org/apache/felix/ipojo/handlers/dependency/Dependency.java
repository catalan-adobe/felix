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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.logging.Level;

import org.apache.felix.ipojo.Activator;
import org.apache.felix.ipojo.ComponentManager;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * Represent a service dependency either for a componenet dependency either for
 * a provided service dependency. Date : 3 d�c. 2005
 * @author cl�ment
 */
public class Dependency implements ServiceListener {

    /**
     * Dependency State : RESOLVED.
     */
    public static final int RESOLVED = 1;

    /**
     * Dependency State : UNRESOLVED.
     */
    public static final int UNRESOLVED = 2;

    /**
     * Link to the Component Manager.
     * m_handler : ComponentManager
     */
    private DependencyHandler m_handler;


    /**
     * Metadata of the dependency.
     * m_metadata : dependency metadata
     */
    private DependencyMetadata m_metadata;

    /**
     * Array of Service Objects.
     * When cardinality = 1 : just the first element is returned
     * When cardinality = ?..n : all the array is returned
     * m_services : Array
     */
    private Object[] m_services = new Object[0];

    /**
     * Array of service references.
     * m_ref : Array
     */
    private ServiceReference[] m_ref = new ServiceReference[0];

    /**
     * State of the dependency.
     * 0 : stopped, 1 : valid, 2 : invalid.
     * m_state : int
     */
    private int m_state;

    /**
     * True if the reference list change after the creation of a service object array.
     */
    private boolean m_change;

    /**
     * Class of the dependency.
     * Usefull to create in the case of multiple dependency
     */
    private Class m_clazz;


    /**
     * Dependency contructor. After the creation the dependency is not started.
     * @param dh : the dependency handler managing this dependency
     * @param dm : the depednency metadata
     */
    public Dependency(DependencyHandler dh, DependencyMetadata dm) {
    	m_handler = dh;
        m_metadata = dm;
    }

    /**
     * @return the dependency metadata.
     */
    public DependencyMetadata getMetadata() { return m_metadata; }

    /**
     * @return the dependency handler of this dependency.
     */
    public DependencyHandler getDependencyHandler() { return m_handler; }

    /**
     * @return the used service.
     */
    public HashMap getUsedServices() {
    	HashMap hm = new HashMap();
    	if (m_metadata.isMultiple()) {
    		for (int i = 0; i < m_ref.length; i++) {
    			if (i < m_services.length) { hm.put(((Object)m_services[i]).toString(), m_ref[i]); }
    		}
    	} else {
    		if (m_ref.length != 0 && m_services.length != 0) { hm.put(((Object)m_services[0]).toString(), m_ref[0]); }
    	}
    	return hm;
    }

    /**
     * A dependency is satisfied if it is optional of ref.length != 0.
     * @return true is the dependency is satified
     */
    protected boolean isSatisfied() {
        return m_metadata.isOptional() || m_ref.length != 0;
    }

    /**
     * This method is called by the replaced code in the component implementation class.
     * Construct the service object list is necessary.
     * @return null or a service object or a list of service object according to the dependency.
     */
    protected Object get() {
        Activator.getLogger().log(Level.INFO, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Call get for a dependency on : " + m_metadata.getServiceSpecification()
                        + " Multiple : " + m_metadata.isMultiple() + " Optional : " + m_metadata.isOptional());
        try {

            // 1 : Test if there is any change in the reference list :
            if (!m_change) {
                if (!m_metadata.isMultiple()) {
                    if (m_services.length > 0) {
                        return m_services[0]; }
                    }
                else {
                    return m_services;
               }
            }

            // 2 : Else there is a change in the list -> recompute the m_services array
            Activator.getLogger().log(Level.INFO, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Create a service array of " + m_clazz.getName());
            m_services = (Object[])Array.newInstance(m_clazz, m_ref.length);

            for (int i = 0; i < m_ref.length; i++) {
                m_services[i] = m_handler.getComponentManager().getContext().getService(m_ref[i]);
            }

            m_change = false;
            Activator.getLogger().log(Level.INFO, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Create an array with the size " + m_services.length);


            // 3 : The service object list is populated, I return either the first service object, either the array.
            // Return null or an empty array if no service are found.
            if (!m_metadata.isMultiple()) {
                if (m_services.length > 0) {
                    return m_services[0];
                } else {
                		// Load the nullable class
                		String[] segment = m_metadata.getServiceSpecification().split("[.]");
                		String className = "org.apache.felix.ipojo." + segment[segment.length - 1] + "Nullable";
                		Activator.getLogger().log(Level.INFO, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Try to load the nullable class for " + getMetadata().getServiceSpecification() + " -> " + className);
                		Class nullableClazz = m_handler.getNullableClass(className);

                		if (nullableClazz == null) {
                			Activator.getLogger().log(Level.INFO, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Cannot load the nullable class to return a dependency object for " + m_metadata.getField() + " -> " + m_metadata.getServiceSpecification());
                			return null;
                		}

                		// The nullable class is loaded, create the object and return it
                		Object instance = nullableClazz.newInstance();
                		Activator.getLogger().log(Level.INFO, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Nullable object created for " + getMetadata().getServiceSpecification() + " -> " + instance);
                		return instance;
                	}
            }
            else { // Multiple dependency
                    return m_services;
            }
        } catch (Exception e) {
            // There is a problem in the dependency resolving (like in stopping method)
            if (!m_metadata.isMultiple()) {
            	Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Return null, an exception was throwed in the get method -> " + e.getMessage());
                return null; }
            else {
            	Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Return an empty array, an exception was throwed in the get method" + e.getMessage());
                return Array.newInstance(m_clazz, 0); }
        }
    }

    /**
     * Method calld when a service event is throwed.
     * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
     * @param event : the received service event
     */
    public void serviceChanged(ServiceEvent event) {
        synchronized (this) {

            // If a service goes way.
            if (event.getType() == ServiceEvent.UNREGISTERING) {
                Activator.getLogger().log(Level.INFO, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] A service is gone -> " + event.getServiceReference().getBundle());
                if (containsSR(event.getServiceReference())) {
                		// Call unbind method
                		callUnbindMethod(event.getServiceReference());
                		// Unget the service reference
                    	m_handler.getComponentManager().getContext().ungetService(event.getServiceReference());
                        int index = removeReference(event.getServiceReference());

                        // Is the state valid or invalid
                        if (m_ref.length == 0 && !m_metadata.isOptional()) {
                            m_state = UNRESOLVED;
                        }
                        if (m_ref.length == 0 && m_metadata.isOptional()) {
                            m_state = RESOLVED;
                        }
                        // Is there any change ?
                        if (!m_metadata.isMultiple() && index == 0) { m_change = true; }
                        if (!m_metadata.isMultiple() && index != 0) { m_change = false; }
                        if (m_metadata.isMultiple()) { m_change = true;  }
                    }
                    m_handler.checkContext();
                    return;
            }

            // If a service arrives
            if (event.getType() == ServiceEvent.REGISTERED) {
                // Add the new service inside the ref list
                Activator.getLogger().log(Level.INFO, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Add a service for a dependency");
                addReference(event.getServiceReference());
                if (isSatisfied()) {
                    m_state = RESOLVED;
                    if (m_metadata.isMultiple() || m_ref.length == 1) { m_change = true; }
                    callBindMethod(event.getServiceReference());
                }
                m_handler.checkContext();
            }

        }
    }

    private void callUnbindMethod(ServiceReference ref) {
        if (m_handler.getComponentManager().getState() == ComponentManager.VALID && m_metadata.isMultiple()) {
        	for (int i = 0; i < m_metadata.getCallbacks().length; i++) {
        		if (m_metadata.getCallbacks()[i].getMethodType() == DependencyCallback.UNBIND) {
        			// Try to call the bind method with a service reference inside
        			try {
						m_metadata.getCallbacks()[i].call(new Object[] {ref});
					} catch (NoSuchMethodException e) {
						// The method was not found : try without service reference
						try {
							m_metadata.getCallbacks()[i].call();
						} catch (NoSuchMethodException e1) {
							// The method was not found : try with the service object
							try {
								m_metadata.getCallbacks()[i].call(new Object[] {m_handler.getComponentManager().getContext().getService(ref)});
							} catch (NoSuchMethodException e2) {
								Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Dependency Callback Error : Unbind method not found : " + e1.getMessage());
								return;
							} catch (IllegalAccessException e2) {
								Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Dependency Callback Error : Illegal access on unbind method : " + e2.getMessage());
								return;
							} catch (InvocationTargetException e2) {
								Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the unbind method " + e2.getMessage());
								return;
							}
						} catch (IllegalAccessException e1) {
							Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Dependency Callback Error : Illegal access on unbind method : " + e1.getMessage());
							return;
						} catch (InvocationTargetException e1) {
							Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the unbind method " + e1.getMessage());
							return;
						}

					} catch (IllegalAccessException e) {
						Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Dependency Callback Error : Illegal access on bind method : " + e.getMessage());
						return;
					} catch (InvocationTargetException e) {
						Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the bind method " + e.getMessage());
						return;
					}
        		}
        	}
        }
    }

    private void callBindMethod(ServiceReference ref) {
    	// call bind method :
        if (m_handler.getComponentManager().getState() == ComponentManager.VALID && m_metadata.isMultiple()) {
        	for (int i = 0; i < m_metadata.getCallbacks().length; i++) {
        		if (m_metadata.getCallbacks()[i].getMethodType() == DependencyCallback.BIND) {
        			// Try to call the bind method with a service reference inside
        			try {
						m_metadata.getCallbacks()[i].call(new Object[] {ref});
					} catch (NoSuchMethodException e) {
						// The method was not found : try without service reference
						try {
							m_metadata.getCallbacks()[i].call();
						} catch (NoSuchMethodException e1) {
							// The method was not found : try with the service object
							try {
								m_metadata.getCallbacks()[i].call(new Object[] {m_handler.getComponentManager().getContext().getService(ref)});
							} catch (NoSuchMethodException e2) {
								Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Dependency Callback Error : Bind method not found : " + e1.getMessage());
								return;
							} catch (IllegalAccessException e2) {
								Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Dependency Callback Error : Illegal access on bind method : " + e2.getMessage());
								return;
							} catch (InvocationTargetException e2) {
								Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the bind method " + e2.getMessage());
								return;
							}
						} catch (IllegalAccessException e1) {
							Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Dependency Callback Error : Illegal access on bind method : " + e1.getMessage());
							return;
						} catch (InvocationTargetException e1) {
							Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the bind method " + e1.getMessage());
							return;
						}

					} catch (IllegalAccessException e) {
						Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Dependency Callback Error : Illegal access on bind method : " + e.getMessage());
						return;
					} catch (InvocationTargetException e) {
						Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the bind method " + e.getMessage());
						return;
					}
        		}
        	}
        }
    }

    /**
     * Start the dependency.
     */
    public void start() {
        // Construct the filter with the objectclass + filter
        String classnamefilter = "(objectClass=" + m_metadata.getServiceSpecification() + ")";
        String filter = "";
        if (!m_metadata.getFilter().equals("")) {
            filter = "(&" + classnamefilter + m_metadata.getFilter() + ")";
        }
        else {
            filter = classnamefilter;
        }

        Activator.getLogger().log(Level.INFO, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Start a dependency on : " + m_metadata.getServiceSpecification() + " with " + m_metadata.getFilter());
        m_state = UNRESOLVED;

        try {
            m_clazz = m_handler.getComponentManager().getContext().getBundle().loadClass(m_metadata.getServiceSpecification());
        } catch (ClassNotFoundException e) {
            System.err.println("Cannot load the interface class for the dependency " + m_metadata.getField() + " [" + m_metadata.getServiceSpecification() + "]");
            e.printStackTrace();
        }

        try {
            // Look if the service is already present :
            ServiceReference[] sr = m_handler.getComponentManager().getContext().getServiceReferences(
            		m_metadata.getServiceSpecification(), filter);
            if (sr != null) {
                for (int i = 0; i < sr.length; i++) { addReference(sr[i]); }
                m_state = RESOLVED;
                }
            // Register a listener :
            m_handler.getComponentManager().getContext().addServiceListener(this, filter);
            m_change = true;
        }
        catch (InvalidSyntaxException e1) {
            Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] A filter is malformed : " + filter);
            e1.printStackTrace();
        }
    }

    /**
     * Stop the dependency.
     */
    public void stop() {
        Activator.getLogger().log(Level.INFO, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Stop a dependency on : " + m_metadata.getServiceSpecification() + " with " + m_metadata.getFilter());
        m_state = UNRESOLVED;

        // Unget all services references
        for (int i = 0; i < m_ref.length; i++) {
        	m_handler.getComponentManager().getContext().ungetService(m_ref[i]);
        }

        m_ref = new ServiceReference[0];
        m_handler.getComponentManager().getContext().removeServiceListener(this);
        m_clazz = null;
        m_services = null;
    }

    /**
     * Return the state of the dependency.
     * @return the state of the dependency (1 : valid, 2 : invalid)
     */
    public int getState() {
        return m_state;
    }

    /**
     * Return the list of service reference.
     * @return the service reference list.
     */
    public ServiceReference[] getServiceReferences() {
        return m_ref;
    }

    /**
     * Add a service reference in the current list.
     * @param r : the new service reference to add
     */
    private void addReference(ServiceReference r) {
        for (int i = 0; (m_ref != null) && (i < m_ref.length); i++) {
            if (m_ref[i] == r) {
                return;
            }
        }

        if (m_ref != null) {
            ServiceReference[] newSR = new ServiceReference[m_ref.length + 1];
            System.arraycopy(m_ref, 0, newSR, 0, m_ref.length);
            newSR[m_ref.length] = r;
            m_ref = newSR;
        }
        else {
            m_ref = new ServiceReference[] {r};
        }
    }

    /**
     * Find if a service registration il already registred.
     * @param sr : the service registration to find.
     * @return true if the service registration is already in the array
     */
    private boolean containsSR(ServiceReference sr) {
        for (int i = 0; i < m_ref.length; i++) {
            if (m_ref[i] == sr) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove a service reference in the current list.
     * @param r : the new service reference to remove
     * @return the index of the founded element, or -1 if the element is not found
     */
    private int removeReference(ServiceReference r) {
        if (m_ref == null) {
            m_ref = new ServiceReference[0];
        }

        int idx = -1;
        for (int i = 0; i < m_ref.length; i++) {
            if (m_ref[i] == r) {
                idx = i;
                break;
            }
        }

        if (idx >= 0) {
            // If this is the module, then point to empty list.
            if ((m_ref.length - 1) == 0) {
                m_ref = new ServiceReference[0];
            }
            // Otherwise, we need to do some array copying.
            else {
                ServiceReference[] newSR = new ServiceReference[m_ref.length - 1];
                System.arraycopy(m_ref, 0, newSR, 0, idx);
                if (idx < newSR.length)             {
                    System.arraycopy(
                            m_ref, idx + 1, newSR, idx, newSR.length - idx);
                }
                m_ref = newSR;
            }
        }
        return idx;
    }


}
