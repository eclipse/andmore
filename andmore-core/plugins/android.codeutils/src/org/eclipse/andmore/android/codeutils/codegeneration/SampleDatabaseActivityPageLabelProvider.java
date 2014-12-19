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

package org.eclipse.andmore.android.codeutils.codegeneration;

import org.eclipse.datatools.modelbase.sql.schema.Database;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Label provider for the tree viewer in the
 * {@link CreateSampleDatabaseActivityPage}.
 */
public class SampleDatabaseActivityPageLabelProvider extends LabelProvider {

	public static final String DATATOOLS_UI_PLUGIN_ID = "org.eclipse.datatools.connectivity.sqm.core.ui";

	private static final String DATABASE_ICON = "icons/database.gif";

	private static final String TABLE_ICON = "icons/table.gif";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		Image resultImage = null;

		// Element type should be TreeObject
		if (element instanceof TreeNode) {
			// Get the value and check if it's a database or table
			Object value = ((TreeNode) element).getValue();
			if (value instanceof Database) {
				ImageDescriptor desc = AbstractUIPlugin
						.imageDescriptorFromPlugin(DATATOOLS_UI_PLUGIN_ID, DATABASE_ICON);
				resultImage = desc.createImage();
			} else if (value instanceof Table) {
				ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin(DATATOOLS_UI_PLUGIN_ID, TABLE_ICON);
				resultImage = desc.createImage();
			} else if (value instanceof org.eclipse.andmore.android.db.wizards.model.Table) {
				ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin(DATATOOLS_UI_PLUGIN_ID, TABLE_ICON);
				resultImage = desc.createImage();
			}
		}

		return resultImage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		String result = "";

		// Element type should be TreeObject
		if (element instanceof TreeNode) {
			// Get the value and check if it's a database or table
			Object value = ((TreeNode) element).getValue();
			if (value instanceof Database) {
				result = ((Database) value).getName();
			} else if (value instanceof Table) {
				result = ((Table) value).getName();
			} else if (value instanceof org.eclipse.andmore.android.db.wizards.model.Table) {
				result = ((org.eclipse.andmore.android.db.wizards.model.Table) value).getTableName();
			}
		}

		return result;
	}
}
