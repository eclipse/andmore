/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Vinicius Hernandes (Motorola)
 * Matheus Tait Lima (Eldorado)
 * 
 * Contributors:
 * Marcelo Marzola Bossoni (Eldorado) - Bug [289146] - Performance and Usability Issues
 * Marcelo Marzola Bossoni (Eldorado) - Bug (289236) - Editor Permitting create 2 columns with same id
 * Marcelo Marzola Bossoni (Eldorado) - Bug (289282) - NullPointer adding keyNullPointer adding key
 *  * Vinicius Rigoni Hernandes (Eldorado) - Bug [289885] - Localization Editor doesn't recognize external file changes
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.managers;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.localization.tools.datamodel.LocaleInfo;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFile;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFileBean;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationProject;
import org.eclipse.sequoyah.localization.tools.datamodel.StringLocalizationFile;
import org.eclipse.sequoyah.localization.tools.datamodel.node.GrammarCheckerResult;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringNode;
import org.eclipse.sequoyah.localization.tools.extensions.classes.ILocalizationSchema;
import org.eclipse.sequoyah.localization.tools.persistence.ProjectPersistenceManager;
import org.eclipse.ui.PlatformUI;

/**
 * This manager is instantiated on demand and is responsible for one specific
 * Project
 * 
 * It provides methods to make operations related with the localization files of
 * that Project
 */
public class ProjectLocalizationManager {

	/*
	 * The project that this manager is responsible for
	 */
	private LocalizationProject localizationProject;

	/*
	 * The Persistence Manager for the project
	 */
	private ProjectPersistenceManager projectPersistenceManager;

	/*
	 * The Preferences Manager for the project
	 */
	private ProjectPreferencesManager projectPreferencesManager;

	/*
	 * The localization schema related to the project, based on its nature
	 */
	private ILocalizationSchema projectLocalizationSchema;

	/*
	 * The project being managed by this manager
	 */
	private IProject project;

