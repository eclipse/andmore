/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Matheus Tait Lima (Eldorado)
 * Vinicius Hernandes (Eldorado)
 * Marcel Gorri (Eldorado)
 * 
 * Contributors:
 * name (company) - description.
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.extensions.implementation.generic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.sequoyah.localization.tools.LocalizationToolsPlugin;
import org.eclipse.sequoyah.localization.tools.extensions.classes.ILocalizationSchema;
import org.eclipse.sequoyah.localization.tools.extensions.classes.ITranslator;
import org.eclipse.sequoyah.localization.tools.i18n.Messages;
import org.eclipse.sequoyah.localization.tools.managers.PreferencesManager;
import org.eclipse.sequoyah.localization.tools.managers.TranslatorManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Utility class to work with the available languages and their IDs
 * 
 */
public class LanguagesUtil implements TranslatorConstants {

	private static Map<String, String> availableLanguages = new LinkedHashMap<String, String>();

	private static String COMBO_SEPARATOR = "-"; //$NON-NLS-1$

	// Images
	private static Image warningImage = new Image(Display.getDefault(),
			PlatformUI.getWorkbench().getSharedImages()
					.getImage(ISharedImages.IMG_OBJS_WARN_TSK).getImageData()
					.scaledTo(16, 16));

	private static Image okImage = new Image(Display.getDefault(),
			LocalizationToolsPlugin.imageDescriptorFromPlugin(
					LocalizationToolsPlugin.PLUGIN_ID, "icons/obj16_ok.png") //$NON-NLS-1$
					.getImageData());

