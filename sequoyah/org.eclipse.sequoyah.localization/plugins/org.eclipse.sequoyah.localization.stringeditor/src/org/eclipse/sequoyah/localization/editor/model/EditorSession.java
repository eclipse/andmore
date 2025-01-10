/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Marcelo Marzola Bossoni (Eldorado)
 * 
 * Contributors:
 * name (company) - description.
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;

public class EditorSession {

	private final Map<QualifiedName, String> session;

	private final IProject project;

	private static final String PROPERTY_GROUP = "org.eclipse.sequoyah.localization.editor"; //$NON-NLS-1$

	public static enum PROPERTY {
		WIDTH, VISIBLE, ORDER, SORT_BY_COLUMN, HIGHLIGHT_CHANGES, SEARCH_TEXT, SHOW_COMMENTS, FILTER_BY_KEY, EXPAND_ROW
	}

	private EditorSession(Map<QualifiedName, String> session, IProject project) {
		this.session = session;
		this.project = project;
	};

	@SuppressWarnings("unchecked")
	public static EditorSession loadFromProject(IProject project) {
		EditorSession session = null;
		try {
			Map<QualifiedName, String> persistentProperties = project
					.getPersistentProperties();
			Map<QualifiedName, String> editorProperties = new HashMap<QualifiedName, String>();
			for (QualifiedName name : persistentProperties.keySet()) {
				if (name.getQualifier() != null
						&& name.getQualifier().contains(PROPERTY_GROUP)) {
					editorProperties.put(name, persistentProperties.get(name));
				}
			}

			session = new EditorSession(editorProperties, project);
		} catch (CoreException e) {
			BasePlugin.logError(
					"Error loading editor preferences from project: " //$NON-NLS-1$
							+ project.getName(), e);

		}

		return session;
	}

	/**
	 * get the following editor session property
	 * 
	 * @param namespace
	 *            the namespace of the property
	 * @param p
	 *            the property
	 * @return the string representation of the property
	 */
	public String getProperty(String namespace, PROPERTY p) {
		String qualifier = PROPERTY_GROUP + "." + namespace; //$NON-NLS-1$
		QualifiedName property = new QualifiedName(qualifier, p.name());
		return session.get(property);
	}

	/**
	 * Set a property value, or null to remove
	 * 
	 * @param name
	 *            the property name
	 * @param p
	 *            the property type
	 * @param value
	 *            the value of the property
	 */
	public void setProperty(String name, PROPERTY p, String value) {
		String qualifier = PROPERTY_GROUP + "." + name; //$NON-NLS-1$
		QualifiedName property = new QualifiedName(qualifier, p.name());
		session.put(property, value);
	}

	public void save() {
		for (QualifiedName key : session.keySet()) {
			try {
				project.setPersistentProperty(key, session.get(key));
			} catch (CoreException e) {
				BasePlugin.logError("Error saving preferences to project: " //$NON-NLS-1$
						+ project.getName(), e);
			}
		}
	}

	/**
	 * Clean all editor properties from the project
	 */
	public void clean() {
		for (QualifiedName key : session.keySet()) {
			try {
				project.setPersistentProperty(key, null);
			} catch (CoreException e) {
				BasePlugin.logError("Error cleaning preferences of project: " //$NON-NLS-1$
						+ project.getName(), e);
			}
		}
		session.clear();
	}
}
