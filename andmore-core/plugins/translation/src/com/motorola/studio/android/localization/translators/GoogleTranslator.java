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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.proxy.ProxyAuthenticator;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.json.JSONArray;
import org.eclipse.andmore.android.json.JSONObject;
import org.eclipse.andmore.android.json.JSONPair;
import org.eclipse.andmore.android.json.JSONString;
import org.eclipse.andmore.android.json.JSONValue;
import org.eclipse.andmore.android.json.Jason;
import org.eclipse.core.internal.net.ProxyManager;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.localization.tools.datamodel.node.TranslationResult;
import org.eclipse.sequoyah.localization.tools.extensions.classes.ITranslator;
import org.eclipse.sequoyah.localization.tools.extensions.implementation.generic.ITranslateDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;

import com.motorola.studio.android.localization.translators.i18n.TranslateNLS;

/**
 * Uses the Google translator web service (via executing a http request and
 * parsing the answer) in order to translate a text string.
 * 
 * Google v2 supports only one source and one destination per request, but
 * support many words.
 * 
 */
@SuppressWarnings("restriction")
public final class GoogleTranslator extends ITranslator implements GoogleTranslatorConstants {

	/**
	 * Translate all words from some source language to destination language.
	 * This method handles any needed splits in original request due to API
	 * limitations.
	 * 
	 * @param words
	 *            the words being translated
	 * @param fromLanguage
	 *            the origin language
	 * @param toLanguage
	 *            the destination language
	 * @param monitor
	 *            progress monitor
	 * @return a list of translation results
	 * @throws Exception
	 */
	public List<TranslationResult> translate(List<String> words, String fromLanguage, String toLanguage,
			IProgressMonitor monitor) throws Exception {
		List<TranslationResult> translationResults = new ArrayList<TranslationResult>();
		int characterCount = 0;
		int MAX_REQUEST_SIZE = getMaxQuerySize(fromLanguage, toLanguage);

		int maxRequestSize = MAX_REQUEST_SIZE;
		List<String> wordsToTranslate = new ArrayList<String>();

		Iterator<String> wordsIterator = words.iterator();
		int counter = 0;

		while (wordsIterator.hasNext()) {
			maxRequestSize -= STRING_PAR.length();
			String word = wordsIterator.next();
			/*
			 * try to add some more words to the request. If there is no more
			 * room left to request, execute the translation and continue
			 * afterwards
			 */
			if (characterCount + word.length() < maxRequestSize) {
				wordsToTranslate.add(word);
				characterCount += word.length();
			} else {
				URL translationURL = createTranslationURL(wordsToTranslate, fromLanguage, toLanguage);
				String httpRequestResponseBody = executeHttpGetRequest(translationURL);

				List<String> responses = parseTranslationResponse(httpRequestResponseBody);

				for (int i = 0; i < wordsToTranslate.size(); i++) {
					translationResults.add(new TranslationResult(words.get(counter++), this, responses.get(i),
							fromLanguage, toLanguage, Calendar.getInstance().getTime(), true));
				}
				characterCount = 0;
				maxRequestSize = MAX_REQUEST_SIZE;
				wordsToTranslate.clear();
				wordsToTranslate.add(word);
			}
		}

		/*
		 * execute the request with remaining sentences
		 */
		if (!wordsToTranslate.isEmpty()) {
			URL translationURL = createTranslationURL(wordsToTranslate, fromLanguage, toLanguage);
			String httpRequestResponseBody = executeHttpGetRequest(translationURL);
			List<String> responses = parseTranslationResponse(httpRequestResponseBody);

			for (int i = 0; i < wordsToTranslate.size(); i++) {
				translationResults.add(new TranslationResult(words.get(counter++), this, responses.get(i),
						fromLanguage, toLanguage, Calendar.getInstance().getTime(), true));
			}
		}

		return translationResults;
	}

