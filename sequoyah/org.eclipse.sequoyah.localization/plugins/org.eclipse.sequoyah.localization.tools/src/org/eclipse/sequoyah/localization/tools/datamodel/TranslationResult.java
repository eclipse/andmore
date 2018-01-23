/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Inc.
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
package org.eclipse.sequoyah.localization.tools.datamodel;

import java.util.Date;

import org.eclipse.sequoyah.localization.tools.extensions.classes.ITranslator;

/**
 *
 */
public class TranslationResult {

	private String word;

	private ITranslator translator;

	private String translatedWord;

	private String originalLanguage;

	private String translatedLanguage;

	private Date date;

	private boolean succesful;

	public TranslationResult(String word, ITranslator translator,
			String translatedWord, String originalLanguage,
			String translatedLanguage, Date date, boolean succesful) {
		super();
		this.word = word;
		this.translator = translator;
		this.translatedWord = translatedWord;
		this.originalLanguage = originalLanguage;
		this.translatedLanguage = translatedLanguage;
		this.date = date;
		this.succesful = succesful;
	}

	/**
	 * @return
	 */
	public String getWord() {
		return word;
	}

	/**
	 * @param word
	 */
	public void setWord(String word) {
		this.word = word;
	}

	/**
	 * @return
	 */
	public ITranslator getTranslator() {
		return translator;
	}

	/**
	 * @param translator
	 */
	public void setTranslator(ITranslator translator) {
		this.translator = translator;
	}

	/**
	 * @return
	 */
	public String getTranslatedWord() {
		return translatedWord;
	}

	/**
	 * @param translatedWord
	 */
	public void setTranslatedWord(String translatedWord) {
		this.translatedWord = translatedWord;
	}

	/**
	 * @return
	 */
	public String getOriginalLanguage() {
		return originalLanguage;
	}

	/**
	 * @param originalLanguage
	 */
	public void setOriginalLanguage(String originalLanguage) {
		this.originalLanguage = originalLanguage;
	}

	/**
	 * @return
	 */
	public String getTranslatedLanguage() {
		return translatedLanguage;
	}

	/**
	 * @param translatedLanguage
	 */
	public void setTranslatedLanguage(String translatedLanguage) {
		this.translatedLanguage = translatedLanguage;
	}

	/**
	 * @return
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * @param date
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * @return
	 */
	public boolean isSuccesful() {
		return succesful;
	}

	/**
	 * @param succesful
	 */
	public void setSuccesful(boolean succesful) {
		this.succesful = succesful;
	}

}
