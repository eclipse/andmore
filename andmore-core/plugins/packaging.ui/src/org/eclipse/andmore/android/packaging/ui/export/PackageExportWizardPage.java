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
package org.eclipse.andmore.android.packaging.ui.export;

import org.eclipse.andmore.android.packaging.ui.PackagingUIPlugin;
import org.eclipse.andmore.android.packaging.ui.i18n.Messages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Package Export Wizard Page
 * 
 */
public class PackageExportWizardPage extends WizardPage implements Listener {
	private PackageExportWizardArea dialogArea;

	private final IStructuredSelection structuredSelection;

	private final boolean signEnabled;

	/**
	 * Wizard page constructor.
	 * 
	 * @param pageName
	 * @param selection
	 */
	public PackageExportWizardPage(String pageName, IStructuredSelection selection, boolean signEnabled) {
		super(pageName);
		this.structuredSelection = selection;
		this.signEnabled = signEnabled;
		setTitle(pageName); // NON-NLS-1

		ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PackagingUIPlugin.PLUGIN_ID,
				PackagingUIPlugin.EXPORT_WIZARD_ICON);

		if (imageDescriptor != null) {
			setImageDescriptor(imageDescriptor);
		}

		setDescription(Messages.EXPORT_WIZARD_DESCRIPTION);
	}

	/**
	 * Create the Wizard page control
	 * 
	 * @param parent
	 *            the parent composite
	 */
	@Override
	public void createControl(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayout(new GridLayout());
		mainComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mainComposite.addListener(SWT.Modify, this);

		this.dialogArea = new PackageExportWizardArea(structuredSelection, mainComposite, this.signEnabled);
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(mainComposite, PackagingUIPlugin.PACKAGING_WIZARD_CONTEXT_HELP_ID);
		this.setControl(mainComposite);

	}

	/**
	 * Override the method isPageComplete
	 */
	@Override
	public boolean isPageComplete() {
		boolean pageComplete = this.dialogArea.canFinish();
		setMessage(this.dialogArea.getMessage(), this.dialogArea.getSeverity());
		return pageComplete;
	}

	/**
	 * Delegates the finish operation to Dialog Implementation.
	 */
	public boolean finish() {
		return this.dialogArea.performFinish();
	}

	/**
	 * Event Handler
	 * 
	 * @param event
	 */
	@Override
	public void handleEvent(Event event) {
		this.getContainer().updateButtons();
		setMessage(this.dialogArea.getMessage(), this.dialogArea.getSeverity());
	}
}
