/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
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
 * Paulo Faria (Eldorado) - Add methods for not to lose comments on save (currently only on update)
 * Marcel Augusto Gorri (Eldorado) - Bug 323036 - Add support to other Localizable Resources
 * 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.datamodel.node;

/**
 * This class represents an entry in a localization file for text.
 * 
 */
public class StringNode extends Node {
	/*
	 * Details of the translation process applied to the node, if any
	 */
	protected TranslationDetails translationDetails = null;

	/*
	 * Details of the grammar checker process applied to the node, if any
	 */
	protected GrammarCheckerDetails grammarCheckerDetails = null;

	/*
	 * Whether the value has been generated using the translation process or not
	 */
	protected boolean translated;

	/*
	 * Whether the value has been checked using the grammar checker process or
	 * not
	 */
	protected boolean checked;

	/**
	 * Constructor method
	 * 
	 * @param key
	 * @param value
	 */
	public StringNode(String key, String value) {
		super(key, value);
	}

	/**
	 * Get the details of the translation process applied to the node, if any
	 * 
	 * @return TranslationDetails object with details of the translation process
	 *         applied to the node
	 */
	public TranslationDetails getTranslationDetails() {
		return translationDetails;
	}

	/**
	 * Set the details of the translation process applied to the node, if any
	 * 
	 * @param translationDetails
	 *            TranslationDetails object with details of the translation
	 *            process applied to the node
	 */
	public void setTranslationDetails(TranslationDetails translationDetails) {
		this.translationDetails = translationDetails;
	}

	/**
	 * Get the details of the grammar checker process applied to the node, if
	 * any
	 * 
	 * @return GrammarCheckerDetails object with details of the grammar checker
	 *         process applied to the node
	 */
	public GrammarCheckerDetails getGrammarCheckerDetails() {
		return grammarCheckerDetails;
	}

	/**
	 * Set the details of the grammar checker process applied to the node, if
	 * any
	 * 
	 * @param grammarCheckerDetails
	 *            GrammarCheckerDetails object with details of the grammar
	 *            checker process applied to the node
	 */
	public void setGrammarCheckerDetails(
			GrammarCheckerDetails grammarCheckerDetails) {
		this.grammarCheckerDetails = grammarCheckerDetails;
	}

	/**
	 * Check whether the value has been generated using the translation process
	 * or not
	 * 
	 * @return true if the value has been generated using the translation
	 *         process, false otherwise
	 */
	public boolean isTranslated() {
		return translated;
	}

	/**
	 * Set whether the value has been generated using the translation process or
	 * not
	 * 
	 * @param translated
	 *            true if the value has been generated using the translation
	 *            process, false otherwise
	 */
	public void setTranslated(boolean translated) {
		this.translated = translated;
	}

	/**
	 * Check whether the value has been checked using the grammar checker
	 * process or not
	 * 
	 * @return true if the value has been checked using the grammar checker
	 *         process, false otherwise
	 */
	public boolean isChecked() {
		return checked;
	}

	/**
	 * Set whether the value has been checked using the grammar checker process
	 * or not
	 * 
	 * @param checked
	 *            true if the value has been checked using the grammar checker
	 *            process, false otherwise
	 */
	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	/**
	 * Delete this StringNode. - Remove it from the model - Remove its comment,
	 * if any - Remove all meta-data associated - Remove all extra-info
	 * associated
	 */
	public void delete() {
		// TODO: implement delete
	}

	/**
	 * Check if the StringNode has any meta-data associated
	 * 
	 * @return true if the StringNode has any meta-data associated, false
	 *         otherwise
	 */
	public boolean hasMetaData() {
		// TODO: implement hasMetadata
		return false;
	}

	/**
	 * Check if the StringNode has any extra-info associated
	 * 
	 * @return true if the StringNode has any extra-info associated, false
	 *         otherwise
	 */
	public boolean hasExtraInfo() {
		// TODO: implement hasExtraInfo
		return false;
	}

}