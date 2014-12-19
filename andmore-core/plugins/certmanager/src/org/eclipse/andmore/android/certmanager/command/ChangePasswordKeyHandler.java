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

import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.UnrecoverableKeyException;

import org.eclipse.andmore.android.certmanager.core.KeyStoreUtils;
import org.eclipse.andmore.android.certmanager.core.PasswordProvider;
import org.eclipse.andmore.android.certmanager.event.KeyStoreModelEventManager;
import org.eclipse.andmore.android.certmanager.event.KeyStoreModelEvent.EventType;
import org.eclipse.andmore.android.certmanager.exception.InvalidPasswordException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.model.EntryNode;
import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;
import org.eclipse.andmore.android.certmanager.ui.model.KeyStoreNode;
import org.eclipse.andmore.android.certmanager.views.KeystoreManagerView;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.common.utilities.ui.PasswordInputDialog;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;

/**
 * This class changes the password of a key/certificate from the tree of the
 * {@link KeystoreManagerView}
 * */
public class ChangePasswordKeyHandler extends AbstractHandler2 implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		// retrieves the first element of the selection
		// note that this command should be enabled only when selection.count ==
		// 1.
		ITreeNode entry = getSelection().get(0);

		if (entry instanceof EntryNode) {
			// must enter old and new password
			KeyStoreNode keyStoreNode = (KeyStoreNode) entry.getParent();
			PasswordProvider passwordProvider = new PasswordProvider(keyStoreNode.getFile());
			EntryNode entryNode = (EntryNode) entry;

			PasswordInputDialog passwordInputDialog = null;

			boolean cancelledOrChangedPassword = false;

			do {
				passwordInputDialog = new PasswordInputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getShell(), CertificateManagerNLS.PasswordInput_EnterOldKeyPasssword_Message, true, null,
						EntryNode.KEY_PASSWORD_MIN_SIZE);
				if (passwordInputDialog.open() == Window.OK) {
					String newPassword = passwordInputDialog.getNewPassword();
					String oldPassword = passwordInputDialog.getOldPassword();

					if ((newPassword != null) && (oldPassword != null)) {
						try {
							Entry keyEntry = entryNode.getKeyEntry(oldPassword);
							if (keyEntry != null) {
								boolean tryAgain = false;
								boolean useSavedPass = true;
								String keystorePassword = null;
								KeyStore keyStore = null;
								do {
									if (tryAgain) {
										useSavedPass = false;
									}
									keystorePassword = passwordProvider.getKeyStorePassword(true, useSavedPass);
									tryAgain = false;
									if (keystorePassword != null) {
										try {
											if (keyStoreNode.isPasswordValid(keystorePassword)) {
												keyStore = keyStoreNode.getKeyStore(keystorePassword);
											} else {
												tryAgain = true;
											}
										} catch (InvalidPasswordException e) {
											tryAgain = true;
										}
									}
								} while (tryAgain);
								if (keyStore != null) {
									KeyStoreUtils.changeEntryPassword(keyStore, keystorePassword.toCharArray(),
											keyStoreNode.getFile(), entryNode.getAlias(), keyEntry,
											newPassword.toCharArray());

									// delete old save password - if there is
									// one
									if (passwordProvider.isPasswordSaved(entryNode.getAlias())) {
										passwordProvider.deleteSavedPassword(entryNode.getAlias());
									}

									if (passwordInputDialog.needToStorePassword()) {
										// store new password at provider
										passwordProvider.savePassword(entryNode.getAlias(), newPassword);
									}

									// success
									cancelledOrChangedPassword = true;
									EclipseUtils.showInformationDialog(
											CertificateManagerNLS.PasswordChanged_Info_Title,
											CertificateManagerNLS.PasswordChanged_KeyInfo_Message);

									// update tooltip and image
									KeyStoreModelEventManager.getInstance().fireEvent(entryNode, EventType.UPDATE);
								}
							}
						} catch (UnrecoverableKeyException e) {
							// error - notify on screen
							cancelledOrChangedPassword = false;
							EclipseUtils.showErrorDialog(
									CertificateManagerNLS.ChangePasswordKeyHandler_Error_WrongOldKeyPassword,
									CertificateManagerNLS.ChangePasswordKeyHandler_Wrong_Key_Password);
						} catch (Exception e) {
							// error - notify on screen
							cancelledOrChangedPassword = false;
							EclipseUtils.showErrorDialog(
									CertificateManagerNLS.ChangePasswordKeyHandler_Error_WrongOldKeyPassword,
									e.getMessage());
						}
					}
				} else {
					// user cancelled screen
					cancelledOrChangedPassword = true;
				}

			} while (!cancelledOrChangedPassword);

		}

		return null;
	}
}
