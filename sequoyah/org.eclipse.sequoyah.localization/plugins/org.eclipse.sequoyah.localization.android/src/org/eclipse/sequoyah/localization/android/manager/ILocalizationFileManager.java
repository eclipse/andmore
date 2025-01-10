/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Marcel Augusto Gorri (Eldorado) - Bug [323036] - Add support to other localizable resources
 * 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.android.manager;

import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFile;

/**
 * This abstract class defined the operations to be executed with each specific
 * type of LocalizationFile in the AndroidLocalizationSchema.
 * 
 */
public abstract class ILocalizationFileManager {

	/**
	 * Load a localization file according to the rules for Android localization
	 * schema. The file loaded is based on the generic LocalizationFile object
	 * passed as a parameter.
	 * 
	 * @param locFile
	 *            an object which has information about the localization file
	 *            that shall be loaded, as well as its content
	 * @return LocalizationFile created
	 */
	public abstract LocalizationFile loadFile(LocalizationFile locFile)
			throws SequoyahException;

	/**
	 * Create a new localization file according to the rules for Android
	 * localization schema. The file generated is based on the generic
	 * LocalizationFile object passed as a parameter.
	 * 
	 * @param locFile
	 *            an object which has information about the localization file
	 *            that shall be created, as well as its content
	 */
	public abstract void createFile(LocalizationFile locFile)
			throws SequoyahException;

	/**
	 * Update a localization file according to the rules for Android
	 * localization schema. The file udpated is based on the generic
	 * LocalizationFile object passed as a parameter.
	 * 
	 * @param locFile
	 *            an object which has information about the localization file
	 *            that shall be updated, as well as its content
	 */
	public abstract void updateFile(LocalizationFile locFile)
			throws SequoyahException;

	/**
	 * 
	 * @param localizationFile
	 * @param content
	 * @throws SequoyahException
	 */
	public abstract void updateLocalizationFileContent(
			LocalizationFile localizationFile, String content)
			throws SequoyahException;

	/**
	 * 
	 * @param locFile
	 * @return
	 */
	public abstract Object getLocalizationFileContent(LocalizationFile locFile);

}