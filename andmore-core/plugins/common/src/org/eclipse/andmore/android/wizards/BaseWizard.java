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
package org.eclipse.andmore.android.wizards;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

/**
 * Base class to simple wizards (without progress monitor and help).
 */
public abstract class BaseWizard extends Wizard {
	public BaseWizard() {
		setNeedsProgressMonitor(true);
		setHelpAvailable(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		return doPerformFinish();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	@Override
	public boolean canFinish() {
		IWizardPage currentPage = getContainer().getCurrentPage();
		if (getNextPage(currentPage) == null) {
			return currentPage.isPageComplete();
		}
		return super.canFinish();
	}

	/**
	 * This method is executed when the user finishes the wizard.
	 * 
	 * @return <code>true</code> if the method succeed.
	 */
	protected abstract boolean doPerformFinish();
}
