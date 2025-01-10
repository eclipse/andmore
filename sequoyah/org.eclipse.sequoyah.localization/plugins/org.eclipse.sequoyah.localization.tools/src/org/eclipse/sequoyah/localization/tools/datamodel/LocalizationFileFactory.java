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
package org.eclipse.sequoyah.localization.tools.datamodel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.eclipse.sequoyah.device.common.utilities.BasePlugin;

/**
 * Factory for creating the different types of LocalizationFile (named: String,
 * Image, Sound, Video). It is also a singleton.
 * 
 */
public class LocalizationFileFactory {
	/**
	 * Private instance of this factory for singleton purposes.
	 */
	private volatile static LocalizationFileFactory localizationFileFactory;

	/**
	 * Private map for keeping LocalizationFile instances for creation.
	 */
	private HashMap<String, Class> hashMap = new HashMap<String, Class>();

	/**
	 * Store LocalizationFile classes for creation.
	 * 
	 * @param str
	 * @param cls
	 */
	public void addFileType(String str, Class cls) {
		hashMap.put(str, cls);
	}

	/**
	 * Default constructor (private since it is a singleton).
	 */
	private LocalizationFileFactory() {
	}

	/**
	 * This method provides a single instance of this factory for whoever needs
	 * to use it.
	 * 
	 * @return unique instance of this factory
	 */
	public static LocalizationFileFactory getInstance() {
		if (localizationFileFactory == null) {
			synchronized (LocalizationFileFactory.class) {
				if (localizationFileFactory == null) {
					localizationFileFactory = new LocalizationFileFactory();
				}
			}
		}
		return localizationFileFactory;
	}

	/**
	 * Method responsible for creating the different types of LocalizationFile
	 * based on the type attribute of the LocalizationFileBean received as
	 * parameter.
	 * 
	 * @param bean
	 *            Bean containing all information necessary for the creation of
	 *            a LocalizationFile.
	 * @return LocalizationFile created if the parameter received is not null.
	 */
	public LocalizationFile createLocalizationFile(LocalizationFileBean bean) {
		LocalizationFile locFile = null;
		try {
			// Loading a class as the first step in order to be able to retrieve
			// it from the hash map based on its name
			Class.forName(bean.getType());
			// Creates a class of the desired type
			Class c = Class.forName(hashMap.get(bean.getType()).toString()
					.substring(6));
			// Instantiates a new object to invoke its methods
			Object o = c.newInstance();
			// Creates the desired method
			Method mthd = c.getMethod("create", LocalizationFileBean.class); //$NON-NLS-1$
			// Invokes the method of the desired type
			locFile = (LocalizationFile) mthd.invoke(o, bean);
		} catch (ClassNotFoundException e) {
			BasePlugin.logError("Could not find class for LocalizationFile", e); //$NON-NLS-1$
		} catch (InstantiationException e) {
			BasePlugin.logError(
					"Could not instantiate class for LocalizationFile", e); //$NON-NLS-1$
		} catch (IllegalAccessException e) {
			BasePlugin.logError(
					"Could not access class or method for LocalizationFile", e); //$NON-NLS-1$
		} catch (SecurityException e) {
			BasePlugin.logError("Access to method denied", e); //$NON-NLS-1$
		} catch (NoSuchMethodException e) {
			BasePlugin
					.logError("Could not find method for LocalizationFile", e); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			BasePlugin.logError(
					"Invalid arguments for method for LocalizationFile", e); //$NON-NLS-1$
		} catch (InvocationTargetException e) {
			BasePlugin
					.logError("Could not call method for LocalizationFile", e); //$NON-NLS-1$
		}
		return locFile;
	}

	// public static void main(String[] args) {
	// IFile file = null;
	// LocaleInfo localeInfo = new LocaleInfo();
	// StringNode node = new StringNode("", "");
	// StringArray array = new StringArray("");
	//
	// List<StringNode> stringNodes = new ArrayList<StringNode>();
	// stringNodes.add(node);
	// List<StringArray> stringArrays = new ArrayList<StringArray>();
	// stringArrays.add(array);
	//
	// LocalizationFileBean bean = new LocalizationFileBean(
	// StringLocalizationFile.class.getName(), file, localeInfo,
	// stringNodes, stringArrays);
	// LocalizationFile locFile1 = LocalizationFileFactory.getInstance()
	// .createLocalizationFile(bean);
	// System.out.println("locFile1: " + locFile1.getClass().getName());
	// bean = new LocalizationFileBean(ImageLocalizationFile.class.getName(),
	// file, localeInfo, null, null);
	// LocalizationFile locFile2 = LocalizationFileFactory.getInstance()
	// .createLocalizationFile(bean);
	// System.out.println("locFile2: " + locFile2.getClass().getName());
	//
	// }
}