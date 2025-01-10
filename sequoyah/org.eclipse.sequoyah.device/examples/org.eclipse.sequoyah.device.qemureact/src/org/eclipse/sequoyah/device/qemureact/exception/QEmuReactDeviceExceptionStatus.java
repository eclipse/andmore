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
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.qemureact.exception;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.sequoyah.device.common.utilities.exception.AbstractExceptionStatus;
import org.eclipse.sequoyah.device.common.utilities.exception.ExceptionMessage;
import org.eclipse.sequoyah.device.qemureact.QEmuReactResources;

public class QEmuReactDeviceExceptionStatus extends AbstractExceptionStatus {
	public static final int CODE_ERROR_RESOURCE_NOT_AVAILABLE = 3101;
	

	public QEmuReactDeviceExceptionStatus(IStatus status) {
		super(status);
	}
	
	public QEmuReactDeviceExceptionStatus(int severity, String pluginId, int code, String message, Throwable exception) {
		super(severity, pluginId, code, message, exception);
	}

	public QEmuReactDeviceExceptionStatus(int code,String pluginId){
		super(code,pluginId,null,null);
	}

	public QEmuReactDeviceExceptionStatus(int code,String pluginId,Throwable exception) {
		super(code,pluginId,exception);
	}
	
	public QEmuReactDeviceExceptionStatus(int code,String pluginId,Object[] args,Throwable exception) {
		super(code,pluginId,args,exception);
		
	}
	
	@Override
	public ExceptionMessage getEmulatorMessage(int code) {
		ExceptionMessage message = null;
		switch (code) {
			case CODE_ERROR_RESOURCE_NOT_AVAILABLE: message = new ExceptionMessage(IStatus.ERROR,QEmuReactResources.SEQUOYAH_Resource_Not_Available);break;			
			default: message = new ExceptionMessage(IStatus.ERROR,QEmuReactResources.SEQUOYAH_Error); break;		        
		}			
		return message;
	}

}
