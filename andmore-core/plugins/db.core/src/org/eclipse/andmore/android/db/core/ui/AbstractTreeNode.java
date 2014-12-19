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
package org.eclipse.andmore.android.db.core.ui;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.event.DatabaseModelEvent;
import org.eclipse.andmore.android.db.core.event.DatabaseModelEventManager;
import org.eclipse.andmore.android.db.core.event.DatabaseModelEvent.EVENT_TYPE;
import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.ui.view.SaveStateManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * Node abstraction to be contributed/implemented through extension point
 * org.eclipse.andmore.android.db.core.dbRootNode.
 */
public abstract class AbstractTreeNode implements ITreeNode {
	/**
	 * Property value used to check if the node has an error status.
	 */
	public static final String PROP_VALUE_NODE_STATUS_ERROR = "org.eclipse.andmore.android.db.core.nodeStatusError"; //$NON-NLS-1$

	/**
	 * Property name used to test the status of the node.
	 */
	public static final String PROP_NAME_NODE_STATUS = "org.eclipse.andmore.android.db.core.nodeStatus"; //$NON-NLS-1$

	private static final String DEFAULT_ICON_PATH = "icons/obj16/plate16.png"; //$NON-NLS-1$

	/*
	 * id, name, and icon will come from extension point
	 * org.eclipse.andmore.android.db.core.dbRootNode
	 */
	private String id;

	private String name;

	private ITreeNode parent;

	private ImageDescriptor icon = DbCoreActivator.imageDescriptorFromPlugin(DbCoreActivator.PLUGIN_ID,
			DEFAULT_ICON_PATH);

	private IStatus nodeStatus = Status.OK_STATUS;

	/**
	 * We need to guarantee the order of the inserted items to use
	 * {@link ILazyTreeContentProvider} We need to guarantee the change
	 * operations are thread-safe by using synchronized list
	 */
	private final List<ITreeNode> children = new CopyOnWriteArrayList<ITreeNode>();

	private volatile boolean isLoading = false;

	private String tooltip;

	/**
	 * Default constructor
	 * 
	 * Warning: If the node comes is declared through extension point
	 * org.eclipse.andmore.android.db.core.dbRootNode this constructor is
	 * mandatory to exist and be the unique to be used.
	 */
	public AbstractTreeNode() {
		this(null, null, null);
	}

	public AbstractTreeNode(ITreeNode parent) {
		this(null, null, parent);
	}

	public AbstractTreeNode(String id, String name, ITreeNode parent) {
		this(id, name, parent, null);
	}

	public AbstractTreeNode(String id, String name, ITreeNode parent, ImageDescriptor icon) {
		this.id = id;
		this.name = name;
		this.parent = parent;
		this.icon = icon;

		if (this instanceof ISaveStateTreeNode) {
			ISaveStateTreeNode saveStateTreeNode = (ISaveStateTreeNode) this;
			SaveStateManager saveStateManager = SaveStateManager.getInstance();
			saveStateManager.registerSaveStateNode(saveStateTreeNode);
			IEclipsePreferences prefNode = saveStateManager.getPrefNode();
			if (prefNode != null) {
				saveStateTreeNode.restoreState(prefNode);
			}
		}
	}

	@Override
	public final void refreshAsync() {
		refreshAsync(true);
	}

	@Override
	public final void refreshAsync(final boolean canRefreshYesResponse) {
		if (!isLoading()) {
			AbstractLoadingNodeJob loadingJob = new AbstractLoadingNodeJob(NLS.bind(
					DbCoreNLS.AbstractTreeNode_Loading_Job_Name, getName()), this) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					refresh(canRefreshYesResponse);
					return Status.OK_STATUS;
				}
			};
			loadingJob.schedule();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITreeNode#refresh()
	 */
	@Override
	public abstract void refresh();

