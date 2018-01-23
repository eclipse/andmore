/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Mobility, Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Fantato (Motorola)
 *
 * Contributors:
 * Fabio Fantato (Motorola) - bug#221736 - new instance wizard
 * Otavio Ferranti (Eldorado Research Institute) - bug#221733 - removing the
 *                          project location field from the default project page
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah    
 * Daniel Pastore (Eldorado) - [278436] New device wizard does not offer a way to set its window title
 *                      
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.wizard.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.common.utilities.PluginUtils;
import org.eclipse.sequoyah.device.framework.wizard.DeviceWizardConstants;
import org.eclipse.sequoyah.device.framework.ui.wizard.DeviceWizardResources;
import org.eclipse.sequoyah.device.framework.wizard.ui.WizardNewProjectPage;
import org.eclipse.sequoyah.device.framework.wizard.ui.WizardNewPropertyPage;

/**
 * Provide all information about Wizard Extension Point
 * to load a new wizard for emulator instance project
 * @author Fabio Fantato
 *
 */
public class DeviceWizardBean
{
    public String extensionId;

    private boolean needsProgressMonitor;

    private boolean forcePreviousAndNextButtons;

    private boolean canFinishEarly;

    private String image;

    private String title;

    private String customizerName;

    private IWizardCustomizer customizer;

    private String projectDescription;

    private String projectTitle;

    private String propertyDescription;

    private String propertyTitle;

    private String propertyXML;

    private String otherDescription;

    private String otherTitle;

    /**
     * Based on plugin id of emulator the wizard properties will be loaded.
     * @param id
     */
    protected DeviceWizardBean(String id)
    {
        extensionId = id;
        needsProgressMonitor =
                Boolean.parseBoolean(PluginUtils.getPluginAttribute(
                        DeviceWizardConstants.EXTENSION_INSTANCE_ID, extensionId,
                        DeviceWizardConstants.SETTINGS, DeviceWizardConstants.ATB_MONITOR));
        forcePreviousAndNextButtons =
                Boolean.parseBoolean(PluginUtils.getPluginAttribute(
                        DeviceWizardConstants.EXTENSION_INSTANCE_ID, extensionId,
                        DeviceWizardConstants.SETTINGS, DeviceWizardConstants.ATB_FORCE));
        canFinishEarly =
                Boolean.parseBoolean(PluginUtils.getPluginAttribute(
                        DeviceWizardConstants.EXTENSION_INSTANCE_ID, extensionId,
                        DeviceWizardConstants.SETTINGS, DeviceWizardConstants.ATB_FINISH));
        image =
                PluginUtils.getPluginAttribute(DeviceWizardConstants.EXTENSION_INSTANCE_ID,
                        extensionId, DeviceWizardConstants.SETTINGS,
                        DeviceWizardConstants.ATB_IMAGE);
        title =
                PluginUtils.getPluginAttribute(DeviceWizardConstants.EXTENSION_INSTANCE_ID,
                        extensionId, DeviceWizardConstants.SETTINGS,
                        DeviceWizardConstants.ATB_TITLE);
        customizer = null;
        customizerName =
                PluginUtils.getPluginAttribute(DeviceWizardConstants.EXTENSION_INSTANCE_ID,
                        extensionId, DeviceWizardConstants.ELEMENT_CUSTOMIZER,
                        DeviceWizardConstants.ATB_CLASS);

        if (customizerName != null)
        {
            try
            {
                customizer =
                        (IWizardCustomizer) PluginUtils.getExecutable(extensionId,
                                DeviceWizardConstants.ELEMENT_CUSTOMIZER);
            }
            catch (CoreException e)
            {
                BasePlugin.logError(e.getMessage(), e);
            }

        }

        projectDescription =
                PluginUtils.getPluginAttribute(DeviceWizardConstants.EXTENSION_INSTANCE_ID,
                        extensionId, DeviceWizardConstants.ELEMENT_PROJECT,
                        DeviceWizardConstants.ATB_TITLE);
        projectTitle =
                PluginUtils.getPluginAttribute(DeviceWizardConstants.EXTENSION_INSTANCE_ID,
                        extensionId, DeviceWizardConstants.ELEMENT_PROJECT,
                        DeviceWizardConstants.ATB_DESCRIPTION);

        propertyDescription =
                PluginUtils.getPluginAttribute(DeviceWizardConstants.EXTENSION_INSTANCE_ID,
                        extensionId, DeviceWizardConstants.ELEMENT_PROPERTY,
                        DeviceWizardConstants.ATB_TITLE);
        propertyTitle =
                PluginUtils.getPluginAttribute(DeviceWizardConstants.EXTENSION_INSTANCE_ID,
                        extensionId, DeviceWizardConstants.ELEMENT_PROPERTY,
                        DeviceWizardConstants.ATB_DESCRIPTION);
        propertyXML =
                PluginUtils.getPluginAttribute(DeviceWizardConstants.EXTENSION_INSTANCE_ID,
                        extensionId, DeviceWizardConstants.ELEMENT_PROPERTY,
                        DeviceWizardConstants.ATB_XML);

        otherDescription =
                PluginUtils.getPluginAttribute(DeviceWizardConstants.EXTENSION_INSTANCE_ID,
                        extensionId, DeviceWizardConstants.ELEMENT_OTHER,
                        DeviceWizardConstants.ATB_TITLE);
        otherTitle =
                PluginUtils.getPluginAttribute(DeviceWizardConstants.EXTENSION_INSTANCE_ID,
                        extensionId, DeviceWizardConstants.ELEMENT_OTHER,
                        DeviceWizardConstants.ATB_DESCRIPTION);

    }