	static {
		availableLanguages.put("AFRIKAANS", AFRIKAANS); //$NON-NLS-1$
		availableLanguages.put("ALBANIAN", ALBANIAN); //$NON-NLS-1$
		availableLanguages.put("AMHARIC", AMHARIC); //$NON-NLS-1$
		availableLanguages.put("ARABIC", ARABIC); //$NON-NLS-1$
		availableLanguages.put("ARMENIAN", ARMENIAN); //$NON-NLS-1$
		availableLanguages.put("AZERBAIJANI", AZERBAIJANI); //$NON-NLS-1$
		availableLanguages.put("BASQUE", BASQUE); //$NON-NLS-1$
		availableLanguages.put("BELARUSIAN", BELARUSIAN); //$NON-NLS-1$
		availableLanguages.put("BENGALI", BENGALI); //$NON-NLS-1$
		availableLanguages.put("BIHARI", BIHARI); //$NON-NLS-1$
		availableLanguages.put("BULGARIAN", BULGARIAN); //$NON-NLS-1$
		availableLanguages.put("BURMESE", BURMESE); //$NON-NLS-1$
		availableLanguages.put("CATALAN", CATALAN); //$NON-NLS-1$
		availableLanguages.put("CHEROKEE", CHEROKEE); //$NON-NLS-1$
		availableLanguages.put("CHINESE", CHINESE); //$NON-NLS-1$
		availableLanguages.put("CHINESE_SIMPLIFIED", CHINESE_SIMPLIFIED); //$NON-NLS-1$
		availableLanguages.put("CHINESE_TRADITIONAL", CHINESE_TRADITIONAL); //$NON-NLS-1$
		availableLanguages.put("CROATIAN", CROATIAN); //$NON-NLS-1$
		availableLanguages.put("CZECH", CZECH); //$NON-NLS-1$
		availableLanguages.put("DANISH", DANISH); //$NON-NLS-1$
		availableLanguages.put("DHIVEHI", DHIVEHI); //$NON-NLS-1$
		availableLanguages.put("DUTCH", DUTCH); //$NON-NLS-1$
		availableLanguages.put("ENGLISH", ENGLISH); //$NON-NLS-1$
		availableLanguages.put("ESPERANTO", ESPERANTO); //$NON-NLS-1$
		availableLanguages.put("ESTONIAN", ESTONIAN); //$NON-NLS-1$
		availableLanguages.put("FILIPINO", FILIPINO); //$NON-NLS-1$
		availableLanguages.put("FINNISH", FINNISH); //$NON-NLS-1$
		availableLanguages.put("FRENCH", FRENCH); //$NON-NLS-1$
		availableLanguages.put("GALACIAN", GALACIAN); //$NON-NLS-1$
		availableLanguages.put("GEORGIAN", GEORGIAN); //$NON-NLS-1$
		availableLanguages.put("GERMAN", GERMAN); //$NON-NLS-1$
		availableLanguages.put("GREEK", GREEK); //$NON-NLS-1$
		availableLanguages.put("GUARANI", GUARANI); //$NON-NLS-1$
		availableLanguages.put("GUJARATI", GUJARATI); //$NON-NLS-1$
		availableLanguages.put("HEBREW", HEBREW); //$NON-NLS-1$
		availableLanguages.put("HINDI", HINDI); //$NON-NLS-1$
		availableLanguages.put("HUNGARIAN", HUNGARIAN); //$NON-NLS-1$
		availableLanguages.put("ICELANDIC", ICELANDIC); //$NON-NLS-1$
		availableLanguages.put("INDONESIAN", INDONESIAN); //$NON-NLS-1$
		availableLanguages.put("INUKTITUT", INUKTITUT); //$NON-NLS-1$
		availableLanguages.put("IRISH", IRISH); //$NON-NLS-1$
		availableLanguages.put("ITALIAN", ITALIAN); //$NON-NLS-1$
		availableLanguages.put("JAPANESE", JAPANESE); //$NON-NLS-1$
		availableLanguages.put("KANNADA", KANNADA); //$NON-NLS-1$
		availableLanguages.put("KAZAKH", KAZAKH); //$NON-NLS-1$
		availableLanguages.put("KHMER", KHMER); //$NON-NLS-1$
		availableLanguages.put("KOREAN", KOREAN); //$NON-NLS-1$
		availableLanguages.put("KURDISH", KURDISH); //$NON-NLS-1$
		availableLanguages.put("KYRGYZ", KYRGYZ); //$NON-NLS-1$
		availableLanguages.put("LAOTHIAN", LAOTHIAN); //$NON-NLS-1$
		availableLanguages.put("LATVIAN", LATVIAN); //$NON-NLS-1$
		availableLanguages.put("LITHUANIAN", LITHUANIAN); //$NON-NLS-1$
		availableLanguages.put("MACEDONIAN", MACEDONIAN); //$NON-NLS-1$
		availableLanguages.put("MALAY", MALAY); //$NON-NLS-1$
		availableLanguages.put("MALAYALAM", MALAYALAM); //$NON-NLS-1$
		availableLanguages.put("MALTESE", MALTESE); //$NON-NLS-1$
		availableLanguages.put("MARATHI", MARATHI); //$NON-NLS-1$
		availableLanguages.put("MONGOLIAN", MONGOLIAN); //$NON-NLS-1$
		availableLanguages.put("NEPALI", NEPALI); //$NON-NLS-1$
		availableLanguages.put("NORWEGIAN", NORWEGIAN); //$NON-NLS-1$
		availableLanguages.put("ORIYA", ORIYA); //$NON-NLS-1$
		availableLanguages.put("PASHTO", PASHTO); //$NON-NLS-1$
		availableLanguages.put("PERSIAN", PERSIAN); //$NON-NLS-1$
		availableLanguages.put("POLISH", POLISH); //$NON-NLS-1$
		availableLanguages.put("PORTUGUESE", PORTUGUESE); //$NON-NLS-1$
		availableLanguages.put("PUNJABI", PUNJABI); //$NON-NLS-1$
		availableLanguages.put("ROMANIAN", ROMANIAN); //$NON-NLS-1$
		availableLanguages.put("RUSSIAN", RUSSIAN); //$NON-NLS-1$
		availableLanguages.put("SANSKRIT", SANSKRIT); //$NON-NLS-1$
		availableLanguages.put("SERBIAN", SERBIAN); //$NON-NLS-1$
		availableLanguages.put("SINDHI", SINDHI); //$NON-NLS-1$
		availableLanguages.put("SINHALESE", SINHALESE); //$NON-NLS-1$
		availableLanguages.put("SLOVAK", SLOVAK); //$NON-NLS-1$
		availableLanguages.put("SLOVENIAN", SLOVENIAN); //$NON-NLS-1$
		availableLanguages.put("SPANISH", SPANISH); //$NON-NLS-1$
		availableLanguages.put("SWAHILI", SWAHILI); //$NON-NLS-1$
		availableLanguages.put("SWEDISH", SWEDISH); //$NON-NLS-1$
		availableLanguages.put("TAJIK", TAJIK); //$NON-NLS-1$
		availableLanguages.put("TAMIL", TAMIL); //$NON-NLS-1$
		availableLanguages.put("TAGALOG", TAGALOG); //$NON-NLS-1$
		availableLanguages.put("TELUGU", TELUGU); //$NON-NLS-1$
		availableLanguages.put("THAI", THAI); //$NON-NLS-1$
		availableLanguages.put("TIBETAN", TIBETAN); //$NON-NLS-1$
		availableLanguages.put("TURKISH", TURKISH); //$NON-NLS-1$
		availableLanguages.put("UKRANIAN", UKRANIAN); //$NON-NLS-1$
		availableLanguages.put("URDU", URDU); //$NON-NLS-1$
		availableLanguages.put("UZBEK", UZBEK); //$NON-NLS-1$
		availableLanguages.put("UIGHUR", UIGHUR); //$NON-NLS-1$
		availableLanguages.put("VIETNAMESE", VIETNAMESE); //$NON-NLS-1$
		availableLanguages.put("WELSH", WELSH); //$NON-NLS-1$
		availableLanguages.put("YIDDISH", YIDDISH); //$NON-NLS-1$
		availableLanguages.put("VIETNAMESE", VIETNAMESE); //$NON-NLS-1$
		availableLanguages.put("WELSH", WELSH); //$NON-NLS-1$
		availableLanguages.put("YIDDISH", YIDDISH); //$NON-NLS-1$
	}

