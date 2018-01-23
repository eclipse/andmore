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
 * Marcelo Marzola Bossoni (Eldorado) - Bug [289146] - Performance and Usability Issues
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.managers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.sequoyah.localization.tools.extensions.classes.IGrammarChecker;
import org.eclipse.sequoyah.localization.tools.extensions.classes.ITranslator;
import org.eclipse.swt.graphics.RGB;

/**
 * This manager is responsible for storing and retrieving preferences values
 * used by the Localization Framework
 */
public class PreferencesManager {

	/*
	 * The default translator to be used in the translation processes
	 */
	private ITranslator defaultTranslator;

	/*
	 * The default grammar checker to be used in the grammar checking processes
	 */
	private IGrammarChecker defaultGrammarChecker;

	/*
	 * The color used by the Localization Editor to highlight cells
	 */
	private RGB highlightColor;

	/*
	 * States if the Localization Framework is allowed to store meta-data by
	 * default. This happens as a result of some operations applied in the
	 * localization files data through the use of the Localization Editor
	 */
	private boolean metadataEnabledByDefault;

	/*
	 * The Managers for each Project
	 */
	private Map<IProject, ProjectPreferencesManager> projectPreferencesManagers = new HashMap<IProject, ProjectPreferencesManager>();

	/*
	 * Singleton instance
	 */
	private static PreferencesManager instance;

	/**
	 * Singleton
	 * 
	 * @return LocalizationManager
	 */
	public static PreferencesManager getInstance() {
		if (instance == null) {
			instance = new PreferencesManager();
		}
		return instance;
	}

	public ProjectPreferencesManager getProjectPreferencesManager(
			IProject project) {

		ProjectPreferencesManager projectPreferencesManager = projectPreferencesManagers
				.get(project);
		if (projectPreferencesManager == null) {

			projectPreferencesManager = new ProjectPreferencesManager(project);
			projectPreferencesManagers.put(project, projectPreferencesManager);

		}

		return projectPreferencesManager;
	}

	/**
	 * Get the default translator to be used in the translation processes
	 * 
	 * @return the default translator to be used in the translation processes
	 */
	public ITranslator getDefaultTranslator() {
		if (defaultTranslator == null) {
			List<ITranslator> translators = TranslatorManager.getInstance()
					.getTranslators();
			if ((translators != null) && (translators.size() > 0)) {
				defaultTranslator = translators.get(0);
			}
		}
		return defaultTranslator;
	}

	/**
	 * Set the default translator to be used in the translation processes
	 * 
	 * @param defaultTranslator
	 *            the default translator to be used in the translation processes
	 */
	public void setDefaultTranslator(ITranslator defaultTranslator) {
		this.defaultTranslator = defaultTranslator;
	}

	/**
	 * Get the default grammar checker to be used in the grammar checking
	 * processes
	 * 
	 * @return the default grammar checker to be used in the grammar checking
	 *         processes
	 */
	public IGrammarChecker getDefaultGrammarChecker() {
		return defaultGrammarChecker;
	}

	/**
	 * Set the default grammar checker to be used in the grammar checking
	 * processes
	 * 
	 * @param defaultGrammarChecker
	 *            the default grammar checker to be used in the grammar checking
	 *            processes
	 */
	public void setDefaultGrammarChecker(IGrammarChecker defaultGrammarChecker) {
		this.defaultGrammarChecker = defaultGrammarChecker;
	}

	/**
	 * Get the color used by the Localization Editor to highlight cells
	 * 
	 * @return the color used by the Localization Editor to highlight cells
	 */
	public RGB getHighlightColor() {
		return highlightColor;
	}

	/**
	 * Set the color used by the Localization Editor to highlight cells
	 * 
	 * @param highlightColor
	 *            the color used by the Localization Editor to highlight cells
	 */
	public void setHighlightColor(RGB highlightColor) {
		this.highlightColor = highlightColor;
	}

	/**
	 * Check whether the Localization Framework is allowed to store meta-data by
	 * default or not
	 * 
	 * @return true if the Localization Framework is allowed to store meta-data
	 *         by default, false otherwise
	 */
	public boolean isMetadataEnabledByDefault() {
		return metadataEnabledByDefault;
	}

	/**
	 * Set whether the Localization Framework is allowed to store meta-data by
	 * default or not
	 * 
	 * @param metadataEnabledByDefault
	 *            true if the Localization Framework is allowed to store
	 *            meta-data by default, false otherwise
	 */
	public void setMetadataEnabledByDefault(boolean metadataEnabledByDefault) {
		this.metadataEnabledByDefault = metadataEnabledByDefault;
	}

}
