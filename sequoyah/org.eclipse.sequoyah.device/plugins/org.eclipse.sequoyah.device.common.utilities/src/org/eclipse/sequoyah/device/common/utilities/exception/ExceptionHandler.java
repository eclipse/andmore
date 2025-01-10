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
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;

public class ExceptionHandler {
	

	public static String logException(SequoyahException exception) {
		IStatus status = exception.getStatus(); 
		int code = status.getCode();
		String plugin = status.getPlugin();
		String msg = status.getMessage();
		Throwable t = status.getException();
		String logMessage= msg+"(PLUGIN="+plugin+";ERROR="+String.valueOf(code)+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		switch(exception.getStatus().getSeverity()){
			case IStatus.INFO:
				BasePlugin.logInfo(logMessage);
				break;
			case IStatus.WARNING:
				BasePlugin.logWarning(logMessage);
				break;
			default:
				if (t!=null) {
					BasePlugin.logError(logMessage, t);
				} else {
					BasePlugin.logError(logMessage);
				}				
		}
		return logMessage;
	}
	
	public static void showException(SequoyahException exception) {

		//String logMessage = logException(exception);
		//Throwable t = exception.getStatus().getException();
		// UI print message
	}
	
}
