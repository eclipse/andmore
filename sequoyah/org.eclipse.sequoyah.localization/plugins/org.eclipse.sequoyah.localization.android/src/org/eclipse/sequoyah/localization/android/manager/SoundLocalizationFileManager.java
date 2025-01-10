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
 * This class deals specifically with localized sound content.
 * 
 */
public class SoundLocalizationFileManager extends ILocalizationFileManager {

	/*
	 * Static code to add this manager to factory's hashmap
	 */
	static {
		LocalizationFileManagerFactory.getInstance().addManager(
				SoundLocalizationFileManager.class.getName(),
				SoundLocalizationFileManager.class);
	}

	/**
	 * Default constructor.
	 */
	public SoundLocalizationFileManager() {
	}

	/**
	 * SoundLocalizationFileManager knows how to create itself.
	 * 
	 * @return StringLocalizationFileManager created
	 */
	public static ILocalizationFileManager create() {
		SoundLocalizationFileManager locFileManager;
		locFileManager = new SoundLocalizationFileManager();
		return locFileManager;
	}

	@Override
	public LocalizationFile loadFile(LocalizationFile locFile) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createFile(LocalizationFile locFile) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateFile(LocalizationFile locFile) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateLocalizationFileContent(
			LocalizationFile localizationFile, String content)
			throws SequoyahException {
		// TODO Auto-generated method stub

	}

	@Override
	public Object getLocalizationFileContent(LocalizationFile locFile) {
		// TODO Auto-generated method stub
		return null;
	}

}
