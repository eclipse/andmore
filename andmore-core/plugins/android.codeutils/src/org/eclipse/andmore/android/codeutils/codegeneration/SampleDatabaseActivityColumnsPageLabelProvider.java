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

import org.eclipse.datatools.modelbase.sql.tables.Column;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Label provider for the tree viewer in the
 * {@link CreateSampleDatabaseActivityColumnsPage}.
 */
public class SampleDatabaseActivityColumnsPageLabelProvider extends LabelProvider {

	public static final String DATATOOLS_UI_PLUGIN_ID = "org.eclipse.datatools.connectivity.sqm.core.ui";

	private static final String COLUMN_ICON = "icons/columns.gif";

	private ImageDescriptor desc = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		Image resultImage = null;

		if (element instanceof Column) {
			if (desc == null) {
				desc = AbstractUIPlugin.imageDescriptorFromPlugin(DATATOOLS_UI_PLUGIN_ID, COLUMN_ICON);

			}

			resultImage = desc.createImage();
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
		String resultLabel = null;

		if (element instanceof Column) {
			resultLabel = ((Column) element).getName();
		}

		return resultLabel;
	}

}
