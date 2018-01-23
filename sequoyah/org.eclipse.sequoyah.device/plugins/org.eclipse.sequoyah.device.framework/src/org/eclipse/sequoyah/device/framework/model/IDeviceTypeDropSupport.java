/********************************************************************************
 * Copyright (c) 2011 Motorola Mobility, Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Marcelo Marzola Bossoni (Instituto de Pesquisas Eldorado)
 * 
 * Contributors:
 * <name> (<corp>) - [<bugid>] - <description>
 ********************************************************************************/
package org.eclipse.sequoyah.device.framework.model;

import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;

/**
 * @author rdbp36 This interface describes which methods should be implemented
 *         by device type providers in order to support drag and drop
 */
public interface IDeviceTypeDropSupport {
	public boolean canDrop(IInstance instance, TransferData data,
			DropTargetEvent event);

	public void drop(IInstance instance, TransferData data,
			DropTargetEvent event);
}