	/**
	 * Translates a string.
	 * 
	 * @param text
	 *            The String to be translated.
	 * @param from
	 *            Original language.
	 * @param to
	 *            Target language.
	 * @return The translated String.
	 * @throws Exception
	 *             on errors.
	 */
	@Override
	public TranslationResult translate(final String text, String from, String to) throws Exception {
		TranslationResult translationResult;

		if (text != null && !text.equals("") && text.length() < getMaxQuerySize(from, to)) //$NON-NLS-1$
		{
			String httpResult = ""; //$NON-NLS-1$
			URL url = null;

			// Creates the URL to be used as request
			try {
				List<String> sentences = new ArrayList<String>();
				sentences.add(text);
				url = createTranslationURL(sentences, from, to);
				httpResult = executeHttpGetRequest(url);
				translationResult = new TranslationResult(text, this, parseTranslationResponse(httpResult).get(0),
						from, to, Calendar.getInstance().getTime(), true);
			} catch (UnsupportedEncodingException e) {
				throw new HttpException(TranslateNLS.GoogleTranslator_Error_UnsupportedEncoding + ENCODING_TYPE);
			}

		} else if (text.length() >= getMaxQuerySize(from, to)) {
			throw new Exception(TranslateNLS.GoogleTranslator_Error_QueryTooBig);
		} else {
			translationResult = new TranslationResult(text, this, text, from, to, new Date(), true);
		}

		try {
			String descriptionToLog = UsageDataConstants.KEY_TRANSLATION_PROVIDER + UsageDataConstants.VALUE_GOOGLE
					+ UsageDataConstants.SEPARATOR + UsageDataConstants.KEY_TRANSLATION_FROM_LANG + from
					+ UsageDataConstants.SEPARATOR + UsageDataConstants.KEY_TRANSLATION_TO_LANG + to;
			StudioLogger.collectUsageData(UsageDataConstants.WHAT_LOCALIZATION_AUTOMATICTRANSLATION,
					UsageDataConstants.KIND_LOCALIZATION, descriptionToLog, TranslationPlugin.PLUGIN_ID,
					TranslationPlugin.getDefault().getBundle().getVersion().toString());
		} catch (Throwable t) {
			// Do nothing, usage data collection is for statistics and should
			// not prevent tool from work
		}

		return translationResult;

	}

	/**
	 * Translate a single word from one language to several other
	 * 
	 * @param sentences
	 *            sentence being translated
	 * @param fromLanguage
	 *            source language
	 * @param toLanguages
	 *            target languages
	 */
	@Override
	public List<TranslationResult> translate(String sentence, String fromLanguage, List<String> toLanguages)
			throws Exception {
		List<TranslationResult> translationResults = new ArrayList<TranslationResult>();

		// Lets start with some checkings, one can never be too careful
		if (fromLanguage == null || toLanguages == null || toLanguages.isEmpty()) {
			// We must have a FROM and a TO languages
			throw new IllegalArgumentException(TranslateNLS.GoogleTranslator_Error_ToAndFromLanguagesAreEmpty);
		} else if (sentence == null || sentence.equals("")) //$NON-NLS-1$
		{
			// We must have something to be translated
			sentence = ""; //$NON-NLS-1$
			if (toLanguages.size() == 1) {
				translationResults.add(new TranslationResult("", this, "", fromLanguage, //$NON-NLS-1$ //$NON-NLS-2$
						toLanguages.get(0), new Date(), true));
			}
		} else if (sentence.length() >= getMaxQuerySize(fromLanguage, toLanguages.get(0))) {
			throw new Exception(TranslateNLS.GoogleTranslator_Error_QueryTooBig);
		}
		/*
		 * Delegate the translation to another method
		 */
		else {
			for (String toLanguage : toLanguages) {
				translationResults.add(translate(sentence, fromLanguage, toLanguage));
			}
		}

		try {
			// Collecting usage data for statistic purposes
			String descriptionToLog = UsageDataConstants.KEY_TRANSLATION_PROVIDER + UsageDataConstants.VALUE_GOOGLE
					+ UsageDataConstants.SEPARATOR + UsageDataConstants.KEY_TRANSLATION_FROM_LANG + fromLanguage
					+ UsageDataConstants.SEPARATOR + UsageDataConstants.KEY_TRANSLATION_TO_LANG + "several languages"; //$NON-NLS-1$

			StudioLogger.collectUsageData(UsageDataConstants.WHAT_LOCALIZATION_AUTOMATICTRANSLATION,
					UsageDataConstants.KIND_LOCALIZATION, descriptionToLog, TranslationPlugin.PLUGIN_ID,
					TranslationPlugin.getDefault().getBundle().getVersion().toString());
		} catch (Throwable t) {
			// Do nothing, usage data collection is for statistics and should
			// not prevent tool from work
		}

		return translationResults;
	}

