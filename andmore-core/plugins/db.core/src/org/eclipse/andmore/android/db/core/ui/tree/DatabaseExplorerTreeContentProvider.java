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
package org.eclipse.andmore.android.db.core.ui.tree;

import org.eclipse.andmore.android.db.core.ui.AbstractTreeNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.core.ui.LoadingNode;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content provider that uses {@link AbstractTreeNode} as the model
 */
public class DatabaseExplorerTreeContentProvider implements ILazyTreeContentProvider {

	private final TreeViewer treeViewer;

	public DatabaseExplorerTreeContentProvider(TreeViewer viewer) {
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
	public Object getParent(Object element) {
		if (element instanceof AbstractTreeNode) {
			ITreeNode treeNode = (ITreeNode) element;
			return treeNode.getParent();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ILazyTreeContentProvider#updateElement(java
	 * .lang.Object, int)
	 */
	@Override
	public void updateElement(Object parent, int index) {
		if (parent instanceof ITreeNode) {
			ITreeNode parentTreeNode = (ITreeNode) parent;
			ITreeNode child = null;
			if ((index == 0) && parentTreeNode.isLoading()) {
				child = new LoadingNode(parentTreeNode);
			} else {
				child = parentTreeNode.getChild(index);
			}
			if (child != null) {
				treeViewer.replace(parent, index, child);
				if (!child.isLeaf()) {
					treeViewer.setHasChildren(child, true);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ILazyTreeContentProvider#updateChildCount(java
	 * .lang.Object, int)
	 */
	@Override
	public void updateChildCount(Object element, int currentChildCount) {
		if (element instanceof AbstractTreeNode) {
			ITreeNode treeNode = (ITreeNode) element;
			treeNode.refreshAsync();
			int childCount = 0;
			if (treeNode.isLoading()) {
				childCount = 1;
			} else {
				childCount = treeNode.getChildren().size();
			}
			if (childCount != currentChildCount) {
				treeViewer.setChildCount(element, childCount);
			}
		}
	}
}
