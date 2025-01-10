/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Fantato (Motorola)
 *
 * Contributors:
 * Fabio Fantato (Motorola) - bug#221733 - code revisited
 * Fabio Fantato (Instituto Eldorado) - [263188] - Create new examples to support tutorial presentation
 * Fabio Fantato (Instituto Eldorado) - [243494] Change the reference implementation to work on Galileo
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.ui.wizard;

/**
 * Define the common constants used by emulator framework.
 * <p>
 * The purpose of this constants is keep together all definitions for extension
 * points, views, shortcuts, propertys, attributes e several others.
 * 
 * @author Fabio Fantato
 */
public interface DeviceWizardConstants {

	
	/**
	 * Extension ID for New Wizard of Emulator Instances
	 */
	//public static final String EXTENSION_INSTANCE_ID = "org.eclipse.sequoyah.device.framework.ui.wizard"; //$NON-NLS-1$
	public static final String EXTENSION_INSTANCE_ID = "org.eclipse.sequoyah.device.framework.ui.newDeviceWizardPages";//$NON-NLS-1$

	public static final String PROPERTIES_FILENAME = "instance.properties"; //$NON-NLS-1$
	public static final String PROPERTIES_FILENAME_FULL = "/org/eclipse/sequoyah/device/wizard/resources/instance.properties"; //$NON-NLS-1$

	
	/**
	 * Shortcut to New Folder Wizard
	 */
	public static final String SHORTCUT_FOLDER = "org.eclipse.ui.wizards.new.folder"; //$NON-NLS-1$

	/**
	 * Shortcut to New File Wizard
	 */
	public static final String SHORTCUT_FILE = "org.eclipse.ui.wizards.new.file"; //$NON-NLS-1$

	/**
	 * ID for project page wizard
	 */
	public static final String PAGE_PROJECT = "projectPage"; //$NON-NLS-1$

	/**
	 * ID for property page wizard
	 */
	public static final String PAGE_PROPERTY = "propertyPage"; //$NON-NLS-1$

	/**
	 * ID for other pages wizard
	 */
	public static final String PAGE_OTHER = "otherPage"; //$NON-NLS-1$

	/**
	 * ID for read SETTINGS elements from plugin extensions
	 */
	public static final String SETTINGS = "settings"; //$NON-NLS-1$

	/**
	 * ID for read LAUNCHER elements from plugin extensions
	 */
	public static final String ELEMENT_LAUNCHER = "launcher"; //$NON-NLS-1$

	/**
	 * ID for read PROJECT elements from plugin extensions
	 */
	public static final String ELEMENT_CUSTOMIZER = "customizer"; //$NON-NLS-1$

	
	/**
	 * ID for read PROJECT elements from plugin extensions
	 */
	public static final String ELEMENT_PROJECT = "project"; //$NON-NLS-1$

	/**
	 * ID for read PROPERTY elements from plugin extensions
	 */
	public static final String ELEMENT_PROPERTY = "property"; //$NON-NLS-1$

	/**
	 * ID for read OTHER elements from plugin extensions
	 */
	public static final String ELEMENT_OTHER = "other"; //$NON-NLS-1$

	/**
	 * ID for read xml attribute from plugin extensions
	 */
	public static final String ATB_XML = "xml"; //$NON-NLS-1$
	
	/**
	 * ID for read monitor attribute from plugin extensions
	 */
	public static final String ATB_MONITOR = "needsProgressMonitor"; //$NON-NLS-1$
	
	/**
	 * ID for read forcePreviousAndNextButtons attribute from plugin extensions
	 */
	public static final String ATB_FORCE = "forcePreviousAndNextButtons"; //$NON-NLS-1$
	
	/**
	 * ID for read canFinishEarly attribute from plugin extensions
	 */
	public static final String ATB_FINISH = "canFinishEarly"; //$NON-NLS-1$
	
	/**
	 * ID for read image attribute from plugin extensions
	 */
	public static final String ATB_IMAGE = "image"; //$NON-NLS-1$
	
	/**
	 * ID for read title attribute from plugin extensions
	 */
	public static final String ATB_TITLE = "title"; //$NON-NLS-1$

	/**
	 * ID for read class attribute from plugin extensions
	 */
	public static final String ATB_CLASS = "class"; //$NON-NLS-1$

	/**
	 * ID for read name attribute from plugin extensions
	 */

	public static final String ATB_NAME = "name"; //$NON-NLS-1$

	/**
	 * ID for read description attribute from plugin extensions
	 */
	public static final String ATB_DESCRIPTION = "description"; //$NON-NLS-1$

    /**
     * ID for read USAGE elements from plugin extensions
     */
    public static final String ELEMENT_USAGE = "usage"; //$NON-NLS-1$

    /**
     * ID for read device attribute from plugin extensions
     */
    public static final String ATB_DEVICE = "device"; //$NON-NLS-1$

    /**
     * A null string
     */
	public static final String STRING_NULL = ""; //$NON-NLS-1$
	/**
	 * 
	 */
	public static final String FOLDER_SEPARATOR = "/"; //$NON-NLS-1$
	/**
	 * 
	 */
	public static final String SLASH = "\\"; //$NON-NLS-1$

}
