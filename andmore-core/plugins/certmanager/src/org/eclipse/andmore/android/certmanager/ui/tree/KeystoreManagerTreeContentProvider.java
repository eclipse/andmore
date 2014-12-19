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
package org.eclipse.andmore.android.certmanager.ui.tree;

import java.util.List;

import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

public class KeystoreManagerTreeContentProvider implements ILazyTreeContentProvider {

	private final TreeViewer treeViewer;

	public KeystoreManagerTreeContentProvider(TreeViewer viewer) {
		this.treeViewer = viewer;
	}

	@Override
	public void dispose() {
		// Nothing
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// Nothing
	}

	@Override
	public void updateElement(Object parent, int index) {
		if (parent instanceof ITreeNode) {
			ITreeNode parentNode = (ITreeNode) parent;
			ITreeNode child = null;
			try {
				List<ITreeNode> children = parentNode.getChildren();
				if (!children.isEmpty()) {
					child = children.get(index);
				}
			} catch (Exception e) {
				child = null;
			}

			if (child != null) {
				treeViewer.replace(parent, index, child);
				try {
					if (child.getChildren().isEmpty()) {
						treeViewer.setHasChildren(child, !child.isLeaf());
					} else {
						treeViewer.setChildCount(child, child.getChildren().size());
					}
				} catch (KeyStoreManagerException e) {
					StudioLogger.error("Error while accessing keystore manager. " + e.getMessage());
				}
			}
		}

	}

	@Override
	public void updateChildCount(Object element, int currentChildCount) {
		if (element instanceof ITreeNode) {
			ITreeNode treeNode = (ITreeNode) element;
			int childCount = 0;
			try {
				treeNode.refresh();
				List<ITreeNode> children = treeNode.getChildren();
				if (!children.isEmpty()) {
					childCount = children.size();
				}
			} catch (KeyStoreManagerException e) {
				StudioLogger.error(e.getMessage());
			}

			if (childCount != currentChildCount) {
				treeViewer.setChildCount(element, childCount);
			}
		}
	}

	@Override
	public ITreeNode getParent(Object element) {
		if (element instanceof ITreeNode) {
			ITreeNode treeNode = (ITreeNode) element;
			return treeNode.getParent();
		}
		return null;
	}

}
