/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Mobility, Inc. and others.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Yu-Fen Kuo (MontaVista) - bug#236476 - provide a generic device type
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Pablo Leite (Eldorado) - [329548] Allow multiple instances selection on Device Manager View 
 ********************************************************************************/
package org.eclipse.sequoyah.device.framework.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.sequoyah.device.common.utilities.PluginUtils;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.DevicePlugin;
import org.eclipse.sequoyah.device.framework.internal.model.MobileDeviceType;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.IDeviceTypeRegistry;
import org.eclipse.sequoyah.device.framework.model.IService;

public class DeviceTypeRegistry implements IDeviceTypeRegistry {
	private static final String XML_ELEMENT_DEVICE_TYPE = "deviceType"; //$NON-NLS-1$
	private static final String XML_ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	private static final String XML_ATTRIBUTE_LABEL = "label"; //$NON-NLS-1$
	private static final String XML_ATTRIBUTE_IS_ABSTRACT = "isAbstract"; //$NON-NLS-1$
	private static final String XML_ATTRIBUTE_SUPERCLASS = "superClass"; //$NON-NLS-1$
	private static final String XML_ATTRIBUTE_PROPERTY = "property"; //$NON-NLS-1$
	private static final String XML_ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
	private static final String XML_ATTRIBUTE_VALUE = "value"; //$NON-NLS-1$
	private static DeviceTypeRegistry _instance;
	private Map<String, IDeviceType> deviceTypes;

	private DeviceTypeRegistry() {
		deviceTypes = new HashMap<String, IDeviceType>();
	}

	public static DeviceTypeRegistry getInstance() {
		if (_instance == null) {
			_instance = new DeviceTypeRegistry();

		}
		return _instance;
	}

	public IDeviceType getDeviceTypeById(String id) {
		loadDeviceTypesExtensions();
		return deviceTypes.get(id);
	}

	public Collection<IDeviceType> getDeviceTypes() {
		loadDeviceTypesExtensions();
		return deviceTypes.values();
	}

	public Set<String> getDeviceTypeIds() {
		loadDeviceTypesExtensions();
		return deviceTypes.keySet();
	}

	public ImageDescriptor getImage() {
		return DevicePlugin.getDefault().getImageDescriptor(
				DevicePlugin.ICON_MOVING);
	}

	private synchronized Map<String, IDeviceType> loadDeviceTypesExtensions() {
		if (deviceTypes.isEmpty()) {

			// Get the extensions
			IExtensionPoint extensionPoint = Platform.getExtensionRegistry()
					.getExtensionPoint(
							DevicePlugin.DEVICE_TYPES_EXTENSION_POINT_ID);
			if (extensionPoint != null) {
				IExtension[] extensions = extensionPoint.getExtensions();
				if (extensions != null) {

					for (int i = 0; i < extensions.length; ++i) {
						IExtension extension = extensions[i];
						IConfigurationElement[] elements = extension
								.getConfigurationElements();

						// process the top level elements for this extension
						for (int k = 0; k < elements.length; k++) {
							IConfigurationElement element = elements[k];

							if (element.getName().equals(
									XML_ELEMENT_DEVICE_TYPE)) {
								IDeviceType deviceType = loadDeviceType(element);
								deviceType.setBundleName(element
										.getContributor().getName());
								deviceTypes.put(deviceType.getId(), deviceType);
							}

						}

					}

					for (Iterator<IDeviceType> iterator = deviceTypes.values()
							.iterator(); iterator.hasNext();) {
						IDeviceType deviceType = iterator.next();
						// associate services with device types
						deviceType.setServices(loadServices(deviceType));
						deviceType.getProperties().putAll(
								loadSuperClassProperties(deviceType));
					}

				}
			}
		}
		return deviceTypes;
	}

	private Properties loadSuperClassProperties(IDeviceType deviceType) {
		Properties properties = new Properties();
		String superClassDeviceTypeId = deviceType.getSuperClass();
		if (superClassDeviceTypeId != null) {
			// load all properties defined in super class first.
			// if the same property is defined in the child deviceType, do not
			// overwrite it.
			if (deviceTypes.containsKey(superClassDeviceTypeId)) {
				IDeviceType superClassDeviceType = deviceTypes
						.get(superClassDeviceTypeId);

				properties
						.putAll(loadSuperClassProperties(superClassDeviceType));

				Properties superClassDeviceTypeProperties = superClassDeviceType
						.getProperties();
				Properties currentDeviceTypeProperties = deviceType
						.getProperties();
				for (Iterator<Object> propertyKeys = superClassDeviceTypeProperties
						.keySet().iterator(); propertyKeys.hasNext();) {
					String currentKey = (String) propertyKeys.next();
					if (!currentDeviceTypeProperties.containsKey(currentKey))
						properties.put(currentKey,
								superClassDeviceTypeProperties.get(currentKey));
				}

			}
		}
		return properties;
	}

	private IDeviceType loadDeviceType(IConfigurationElement element) {
		String id = element.getAttribute(XML_ATTRIBUTE_ID);
		String label = element.getAttribute(XML_ATTRIBUTE_LABEL);

		IDeviceType deviceType = new MobileDeviceType(id, label);
		// load properties
		IConfigurationElement[] children = element.getChildren();

		for (int i = 0; i < children.length; i++) {
			IConfigurationElement childElement = children[i];

			if (childElement.getName().equals(XML_ATTRIBUTE_PROPERTY)) {
				String name = childElement.getAttribute(XML_ATTRIBUTE_NAME);
				String value = childElement.getAttribute(XML_ATTRIBUTE_VALUE);

				deviceType.addProperty(name, value);

			}
		}
		String isAbstract = element.getAttribute(XML_ATTRIBUTE_IS_ABSTRACT);
		if (isAbstract != null) {
			if (Boolean.valueOf(isAbstract).booleanValue())
				deviceType.setAbstract(true);
		}
		String superClass = element.getAttribute(XML_ATTRIBUTE_SUPERCLASS);
		if (superClass != null) {

			deviceType.setSuperClass(superClass);
		}

		return deviceType;
	}

	private List<IService> loadServices(IDeviceType deviceType) {
		List<IService> services = new ArrayList<IService>();
		services.addAll(loadServices(deviceType.getId()));

		String superClassDeviceTypeId = deviceType.getSuperClass();
		if (superClassDeviceTypeId != null) {
			// load all services defined in super class first.
			if (deviceTypes.containsKey(superClassDeviceTypeId)) {
				List<IService> superServices = loadServices(deviceTypes
						.get(superClassDeviceTypeId));
				for (IService service : superServices) {
					// do not overwrite the same service if already defined in
					// child class
					if (!serviceExists(services, service.getId()))
						services.add(service);
				}
			}
		}

		return services;
	}

	private boolean serviceExists(List<IService> services, String serviceId) {
		if (services != null) {
			for (IService service : services) {
				if (service.getId().equals(serviceId))
					return true;
			}
		}
		return false;
	}

	private List<IService> loadServices(String deviceTypeId) {
		List<IService> services = new ArrayList<IService>();
		Collection<IExtension> servs = PluginUtils
				.getInstalledExtensions(DevicePlugin.SERVICE_DEF_ID);
		for (IExtension serviceDef : servs) {
			if (serviceDef.getUniqueIdentifier().equals(deviceTypeId)) {
				try {
					IService service = ServiceFactory.createService(serviceDef);
					if(service != null) {
					    services.add(service);
					}
				} catch (SequoyahException t) {
					t.printStackTrace();
				}
			}
		}
		return services;
	}

}
