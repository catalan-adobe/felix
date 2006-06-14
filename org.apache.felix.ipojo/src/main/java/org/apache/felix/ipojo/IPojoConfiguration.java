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
package org.apache.felix.ipojo;

import java.util.logging.Level;

import org.apache.felix.ipojo.handlers.architecture.ArchitectureHandler;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandler;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandler;
import org.apache.felix.ipojo.handlers.lifecycle.callback.LifecycleCallbackHandler;
import org.apache.felix.ipojo.handlers.providedService.ProvidedServiceHandler;

/**
 * Activator Basic Configuration.
 * - Log Level
 * - Available handlers
 * @author Clement Escoffier
 */
public class IPojoConfiguration {

	/**
	 * iPOJO logger log level.
	 */
	public static final Level LOG_LEVEL = Level.WARNING;

	/**
	 * Available handlers in the iPOJO bundle.
	 */
	public static final Class[] INTERNAL_HANDLERS = new Class[] {
		DependencyHandler.class,
		ProvidedServiceHandler.class,
		LifecycleCallbackHandler.class,
		ConfigurationHandler.class,
		ArchitectureHandler.class
	};

}