	/**
	 * Translate a list of sentences from one language to another
	 * 
	 * @param sentences
	 *            sentences being translated
	 * @param fromLanguage
	 *            source language
	 * @param toLanguage
	 *            target language
	 * @param monitor
	 *            progress monitor
	 */
	@Override
	public List<TranslationResult> translateAll(List<String> sentences, String fromLanguage, String toLanguage,
			IProgressMonitor monitor) throws Exception {
		// The result (duhh)
		List<TranslationResult> translationResults = new ArrayList<TranslationResult>();

		// Lets start with some checkings, one can never be too carefull
		if (fromLanguage == null || toLanguage == null) {
			// We must have a FROM and a TO languages
			throw new IllegalArgumentException(TranslateNLS.GoogleTranslator_Error_ToAndFromLanguagesAreEmpty);
		} else if (sentences == null || sentences.size() == 0) {
			// We must have something to be translated
			throw new IllegalArgumentException(TranslateNLS.GoogleTranslator_Error_NoAvailableData);
		} else {
			translationResults.addAll(translate(sentences, fromLanguage, toLanguage, monitor));
		}

		try {
			// Collecting usage data for statistic purposes
			String descriptionToLog = UsageDataConstants.KEY_TRANSLATION_PROVIDER + UsageDataConstants.VALUE_GOOGLE
					+ UsageDataConstants.SEPARATOR + UsageDataConstants.KEY_TRANSLATION_FROM_LANG + fromLanguage
					+ UsageDataConstants.SEPARATOR + UsageDataConstants.KEY_TRANSLATION_TO_LANG + toLanguage;

			StudioLogger.collectUsageData(UsageDataConstants.WHAT_LOCALIZATION_AUTOMATICTRANSLATION,
					UsageDataConstants.KIND_LOCALIZATION, descriptionToLog, TranslationPlugin.PLUGIN_ID,
					TranslationPlugin.getDefault().getBundle().getVersion().toString());
		} catch (Throwable t) {
			// Do nothing, usage data collection is for statistics and should
			// not prevent tool from work
		}

		return translationResults;
	}

	/**
	 * Translates a list of strings from a list of given languages to other
	 * given languages (given by a list, or course), using google Ajax API's for
	 * that.
	 * 
	 * The three lists have the same number of elements.
	 * 
	 * This comment feels like the
	 * "Three Swatch watch switching witches watched switched Swatch watch witches switch"
	 * , but I'll let it here anyway.
	 */
	@Override
	public List<TranslationResult> translateAll(List<String> words, List<String> fromLanguage, List<String> toLanguage,
			IProgressMonitor monitor) throws Exception {
		// The result (duhh)
		List<TranslationResult> translationResults = new ArrayList<TranslationResult>();

		// Lets start with some checkings, one can never be too carefull
		if (fromLanguage == null || toLanguage == null) {
			// We must have a FROM and a TO languages
			throw new IllegalArgumentException(TranslateNLS.GoogleTranslator_Error_ToAndFromLanguagesAreEmpty);
		} else if (words == null || words.size() == 0) {
			// We must have something to be translated
			throw new IllegalArgumentException(TranslateNLS.GoogleTranslator_Error_NoAvailableData);
		} else {
			translationResults.addAll(groupAndTranslate(words, fromLanguage, toLanguage, monitor));
		}

		try {
			// Collecting usage data for statistic purposes
			String descriptionToLog = UsageDataConstants.KEY_TRANSLATION_PROVIDER + UsageDataConstants.VALUE_GOOGLE
					+ UsageDataConstants.SEPARATOR + UsageDataConstants.KEY_TRANSLATION_FROM_LANG + fromLanguage
					+ UsageDataConstants.SEPARATOR + UsageDataConstants.KEY_TRANSLATION_TO_LANG + toLanguage;

			StudioLogger.collectUsageData(UsageDataConstants.WHAT_LOCALIZATION_AUTOMATICTRANSLATION,
					UsageDataConstants.KIND_LOCALIZATION, descriptionToLog, TranslationPlugin.PLUGIN_ID,
					TranslationPlugin.getDefault().getBundle().getVersion().toString());
		} catch (Throwable t) {
			// Do nothing, usage data collection is for statistics and should
			// not prevent tool from work
		}

		return translationResults;
	}

