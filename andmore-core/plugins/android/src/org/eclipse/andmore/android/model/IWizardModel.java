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
package org.eclipse.andmore.android.model;

import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;

/**
 * Controller interface. It should communicate with the Wizard UI to create on
 * workspace the wizard output.
 */
public interface IWizardModel {
	public static final int MODIFIED = 1546;

	/**
	 * Return the Model Status. This Status must contains the
	 * {@link IStatus#getSeverity()} according with the needed values, and must
	 * contain the message for not OK Status.
	 * 
	 * @return The Model Status
	 */
	public IStatus getStatus();

	/**
	 * Save Contents in Workspace;
	 * 
	 * @param container
	 * @return
	 * @throws AndroidException
	 */
	/**
	 * Save Contents in Workspace;
	 * 
	 * @param container
	 * @param monitor
	 * @return
	 * @throws AndroidException
	 */
	boolean save(IWizardContainer container, IProgressMonitor monitor) throws AndroidException;

	/**
	 * Check if need more information to finish.
	 * 
	 * @see IWizard#canFinish()
	 * @return
	 */
	boolean needMoreInformation();
}
