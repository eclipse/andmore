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
package org.eclipse.andmore.android.certmanager.ui.tree;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.jface.viewers.ColumnLabelProvider;

public class LastBackupDateColumnLabelProvider extends ColumnLabelProvider {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ColumnLabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		if (element instanceof IKeyStore) {
			IKeyStore iKeyStore = (IKeyStore) element;

			if (iKeyStore.getLastBackupDate() != null) {
				SimpleDateFormat formatter = new SimpleDateFormat("MMM dd yyyy HH:mm:ss", Locale.getDefault());
				return formatter.format(iKeyStore.getLastBackupDate());
			}
		}
		return "";
	}
}