	private List<TranslationResult> groupAndTranslate(List<String> words, List<String> fromLanguage,
			List<String> toLanguage, IProgressMonitor monitor) throws Exception {

		List<TranslationResult> results = new ArrayList<TranslationResult>();
		/*
		 * Get all words with same source and same destination and group them to
		 * make translation more efficient Notice that this implementation
		 * relies on input condition that all lists have the same size and all
		 * elements with same index makes one request
		 */

		class StringItem {
			private String sentence = "";

			private int orderNumber = -1;

			public StringItem(String sentence, int orderNumber) {
				this.sentence = sentence;
				this.orderNumber = orderNumber;
			}

			public Integer getOrderNumber() {
				return orderNumber;
			}

			public String getSentence() {
				return sentence;
			}

			@Override
			public String toString() {
				return orderNumber + "|" + sentence;
			}
		}

		/*
		 * This map holds a list of group of stringitems being translated. These
		 * items have the same from and to languages Using linkedHashMap to keep
		 * insertionOrder
		 */
		Map<String, List<StringItem>> sameSourceDestMap = new LinkedHashMap<String, List<StringItem>>();
		Map<Integer, TranslationResult> translations = new HashMap<Integer, TranslationResult>();

		// group things with same from and to languages
		for (int i = 0; i < words.size(); i++) {
			/*
			 * Check if one of the words are big enough to not be translated
			 */
			if (words.get(i).length() >= getMaxQuerySize(fromLanguage.get(i), toLanguage.get(i)) - STRING_PAR.length()
					- 1) {
				throw new Exception(TranslateNLS.GoogleTranslator_Error_QueryTooBig);
			}

			String key = fromLanguage.get(i) + "|" + toLanguage.get(i);
			List<StringItem> itemsToTranslate = sameSourceDestMap.get(key);
			if (itemsToTranslate == null) {
				itemsToTranslate = new ArrayList<StringItem>();
				sameSourceDestMap.put(key, itemsToTranslate);
			}
			itemsToTranslate.add(new StringItem(words.get(i), i));
		}

		for (String key : sameSourceDestMap.keySet()) {
			List<StringItem> itemsToTranslate = sameSourceDestMap.get(key);
			List<String> items = new ArrayList<String>();
			for (StringItem item : itemsToTranslate) {
				items.add(item.getSentence());
			}
			List<TranslationResult> tempResults = translate(items, key.split("\\|")[0], key.split("\\|")[1], monitor);
			for (int i = 0; i < itemsToTranslate.size(); i++) {
				translations.put(itemsToTranslate.get(i).getOrderNumber(), tempResults.get(i));
			}

		}

		for (int i = 0; i < words.size(); i++) {
			results.add(translations.get(i));
		}

		return results;
	}

	private int getMaxQuerySize(String fromLanguage, String toLanguage) {
		return MAX_QUERY_SIZE - TRANSLATE_URL_WITHOUT_PARAMS.length() - API_KEY_PARAM.length() - getApiKey().length()
				- SOURCE_PAR.length() - fromLanguage.length() - TARGET_PARAM.length() - toLanguage.length();
	}

	/**
	 * Parse the translation response of the http request
	 * 
	 * @param httpRequestResponseBody
	 *            the response body
	 * @param sourceLanguage
	 *            the source language
	 * @param destinationLanguage
	 *            the destination language
	 * @return a list of String objects for the strings translated for
	 *         source/destination languages pair
	 */
	private List<String> parseTranslationResponse(String httpRequestResponseBody) {
		JSONPair translationSection = getTranslationSection(httpRequestResponseBody);

		return getTranslations(translationSection);
	}

