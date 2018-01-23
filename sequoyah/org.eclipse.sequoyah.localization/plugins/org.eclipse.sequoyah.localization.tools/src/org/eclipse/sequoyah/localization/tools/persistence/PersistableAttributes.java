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
package org.eclipse.sequoyah.localization.tools.persistence;

import java.util.List;

/**
 *
 */
public class PersistableAttributes {

	private boolean all;

	private List<String> attributeNames;

	/**
	 * 
	 */
	public void setAll() {

	}

	/**
	 * @return
	 */
	public List<String> getAttributeNames() {
		return attributeNames;
	}

	/**
	 * @param attributeNames
	 */
	public void setAttributeNames(List<String> attributeNames) {
		this.attributeNames = attributeNames;
	}

	/**
	 * @param attrName
	 */
	public void addAttributeName(String attrName) {

	}

}
