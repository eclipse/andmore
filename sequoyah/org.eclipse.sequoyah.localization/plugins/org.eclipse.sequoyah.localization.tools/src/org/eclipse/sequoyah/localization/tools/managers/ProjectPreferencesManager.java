/********************************************************************************
 * Copyright (c) 2009 Motorola Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Vinicius Hernandes (Motorola)
 * 
 * Contributors:
 * name (company) - description.
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.sequoyah.localization.tools.LocalizationToolsPlugin;

/**
 * This manager is responsible for storing and retrieving preferences values for
 * specific projects
 */
public class ProjectPreferencesManager {

	private IProject project;

	/*
	 * The default language of this project
	 */
	private final String DEFAULT_LANGUAGE = "en"; //$NON-NLS-1$

	private Map<String, String> defaultLanguageForColumn = new HashMap<String, String>();

	private IPreferenceStore preferenceStore = null;

	private final String PREFERENCE_KEY_SUFFIX = "_DEFAULTLANGUAGES"; //$NON-NLS-1$

	private final String NODE_SEPARATOR = ","; //$NON-NLS-1$

	private final String FIELD_SEPARATOR = ":"; //$NON-NLS-1$

	private ProjectLocalizationManager projectLocalizationManager;

	private boolean metadataEnabled;

	public ProjectPreferencesManager(IProject project) {
		this.project = project;
		this.preferenceStore = LocalizationToolsPlugin.getDefault()
				.getPreferenceStore();
		load();
	}

	/**
	 * Get the default language to be used in the translation processes
	 * 
	 * @return the default language to be used in the translation processes
	 */
	public String getDefaultLanguage() {
		return DEFAULT_LANGUAGE;
	}

	/**
	 * Get the default language for a specific column to be used in the
	 * translation processes
	 * 
	 * @param columnID
	 *            column ID
	 * @return default language for the column passed as parameter
	 */
	public String getDefaultLanguageForColumn(String columnID) {
		String defaultLanguage = defaultLanguageForColumn.get(columnID);
		if (defaultLanguage == null) {
			defaultLanguage = getDefaultLanguage();
		}
		return defaultLanguage;
	}

	/**
	 * Set the default language for a specific column to be used in the
	 * translation processes
	 * 
	 * @param columnID
	 *            the column ID
	 * @param langID
	 *            the default language ID to be used in the translation
	 *            processes
	 */
	public void setDefaultLanguageForColumn(String columnID, String langID) {
		defaultLanguageForColumn.put(columnID, langID);
		save();
	}

	/**
	 * Clean language information for a specific column
	 * 
	 * @param columnID
	 *            column ID
	 */
	private void cleanLanguageInfoForColumn(String columnID) {
		defaultLanguageForColumn.remove(columnID);
		save();
	}

	public void clearDataForFile(IFile file) {

	}

	/**
	 * Clean info for columns that does not exist anymore
	 * 
	 * @param columnIDs
	 *            the list of existent columns
	 */
	private void cleanInfoForInexistentColumns(String[] columnIDs) {

		List<String> currentColumns = Arrays.asList(columnIDs);
		Set<String> persistedColumns = defaultLanguageForColumn.keySet();
		List<String> keysToBeDeleted = new ArrayList<String>();

		for (String persistedColumn : persistedColumns) {
			if (!currentColumns.contains(persistedColumn)) {
				keysToBeDeleted.add(persistedColumn);
			}
		}

		// remove keys
		for (String key : keysToBeDeleted) {
			defaultLanguageForColumn.remove(key);
		}

		save();
	}

	/**
	 * Retrieve map columnID -> default lang ID
	 */
	private void load() {
		String persistedData = preferenceStore.getString(project.getName()
				+ PREFERENCE_KEY_SUFFIX);
		if ((persistedData != null) && (!persistedData.equals(""))) { //$NON-NLS-1$
			String[] dataForColumn = persistedData.split(NODE_SEPARATOR);
			String[] data;
			for (String columnData : dataForColumn) {
				data = columnData.split(FIELD_SEPARATOR);
				defaultLanguageForColumn.put(data[0], data[1]);
			}
		}
	}

	/**
	 * Persist map columnID -> default lang ID
	 */
	private void save() {
		String persistedData = ""; //$NON-NLS-1$
		for (Map.Entry<String, String> entry : defaultLanguageForColumn
				.entrySet()) {
			persistedData += ((persistedData != "") ? NODE_SEPARATOR : "") //$NON-NLS-1$ //$NON-NLS-2$
					+ entry.getKey() + FIELD_SEPARATOR + entry.getValue();
		}
		preferenceStore.setValue(project.getName() + PREFERENCE_KEY_SUFFIX,
				persistedData);
	}

	/**
	 * @return
	 */
	public boolean hasMetadata() {
		return false;
	}

	/**
	 * @return
	 */
	public boolean hasExtraInfo() {
		return false;
	}

	/**
     * 
     */
	public void clearMetadata() {

	}

	/**
     * 
     */
	public void clearExtraInfo() {

	}

	/**
     * 
     */
	public boolean hasComments() {
		return false;
	}

	/**
     * 
     */
	public boolean hasTranslationDetails() {
		return false;
	}

	/**
     * 
     */
	public boolean hasGrammarCheckerDetails() {
		return false;
	}

	/**
     * 
     */
	public void clearComments() {

	}

	/**
     * 
     */
	public void clearTranslationDetails() {

	}

	/**
     * 
     */
	public void clearGrammarCheckerDetails() {

	}

	/**
	 * 
	 * 
	 * @return
	 */
	public boolean isMetadataEnabled() {
		return metadataEnabled;
	}

	/**
	 * @param metadataEnabled
	 */
	public void setMetadataEnabled(boolean metadataEnabled) {
		this.metadataEnabled = metadataEnabled;
	}

}
