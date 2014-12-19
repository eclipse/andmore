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

import java.io.File;

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.model.DbModel;
import org.eclipse.andmore.android.db.core.ui.IDbMapperNode;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class MapDatabaseHandler extends AbstractHandler implements IHandler {

	private IDbMapperNode dbMapperNode;

	public MapDatabaseHandler() {
	}

	public MapDatabaseHandler(IDbMapperNode node) {
		this.dbMapperNode = node;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (dbMapperNode != null) {
			IPath dbFilePath = null;

			Shell shell = Display.getCurrent().getActiveShell();

			FileDialog dialog = new FileDialog(shell);
			String[] filterExt = { "*.db", "*.*" //$NON-NLS-1$ //$NON-NLS-2$
			};
			dialog.setFilterExtensions(filterExt);

			String dbFilePathString = dialog.open();

			if (dbFilePathString != null) {
				File dbFile = new File(dbFilePathString);
				dbFilePath = new Path(dbFile.getAbsolutePath());

				if (dbFile.exists() && DbModel.isValidSQLiteDatabase(dbFile)) {
					dbMapperNode.map(dbFilePath);
				} else {
					// Notify db does not exist or it is invalid
					EclipseUtils.showErrorDialog(DbCoreNLS.MapDatabaseHandler_Title_Error,
							NLS.bind(DbCoreNLS.Invalid_Db_Error, dbFilePath));
				}
			}
		}
		return null;
	}

}
