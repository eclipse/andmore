/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Matheus Tait Lima (Eldorado)
 * 
 * Contributors:
 * Paulo Faria (Eldorado) - Add methods for not to lose comments on save
 * 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.android.datamodel;

import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFile;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFileBean;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFileFactory;
import org.eclipse.sequoyah.localization.tools.datamodel.VideoLocalizationFile;

/**
 * This class represents a real Android localization file in the project and
 * contains information about XML file saved
 * 
 */
public class AndroidVideoLocalizationFile extends VideoLocalizationFile {

	/*
	 * Static code to add file type to factory's hashmap
	 */
	static {
		LocalizationFileFactory.getInstance().addFileType(
				AndroidVideoLocalizationFile.class.getName(),
				AndroidVideoLocalizationFile.class);
	}

	/**
	 * Default constructor.
	 */
	public AndroidVideoLocalizationFile() {
	}

	/**
	 * 
	 */
	public AndroidVideoLocalizationFile(LocalizationFileBean bean) {
		super(bean);
	}

	/**
	 * ImageLocalizationFile knows how to create itself.
	 * 
	 * @param bean
	 *            Bean containing all information necessary to create any type
	 *            of LocalizationFile.
	 * @return LocalizationFile created (if the parameter received is not null).
	 */
	public static LocalizationFile create(LocalizationFileBean bean) {
		LocalizationFile locFile;
		if (bean != null) {
			locFile = new AndroidVideoLocalizationFile(bean);
		} else {
			locFile = null;
		}
		return locFile;
	}
}
