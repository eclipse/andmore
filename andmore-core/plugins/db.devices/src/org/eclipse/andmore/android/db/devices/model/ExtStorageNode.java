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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.db.core.CanRefreshStatus;
import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.event.DatabaseModelEventManager;
import org.eclipse.andmore.android.db.core.event.DatabaseModelEvent.EVENT_TYPE;
import org.eclipse.andmore.android.db.core.ui.AbstractTreeNode;
import org.eclipse.andmore.android.db.core.ui.IDbMapperNode;
import org.eclipse.andmore.android.db.core.ui.IDbNode;
import org.eclipse.andmore.android.db.core.ui.ISaveStateTreeNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.core.ui.view.SaveStateManager;
import org.eclipse.andmore.android.db.devices.DbDevicesPlugin;
import org.eclipse.andmore.android.db.devices.i18n.DbDevicesNLS;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * This node represents a external storage node, sd card on devices that have
 * it.
 */
public class ExtStorageNode extends AbstractTreeNode implements IDbDeviceMapperNode, ISaveStateTreeNode {

	private static final String MEMENTO_EXTERNAL_STORAGE = "ExternalStorageMapping"; //$NON-NLS-1$

	private static final String MEMENTO_KEY_PREFIX = "MappedPath_"; //$NON-NLS-1$

	private static final String ID_SUFFIX = "_EXT_STOR"; //$NON-NLS-1$

	private static final String ICON_PATH = "icons/obj16/card.png"; //$NON-NLS-1$

	private String serialNumber;

	private Set<IPath> dbNodes;

	private static final Map<String, Set<IPath>> dbNodesMap = new HashMap<String, Set<IPath>>();

