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
 * Marcelo Marzola Bossoni (Eldorado) - Bug [289146] - Performance and Usability Issues
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.datamodel;

import java.util.Date;

import org.eclipse.sequoyah.localization.tools.persistence.IPersistentMetaData;
import org.eclipse.sequoyah.localization.tools.persistence.PersistableAttributes;

/**
 * This class stores meta-info about the translation process and is intended to
 * be persisted as part of the meta-info related to a StringNode object
 */
public class TranslationDetails implements IPersistentMetaData {

	/*
	 * The StringNode this info is related to
	 */
	private StringNode stringNode;

	/*
	 * The name of the Translator used in the process
	 */
	private String translator;

	/*
	 * When the process took place
	 */
	private Date date;

	/*
	 * Whether the process succeeded or not
	 */
	private boolean success;

	/**
	 * Get the StringNode this info is related to
	 * 
	 * @return the StringNode this info is related to
	 */
	public StringNode getStringNode() {
		return stringNode;
	}

	/**
	 * Set the StringNode this info is related to
	 * 
	 * @param stringNode
	 *            the StringNode this info is related to
	 */
	public void setStringNode(StringNode stringNode) {
		this.stringNode = stringNode;
	}

	/**
	 * Get the name of the Translator used in the process
	 * 
	 * @return the name of the Translator used in the process
	 */
	public String getTranslator() {
		return translator;
	}

	/**
	 * Set the name of the Translator used in the process
	 * 
	 * @param translator
	 *            the name of the Translator used in the process
	 */
	public void setTranslator(String translator) {
		this.translator = translator;
	}

	/**
	 * Get when the translator process took place
	 * 
	 * @return when the grammar checker process took place
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * Set when the translation process took place
	 * 
	 * @param date
	 *            when the translation process took place
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * Check whether the translation process succeeded or not
	 * 
	 * @return true if the translation process succeeded, false otherwise
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * Set whether the translation process succeeded or not
	 * 
	 * @param success
	 *            true if the translation process succeeded, false otherwise
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.tools.persistence.IPersistentData#
	 * getPersistableAttributes()
	 */
	public PersistableAttributes getPersistableAttributes() {
		// TODO: implement persistence
		return null;
	}

}
