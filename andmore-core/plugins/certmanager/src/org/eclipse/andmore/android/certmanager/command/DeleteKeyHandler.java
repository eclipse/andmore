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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.model.EntryNode;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;
import org.eclipse.andmore.android.certmanager.views.KeystoreManagerView;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * This class deletes the key entry from the tree of the
 * {@link KeystoreManagerView}
 * */
public class DeleteKeyHandler extends AbstractHandler2 implements IHandler {

	/*
	 * Question dialog confirming deletion of the key with a toggle asking if
	 * the key should also be deleted from the filesystem
	 * 
	 * @return true if the deletion is confirmed, false otherwise and true in
	 * the toggleState if it can also be deleted from the filesystem, false
	 * otherwise
	 */
	private boolean showQuestion(List<ITreeNode> nodesToDelete) {

		final Boolean[] reply = new Boolean[1];

		final String entryName = nodesToDelete.size() == 1 ? nodesToDelete.get(0).getName() : null;

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow ww = workbench.getActiveWorkbenchWindow();
				Shell shell = ww.getShell();

				reply[0] = MessageDialog.openQuestion(
						shell,
						CertificateManagerNLS.DeleteKeyHandler_ConfirmationQuestionDialog_Title,
						entryName != null ? NLS.bind(
								CertificateManagerNLS.DeleteKeyHandler_ConfirmationQuestionDialog_Description,
								entryName) : CertificateManagerNLS.DeleteKeyHandler_Delete_Selected_Keys);
			}
		});

		return reply[0];
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<ITreeNode> nodesToDelete = getSelection();

		if (!nodesToDelete.isEmpty()) {
			boolean shouldProceed = showQuestion(nodesToDelete);
			if (shouldProceed) {
				Map<IKeyStore, List<String>> deleteNodesMap = getKeysMap(nodesToDelete);

				for (IKeyStore keyStore : deleteNodesMap.keySet()) {
					try {
						keyStore.removeKeys(deleteNodesMap.get(keyStore));
					} catch (KeyStoreManagerException e) {
						EclipseUtils.showErrorDialog(e);
						throw new ExecutionException(e.getMessage(), e);
					}
				}
			}
		}

		nodesToDelete.clear();

		return null;
	}

	private Map<IKeyStore, List<String>> getKeysMap(List<ITreeNode> nodesToDelete) {
		Map<IKeyStore, List<String>> deleteNodesMap = new HashMap<IKeyStore, List<String>>();
		for (ITreeNode node2Delete : nodesToDelete) {
			if (node2Delete instanceof EntryNode) {
				EntryNode entryNode = (EntryNode) node2Delete;
				IKeyStore iKeyStore = (IKeyStore) entryNode.getParent();
				List<String> keyList = deleteNodesMap.get(iKeyStore);
				if (keyList == null) {
					keyList = new ArrayList<String>();
				}
				keyList.add(entryNode.getAlias());
				deleteNodesMap.put(iKeyStore, keyList);
			}
		}
		return deleteNodesMap;
	}
}
