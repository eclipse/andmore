/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Inc.
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
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.datamodel;

import java.util.Date;

import org.eclipse.sequoyah.localization.tools.persistence.IPersistentExtraData;
import org.eclipse.sequoyah.localization.tools.persistence.PersistableAttributes;

/**
 * This class stores a comment associated to a StringNode.
 * 
 * It's intended to be persisted as part of the extra-info related to a
 * StringNode object
 */
public class StringNodeComment implements IPersistentExtraData {

	/*
	 * The StringNode this commented is related to
	 */
	private StringNode stringNode;

	/*
	 * The comment associated to the StringNode
	 */
	private String comment;

	/*
	 * When the comment has been added / edited
	 */
	private Date date;

	/**
	 * Get the StringNode this commented is related to
	 * 
	 * @return the StringNode this commented is related to
	 */
	public StringNode getStringNode() {
		return stringNode;
	}

	/**
	 * Set the StringNode this commented is related to
	 * 
	 * @param stringNode
	 *            the StringNode this commented is related to
	 */
	public void setStringNode(StringNode stringNode) {
		this.stringNode = stringNode;
	}

	/**
	 * Get the comment associated to the StringNode
	 * 
	 * @return the comment associated to the StringNode
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Set the comment associated to the StringNode
	 * 
	 * @param comment
	 *            the comment associated to the StringNode
	 */
	public void setComment(String comment) {
		if (this.comment != comment) {
			this.comment = comment;
			if (this.stringNode != null) {
				this.stringNode.setDirty(true);
			}
		}
	}

	/**
	 * Get when the comment has been added / edited
	 * 
	 * @return Date object stating when the comment has been added / edited
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * Set when the comment has been added / edited
	 * 
	 * @param date
	 *            Date object stating when the comment has been added / edited
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * @see org.eclipse.sequoyah.localization.tools.persistence.IPersistentData#getPersistableAttributes()
	 */
	public PersistableAttributes getPersistableAttributes() {
		// TODO: implement persistence
		return null;
	}

}
