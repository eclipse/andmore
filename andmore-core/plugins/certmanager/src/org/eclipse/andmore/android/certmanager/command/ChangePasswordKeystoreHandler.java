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

import org.eclipse.andmore.android.certmanager.core.KeyStoreUtils;
import org.eclipse.andmore.android.certmanager.core.PasswordProvider;
import org.eclipse.andmore.android.certmanager.event.KeyStoreModelEventManager;
import org.eclipse.andmore.android.certmanager.event.KeyStoreModelEvent.EventType;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;
import org.eclipse.andmore.android.certmanager.ui.model.KeyStoreNode;
import org.eclipse.andmore.android.certmanager.views.KeystoreManagerView;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.common.utilities.ui.PasswordInputDialog;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

/**
 * This class deletes the keystore from the tree of the
 * {@link KeystoreManagerView}
 * */
public class ChangePasswordKeystoreHandler extends AbstractHandler2 implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		// retrieves the first element of the selection
		// note that this command should be enabled only when selection.count ==
		// 1.
		ITreeNode node = getSelection().get(0);

		if (node instanceof KeyStoreNode) {
			KeyStoreNode keyStoreNode = (KeyStoreNode) node;
			PasswordProvider passwordProvider = new PasswordProvider(keyStoreNode.getFile());

			// must enter old and new password
			PasswordInputDialog passwordInputDialog = null;

			boolean cancelledOrChangedPassword = false;

			do {
				passwordInputDialog = new PasswordInputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getShell(), NLS.bind(CertificateManagerNLS.Passwordinput_EnterOldKeystorePasssword_Message,
						keyStoreNode.getName()), true, null, IKeyStore.KEYSTORE_PASSWORD_MIN_SIZE);
				if (passwordInputDialog.open() == Window.OK) {
					String newPassword = passwordInputDialog.getNewPassword();
					String oldPassword = passwordInputDialog.getOldPassword();

					if ((newPassword != null) && (oldPassword != null)) {
						KeyStore keyStore = null;
						try {
							keyStore = KeyStoreUtils.loadKeystore(keyStoreNode.getFile(), oldPassword.toCharArray(),
									keyStoreNode.getType());

							try {
								// rewrite keystore with new password
								KeyStoreUtils.changeKeystorePasswd(keyStore, keyStoreNode.getFile(),
										oldPassword.toCharArray(), newPassword.toCharArray());

								// deletes old password from provider
								if (passwordProvider.isPasswordSaved()) {
									passwordProvider.deleteKeyStoreSavedPassword();
								}

								if (passwordInputDialog.needToStorePassword()) {
									// store new password at provider
									passwordProvider.saveKeyStorePassword(newPassword);
								}

								// success
								cancelledOrChangedPassword = true;
								EclipseUtils.showInformationDialog(
										CertificateManagerNLS.PasswordChanged_Info_Title,
										NLS.bind(CertificateManagerNLS.PasswordChanged_Info_Message,
												keyStoreNode.getFile()));

								// update tooltip and image
								KeyStoreModelEventManager.getInstance().fireEvent(node, EventType.UPDATE);
							} catch (KeyStoreManagerException e) {
								// error to change password - notify on screen
								cancelledOrChangedPassword = false;
								EclipseUtils
										.showErrorDialog(
												CertificateManagerNLS.ChangePasswordKeystoreHandler_Error_ChangingKeystorePassword,
												e.getMessage());
							}
						} catch (Exception e1) {
							// invalid old password
							StudioLogger.error(ChangePasswordKeystoreHandler.class, e1.getMessage(), e1);
							cancelledOrChangedPassword = false;
							EclipseUtils.showErrorDialog(
									CertificateManagerNLS.ChangePasswordKeystoreHandler_Error_WrongOldKeystorePassword,
									NLS.bind(CertificateManagerNLS.ChangePasswordKeystoreHandler_InvalidOldPassword,
											keyStoreNode.getFile()));
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
