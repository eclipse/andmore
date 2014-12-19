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
package org.eclipse.andmore.android.certmanager.ui.model;

import org.eclipse.andmore.android.certmanager.event.KeyStoreModelEventManager;
import org.eclipse.andmore.android.certmanager.event.KeyStoreModelEvent.EventType;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.views.KeystoreManagerView;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Abstract node for {@link KeystoreManagerView}
 */
public abstract class AbstractTreeNode implements ITreeNode {
	private static final String UNSAVED_PASSWORD = CertificateManagerNLS.AbstractTreeNode_UnsavedPassword_Tooltip;

	private static final String SAVED_PASSWORD = CertificateManagerNLS.AbstractTreeNode_SavedPassword_Tooltip;

	private IStatus nodeStatus = Status.OK_STATUS;

	private ITreeNode parent;

	private String tooltip;

	@Override
	public String getTooltip() {
		if (tooltip != null) {
			return isPasswordSaved() ? SAVED_PASSWORD + "\n" + tooltip : UNSAVED_PASSWORD //$NON-NLS-1$
					+ "\n" + tooltip; //$NON-NLS-1$
		} else {
			return isPasswordSaved() ? SAVED_PASSWORD : UNSAVED_PASSWORD;
		}
	}

	/**
	 * @return the nodeStatus
	 */
	@Override
	public IStatus getNodeStatus() {
		return nodeStatus;
	}

	/**
	 * @param nodeStatus
	 *            the nodeStatus to set
	 */
	@Override
	public void setNodeStatus(IStatus nodeStatus) {
		this.nodeStatus = nodeStatus;
		KeyStoreModelEventManager.getInstance().fireEvent(this, EventType.UPDATE);
	}

	/**
	 * @param tooltip
	 *            the tooltip to set
	 */
	@Override
	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	/**
	 * @return the parent
	 */
	@Override
	public ITreeNode getParent() {
		return parent;
	}

	/**
	 * @param parent
	 *            the parent to set
	 */
	public void setParent(ITreeNode parent) {
		this.parent = parent;
	}

	@Override
	public void addChild(ITreeNode newChild) throws KeyStoreManagerException {
		// Default implementation won't do nothing
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		boolean result = false;
		if (name.equals(PROP_NAME_NODE_STATUS)) {
			if (value.equals(PROP_VALUE_NODE_STATUS_ERROR)) {
				result = !getNodeStatus().isOK(); // true if there is an error
				if (result) {
					setTooltip(getNodeStatus().getMessage());
				}
			} else if (value.equals(PROP_VALUE_NODE_STATUS_OK)) {
				result = getNodeStatus().isOK();
			}
		}
		return result;
	}

	/**
	 * For dummy nodes and root nodes it is false. Override for other nodes:
	 * {@link KeyStoreNode} and {@link EntryNode}
	 * 
	 * @return
	 */
	protected boolean isPasswordSaved() {
		return false;
	}
}
