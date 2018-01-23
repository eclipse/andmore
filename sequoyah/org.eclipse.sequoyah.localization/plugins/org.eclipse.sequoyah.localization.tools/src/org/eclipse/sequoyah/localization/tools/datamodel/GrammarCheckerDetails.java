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
 * This class stores meta-info about the grammar checker process and is intended
 * to be persisted as part of the meta-data related to a StringNode object
 */
public class GrammarCheckerDetails implements IPersistentMetaData {

	/*
	 * The StringNode this info is related to
	 */
	private StringNode stringNode;

	/*
	 * The name of the Grammar Checker used in the process
	 */
	private String grammarChecker;

	/*
	 * When the process took place
	 */
	private Date date;

	/*
	 * Whether the process succeeded or not
	 */
	private boolean success;

	/**
	 * Constructor class
	 * 
	 * @param stringNode
	 */
	public GrammarCheckerDetails(StringNode stringNode) {
		this.stringNode = stringNode;
	}

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
	 * Get the name of the Grammar Checker used in the process
	 * 
	 * @return the name of the Grammar Checker used in the process
	 */
	public String getGrammarChecker() {
		return grammarChecker;
	}

	/**
	 * Set the name of the Grammar Checker used in the process
	 * 
	 * @param grammarChecker
	 *            the name of the Grammar Checker used in the process
	 */
	public void setGrammarChecker(String grammarChecker) {
		this.grammarChecker = grammarChecker;
	}

	/**
	 * Get when the grammar checker process took place
	 * 
	 * @return when the grammar checker process took place
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * Set when the grammar checker process took place
	 * 
	 * @param date
	 *            when the grammar checker process took place
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * Check whether the grammar checker process succeeded or not
	 * 
	 * @return true if the grammar checker process succeeded, false otherwise
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * Set whether the grammar checker process succeeded or not
	 * 
	 * @param success
	 *            true if the grammar checker process succeeded, false otherwise
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}

	/**
	 * @see org.eclipse.sequoyah.localization.tools.persistence.IPersistentData#getPersistableAttributes()
	 */
	public PersistableAttributes getPersistableAttributes() {
		return null;
	}

}
