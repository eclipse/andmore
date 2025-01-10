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
import org.eclipse.sequoyah.device.common.utilities.UtilitiesPlugin;
import org.eclipse.sequoyah.device.common.utilities.UtilitiesResources;


public abstract class ExceptionStatus {
	private IStatus status;
	public static final int CODE_ERROR_DEFAULT = 1;
	
	public static final IStatus ERROR_STATUS_DEFAULT = new Status(IStatus.ERROR,UtilitiesPlugin.PLUGIN_ID,CODE_ERROR_DEFAULT,UtilitiesResources.Sequoyah_Error,null);
			
	public ExceptionStatus(IStatus status) {
		this.status = status;
	}

	public IStatus getStatus(){
		return this.status;
	};
	
	protected void setStatus(IStatus status){
		this.status = status;
	}

	public ExceptionStatus(int severity, String pluginId, int code, String message, Throwable exception) {
		this.status = new Status(severity, pluginId, code, message, exception);
	}

	public ExceptionStatus(int code,String pluginId){
		createEmulatorStatus(code,pluginId,null,null);
	}

	public ExceptionStatus(int code,String pluginId,Throwable exception) {
		createEmulatorStatus(code,pluginId,null,exception);
	}
	
	public ExceptionStatus(int code,String pluginId,Object[] args,Throwable exception) {
		createEmulatorStatus(code,pluginId,args,exception);
	}

	public abstract void createEmulatorStatus(int code,String pluginId,Object[] args,Throwable exception);
	
	
}
