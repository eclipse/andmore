/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Fabio Fantato (Motorola) - bug#221736 - new instance wizard
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.wizard.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.sequoyah.device.common.utilities.PluginUtils;
import org.eclipse.sequoyah.device.framework.wizard.DeviceWizardConstants;

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

    /*
     * Map for holding the ids for each deviceWizard extension, having the
     * device id as the key.
     */
    private final Map<String, String> deviceWizardIds = new LinkedHashMap<String, String>();

    /**
     * Private constructor to avoid instantiation outside of the class (singleton).
     */
    private DeviceWizardExtensionManager()
    {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint extensionPoint =
                registry.getExtensionPoint(DeviceWizardConstants.EXTENSION_INSTANCE_ID);
        IExtension[] extensions = extensionPoint.getExtensions();

        for (IExtension extension : extensions)
        {
            String device =
                    PluginUtils.getPluginAttribute(extension, DeviceWizardConstants.ELEMENT_USAGE,
                            DeviceWizardConstants.ATB_DEVICE);
            deviceWizardIds.put(device, extension.getUniqueIdentifier());
        }
    }

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
        String extensionId = deviceWizardIds.get(deviceId);

        if (extensionId != null)
        {
            try
            {
                Object executable =
                        PluginUtils.getExecutableAttribute(PluginUtils.getExtension(extensionId),
                                DeviceWizardConstants.ELEMENT_USAGE,
                                DeviceWizardConstants.ATB_CLASS);

                if (executable instanceof IWizard)
                {
                    wizard = (IWizard) executable;
                }
            }
            catch (CoreException e)
            {
                // nothing to do, null will be returned
            }
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
