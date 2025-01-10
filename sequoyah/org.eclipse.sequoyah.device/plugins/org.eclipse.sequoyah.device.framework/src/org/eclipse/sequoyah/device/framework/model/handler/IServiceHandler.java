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
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Pablo Leite (Eldorado) - [329548] Allow multiple instances selection on Device Manager View 
 ********************************************************************************/
package org.eclipse.sequoyah.device.framework.model.handler;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.IService;

public interface IServiceHandler extends Cloneable {
	public void setParent(IServiceHandler handler);
	public IServiceHandler getParent();
	public void run(IInstance instance) throws SequoyahException;
	public void updatingService(IInstance instance);	
	public void setService(IService service);		
	public IService getService();
	public Object clone();
	public IServiceHandler newInstance();
    IStatus singleInit(List<IInstance> instances);
}