    /**
     * Gets if wizard needs of progress monitor
     * @return
     */
    public boolean needsProgressMonitor()
    {
        return needsProgressMonitor;
    }

    /**
     * Gets if the wizard should force previous and next buttons
     * @return
     */
    public boolean forcePreviousAndNextButtons()
    {
        return forcePreviousAndNextButtons;
    }

    /**
     * gets if wizrd can finish early
     * @return
     */
    public boolean canFinishEarly()
    {
        return canFinishEarly;
    }

    /**
     * Gets image
     * @return
     */
    public String getImage()
    {
        return image;
    }

    /**
     * Get Title
     * @return
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Verify if has property Page
     * @return
     */
    public boolean hasProjectPage()
    {
        return ((customizer != null) && (customizer.hasCustomizedProjectPage()));
    }

    /**
     * Get Project Page class information
     * @return
     */
    public WizardPage getProjectPage()
    {
        WizardPage page = null;
        if (hasProjectPage())
        {
            page = customizer.getCustomizedProjectPage();
        }
        else
        {
            page = new WizardNewProjectPage(DeviceWizardConstants.PAGE_PROJECT);
        }
        return page;
    }

    /**
     * Get Project Page title information
     * @return
     */
    public String getProjectTitle()
    {
        if (projectTitle == null)
        {
            projectTitle = DeviceWizardResources.SEQUOYAH_Emulator_Wizard_Project_Title;
        }
        return projectTitle;
    }

    /**
     * Get Project Page description information
     * @return
     */
    public String getProjectDescription()
    {
        if (projectDescription == null)
        {
            projectDescription = DeviceWizardResources.SEQUOYAH_Emulator_Wizard_Project_Description;
        }
        return projectDescription;
    }

    /**
     * Verify if has property Page
     * @return
     */
    public boolean hasPropertyPage()
    {
        return ((customizer != null) && (customizer.hasCustomizedPropertyPage()));
    }

    /**
     * Get Property Page class information
     * @return
     */
    public WizardPage getPropertyPage()
    {
        //WizardPage page = customizer.getCustomizedPropertyPage();
        //if (page==null) {
        //	page = null;
        //}
        //return page;

        WizardPage page = null;
        if (hasPropertyPage())
        {
            page = customizer.getCustomizedPropertyPage();
        }
        else
        {
            page = new WizardNewPropertyPage(DeviceWizardConstants.PAGE_PROPERTY);
        }
        return page;

    }

    /**
     * Get Property Page title information
     * @return
     */
    public String getPropertyTitle()
    {
        return propertyTitle;
    }

    /**
     * Get Property Page description information
     * @return
     */
    public String getPropertyDescription()
    {
        return propertyDescription;
    }

    /**
     * Get Property Page xml information
     * @return
     */
    public String getPropertyXML()
    {
        return propertyXML;
    }

    /**
     * Verify if has other page
     * @return
     */
    public boolean hasOtherPage()
    {
        return ((customizer != null) && (customizer.hasCustomizedOtherPage()));
    }

    /**
     * Get Other Page class information
     * @return
     */
    public WizardPage getOtherPage()
    {
        WizardPage page = customizer.getCustomizedOtherPage();
        if (page == null)
        {
            page = null;
        }
        return page;
    }

    /**
     * Get Other Page title information
     * @return
     */
    public String getOtherTitle()
    {
        return otherTitle;
    }

    /**
     * Get Other Page description information
     * @return
     */
    public String getOtherDescription()
    {
        return otherDescription;
    }
}
