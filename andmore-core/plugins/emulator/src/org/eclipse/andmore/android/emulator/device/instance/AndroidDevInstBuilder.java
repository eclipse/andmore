/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.andmore.android.emulator.device.instance;

import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.sequoyah.device.framework.model.IInstanceBuilder;

/**
 * DESCRIPTION: <br>
 * This class represents a TmL IInstanceBuilder for Android Emulator Instances. <br>
 * It is only necessary because TmL's AbstractNewEmulatorInstanceWizard could
 * not be used for Android Emulator Device Instance New Wizard implementation,
 * so an IInstanceBuilder became necessary for our Wizard implementation. <br>
 * This class should be removed if TmL's Wizard begins to be used since this
 * implementation is very similar to TmL DefaultInstanceBuilder implementation
 * and Tml's Wizard would use it instead. <br>
 * RESPONSIBILITY: <br>
 * - Hold necessary information about a Android Emulator Device Instance for it
 * to be created. <br>
 * COLABORATORS: <br>
 * IInstanceBuilder: implements this interface <br>
 * USAGE: <br>
 * The AndroidNewWizard uses this class to hold information for creating a
 * Android Emulator Device Instance and passes it on to TmL's InstanceManager to
 * carry on the creation.
 */
public class AndroidDevInstBuilder implements IInstanceBuilder {
	private final Properties properties;

	private final String name;

	/**
	 * Creates a new Instance Builder with the given information.
	 * 
	 * @param instanceName
	 *            the name of the instance to be created using this builder
	 * @param properties
	 *            the properties of the instance to be created using this
	 *            builder
	 */
	public AndroidDevInstBuilder(String instanceName, Properties properties) {
		this.properties = properties;
		this.name = instanceName;
	}

	/**
	 * Always returns <code>null</code> since this information does not make
	 * sense for Android Emulator Instances.
	 */
	@Override
	public IPath getLocationPath() {
		return null;
	}

	/**
	 * Retrieves the name of the instance to be created using this builder
	 * 
	 * @return the name of the instance to be created using this builder
	 */
	@Override
	public String getProjectName() {
		return name;
	}

	/**
	 * Retrieves the properties of the instance to be created using this builder
	 * 
	 * @return the properties of the instance to be created using this builder
	 */
	@Override
	public Properties getProperties() {
		return properties;
	}

	/**
	 * Retrieves the value of the give property key.
	 * 
	 * @param key
	 *            the key of the property
	 * 
	 * @return the value for the property for the instance to be created using
	 *         this builder
	 */
	@Override
	public String getProperty(String key) {
		return properties.getProperty(key);
	}
}
