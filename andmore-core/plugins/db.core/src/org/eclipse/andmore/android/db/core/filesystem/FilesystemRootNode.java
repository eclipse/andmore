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

package org.eclipse.andmore.android.db.core.filesystem;

import java.util.List;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.event.DatabaseModelEventManager;
import org.eclipse.andmore.android.db.core.event.DatabaseModelEvent.EVENT_TYPE;
import org.eclipse.andmore.android.db.core.exception.MotodevDbException;
import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.ui.AbstractTreeNode;
import org.eclipse.andmore.android.db.core.ui.DbNode;
import org.eclipse.andmore.android.db.core.ui.IDbMapperNode;
import org.eclipse.andmore.android.db.core.ui.IDbNode;
import org.eclipse.andmore.android.db.core.ui.ISaveStateTreeNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.core.ui.view.SaveStateManager;
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
 * Root node for the filesystem, that allows database mapping/unmapping.
 *
 */
public class FilesystemRootNode extends AbstractTreeNode implements IDbMapperNode, ISaveStateTreeNode {

	private static final String MEMENTO_KEY = "FileSystemNode"; //$NON-NLS-1$

	private static final String MEMENTO_PATH_PREFIX = "MappedPath_"; //$NON-NLS-1$

	private static final String ICON_PATH = "icons/filesystem.png"; //$NON-NLS-1$

	public FilesystemRootNode() {
	}

	public FilesystemRootNode(ITreeNode parent) {
		super(parent);
	}

	public FilesystemRootNode(String id, String name, ITreeNode parent) {
		super(id, name, parent);
	}

	public FilesystemRootNode(String id, String name, ITreeNode parent, ImageDescriptor icon) {
		super(id, name, parent, icon);
	}

	@Override
	public void refresh() {
		List<ITreeNode> children = getChildren();
		for (ITreeNode child : children) {
			if (child instanceof DbNode) {
				DbNode dbNode = (DbNode) child;
				if (!dbNode.existsDbFile()) {
					// Set error node! Filesystem path does not exist anymore.
					IStatus error = new Status(IStatus.ERROR, DbCoreActivator.PLUGIN_ID,
							DbCoreNLS.FilesystemRootNode_Mapped_Db_Not_Found);
					dbNode.setNodeStatus(error);
				} else {
					dbNode.setNodeStatus(Status.OK_STATUS);
				}
			}
		}
	}

	@Override
	public boolean isLeaf() {
		return getChildren().isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#getIcon()
	 */
	@Override
	public ImageDescriptor getIcon() {
		return AbstractUIPlugin.imageDescriptorFromPlugin(DbCoreActivator.PLUGIN_ID, ICON_PATH);
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
		}
		return canUnmap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.IDbMapperNode#map(org.eclipse.
	 * core.runtime.IPath)
	 */
	@Override
	public IStatus map(IPath dbFilePath) {
		IStatus status = new Status(IStatus.OK, DbCoreActivator.PLUGIN_ID, DbCoreNLS.FilesystemRootNode_Map_Successful);
		DbNode dbNode = null;
		try {
			dbNode = new DbNode(dbFilePath, this);
			putChild(dbNode);
			DatabaseModelEventManager.getInstance().fireEvent(dbNode, EVENT_TYPE.SELECT);
			saveState(SaveStateManager.getInstance().getPrefNode());
		} catch (MotodevDbException e) {
			status = new Status(IStatus.ERROR, DbCoreActivator.PLUGIN_ID, NLS.bind(
					DbCoreNLS.FilesystemRootNode_Error_Mapping_Description, dbFilePath));
		}
		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.IDbMapperNode#unmap(org.eclipse
	 * .andmore.android.db.core.ui.ITreeNode)
	 */
	@Override
	public IStatus unmap(ITreeNode dbNode) {
		IStatus status = new Status(IStatus.OK, DbCoreActivator.PLUGIN_ID,
				DbCoreNLS.FilesystemRootNode_Unmapping_Successful);
		if (dbNode instanceof IDbNode) {
			IDbNode node = (IDbNode) dbNode;
			if (node.isConnected()) {
				// node connected => disconnect
				status = node.disconnect();
			}
			// node disconnected => remove the node from the tree
			removeChild(node);
			saveState(SaveStateManager.getInstance().getPrefNode());
		}
		return status;
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
		MultiStatus operationsStatus = null;
		if (dbNodeList != null) {
			boolean hasError = false;
			IStatus[] children = new IStatus[dbNodeList.size()];
			int i = 0;
			for (ITreeNode treeNode : dbNodeList) {
				IStatus operationStatus = unmap(treeNode);
				if (!operationStatus.isOK()) {
					hasError = true;
				}
				children[i] = operationStatus;
				i++;
			}
			int code = hasError ? IStatus.ERROR : IStatus.OK;
			String msg = hasError ? DbCoreNLS.FilesystemRootNode_UnmappingList_Error
					: DbCoreNLS.FilesystemRootNode_UnmappingList_Successful;
			operationsStatus = new MultiStatus(DbCoreActivator.PLUGIN_ID, code, children, msg, null);
			if (operationsStatus.isOK()) {
				saveState(SaveStateManager.getInstance().getPrefNode());
			}
		}
		return operationsStatus;
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
		Preferences node = preferences.node(MEMENTO_KEY);

		int i = 1;
		List<ITreeNode> children = getChildren();
		for (ITreeNode child : children) {
			DbNode dbNode = (DbNode) child;
			node.put(MEMENTO_PATH_PREFIX + i, dbNode.getPath().toString());
			i++;
		}
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			StudioLogger.debug(this, "Unable to save preferences, flush failed.");
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
		try {
			if (preferences.nodeExists(MEMENTO_KEY)) {
				Preferences node = preferences.node(MEMENTO_KEY);
				String[] attributeKeys = node.keys();
				if (attributeKeys.length > 0) {
					for (String key : attributeKeys) {
						if (key.startsWith(MEMENTO_PATH_PREFIX)) {
							String mappedPath = node.get(key, null);
							if (mappedPath != null) {
								map(new Path(mappedPath));
							}
						}
					}
				}
			}
		} catch (BackingStoreException e) {
			StudioLogger.debug(this, "Unable to restore preferences.");
		}
	}
}
