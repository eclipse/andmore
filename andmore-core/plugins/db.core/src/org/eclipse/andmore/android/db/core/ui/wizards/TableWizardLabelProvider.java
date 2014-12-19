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

import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.model.Field;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

public class TableWizardLabelProvider implements ITableLabelProvider {

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		String output = new String();
		if (element instanceof Field) {
			switch (columnIndex) {
			case 0:
				output = ((Field) element).getName();
				break;
			case 1:
				output = ((Field) element).getType().toString();
				break;
			case 2:
				output = ((Field) element).isPrimaryKey() ? "" : ((Field) element).getDefaultValue(); //$NON-NLS-1$
				break;
			case 3:
				output = ((Field) element).isPrimaryKey() ? DbCoreNLS.TableWizardLabelProvider_isPrimary_true
						: DbCoreNLS.TableWizardLabelProvider_isPrimary_False;
				break;

			}

		}
		return output;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		// Do nothing.
	}

	@Override
	public void dispose() {
		// Do nothing.
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// Do nothing.
	}

}
