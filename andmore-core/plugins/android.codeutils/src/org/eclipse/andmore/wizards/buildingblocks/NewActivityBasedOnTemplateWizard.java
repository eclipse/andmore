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

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.model.ActivityBasedOnTemplate;
import org.eclipse.andmore.android.model.BuildingBlockModel;
import org.eclipse.andmore.android.model.IDatabaseSampleActivityParametersWizardCollector;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkbench;

/**
 * Class that implements the Activity Wizard
 */
public class NewActivityBasedOnTemplateWizard extends NewBuildingBlocksWizard {
	private static final String WIZBAN_ICON = "icons/wizban/new_activity_template_wiz.png"; //$NON-NLS-1$

	private final ActivityBasedOnTemplate activity = new ActivityBasedOnTemplate();

	/*
	 * IRunnableWithProgress object to create the activity
	 */
	private class DoSave implements IRunnableWithProgress {
		AndroidException exception = null;

		boolean saved = false;

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			try {
				saved = getBuildingBlock().save(getContainer(), monitor);
				getBuildingBlock().getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
				getBuildingBlock().getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
			} catch (CoreException ce) {
				// build failed - show a warning message
				StudioLogger.error(this.getClass(), ce.getMessage(), ce);
				EclipseUtils.showWarningDialog(CodeUtilsNLS.UI_NewActivityWizard_TitleNewActivityWizard,
						CodeUtilsNLS.NewActivityWizard_MessageSomeProblemsOccurredWhileBuildingProject);
			} catch (AndroidException e) {
				exception = e;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	@Override
	public boolean canFinish() {
		return (!activity.needMoreInformation())
				&& !(getContainer().getCurrentPage() instanceof ActivitySampleSelectionPage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		boolean saved = false;

		try {
			DoSave doSave = new DoSave();

			getContainer().run(false, false, doSave);

			if (doSave.exception != null) {
				throw doSave.exception;
			} else {
				saved = doSave.saved;
			}
		} catch (Exception e) {
			IStatus status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID, e.getLocalizedMessage());
			EclipseUtils.showErrorDialog(CodeUtilsNLS.UI_GenericErrorDialogTitle,
					CodeUtilsNLS.ERR_BuildingBlockCreation_ErrorMessage, status);
		}

		if (saved) {
			ICompilationUnit javaFile = getBuildingBlock().getPackageFragment().getCompilationUnit(
					getBuildingBlock().getName() + ".java"); //$NON-NLS-1$

			if ((javaFile != null) && javaFile.exists()) {
				try {
					JavaUI.openInEditor(javaFile);
				} catch (Exception e) {
					// Do nothing
					StudioLogger.error(NewActivityBasedOnTemplateWizard.class, "Could not open the activity " //$NON-NLS-1$
							+ getBuildingBlock().getName() + " on an editor.", e); //$NON-NLS-1$
				}
			}
		}

		if (saved) {
			// Collecting usage data for statistical purposes
			try {
				StudioLogger.collectUsageData(UsageDataConstants.WHAT_BUILDINGBLOCK_ACTIVITY,
						UsageDataConstants.KIND_BUILDINGBLOCK, UsageDataConstants.DESCRIPTION_DEFAULT,
						CodeUtilsActivator.PLUGIN_ID, CodeUtilsActivator.getDefault().getBundle().getVersion()
								.toString());
			} catch (Throwable e) {
				// Do nothing, but error on the log should never prevent app
				// from working
			}
		}
		return saved;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
	 * org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(CodeUtilsNLS.UI_NewActivityWizard_TitleNewActivityBasedOnTemplateWizard);
		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(CodeUtilsActivator.getImageDescriptor(WIZBAN_ICON));
		activity.configure(selection);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		addPage(new ActivitySampleSelectionPage(activity));
		addPage(new NewActivityBasedOnTemplatePage(activity));
		IDatabaseSampleActivityParametersWizardCollector collector = activity
				.getDatabaseSampleActivityParametersWizardCollector();
		if (collector != null) {
			List<IWizardPage> contributedPageList = collector.getWizardPages();
			if (contributedPageList != null) {
				for (IWizardPage page : contributedPageList) {
					if (page instanceof NewBuildingBlocksWizardPage) {
						// there is page to select parameters for activity
						// creation
						NewBuildingBlocksWizardPage buildBlockPage = (NewBuildingBlocksWizardPage) page;
						buildBlockPage.setBuildBlock(activity);
						addPage(buildBlockPage);
					}
				}
			}
		}
		addPage(new NewActivityWizardListTemplatesPage(activity));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.wizards.buildingblocks.NewBuildingBlocksWizard
	 * #getBuildingBlock()
	 */
	@Override
	protected BuildingBlockModel getBuildingBlock() {
		return activity;
	}
}
