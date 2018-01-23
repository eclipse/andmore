/*******************************************************************************
 * Copyright (c) 2008-2010 MontaVista Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Yu-Fen Kuo (MontaVista) - initial API and implementation
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [271695] - Support to non-persistent instances of devices
 * Mauren Brenner (Eldorado) - [281377] Support device types whose instances cannot be created by user
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Marcelo Marzola Bossoni (Instituto de Pesquisas Eldorado) - [352157] Added basic drag and drop support 
 *******************************************************************************/

package org.eclipse.sequoyah.device.framework.model;

import java.util.List;
import java.util.Properties;

import org.eclipse.sequoyah.device.framework.model.handler.IDeviceHandler;
import org.eclipse.swt.graphics.Image;

public interface IDeviceType {

	public String getId();

	public void setId(String id);

	public String getLabel();

	public void setLabel(String label);

	public boolean isAbstract();

	public boolean isPersistent();

	public boolean supportsUserInstances();

	public void setAbstract(boolean isAbstract);

	public String getSuperClass();

	public void setSuperClass(String superClass);

	public Properties getProperties();

	public void setProperties(Properties properties);

	public void addProperty(String name, String value);

	public String getBundleName();

	public void setBundleName(String bundleName);

	public Image getImage();

	public IDeviceHandler getHandler();

	public void setHandler(IDeviceHandler handler);	
	
	public List<IService> getServices();

	public void setServices(List<IService> services);

	public IDeviceTypeDropSupport getDropSupport();
}
