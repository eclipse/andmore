/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.eclipse.andmore.ddms.views;

import com.android.ddmuilib.EmulatorControlPanel;

import org.eclipse.swt.widgets.Composite;

public class EmulatorControlView extends SelectionDependentViewPart {

	public static final String ID = "org.eclipse.andmore.ddms.views.EmulatorControlView"; //$NON-NLS-1$

	private EmulatorControlPanel mPanel;

	@Override
	public void createPartControl(Composite parent) {
		mPanel = new EmulatorControlPanel();
		mPanel.createPanel(parent);
		setSelectionDependentPanel(mPanel);
	}

	@Override
	public void setFocus() {
		mPanel.setFocus();
	}

}
