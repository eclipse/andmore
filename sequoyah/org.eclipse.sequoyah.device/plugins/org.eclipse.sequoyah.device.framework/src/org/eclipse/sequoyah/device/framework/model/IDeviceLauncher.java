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
 * Fabio Fantato (Instituto Eldorado) - [263188] - Create new examples to support tutorial presentation
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.model;



/**
 * Define a launcher for emulators
 *
 */
public interface IDeviceLauncher {
	public String getLocation() ;
	public String getFileId();
	public String getToolArguments() ;
	public String getWorkingDirectory();
	public IConnection getConnection();
	public int getPID();
	public void setPID(int pid);
}
