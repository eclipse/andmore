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
import org.eclipse.andmore.android.certmanager.job.CreateKeyJob;
import org.eclipse.andmore.android.certmanager.ui.composite.NewKeyBlock;
import org.eclipse.andmore.android.certmanager.ui.model.CertificateDetailsInfo;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.wizards.BaseWizardPage;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.jface.wizard.IWizard;

/**
 * Wizard page to create an Android key.
 */
public class CreateKeyWizardPage extends BaseWizardPage {
	private IKeyStore keystore = null;

	private String alias = null;

	private String keyStorePass;

	public static final String CREATE_SELF_SIGNED_CERTIFICATE_HELP_ID = CertificateManagerActivator.PLUGIN_ID
			+ ".create-self-cert";

	private IJobChangeListener createKeyJobListener = null;

	/**
	 * The default constructor.
	 */
	public CreateKeyWizardPage(IKeyStore keystore) {
		super(new NewKeyBlock(), CertificateManagerNLS.CreateSelfSignedCertificateWizardPage_Title,
				CertificateManagerNLS.CreateSelfSignedCertificateWizardPage_Description,
				CREATE_SELF_SIGNED_CERTIFICATE_HELP_ID);
		((NewKeyBlock) block).setBaseWizardPage(this);
		this.keystore = keystore;
	}

	public CreateKeyWizardPage(IKeyStore keystore, String keystorePassword, IJobChangeListener createKeyJobListener) {
		this(keystore);
		setKeyStorePass(keystorePassword);
		this.createKeyJobListener = createKeyJobListener;
	}

	/**
	 * Obtains the key pair alias defined by user.
	 * 
	 * @return The key pair alias.
	 */
	public String getAlias() {
		return ((NewKeyBlock) block).getAlias();
	}

	/**
	 * Obtains the common name defined by user.
	 * 
	 * @return The common name.
	 */
	public String getCommonName() {
		return ((NewKeyBlock) block).getCommonName();
	}

	/**
	 * Obtains the organization defined by user.
	 * 
	 * @return The organization.
	 */
	public String getOrganization() {
		return ((NewKeyBlock) block).getOrganization();
	}

	/**
	 * Obtains the organization unit defined by user.
	 * 
	 * @return The organization unit.
	 */
	public String getOrganizationUnit() {
		return ((NewKeyBlock) block).getOrganizationUnit();
	}

	/**
	 * Obtains the locality defined by user.
	 * 
	 * @return The locality.
	 */
	public String getLocality() {
		return ((NewKeyBlock) block).getLocality();
	}

	/**
	 * Obtains the state defined by user.
	 * 
	 * @return The state.
	 */
	public String getState() {
		return ((NewKeyBlock) block).getState();
	}

	/**
	 * Obtains the country defined by user.
	 * 
	 * @return The country.
	 */
	public String getCountry() {
		return ((NewKeyBlock) block).getCountry();
	}

	/**
	 * Obtains the validity defined by user.
	 * 
	 * @return The validity.
	 */
	public String getValidity() {
		return ((NewKeyBlock) block).getValidity();
	}

	public String getEntryPassword() {
		return ((NewKeyBlock) block).getKeyPassword();
	}

	public String getEntryConfirmPassword() {
		return ((NewKeyBlock) block).getKeyConfirmPassword();
	}

	public boolean needToSaveKeyEntryPassword() {
		return ((NewKeyBlock) block).needToSaveKeyPassword();
	}

	public boolean createKey() {
		boolean successfullyCreated = true;
		alias = getAlias();

		if (isPageCompleteWithAllFieldsBlank()) {
			successfullyCreated = true;
		} else {
			final CertificateDetailsInfo certificateDetailsInfo = new CertificateDetailsInfo(alias, getCommonName(),
					getOrganization(), getOrganizationUnit(), getLocality(), getCountry(), getState(), getValidity(),
					getEntryPassword());
			CreateKeyJob createKeyJob = new CreateKeyJob("Create key job", (NewKeyBlock) block, certificateDetailsInfo,
					keystore, keyStorePass);

			if (createKeyJobListener != null) {
				createKeyJob.addJobChangeListener(createKeyJobListener);
			}
			createKeyJob.schedule();

			successfullyCreated = true;
		}

		return successfullyCreated;
	}

	/**
	 * @param keystoreNode
	 */
	public void setKeyStore(IKeyStore keystoreNode) {
		this.keystore = keystoreNode;
	}

	/*
	 * If all fields are blank and this page is under CreateKeystoreWizard
	 * context, then if all fields are blank the page is considered complete and
	 * no keypair will be created.
	 */
	public boolean isPageCompleteWithAllFieldsBlank() {
		boolean result = false;
		IWizard wizardContext = getWizard();

		// in the context of CreateKeyStoreWizard and if this is NOT the current
		// page, then allow all fields blank
		// be a valid complete page
		if ((wizardContext instanceof CreateKeystoreWizard) && !isCurrentPage() && areAllFieldsBlank()) {
			result = true;
		}

		return result;
	}

	/*
	 * return true if all fields are blank
	 */
	private boolean areAllFieldsBlank() {
		boolean result = false;
		if (getAlias().isEmpty() && getCommonName().isEmpty() && getOrganization().isEmpty()
				&& getOrganizationUnit().isEmpty() && getLocality().isEmpty() && getState().isEmpty()
				&& getCountry().isEmpty() && getEntryPassword().isEmpty() && getEntryConfirmPassword().isEmpty()) {
			result = true;
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	@Override
	public boolean isPageComplete() {
		if (this.block == null) {
			return true;
		}
		return this.block.isPageComplete() || isPageCompleteWithAllFieldsBlank();
	}

	/**
	 * This method just works.
	 * 
	 * @return
	 */
	public String getTrueAlias() {
		return alias;
	}

	public void setKeyStorePass(String password) {
		this.keyStorePass = password;
	}

	/**
	 * @return the keystore
	 */
	public IKeyStore getKeystore() {
		return keystore;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			((NewKeyBlock) block).setFocus();
		}

	}
}
