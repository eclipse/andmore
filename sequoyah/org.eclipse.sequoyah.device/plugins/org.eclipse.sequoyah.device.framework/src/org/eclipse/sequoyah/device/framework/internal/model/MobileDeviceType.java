/*******************************************************************************
 * Copyright (c) 2008-2010 MontaVista Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yu-Fen Kuo (MontaVista) - initial API and implementation
 *     Daniel Barboza Franco (Eldorado Research Institute) - Bug [259243] - instance management view is showing device type ids instead of names
 *     Daniel Barboza Franco (Eldorado Research Institute) - Bug [271695] - Support to non-persistent instances of devices
 *     Mauren Brenner (Eldorado) - [281377] Support device types whose instances cannot be created by user
 *     Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *     Pablo Leite (Eldorado) - [329548] Allow multiple instances selection on Device Manager View
 *     Marcelo Marzola Bossoni (Instituto de Pesquisas Eldorado) - [352157] Added basic drag and drop support  
 *******************************************************************************/
package org.eclipse.sequoyah.device.framework.internal.model;

import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.sequoyah.device.common.utilities.PluginUtils;
import org.eclipse.sequoyah.device.framework.DevicePlugin;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.IDeviceTypeDropSupport;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.model.handler.IDeviceHandler;
import org.eclipse.swt.graphics.Image;

public class MobileDeviceType implements IDeviceType {

	private static final String ELEMENT_DEVICE = "deviceType";//$NON-NLS-1$
	private static final String ATR_ICON = "icon";//$NON-NLS-1$
	private static final String ATR_IS_PERSISTENT = "isPersistent";//$NON-NLS-1$
	private static final String ATR_SUPPORTS_USER_INSTANCES = "supportsUserInstances";//$NON-NLS-1$
	private static final String ATR_DROP_SUPPORT_HANDLER = "dropSupportHandler";//$NON-NLS-1$

	private static final String PROPERTY_ICON = "icon"; //$NON-NLS-1$
	private String id;
	private String label;
	private String bundleName;
	private boolean isAbstract = false;
	private String superClass;
	private IDeviceHandler handler;
	private Properties properties = new Properties();
	private List<IService> services;
	private boolean isPersistent = true;
	private boolean supportsUserInstances = true;
	private final IDeviceTypeDropSupport dropSupport;

	public MobileDeviceType(String id, String label) {
		this.id = id;
		this.label = label;

		IExtension fromPlugin =  PluginUtils.getExtension(DevicePlugin.DEVICE_TYPES_EXTENSION_POINT_ID, id);
		String isPersistentStr = PluginUtils.getPluginAttribute(fromPlugin, ELEMENT_DEVICE, ATR_IS_PERSISTENT);
		String supportsUserInstancesStr = PluginUtils.getPluginAttribute(fromPlugin, ELEMENT_DEVICE, ATR_SUPPORTS_USER_INSTANCES);
		Object dropSupportExtension;
		IDeviceTypeDropSupport dropSupportObj = null;
		try {
			dropSupportExtension = PluginUtils.getExecutableAttribute(
					fromPlugin, ELEMENT_DEVICE, ATR_DROP_SUPPORT_HANDLER);
			if (dropSupportExtension instanceof IDeviceTypeDropSupport) {
				dropSupportObj = (IDeviceTypeDropSupport) dropSupportExtension;
			}
		} catch (CoreException e) {
			// do nothing
		}
		dropSupport = dropSupportObj;

		if (isPersistentStr != null) {
			isPersistent = Boolean.valueOf(isPersistentStr);
		}
		if (supportsUserInstancesStr != null) {
			supportsUserInstances = Boolean.valueOf(supportsUserInstancesStr);
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public void addProperty(String name, String value) {
		this.properties.put(name, value);
	}

	public String getBundleName() {
		return bundleName;
	}

	public void setBundleName(String bundleName) {
		this.bundleName = bundleName;
	}

	public List<IService> getServices() {
		return services;
	}

	public void setServices(List<IService> services) {
		this.services = services;
	}

	public IDeviceHandler getHandler() {
		return handler;
	}

	public void setHandler(IDeviceHandler handler) {
		this.handler = handler;
	}

	public Image getImage() {
		if (getProperties().containsKey(PROPERTY_ICON)) {
			String path = getProperties().getProperty(PROPERTY_ICON);
			Image image = DevicePlugin.getDefault().getImageFromRegistry(
					getBundleName(), path);
			return image;
		}
		else {
			IExtension fromPlugin =  PluginUtils.getExtension(DevicePlugin.DEVICE_TYPES_EXTENSION_POINT_ID, id);
			String iconName = PluginUtils.getPluginAttribute(fromPlugin, ELEMENT_DEVICE, ATR_ICON);
			ImageDescriptor descr = null;

			try {
				descr = DevicePlugin.getPluginImage(Platform.getBundle(fromPlugin.getContributor().getName()), iconName);
			} catch (InvalidRegistryObjectException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return descr.createImage();
		}
	}

	public String toString() {
		return "[DeviceType: " + "id=" + (id == null ? "" : id) + ",label=" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				+ (label == null ? "" : label) + ",bundleName=" //$NON-NLS-1$ //$NON-NLS-2$
				+ (bundleName == null ? "" : bundleName) + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public String getSuperClass() {
		return superClass;
	}

	public void setSuperClass(String superClass) {
		this.superClass = superClass;
	}

	public boolean isPersistent() {
		return isPersistent;
	}

	public boolean supportsUserInstances() {
		return supportsUserInstances;
	}

	public IDeviceTypeDropSupport getDropSupport() {
		return dropSupport;
	}
}
