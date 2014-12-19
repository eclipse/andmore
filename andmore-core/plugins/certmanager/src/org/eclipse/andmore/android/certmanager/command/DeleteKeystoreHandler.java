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

import java.util.List;

import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;
import org.eclipse.andmore.android.certmanager.ui.model.KeyStoreNode;
import org.eclipse.andmore.android.certmanager.ui.model.SigningAndKeysModelManager;
import org.eclipse.andmore.android.certmanager.views.KeystoreManagerView;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * This class deletes the keystore from the tree of the
 * {@link KeystoreManagerView}
 * */
public class DeleteKeystoreHandler extends AbstractHandler2 implements IHandler2 {

	private static boolean toggleState = false;

	/*
	 * Question dialog confirming deletion of the keystore with a toggle asking
	 * if the keystore should also be deleted from the filesystem
	 * 
	 * @return true if the deletion is confirmed, false otherwise and true in
	 * the toggleState if it can also be deleted from the filesystem, false
	 * otherwise
	 */
	private boolean showQuestion(List<ITreeNode> nodesToDelete) {

		final Boolean[] reply = new Boolean[2];

		final String keystoreName = nodesToDelete.size() == 1 ? nodesToDelete.get(0).getName() : null;

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow ww = workbench.getActiveWorkbenchWindow();
				Shell shell = ww.getShell();

				MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(
						shell,
						CertificateManagerNLS.DeleteKeystoreHandler_ConfirmationQuestionDialog_Title,
						keystoreName != null ? NLS.bind(
								CertificateManagerNLS.DeleteKeystoreHandler_ConfirmationQuestionDialog_Description,
								keystoreName) : CertificateManagerNLS.DeleteKeystoreHandler_Delete_Selected_Keystores,
						CertificateManagerNLS.DeleteKeystoreHandler_ConfirmationQuestionDialog_Toggle, false, null,
						null);
				reply[0] = (dialog.getReturnCode() == IDialogConstants.YES_ID);
				reply[1] = dialog.getToggleState();
			}
		});

		toggleState = reply[1];

		return reply[0];
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<ITreeNode> nodesToDelete = getSelection();

		if (!nodesToDelete.isEmpty()) {
			boolean shouldProceed = showQuestion(nodesToDelete);
			if (shouldProceed) {
				for (ITreeNode node2Delete : nodesToDelete) {
					KeyStoreNode keyStoreNode = (KeyStoreNode) node2Delete;

					// remove from the tree
					SigningAndKeysModelManager.getInstance().unmapKeyStore(keyStoreNode);

					if (toggleState) {
						keyStoreNode.getFile().delete();
					}
				}
			}
		}

		nodesToDelete.clear();

		return null;
	}

}
