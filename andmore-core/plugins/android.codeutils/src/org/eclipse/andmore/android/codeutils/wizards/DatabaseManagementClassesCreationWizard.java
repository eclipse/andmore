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

package org.eclipse.andmore.android.codeutils.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.db.utils.DatabaseUtils;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.preferences.DialogWithToggleUtils;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * 
 * Wizard to create classes which aids the database management.
 *
 */
public class DatabaseManagementClassesCreationWizard extends Wizard {

	private static final String SDK_SWITCH_PERSPECTIVE_TO_MOTODEV_STUDIO_ANDROID_KEY = "switch.perspective.to.motodevstudioandroid"; //$NON-NLS-1$

	private static final String SDK_SWITCH_PERSPECTIVE_TO_JAVA_KEY = "switch.perspective.to.java"; //$NON-NLS-1$

	/**
	 * This listener generates Database Management classes. It has the
	 * characteristic of displaying a progress bar.
	 */
	private final class GenerateDatabaseManagementClassesRunnableWithProgress implements IRunnableWithProgress {
		private final IProject project;

		private final IPath databaseFilePath;

		private final boolean isCreateSQLOpenHelper;

		private final boolean isCreateContentProviders;

		private final String sqlOpenHelperPackageName;

		private final String contentProvidersPackageName;

		private final String sqlOpenHelperClassName;

		private final boolean isOverrideContentProviders;

