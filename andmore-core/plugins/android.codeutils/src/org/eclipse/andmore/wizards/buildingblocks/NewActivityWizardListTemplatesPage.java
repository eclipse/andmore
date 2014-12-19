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

package org.eclipse.andmore.wizards.buildingblocks;

import java.net.URL;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.utilities.AndroidUtils;
import org.eclipse.andmore.android.model.ActivityBasedOnTemplate;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * New activity wizard sample page.
 */
public class NewActivityWizardListTemplatesPage extends NewLauncherWizardPage {

	private TreeViewer treeViewer;

	private Label imgLabel;

	private Label descriptionLabel;

	private String content[];

	private static final String NEW_ACTIVITY_HELP = CodeUtilsActivator.PLUGIN_ID + ".newactivity";

	private static Image androidImg = null;

	public static final String PAGE_NAME = "List Activities Page";

	// private boolean canFlip = false;

	/*
	 * Listener to update description pane whenever this page is open
	 */
	private class PageChangeListener implements IPageChangedListener {
		@Override
		public void pageChanged(PageChangedEvent event) {
			if ((event.getSelectedPage() == NewActivityWizardListTemplatesPage.this)) {
				if (!treeViewer.getSelection().isEmpty()) {
					updateTreeViewAfterSelection(treeViewer.getSelection());
				}

				NewActivityWizardListTemplatesPage.this.getControl().update();
				((NewActivityWizardListTemplatesPage) event.getSelectedPage()).updateDescriptionPane();
			}
		}
	}

	protected NewActivityWizardListTemplatesPage(ActivityBasedOnTemplate activity) {
		super(activity, PAGE_NAME);

		activity.evaluateSamplesList(ActivityBasedOnTemplate.SAMPLE_CATEGORY.LIST_ACTIVITIES_CATEGORY);

		ImageDescriptor descr = AbstractUIPlugin.imageDescriptorFromPlugin(CodeUtilsActivator.PLUGIN_ID,
				"icons/device_refresh_on.png");
		androidImg = descr.createImage();
	}

	/*
	 * (non-Javadoc)
	 */
	@Override
	public boolean canFlipToNextPage() {
		return getBuildBlock().isListActivitySelected();
	}

	@Override
	public IWizardPage getNextPage() {
		return this.getWizard().getPage(CodeUtilsNLS.UI_NewActivityMainPage_PageTitle);
	}

