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

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringArrayNode;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringNode;

/**
 * This bean contains all information necessary to create a LocalizationFile,
 * including the type of such file.
 * 
 */
public class LocalizationFileBean {

	/*
	 * Specifies if the bean relates to a certain type of LocalizationFile
	 */
	private String type;

	/*
	 * A reference to the file being represented
	 */
	private IFile file;

	/*
	 * The information about the locale represented by the localization file
	 */
	private LocaleInfo locale;

	/*
	 * The list of StringArrays which are part of the file
	 */
	private List<StringArrayNode> stringArrays;

	/*
	 * The list of StringNodes which are part of the file
	 */
	private List<StringNode> stringNodes;

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the file
	 */
	public IFile getFile() {
		return file;
	}

	/**
	 * @param file
	 *            the file to set
	 */
	public void setFile(IFile file) {
		this.file = file;
	}

	/**
	 * @return the locale
	 */
	public LocaleInfo getLocale() {
		return locale;
	}

	/**
	 * @param locale
	 *            the locale to set
	 */
	public void setLocale(LocaleInfo locale) {
		this.locale = locale;
	}

	/**
	 * @return the stringArrays
	 */
	public List<StringArrayNode> getStringArrays() {
		return stringArrays;
	}

	/**
	 * @param stringArrays
	 *            the stringArrays to set
	 */
	public void setStringArrays(List<StringArrayNode> stringArrays) {
		this.stringArrays = stringArrays;
	}

	/**
	 * @return the stringNodes
	 */
	public List<StringNode> getStringNodes() {
		return stringNodes;
	}

	/**
	 * @param stringNodes
	 *            the stringNodes to set
	 */
	public void setStringNodes(List<StringNode> stringNodes) {
		this.stringNodes = stringNodes;
	}

	/**
	 * Default constructor
	 * 
	 */
	public LocalizationFileBean() {
	}

	/**
	 * Constructor receiving parameters
	 * 
	 * @param stringArrays
	 * @param stringNodes
	 * @param file
	 * @param locale
	 */
	public LocalizationFileBean(String type, IFile file, LocaleInfo locale,
			List<StringNode> stringNodes, List<StringArrayNode> stringArrays) {
		this.type = type;
		this.file = file;
		this.locale = locale;
		this.stringNodes = stringNodes;
		this.stringArrays = stringArrays;
	}

	/**
	 * Constructor based on a LocalizationFile.
	 * 
	 * @param locFile
	 */
	public LocalizationFileBean(LocalizationFile locFile) {
		this.file = locFile.getFile();
		this.locale = locFile.getLocaleInfo();
	}

}