	/**
	 * Constructor
	 */
	public ProjectLocalizationManager(IProject project,
			ILocalizationSchema localizationSchema) {

		this.projectPersistenceManager = new ProjectPersistenceManager(
				this.localizationProject);
		this.projectPreferencesManager = new ProjectPreferencesManager(project);
		this.projectLocalizationSchema = localizationSchema;
		try {
			List<LocalizationFile> localizationFiles = new ArrayList<LocalizationFile>();

			localizationFiles.addAll(this.projectLocalizationSchema
					.loadAllFiles(project).values());
			this.localizationProject = new LocalizationProject(project,
					localizationFiles);
			this.project = project;
			syncDefaultColumn();

		} catch (SequoyahException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void reload(IProject project) {
		List<LocalizationFile> localizationFiles = new ArrayList<LocalizationFile>();
		List<LocalizationFile> notPersisted = new ArrayList<LocalizationFile>();
		Map<IFile, LocalizationFile> localizationFilesMap = new HashMap<IFile, LocalizationFile>();

		boolean createLocalizationProject = false;
		if (getLocalizationProject() != null) {
			notPersisted = getLocalizationProject().getLocalizationFiles();
		} else {
			// manager is loaded but location project is not updated
			createLocalizationProject = true;
		}

		try {
			Collection<LocalizationFile> loadedLocationFiles = this.projectLocalizationSchema
					.loadAllFiles(project).values();
			if (createLocalizationProject) {

			}
			localizationFiles.addAll(loadedLocationFiles);
			for (LocalizationFile file : localizationFiles) {
				localizationFilesMap.put(file.getFile(), file);
			}

			for (LocalizationFile file : notPersisted) {
				if (localizationFilesMap.get(file.getFile()) == null) {
					localizationFiles.add(file);
				}
			}

			this.localizationProject.getLocalizationFiles().clear();
			for (LocalizationFile file : localizationFiles) {
				localizationProject.addLocalizationFile(file);
			}
			syncDefaultColumn();
		} catch (SequoyahException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// add missing string nodes from source to destination
	private void syncNodes(StringLocalizationFile destination,
			StringLocalizationFile source) {
		for (StringNode node : source.getStringNodes()) {
			destination.getStringNodeByKey(node.getKey());
		}
	}

	// ensure that all keys exists within the default columns
	private void syncDefaultColumn() {
		LocalizationFile mainFile = localizationProject
				.getLocalizationFile(getProjectLocalizationSchema()
						.getLocaleInfoFromID(
								getProjectLocalizationSchema().getDefaultID()));
		if (mainFile != null) {
			for (LocalizationFile locFile : localizationProject
					.getLocalizationFiles()) {
				if (locFile != mainFile) {
					syncNodes((StringLocalizationFile) mainFile,
							(StringLocalizationFile) locFile);
				}
			}
		}
	}

	/**
	 * Get the full list of locales that are available in this project
	 * 
	 * @return a full list of locales that are available in this project
	 */
	public List<LocaleInfo> getAvailableLocales() {

		List<LocaleInfo> localeInfoList = new ArrayList<LocaleInfo>();

		List<LocalizationFile> localizationFiles = localizationProject
				.getLocalizationFiles();
		for (LocalizationFile localizationFile : localizationFiles) {
			localeInfoList.add(localizationFile.getLocaleInfo());
		}

		return localeInfoList;
	}

	/**
	 * Create a new localization file containing the string nodes passed as a
	 * parameter
	 * 
	 * The new localization file refers to a locale also passed as a parameter
	 * 
	 * @param bean
	 */
	public boolean createOrUpdateFile(LocalizationFileBean bean) {

		LocalizationFile localizationFile = getProjectLocalizationSchema()
				.createLocalizationFile(bean);

		projectLocalizationSchema.createStringFile(localizationFile);

		return true;
	}

	/**
	 * Persist all changes in the localization files, as well as any meta or
	 * extra-data associated with any localization files inside this project
	 */
	public boolean saveProject() {
		syncDefaultColumn();
		List<LocalizationFile> localizationFiles = localizationProject
				.getLocalizationFiles();

		for (LocalizationFile localizationFile : localizationFiles) {

			// Check if the file is not to be deleted
			if (!localizationFile.isToBeDeleted()) {

				/*
				 * Persist localization files
				 */
				if (localizationFile.isDirty()) {
					try {
						projectLocalizationSchema.updateFile(localizationFile);
					} catch (SequoyahException e) {
						BasePlugin.logInfo("Error while updating file"); //$NON-NLS-1$
					}
				}
				/*
				 * Persist extra/meta-data
				 */
				if (localizationFile.isDirtyMetaExtraData()) {
					projectPersistenceManager.saveData();
				}

			} else {
				try {

					refreshWorkspace();

					// delete the file on file system
					localizationFile.getFile().delete(true, null);

					// If the parent folder is empty, also delete the folder

					if (localizationFile.getFile().getLocation() != null) {
						File tempFile = new File(localizationFile.getFile()
								.getLocation().removeLastSegments(1)
								.toOSString());
						if (tempFile.exists() && tempFile.isDirectory()) {
							if (tempFile.listFiles().length == 0) {
								tempFile.delete();
							}
						}
					}

				} catch (Exception e) {
					BasePlugin.logError("Could not delete file: " //$NON-NLS-1$
							+ e.getMessage());
				}
			}

		}

		// If the file was deleted, also remove it from the model
		List<LocalizationFile> tempLocalizationFiles = new ArrayList<LocalizationFile>(
				localizationFiles);
		for (LocalizationFile localizationFile : tempLocalizationFiles) {
			if (localizationFile.isToBeDeleted()) {
				localizationProject.removeLocalizationFile(localizationFile);
			}
		}

		refreshWorkspace();

		localizationProject.setDirty(false);
		return true;

	}

	private void refreshWorkspace() {
		try {
			PlatformUI
					.getWorkbench()
					.getProgressService()
					.runInUI(
							PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow(),
							new IRunnableWithProgress() {

								public void run(IProgressMonitor monitor)
										throws InvocationTargetException,
										InterruptedException {

									try {
										localizationProject
												.getProject()
												.refreshLocal(
														IResource.DEPTH_INFINITE,
														monitor);
									} catch (CoreException e) {
										// Do nothing
									}

								}

							}, null);
		} catch (InvocationTargetException e) {
			// Do nothing
		} catch (InterruptedException e) {
			// Do nothing
		}
	}

	/**
	 * 
	 * @param localizationFile
	 * @param newFileLangInfo
	 */
	public void translateAndCreateFile(StringLocalizationFile localizationFile,
			LocaleInfo newFileLangInfo) {

		// TODO: implement translateAndCreateFile

	}

	/**
	 * @param localizationFile
	 * @return
	 */
	public List<GrammarCheckerResult> checkGrammar(
			StringLocalizationFile localizationFile) {
		return null;
	}

	/**
	 * @return
	 */
	public Map<StringLocalizationFile, List<GrammarCheckerResult>> checkAllGrammar() {
		return null;
	}

	/**
	 * @return
	 */
	public LocalizationProject getLocalizationProject() {
		return localizationProject;
	}

	/**
	 * @param _class
	 */
	public void clearMetaData(Class _class) {

	}

	/**
	 * @param _class
	 */
	public void clearExtraInfo(Class _class) {

	}

	/**
	 * Mark the file to be deleted
	 * 
	 * @param localizationFile
	 *            the localization file that shall be deleted
	 */
	public void markFileForDeletion(LocalizationFile localizationFile) {
		localizationFile.setToBeDeleted(true);
	}

	/**
	 * Delete all file related information
	 * 
	 * @param file
	 */
	public void deleteFileMetaExtraData(IFile file) {
		// projectPersistenceManager.clearDataForFile(file);
	}

	/**
	 * Get the Localization Schema for this project
	 * 
	 * @return Localization Schema for this project
	 */
	public ILocalizationSchema getProjectLocalizationSchema() {
		return projectLocalizationSchema;
	}

	/**
	 * Get the Project Persistence Manager for this project
	 * 
	 * @return Project Persistence Manager for this project
	 */
	public ProjectPersistenceManager getProjectPersistenceManager() {
		return projectPersistenceManager;
	}

	/**
	 * Get the Project Preferences Manager for this project
	 * 
	 * @return Project Preferences Manager for this project
	 */
	public ProjectPreferencesManager getProjectPreferencesManager() {
		return projectPreferencesManager;
	}

	/**
	 * Get the locale information for a especific file
	 * 
	 * @param file
	 *            IFile to be analysed
	 * @return the LocaleInfo for the file passed as a parameter
	 */
	public LocaleInfo getLocaleInfoForFile(IFile file) {

		LocaleInfo localeInfo = null;

		List<LocalizationFile> localizationFiles = localizationProject
				.getLocalizationFiles();
		for (LocalizationFile localizationFile : localizationFiles) {
			if (localizationFile.getFile().equals(file)) {
				localeInfo = localizationFile.getLocaleInfo();
			}
		}

		return localeInfo;

	}

	/**
	 * @param localizationProject
	 *            the localizationProject to set
	 */
	public void setLocalizationProject(LocalizationProject localizationProject) {
		this.localizationProject = localizationProject;
	}

	/**
	 * @param project
	 *            the project to set
	 */
	public void setProject(IProject project) {
		this.project = project;
	}

}
