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
package com.motorola.studio.android.localization.translators;

import org.eclipse.sequoyah.localization.tools.extensions.implementation.generic.TranslatorConstants;

/**
 * Constants used by the GoogleTranslator class and its users.
 */
public interface GoogleTranslatorConstants extends TranslatorConstants {

	// API Key to identify MOTODEV site
	static final String API_KEY = "ABQIAAAAzLP1ONRdncTVQSc4T1g2MRT7zv61Fj6qXODo5OU8i7eIwTs2GRQjZ2moI_dkzMckcgUZys7M9wAMvQ";

	public static final String API_KEY_VALUE_PREFERENCE = "google.translator.apikey";

	// Encoding type
	public static final String ENCODING_TYPE = "UTF-8";

	/**
	 * Parameter used to create URL
	 */
	public static final String SOURCE_PAR = "&source=";

	/**
	 * Parameter used to create URL
	 */
	public static final String STRING_PAR = "&q=";

	/**
	 * Parameter used to create URL
	 */
	public static final String TARGET_PARAM = "&target=";

	/**
	 * Parameter used to create URL
	 */
	public static final String API_KEY_PARAM = "&key=";

	/**
	 * The base URL to access the translation service. the #FROM# and #TO# parts
	 * are replaced on execution time.
	 */
	public static final String URL_PARAMETERS = API_KEY_PARAM + "#API_KEY#" + SOURCE_PAR + "#FROM#" + TARGET_PARAM
			+ "#TO#";

	/**
	 * The base URL to access the translation service, without the parameters.
	 * The parameters are created on real time when needed.
	 */
	public static final String TRANSLATE_URL_WITHOUT_PARAMS = "https://www.googleapis.com/language/translate/v2?prettyprint=false";

	/**
	 * The base URL to access the translation service. the #FROM# and #TO# parts
	 * are replaced on execution time.
	 */
	public static final String BASE_TRANSLATE_URL = TRANSLATE_URL_WITHOUT_PARAMS + URL_PARAMETERS;

	/**
	 * Text that appears just before the translated text begins on a typical
	 * answer from the web server
	 */
	public static final String TRANSLATED_TEXT_KEY = "translatedText";

	/**
	 * Text defining the translation section of response
	 */
	public static final String TRANSLATIONS_SECTION = "translations";

	/**
	 * Text defining the error section of response
	 */
	public static final String ERROR_SECTION = "error";

	/**
	 * Text defining the message section of response
	 */
	public static final String MESSAGE_TEXT = "message";

	// Max string size
	public static int MAX_SIZE = 100000;

	// Max string size
	public static int MAX_QUERY_SIZE = 2000; // google rules!! Do not change
												// without VT

	// Number of retries for the http request when there are connection problems
	public static final int RETRIES = 0;

	// Timeout in miliseconds for the http queries
	public static final int TIMEOUT = 4000;

	// HTTP Header constant to set the referer
	public static final String REFERER_HEADER = "Referer";

	// Site to be used as a referer site
	public static final String REFERER_SITE = "http://developer.motorola.com";

}
