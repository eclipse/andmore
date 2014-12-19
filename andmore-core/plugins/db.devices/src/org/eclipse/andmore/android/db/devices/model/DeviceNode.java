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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.DdmsRunnable;
import org.eclipse.andmore.android.StudioAndroidEventManager;
import org.eclipse.andmore.android.StudioAndroidEventManager.EventType;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.db.core.CanRefreshStatus;
import org.eclipse.andmore.android.db.core.ui.AbstractTreeNode;
import org.eclipse.andmore.android.db.core.ui.ISaveStateTreeNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.core.ui.view.SaveStateManager;
import org.eclipse.andmore.android.db.devices.DbDevicesPlugin;
import org.eclipse.andmore.android.db.devices.i18n.DbDevicesNLS;
import org.eclipse.andmore.android.db.devices.utils.DeviceDbUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * This class represents a tree node for a given Android Device.
 */
public class DeviceNode extends AbstractTreeNode implements IDeviceNode, ISaveStateTreeNode {
	private static final String MEMENTO_FILTER_TYPE = "filterAppsWithDb"; //$NON-NLS-1$

	private static final String MEMENTO_FILTER_KEY = "filterEnabled"; //$NON-NLS-1$

	private static final String ICON_PATH = "icons/obj16/device.png"; //$NON-NLS-1$

	private String serialNumber;

	private boolean filterAppsWithDb;

	private String deviceName;

	private final PackageChangedListener listener = new PackageChangedListener(this);

	@SuppressWarnings("unused")
	private DeviceNode() {
		// Forcing user to use a proper constructor
	}

