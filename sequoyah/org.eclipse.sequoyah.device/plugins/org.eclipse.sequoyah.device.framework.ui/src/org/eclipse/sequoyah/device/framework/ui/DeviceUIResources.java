/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc and others.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Otavio Luiz Ferranti (Eldorado Research Institute) - bug#221733 - Adding data
 *                                                                    persistence
 * Yu-Fen Kuo (MontaVista) - bug#236476 - provide a generic device type
 * Fabio Fantato (Instituto Eldorado) - [263188] - Create new examples to support tutorial presentation
 * Fabio Fantato (Instituto Eldorado) - [243494] Change the reference implementation to work on Galileo
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [271682] - Default Wizard Page accepting invalid names
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.ui;

import org.eclipse.osgi.util.NLS;

/**
 * Resources for externalized Strings of the Sequoyah Emulator Core.
 */
public class DeviceUIResources extends NLS {
	

	

	private static String BUNDLE_NAME = "org.eclipse.sequoyah.device.framework.ui.DeviceUIResources";//$NON-NLS-1$

	public static String SEQUOYAH_Device_Plugin_Name;
	public static String SEQUOYAH_Error;
	public static String SEQUOYAH_Resource_Not_Available;
	public static String SEQUOYAH_Handler_Not_Instanced;
	public static String SEQUOYAH_Instance_Name_Duplicated_Error;
	public static String SEQUOYAH_Instance_Name_Invalid_Error;
	public static String SEQUOYAH_Emulator_Wizard_Project_Description_Duplicated_Error;
	public static String SEQUOYAH_Default_Device_Type_Wizard_Page_title;
	public static String SEQUOYAH_Default_Device_Type_Wizard_Page_message;
	public static String SEQUOYAH_Default_Device_Type_Wizard_Page_name;
	public static String SEQUOYAH_Default_Device_Type_Wizard_Page_deviceTypes;
	public static String DefaultDeviceTypeWizardPage_0;

	public static String DefaultDeviceTypeWizardPage_1;

	public static String DefaultDeviceTypeWizardPage_title;
	public static String DefaultDeviceTypeWizardPage_message;
	public static String DefaultDeviceTypeWizardPage_name;
	public static String DefaultDeviceTypeWizardPage_deviceTypes;
	public static String ConnectionInfoWizardPage_title;
	public static String ConnectionInfoWizardPage_message;
	public static String ConnectionInfoWizardPage_Host;
	public static String ConnectionInfoWizardPage_Port;
	public static String ConnectionInfoWizardPage_Display;
	
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, DeviceUIResources.class);
	}

}