	/*
	 * { "data": { "translations": [ { "translatedText": "bla bla bla" }, {
	 * "translatedText": "foo bar" } ] } }
	 */
	private JSONPair getTranslationSection(String httpRequestResponseBody) {
		Jason ripper = new Jason(httpRequestResponseBody);
		JSONPair translationsSection = null;
		Iterator<JSONObject> jsonIterator = ripper.getJSON().iterator();
		while (translationsSection == null && jsonIterator.hasNext()) {
			translationsSection = findPair(jsonIterator.next(), TRANSLATIONS_SECTION);
		}

		return translationsSection;
	}

	private static JSONPair findPair(JSONValue origin, String name) {
		JSONPair pair = null;
		if (origin instanceof JSONObject) {
			JSONObject object = (JSONObject) origin;
			Iterator<JSONPair> pairs = object.getValue().iterator();
			while (pair == null && pairs.hasNext()) {
				JSONPair jsonPair = pairs.next();
				if (jsonPair.getName().equals(name)) {
					pair = jsonPair;
				} else {
					pair = findPair(jsonPair.getValue(), name);
				}

			}
		}

		return pair;
	}

	private List<String> getTranslations(JSONPair translationSection) {
		List<String> translations = new ArrayList<String>();
		if (translationSection.getValue() instanceof JSONArray) {
			JSONArray translationsArray = (JSONArray) translationSection.getValue();
			for (JSONValue translationObject : translationsArray.getValue()) {
				translations.add(getTranslation(translationObject));
			}
		}

		return translations;
	}

