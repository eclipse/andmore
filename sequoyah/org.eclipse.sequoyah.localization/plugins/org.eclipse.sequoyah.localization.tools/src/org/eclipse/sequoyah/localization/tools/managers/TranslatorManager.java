/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Vinicius Hernandes (Motorola)
 * 
 * Contributors:
 * Marcel Gorri (Eldorado) - Implement methods to make automatic translation
 * Matheus Lima (Eldorado) - Bug [326793] - Fixed so no exception occurs when no translator is associated
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.localization.tools.datamodel.node.TranslationResult;
import org.eclipse.sequoyah.localization.tools.extensions.classes.ITranslator;
import org.eclipse.sequoyah.localization.tools.extensions.providers.TranslatorProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Label;

/**
 * Manage translators
 */
public class TranslatorManager {

	/*
	 * The translators, indexed by name
	 */
	private Map<String, ITranslator> translators;

	private PreferencesManager preferencesManager;

	private TranslatorProvider translatorProvider;

	/*
	 * Singleton instance
	 */
	private static TranslatorManager instance = null;

	/**
	 * Singleton
	 * 
	 * @return TranslatorManager
	 */
	public static TranslatorManager getInstance() {
		if (instance == null) {
			instance = new TranslatorManager();
		}
		return instance;
	}

	/**
	 * Constructor
	 */
	public TranslatorManager() {
		this.translatorProvider = TranslatorProvider.getInstance();
		this.preferencesManager = PreferencesManager.getInstance();
		this.translators = this.translatorProvider.getTranslators();
		this.preferencesManager.setDefaultTranslator(this.translators
				.get("Google Translator")); //$NON-NLS-1$
	}

	/**
	 * Translate the string passed as parameter
	 * 
	 * @param string
	 *            string to translate
	 * @param fromLanguage
	 *            language ID to translate from
	 * @param toLanguage
	 *            language ID to translate to
	 * @return a TranslationResult object with information about the translation
	 */
	public TranslationResult translate(String string, String fromLanguage,
			String toLanguage) {

		TranslationResult translationResults = null;

		ITranslator translator = preferencesManager.getDefaultTranslator();

		try {
			translationResults = translator.translate(string, fromLanguage,
					toLanguage);
		} catch (Exception e) {
			BasePlugin.logError("Error while using translator"); //$NON-NLS-1$
		}

		return translationResults;
	}

	/**
	 * Translate all strings passed as parameter
	 * 
	 * @param strings
	 *            strings to translate
	 * @param fromLanguage
	 *            language ID to translate from
	 * @param toLanguage
	 *            language ID to translate to
	 * @return a TranslationResult object with information about the translation
	 */
	public List<TranslationResult> translateAll(List<String> strings,
			String fromLanguage, String toLanguage) {

		List<TranslationResult> translationResults = null;

		ITranslator translator = preferencesManager.getDefaultTranslator();

		try {
			translationResults = translator.translateAll(strings, fromLanguage,
					toLanguage, null);
		} catch (Exception e) {
			BasePlugin.logError("Error while using translator"); //$NON-NLS-1$
		}

		return translationResults;
	}

	/**
	 * Get all registered translators
	 * 
	 * @return all registered translators
	 */
	public List<ITranslator> getTranslators() {
		return new ArrayList<ITranslator>(translators.values());
	}

	/**
	 * Get a specific translator by its name
	 * 
	 * @param name
	 *            translator name
	 * @return the translator
	 */
	public ITranslator getTranslatorByName(String name) {
		return this.translators.get(name);
	}

	/**
	 * Set translator branding image to a Label
	 * 
	 * @param translatorBrandingImage
	 *            Label that will display the image
	 * @throws SequoyahException
	 */
	public void setTranslatorBranding(String translatorName,
			Label translatorBrandingImage) {
		ITranslator translatorObj = getTranslatorByName(translatorName);
		if (translatorObj != null) {
			Image brandingImg = translatorObj.getBrandingImg();
			if (brandingImg != null) {
				translatorBrandingImage.setImage(brandingImg);
			}
		}

	}
}
