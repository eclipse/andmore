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
package org.eclipse.sequoyah.device.framework.model;

import org.eclipse.jface.resource.ImageDescriptor;

public interface IStatus {
	public enum eStatus {STARTED, STOPPED , REFRESHING,INACTIVE}; 	
	public eStatus getStatus();
	public void setStatus(eStatus status);
	public ImageDescriptor getImage();
	public String toString();
	IInstance getParent();	
	void setParent(IInstance instance);
	public Object clone();
}
