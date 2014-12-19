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
package org.eclipse.andmore.android.certmanager.job;

import org.eclipse.andmore.android.certmanager.core.PasswordProvider;
import org.eclipse.andmore.android.certmanager.exception.InvalidPasswordException;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.composite.NewKeyBlock;
import org.eclipse.andmore.android.certmanager.ui.model.CertificateDetailsInfo;
import org.eclipse.andmore.android.certmanager.ui.model.EntryNode;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStoreEntry;
import org.eclipse.andmore.android.certmanager.ui.wizards.CreateKeyWizard;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Creating a key is a time expensive task. In order to not block the UI while
 * creating the key, a new job is creating and executed in the background.
 */
public class CreateKeyJob extends Job {
	private static final String KEY_PASSWORD_SAVED = "Key password saved";

	private static final String SAVING_KEY_PASSWORD = "Saving key password";

	private static final String KEY_CREATED = "Key created";

	private static final String CREATING_KEY = "Creating key";

	private static final String GETTING_KEY_INFO = "Getting key info";

	private static final int NUMBER_OF_TASKS = 5;

	/*
	 * Key block, used to retrieve information from the create key wizard.
	 */
	private NewKeyBlock newKeyBlock = null;

	/*
	 * Information to create key
	 */
	private CertificateDetailsInfo certificateDetailsInfo = null;

	private IKeyStore keystore = null;

	private String keyStorePass;

	public CreateKeyJob(String jobName, NewKeyBlock newKeyBlock, CertificateDetailsInfo certificateDetailsInfo,
			IKeyStore keystore, String keyStorePass) {
		super(jobName);
		this.certificateDetailsInfo = certificateDetailsInfo;
		this.newKeyBlock = newKeyBlock;
		this.keystore = keystore;
		this.keyStorePass = keyStorePass;
		if (this.keyStorePass == null) {
			try {
				this.keyStorePass = keystore.getPasswordProvider().getKeyStorePassword(false);
			} catch (KeyStoreManagerException e) {
				StudioLogger.error("Error while accessing keystore manager. " + e.getMessage());
			}
		}
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask(CREATING_KEY, NUMBER_OF_TASKS);

		StudioLogger.debug(GETTING_KEY_INFO);
		subMonitor.worked(1);

		IKeyStoreEntry entryNode = null;
		try {
			StudioLogger.debug(CREATING_KEY);
			subMonitor.worked(1);
			String keystorePassword = getKeyStorePassword();

			if (keystorePassword != null) {
				entryNode = EntryNode.createSelfSignedNode(keystore, keystorePassword,
						certificateDetailsInfo.getAlias(), certificateDetailsInfo);
				StudioLogger.debug(KEY_CREATED);
				subMonitor.worked(1);

				if (newKeyBlock.needToSaveKeyPassword()) {
					StudioLogger.debug(SAVING_KEY_PASSWORD);
					subMonitor.worked(1);
					PasswordProvider passwordProvider = new PasswordProvider(keystore.getFile());
					passwordProvider.savePassword(entryNode.getAlias(), newKeyBlock.getKeyPassword());
					StudioLogger.debug(KEY_PASSWORD_SAVED);
					subMonitor.worked(1);
				}
			}

		} catch (KeyStoreManagerException e) {
			EclipseUtils.showErrorDialog(CertificateManagerNLS.CreateKeyWizard_ErrorCreatingKey_DialogTitle,
					e.getMessage());
			StudioLogger.error(CreateKeyWizard.class,
					CertificateManagerNLS.CreateKeyWizard_ErrorCreatingKey_DialogTitle, e);
		}

		subMonitor.done();
		return Status.OK_STATUS;
	}

	private String getKeyStorePassword() throws KeyStoreManagerException {
		boolean invalidPass = true;
		String keystorePassword = this.keyStorePass;

		try {
			if (keystorePassword != null) {
				this.keystore.isPasswordValid(keystorePassword);
				invalidPass = false;
			}
		} catch (InvalidPasswordException e1) {
			invalidPass = true;
		}
		while (invalidPass) {
			try {
				this.keystore.isPasswordValid(keystorePassword);
				invalidPass = false;
			} catch (InvalidPasswordException e) {
				invalidPass = true;
				PasswordProvider passwordProvider = new PasswordProvider(this.keystore.getFile());
				keystorePassword = passwordProvider.getKeyStorePassword(true, false);

				if (keystorePassword == null) {
					invalidPass = false;
				}
			}
		}

		return keystorePassword;
	}

	public String getCreatedKeyAlias() {
		return this.certificateDetailsInfo.getAlias();
	}

	public IKeyStore getKeyStore() {
		return this.keystore;
	}

}
