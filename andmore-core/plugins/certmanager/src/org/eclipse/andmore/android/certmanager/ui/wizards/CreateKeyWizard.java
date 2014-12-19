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
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.wizards.BaseWizard;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Wizard to create an Android key.
 */
public class CreateKeyWizard extends BaseWizard {

	private static final String WIZARD_BANNER = "icons/wizban/create_key_wiz.png"; //$NON-NLS-1$

	private boolean success = false;

	/**
	 * Wizard page to allow the user to inform the key pair alias and
	 * distinguished name.
	 */
	private final CreateKeyWizardPage createkeyWizardPage;

	public CreateKeyWizard(IKeyStore keystore) {
		setupWizardUi();
		createkeyWizardPage = new CreateKeyWizardPage(keystore);
	}

	public CreateKeyWizard(IKeyStore keystore, String keystorePassword, IJobChangeListener createKeyJobListener) {
		setupWizardUi();
		createkeyWizardPage = new CreateKeyWizardPage(keystore, keystorePassword, createKeyJobListener);
	}

	private void setupWizardUi() {
		setWindowTitle(CertificateManagerNLS.CreateSelfSignedCertificateWizardPage_Title);
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

		// the shell has the same help as its single page
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(getShell(), CreateKeyWizardPage.CREATE_SELF_SIGNED_CERTIFICATE_HELP_ID);
	}

	@Override
	protected boolean doPerformFinish() {
		success = createkeyWizardPage.createKey();
		return success;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		super.addPages();
		addPage(createkeyWizardPage);
	}

	/**
	 * Returns the alias of the just created key or null otherwise.
	 */
	public String getAlias() {
		return success ? createkeyWizardPage.getTrueAlias() : null;
	}
}
