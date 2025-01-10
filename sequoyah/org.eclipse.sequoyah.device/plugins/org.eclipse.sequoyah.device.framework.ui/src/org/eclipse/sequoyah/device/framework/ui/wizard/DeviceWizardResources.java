/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Mobility, Inc. and others.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Otavio Luiz Ferranti (Eldorado Research Institute) - bug#221733 - Added a 
 * Fabio Fantato (Instituto Eldorado) - [263188] - Create new examples to support tutorial presentation
 * Fabio Fantato (Instituto Eldorado) - [243494] Change the reference implementation to work on Galileo
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Daniel Pastore (Eldorado) - [278436] New device wizard does not offer a way to set its window title
 * 
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.ui.wizard;

import org.eclipse.osgi.util.NLS;


/**
 * Resources for externalized Strings of the Sequoyah Emulator Core.
 */
public class DeviceWizardResources extends NLS {
	

	private static String BUNDLE_NAME = "org.eclipse.sequoyah.device.framework.ui.wizard.DeviceWizardResources";//$NON-NLS-1$

	public static String SEQUOYAH_Device_Plugin_Name;
	public static String SEQUOYAH_Error;
	public static String SEQUOYAH_Resource_Not_Available;
	public static String SEQUOYAH_Handler_Not_Instanced;

	public static String SEQUOYAH_Emulator_Wizard_Plugin_Name;
	public static String SEQUOYAH_Emulator_Wizard_Project_Title;
	public static String SEQUOYAH_Emulator_Wizard_Project_Description;
	public static String SEQUOYAH_Emulator_Wizard_Project_Description_Duplicated_Error;

	public static String SEQUOYAH_NewDeviceMenuWizard_Window_Title;
	
	public static String msg_new_wizard_emulator_title;
	public static String msg_new_wizard_emulator_description;
	public static String msg_nature_emulator_name;
	public static String msg_preference_page_title;
	public static String msg_property_page_title;
	public static String msg_emulator_start;
	public static String msg_emulator_stop;
	public static String msg_emulator_show_view;
	public static String msg_viewer_update_properties;
	public static String msg_property_field_title;
	public static String msg_emulator_view_title;
	public static String msg_emulator_control_title;
	public static String msg_emulator_button_stop;
	public static String msg_emulator_button_start;
	public static String msg_emulator_default_name;
	public static String msg_emulator_default_description;
	public static String msg_creating_project;
	public static String por_creating_project;
	public static String msg_creating_folders;
	public static String msg_creating_files;
	public static String msg_emulator_instance_name;
	public static String msg_emulator_instance_name_set;
	public static String msg_console_title;
	
	
	
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, DeviceWizardResources.class);
	}

}
