/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.eclipse.andmore.hierarchyviewer.views;

import com.android.hierarchyviewerlib.ui.PropertyViewer;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class PropertyView extends ViewPart {

	public static final String ID = "org.eclipse.andmore.hierarchyviewer.views.PropertyView"; //$NON-NLS-1$

	private PropertyViewer mPropertyViewer;

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());

		mPropertyViewer = new PropertyViewer(parent);
	}

	@Override
	public void setFocus() {
		mPropertyViewer.setFocus();
	}

}
