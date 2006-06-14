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
package org.apache.felix.ipojo.handlers.providedService;

import java.util.Properties;
import java.util.logging.Level;

import org.apache.felix.ipojo.ComponentManager;
import org.apache.felix.ipojo.Activator;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;


/**
 * Provided Service represent a provided service by the component.
 * Date : 3 d�c. 2005
 * @author cl�ment
 *
 */
public class ProvidedService implements ServiceFactory {

    /**
     * Service State : REGISTRED.
     */
    public static final int REGISTERED = 1;

    /**
     * Service State : UNREGISTRED.
     */
    public static final int UNREGISTERED = 0;

    /**
     * Factory Policy : SINGLETON_FACTORY.
     */
    public static final int SINGLETON_FACTORY = 0;

    /**
     * Factory policy : SERVICE_FACTORY.
     */
    public static final int SERVICE_FACTORY = 1;

    /**
     * Factory policy : COMPONENT_FACTORY.
     * TODO : Component_factory behavior
     */
    public static final int COMPONENT_FACTORY = 2;

    /**
     * The service registration.
     * is null when the service is not registred.
     * m_serviceRegistration : ServiceRegistration
     */
    private ServiceRegistration m_serviceRegistration;

    /**
     * Link to the component manager.
     * m_handler : ComponentManager
     */
    private ProvidedServiceHandler m_handler;

    /**
     * Provided service metadata.
     */
    private ProvidedServiceMetadata m_metadata;

    /**
     * State of the provided service.
     */
    private int m_state;

    /**
     * Properties Array.
     */
    private Property[] m_properties = new Property[0];


    /**
     * Construct a provided service object.
     * @param handler : the provided service handler.
     * @param psm : the provided service metadata.
     */
    public ProvidedService(ProvidedServiceHandler handler, ProvidedServiceMetadata psm) {
        m_handler = handler;
        m_metadata = psm;
        for (int i = 0; i < psm.getProperties().length; i++) {
        	Property prop = new Property(this, ((PropertyMetadata)psm.getProperties()[i]));
        	addProperty(prop);
        }
    }

    /**
     * Add the given property to the property list.
     * @param p : the element to add
     */
    private void addProperty(Property p) {
        for (int i = 0; (m_properties != null) && (i < m_properties.length); i++) {
            if (m_properties[i] == p) { return; }
        }

        if (m_properties.length > 0) {
            Property[] newProp = new Property[m_properties.length + 1];
            System.arraycopy(m_properties, 0, newProp, 0, m_properties.length);
            newProp[m_properties.length] = p;
            m_properties = newProp;
        }
        else { m_properties = new Property[] {p}; }
    }

    /**
     * @return the service reference of the provided service (null if the service is not published).
     */
    public ServiceReference getServiceReference() {
    	if (m_serviceRegistration != null) { return m_serviceRegistration.getReference(); }
    	else { return null; }
    }

    /**
     * Return a service object for the dependency.
     * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
     * @param bundle : the bundle
     * @param registration : the service registration of the registred service
     * @return : a new service object or a already created service object (in the case of singleton)
     */
    public Object getService(Bundle bundle, ServiceRegistration registration) {

    	switch(m_metadata.getFactoryPolicy()) {

    	case SINGLETON_FACTORY :
            return m_handler.getComponentManager().getInstance();

    	case SERVICE_FACTORY :
    		return m_handler.getComponentManager().createInstance();

    	case COMPONENT_FACTORY :
    		//TODO Component Factory Behavior
    		return null;

    	default :
    		Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Unknown factory policy for " + m_metadata.getServiceSpecification() + " : " + m_metadata.getFactoryPolicy());
    		return null;
    	}

    }

    /**
     * The unget method.
     * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
     * @param bundle : bundle
     * @param registration : service registration
     * @param service : service object
     */
    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        //Nothing to do
    }

//    /**
//     * Validate the service dependencies of the current provided service.
//     * @return true if the service dependencies are valid
//     */
//    public boolean validate() {
//        boolean valide = true;
//        for (int i = 0; i < m_dependencies.length; i++) {
//            Dependency dep = m_dependencies[i];
//            valide = valide & dep.isSatisfied();
//            if (!valide) {
//                ComponentManager.getLogger().log(Level.INFO, "Service Dependency  for " + m_interface + " not valid : " + dep.getInterface());
//                return false;
//            }
//        }
//        ComponentManager.getLogger().log(Level.INFO, "Service dependencies for " + m_interface + " are valid");
//        return valide;
//    }

    /**
     * Register the service.
     * The service object must be able to serve this service.
     * To avoid cycle in Check Context, the registred service is set to registred before the real registration.
     */
    protected void registerService() {
    	if (m_state != REGISTERED) {
            String spec = "";
            for (int i = 0; i < m_metadata.getServiceSpecification().length; i++) {
                spec = spec + m_metadata.getServiceSpecification()[i] + ", ";
            }
    			Activator.getLogger().log(Level.INFO, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Register the service : " + spec);
    			// Contruct the service properties list
    			Properties serviceProperties = getServiceProperties();

    			m_state = REGISTERED;
    			m_serviceRegistration = m_handler.getComponentManager().getContext().registerService(m_metadata.getServiceSpecification(), this, serviceProperties);
    	}
    }

    /**
     * Unregister the service.
     */
    protected void unregisterService() {
    	if (m_state == REGISTERED) {
    		try {
    			m_serviceRegistration.unregister();
    			m_serviceRegistration = null;
    		} catch (Exception e) { return; }
			m_state = UNREGISTERED;
    	}
    }

    /**
     * @return The state of the provided service.
     */
    public int getState() {
        return m_state;
    }

    /**
     * @return the component manager.
     */
    protected ComponentManager getComponentManager() {
    	return m_handler.getComponentManager();
    }

    /**
     * Return the list of properties attached to this service.
     * This list contains only property where a value are assigned.
     * @return the properties attached to the provided service.
     */
    private Properties getServiceProperties() {
        // Contruct the service properties list
        Properties serviceProperties = new Properties();
        for (int i = 0; i < m_properties.length; i++) {
            if (m_properties[i].get() != null) {
                serviceProperties.put(m_properties[i].getMetadata().getName(), m_properties[i].get().toString());
            }
        }
        return serviceProperties;
    }

    /**
     * @return the properties attached to the provided service.
     */
    public Property[] getProperties() {
        return m_properties;
    }

    /**
     * Update refresh the service properties.
     * The new list of properties is sended to the service registry.
     */
    protected void update() {
        // Update the service properties

        // Contruct the service properties list
        Properties serviceProperties = getServiceProperties();

        // Update the service registration
        if (m_state == REGISTERED) { m_serviceRegistration.setProperties(serviceProperties); }
    }

	/**
	 * @return the propvided service metadata.
	 */
	public ProvidedServiceMetadata getMetadata() {
		return m_metadata;
	}

}
