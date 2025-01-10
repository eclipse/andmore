/********************************************************************************
 * Copyright (c) 2010 Motorola Inc.
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
 * 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.datamodel;

/**
 * This class represents an entry in a localization file.
 * 
 * It basically stores a <key>:<value> pair, which contains the key used in the
 * localization process and the associated value in that specific language
 * represented by the given localization file.
 * 
 * It also stores or refers to any state data or meta-data associated with the
 * <key>:<value> pair that is represented by it.
 */
public class StringNode implements Comparable<StringNode> {

	/*
	 * The LocalizationFile which the StringNode belongs to
	 */
	private LocalizationFile localizationFile = null;

	/*
	 * Details of the translation process applied to the node, if any
	 */
	private TranslationDetails translationDetails = null;

	/*
	 * Details of the grammar checker process applied to the node, if any
	 */
	private GrammarCheckerDetails grammarCheckerDetails = null;

	/*
	 * Comment associated to the node
	 */
	private StringNodeComment stringNodeComment = null;

	/*
	 * Key used in the localization process
	 */
	private String key = null;

	/*
	 * Associated value for the language represented by the localizationFile
	 */
	private String value = null;

	/*
	 * Whether the value has been generated using the translation process or not
	 */
	private boolean translated;

	/*
	 * Whether the value has been checked using the grammar checker process or
	 * not
	 */
	private boolean checked;

	/*
	 * Whether the value has been edited by the Localization Editor and differs
	 * from the value saved in the localizationFile
	 */
	private boolean dirty;

	/*
	 * Whether there are changes in the associated meta-data
	 */
	private boolean dirtyMetaData;

	/*
	 * Whether there are changes in the associated extra-info
	 */
	private boolean dirtyExtraInfo;

	private boolean isArray;

	private StringArray stringArray;

	/**
	 * Constructor method
	 * 
	 * @param key
	 * @param value
	 */
	public StringNode(String key, String value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * Get the LocalizationFile which the StringNode belongs to
	 * 
	 * @return the LocalizationFile which the StringNode belongs to
	 */
	public LocalizationFile getLocalizationFile() {
		return localizationFile;
	}

	/**
	 * Set the LocalizationFile which the StringNode belongs to
	 * 
	 * @param localizationFile
	 *            the LocalizationFile which the StringNode belongs to
	 */
	public void setLocalizationFile(LocalizationFile localizationFile) {
		this.localizationFile = localizationFile;
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
	 * Get the comment associated to the node
	 * 
	 * @return comment associated to the node
	 */
	public StringNodeComment getStringNodeComment() {
		return stringNodeComment;
	}

	/**
	 * Set the comment associated to the node
	 * 
	 * @param stringNodeComment
	 *            comment to be associated to the node
	 */
	public void setStringNodeComment(StringNodeComment stringNodeComment) {
		this.stringNodeComment = stringNodeComment;
		if (this.stringNodeComment != null) {
			this.stringNodeComment.setStringNode(this);
		}
	}

	/**
	 * Get the key used in the localization process
	 * 
	 * @return key used in the localization process
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Set the key used in the localization process
	 * 
	 * @param key
	 *            key used in the localization process
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Get the value associated to the key for the language represented by the
	 * localizationFile
	 * 
	 * @return value associated to the key
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Set the value associated to the key for the language represented by the
	 * localizationFile
	 * 
	 * @param value
	 *            value associated to the key
	 */
	public void setValue(String value) {
		if ((this.value != null) && (!this.value.equals(value))) {
			setDirty(true);
		}
		this.value = value;
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
	 * Check whether the value has been edited by the Localization Editor and
	 * differs from the value saved in the localizationFile
	 * 
	 * @return true if he value has been edited by the Localization Editor and
	 *         differs from the value saved in the localizationFile, false
	 *         otherwise
	 */
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * Set whether the value has been edited by the Localization Editor and
	 * differs from the value saved in the localizationFile
	 * 
	 * @param dirty
	 *            true if he value has been edited by the Localization Editor
	 *            and differs from the value saved in the localizationFile,
	 *            false otherwise
	 */
	public void setDirty(boolean dirty) {
		// propagate the state if dirty = true
		if (dirty) {
			this.getLocalizationFile().setDirty(dirty);
		}
		this.dirty = dirty;
	}

	/**
	 * Check whether there are changes in the associated meta-data
	 * 
	 * @return true if there are changes in the associated meta-data, false
	 *         otherwise
	 */
	public boolean isDirtyMetaData() {
		return dirtyMetaData;
	}

	/**
	 * Set whether there are changes in the associated meta-data
	 * 
	 * @param dirtyMetaData
	 *            true if there are changes in the associated meta-data, false
	 *            otherwise
	 */
	public void setDirtyMetaData(boolean dirtyMetaData) {
		this.dirtyMetaData = dirtyMetaData;
	}

	/**
	 * Check whether there are changes in the associated extra-info
	 * 
	 * @return true if there are changes in the associated extra-info, false
	 *         otherwise
	 */
	public boolean isDirtyExtraInfo() {
		return dirtyExtraInfo;
	}

	/**
	 * Set whether there are changes in the associated extra-info
	 * 
	 * @param dirtyExtraInfo
	 *            true if there are changes in the associated extra-info, false
	 *            otherwise
	 */
	public void setDirtyExtraInfo(boolean dirtyExtraInfo) {
		this.dirtyExtraInfo = dirtyExtraInfo;
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

	public boolean isArray() {
		return isArray;
	}

	public void setArray(boolean isArray) {
		this.isArray = isArray;
	}

	public StringArray getStringArray() {
		return stringArray;
	}

	public void setStringArray(StringArray stringArray) {
		this.stringArray = stringArray;
	}

	public int compareTo(StringNode o) {
		return this.getKey().compareTo(o.getKey());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringNode other = (StringNode) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}

}
