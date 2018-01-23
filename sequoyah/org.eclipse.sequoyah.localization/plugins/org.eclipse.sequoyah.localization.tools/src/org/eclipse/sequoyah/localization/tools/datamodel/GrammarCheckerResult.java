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
package org.eclipse.sequoyah.localization.tools.datamodel;

import java.util.Date;

import org.eclipse.sequoyah.localization.tools.extensions.classes.IGrammarChecker;

/**
 *
 */
public class GrammarCheckerResult {

	private String word;

	private IGrammarChecker grammarChecker;

	private String language;

	private Date date;

	private boolean correct;

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
	public IGrammarChecker getGrammarChecker() {
		return grammarChecker;
	}

	/**
	 * @param grammarChecker
	 */
	public void setGrammarChecker(IGrammarChecker grammarChecker) {
		this.grammarChecker = grammarChecker;
	}

	/**
	 * @return
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * @param language
	 */
	public void setLanguage(String language) {
		this.language = language;
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
	 * @param correct
	 */
	public void setCorrect(boolean correct) {
		this.correct = correct;
	}

	/**
	 * @return
	 */
	public boolean isCorrect() {
		return correct;
	}

}
