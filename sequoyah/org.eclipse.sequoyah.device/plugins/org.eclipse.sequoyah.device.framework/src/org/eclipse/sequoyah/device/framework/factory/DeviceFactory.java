/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Fabio Fantato (Motorola) - bug#221733 - code revisited
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.device.framework.factory;

import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.sequoyah.device.common.utilities.PluginUtils;
import org.eclipse.sequoyah.device.common.utilities.exception.ExceptionHandler;
import org.eclipse.sequoyah.device.common.utilities.IPropertyConstants;
import org.eclipse.sequoyah.device.framework.DevicePlugin;
import org.eclipse.sequoyah.device.framework.exception.DeviceExceptionHandler;
import org.eclipse.sequoyah.device.framework.exception.DeviceExceptionStatus;
import org.eclipse.sequoyah.device.framework.internal.model.MobileDeviceType;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.handler.IDeviceHandler;


@Deprecated
public class DeviceFactory {
	private static final String ELEMENT_DEVICE = "device"; //$NON-NLS-1$
	private static final String ATR_ID = "id"; //$NON-NLS-1$
	private static final String ATR_LABEL = "label"; //$NON-NLS-1$
	private static final String ATR_NAME = "name"; //$NON-NLS-1$
	//private static final String ATR_ICON = "icon"; //$NON-NLS-1$
	//private static final String ATR_DESCRIPTION = "description"; //$NON-NLS-1$
	//private static final String ATR_PROVIDER = "provider"; //$NON-NLS-1$
	//private static final String ATR_COPYRIGHT = "copyright"; //$NON-NLS-1$
	//private static final String ATR_VERSION = "version"; //$NON-NLS-1$
	private static final String ATR_HANDLER = "handler"; //$NON-NLS-1$
	private static final Properties DEFAULT_PROPERTIES = new Properties();

    public DeviceFactory()
    {
		DEFAULT_PROPERTIES.setProperty(IPropertyConstants.HOST,
				IPropertyConstants.DEFAULT_HOST);
		DEFAULT_PROPERTIES.setProperty(IPropertyConstants.DISPLAY,
				IPropertyConstants.DEFAULT_DISPLAY);
		DEFAULT_PROPERTIES.setProperty(IPropertyConstants.PORT,
				IPropertyConstants.DEFAULT_PORT);
    }
    	
	public static IDeviceType createDevice(String deviceId) {

		IExtension fromPlugin =  PluginUtils.getExtension(DevicePlugin.DEVICE_TYPES_EXTENSION_POINT_ID, deviceId);

		String id = PluginUtils.getPluginAttribute(fromPlugin, ELEMENT_DEVICE, ATR_ID);
		String label = PluginUtils.getPluginAttribute(fromPlugin, ELEMENT_DEVICE, ATR_LABEL);
		
		IDeviceType device = new MobileDeviceType(id,label);
		device.setBundleName(PluginUtils.getPluginAttribute(fromPlugin, ELEMENT_DEVICE, ATR_NAME));
		device.setProperties(DEFAULT_PROPERTIES);
		try {
			device.setHandler((IDeviceHandler)PluginUtils.getExecutableAttribute(fromPlugin, ELEMENT_DEVICE, ATR_HANDLER));
		} catch (CoreException e) {
			ExceptionHandler.showException(DeviceExceptionHandler.exception(DeviceExceptionStatus.CODE_ERROR_HANDLER_NOT_INSTANCED));
		}
		return device;
	}
	
}
