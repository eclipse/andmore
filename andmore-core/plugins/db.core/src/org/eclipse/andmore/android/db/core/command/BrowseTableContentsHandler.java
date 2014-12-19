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
package org.eclipse.andmore.android.db.core.command;

import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.ui.ITableNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.core.ui.TableNode;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

/**
 * This class implements the command to browse a table content using an instance
 * of {@link TableNode} object.
 */
public class BrowseTableContentsHandler extends AbstractHandler implements IHandler {

	private ITableNode tableNode = null;

	public BrowseTableContentsHandler() {
	}

	public BrowseTableContentsHandler(ITableNode tableNode) {
		this.tableNode = tableNode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.
	 * ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		if (tableNode == null) {
			tableNode = getSelectedItem();
		}

		if (tableNode != null) {
			tableNode.browseTableContents();
			tableNode = null; // clear selected node to force getSelectedItem to
								// be called when calling via toolbar
		}

		return null;
	}

	private ITableNode getSelectedItem() {
		ITableNode selectedNode = null;
		ITreeNode selectedItem = DbCoreActivator.getMOTODEVDatabaseExplorerView().getSelectedItemOnTree();

		if (selectedItem instanceof ITableNode) {
			selectedNode = (ITableNode) selectedItem;
		}

		return selectedNode;
	}

}
