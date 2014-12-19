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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.preferences.DialogWithToggleUtils;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.model.AndroidProject;
import org.eclipse.andmore.android.obfuscate.ObfuscatorManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.osgi.framework.Bundle;

import com.android.sdklib.IAndroidTarget;

/**
 * Class that represents the Android New Project Wizard
 */
public class NewAndroidProjectWizard extends BasicNewProjectResourceWizard implements INewWizard {
	private static final String WIZARD_BANNER = "icons/wizban/newprjwiz.png"; //$NON-NLS-1$

	protected static final String NATIVE_PAGE_NAME = "native_page"; //$NON-NLS-1$

	protected static final String SAMPLE_PAGE_NAME = AndroidNLS.UI_SampleSelectionPage_TitleSourcePage;

	private final AndroidProject project = new AndroidProject();

	private WizardPage nativePage = null;

	private NewAndroidProjectMainPage mainPage = null;

	private Class<?> nativePageClass = null;

	private Object classInstance = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */

	@Override
	public boolean canFinish() {
		boolean canFinish = (project.getStatus().getSeverity() != IStatus.ERROR) && !project.needMoreInformation();

		if ((nativePage != null) && !nativePage.isPageComplete() && project.isAddingNativeSupport()) {
			canFinish = false;
		}

		return canFinish;
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
		} catch (Exception e) {
			String errMsg = NLS.bind(AndroidNLS.EXC_NewAndroidProjectWizard_AnErrorHasOccurredWhenCreatingTheProject,
					e.getLocalizedMessage());
			StudioLogger.error(NewAndroidProjectWizard.class, errMsg, e);

			EclipseUtils.showErrorDialog(AndroidNLS.UI_GenericErrorDialogTitle, errMsg, null);
		}
		boolean success = doSave.isSaved();

