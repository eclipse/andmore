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
package org.eclipse.sequoyah.device.framework.ui.view;

import org.eclipse.sequoyah.device.framework.factory.DeviceTypeRegistry;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.IDeviceTypeDropSupport;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.ui.view.model.ViewerInstanceNode;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.widgets.TreeItem;

/**
 * @author rdbp36
 * 
 */
public class InstanceStatusCompositeDropAdapter implements DropTargetListener {

	private IInstance currentInstance = null;

	private IDeviceTypeDropSupport currentDropSupport = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.dnd.DropTargetListener#dragEnter(org.eclipse.swt.dnd.
	 * DropTargetEvent)
	 */
	public void dragEnter(DropTargetEvent event) {
		//do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.dnd.DropTargetListener#dragLeave(org.eclipse.swt.dnd.
	 * DropTargetEvent)
	 */
	public void dragLeave(DropTargetEvent event) {
		//do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.dnd.DropTargetListener#dragOperationChanged(org.eclipse
	 * .swt.dnd.DropTargetEvent)
	 */
	public void dragOperationChanged(DropTargetEvent event) {
		//do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.dnd.DropTargetListener#dragOver(org.eclipse.swt.dnd.
	 * DropTargetEvent)
	 */
	public void dragOver(DropTargetEvent event) {
        event.detail = DND.DROP_NONE;
		IInstance instance = getInstance(event);
		currentInstance = instance;
		if (instance != null) {
			IDeviceTypeDropSupport dropSupport = getDropSupport(instance);
			currentDropSupport = dropSupport;
			if (dropSupport != null
					&& dropSupport.canDrop(instance, event.currentDataType,
							event)) {
				event.detail = DND.DROP_MOVE;
			}
		}

	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.dnd.DropTargetListener#drop(org.eclipse.swt.dnd.
	 * DropTargetEvent)
	 */
	public void drop(DropTargetEvent event) {
		if (currentDropSupport != null && currentInstance != null) {
			currentDropSupport.drop(currentInstance, event.currentDataType,
					event);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.dnd.DropTargetListener#dropAccept(org.eclipse.swt.dnd
	 * .DropTargetEvent)
	 */
	public void dropAccept(DropTargetEvent event) {
		//do nothing
	}

	private IInstance getInstance(DropTargetEvent event) {
		IInstance instance = null;
		if (event.item != null && event.item instanceof TreeItem) {
			Object data = ((TreeItem) event.item).getData();
			ViewerInstanceNode node = null;
			if (data instanceof ViewerInstanceNode) {
				node = (ViewerInstanceNode) data;
				instance = node.getInstance();
			}
		}
		return instance;
	}

	private IDeviceTypeDropSupport getDropSupport(IInstance instance) {
		IDeviceTypeDropSupport dropSupport = null;
		String deviceTypeID = instance.getDeviceTypeId();
		IDeviceType deviceType = DeviceTypeRegistry.getInstance()
				.getDeviceTypeById(deviceTypeID);
		dropSupport = deviceType.getDropSupport();
		return dropSupport;
	}

}
