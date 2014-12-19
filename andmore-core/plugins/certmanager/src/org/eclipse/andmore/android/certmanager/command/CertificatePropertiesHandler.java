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

import org.eclipse.andmore.android.certmanager.ui.composite.KeyPropertiesBlock;
import org.eclipse.andmore.android.certmanager.ui.dialogs.CertificateInfoDialog;
import org.eclipse.andmore.android.certmanager.ui.model.EntryNode;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStoreEntry;
import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * This class implements the command to display certificate properties
 * */
public class CertificatePropertiesHandler extends AbstractHandler2 implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// retrieves the first element of the selection
		// note that this command should be enabled only when selection.count ==
		// 1.
		ITreeNode node = getSelection().get(0);

		if (node instanceof IKeyStoreEntry) {
			final EntryNode keyStoreEntry = (EntryNode) node;
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					IWorkbench workbench = PlatformUI.getWorkbench();
					IWorkbenchWindow ww = workbench.getActiveWorkbenchWindow();
					Shell shell = ww.getShell();

					CertificateInfoDialog dialog = new CertificateInfoDialog(shell, new KeyPropertiesBlock(),
							keyStoreEntry);

					dialog.open();
				}
			});
		}

		return null;
	}
}
