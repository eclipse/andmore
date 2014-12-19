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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.DDMSUtils;
import org.eclipse.andmore.android.db.core.CanRefreshStatus;
import org.eclipse.andmore.android.db.core.exception.MotodevDbException;
import org.eclipse.andmore.android.db.core.model.TableModel;
import org.eclipse.andmore.android.db.core.ui.AbstractTreeNode;
import org.eclipse.andmore.android.db.core.ui.IDbNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.core.ui.action.IDbCreatorNode;
import org.eclipse.andmore.android.db.devices.DbDevicesPlugin;
import org.eclipse.andmore.android.db.devices.i18n.DbDevicesNLS;
import org.eclipse.andmore.android.db.devices.utils.DeviceDbUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * This class represents a tree node for an application installed on a device.
 */
public class ApplicationNode extends AbstractTreeNode implements IDbCreatorNode {

	private String appName;

	private String serialNumber;

	@SuppressWarnings("unused")
	private ApplicationNode() {
		// Forcing user to use a proper constructor
	}

	/**
	 * Creates a new Application node for the app
	 * 
	 * @param appName
	 *            the application name
	 * @param parent
	 *            this node parent
	 */
	public ApplicationNode(String appName, DeviceNode parent) {
		super(parent);
		serialNumber = parent.getSerialNumber();
		this.appName = appName;
		setId(appName);
		setName(appName);
		ImageDescriptor icon = AbstractUIPlugin.imageDescriptorFromPlugin("org.eclipe.andmore", //$NON-NLS-1$
				"icons/android.png"); //$NON-NLS-1$
		setIcon(icon);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#canRefresh()
	 */
	@Override
	public IStatus canRefresh() {
		IStatus status = null;
		boolean hasConnectedDb = false;

		// Search for connected dbs
		for (ITreeNode treeNode : getChildren()) {
			if (treeNode instanceof DeviceDbNode) {
				DeviceDbNode dbNode = (DeviceDbNode) treeNode;
				hasConnectedDb = dbNode.isConnected();
				break;
			}
		}

		if (hasConnectedDb) {
			status = new CanRefreshStatus(CanRefreshStatus.ASK_USER, DbDevicesPlugin.PLUGIN_ID, NLS.bind(
					DbDevicesNLS.ApplicationNode_ConnectedDbs_Refresh_Message, getName()));
		}

		return status != null ? status : Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.andkroid.db.core.ui.AbstractTreeNode#refresh()
	 */
	@Override
	public void refresh() {
		clear();
		try {
			List<String> applicationDatabases = DDMSUtils.getApplicationDatabases(serialNumber, appName);

			List<ITreeNode> dbNodes = new ArrayList<ITreeNode>(applicationDatabases.size());

			for (String dbLocation : applicationDatabases) {
				IPath dbPath = DeviceDbUtils.getRemoteDbPath(appName, dbLocation);
				DeviceDbNode deviceDbNode = new DeviceDbNode(dbPath, serialNumber, this);
				dbNodes.add(deviceDbNode);
			}

			putChildren(dbNodes);
			setNodeStatus(Status.OK_STATUS);
		} catch (IOException e) {
			setNodeStatus(new Status(IStatus.ERROR, DbDevicesPlugin.PLUGIN_ID, e.getMessage()));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.action.IDbCreatorNode#createDb
	 * (java.lang.String)
	 */
	@Override
	public IStatus createDb(String dbName) {
		return createDb(dbName, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.action.IDbCreatorNode#createDb
	 * (java.lang.String, java.util.List)
	 */
	@Override
	public IStatus createDb(String dbName, List<TableModel> tables) {
		IStatus status = null;

		IPath remoteDbPath = DeviceDbUtils.getRemoteDbPath(appName, dbName);
		try {
			DeviceDbNode dbNode = new DeviceDbNode(remoteDbPath, serialNumber, this, true);
			if (tables != null) {
				dbNode.createTables(tables);
			}
			putChild(dbNode);
		} catch (MotodevDbException e) {
			status = new Status(IStatus.ERROR, DbDevicesPlugin.PLUGIN_ID, NLS.bind(
					DbDevicesNLS.ApplicationNode_Could_Not_Create_Database, remoteDbPath.toString(), serialNumber));
		}

		return status != null ? status : Status.OK_STATUS;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.action.IDbCreatorNode#deleteDb
	 * (org.eclipse.andmore.android.db.core.ui.IDbNode)
	 */
	@Override
	public IStatus deleteDb(IDbNode dbNode) {
		IStatus status = dbNode.deleteDb();

		if (status.isOK()) {
			removeChild(dbNode);
		}

		return status;
	}

}