	public ExtStorageNode(DeviceNode parent) {
		super(parent);
		setId(serialNumber + ID_SUFFIX);
		serialNumber = parent.getSerialNumber();
		setName(DbDevicesNLS.ExtStorageNode_Node_Name);
		ImageDescriptor icon = AbstractUIPlugin.imageDescriptorFromPlugin(DbDevicesPlugin.PLUGIN_ID, ICON_PATH);
		setIcon(icon);

		Set<IPath> dbNodes = new HashSet<IPath>(getDbNodes(serialNumber));

		for (IPath path : dbNodes) {
			map(path);
		}
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
			status = new CanRefreshStatus(CanRefreshStatus.ASK_USER, DbDevicesPlugin.PLUGIN_ID,
					DbDevicesNLS.ExternalStorageNode_ConnectedDbs_Refresh_Message);
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
		List<ITreeNode> children = getChildren();
		for (ITreeNode child : children) {
			DeviceDbNode dbNode = (DeviceDbNode) child;
			if (!dbNode.remoteFileExists()) {
				dbNode.clear();
				dbNode.setNodeStatus(new Status(IStatus.ERROR, DbDevicesPlugin.PLUGIN_ID,
						DbDevicesNLS.ExtStorageNode_RemoteFile_Not_Exist));
			} else {
				dbNode.disconnect();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#isLeaf()
	 */
	@Override
	public boolean isLeaf() {
		return getChildren().isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.IDbMapperNode#map()
	 */
	@Override
	public IStatus map(IPath remoteDbPath) {
		DeviceDbNode deviceDbNode = new DeviceDbNode(remoteDbPath, serialNumber, this);
		putChild(deviceDbNode);

		dbNodes.add(remoteDbPath);

		DatabaseModelEventManager.getInstance().fireEvent(deviceDbNode, EVENT_TYPE.SELECT);
		saveState(SaveStateManager.getInstance().getPrefNode());
		return Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionFilter#testAttribute(java.lang.Object,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public boolean testAttribute(Object target, String name, String value) {
		boolean canUnmap = false;
		if (name.equals(IDbMapperNode.UNMAP_ACTIONFILTER_NAME) && value.equals(IDbMapperNode.UNMAP_ACTIONFILTER_VALUE)) {
			if (!getChildren().isEmpty()) {
				canUnmap = true;
			}
		} else {
			super.testAttribute(target, name, value);
		}
		return canUnmap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.IDbMapperNode#unmap(org.eclipse
	 * .andmore.android.db.core.ui.ITreeNode)
	 */
	@Override
	public IStatus unmap(ITreeNode treeNode) {
		IStatus status = Status.OK_STATUS;
		if (treeNode instanceof IDbNode) {
			IDbNode dbNode = (IDbNode) treeNode;
			if (dbNode.isConnected()) {
				status = dbNode.disconnect();
			}
			if (status.isOK()) {
				removeChild(dbNode);
				dbNodes.remove(((DeviceDbNode) treeNode).getRemoteDbPath());

			} else {
				status = new Status(IStatus.ERROR, DbCoreActivator.PLUGIN_ID, NLS.bind(
						DbDevicesNLS.ExtStorageNode_Disconnect_Failed, dbNode.getName(), status.getMessage()));
			}
		}
		saveState(SaveStateManager.getInstance().getPrefNode());
		return status != null ? status : Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.IDbMapperNode#unmap(java.util.
	 * List)
	 */
	@Override
	public IStatus unmap(List<ITreeNode> dbNodeList) {
		MultiStatus status = new MultiStatus(DbDevicesPlugin.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$
		for (ITreeNode dbNode : dbNodeList) {
			status.add(unmap(dbNode));
		}
		saveState(SaveStateManager.getInstance().getPrefNode());
		return status != null ? status : Status.OK_STATUS;
	}

	private Set<IPath> getDbNodes(String serialNumber_) {
		dbNodes = dbNodesMap.get(serialNumber_);

		if (dbNodes == null) {
			dbNodes = new HashSet<IPath>();
			dbNodesMap.put(serialNumber_, dbNodes);

		}
		return dbNodes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.ISaveStateTreeNode#saveState(org
	 * .eclipse.core.runtime.preferences.IEclipsePreferences)
	 */
	@Override
	public void saveState(IEclipsePreferences preferences) {
		Preferences node = preferences.node(MEMENTO_EXTERNAL_STORAGE);
		Preferences serialNode = node.node(DDMSFacade.getNameBySerialNumber(serialNumber));

		int i = 1;
		List<ITreeNode> children = getChildren();
		for (ITreeNode child : children) {
			DeviceDbNode dbNode = (DeviceDbNode) child;
			serialNode.put(MEMENTO_KEY_PREFIX + i, dbNode.getRemoteDbPath().toString());
			i++;
		}
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			StudioLogger.error("Could not contact backing store: ", e.getMessage()); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.ISaveStateTreeNode#restoreState
	 * (org.eclipse.core.runtime.preferences.IEclipsePreferences)
	 */
	@Override
	public void restoreState(IEclipsePreferences preferences) {
		if (serialNumber == null) {
			serialNumber = ((DeviceNode) getParent()).getSerialNumber();
		}

		boolean firstTime = !dbNodesMap.containsKey(serialNumber);

		Set<IPath> dbNodes = getDbNodes(serialNumber);

		if (firstTime) {
			try {
				if (preferences.nodeExists(MEMENTO_EXTERNAL_STORAGE)) {
					Preferences node = preferences.node(MEMENTO_EXTERNAL_STORAGE);
					String deviceName = DDMSFacade.getNameBySerialNumber(serialNumber);
					if (node.nodeExists(deviceName)) {
						Preferences deviceNode = node.node(deviceName);
						String[] attributeKeys = deviceNode.keys();
						if (attributeKeys.length > 0) {
							for (String key : attributeKeys) {
								if (key.startsWith(MEMENTO_KEY_PREFIX)) {
									String mappedPath = deviceNode.get(key, null);
									if (mappedPath != null) {
										dbNodes.add(new Path(mappedPath));
									}
								}
							}
						}
					}
				}
			} catch (BackingStoreException e) {
				StudioLogger.error("Could not contact backing store: ", e.getMessage()); //$NON-NLS-1$
			}
		}
	}
}
