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
package org.eclipse.andmore.android.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.PlatformUI;

/**
 * Abstract class intended to be used to create menu commands for items in the
 * MOTODEV Menu
 */
abstract class NewWizardHandler extends AbstractHandler {
	private static final int WIZARD_WIDTH = 500;

	/**
	 * Opens a wizard
	 * 
	 * @param wizard
	 *            the wizard to be opened
	 */
	protected void openWizard(final INewWizard wizard) {
		if (!PlatformUI.getWorkbench().isClosing()) {
			Shell shell = new Shell();

			ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService()
					.getSelection();
			IStructuredSelection structuredSelection;

			if (selection instanceof IStructuredSelection) {
				structuredSelection = (IStructuredSelection) selection;
			} else {
				structuredSelection = new StructuredSelection();
			}

			wizard.init(PlatformUI.getWorkbench(), structuredSelection);
			WizardDialog dialog = new WizardDialog(shell, wizard);

			dialog.setPageSize(WIZARD_WIDTH, SWT.DEFAULT);
			shell.pack();
			centralizeShell(shell);

			dialog.open();
		}
	}

	/**
	 * Centralizes a shell on the display
	 * 
	 * @param shell
	 *            The shell to be centralized
	 */
	private void centralizeShell(Shell shell) {
		int displayWidth = shell.getDisplay().getClientArea().width;
		int displayHeight = shell.getDisplay().getClientArea().height;

		int x = (displayWidth - shell.getSize().x) / 2;
		int y = (displayHeight - shell.getSize().y) / 2;

		shell.setLocation(x, y);
	}
}