	/**
	 * @param canRefreshYesResponse
	 */
	public void refresh(boolean canRefreshYesResponse) {
		refresh();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITreeNode#getParent()
	 */
	@Override
	public ITreeNode getParent() {
		return parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.ITreeNode#setParent(org.eclipse
	 * .andmore.android.db.core.ui.ITreeNode)
	 */
	@Override
	public void setParent(ITreeNode parent) {
		this.parent = parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITreeNode#getChildren()
	 */
	@Override
	public List<ITreeNode> getChildren() {
		return children;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITreeNode#clear()
	 */
	@Override
	public void clear() {
		DatabaseModelEventManager.getInstance().fireEvent(this, EVENT_TYPE.CLEAR);
		for (ITreeNode child : getChildren()) {
			child.cleanUp();
			child.clear();
		}
		children.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITreeNode#getChild(int)
	 */
	@Override
	public ITreeNode getChild(int index) {
		return children.size() > index ? children.get(index) : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.ITreeNode#getChildById(String)
	 */
	@Override
	public ITreeNode getChildById(String id) {
		ITreeNode foundChild = null;
		for (ITreeNode node : children) {
			if ((node != null) && (node.getId() != null) && node.getId().equals(id)) {
				foundChild = node;
				break;
			}
		}
		return foundChild;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.ITreeNode#getFilteredChildren(
	 * java.lang.String)
	 */
	@Override
	public List<ITreeNode> getFilteredChildren(String regex) {
		List<ITreeNode> filteredChildren = new ArrayList<ITreeNode>();
		if (regex == null) {
			filteredChildren = getChildren();
		} else {
			for (ITreeNode child : children) {
				if ((regex != null) && child.getId().matches(regex)) {
					filteredChildren.add(child);
				}
			}
		}
		return filteredChildren;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.ITreeNode#putChild(java.lang.String
	 * , org.eclipse.andmore.android.db.core.ui.AbstractTreeNode)
	 */
	@Override
	public void putChild(ITreeNode treeNode) {
		treeNode.setParent(this);
		// node does not exist yet as a child => add it
		children.add(treeNode);
		DatabaseModelEventManager.getInstance().fireEvent(treeNode, DatabaseModelEvent.EVENT_TYPE.ADD);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.ITreeNode#putChildren(java.util
	 * .List)
	 */
	@Override
	public void putChildren(List<ITreeNode> childrenList) {
		children.addAll(childrenList);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.ITreeNode#removeChild(ITreeNode)
	 */
	@Override
	public void removeChild(ITreeNode node) {
		node.cleanUp();
		children.remove(node);
		DatabaseModelEventManager.getInstance().fireEvent(node, DatabaseModelEvent.EVENT_TYPE.REMOVE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITreeNode#isLoading()
	 */
	@Override
	public boolean isLoading() {
		return isLoading;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITreeNode#setLoading(boolean)
	 */
	@Override
	public void setLoading(boolean isLoading) {
		this.isLoading = isLoading;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITreeNode#getId()
	 */
	@Override
	public String getId() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.ITreeNode#setId(java.lang.String)
	 */
	@Override
	public void setId(String id) {
		this.id = id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITreeNode#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.ITreeNode#setName(java.lang.String
	 * )
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITreeNode#getIcon()
	 */
	@Override
	public ImageDescriptor getIcon() {
		return icon;
	}

	/**
	 * Retrieves an icon image descriptor from other Eclipse bundles (JDT,
	 * Datatools, ADT, etc).
	 * 
	 * @param bundleId
	 *            The id of the bundle where the icon should be retrieved from
	 * @param iconPath
	 *            The path of the icon inside the bundle
	 * 
	 * @return The image descriptor for the desired icon, or the default icon
	 *         from the db code plugin in case the process of retrieving the
	 *         desired icon fails
	 */
	protected final ImageDescriptor getSpecificIcon(String bundleId, String iconPath) {
		ImageDescriptor imgDesc = null;

		try {
			Bundle bundle = Platform.getBundle(bundleId);
			URL path = bundle.getEntry("/"); //$NON-NLS-1$
			URL fullPathString = new URL(path, iconPath);
			imgDesc = ImageDescriptor.createFromURL(fullPathString);
		} catch (Exception e) {
			StudioLogger.error(getClass(), "Error retrieving icon for tree node", e); //$NON-NLS-1$
		}
		if (imgDesc == null) {
			imgDesc = getIcon();
		}

		return imgDesc;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.ITreeNode#setIcon(org.eclipse.
	 * jface.resource.ImageDescriptor)
	 */
	@Override
	public void setIcon(ImageDescriptor icon) {
		this.icon = icon;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITreeNode#canRefresh()
	 */
	@Override
	public IStatus canRefresh() {
		return Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITreeNode#isLeaf()
	 */
	@Override
	public abstract boolean isLeaf();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "AbstractTreeNode [id=" + id + ", name=" + name + ", parent=" + parent + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITreeNode#clean()
	 */
	@Override
	public void cleanUp() {
		for (ITreeNode node : children) {
			node.cleanUp();
		}
		if (this instanceof ISaveStateTreeNode) {
			SaveStateManager.getInstance().unregisterSaveStateNode((ISaveStateTreeNode) this);
		}
	}

	@Override
	public void setNodeStatus(IStatus status) {
		nodeStatus = status;
		// update node label/icon/decoration given that its status has changed
		DatabaseModelEventManager.getInstance().fireEvent(this, EVENT_TYPE.UPDATE);
	}

	@Override
	public IStatus getNodeStatus() {
		return nodeStatus;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionFilter#testAttribute(java.lang.Object,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public boolean testAttribute(Object target, String name, String value) {
		boolean result = false;

		if (name.equals(PROP_NAME_NODE_STATUS)) {
			if (value.equals(PROP_VALUE_NODE_STATUS_ERROR)) {
				result = !getNodeStatus().isOK();
			}
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.ITreeNode#setTooltip(java.lang
	 * .String)
	 */
	@Override
	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.ITreeNode#getTooltip()
	 */
	@Override
	public String getTooltip() {
		return tooltip;
	}

}
