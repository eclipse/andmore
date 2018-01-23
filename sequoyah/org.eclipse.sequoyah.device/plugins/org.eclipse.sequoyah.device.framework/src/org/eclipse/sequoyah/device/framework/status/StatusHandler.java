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
package org.eclipse.sequoyah.device.framework.status;

import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.model.IInstance;

public abstract class StatusHandler implements IStatusHandler {

	public String run(IStatusTransition transition,IInstance instance) {
		String status = null;
		try {
			execute(transition,instance);
			status = transition.getEndId();			
		} catch (SequoyahException te) {
			status = transition.getHaltId();	
		}
		return status;
	}

	public abstract void execute(IStatusTransition transition,IInstance instance) throws SequoyahException; 

}
