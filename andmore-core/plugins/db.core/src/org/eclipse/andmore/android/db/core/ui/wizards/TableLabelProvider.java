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
package org.eclipse.andmore.android.db.core.ui.wizards;

import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.model.TableModel;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class TableLabelProvider extends LabelProvider {
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		if (element instanceof TableModel) {
			ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin(DbCoreActivator.DATATOOLS_UI_PLUGIN_ID,
					DbCoreActivator.TABLE_ICON);
			Image resultImage = desc.createImage();
			return resultImage;
		} else {
			return super.getImage(element);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		if (element instanceof TreeNode) {
			TreeNode treeNode = (TreeNode) element;
			Object value = treeNode.getValue();
			if (value instanceof TableModel) {
				TableModel tableModel = (TableModel) value;
				return tableModel.getName();
			}
		}
		return super.getText(element);
	}
}
