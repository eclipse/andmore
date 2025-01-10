/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
********************************************************************************/
 
 package org.eclipse.sequoyah.device.framework.ui.wizard;

import org.eclipse.jface.wizard.IWizard;

/**
 * 
 * This class manages the reading of extensions of the deviceWizard extension point.
 * It is a singleton class and can only be retrieved through {@link #getInstance()}
 * method.
 *
 */
public class DeviceWizardExtensionManager
{
    private static DeviceWizardExtensionManager mgr = new DeviceWizardExtensionManager();

   
    
    /**
     * Retrieves an instance of the wizard class set for the device with the given id.
     * If the wizard class cannot be found or cannot be instantiated, <code>null</code>
     * is returned instead.
     * 
     * @param deviceId the id of the device
     * 
     * @return an instance of the wizard class for the given device, or <code>null</code>
     *          if the wizard class was not instantiated
     */
    public IWizard getDeviceWizard(String deviceId)
    {
        IWizard wizard = null;
                 Object executable = new NewDeviceMenuWizard();
                ((NewDeviceMenuWizard)executable).setCurrentDeviceTypeId(deviceId);
                
                if (executable instanceof IWizard)
                {
                    wizard = (IWizard) executable;
                }

        return wizard;
    }

    /**
     * Retrieves the instance of this singleton class.
     * 
     * @return the {@link DeviceWizardExtensionManager} object
     */
    public static DeviceWizardExtensionManager getInstance()
    {
        return mgr;
    }

    /**
     * Retrieves the {@link DeviceWizardBean} object for the given id.
     * 
     * @param id the deviceWizard extension id
     * 
     * @return the {@link DeviceWizardBean} object
     */
    public DeviceWizardBean getDeviceWizardBean(String id)
    {
        return new DeviceWizardBean(id);
    }

}
