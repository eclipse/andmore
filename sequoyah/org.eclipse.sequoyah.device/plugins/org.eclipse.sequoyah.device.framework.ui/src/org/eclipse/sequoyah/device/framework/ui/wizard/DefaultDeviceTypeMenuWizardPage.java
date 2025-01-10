/*******************************************************************************
 * Copyright (c) 2008-2010 MontaVista Software, Inc and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Yu-Fen Kuo (MontaVista) - initial API and implementation
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Fabio Fantato (Instituto Eldorado) - [263188] - Create new examples to support tutorial presentation
 * Fabio Fantato (Instituto Eldorado) - [243494] Change the reference implementation to work on Galileo
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [271682] - Default Wizard Page accepting invalid names
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [280982] - Add sensitive context help support to InstanceView and New Instance Wizard.
 * Fabio Rigo (Eldorado) - Bug [288006] - Unify features of InstanceManager and InstanceRegistry
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/
package org.eclipse.sequoyah.device.framework.ui.wizard;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.sequoyah.device.framework.factory.DeviceTypeRegistry;
import org.eclipse.sequoyah.device.framework.factory.InstanceRegistry;
import org.eclipse.sequoyah.device.framework.internal.model.MobileDeviceType;
import org.eclipse.sequoyah.device.framework.model.AbstractMobileInstance;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.ui.DeviceUIResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * default first page of the new device instance wizard. Allows user to specify
 * the device instance name and the device type
 * 
 */
public class DefaultDeviceTypeMenuWizardPage extends WizardPage {

	private Text nameText;
	private MobileDeviceType currentDeviceType;

	private static String contextId = null;
	
	
	protected DefaultDeviceTypeMenuWizardPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}
	
	protected DefaultDeviceTypeMenuWizardPage(String pageName,String currentDeviceTypeId) {
		super(pageName);
		setTitle(DeviceUIResources.SEQUOYAH_Default_Device_Type_Wizard_Page_title);
	
		setMessage(DeviceUIResources.SEQUOYAH_Default_Device_Type_Wizard_Page_message);
	
		for (Object device:DeviceTypeRegistry.getInstance().getDeviceTypes().toArray()) {
			if (((IDeviceType)device).getId().equals(currentDeviceTypeId)) {
				currentDeviceType = ((MobileDeviceType)device);
			}
		};
	}
	
	
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		container.setFont(parent.getFont());
	
		// new name label
		Label label = new Label(container, SWT.NONE);
		label.setText(DeviceUIResources.SEQUOYAH_Default_Device_Type_Wizard_Page_name);
		label.setFont(container.getFont());
	
		nameText = new Text(container, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		nameText.setLayoutData(gd);
		nameText.setFont(container.getFont());
	
		nameText.addModifyListener(new ModifyListener() {
	
			public void modifyText(ModifyEvent e) {
				InstanceRegistry registry = InstanceRegistry.getInstance();
				String name = nameText.getText();
				String errorMessage = null;
	
				if (name != null) {
					//name = name.trim();
					if (!name.equals("")) { //$NON-NLS-1$
						if (! (registry.getInstancesByName(name).size() == 0)) {
							errorMessage = DeviceUIResources.SEQUOYAH_Emulator_Wizard_Project_Description_Duplicated_Error;
						}
						else if (!AbstractMobileInstance.validName(name)){
							errorMessage = DeviceUIResources.SEQUOYAH_Instance_Name_Invalid_Error;
						}
					}
				}
	
				setErrorMessage(errorMessage);
				getWizard().getContainer().updateButtons();
	
			}
	
		});
	
		createDeviceTypesArea(container);
	
		if (contextId != null) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(container, contextId);
		}
		setControl(container);
	}
	
	public boolean isPageComplete() {
		if (getErrorMessage() != null)
			return false;
		if (!nameText.getText().trim().equals("") && currentDeviceType != null) { //$NON-NLS-1$
			return true;
		}
	
		return false;
	}
	
	private void createDeviceTypesArea(Composite parent) {
	
		Label label = new Label(parent, SWT.NONE);
		label.setText(DeviceUIResources.SEQUOYAH_Default_Device_Type_Wizard_Page_deviceTypes 
				+ " " + currentDeviceType.getLabel()); //$NON-NLS-1$
		label.setFont(parent.getFont());
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		label.setLayoutData(gd);
		IWizard wizard = getWizard();
			if (wizard instanceof NewDeviceMenuWizard) {
	
				((NewDeviceMenuWizard) wizard).setCurrentDeviceTypeId(currentDeviceType.getId());
			}
	}
	
	public String getInstanceName() {
		return nameText.getText().trim();
	}
	
	public MobileDeviceType getDeviceType() {
		return currentDeviceType;
	}

	
	public static String getHelpContextId() {
		return contextId;
	}
	
	public static void setHelpContextId(String contextId) {
		DefaultDeviceTypeMenuWizardPage.contextId = contextId;
	}
	
	
}
