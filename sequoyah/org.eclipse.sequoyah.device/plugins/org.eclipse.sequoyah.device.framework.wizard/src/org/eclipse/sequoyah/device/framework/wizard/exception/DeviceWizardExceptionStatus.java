/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Mobility, Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Daniel Pastore (Eldorado) - [278436] New device wizard does not offer a way to set its window title
 * 
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.wizard.exception;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.sequoyah.device.common.utilities.exception.AbstractExceptionStatus;
import org.eclipse.sequoyah.device.common.utilities.exception.ExceptionMessage;
import org.eclipse.sequoyah.device.framework.ui.wizard.DeviceWizardResources;

public class DeviceWizardExceptionStatus extends AbstractExceptionStatus {
	public static final int CODE_ERROR_RESOURCE_NOT_AVAILABLE = 201;
	public static final int CODE_ERROR_HANDLER_NOT_INSTANCED = 202;
	

	public DeviceWizardExceptionStatus(IStatus status) {
		super(status);
	}
	
	public DeviceWizardExceptionStatus(int severity, String pluginId, int code, String message, Throwable exception) {
		super(severity, pluginId, code, message, exception);
	}

	public DeviceWizardExceptionStatus(int code,String pluginId){
		super(code,pluginId,null,null);
	}

	public DeviceWizardExceptionStatus(int code,String pluginId,Throwable exception) {
		super(code,pluginId,exception);
	}
	
	public DeviceWizardExceptionStatus(int code,String pluginId,Object[] args,Throwable exception) {
		super(code,pluginId,args,exception);
		
	}
	
	@Override
	public ExceptionMessage getEmulatorMessage(int code) {
		ExceptionMessage message = null;
		switch (code) {
			case CODE_ERROR_RESOURCE_NOT_AVAILABLE: message = new ExceptionMessage(IStatus.ERROR,DeviceWizardResources.SEQUOYAH_Resource_Not_Available);break;			
			case CODE_ERROR_HANDLER_NOT_INSTANCED: message = new ExceptionMessage(IStatus.ERROR,DeviceWizardResources.SEQUOYAH_Handler_Not_Instanced);break;
			default: message = new ExceptionMessage(IStatus.ERROR,DeviceWizardResources.SEQUOYAH_Error); break;		        
		}			
		return message;
	}

}
