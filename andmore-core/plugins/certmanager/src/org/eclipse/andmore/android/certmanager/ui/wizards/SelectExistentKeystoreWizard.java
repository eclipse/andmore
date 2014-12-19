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

package org.eclipse.andmore.android.certmanager.ui.wizards;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.exception.InvalidPasswordException;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.certmanager.ui.model.KeyStoreNode;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Enables selection of a keystore (similar to {@link ImportKeystoreWizard}
 * functionality. It adds a checkbox button that enables user to add the
 * imported keystore into Signing and Keys view.
 */
public class SelectExistentKeystoreWizard extends Wizard {
	protected SelectExistentKeystorePage selectExistentKeystorePage = null;

	private static final String WIZARD_BANNER = "icons/wizban/import_keystore_wiz.png"; //$NON-NLS-1$

	public static final String SELECT_KEYSTORE_HELP_ID = CertificateManagerActivator.PLUGIN_ID + ".select_keystore"; //$NON-NLS-1$

	public SelectExistentKeystoreWizard() {
		setWindowTitle(CertificateManagerNLS.ImportKeystoreWizard_ImportKeystore);
		setDefaultPageImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(CertificateManagerActivator.PLUGIN_ID,
				WIZARD_BANNER));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.wizard.Wizard#createPageControls(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);

		// the shell has the same help as its page
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getShell(), SELECT_KEYSTORE_HELP_ID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		// check password and keystore type before importing
		IKeyStore iKeyStore = selectExistentKeystorePage.getSelectedKeystore();
		if (iKeyStore instanceof KeyStoreNode) {
			KeyStoreNode keyStoreNode = (KeyStoreNode) iKeyStore;
			try {
				keyStoreNode.isPasswordValid(selectExistentKeystorePage.getPassword());
			} catch (KeyStoreManagerException e) {
				selectExistentKeystorePage
						.setErrorMessage(CertificateManagerNLS.SelectExistentKeystoreWizard_Error_KeystoreType);
				// let dialog opened
				return false;
			} catch (InvalidPasswordException e) {
				selectExistentKeystorePage
						.setErrorMessage(CertificateManagerNLS.SelectExistentKeystoreWizard_Error_InvalidPassword);
				// let dialog opened
				return false;
			}
		}

		if (selectExistentKeystorePage.needToImportIntoView()) {
			// if user asked to import item in the Signing and keys view
			selectExistentKeystorePage.importKeystore();
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		selectExistentKeystorePage = new SelectExistentKeystorePage(
				CertificateManagerNLS.SelectExistentKeystoreWizard_BrowseExistentKeystore_PageTitle);
		addPage(selectExistentKeystorePage);
	}

	/**
	 * 
	 * @return
	 */
	public IKeyStore getSelectedKeystore() {
		IKeyStore iKeyStore = selectExistentKeystorePage.getSelectedKeystore();
		return iKeyStore;
	}

	/**
	 * 
	 * @return true if need to import into view, false otherwise
	 */
	public boolean canSavePassword() {
		return selectExistentKeystorePage.canSavePassword();
	}

	/**
	 * 
	 * @return true if need to import into view, false otherwise
	 */
	public String getPassword() {
		return selectExistentKeystorePage.getPassword();
	}
}
