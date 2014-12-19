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

package org.eclipse.andmore.android.certmanager.command;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;

/**
 * This abstract handler adds convenient methods, like methods to retrieve the
 * current selection.
 */
public abstract class AbstractHandler2 extends AbstractHandler implements IHandler2 {

	/**
	 * Retrieves the list of selected nodes.
	 * */
	@SuppressWarnings("unchecked")
	protected List<ITreeNode> getSelection() {
		List<ITreeNode> selectedNodes = new ArrayList<ITreeNode>(1);
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService()
				.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection treeSelection = (IStructuredSelection) selection;
			List<Object> selectedElements = treeSelection.toList();

			for (Object selectedObject : selectedElements) {
				if (selectedObject instanceof ITreeNode) {
					selectedNodes.add((ITreeNode) selectedObject);
				}
			}
		}

		return selectedNodes;
	}

}
