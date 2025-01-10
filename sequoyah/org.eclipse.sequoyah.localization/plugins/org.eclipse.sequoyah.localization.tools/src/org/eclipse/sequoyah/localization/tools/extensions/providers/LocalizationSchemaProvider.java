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
package org.eclipse.sequoyah.localization.tools.extensions.providers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.sequoyah.localization.tools.extensions.classes.ILocalizationSchema;

/**
 * This class is responsible for gathering the "localizationSchema" extension
 * point information, which is used to register ILocalizationSchema
 * implementations. This way, it's possible to declare different localization
 * schemas for different projects (the type os the project is defined by its
 * natures)
 * 
 * It also instantiates the declared ILocalizationSchema classes and make them
 * available for callers. For this specific class, the LocalizationManager is
 * the user/caller
 */
public class LocalizationSchemaProvider {

	/*
	 * Store all loaded localization schemas
	 */
	private Map<String, ILocalizationSchema> localizationSchemas = null;

	/*
	 * The "localizationSchema" extension point ID
	 */
	private final String EXTENSION_ID = "org.eclipse.sequoyah.localization.tools.localizationSchema"; //$NON-NLS-1$

	/*
	 * Fields in "localizationSchema" extension point definition
	 */
	private final String EXTENSION_FIELD_NAME = "name"; //$NON-NLS-1$

	private final String EXTENSION_FIELD_NATURE = "nature"; //$NON-NLS-1$

	private final String EXTENSION_FIELD_NATURE_PRECEDENCE = "naturePrecedence"; //$NON-NLS-1$

	private final String EXTENSION_FIELD_CLASS = "class"; //$NON-NLS-1$

	/*
	 * Singleton instance
	 */
	private static LocalizationSchemaProvider instance = null;

	/**
	 * Singleton
	 * 
	 * @return LocalizationSchemaProvider
	 */
	public static LocalizationSchemaProvider getInstance() {
		if (instance == null) {
			instance = new LocalizationSchemaProvider();
		}
		return instance;
	}

	/**
	 * Returns the registered implementations of ILocalizationSchema. Each
	 * implementation is related to a IProjectNature, which is defined in the
	 * extension point declaration
	 * 
	 * @return a Map<String, ILocalizationSchema> containing all
	 *         ILocalizationSchema implementations and their related
	 *         IProjectNature
	 */
	public Map<String, ILocalizationSchema> getLocalizationSchemas() {

		if (localizationSchemas == null) {

			localizationSchemas = new HashMap<String, ILocalizationSchema>();

			/*
			 * Get extension points defined
			 */
			IConfigurationElement[] config = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(EXTENSION_ID);

			String name, natureName, naturePrecedence;
			List<String> naturePrecedenceList;

			/*
			 * Iterate through extension points
			 */
			for (IConfigurationElement configElem : config) {

				try {

					// get information from extension point
					name = configElem.getAttribute(EXTENSION_FIELD_NAME);
					natureName = configElem
							.getAttribute(EXTENSION_FIELD_NATURE);
					naturePrecedence = configElem
							.getAttribute(EXTENSION_FIELD_NATURE_PRECEDENCE);
					naturePrecedenceList = new ArrayList<String>();
					naturePrecedenceList.add(naturePrecedence);

					// instantiate the ILocalizationSchema object and set their
					// attributes
					// according to the extension point information
					ILocalizationSchema localizationSchema = (ILocalizationSchema) configElem
							.createExecutableExtension(EXTENSION_FIELD_CLASS);
					localizationSchema.setName(name);
					localizationSchema.setNatureName(natureName);
					localizationSchema
							.setNaturePrecedence(naturePrecedenceList);

					// Add the localization schema to the list
					localizationSchemas.put(natureName, localizationSchema);

				} catch (Exception e) {
					// TODO Auto-generated catch block
				}
			}
		}

		return localizationSchemas;
	}

	/**
	 * Returns a specific implementation of ILocalizationSchema, given the
	 * IProjectNature
	 * 
	 * @param nature
	 *            the IProjectNature for which a ILocalizationSchema
	 *            implementation is required
	 */
	public ILocalizationSchema getLocalizationSchemaByNature(String nature) {

		ILocalizationSchema localizationSchema = null;
		Map<String, ILocalizationSchema> allSchemas = null;

		/*
		 * If the localization schemas have already been loaded, use them
		 * Otherwise, call the method which loads them before using
		 */
		if (localizationSchemas != null) {
			allSchemas = localizationSchemas;
		} else {
			allSchemas = getLocalizationSchemas();
		}

		/*
		 * Check if there are schemas defined and search for the one which
		 * applies to the nature passed as parameter
		 */
		if (allSchemas != null) {
			localizationSchema = allSchemas.get(nature);
		}

		return localizationSchema;
	}
}
