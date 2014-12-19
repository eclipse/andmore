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
package org.eclipse.andmore.android.emulator.core.emulationui;

import static org.eclipse.andmore.android.common.log.StudioLogger.warn;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.andmore.android.emulator.core.devfrm.DeviceFrameworkManager;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IViewSite;

/**
 * DESCRIPTION: This class is the abstract parent of all emulation views content
 * providers
 *
 * RESPONSIBILITY: Provide common method implementation for the several
 * emulation views content providers
 *
 * COLABORATORS: None.
 *
 * USAGE: The class is used when a emulation view content provider is
 * instantiated
 */
public abstract class AbstractEmuCtProvider implements ITreeContentProvider {
	/**
	 * The id to be used when constructing a "sent to" node
	 */
	public static final String SENT_TO_EMULATOR_ID = "sent_to";

	/**
	 * The id to be used when constructing a "receive from" node
	 */
	public static final String RECEIVE_FROM_EMULATOR_ID = "received_from";

	/**
	 * The parent of the entire tree
	 */
	private static IViewSite treeParent;

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(Object)
	 */
	@Override
	public Object getParent(Object element) {

		Object parent = null;

		if (element instanceof EmuViewerNode) {
			EmuViewerNode nodeElement = (EmuViewerNode) element;

			if (nodeElement instanceof EmuViewerRootNode) {
				// The IViewSite object is the parent of the whole tree
				parent = treeParent;
			} else {
				parent = nodeElement.getParent();
			}
		} else {
			warn("Tried to get parent of an object that is not an emulation tree node");
		}

		return parent;
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(Object)
	 */
	@Override
	public boolean hasChildren(Object object) {

		boolean hasChildren = false;

		if (object instanceof EmuViewerNode) {
			EmuViewerNode nodeObject = (EmuViewerNode) object;

			// The node has children if its children collection is bigger than 0
			// in size
			hasChildren = (nodeObject.getChildren().size() > 0);

		} else {
			warn("Tried to test if an object that is not an emulation tree node has children");
		}

		return hasChildren;
	}

	/**
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(Object)
	 */
	@Override
	public Object[] getElements(Object parent) {

		Object[] elements;

		if (parent instanceof IViewSite) {

			if (treeParent == null) {
				// Sets the treeParent attribute to store which view site is the
				// parent of the
				// whole tree. This is done only once for each content provider
				// instance.
				// Each provider instance is supposed to be used with a single
				// view
				treeParent = (IViewSite) parent;
			}

			Collection<EmuViewerRootNode> emuNodeCollection = new HashSet<EmuViewerRootNode>();

			Set<String> hostSet = DeviceFrameworkManager.getInstance().getAllStartedInstancesHosts();

			// A root node will be added per active emulator at the tree viewer
			for (String host : hostSet) {
				EmuViewerRootNode node = new EmuViewerRootNode(host);
				emuNodeCollection.add(node);

				addChildrenToRootNode(node);
			}

			// Creating the array of elements (in this case, emulator root
			// nodes) to be
			// returned, when the parent is the view site itself
			Object[] emuNodeArray = emuNodeCollection.toArray(new Object[emuNodeCollection.size()]);
			elements = emuNodeArray;

		} else {
			// When elements different from the view site are provided, the
			// elements will be
			// their children
			elements = getChildren(parent);
		}

		return elements;
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(Object)
	 */
	@Override
	public Object[] getChildren(Object parent) {

		Set<EmuViewerNode> childrenCollection;
		Object[] returnArray;

		if (parent instanceof EmuViewerNode) {
			// Firstly, try to retrieve the parent's children by means of the
			// appropriate method
			EmuViewerNode parentNode = (EmuViewerNode) parent;

			childrenCollection = parentNode.getChildren();

			// If the provided element is an emulator root node, it is needed to
			// test if the
			// intermediate nodes were already created (they are not created in
			// the first request).
			// If they were not created, assure that when the content framework
			// requests, the
			// intermediate nodes will be found.
			//
			// This procedure guarantees that once an emulator is started, it
			// has the intermediate
			// nodes constructed even if no emulation is being performed.
			if (parentNode instanceof EmuViewerRootNode) {
				String host = ((EmuViewerRootNode) parentNode).getEmulatorIdentifier();
				for (EmuViewerNode child : childrenCollection) {
					if (child.getChildren().size() == 0) {

						addChildrenToLeafParentNode(child, host);
					}
				}
			}

			// Creating the array of elements to be returned
			returnArray = childrenCollection.toArray(new EmuViewerNode[childrenCollection.size()]);

		} else {
			warn("Tried to get children of an object that is not an emulation tree node");
			returnArray = new Object[0];
		}

		return returnArray;
	}

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		// Do nothing
	}

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(Viewer,
	 *      Object, Object)
	 */
	@Override
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		// Do nothing
	}

	/**
	 * Given a root node, adds children nodes to it
	 * 
	 * @param root
	 *            The root node that will receive the children nodes
	 */
	protected void addChildrenToRootNode(EmuViewerRootNode root) {

		root.addChild(new EmuViewerNode(root, RECEIVE_FROM_EMULATOR_ID));
		root.addChild(new EmuViewerNode(root, SENT_TO_EMULATOR_ID));
	}

	/**
	 * Given a node, adds children leaf nodes to it
	 * 
	 * @param leafParentNode
	 *            The node that will receive the children leaf nodes
	 * @param host
	 *            The identifier of the emulator that owns this sub-tree
	 */
	protected abstract void addChildrenToLeafParentNode(EmuViewerNode leafParentNode, String host);
}
