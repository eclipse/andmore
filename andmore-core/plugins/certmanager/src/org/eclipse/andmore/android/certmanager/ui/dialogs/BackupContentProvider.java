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
package org.eclipse.andmore.android.certmanager.ui.dialogs;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class BackupContentProvider implements IStructuredContentProvider {

	@Override
	public void dispose() {
		// do nothing
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// do nothing
	}

	@Override
	public Object[] getElements(Object inputElement) {
		Object[] elements = null;
		if (inputElement instanceof List) {
			elements = ((List<?>) inputElement).toArray();
		} else if (inputElement instanceof String[]) {
			elements = (String[]) inputElement;
		}
		return elements;
	}

}