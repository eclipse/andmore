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
 * Fabio Fantato (Motorola) - bug#221733 - code revisited
 * Otavio Luiz Ferranti (Eldorado Research Institute) - bug#221733 - Adding data persistence
 * Yu-Fen Kuo (MontaVista) - try to replace jdom dependencies with eclipse default xml parsers.
 * Fabio Rigo (Eldorado) - [245114] Enhance persistence policies
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [221739] - Improvements to State machine implementation
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [252261] - Internal class MobileInstance providing functionalities
 * Fabio Fantato (Instituto Eldorado) - [263188] - Create new examples to support tutorial presentation
 * Fabio Fantato (Instituto Eldorado) - [243494] Change the reference implementation to work on Galileo
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [269716] - InstanceDeleted event is been fired when a new Instance is created
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [272056] - Method getInstance() on the singleton class InatanceManager is not synchronized.
 * Fabio Rigo (Eldorado) - Bug [288006] - Unify features of InstanceManager and InstanceRegistry
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [288301] - Device view crashes when there is a device plug-in missing.
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Daniel Barboza Franco (Eldorado) - [329548] - Allow multiple instances selection on Device Manager View
 ********************************************************************************/
package org.eclipse.sequoyah.device.framework.manager;

import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.sequoyah.device.common.utilities.PluginUtils;
import org.eclipse.sequoyah.device.common.utilities.exception.ExceptionHandler;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.DevicePlugin;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.events.InstanceEventManager;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent.InstanceEventType;
import org.eclipse.sequoyah.device.framework.exception.DeviceExceptionHandler;
import org.eclipse.sequoyah.device.framework.exception.DeviceExceptionStatus;
import org.eclipse.sequoyah.device.framework.factory.InstanceRegistry;
import org.eclipse.sequoyah.device.framework.manager.persistence.DeviceXmlWriter;
import org.eclipse.sequoyah.device.framework.model.AbstractMobileInstance;
import org.eclipse.sequoyah.device.framework.model.IDeviceLauncher;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.IInstanceBuilder;
import org.eclipse.sequoyah.device.framework.model.handler.IDeviceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.UndefinedDeviceHandler;
import org.eclipse.sequoyah.device.framework.statemachine.StateMachineHandler;

/**
 * Manages the device instances
 * 
 * @author Fabio Fantato
 */
public class InstanceManager {

	private static final String ELEMENT_DEVICE = "deviceType"; //$NON-NLS-1$
	private static final String ATTR_HANDLER = "handler"; //$NON-NLS-1$
	
	private static IInstance currentInstance;
	
		/**
	 * Sets the current instance. The current instance information is used be
	 * the InstanceView class.
	 * 
	 * @param instance -
	 *            The current instance.
	 */
	public static void setInstance(IInstance instance) {
		currentInstance = instance;
	}

	/**
	 * Retrieves the currently selected instance.
	 * 
	 * @return The current instance.
	 */
	public static IInstance getCurrentInstance() {
		return currentInstance;
	}
	
	public static IDeviceLauncher createLauncher(IInstance instance) throws SequoyahException {

		IDeviceHandler deviceHandler = null;
		IDeviceLauncher launcher = null;
		try {
			IExtension fromPlugin = PluginUtils.getExtension(
					DevicePlugin.DEVICE_TYPES_EXTENSION_POINT_ID, instance.getDeviceTypeId());
			deviceHandler = (IDeviceHandler) PluginUtils
					.getExecutableAttribute(fromPlugin, ELEMENT_DEVICE,
							ATTR_HANDLER);
			launcher = deviceHandler.createDeviceLauncher(instance);
		} catch (CoreException ce) {
			ExceptionHandler
					.showException(DeviceExceptionHandler
							.exception(DeviceExceptionStatus.CODE_ERROR_HANDLER_NOT_INSTANCED));
		}
		
		return launcher;
	}
	
	/**
	 * Creates a new instance.
	 * 
	 * @param name -
	 *            Instance name.
	 * @param deviceId -
	 *            The instance device id.
	 * @param status -
	 *            The instance status.
	 * @param properties -
	 *            The instance properties.
	 * @return The created instance.
	 * @throws SequoyahException
	 */
	public static IInstance createInstance(String name, String deviceId,
			String status, Properties properties) throws SequoyahException {

		IDeviceHandler deviceHandler = null;
		IInstance instance = null;
		try {
			IExtension fromPlugin = PluginUtils.getExtension(DevicePlugin.DEVICE_TYPES_EXTENSION_POINT_ID, deviceId);
			
			if (fromPlugin != null) {
				deviceHandler = (IDeviceHandler) PluginUtils
						.getExecutableAttribute(fromPlugin, ELEMENT_DEVICE,
								ATTR_HANDLER);
				// getExecutable(DevicePlugin.DEVICE_ID, deviceId);
				if (deviceHandler == null) {
					throw DeviceExceptionHandler.exception(DeviceExceptionStatus.CODE_ERROR_HANDLER_NOT_INSTANCED);
				}
			}
			else {
				deviceHandler = new UndefinedDeviceHandler();
			}
			
			instance = deviceHandler.createDeviceInstance(name + deviceId);
			instance.setDeviceTypeId(deviceId);
			instance.setName(name);
			
			if (fromPlugin != null) {
				((AbstractMobileInstance) instance).setStateMachineHandler(new StateMachineHandler(instance));
				instance.setStatus(status);
			}
			
			instance.setProperties((Properties) properties.clone());
			
		} catch (CoreException ce) {
			throw DeviceExceptionHandler.exception(DeviceExceptionStatus.CODE_ERROR_HANDLER_NOT_INSTANCED);
		}
		InstanceEventManager.getInstance().notifyListeners(new InstanceEvent(InstanceEventType.INSTANCE_CREATED, instance));
		return instance;
	}
	
	public static void deleteInstance(IInstance instance) {
        if (currentInstance == instance) {
            currentInstance = null;
        }
        InstanceRegistry registry = InstanceRegistry.getInstance();
        registry.removeInstance(instance); 
        DeviceXmlWriter.saveInstances();
        InstanceEventManager.getInstance().notifyListeners(new InstanceEvent(InstanceEventType.INSTANCE_DELETED, instance));
    }
	
	public static void deleteInstances(List<Object> instances) {
		
		for (Object instance: instances) {
			if (instance instanceof IInstance) {
				deleteInstance((IInstance)instance);
			}
		}
		
	}
	
	/**
	 * Creates an instance, sets it as the currently selected and adds it to the
	 * instance registry.
	 * 
	 * @param device -
	 *            The instance device id.
	 * @param projectBuilder
	 * @param monitor
	 */
	public static void createProject(IDeviceType device, IInstanceBuilder projectBuilder,
			IProgressMonitor monitor) throws SequoyahException {
		IInstance inst = createInstance(projectBuilder.getProjectName(), device.getId(), DevicePlugin.SEQUOYAH_STATUS_OFF, projectBuilder.getProperties());
		if (currentInstance == null) {
			currentInstance = inst;
		}
		InstanceRegistry registry = InstanceRegistry.getInstance();
		registry.addInstance(inst);
		DeviceXmlWriter.saveInstances();
	}


}