		/**
		 * Constructor for assigning values
		 * 
		 * @param project
		 *            Target Project
		 * @param databaseFilePath
		 *            Database file path
		 * @param isCreateSQLOpenHelper
		 *            Creates or not SQL Open Helper class
		 * @param isCreateContentProviders
		 *            Creates or not Content Provider classes
		 * @param sqlOpenHelperPackageName
		 *            SQL Open Helper Package Name
		 * @param contentProvidersPackageName
		 *            Content Provider classes package name
		 * @param sqlOpenHelperClassName
		 *            SQL Open Helper class Name
		 * @param isOverrideContentProviders
		 *            Overrides or not existing Content Provider classes
		 */
		public GenerateDatabaseManagementClassesRunnableWithProgress(IProject project, IPath databaseFilePath,
				boolean isCreateSQLOpenHelper, boolean isCreateContentProviders, String sqlOpenHelperPackageName,
				String contentProvidersPackageName, String sqlOpenHelperClassName, boolean isOverrideContentProviders) {
			this.project = project;
			this.databaseFilePath = databaseFilePath;
			this.isCreateSQLOpenHelper = isCreateSQLOpenHelper;
			this.isCreateContentProviders = isCreateContentProviders;
			this.sqlOpenHelperPackageName = sqlOpenHelperPackageName;
			this.contentProvidersPackageName = contentProvidersPackageName;
			this.sqlOpenHelperClassName = sqlOpenHelperClassName;
			this.isOverrideContentProviders = isOverrideContentProviders;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse
		 * .core.runtime.IProgressMonitor)
		 */
		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			// create Database Management classes
			try {
				// sub monitor
				SubMonitor subMonitor = SubMonitor.convert(monitor, 10);

				// copy Database file to assets folder, in case it is not
				// already there
				DatabaseUtils.copyDatabaseFileToAssetsFolder(databaseFilePath, project, subMonitor.newChild(4));

				DatabaseUtils.createDatabaseManagementClasses(project, databaseFilePath.toFile().getName(),
						isCreateSQLOpenHelper, isCreateContentProviders, sqlOpenHelperPackageName,
						contentProvidersPackageName, sqlOpenHelperClassName, isOverrideContentProviders, false,
						subMonitor.newChild(6), true);
			} catch (Exception e) {
				StudioLogger.error(DatabaseManagementClassesCreationWizard.class,
						CodeUtilsNLS.DATABASE_DEPLOY_ERROR_DEPLOYING_DATABASE, e);
				IStatus status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID, e.getLocalizedMessage());
				EclipseUtils.showErrorDialog(CodeUtilsNLS.DATABASE_DEPLOY_ERROR_DEPLOYING_DATABASE,
						CodeUtilsNLS.DATABASE_DEPLOY_ERROR_DEPLOYING_DATABASE, status);
			}

		}
	}

	// deployment page
	private DatabaseManagementClassesCreationMainPage page = null;

	public DatabaseManagementClassesCreationWizard(IProject selectedProject, IResource selectedDatabase) {
		// set title
		setWindowTitle(CodeUtilsNLS.UI_PersistenceWizardPageTitleDeploy);
		// set properties
		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(CodeUtilsActivator.PLUGIN_ID,
				"icons/wizban/create_management_classes.png")); //$NON-NLS-1$
		// set page
		this.page = new DatabaseManagementClassesCreationMainPage("Main Page", selectedProject, //$NON-NLS-1$
				selectedDatabase);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		addPage(this.page);
	}

	/**
	 * Generate the requested classes and finishes this wizard.
	 */
	@Override
	public boolean performFinish() {
		// get project
		IProject project = page.getSelectedProject();
		// get the file name
		IPath databaseFilePath = page.getDatabaseFilePath();
		// get package of content providers classes
		String contentProvidersPackageName = page.getContentProviderPackage();
		// get the package of SQL Open Helper
		String sqlOpenHelperPackageName = page.getDatabaseOpenHelperPackage();
		// get the SQL Open Helper class name
		String sqlOpenHelperClassName = page.getSQLOpenHelperClassName();
		// create or no open helper
		boolean isCreateSQLOpenHelper = page.isCreateSQLOpenHelperClass();
		// create or not content providers
		boolean isCreateContentProviders = page.isCreateContentProviders();
		// override or not content providers
		boolean isOverrideContentProviders = page.isOverrideExistingContentProviderClasses();

		try {
			// create database management classes
			getContainer().run(
					true,
					true,
					new GenerateDatabaseManagementClassesRunnableWithProgress(project, databaseFilePath,
							isCreateSQLOpenHelper, isCreateContentProviders, sqlOpenHelperPackageName,
							contentProvidersPackageName, sqlOpenHelperClassName, isOverrideContentProviders));
		} catch (Exception e) {
			StudioLogger.error(DatabaseManagementClassesCreationWizard.class,
					CodeUtilsNLS.DATABASE_DEPLOY_ERROR_DEPLOYING_DATABASE, e);
			IStatus status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID, e.getLocalizedMessage());
			EclipseUtils.showErrorDialog(CodeUtilsNLS.DATABASE_DEPLOY_ERROR_DEPLOYING_DATABASE,
					CodeUtilsNLS.DATABASE_DEPLOY_ERROR_DEPLOYING_DATABASE, status);
		}

		try {

			if ((PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null)
					&& (PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null)) {
				if (PlatformUI.getWorkbench().getPerspectiveRegistry()
						.findPerspectiveWithId(CodeUtilsActivator.PERSPECTIVE_ID) != null) {
					if (!PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getPerspective().getId()
							.equals(CodeUtilsActivator.PERSPECTIVE_ID)) {

						// ask users if they want to change perspective to check
						// files created
						if (DialogWithToggleUtils
								.showQuestion(
										SDK_SWITCH_PERSPECTIVE_TO_MOTODEV_STUDIO_ANDROID_KEY,
										CodeUtilsNLS.UI_PersistenceWizard_ChangePerspectiveToMOTODEVStudioAndroid_DialogTitle,
										CodeUtilsNLS.UI_PersistenceWizard_ChangePerspectiveToMOTODEVStudioAndroid_DialogMessage)) {
							PlatformUI.getWorkbench().showPerspective(CodeUtilsActivator.PERSPECTIVE_ID,
									PlatformUI.getWorkbench().getActiveWorkbenchWindow());
						}
					}
				} else {
					if (DialogWithToggleUtils.showQuestion(SDK_SWITCH_PERSPECTIVE_TO_JAVA_KEY,
							CodeUtilsNLS.UI_PersistenceWizard_ChangePerspectiveToJava_DialogTitle,
							CodeUtilsNLS.UI_PersistenceWizard_ChangePerspectiveToJava_DialogMessage)) {
						PlatformUI.getWorkbench().showPerspective(JavaUI.ID_PERSPECTIVE,
								PlatformUI.getWorkbench().getActiveWorkbenchWindow());
					}
				}
			}
		} catch (WorkbenchException e) {
			StudioLogger.warn("It was not possible to change perspective to " //$NON-NLS-1$
					+ CodeUtilsActivator.PERSPECTIVE_ID);
		}

		// create UDC log for db file size
		StudioLogger.collectUsageData(UsageDataConstants.WHAT_DATABASE_MANAGMT_CLASSES,
				UsageDataConstants.KIND_DATABASE_MANAGMT_CLASSES,
				"Database classes created. Database filesize " + getDatabaseFileSize(databaseFilePath) + ".", //$NON-NLS-1$ //$NON-NLS-2$
				CodeUtilsActivator.PLUGIN_ID, CodeUtilsActivator.getDefault().getBundle().getVersion().toString());

		return true;
	}

	/**
	 * Returns a database file size string in Kb, Mb or Gb. Example: '10 Kb'.
	 * 
	 * @param databaseFilePath
	 *            The database file path.
	 * @return String containing the database file size.
	 */
	private String getDatabaseFileSize(IPath databaseFilePath) {

		String fileSizeStr = null;

		long fileSize = databaseFilePath.toFile().length();

		long KBsize = fileSize / 1024;

		if (KBsize > 1024) {

			long MBSize = KBsize / 1024;

			if (MBSize > 1024) {

				long GBSize = MBSize / 1024;

				fileSizeStr = GBSize + " Gb"; //$NON-NLS-1$

			} else {
				fileSizeStr = MBSize + " Mb"; //$NON-NLS-1$

			}
		} else {
			fileSizeStr = KBsize + " Kb"; //$NON-NLS-1$

		}

		return fileSizeStr;
	}

}
