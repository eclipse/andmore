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

package org.eclipse.sequoyah.device.common.utilities.exception;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.device.common.utilities.UtilitiesResources;


public abstract class AbstractExceptionStatus extends ExceptionStatus {
	
	public static final int CODE_ERROR_USER = 100;

	public AbstractExceptionStatus(IStatus status) {
		super(status);
	}
	
	public AbstractExceptionStatus(int severity, String pluginId, int code, String message, Throwable exception) {
		super(severity, pluginId, code, message, exception);
	}

	public AbstractExceptionStatus(int code,String pluginId){
		super(code,pluginId,null,null);
	}

	public AbstractExceptionStatus(int code,String pluginId,Throwable exception) {
		super(code,pluginId,exception);
	}
	
	public AbstractExceptionStatus(int code,String pluginId,Object[] args,Throwable exception) {
		super(code,pluginId,args,exception);
		
	}
	
	public abstract ExceptionMessage getEmulatorMessage(int code);
	
	public ExceptionMessage getDefaultEmulatorMessage(int code) {
		ExceptionMessage message = null;
		switch (code) {
			case CODE_ERROR_USER: message = new ExceptionMessage(IStatus.ERROR,UtilitiesResources.Sequoyah_Error);break;			
			default: message = new ExceptionMessage(IStatus.ERROR,UtilitiesResources.Sequoyah_Error); break;		        
		}			
		return message;
	}
	
	public void createEmulatorStatus(int code,String pluginId,Object[] args,Throwable exception){
		ExceptionMessage message = null;		
		if (code > CODE_ERROR_USER) {
			message = getEmulatorMessage(code);
		} else {
			message = getDefaultEmulatorMessage(code);
		}		
		String messageText = message.getMessage();
		if (args!=null){
			messageText = NLS.bind(message.getMessage(),args);
		}
		setStatus(new Status(message.getSeverity(), pluginId, code, messageText, exception));
	}

	
	
}
