/*******************************************************************************
 * Copyright (c) 2008-2010 MontaVista Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yu-Fen Kuo (MontaVista) - initial API and implementation
 *     Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/
package org.eclipse.sequoyah.device.framework.ui.wizard;

import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.sequoyah.device.framework.model.IInstanceBuilder;

public class InstanceBuilder implements IInstanceBuilder {
	private String instanceName;
	private Properties properties;

	public InstanceBuilder(String instanceName, Properties properties) {
		this.instanceName = instanceName;
		this.properties = properties;
	}

	public IPath getLocationPath() {
		return null;
	}

	public String getProjectName() {
		return instanceName;
	}

	public Properties getProperties() {
		return properties;
	}

	public String getProperty(String key) {
		return properties.getProperty(key);
	}

}
