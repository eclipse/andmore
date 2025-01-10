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

import java.util.List;

/**
 * This class represents a specific locale and has all details about it
 */
public class LocaleInfo {

	/*
	 * The attributes which represent the language
	 */
	private List<LocaleAttribute> localeAttributes;

	/**
	 * Get the attributes which represent the locale
	 * 
	 * @return the attributes which represent the locale
	 */
	public List<LocaleAttribute> getLocaleAttributes() {
		return localeAttributes;
	}

	/**
	 * Set the attributes which represent the locale
	 * 
	 * @param languageAttributes
	 *            the attributes which represent the locale
	 */
	public void setLocaleAttributes(List<LocaleAttribute> localeAttributes) {
		this.localeAttributes = localeAttributes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object arg0) {
		boolean equals = false;

		if (arg0 instanceof LocaleInfo) {
			int i = 0;
			int localSize = this.localeAttributes.size();
			int paramSize = ((LocaleInfo) arg0).getLocaleAttributes().size();

			equals = (localSize == paramSize);

			while ((i < localSize) && equals) {
				LocaleAttribute localAttribute = this.localeAttributes.get(i);
				LocaleAttribute comparedAttribute = ((LocaleInfo) arg0).localeAttributes
						.get(i);

				if (localAttribute == null) {
					equals = equals && (comparedAttribute == null);
				} else {
					equals = equals
							&& (localAttribute.equals(comparedAttribute));
				}
				i++;
			}
		}

		return equals;
	}

}
