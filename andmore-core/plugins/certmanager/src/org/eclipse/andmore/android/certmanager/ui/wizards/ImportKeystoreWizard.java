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
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ImportKeystoreWizard extends Wizard {
	ImportKeystorePage importKeystorePage = null;

	private static final String WIZARD_BANNER = "icons/wizban/import_keystore_wiz.png"; //$NON-NLS-1$

	public ImportKeystoreWizard() {
		setWindowTitle(CertificateManagerNLS.ImportKeystorePage_Title);
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
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getShell(), ImportKeystorePage.IMPORT_KEYSTORE_HELP_ID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		return importKeystorePage.importKeystore();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		importKeystorePage = new ImportKeystorePage(CertificateManagerNLS.ImportKeystoreWizard_ImportKeystore);
		addPage(importKeystorePage);
	}
}
