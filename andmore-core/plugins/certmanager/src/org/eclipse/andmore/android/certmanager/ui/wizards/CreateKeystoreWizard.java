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
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.model.KeyStoreNode;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 *
 */
public class CreateKeystoreWizard extends Wizard {

	private final CreateKeystorePage createKeystorePage;

	private final CreateKeyWizardPage createKeyPairPage;

	private static final String WIZARD_BANNER = "icons/wizban/create_keystore_wiz.png"; //$NON-NLS-1$

	private static final String KEYSTORE_KEY_HELP_ID = CertificateManagerActivator.PLUGIN_ID + ".keystore-key-help-id";

	private KeyStoreNode createdKeystoreNode;

	/**
     * 
     */
	public CreateKeystoreWizard() {
		this(null);
	}

	public CreateKeystoreWizard(IJobChangeListener createKeystoreJobListener) {
		setWindowTitle(CertificateManagerNLS.CreateKeystoreWizard_CreateNewKeyStore);
		setDefaultPageImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(CertificateManagerActivator.PLUGIN_ID,
				WIZARD_BANNER));

		this.createKeyPairPage = new CreateKeyWizardPage(null, "", createKeystoreJobListener);
		this.createKeystorePage = new CreateKeystorePage(CertificateManagerNLS.CreateKeystoreWizard_CreateNewKeyStore);
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

		// the shell has a generic help that talks about keystore and keys
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getShell(), KEYSTORE_KEY_HELP_ID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		createdKeystoreNode = createKeystorePage.createKeyStore();
		if (createdKeystoreNode != null) {
			createKeyPairPage.setKeyStore(createdKeystoreNode);
			createKeyPairPage.setKeyStorePass(createKeystorePage.getPassword());
			createKeyPairPage.createKey();
		}

		// check if the keystore was created
		return (createdKeystoreNode != null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		this.addPage(createKeystorePage);
		this.addPage(createKeyPairPage);
	}

	/**
	 * @return the createdKeystoreNode
	 */
	public KeyStoreNode getCreatedKeystoreNode() {
		return createdKeystoreNode;
	}

	public String getCreatedKeystorePassword() {
		String result = null;
		if (createKeystorePage != null) {
			result = createKeystorePage.getPassword();
		}

		return result;
	}
}
