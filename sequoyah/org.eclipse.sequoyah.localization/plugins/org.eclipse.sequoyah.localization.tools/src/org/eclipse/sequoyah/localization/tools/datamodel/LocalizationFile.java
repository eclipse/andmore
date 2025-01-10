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
 * Vinicius Rigoni Hernandes (Eldorado) - Bug [289885] - Localization Editor doesn't recognize external file changes
 * Fabricio Violin (Eldorado) - Bug [317065] - Localization file initialization bug
 * Daniel Pastore (Eldorado) - Bug [323036] - Add support to other localizable resources
 *  
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.datamodel;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.sequoyah.localization.tools.persistence.IFilePersistentData;
import org.eclipse.sequoyah.localization.tools.persistence.IPersistentData;
import org.eclipse.sequoyah.localization.tools.persistence.PersistableAttributes;

/**
 * This class represents a real localization file in the project and contains
 * other information about it
 */
public class LocalizationFile implements IFilePersistentData {

	/*
	 * The LocalizationProject which the LocalizationFile belongs to
	 */
	protected LocalizationProject localizationProject;

	/*
	 * A reference to the file being represented
	 */
	protected IFile file;

	/*
	 * The information about the locale represented by the localization file
	 */
	protected LocaleInfo localeInfo;

	/*
	 * Whether the data in the model has been modified and differs from the
	 * values saved
	 */
	protected boolean dirty = false;

	/*
	 * Whether there are changes in the associated meta-data / extra-info or not
	 */
	protected boolean dirtyMetaExtraData = false;

	/*
	 * Whether the file is marked to be deleted or not
	 */
	protected boolean toBeDeleted = false;

	/**
	 * Default constructor
	 * 
	 */
	public LocalizationFile() {
	}

	/**
	 * Constructor method
	 * 
	 * @param file
	 *            a reference to the file being represented
	 * @param localeInfo
	 *            the locale represented by the localization file
	 */
	public LocalizationFile(LocalizationFileBean bean) {
		this.file = bean.getFile();
		this.localeInfo = bean.getLocale();
	}

	/**
	 * Get the LocalizationProject which the LocalizationFile belongs to
	 * 
	 * @return the LocalizationProject which the LocalizationFile belongs to
	 */
	public LocalizationProject getLocalizationProject() {
		return localizationProject;
	}

	/**
	 * Set the LocalizationProject which the LocalizationFile belongs to
	 * 
	 * @param localizationProject
	 *            the LocalizationProject which the LocalizationFile belongs to
	 */
	public void setLocalizationProject(LocalizationProject localizationProject) {
		this.localizationProject = localizationProject;
	}

	/**
	 * Get information about the locale represented by the localization file
	 * 
	 * @return information about the locale represented by the localization file
	 */
	public LocaleInfo getLocaleInfo() {
		return localeInfo;
	}

	/**
	 * Set information about the locale represented by the localization file
	 * 
	 * @param localeInfo
	 *            information about the locale represented by the localization
	 *            file
	 */
	public void setLocaleInfo(LocaleInfo localeInfo) {
		this.localeInfo = localeInfo;
	}

	/**
	 * Check whether the data in the model has been modified and differs from
	 * the values saved
	 * 
	 * @return true if the data in the model has been modified and differs from
	 *         the values saved, false otherwise
	 */
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * Set whether the data in the model has been modified and differs from the
	 * values saved
	 * 
	 * @param dirty
	 *            true if the data in the model has been modified and differs
	 *            from the values saved, false otherwise
	 */
	public void setDirty(boolean dirty) {
		// propagate the state if dirty = true
		if (dirty) {
			this.getLocalizationProject().setDirty(dirty);
		}
		this.dirty = dirty;
	}

	/**
	 * Check whether there are changes in the associated meta-data / extra-info
	 * or not
	 * 
	 * @return true if there are changes in the associated meta-data /
	 *         extra-info, false otherwise
	 */
	public boolean isDirtyMetaExtraData() {
		return dirtyMetaExtraData;
	}

	/**
	 * Set whether there are changes in the associated meta-data / extra-info or
	 * not
	 * 
	 * @param dirtyMetaExtraData
	 *            true if there are changes in the associated meta-data /
	 *            extra-info, false otherwise
	 */
	public void setDirtyMetaExtraData(boolean dirtyMetaExtraData) {
		this.dirtyMetaExtraData = dirtyMetaExtraData;
	}

	/**
	 * Set the file that is being represented
	 * 
	 * @param file
	 *            the file that is being represented
	 */
	public void setFile(IFile file) {
		this.file = file;
	}

	/**
	 * Return the file that is being represented
	 * 
	 * @see org.eclipse.sequoyah.localization.tools.persistence.IFilePersistentData#getFile()
	 */
	public IFile getFile() {
		return file;
	}

	/**
	 * @see org.eclipse.sequoyah.localization.tools.persistence.IFilePersistentData#getPersistentData()
	 */
	public List<IPersistentData> getPersistentData() {
		List<IPersistentData> persistentData = new ArrayList<IPersistentData>();
		return persistentData;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.sequoyah.localization.tools.persistence.IPersistentData#
	 * getPersistableAttributes()
	 */
	public PersistableAttributes getPersistableAttributes() {
		return null;
	}

	/**
	 * Check whether the file shall be deleted or not
	 * 
	 * @return true if the shall be deleted or not, false otherwise
	 */
	public boolean isToBeDeleted() {
		return toBeDeleted;
	}

	/**
	 * Set whether the file shall be deleted or not
	 * 
	 * @param toBeDeleted
	 *            true if the file shall be deleted or false otherwise
	 */
	public void setToBeDeleted(boolean toBeDeleted) {
		this.toBeDeleted = toBeDeleted;
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = true;

		if (!this.getLocaleInfo().equals(
				((LocalizationFile) obj).getLocaleInfo())) {
			result = false;
		}
		return result;
	}

}
