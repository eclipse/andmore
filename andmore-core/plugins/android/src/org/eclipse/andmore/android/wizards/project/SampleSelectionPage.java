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

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.Sample;
import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.model.AndroidProject;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;

/**
 * Sample Selection Wizard Page.
 */
public class SampleSelectionPage extends WizardPage {
	private TreeViewer treeViewer;

	final private AndroidProject project;

	private Sample selection = null;

	private final String NEW_PROJECT_SAMPLE_HELP = AndroidPlugin.PLUGIN_ID + ".newproj";

	/**
	 * Constructor
	 * 
	 * @param project
	 */
	public SampleSelectionPage(AndroidProject project) {
		super(AndroidNLS.UI_SampleSelectionPage_TitleSourcePage);
		this.project = project;
		setTitle(AndroidNLS.UI_SampleSelectionPage_WizardTitle);
		setDescription(AndroidNLS.UI_SampleSelectionPage_WizardDescription);
		setPageComplete(true);
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
		initializeDialogUnits(parent);
		Composite mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayout(new GridLayout());
		mainComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		mainComposite.setLayout(new GridLayout());

		// Samples Tree Label
		Label itemsTableLabel = new Label(mainComposite, SWT.NONE);
		itemsTableLabel.setText(AndroidNLS.UI_SampleSelectionPage_SamplesTreeLabel);

		Composite composite = new Composite(mainComposite, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Samples Tree Viewer
		treeViewer = new TreeViewer(composite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
		layoutData.heightHint = 250;
		treeViewer.getControl().setLayoutData(layoutData);
		treeViewer.setContentProvider(new TreeContentProvider(project));
		treeViewer.setLabelProvider(new TreeLabelProvider());
		treeViewer.setInput(SdkUtils.getCurrentSdk());
		treeViewer.expandAll();
		treeViewer.getTree().addSelectionListener(new SamplesSelectionAdapter(this, project));

		setControl(mainComposite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, NEW_PROJECT_SAMPLE_HELP);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			treeViewer.refresh();
			Tree tree = treeViewer.getTree();
			tree.deselectAll();

			if (selection != null) {
				for (TreeItem item : treeViewer.getTree().getItems()) {
					if (item.getData() instanceof Sample) {
						Sample sample = (Sample) item.getData();
						if (sample.getName().equals(selection.getName())) {
							tree.setSelection(item);
							tree.select(item);
							project.setSample(sample);
							setPageComplete(true);
						}
					}
				}
			}
			if ((tree.getSelection().length == 0) && (tree.getItemCount() > 0)) {
				tree.setSelection(tree.getItem(0));
				tree.select(tree.getItem(0));
				project.setSample((Sample) tree.getItem(0).getData());
				setPageComplete(true);
			}
		} else {
			selection = project.getSample();
			setPageComplete(false);
		}
		super.setVisible(visible);
		getContainer().updateButtons();
	}

	// returns native page when selected or main page
	@Override
	public IWizardPage getPreviousPage() {
		return super.getPreviousPage();
	}

}