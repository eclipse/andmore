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

package com.motorola.studio.android.logger.collector.ui.wizard;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;

import com.motorola.studio.android.logger.collector.util.LoggerCollectorConstants;
import com.motorola.studio.android.logger.collector.util.LoggerCollectorMessages;
import com.motorola.studio.android.logger.collector.util.PlatformException;

/**
 * This class represents the logger collector wizard.
 */
public class LoggerCollectorWizard extends Wizard {

	/**
	 * Initializing instance of wizard page
	 */
	private final LoggerCollectorWizardPage loggerCollectorWizardPage = new LoggerCollectorWizardPage("wizardPage"); //$NON-NLS-1$ 

	/**
	 * The Constructor
	 */
	public LoggerCollectorWizard() {
		setWindowTitle(LoggerCollectorMessages.getInstance().getString("logger.collector.wizard.page.title")); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	@Override
	public boolean canFinish() {
		return loggerCollectorWizardPage.isPageComplete();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		super.addPages();
		addPage(loggerCollectorWizardPage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		if (loggerCollectorWizardPage.getLogFileColumn() != null) {
			try {

				IPath filename = new Path(loggerCollectorWizardPage.getFilename());
				if (filename.getFileExtension() == null
						|| !filename.getFileExtension().equalsIgnoreCase(LoggerCollectorConstants.ZIP_FILE_EXTENSION)) {
					filename = filename.addFileExtension(LoggerCollectorConstants.ZIP_FILE_EXTENSION);
				}
				if (loggerCollectorWizardPage.getLogFileColumn().collect(filename.toOSString())) {
					MessageDialog.openInformation(getShell(),
							LoggerCollectorMessages.getInstance().getString("logger.collector.wizard.page.title"), //$NON-NLS-1$ 
							LoggerCollectorMessages.getInstance().getString("logger.collector.wizard.success")); //$NON-NLS-1$
				}
				return true;
			} catch (PlatformException e) {
				MessageDialog.openError(getShell(),
						LoggerCollectorMessages.getInstance().getString("logger.collector.wizard.page.title"), //$NON-NLS-1$ 
						e.getMessage());
				return false;
			}
		}
		return false;
	}
}
