/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.andmore.android.db.devices.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.DdmsRunnable;
import org.eclipse.andmore.android.StudioAndroidEventManager;
import org.eclipse.andmore.android.db.core.CanRefreshStatus;
import org.eclipse.andmore.android.db.core.ui.AbstractTreeNode;
import org.eclipse.andmore.android.db.core.ui.IRootNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.devices.DbDevicesPlugin;
import org.eclipse.andmore.android.db.devices.i18n.DbDevicesNLS;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * This class represents the devices root tree node and will be initialized by
 * the extension point. Is responsible to load the connected devices and listen
 * to device events(connected/disconnected) It listens to device events and
 * update its children accordingly.
 */
public class DevicesRootNode extends AbstractTreeNode implements IRootNode {
	private static final String ICON_PATH = "icons/obj16/devices.png"; //$NON-NLS-1$

	private ConnectDeviceListener connectedListener = new ConnectDeviceListener(this);

	private DisconnectDeviceListener disconnectedListener = new DisconnectDeviceListener(this);

	public DevicesRootNode() {
		StudioAndroidEventManager.asyncAddDeviceChangeListeners(connectedListener, disconnectedListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#canRefresh()
	 */
	@Override
	public IStatus canRefresh() {
		List<IStatus> childrenStatus = new ArrayList<IStatus>(getChildren().size());
		String message = NLS.bind(DbDevicesNLS.DevicesRootNode_Cant_Refresh_Node, getName());
		for (ITreeNode treeNode : getChildren()) {
			IStatus nodecanRefresh = treeNode.canRefresh();
			if (!nodecanRefresh.isOK()) {
				childrenStatus.add(nodecanRefresh);
				message = nodecanRefresh.getMessage();
				break;
			}
		}

		IStatus status = null;
		if (!childrenStatus.isEmpty()) {
			status = new CanRefreshStatus(CanRefreshStatus.ASK_USER, DbDevicesPlugin.PLUGIN_ID,
					childrenStatus.toArray(new IStatus[0]), message);
		}
		return status != null ? status : Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#refresh()
	 */
	@Override
	public void refresh() {
		clear();
		Collection<String> connectedSerialNumbers = DDMSFacade.getConnectedSerialNumbers();
		List<ITreeNode> deviceNodes = new ArrayList<ITreeNode>();
		for (String serialNumber : connectedSerialNumbers) {
			DeviceNode deviceNode = new DeviceNode(serialNumber, this);
			deviceNodes.add(deviceNode);
		}
		putChildren(deviceNodes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#isLeaf()
	 */
	@Override
	public boolean isLeaf() {
		return false;
	}

	/**
	 * Listener called when a device is disconnected
	 */
	private static class DisconnectDeviceListener implements DdmsRunnable {
		private DevicesRootNode devicesRootNode;

		public DisconnectDeviceListener(DevicesRootNode devicesRootNode) {
			this.devicesRootNode = devicesRootNode;
		}

		@Override
		public void run(String serialNumber) {
			ITreeNode treeNode = devicesRootNode.getChildById(serialNumber);
			if (treeNode instanceof DeviceNode) {
				DeviceNode deviceNode = (DeviceNode) treeNode;
				devicesRootNode.removeChild(deviceNode);
			}
		}
	};

	/**
	 * Listener called when a new device is connected
	 */
	private static class ConnectDeviceListener implements DdmsRunnable {
		private DevicesRootNode devicesRootNode;

		public ConnectDeviceListener(DevicesRootNode devicesRootNode) {
			this.devicesRootNode = devicesRootNode;
		}

		@Override
		public void run(String serialNumber) {
			DeviceNode deviceNode = new DeviceNode(serialNumber, devicesRootNode);
			devicesRootNode.putChild(deviceNode);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#getIcon()
	 */
	@Override
	public ImageDescriptor getIcon() {
		return AbstractUIPlugin.imageDescriptorFromPlugin(DbDevicesPlugin.PLUGIN_ID, ICON_PATH);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#clean()
	 */
	@Override
	public void cleanUp() {
		super.cleanUp();
		StudioAndroidEventManager.removeEventListener(StudioAndroidEventManager.EventType.DEVICE_CONNECTED,
				connectedListener);
		StudioAndroidEventManager.removeEventListener(StudioAndroidEventManager.EventType.DEVICE_DISCONNECTED,
				disconnectedListener);
	}
}
