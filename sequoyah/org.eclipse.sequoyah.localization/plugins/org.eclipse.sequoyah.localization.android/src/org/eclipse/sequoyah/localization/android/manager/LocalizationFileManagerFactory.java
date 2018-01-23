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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.eclipse.sequoyah.device.common.utilities.BasePlugin;

/**
 * Factory for creating the different types of LocalizationFileManagers (named:
 * String, Image, Sound, Video). It is also a singleton.
 * 
 */
public class LocalizationFileManagerFactory {
	/**
	 * Private instance of this factory for singleton purposes.
	 */
	private volatile static LocalizationFileManagerFactory localizationFileManagerFactory;

	/**
	 * Private map for keeping LocalizationFileManager instances for creation.
	 */
	private HashMap<String, Class> hashMap = new HashMap<String, Class>();

	/**
	 * Store LocalizationFileManager classes for creation.
	 * 
	 * @param str
	 * @param cls
	 */
	public void addManager(String str, Class cls) {
		hashMap.put(str, cls);
	}

	/**
	 * Default constructor (private since it is a singleton).
	 */
	private LocalizationFileManagerFactory() {
	}

	/**
	 * This method provides a single instance of this factory for whoever needs
	 * to use it.
	 * 
	 * @return unique instance of this factory
	 */
	public static LocalizationFileManagerFactory getInstance() {
		if (localizationFileManagerFactory == null) {
			synchronized (LocalizationFileManagerFactory.class) {
				if (localizationFileManagerFactory == null) {
					localizationFileManagerFactory = new LocalizationFileManagerFactory();
				}
			}
		}
		return localizationFileManagerFactory;
	}

	/**
	 * Method responsible for creating the different types of LocalizationFile
	 * based on the type attribute of the LocalizationFileBean received as
	 * parameter.
	 * 
	 * @param className
	 *            The name of the LocalizationFileManager class to be created.
	 * @return ILocalizationFileManager created if the parameter received is not
	 *         null.
	 */
	public ILocalizationFileManager createLocalizationFileManager(
			String className) {
		ILocalizationFileManager locFileManager = null;

		try {
			// Loading a class as the first step in order to be able to
			// retrieve
			// it from the hash map based on its name
			Class.forName(className);
			// Creates a class of the desired type
			String forName = hashMap.get(className).getName();
			Class c = Class.forName(forName);
			// Instantiates a new object to invoke its methods
			Object o = c.newInstance();
			// Creates the desired method
			Method mthd = c.getMethod("create", null); //$NON-NLS-1$
			// Invokes the method of the desired type
			locFileManager = (ILocalizationFileManager) mthd.invoke(o);
		} catch (ClassNotFoundException e) {
			BasePlugin.logError(
					"Could not find class for LocalizationFileManager", e); //$NON-NLS-1$
		} catch (InstantiationException e) {
			BasePlugin
					.logError(
							"Could not instantiate class for LocalizationFileManager", e); //$NON-NLS-1$
		} catch (IllegalAccessException e) {
			BasePlugin
					.logError(
							"Could not access class or method for LocalizationFileManager", e); //$NON-NLS-1$
		} catch (SecurityException e) {
			BasePlugin.logError("Access to method denied", e); //$NON-NLS-1$
		} catch (NoSuchMethodException e) {
			BasePlugin.logError(
					"Could not find method for LocalizationFileManager", e); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			BasePlugin
					.logError(
							"Invalid arguments for method for LocalizationFileManager", e); //$NON-NLS-1$
		} catch (InvocationTargetException e) {
			BasePlugin.logError(
					"Could not call method for LocalizationFileManager", e); //$NON-NLS-1$
		}

		return locFileManager;

	}

	// public static void main(String[] args) {
	// ILocalizationFileManager locFileManager1 = LocalizationFileManagerFactory
	// .getInstance().createLocalizationFileManager(
	// StringLocalizationFileManager.class.getName());
	// System.out.println("locFileManager1: "
	// + locFileManager1.getClass().getName());
	//
	// ILocalizationFileManager locFileManager2 = LocalizationFileManagerFactory
	// .getInstance().createLocalizationFileManager(
	// SoundLocalizationFileManager.class.getName());
	// System.out.println("locFileManager2: "
	// + locFileManager2.getClass().getName());
	// }
}