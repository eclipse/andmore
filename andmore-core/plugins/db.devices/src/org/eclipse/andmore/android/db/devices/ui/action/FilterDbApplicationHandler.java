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
package org.eclipse.andmore.android.db.devices.ui.action;

import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.devices.model.IDeviceNode;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler responsible for toggling filter apps with dbs.
 */
public class FilterDbApplicationHandler extends AbstractHandler implements IHandler {

	private ITreeNode node;

	public FilterDbApplicationHandler() {
		// do nothing
	}

	public FilterDbApplicationHandler(ITreeNode node) {
		this.node = node;
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
		if (node == null) {
			node = DbCoreActivator.getMOTODEVDatabaseExplorerView().getSelectedItemOnTree();
		}
		if (node instanceof IDeviceNode) {
			boolean oldValue = HandlerUtil.toggleCommandState(event.getCommand());
			IDeviceNode deviceNode = (IDeviceNode) node;
			deviceNode.setFilterAppWithDb(!oldValue);
			deviceNode.refreshAsync();
		}
		node = null;

		return null;
	}

}