	@Override
	public IWizardPage getPreviousPage() {
		return this.getWizard().getPage(ActivitySampleSelectionPage.PAGE_NAME);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.wizards.buildingblocks.NewBuildingBlocksWizard
	 * #getBuildBlock()
	 */
	@Override
	public ActivityBasedOnTemplate getBuildBlock() {
		return (ActivityBasedOnTemplate) super.getBuildBlock();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getMethods()
	 */
	@Override
	protected Method[] getMethods() {
		Method onCreateMethod = new Method(getBuildBlock().getOnStartMessage()) {
			@Override
			public void handle(boolean selection) {
				getBuildBlock().setOnStart(selection);
			}
		};
		return new Method[] { onCreateMethod };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage
	 * #createIntermediateControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createExtendedControls(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.FILL);
		mainComposite.setLayout(new GridLayout(2, false));
		mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Samples Tree Label
		Label itemsTableLabel = new Label(mainComposite, SWT.NONE);
		itemsTableLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
		itemsTableLabel.setText(CodeUtilsNLS.UI_SampleSelectionPage_SamplesTreeLabel);

		// Samples Tree Viewer
		treeViewer = new TreeViewer(mainComposite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		treeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		treeViewer.setLabelProvider(new LabelProvider() {
			@Override
			public Image getImage(Object obj) {
				return androidImg;
			}

			@Override
			public String getText(Object element) {
				return element.toString();
			}
		});

		content = new String[getBuildBlock().getListActivitiesSamples().size()];

		int i = 0;
		for (String currentSample : getBuildBlock().getListActivitiesSamples().keySet()) {
			content[i] = currentSample;
			i++;
		}

		// sets tree content and icon
		treeViewer.setContentProvider(new SampleTreeContentProvider(content));
		treeViewer.setInput(content);

		final Group previewGroup = new Group(mainComposite, SWT.NONE);
		previewGroup.setText(CodeUtilsNLS.UI_ListActivityPage_Preview);
		previewGroup.setLayout(new GridLayout(1, false));
		previewGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true, 1, 1));

		imgLabel = new Label(previewGroup, SWT.NONE);
		imgLabel.setImage(null);

		GridData imageLabelData = new GridData(GridData.FILL, GridData.FILL, true, true);
		imageLabelData.widthHint = 200;
		imgLabel.setLayoutData(imageLabelData);

		final Group descriptionGroup = new Group(mainComposite, SWT.NONE);

		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent e) {
				updateTreeViewAfterSelection(e.getSelection());

				updateDescriptionPane();
				getWizard().getContainer().updateButtons();
			}
		});

		// sort tree
		treeViewer.setComparator(new ViewerComparator());
		treeViewer.expandAll();

		// description pane
		descriptionGroup.setText(CodeUtilsNLS.UI_SampleSelectionPage_SamplesDescriptionPane);
		descriptionGroup.setLayout(new GridLayout(1, false));
		descriptionGroup.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1));

		ScrolledComposite scroll = new ScrolledComposite(descriptionGroup, SWT.V_SCROLL);
		GridData scrollData = new GridData(GridData.FILL, GridData.FILL, true, true);
		scroll.setLayoutData(scrollData);
		scroll.setMinSize(100, 140);

		descriptionLabel = new Label(scroll, SWT.FILL | SWT.WRAP);
		descriptionLabel.setText("");

		scroll.setContent(descriptionLabel);

		// Add a listener to the wizard to listen for page changes
		if (getContainer() instanceof IPageChangeProvider) {
			((IPageChangeProvider) getContainer()).addPageChangedListener(new PageChangeListener());
		}

		setControl(mainComposite);
	}

	private void updateTreeViewAfterSelection(ISelection selection) {
		String template = selection.toString();

		getBuildBlock().setSample(template.substring(1, template.length() - 1));

		// condition to enable finish button
		getBuildBlock().setIsListActivitySelected(true);
		// category of sample, used to load the correct files
		getBuildBlock().setSampleCategoty(ActivityBasedOnTemplate.SAMPLE_CATEGORY.LIST_ACTIVITIES_CATEGORY);

		String strPreview = getBuildBlock().getSamplePreview();
		if (strPreview != null) {
			URL url = null;
			try {
				// loads the selected sample preview image
				url = FileLocator.toFileURL(CodeUtilsActivator.getDefault().getBundle()
						.getEntry(ActivityBasedOnTemplate.ACTIVITY_SAMPLES_FOLDER + IPath.SEPARATOR + strPreview));

				ImageDescriptor imageDesc = ImageDescriptor.createFromURL(url);
				Image image = imageDesc.createImage();

				Image previousImage = imgLabel.getImage();
				if (previousImage != null) {
					previousImage.dispose();
				}

				imgLabel.setImage(image);
			} catch (Exception ex) {
				imgLabel.setImage(null);
			}
		} else {
			imgLabel.setImage(null);
		}
	}

	/*
	 * Updates selected label description
	 */
	private void updateDescriptionPane() {
		if (getBuildBlock().isListActivitySelected()) {
			descriptionLabel.setText(getBuildBlock().getSampleDescription());
		} else {
			descriptionLabel.setText("");
			imgLabel.setImage(null);
		}
		descriptionLabel.update();
		descriptionLabel.setSize(descriptionLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		descriptionLabel.getParent().layout();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getDefaultMessage()
	 */
	@Override
	public String getDefaultMessage() {
		return CodeUtilsNLS.UI_NewActivityMainPage_DescriptionCreateActivityBasedOnTemplate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getWizardTitle()
	 */
	@Override
	public String getWizardTitle() {
		return CodeUtilsNLS.UI_ListActivityPage_TitleWizard;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.wizards.buildingblocks.NewLauncherWizardPage
	 * #getIntentFiltersActions()
	 */
	@Override
	protected String[] getIntentFiltersActions() {
		String[] intentFiltersActions = new String[0];
		try {
			intentFiltersActions = AndroidUtils.getActivityActions(getBuildBlock().getProject());
		} catch (AndroidException e) {
			setErrorMessage(e.getMessage());
		}
		return intentFiltersActions;
	}

	/**
	 * Gets the help ID to be used for attaching context sensitive help.
	 * 
	 * Classes that extends this class and want to set their on help should
	 * override this method
	 */
	@Override
	protected String getHelpId() {
		return NEW_ACTIVITY_HELP;
	}

	/**
	 * Returns true if page has header false otherwise
	 * 
	 * @return true if page has header false otherwise
	 */
	@Override
	public boolean hasHeader() {
		return false;
	}
}
