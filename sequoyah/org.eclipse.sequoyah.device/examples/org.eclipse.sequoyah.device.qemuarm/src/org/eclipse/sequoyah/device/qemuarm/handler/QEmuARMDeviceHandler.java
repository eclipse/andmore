/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc and others.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Otavio Luiz Ferranti (Eldorado Research Institute) - [bug221733]Device handlers
 *                                         shall be able to create device instances.
 * Fabio Fantato (Instituto Eldorado) - [263188] - Create new examples to support tutorial presentation
 * Fabio Fantato (Instituto Eldorado) - [243494] Change the reference implementation to work on Galileo
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.qemuarm.handler;

import org.eclipse.sequoyah.device.framework.internal.model.MobileInstance;
import org.eclipse.sequoyah.device.framework.model.IDeviceLauncher;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IDeviceHandler;
import org.eclipse.sequoyah.device.qemuarm.QEmuARMLauncher;

public class QEmuARMDeviceHandler implements IDeviceHandler {
	public IInstance createDeviceInstance(String id) {
		return new MobileInstance(id);
	}
	public IDeviceLauncher createDeviceLauncher(IInstance instance) {
		return new QEmuARMLauncher(instance);
	}
}
