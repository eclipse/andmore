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

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.utilities.AndroidUtils;
import org.eclipse.andmore.android.model.ActivityBasedOnTemplate;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
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
public class ActivitySampleSelectionPage extends NewLauncherWizardPage {
	private static final String ANDROID_LOGO_ICON_PATH = "icons/obj16/androidLogo.png";

	private TreeViewer treeViewer;

	private Label descriptionLabel;

	private String content[];

	private boolean canFlip = false;

	private static final String NEW_ACTIVITY_BASED_ON_TEMPLATE_HELP = CodeUtilsActivator.PLUGIN_ID
			+ ".new-activity-based-on-template"; //$NON-NLS-1$

	private static Image androidImg = null;

	public static final String PAGE_NAME = "Samples Page";

	/*
	 * Listener to update description pane whenever this page is open
	 */
	private class PageChangeListener implements IPageChangedListener {
		@Override
		public void pageChanged(PageChangedEvent event) {
			if ((event.getSelectedPage() == ActivitySampleSelectionPage.this)) {
				ActivitySampleSelectionPage.this.getControl().update();
				((ActivitySampleSelectionPage) event.getSelectedPage()).updateDescriptionPane();
			}
		}
	}

	/**
	 * Create a new wizard page based on activity samples.
	 * 
	 * @param activity
	 *            The building block model to be used in the wizard page.
	 * */
	protected ActivitySampleSelectionPage(ActivityBasedOnTemplate activity) {
		super(activity, PAGE_NAME);

		activity.evaluateSamplesList(ActivityBasedOnTemplate.SAMPLE_CATEGORY.SAMPLE_ACTIVITIES_CATEGORY);

		ImageDescriptor imgDescr = AbstractUIPlugin.imageDescriptorFromPlugin(CodeUtilsActivator.PLUGIN_ID,
				ANDROID_LOGO_ICON_PATH);
		if (imgDescr != null) {
			androidImg = imgDescr.createImage();
		}
	}

	/*
	 * (non-Javadoc)
	 */
	@Override
	public boolean canFlipToNextPage() {

		return canFlip;
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

	@Override
	public IWizardPage getNextPage() {
		String selection = treeViewer.getSelection() != null ? treeViewer.getSelection().toString() : null;
		selection = selection != null ? selection.substring(1, selection.length() - 1) : null;

		if ((selection != null) && selection.equalsIgnoreCase(ActivityBasedOnTemplate.LIST_ACTIVITIES_SAMPLE_LOCALIZED)) {
			return this.getWizard().getPage(NewActivityWizardListTemplatesPage.PAGE_NAME);
		}
		return this.getWizard().getPage(CodeUtilsNLS.UI_NewActivityMainPage_PageTitle);
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
		mainComposite.setLayout(new GridLayout(1, false));
		mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Samples Tree Label
		Label itemsTableLabel = new Label(mainComposite, SWT.NONE);
		itemsTableLabel.setText(CodeUtilsNLS.UI_SampleSelectionPage_SamplesTreeLabel);

		// Samples Tree Viewer
		treeViewer = new TreeViewer(mainComposite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		treeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
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

		content = new String[getBuildBlock().getAvailableSamples().size()];

		int i = 0;
		for (String currentSample : getBuildBlock().getAvailableSamples().keySet()) {
			content[i] = currentSample;
			i++;
		}

		treeViewer.setContentProvider(new SampleTreeContentProvider(content));
		treeViewer.setInput(content);

		final Group intentFilterGroup = new Group(mainComposite, SWT.NONE);

		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent e) {
				String selection = e.getSelection().toString();
				getBuildBlock().setSample(selection.substring(1, selection.length() - 1));

				getBuildBlock().setSampleCategoty(ActivityBasedOnTemplate.SAMPLE_CATEGORY.SAMPLE_ACTIVITIES_CATEGORY);

				if (selection.substring(1, selection.length() - 1).equals(
						ActivityBasedOnTemplate.DATABASE_LIST_SAMPLE_LOCALIZED)) {
					getBuildBlock().setDatabaseTemplateSelected(true);
				} else {
					getBuildBlock().setDatabaseTemplateSelected(false);
				}

				canFlip = true;

				updateDescriptionPane();
				getWizard().getContainer().updateButtons();
			}
		});

		treeViewer.setComparator(new ViewerComparator());

		treeViewer.expandAll();

		intentFilterGroup.setText(CodeUtilsNLS.UI_SampleSelectionPage_SamplesDescriptionPane);
		intentFilterGroup.setLayout(new GridLayout(1, false));
		intentFilterGroup.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

		ScrolledComposite scrolledArea = new ScrolledComposite(intentFilterGroup, SWT.V_SCROLL);
		GridData descriptionLabelData = new GridData(GridData.FILL, GridData.FILL, true, true);
		descriptionLabelData.heightHint = 140;
		scrolledArea.setLayoutData(descriptionLabelData);

		descriptionLabel = new Label(scrolledArea, SWT.FILL | SWT.WRAP);
		descriptionLabel.setText("");
		scrolledArea.setContent(descriptionLabel);
		descriptionLabel.setSize(descriptionLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		// Add a listener to the wizard to listen for page changes
		if (getContainer() instanceof IPageChangeProvider) {
			((IPageChangeProvider) getContainer()).addPageChangedListener(new PageChangeListener());
		}
		setControl(mainComposite);
	}

	/*
	 * Updates selected label description.
	 */
	private void updateDescriptionPane() {
		descriptionLabel.setText(getBuildBlock().getSampleDescription());
		// descriptionLabel.setVisible(true);
		descriptionLabel.setSize(descriptionLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		descriptionLabel.update();
		descriptionLabel.getParent().update();
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
		return CodeUtilsNLS.UI_NewActivityMainPage_TitleActivityBasedOnTemplate;
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
		return NEW_ACTIVITY_BASED_ON_TEMPLATE_HELP;
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

/**
 * Fills tree viewer with sample options
 */
class SampleTreeContentProvider extends ArrayContentProvider implements ITreeContentProvider {

	Object[] elements;

	public SampleTreeContentProvider(Object[] elements) {
		this.elements = elements;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return elements;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		return new Object[0];
	}

	@Override
	public boolean hasChildren(Object element) {
		return false;
	}
}