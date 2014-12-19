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

import org.eclipse.andmore.android.common.utilities.ui.WidgetsFactory;
import org.eclipse.andmore.android.wizards.elements.IBaseBlock;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;

/**
 * Base class for the wizard pages.
 */
public abstract class BaseWizardPage extends WizardPage implements Listener {

	/**
	 * Default wizard page Composite
	 */
	private Composite composite;

	/**
	 * Default wizard page GridLayout
	 */
	private GridLayout layout;

	/**
	 * The block used by this wizard page.
	 */
	protected IBaseBlock block;

	private String helpID = null;

	/**
	 * Constructs a new instance using a given block.
	 * 
	 * @param block
	 *            Block used to render the screen.
	 * @param title
	 *            Window title
	 * @param description
	 *            Window description
	 */
	public BaseWizardPage(IBaseBlock block, String title, String description) {
		this(block, title, description, null);
		this.block = block;
	}

	/**
	 * Constructs a new instance using a given block.
	 * 
	 * @param block
	 *            Block used to render the screen.
	 * @param title
	 *            Window title
	 * @param description
	 *            Window description
	 * @param contextHelpID
	 *            The context help id used to this page
	 */
	public BaseWizardPage(IBaseBlock block, String title, String description, String contextHelpID) {
		super(title);
		setTitle(title);
		setDescription(description);
		this.block = block;
		helpID = contextHelpID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		getComposite(parent).setLayout(getLayout());
		if (this.block != null) {
			this.block.setShell(getShell());
			Composite blockComposite = this.block.createContent(getComposite(parent));
			if (helpID != null) {
				PlatformUI.getWorkbench().getHelpSystem().setHelp(blockComposite, helpID);
				PlatformUI.getWorkbench().getHelpSystem().setHelp(getComposite(parent), helpID);
			}
		}
		setControl(getComposite(parent));
		setErrorMessage(null);
		getComposite(parent).addListener(SWT.Modify, this);
		getComposite(parent).addListener(SWT.Selection, this);
	}

	/**
	 * Returns the default composite of this wizard page.
	 * 
	 * @param parent
	 *            The parent composite.
	 * @return The composite of this wizard page.
	 */
	protected Composite getComposite(Composite parent) {
		if (this.composite == null) {
			this.composite = WidgetsFactory.createComposite(parent);
		}
		return this.composite;
	}

	/**
	 * Returns the default grid layout of this wizard page.
	 * 
	 * @return The default grid layout of this wizard page.
	 */
	protected GridLayout getLayout() {
		if (this.layout == null) {
			this.layout = WidgetsFactory.createGridLayout();
		}
		return this.layout;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.
	 * Event)
	 */
	@Override
	public void handleEvent(Event event) {
		String message = this.block.getErrorMessage();
		setErrorMessage(message);
		setPageComplete(message == null);
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
		return this.block.isPageComplete();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#canFlipToNextPage()
	 */
	@Override
	public boolean canFlipToNextPage() {
		if (this.block == null) {
			return true;
		}
		return this.block.canFlipToNextPage() && (getNextPage() != null);
	}
}