	/**
	 * @param translationObject
	 *            { "translatedText": "Hallo Welt" }
	 * @return pure translation Hallo Welt
	 */
	private String getTranslation(JSONValue translationObject) {
		String translation = null;
		if (translationObject instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject) translationObject;
			translation = ((JSONString) jsonObject.getValue().iterator().next().getValue()).getValue();
		}
		return translation != null ? fixHTMLTags(translation) : null;
	}

	private URL createTranslationURL(List<String> wordsToTranslate, String fromLanguage, String toLanguage)
			throws UnsupportedEncodingException {
		URL translationURL = null;

		// We need to unescape the ' (apostrophe) before sending it to
		// translation
		String regex = "\\\\'"; //$NON-NLS-1$
		Pattern pattern = Pattern.compile(regex);

		StringBuilder urlBuilder = new StringBuilder(TRANSLATE_URL_WITHOUT_PARAMS);
		urlBuilder.append(URL_PARAMETERS.replace("#FROM#", fromLanguage).replace("#TO#", toLanguage)
				.replace("#API_KEY#", getApiKey()));

		for (String word : wordsToTranslate) {
			String wordToTranslate = pattern.matcher(word).replaceAll("'");
			urlBuilder.append(STRING_PAR);
			urlBuilder.append(URLEncoder.encode(wordToTranslate, ENCODING_TYPE));
		}

		try {
			translationURL = new URL(urlBuilder.toString());
		} catch (MalformedURLException e) {
			StudioLogger.error(getClass(), "Unable to create translation URL", e);
		}

		return translationURL;
	}

	/**
	 * The Android localization files text must accept three HTML tags: i, b and
	 * u. Nevertheless, google translator returns the close part of this tags
	 * with a extra-space that makes the sintax wrong. This method will try to
	 * fix it.
	 * 
	 * @param originalText
	 * @return the text with the tags fixed
	 */
	private static String fixHTMLTags(String originalText) {
		String result = ""; //$NON-NLS-1$
		if (originalText != null) {
			result = originalText;
		}
		result = originalText.replaceAll("</ b>", "</b>"); //$NON-NLS-1$ //$NON-NLS-2$
		result = originalText.replaceAll("</ i>", "</i>"); //$NON-NLS-1$ //$NON-NLS-2$
		result = originalText.replaceAll("</ u>", "</u>"); //$NON-NLS-1$ //$NON-NLS-2$

		result = originalText.replaceAll("</ B>", "</B>"); //$NON-NLS-1$ //$NON-NLS-2$
		result = originalText.replaceAll("</ I>", "</I>"); //$NON-NLS-1$ //$NON-NLS-2$
		result = originalText.replaceAll("</ U>", "</U>"); //$NON-NLS-1$ //$NON-NLS-2$

		result = originalText.replaceAll(" \\\\ n ", " \\\\n "); //$NON-NLS-1$ //$NON-NLS-2$

		return result;
	}

	/**
	 * Creates an HTTP request with the URL, execute it as a get, and returns
	 * the a string with the result.
	 * 
	 * @param url
	 *            URL to be executed.
	 * @return String with the URL execution result.
	 * @throws IOException
	 *             If an exception occurs on transport
	 * @throws HttpException
	 *             If an exception occurs on the protocol
	 * @throws Exception
	 *             on error.
	 */
	protected static String executeHttpGetRequest(final URL url) throws HttpException {

		// Checking query size due to google policies
		if (url.toString().length() > MAX_QUERY_SIZE) {
			throw new HttpException(TranslateNLS.GoogleTranslator_Error_QueryTooBig);
		}

		// Try to retrieve proxy configuration to use if necessary
		IProxyService proxyService = ProxyManager.getProxyManager();
		IProxyData proxyData = null;
		if (proxyService.isProxiesEnabled() || proxyService.isSystemProxiesEnabled()) {
			Authenticator.setDefault(new ProxyAuthenticator());
			String urlStr = url.toString();
			if (urlStr.startsWith("https")) {
				proxyData = proxyService.getProxyData(IProxyData.HTTPS_PROXY_TYPE);
				StudioLogger.debug(GoogleTranslator.class, "Using https proxy"); //$NON-NLS-1$
			} else if (urlStr.startsWith("http")) {
				proxyData = proxyService.getProxyData(IProxyData.HTTP_PROXY_TYPE);
				StudioLogger.debug(GoogleTranslator.class, "Using http proxy"); //$NON-NLS-1$
			} else {
				StudioLogger.debug(GoogleTranslator.class, "Not using any proxy"); //$NON-NLS-1$
			}
		}

		// Creates the http client and the method to be executed
		HttpClient client = null;
		client = new HttpClient();

		// If there is proxy data, work with it
		if (proxyData != null) {
			if (proxyData.getHost() != null) {
				// Sets proxy host and port, if any
				client.getHostConfiguration().setProxy(proxyData.getHost(), proxyData.getPort());
			}

			if (proxyData.getUserId() != null && proxyData.getUserId().trim().length() > 0) {
				// Sets proxy user and password, if any
				Credentials cred = new UsernamePasswordCredentials(proxyData.getUserId(),
						proxyData.getPassword() == null ? "" : proxyData.getPassword()); //$NON-NLS-1$
				client.getState().setProxyCredentials(AuthScope.ANY, cred);
			}
		}

		// Creating the method to be executed, the URL at this point is enough
		// because it is complete
		GetMethod method = new GetMethod(url.toString());

		// Set method to be retried three times in case of error
		method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
				new DefaultHttpMethodRetryHandler(RETRIES, false));

		method.setRequestHeader(REFERER_HEADER, REFERER_SITE);

		// Set the connection timeout
		client.getHttpConnectionManager().getParams().setConnectionTimeout(new Integer(TIMEOUT));

		String result = ""; //$NON-NLS-1$
		try {
			// Execute the method.
			int statusCode;
			try {
				statusCode = client.executeMethod(method);
				result = method.getResponseBodyAsString(MAX_SIZE);
			} catch (IOException e) {
				throw new HttpException(TranslateNLS.GoogleTranslator_Error_CannotConnectToServer + e.getMessage());
			}

			checkStatusCode(statusCode, result);

			// Unescape any possible unicode char
			result = unescapeUnicode(result);

			// Unescape any possible HTML sequence
			result = unescapeHTML(result);

		}

		finally {
			// Release the connection.
			method.releaseConnection();
		}

		return result;
	}

	private static void checkStatusCode(int statusCode, String response) throws HttpException {
		switch (statusCode) {
		case HttpStatus.SC_OK:
			// do nothing
			break;
		case HttpStatus.SC_BAD_REQUEST:
			throw new HttpException(NLS.bind(TranslateNLS.GoogleTranslator_ErrorMessageExecutingRequest,
					getErrorMessage(response)));

		case HttpStatus.SC_REQUEST_URI_TOO_LONG:
			throw new HttpException(TranslateNLS.GoogleTranslator_Error_QueryTooBig);

		case HttpStatus.SC_FORBIDDEN:
			throw new HttpException(NLS.bind(TranslateNLS.GoogleTranslator_ErrorMessageNoValidTranslationReturned,
					getErrorMessage(response)));

		default:
			throw new HttpException(NLS.bind(TranslateNLS.GoogleTranslator_Error_HTTPRequestError, new Object[] {
					statusCode, getErrorMessage(response) }));

		}
	}

	/**
	 * According to APIv2, the error message is in the end of the response {
	 * "error": { "errors": [ { "domain": "global", "reason": "invalid",
	 * "message": "Invalid Value" } ], "code": 400, "message": "Invalid Value" }
	 * }
	 * 
	 * @param response
	 *            the method response body
	 * @return the error message
	 */
	private static String getErrorMessage(String response) {
		Jason ripper = new Jason(response);
		JSONPair translationsSection = null;
		Iterator<JSONObject> jsonIterator = ripper.getJSON().iterator();
		while (translationsSection == null && jsonIterator.hasNext()) {
			translationsSection = findPair(jsonIterator.next(), MESSAGE_TEXT);
		}

		return translationsSection != null ? ((JSONString) translationsSection.getValue()).getValue() : null;
	}

	/**
	 * Unescape any HTML sequence that exists inside the string. For example,
	 * the sequence &#39; will be changed to the ' symbol
	 * 
	 * @param source
	 *            original text
	 * @return the result
	 */
	private static String unescapeHTML(String source) {
		Pattern p = Pattern.compile("&#([0-9]+);"); //$NON-NLS-1$
		String result = source;
		Matcher m = p.matcher(result);
		while (m.find()) {
			char c = (char) Integer.parseInt(m.group(1));
			if (c == "'".charAt(0)) //$NON-NLS-1$
			{
				// Apostrophes must be escaped by preceding it with a backslash
				// (\) on the XML file
				result = result.replaceAll(m.group(0), "\\\\'"); //$NON-NLS-1$
			} else {
				result = result.replaceAll(m.group(0), "" + c); //$NON-NLS-1$
			}
		}

		return result;
	}

	/**
	 * Unescape any Unicode sequence that exists inside the string.
	 * 
	 * For example, the sequence \u0000 will be changed to the symbol
	 * correnponded to the 0000 unicode value.
	 * 
	 * @param source
	 *            original text
	 * @return the result
	 */
	private static String unescapeUnicode(String source) {
		int i = 0, len = source.length();
		char c;
		StringBuffer buffer = new StringBuffer(len);
		while (i < len) {
			c = source.charAt(i++);
			if (c == '\\') {
				if (i < len) {
					c = source.charAt(i++);
					if (c == 'u') {
						c = (char) Integer.parseInt(source.substring(i, i + 4), 16);
						i += 4;
					}
				}
			}
			buffer.append(c);
		}
		return buffer.toString();
	}

	private static String getApiKey() {
		String apiKey = GoogleTranslatorConstants.API_KEY;
		IPreferenceStore prefStore = TranslationPlugin.getDefault().getPreferenceStore();
		if (!prefStore.isDefault(GoogleTranslatorConstants.API_KEY_VALUE_PREFERENCE)) {
			apiKey = prefStore.getString(GoogleTranslatorConstants.API_KEY_VALUE_PREFERENCE);
			if (apiKey == null) {
				apiKey = GoogleTranslatorConstants.API_KEY;
			}
		}

		return apiKey;
	}

	@Override
	public Composite createCustomArea(Composite parent, final ITranslateDialog dialog) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayout(new GridLayout(1, false));

		final Link prefPageLink = new Link(mainComposite, SWT.NONE);
		prefPageLink.setText(TranslateNLS.GoogleTranslator_ChangeAPIkeyLabel);
		prefPageLink.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true, 1, 1));
		prefPageLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseUtils.openPreference(prefPageLink.getShell(),
						"org.eclipse.andmore.android.localization.translators.preferencepage"); //$NON-NLS-1$
				dialog.validate();
			}
		});
		mainComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
		return mainComposite;
	}

	@Override
	public String canTranslate(String fromLanguage, String[] toLanguages) {
		return getApiKey() == null || GoogleTranslatorConstants.API_KEY.equals(getApiKey()) ? TranslateNLS.GoogleTranslator_ErrorNoAPIkeySet
				: null;
	}
}