		if (success) {
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

		return success;
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
			StudioLogger.error(NewAndroidProjectWizard.class, e.getLocalizedMessage(), e);
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

	@SuppressWarnings({ "unchecked" })
	@Override
	public void addPages() {

		classInstance = null;

		try {
			Bundle seqBundle = Platform.getBundle("org.eclipse.sequoyah.android.cdt.build.ui"); //$NON-NLS-1$
			if (seqBundle != null) {
				nativePageClass = seqBundle
						.loadClass("org.eclipse.sequoyah.android.cdt.internal.build.ui.AddNativeProjectPage"); //$NON-NLS-1$

				Class<Boolean> bClass = boolean.class;
				Class[] paramTypes = { bClass };

				Constructor cs = nativePageClass.getConstructor(paramTypes);
				classInstance = cs.newInstance(true);
				nativePage = (WizardPage) classInstance;
			}
		} catch (Exception e) {
			StudioLogger.error(NewAndroidProjectWizard.class, e.getMessage(), e);
		}

		if (nativePage != null) {
			mainPage = new NewAndroidProjectMainPage(project, true);
			addPage(mainPage);
			addPage(nativePage);
		} else {
			mainPage = new NewAndroidProjectMainPage(project, false);
			addPage(mainPage);
		}
		addPage(new SampleSelectionPage(project));

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
		setWindowTitle(AndroidNLS.UI_NewAndroidProjectWizard_TitleNewProjectWizard);
		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(AndroidPlugin.getImageDescriptor(WIZARD_BANNER));
	}

	/**
	 * Implements an IRunnableWithProgress to run the save process
	 */
	private class DoSave implements IRunnableWithProgress {
		private static final String OPHONE_JAR = "oms.jar"; //$NON-NLS-1$

		private static final String OPHONESDK_PROMPT_KEY = "OphoneSDK"; //$NON-NLS-1$

		private boolean saved = false;

		/**
		 * Returns whether the project was saved/created successfuly.
		 * 
		 * @return Returns <code>true</code> in case the project is
		 *         saved/creates successfully.
		 */
		public boolean isSaved() {
			return saved;
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			SubMonitor subMonitor = SubMonitor.convert(monitor, 20);

			subMonitor.beginTask(AndroidNLS.NewAndroidProjectWizard_Message_CreatingAndroidProject, 10);

			// Gets the auto-building configuration to set it back in the end
			final boolean autoBuild = ResourcesPlugin.getWorkspace().isAutoBuilding();

			final IWorkspaceDescription wsd = ResourcesPlugin.getWorkspace().getDescription();
			wsd.setAutoBuilding(false);
			try {
				// Set auto-build off for performance reasons
				ResourcesPlugin.getWorkspace().setDescription(wsd);
			} catch (CoreException e) {
				// there is no need to stop the process because auto-build only
				// improves performance, it does not interferes with the new
				// project creation.
				StudioLogger.error(NewAndroidProjectWizard.class,
						"Error cleaning workspace after project creation: " + e.getMessage()); //$NON-NLS-1$
			}

			// worked 1
			subMonitor.worked(1);

			saved = project.save(getContainer(), subMonitor);
			updatePerspective();

			IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(project.getName());

			addOphoneSDK(newProject, subMonitor);

			try {
				newProject.build(IncrementalProjectBuilder.CLEAN_BUILD, subMonitor);
			} catch (Exception e1) {
				// even if the build fais, the project could still be created,
				// therefore it must continue
				StudioLogger.error(NewAndroidProjectWizard.class,
						"Sleep error when cleaning workspace after project creation: " //$NON-NLS-1$
								+ e1.getMessage());
			}

			// worked 4
			subMonitor.worked(3);

			wsd.setAutoBuilding(autoBuild);
			try {
				// rollback the auto-building setting to the original state
				ResourcesPlugin.getWorkspace().setDescription(wsd);
			} catch (CoreException e) {
				// the auto-building does not interfere with the project
				// creation, therefore in case it fails, the process may contine
				StudioLogger.error(NewAndroidProjectWizard.class,
						"Error cleaning workspace after project creation: " + e.getMessage()); //$NON-NLS-1$
			}

			// worked 5
			subMonitor.worked(1);

			if ((nativePage != null) && project.isAddingNativeSupport() && saved) {
				try {
					Class<IWorkbenchWindow> workbenchClass = IWorkbenchWindow.class;
					Class<IProject> projectClass = IProject.class;
					Class<IProgressMonitor> progressMonitorClass = IProgressMonitor.class;

					IProject createdProject = ResourcesPlugin.getWorkspace().getRoot().getProject(project.getName());
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

					QualifiedName libQN = new QualifiedName(AndroidPlugin.PLUGIN_ID,
							AndroidPlugin.LIB_LOCATION_PROPERTY);

					// get library name field from wizard
					Method getLibraryName = nativePageClass.getMethod("getLibraryName", (Class[]) null); //$NON-NLS-1$
					getLibraryName.setAccessible(true);
					Object returnValue = getLibraryName.invoke(classInstance, (Object[]) null);

					// set project library name property
					createdProject.setPersistentProperty(libQN, returnValue.toString());

					// worked 6
					subMonitor.worked(1);

					Object[] performFinishMethodArguments = { window, createdProject, subMonitor.newChild(4) };

					Class<?>[] performFinishMethodParameterTypes = { workbenchClass, projectClass, progressMonitorClass };

					// invoke page perform finish that will add native support
					// to the brand new project
					Method performFinish = nativePageClass.getMethod("performFinish", //$NON-NLS-1$
							performFinishMethodParameterTypes);
					performFinish.setAccessible(true);

					returnValue = performFinish.invoke(classInstance, performFinishMethodArguments);

					// update success flag
					saved = saved && (Boolean) returnValue;
				} catch (Exception e) {
					// the project may be in a inconsistent state - throw an
					// exception
					saved = false;
					StudioLogger.error(NewAndroidProjectWizard.class, e.getMessage(), e);
					throw new InvocationTargetException(e);
				}
			}

			// add proguard file inside project
			try {
				if (project.needToObfuscate()) {
					ObfuscatorManager.obfuscate(newProject, subMonitor.newChild(10));
				}
				newProject.refreshLocal(IResource.DEPTH_INFINITE, subMonitor);
			} catch (Exception e) {
				StudioLogger.error(NewAndroidProjectWizard.class, e.getMessage(), e);
				throw new InvocationTargetException(e);
			}
		}

		private void addOphoneSDK(IProject p, IProgressMonitor monitor) {
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
							javaProject.open(monitor);
							IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
							IClasspathEntry[] newClasspath = new IClasspathEntry[rawClasspath.length + 1];

							System.arraycopy(rawClasspath, 0, newClasspath, 0, rawClasspath.length);
							newClasspath[newClasspath.length - 1] = JavaCore.newLibraryEntry(
									new Path(file.getAbsolutePath()), null, null);
							javaProject.setRawClasspath(newClasspath, monitor);
						} catch (JavaModelException e) {
							StudioLogger.error(NewAndroidProjectWizard.class,
									"Error while setting up the oms.jar on the project classpath: " //$NON-NLS-1$
											+ e.getMessage());
						}
					}
				}
			}
		}
	}
}