	/**
	 * Creates a new Devicenode based on it's serial number.
	 * 
	 * @param serialNumber
	 *            the device's serial number.
	 * @param parent
	 *            this node parent.
	 */
	public DeviceNode(String serialNumber, ITreeNode parent) {
		super(serialNumber, DDMSFacade.getNameBySerialNumber(serialNumber), parent);
		this.serialNumber = serialNumber;
		this.deviceName = getName();
		ImageDescriptor icon = AbstractUIPlugin.imageDescriptorFromPlugin(DbDevicesPlugin.PLUGIN_ID, ICON_PATH);
		setIcon(icon);

		StudioAndroidEventManager.addEventListener(EventType.PACKAGE_INSTALLED, listener);
		StudioAndroidEventManager.addEventListener(EventType.PACKAGE_UNINSTALLED, listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#canRefresh()
	 */
	@Override
	public IStatus canRefresh() {
		List<IStatus> childrenStatus = new ArrayList<IStatus>(getChildren().size());
		String message = NLS.bind(DbDevicesNLS.DeviceNode_Cant_Refresh_Node, getName());
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
	@SuppressWarnings("unchecked")
	@Override
	public void refresh() {
		clear();

		IStatus status = null;

		Collection<String> loadedApps = null;
		Map<String, String> listInstalledPackages = null;
		Integer totalFiltered = 0;
		try {
			Object[] installedPackagesContainer = DeviceDbUtils.listInstalledPackages(serialNumber,
					mustFilterAppsWithDb());
			listInstalledPackages = (Map<String, String>) installedPackagesContainer[0];
			totalFiltered = (Integer) installedPackagesContainer[1];

		} catch (IOException e) {
			status = new Status(IStatus.ERROR, DbDevicesPlugin.PLUGIN_ID, NLS.bind(
					DbDevicesNLS.DeviceNode_CouldNotLoadInstalledApps, deviceName));
		}

		if (listInstalledPackages != null) {
			loadedApps = listInstalledPackages.keySet();
		}

		List<ITreeNode> childNodes = new ArrayList<ITreeNode>(loadedApps != null ? loadedApps.size() + 1 : 1);
		try {
			if (DDMSFacade.hasSDCard(serialNumber)) {
				ExtStorageNode extStorageNode = new ExtStorageNode(this);
				childNodes.add(extStorageNode);
			}
		} catch (IOException e1) {
			status = new Status(IStatus.ERROR, DbDevicesPlugin.PLUGIN_ID, NLS.bind(
					DbDevicesNLS.DeviceNode_CouldNotVerifySdCard, deviceName));
		}

		if (loadedApps != null) {
			for (String app : loadedApps) {
				if (JavaConventions.validatePackageName(app, JavaCore.VERSION_1_5, JavaCore.VERSION_1_5).isOK()) {
					ApplicationNode appNode = new ApplicationNode(app, DeviceNode.this);
					childNodes.add(appNode);
				}
			}

			if (!childNodes.isEmpty()) {
				putChildren(childNodes);
			}
		}

		updateName(totalFiltered);
		setNodeStatus(status != null ? status : Status.OK_STATUS);
	}

	/**
	 * @param totalFiltered
	 */
	private void updateName(Integer totalFiltered) {
		if (mustFilterAppsWithDb() && (totalFiltered > 0) && !getChildren().isEmpty()) {
			setName(deviceName + NLS.bind(DbDevicesNLS.DeviceNode_X_Apps_Filtered, totalFiltered));
		} else {
			DeviceNode.this.setName(deviceName);
		}
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
	 * @return the filterAppWithDb
	 */
	@Override
	public boolean mustFilterAppsWithDb() {
		return this.filterAppsWithDb;
	}

	/**
	 * @param filterAppWithDb
	 *            the filterAppWithDb to set
	 */
	@Override
	public void setFilterAppWithDb(boolean filterAppWithDb) {
		this.filterAppsWithDb = filterAppWithDb;
		saveState(SaveStateManager.getInstance().getPrefNode());
	}

	/**
	 * @return the serialNumber
	 */
	@Override
	public String getSerialNumber() {
		return serialNumber;
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
		String id = deviceName != null ? deviceName : getName();
		Preferences filterNode = preferences.node(MEMENTO_FILTER_TYPE);
		Preferences deviceNode = filterNode.node(id);
		deviceNode.putBoolean(MEMENTO_FILTER_KEY, mustFilterAppsWithDb());

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
		boolean filterDbApps = true;
		String deviceName = this.deviceName != null ? this.deviceName : getName();
		try {
			if (preferences.nodeExists(MEMENTO_FILTER_TYPE)) {
				Preferences filterNode = preferences.node(MEMENTO_FILTER_TYPE);
				if (filterNode.nodeExists(deviceName)) {
					Preferences deviceNode = filterNode.node(deviceName);
					filterDbApps = deviceNode.getBoolean(MEMENTO_FILTER_KEY, true);
				}
			}
		} catch (BackingStoreException e) {
			StudioLogger.error("Could not contact backing store: ", e.getMessage()); //$NON-NLS-1$
		}
		setFilterAppWithDb(filterDbApps);
	}

	/**
	 * Listener called when a package (application) is installed / uninstalled
	 */
	private static class PackageChangedListener implements DdmsRunnable {
		private final DeviceNode deviceNode;

		public PackageChangedListener(DeviceNode deviceNode) {
			this.deviceNode = deviceNode;
		}

		@Override
		public void run(final String serialNumber) {
			Thread thread = new Thread(new Runnable() {
				@Override
				@SuppressWarnings("unchecked")
				public void run() {
					IStatus status = null;

					// it is an expensive operation
					Map<String, String> listInstalledPackages = null;
					try {
						Object[] installedPackagesContainer = DeviceDbUtils.listInstalledPackages(serialNumber,
								deviceNode.mustFilterAppsWithDb());
						listInstalledPackages = (Map<String, String>) installedPackagesContainer[0];
						Integer totalFiltered = (Integer) installedPackagesContainer[1];

						List<ITreeNode> childNodes = deviceNode.getChildren();

						List<ITreeNode> toRemoveNodes = new ArrayList<ITreeNode>();
						Set<String> newApplications = new HashSet<String>();
						newApplications.addAll(listInstalledPackages.keySet());

						if (childNodes != null) {
							Iterator<ITreeNode> iterator = childNodes.iterator();
							while (iterator.hasNext()) {
								ITreeNode node = iterator.next();
								if (node instanceof ApplicationNode) {
									ApplicationNode appNode = (ApplicationNode) node;
									if (!listInstalledPackages.containsKey(appNode.getId())) {
										// app was removed => mark to remove
										// node from the tree
										toRemoveNodes.add(node);
									} else {
										// app remains installed => remove it
										// from newApplication set
										newApplications.remove(appNode.getId());
									}
								}
							}
							// now the items in newApplication set must be the
							// new applications => add nodes to each one of them
							for (String newApp : newApplications) {
								ApplicationNode appNode = new ApplicationNode(newApp, deviceNode);
								deviceNode.putChild(appNode);
							}
							for (ITreeNode node : toRemoveNodes) {
								// remove the nodes marked
								deviceNode.removeChild(node);
							}
							deviceNode.updateName(totalFiltered);
						}
					} catch (IOException e) {
						status = new Status(IStatus.ERROR, DbDevicesPlugin.PLUGIN_ID, NLS.bind(
								DbDevicesNLS.DeviceNode_CouldNotLoadInstalledApps, deviceNode.getName()));
					}

					deviceNode.setNodeStatus(status != null ? status : Status.OK_STATUS);
				}
			});
			thread.start();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#clean()
	 */
	@Override
	public void cleanUp() {
		super.cleanUp();
		StudioAndroidEventManager.removeEventListener(EventType.PACKAGE_INSTALLED, listener);
		StudioAndroidEventManager.removeEventListener(EventType.PACKAGE_UNINSTALLED, listener);
	}
}
