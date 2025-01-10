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

public class ExceptionMessage {
 private int severity;
 private String message;
 
 public ExceptionMessage(int severity,String message){
	 this.severity = severity;
	 this.message = message;
 }
 
 public int getSeverity() {
	 return this.severity;
 }
 
 public String getMessage() {
	 return this.message;
 }
	
}
