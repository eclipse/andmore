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
 * Otavio Luiz Ferranti (Eldorado Research Institute) - bug#221733 - Code cleanup.
 * Fabio Rigo (Eldorado) - [245111] Disable the "Delete" option in popup if the instance is not prepared for deletion
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [246082] - Complement bug #245111 by allowing disable of "Properties" option as well
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Julia Martinez Perdigueiro (Eldorado) - [329548] Adding default service id retrieval from extension point for double click support behavior
 ********************************************************************************/
package org.eclipse.sequoyah.device.framework.status;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.common.utilities.PluginUtils;
import org.eclipse.sequoyah.device.common.utilities.exception.ExceptionHandler;
import org.eclipse.sequoyah.device.framework.DevicePlugin;
import org.eclipse.sequoyah.device.framework.exception.DeviceExceptionHandler;
import org.eclipse.sequoyah.device.framework.exception.DeviceExceptionStatus;


public class StatusFactory {
	private static final String ELEMENT_STATUS = "status"; //$NON-NLS-1$
	private static final String ATR_ID = "id"; //$NON-NLS-1$
	private static final String ATR_NAME = "name"; //$NON-NLS-1$
	private static final String ATR_IMAGE = "image"; //$NON-NLS-1$
	private static final String ATR_CANDELETE = "canDeleteInstance"; //$NON-NLS-1$
	private static final String ATR_CANEDITPROPERTIES = "canEditProperties"; //$NON-NLS-1$
	private static final String ATR_DEFAULTSERVICEID = "defaultServiceId"; //$NON-NLS-1$
	
	@SuppressWarnings("deprecation")
	public static IStatus createStatus(String statusId) {
		IExtension fromPlugin =  PluginUtils.getExtension(DevicePlugin.STATUS_ID, statusId);
		String id = PluginUtils.getPluginAttribute(fromPlugin, ELEMENT_STATUS, ATR_ID);
		String name = PluginUtils.getPluginAttribute(fromPlugin, ELEMENT_STATUS, ATR_NAME);
		boolean canDelete = Boolean.parseBoolean(PluginUtils.getPluginAttribute(fromPlugin, ELEMENT_STATUS, ATR_CANDELETE));
		boolean canEditProperties = Boolean.parseBoolean(PluginUtils.getPluginAttribute(fromPlugin, ELEMENT_STATUS, ATR_CANEDITPROPERTIES));
		String defaultServiceId = PluginUtils.getPluginAttribute(fromPlugin, ELEMENT_STATUS, ATR_DEFAULTSERVICEID);
		IStatus status = new MobileStatus(id,name);
		status.setCanDeleteInstance(canDelete);
		status.setCanEditProperties(canEditProperties);
		status.setDefaultServiceId(defaultServiceId);
		String imageName = PluginUtils.getPluginAttribute(fromPlugin, ELEMENT_STATUS, ATR_IMAGE);		
		/*
		ImageDescriptor image = null;
			try {
			image = BasePlugin.getPluginImage(fromPlugin.getDeclaringPluginDescriptor().getPlugin().getBundle(), imageName);
		} catch (Throwable t) {
			ExceptionHandler.showException(DeviceExceptionHandler.exception(DeviceExceptionStatus.CODE_ERROR_HANDLER_NOT_INSTANCED));
		}
		status.setImage(image);
		*/
		return status;
	}
	
	public static IStatus createStatus(IExtension fromPlugin) {
		String id = PluginUtils.getPluginAttribute(fromPlugin, ELEMENT_STATUS, ATR_ID);
		return createStatus(id);		
	}
	
}
