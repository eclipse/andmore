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

import org.eclipse.andmore.android.db.core.ui.IDbMapperNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.devices.i18n.DbDevicesNLS;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class MapDeviceDatabaseHandler extends AbstractHandler implements IHandler {

	private IDbMapperNode dbMapperNode;

	public MapDeviceDatabaseHandler() {
		// do nothing
	}

	public MapDeviceDatabaseHandler(ITreeNode node) {
		if (node instanceof IDbMapperNode) {
			this.dbMapperNode = (IDbMapperNode) node;
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (dbMapperNode != null) {

			Shell shell = Display.getCurrent().getActiveShell();
			InputDialog dialog = new InputDialog(shell, DbDevicesNLS.UI_MapDatabaseAction_QueryDbPath_DialogTitle,
					DbDevicesNLS.UI_MapDatabaseAction_QueryDbPath_DialogMessage, "", //$NON-NLS-1$
					new IInputValidator() {

						@Override
						public String isValid(String newText) {
							String errorMsg = null;
							boolean isValid = false;

							if (newText.startsWith("/sdcard/") //$NON-NLS-1$
									&& newText.length() > "/sdcard/".length()) //$NON-NLS-1$
							{
								isValid = true;
							}
							if (!isValid) {
								if (newText.startsWith("/mnt/sdcard/") //$NON-NLS-1$
										&& newText.length() > "/mnt/sdcard/".length()) //$NON-NLS-1$
								{
									isValid = true;
								}
							}

							if (!isValid) {
								errorMsg = DbDevicesNLS.MapDatabaseAction_Error_WrongDatabasePlace;
							}

							return errorMsg;
						}
					});

			dialog.setBlockOnOpen(true);
			int result = dialog.open();

			String dbPath = null;

			if (result == Window.OK) {
				dbPath = dialog.getValue();
			}

			if (dbPath != null) {
				dbMapperNode.map(new Path(dbPath));
			}
		}
		return null;
	}
}
