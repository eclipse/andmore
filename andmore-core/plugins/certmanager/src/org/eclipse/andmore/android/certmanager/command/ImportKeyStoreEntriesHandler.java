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

import java.util.Map;

import org.eclipse.andmore.android.certmanager.core.KeyStoreUtils;
import org.eclipse.andmore.android.certmanager.core.PasswordProvider;
import org.eclipse.andmore.android.certmanager.exception.InvalidPasswordException;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.ui.dialogs.importks.ImportEntriesDialog;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;

/**
 * Handler to execute the wizard that import keys from one keystore to another.
 * */
public class ImportKeyStoreEntriesHandler extends AbstractHandler2 {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ITreeNode node = getSelection().get(0);

		if (node instanceof IKeyStore) {
			IKeyStore keyStore = (IKeyStore) node;
			ImportEntriesDialog dialog = new ImportEntriesDialog(PlatformUI.getWorkbench()
					.getModalDialogShellProvider().getShell(), keyStore);
			int diagStatus = dialog.open();
			if (diagStatus == Window.OK) {
				IKeyStore sourceKeyStore = dialog.getKeyStore();
				String sourcePassword = dialog.getPassword();
				Map<String, String> aliases = dialog.getAliases();
				IKeyStore targetKeyStore = dialog.getTargetKeyStore();

				PasswordProvider passwordProvider = targetKeyStore.getPasswordProvider();
				String password;
				boolean invalidPassword = false;
				do {
					try {
						password = passwordProvider.getKeyStorePassword(true);
						if (password != null) {
							KeyStoreUtils.importKeys(targetKeyStore.getKeyStore(), targetKeyStore.getFile(),
									targetKeyStore.getType(), password.toCharArray(), sourceKeyStore.getKeyStore(),
									sourceKeyStore.getFile(), sourcePassword.toCharArray(), aliases);
							invalidPassword = false;
							targetKeyStore.forceReload(password.toCharArray(), true);
						} else {
							break;
						}
					} catch (KeyStoreManagerException e) {
						EclipseUtils.showErrorDialog(e);
					} catch (InvalidPasswordException e) {
						invalidPassword = true;
					}
				} while (invalidPassword);

			}
		}
		return null;
	}
}
