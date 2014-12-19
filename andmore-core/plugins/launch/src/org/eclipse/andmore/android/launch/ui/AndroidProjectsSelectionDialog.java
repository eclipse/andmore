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

package org.eclipse.andmore.android.launch.ui;

import org.eclipse.andmore.android.launch.LaunchPlugin;
import org.eclipse.andmore.android.launch.LaunchUtils;
import org.eclipse.andmore.android.launch.i18n.LaunchNLS;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * DESCRIPTION: Selection Dialog with only opened MOTOMAGX projects <br>
 * RESPONSIBILITY: Provides a Element Selection Dialog to select a MOTOMAGX
 * project <br>
 * COLABORATORS: none <br>
 * USAGE: This should be instanced when the user must choose one of a MOTOMAGX
 * project list
 */
public class AndroidProjectsSelectionDialog extends ElementListSelectionDialog {

	private static final String PRJ_SELECTION_CONTEXT_HELP_ID = LaunchPlugin.PLUGIN_ID + ".projectSelectionDialog";

	/**
	 * Create a new Project Selection Dialog
	 * 
	 * @param parent
	 *            the parent shell
	 * @param renderer
	 *            the label provider
	 */
	public AndroidProjectsSelectionDialog(Shell parent, ILabelProvider renderer) {
		super(parent, renderer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.dialogs.ElementListSelectionDialog#createDialogArea(org
	 * .eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);

		setHelpAvailable(true);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, PRJ_SELECTION_CONTEXT_HELP_ID);
		return control;
	}

	/**
	 * Creates a selection dialog with the workbench label provider
	 * 
	 * @param parent
	 *            The parent composite
	 */
	public AndroidProjectsSelectionDialog(Shell parent) {
		super(parent, new WorkbenchLabelProvider());
	}

	/**
	 * Sets the default elements: the list of all opened Studio for Android
	 * projects
	 */
	public void setDefaultElements() {
		this.setElements(LaunchUtils.getSupportedProjects());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.AbstractElementListSelectionDialog#open()
	 */
	@Override
	public int open() {
		this.setTitle(LaunchNLS.UI_LaunchComposite_SelectProjectScreenTitle);
		this.setMessage(LaunchNLS.UI_LaunchComposite_SelectProjectScreenMessage);

		setDefaultElements();
		return super.open();
	}
}