	/**
	 * Return a set of the supported languages (strings with the language name
	 * in uppercase)
	 */
	public static List<String> getAvailableLanguages() {
		return new ArrayList<String>(availableLanguages.keySet());
	}

	/**
	 * Get preferred languages for certain localization schema
	 * 
	 * @param locSchema
	 *            localization schema
	 * @return a set of the preferred languages (strings with the language name
	 *         in uppercase)
	 */
	public static List<String> getPreferredLanguages(
			ILocalizationSchema locSchema) {

		List<String> languageNames = new ArrayList<String>();
		List<String> preferedLanguagesIds = locSchema.getPreferedLanguages();

		if (preferedLanguagesIds != null) {
			for (String langID : preferedLanguagesIds) {
				languageNames.add(getLanguageName(langID));
			}
		}

		return languageNames;

	}

	/**
	 * Return the separator used in combobox, which represent a non-valid
	 * selection
	 * 
	 * @return separator used in combobox
	 */
	public static String getComboSeparator() {
		return COMBO_SEPARATOR;
	}

	/**
	 * Given a language name (one of the supported languages) return the
	 * language ID.
	 */
	public static String getLanguageID(String languageName) {
		return availableLanguages.get(languageName);
	}

	/**
	 * @param ID
	 * @return
	 */
	public static String getLanguageName(String ID) {
		String langName = null;
		for (Map.Entry<String, String> entry : availableLanguages.entrySet()) {
			if (entry.getValue().equals(ID)) {
				langName = entry.getKey();
				break;
			}
		}
		return langName;
	}

	public static Combo createTranslatorsCombo(Composite parent) {
		Combo translatorsCombo = null;

		List<ITranslator> translators = TranslatorManager.getInstance()
				.getTranslators();
		ITranslator defaultTranslator = PreferencesManager.getInstance()
				.getDefaultTranslator();
		String defaultTranslatorName = ((defaultTranslator != null) ? defaultTranslator
				.getName() : ""); //$NON-NLS-1$

		translatorsCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY
				| SWT.SINGLE);

		String[] translatorNames = new String[translators.size()];
		int defaultTranslatorIndex = 0;
		for (int i = 0; i < translators.size(); i++) {
			translatorNames[i] = translators.get(i).getName();
			if (translatorNames[i].equals(defaultTranslatorName)) {
				defaultTranslatorIndex = i;
			}

		}
		translatorsCombo.setItems(translatorNames);
		translatorsCombo.select(defaultTranslatorIndex);

		return translatorsCombo;

	}

	public static Combo createLanguagesCombo(Composite parent,
			String initialSelection, String defaultSelection,
			ILocalizationSchema locSchema) {
		Combo languagesCombo = null;

		List<String> availableLangs = getAvailableLanguages();
		List<String> preferedLangs = getPreferredLanguages(locSchema);

		List<String> allLangs = new ArrayList<String>();
		allLangs.addAll(preferedLangs);
		allLangs.add(COMBO_SEPARATOR);
		allLangs.addAll(availableLangs);

		languagesCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY
				| SWT.SINGLE);

		if (allLangs.size() > 0) {
			languagesCombo
					.setItems(allLangs.toArray(new String[allLangs.size()]));

			String comboSelection = initialSelection;

			if (comboSelection == null) {
				comboSelection = defaultSelection;
			}

			if (comboSelection != null) {
				String langName = getLanguageName(comboSelection);
				int index = 0;
				for (int i = 0; i < languagesCombo.getItems().length; i++) {
					String item = languagesCombo.getItem(i);
					if (item.equals(langName)) {
						index = i;
						break;
					}
				}
				languagesCombo.select(index);
			} else {
				languagesCombo.select(0);
			}
		}

		return languagesCombo;
	}

	/**
	 * Create an OK or WARNING image (16x16) based on language information If
	 * the language has correctly been identified, an OK image is created
	 * Otherwise, the method creates a WARNING image
	 * 
	 * @param parent
	 *            parent composite
	 * @param lang
	 *            the language, or null if it's not recognized
	 */
	public static Label createImageStatus(Composite parent, String lang) {
		Label image = new Label(parent, SWT.NONE);

		changeImageStatus(image, lang);

		return image;
	}

	/**
	 * Change status image according to language information
	 * 
	 * @param image
	 *            the label that contain the image
	 * @param lang
	 *            the language, or null if it's not recognized
	 */
	public static void changeImageStatus(Label image, String lang) {
		if (lang != null) {
			image.setImage(okImage);
			image.setToolTipText(Messages.TranslationDialog_ImageOKTooltip);
		} else {
			image.setImage(warningImage);
			image.setToolTipText(Messages.TranslationDialog_ImageWARNTooltip);
		}
	}

}
