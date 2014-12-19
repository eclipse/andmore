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

package org.eclipse.andmore.android.wizards.widget;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.preferences.DialogWithToggleUtils;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.model.AndroidProject;
import org.eclipse.andmore.android.model.AndroidProject.SourceTypes;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

import com.android.sdklib.IAndroidTarget;

/**
 * Class that represents the Android New Widget Project Wizard
 */
public class NewAndroidWidgetProjectWizard extends BasicNewProjectResourceWizard implements INewWizard {

	private static final String WIZARD_BANNER = "icons/wizban/widget_provider_prj_wiz.png"; //$NON-NLS-1$

	private final AndroidProject project = new AndroidProject();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	@Override
	public boolean canFinish() {
		return (project.getStatus().getSeverity() != IStatus.ERROR) && !project.needMoreInformation();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard#
	 * performFinish()
	 */
	@Override
	public boolean performFinish() {
		DoSave doSave = new DoSave();

		try {
			getContainer().run(false, false, doSave);
		} catch (InvocationTargetException e) {
			String errMsg = NLS.bind(AndroidNLS.EXC_NewAndroidProjectWizard_AnErrorHasOccurredWhenCreatingTheProject, e
					.getCause().getLocalizedMessage());
			StudioLogger.error(NewAndroidWidgetProjectWizard.class, errMsg, e);

			EclipseUtils.showErrorDialog(AndroidNLS.UI_GenericErrorDialogTitle, errMsg, null);
		} catch (InterruptedException e) {
			String errMsg = NLS.bind(AndroidNLS.EXC_NewAndroidProjectWizard_AnErrorHasOccurredWhenCreatingTheProject,
					e.getLocalizedMessage());
			StudioLogger.error(NewAndroidWidgetProjectWizard.class, errMsg, e);

			EclipseUtils.showErrorDialog(AndroidNLS.UI_GenericErrorDialogTitle, errMsg, null);
		}

		if (doSave.saved) {
			// Collecting usage data for statistical purposes
			try {
				StudioLogger.collectUsageData(UsageDataConstants.WHAT_APP_MANAGEMENT_CREATE,
						UsageDataConstants.KIND_APP_MANAGEMENT, this.project.getSourceType().name().toLowerCase(),
						AndroidPlugin.PLUGIN_ID, AndroidPlugin.getDefault().getBundle().getVersion().toString());
			} catch (Throwable e) {
				// Do nothing, but error on the log should never prevent app
				// from working
			}
		}

		return doSave.saved;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard#init
	 * (org.eclipse.ui.IWorkbench,
	 * org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(AndroidNLS.UI_NewAndroidWidgetProjectWizard_TitleNewProjectWizard);
		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(AndroidPlugin.getImageDescriptor(WIZARD_BANNER));

		// Set project type to widget
		project.setSourceType(SourceTypes.WIDGET);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performCancel()
	 */
	@Override
	public boolean performCancel() {
		try {
			project.finalize();
		} catch (Throwable e) {
			StudioLogger.error(NewAndroidWidgetProjectWizard.class, e.getLocalizedMessage(), e);
		}
		return super.performCancel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard#addPages
	 * ()
	 */
	@Override
	public void addPages() {
		addPage(new NewAndroidWidgetProjectMainPage(project));
	}

	/**
	 * Implements an IRunnableWithProgress to run the save process
	 */
	private class DoSave implements IRunnableWithProgress {
		private static final String OPHONE_JAR = "oms.jar";

		private static final String OPHONESDK_PROMPT_KEY = "OphoneSDK"; //$NON-NLS-1$

		boolean saved = false;

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			// Gets the auto-building configuration to set it back in the end
			final boolean autoBuild = ResourcesPlugin.getWorkspace().isAutoBuilding();

			final IWorkspaceDescription wsd = ResourcesPlugin.getWorkspace().getDescription();
			wsd.setAutoBuilding(false);
			try {
				// Set auto-build off for performance reasons
				ResourcesPlugin.getWorkspace().setDescription(wsd);
			} catch (CoreException e) {
				StudioLogger.error(NewAndroidWidgetProjectWizard.class,
						"Error cleaning workspace after project creation: " + e.getMessage()); //$NON-NLS-1$
			}

			saved = project.save(getContainer(), monitor);
			updatePerspective();

			IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(project.getName());

			addOphoneSDK(p);

			try {
				p.build(IncrementalProjectBuilder.CLEAN_BUILD, new NullProgressMonitor());
			} catch (Exception e1) {
				StudioLogger.error(NewAndroidWidgetProjectWizard.class,
						"Sleep error when cleaning workspace after project creation: " //$NON-NLS-1$
								+ e1.getMessage());
			}

			wsd.setAutoBuilding(autoBuild);
			try {
				// roollback the auto-bulding setting to the original state
				ResourcesPlugin.getWorkspace().setDescription(wsd);
			} catch (CoreException e) {
				StudioLogger.error(NewAndroidWidgetProjectWizard.class,
						"Error cleaning workspace after project creation: " + e.getMessage()); //$NON-NLS-1$
			}

		}

		private void addOphoneSDK(IProject p) {
			IAndroidTarget sdkTarget = project.getSdkTarget();
			File platformLocation = new File(sdkTarget.getLocation());
			File[] listFiles = platformLocation.listFiles();

			boolean found = false;
			int i = 0;
			File file = null;
			while (!found && (i < listFiles.length)) {
				file = listFiles[i];
				if (file.getName().equals(OPHONE_JAR)) {
					found = true;
				}
				i++;
			}

			if (found) {
				boolean addClasspath = DialogWithToggleUtils.showQuestion(OPHONESDK_PROMPT_KEY,
						AndroidNLS.NewAndroidProjectWizard_OPhonePromptTitle,
						AndroidNLS.NewAndroidProjectWizard_OPhonePromptMessage);

				if (addClasspath) {
					IJavaProject javaProject = JavaCore.create(p);
					if ((javaProject != null) && javaProject.exists()) {
						try {
							javaProject.open(new NullProgressMonitor());
							IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
							IClasspathEntry[] newClasspath = new IClasspathEntry[rawClasspath.length + 1];

							System.arraycopy(rawClasspath, 0, newClasspath, 0, rawClasspath.length);
							newClasspath[newClasspath.length - 1] = JavaCore.newLibraryEntry(
									new Path(file.getAbsolutePath()), null, null);
							javaProject.setRawClasspath(newClasspath, new NullProgressMonitor());
						} catch (JavaModelException e) {
							StudioLogger.error(NewAndroidWidgetProjectWizard.class,
									"Error while setting up the oms.jar on the project classpath: " //$NON-NLS-1$
											+ e.getMessage());
						}
					}
				}
			}
		}
	}

}
