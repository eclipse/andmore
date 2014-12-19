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

package org.eclipse.andmore.android.certmanager.command;

import org.eclipse.andmore.android.certmanager.ui.wizards.CreateKeystoreWizard;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Handler to execute the create keystore wizard.
 * */
public class CreateKeystoreHandler extends AbstractHandler implements IHandler2 {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		CreateKeystoreWizard createKeystoreWizard = new CreateKeystoreWizard();

		WizardDialog dialog = new WizardDialog(shell, createKeystoreWizard);

		// open the wizard to create keystores
		// the result doesn't need to be handled as it is self-contained
		dialog.create();
		dialog.open();

		return null;
	}
}
