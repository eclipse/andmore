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
package org.eclipse.andmore.android.wizards.project;

import org.eclipse.andmore.android.Sample;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.model.AndroidProject;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

/**
 * Class that implements a Selection Adapter for the Sample Selection Page
 */
class SamplesSelectionAdapter extends SelectionAdapter {
	private final SampleSelectionPage sampleSelectionPage;

	private final AndroidProject project;

	/**
	 * Default constructor
	 * 
	 * @param sampleSelectionPage
	 *            The sample selection page
	 * @param treeViewer
	 *            The tree viewer of the selection adapter
	 * @param project
	 *            The project
	 */
	public SamplesSelectionAdapter(SampleSelectionPage sampleSelectionPage, AndroidProject project) {
		this.sampleSelectionPage = sampleSelectionPage;
		this.project = project;

		sampleSelectionPage.setMessage(AndroidNLS.UI_SampleSelectionPage_WizardDescription, IMessageProvider.NONE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt
	 * .events.SelectionEvent)
	 */
	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.item != null) {
			project.setSample((Sample) e.item.getData());
			sampleSelectionPage.getWizard().getContainer().updateButtons();
		}
	}
}
