/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Fantato (Eldorado Research Institute)
 *
 * Contributors:
 * Fabio Fantato (Eldorado Research Institute) - [243493] - PluginUtils has some compatibility issues with Ganymede release
 * Daniel Barboza Franco (Eldorado Research Institute) - [221740] - Sample implementation for Linux host
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Marcelo Marzola Bossoni (Instituto de Pesquisas Eldorado) - [352157] Add NPE check 
 ********************************************************************************/

package org.eclipse.sequoyah.device.common.utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahExceptionHandler;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahExceptionStatus;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

/**
 * This class serves as an utility class containing only static methods such as getters
 * for plugin attributes, plugin resources, installation path etc
 */
public class PluginUtils
{
	
	 public static IProject getSelectedProjectInWorkbench()  {
		 IProject selectedProject = null;
		 try
		 {
			 IWorkbench iworkbench             = PlatformUI.getWorkbench();
			 IWorkbenchWindow iworkbenchwindow = iworkbench.getActiveWorkbenchWindow();
			 IWorkbenchPage iworkbenchpage     = iworkbenchwindow.getActivePage();
			 ISelection selection              = iworkbenchpage.getSelection();
			 IResource resource                = extractSelection(selection);
			 if (resource == null)
			 { 
				 IEditorPart editor = iworkbenchpage.getActiveEditor();
			 	 IEditorInput input = editor.getEditorInput();
			 	 resource           = ( IResource )input.getAdapter(IResource.class);
		 	  }
		 	  selectedProject = resource.getProject();
	 		}
	 	catch (Exception npe){
	 		BasePlugin.logError("Exception: " + npe.getMessage());
	 	}
	 	return selectedProject;
}
	 
	 public static IProject getSelectedProjectInWorkbench(ISelection selection)  {
		 IProject selectedProject = null;
		 try{
			 IWorkbench iworkbench             = PlatformUI.getWorkbench();
			 IWorkbenchWindow iworkbenchwindow = iworkbench.getActiveWorkbenchWindow();
			 IWorkbenchPage iworkbenchpage     = iworkbenchwindow.getActivePage();			 
			 IResource resource                = extractSelection(selection);
		 if (resource == null)
		 { 
			 IEditorPart editor = iworkbenchpage.getActiveEditor();
			 IEditorInput input = editor.getEditorInput();
			 resource           = ( IResource )input.getAdapter(IResource.class);
		 }
		 selectedProject = resource.getProject();
		}
	 	catch (Exception npe){
	 		//
	 	}	 	
	 	return selectedProject;
	}
	 

	 private static IResource extractSelection(ISelection sel) {
		 if (!(sel instanceof IStructuredSelection))
		 return null;
		 IStructuredSelection ss = (IStructuredSelection) sel;
		 IResource element =(IResource) ss.getFirstElement();
		 if (element instanceof IResource)
		 return element;
		 if (!(element instanceof IAdaptable))
		 return null;
		 IAdaptable adaptable = element;
		 Object adapter = adaptable.getAdapter(IResource.class);
		 return (IResource) adapter;
		 }

	
	
    
    /**
     * Returns a class reference based in element name
     * @param extensionId
     * @param elementName the extension element
     * @return the value of the extension attribute
     * @throws CoreException
     */
    public static Object getExecutable(String extensionId, String elementName)
                                throws CoreException
    {
        Object executable = null;

        IExtension fromExtension = getExtension(extensionId);

        if ((fromExtension != null) && (elementName != null))
        {
            IConfigurationElement[] elements = fromExtension.getConfigurationElements();

            for (IConfigurationElement element : elements)
            {
                if (elementName.equals(element.getName()))
                {
                    executable = element.createExecutableExtension("class"); //$NON-NLS-1$
                }
            }
        }

        return executable;
    }

    /**
     * Returns the extension using as parameters the id of the extension
     * and the id of its extension point.
     *
     * @param extensionPointId
     *         the id of the extension point
     * @param extensionId
     *         the id of the extension
     *
     * @return
     *         the extension
     */
    public static IExtension getExtension(String extensionPointId, String extensionId)
    {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtension extension        = registry.getExtension(extensionPointId, extensionId);
        return extension;
    }

    /**
     * Returns the extension using as parameter only the id of the extension.
     *
     * @param extensionId
     *         the id of the extension
     *
     * @return
     *         the extension
     */
    public static IExtension getExtension(String extensionId)
    {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtension extension        = registry.getExtension(extensionId);

        return extension;
    }

    /**
     * Returns the label for the extension (extension name) using as parameters
     * the id of the extension and the id of its extension point.
     *
     * @param extensionPointId
     *         the id of the extension point
     * @param extensionId
     *         the id of the extension
     *
     * @return
     *         the extension label
     */
    public static String getExtensionLabel(String extensionPointId, String extensionId)
    {
        IExtension extension  = getExtension(extensionPointId, extensionId);
        String extensionLabel;

        if (extension != null)
        {
            extensionLabel = extension.getLabel();
        }
        else
        {
            extensionLabel = extensionId;
        }

        return extensionLabel;
    }

    /**
     * Returns the label for the extension (extension name) using as parameter only
     * the id of the extension.
     *
     * @param extensionId
     *         the id of the extension
     *
     * @return
     *         the extension label
     */
    public static String getExtensionLabel(String extensionId)
    {
        IExtension extension  = getExtension(extensionId);
        String extensionLabel;

        if (extension != null)
        {
            extensionLabel = extension.getLabel();
        }
        else
        {
            extensionLabel = extensionId;
        }

        return extensionLabel;
    }

    
    /**
     * Returns a collection of strings containing the ids of installed plugins.
     *
     * @param extensionPointId
     *         the id of the extension point
     *
     * @return
     *         a collection object containing the ids of the installed plugins
     */
    public static Collection<String> getInstalledPlugins(String extensionPointId)
    {
        IExtensionRegistry registry           = Platform.getExtensionRegistry();
        IExtensionPoint extensionPoint        = registry.getExtensionPoint(extensionPointId);
        Collection<String> sampleAppPluginIds = new LinkedHashSet<String>();

        for (IExtension extension : extensionPoint.getExtensions())
        {
            sampleAppPluginIds.add(extension.getUniqueIdentifier());
        }

        return sampleAppPluginIds;
    }
    
    /**
     * Returns a collection of strings containing the ids of installed plugins.
     *
     * @param extensionPointId
     *         the id of the extension point
     *
     * @return
     *         a collection object containing the ids of the installed plugins
     */
    public static Collection<IExtension> getInstalledExtensions(String extensionPointId)
    {
        IExtensionRegistry registry           = Platform.getExtensionRegistry();
        IExtensionPoint extensionPoint        = registry.getExtensionPoint(extensionPointId);
        Collection<IExtension> sampleAppPluginIds = new LinkedHashSet<IExtension>();

        for (IExtension extension : extensionPoint.getExtensions())
        {
            sampleAppPluginIds.add(extension);
        }

        return sampleAppPluginIds;
    }
    
    

    /**
     * Fills an array object with the ids contained in the collection object returned by
     * {@link #getInstalledPlugins(String)}.
     *
     * @param extensionPointId
     *         the id of the extension point
     *
     * @return
     *         an array object containing the ids of the installed plugins
     */
    public static String[] getInstalledPluginsAsArray(String extensionPointId)
    {
        Collection<String> sampleAppPluginIds = getInstalledPlugins(extensionPointId);
        String[] sampleAppPluginIdsArray      = new String[sampleAppPluginIds.size()];

        return sampleAppPluginIds.toArray(sampleAppPluginIdsArray);
    }

      
    
    /**
     * Returns a plugin attribute using the extension id and the extension point id as parameters.
     *
     * @param extensionPointId
     *         the id of the extension point of the exten sion
     * @param extensionId
     *         the id of the extension
     * @param element
     *         the extension element
     * @param attribute
     *         the extension attribute
     *
     * @return
     *         the value of the extension attribute
     */
    public static String getPluginAttribute(String extensionPointId, String extensionId,
                                            String element, String attribute)
    {
        IExtension fromPlugin = getExtension(extensionPointId, extensionId);

        return getPluginAttribute(fromPlugin, element, attribute);
    }

    /**
     * Returns a plugin attribute using the extension id and the extension point id as parameters.
     *
     * @param extensionId
     *         the id of the extension
     * @param element
     *         the extension element
     * @param attribute
     *         the extension attribute
     *
     * @return
     *         the value of the extension attribute
     */
    public static String getPluginAttribute(String extensionId, String element, String attribute)
    {
        IExtension fromPlugin = getExtension(extensionId);

        return getPluginAttribute(fromPlugin, element, attribute);
    }

    /**
     * Returns a plugin attribute using the extension as parameter.
     *
     * @param fromExtension
     *         the extension from which the attribute should be collected
     * @param element
     *         the extension element
     * @param attribute
     *         the extension attribute
     *
     * @return
     *         the value of the extension attribute
     */
    public static String getPluginAttribute(IExtension fromExtension, String element,
                                            String attribute)
    {
        String attributeValue = null;

        if (fromExtension != null)
        {
            IConfigurationElement[] ceArray = fromExtension.getConfigurationElements();

            for (IConfigurationElement ce : ceArray)
            {
                if ((ce != null) && ce.getName().equals(element))
                {
                    attributeValue = ce.getAttribute(attribute);
                }
            }
        }

        return attributeValue;
    }
    
     
    
    public static List<IConfigurationElement> getPluginElementList(IExtension fromExtension, String element,String subElement)
    {
    	List<IConfigurationElement> listValue = new ArrayList<IConfigurationElement>();

        if (fromExtension != null)
        {
            IConfigurationElement[] ceArray = fromExtension.getConfigurationElements();

            for (IConfigurationElement ce : ceArray)
            {
                if ((ce != null) && ce.getName().equals(element))
                {
                	 IConfigurationElement[] sceArray = ce.getChildren(subElement);
                	 for (IConfigurationElement sce : sceArray)
                     {
                         listValue.add(sce);                     
                     }
                }
            }
        }

        return listValue;
    }
    
    
    /**
     * Returns a plugin attribute using the extension as parameter.
     *
     * @param fromExtension
     *         the extension from which the attribute should be collected
     * @param element
     *         the extension element
     * @param attribute
     *         the extension attribute
     *
     * @return
     *         the value of the extension attribute
     */
    public static Object getExecutableAttribute(IExtension fromExtension, String elementName,
                                            String attribute)  throws CoreException
    {
    	Object executable = null;

        if (fromExtension != null)
        {
            IConfigurationElement[] elements = fromExtension.getConfigurationElements();

            for (IConfigurationElement element : elements)
            {
                if (elementName.equals(element.getName()))
                {
                	if (element.getAttribute(attribute) != null	&& element.getAttribute(attribute).trim().length() > 0) {
						executable = element.createExecutableExtension(attribute);
					}
                }
            }
        }
        return executable;
    }

    /**
     * Returns the absolute path of installation as a file object using the plugin as parameter.
     *
     * @param plugin
     *         the plugin installed
     *
     * @return
     *         a file object pointing to the installation path of the plugin
     */
    public static File getPluginInstallationPath(Plugin plugin)
    {
        Bundle pluginBundle = plugin.getBundle();

        return getPluginInstallationPath(pluginBundle);
    }

    /**
     * Returns the absolute path of installation as a file object using the ids of the extension
     * and extension point as parameters.
     *
     * @param extensionPointId
     *         the id of the extension point
     * @param extensionId
     *         the id of the extension
     *
     * @return
     *         a file object pointing to the installation path of the plugin
     */
    public static File getPluginInstallationPath(String extensionPointId, String extensionId)
    {
        IExtension extension = getExtension(extensionPointId, extensionId);

        return getPluginInstallationPath(extension);
    }

    /**
     * Returns the absolute path of installation as a file object using the extension as parameter.
     *
     * @param extension
     *         the extension object
     *
     * @return
     *         a file object pointing to the installation path of the plugin
     */
    public static File getPluginInstallationPath(IExtension extension)
    {
        String pluginId     = extension.getNamespaceIdentifier();
        Bundle pluginBundle = Platform.getBundle(pluginId);

        return getPluginInstallationPath(pluginBundle);
    }

    /**
     * Returns the absolute path of installation as a file object using the plugin bundle as parameter.
     *
     * @param pluginBundle
     *         the plugin bundle
     *
     * @return
     *         a file object pointing to the installation path of the plugin
     */
    public static File getPluginInstallationPath(Bundle pluginBundle)
    {
        //String platformPath = Platform.getInstallLocation().getURL().getPath();
    	String pluginPath = pluginBundle.getLocation();
        int removeIndex = pluginPath.indexOf("file:"); //$NON-NLS-1$
        
        if (Platform.getOS().equals(Platform.OS_WIN32)) {
        	pluginPath = pluginPath.substring(removeIndex + 6);	
        }
        else {
        	pluginPath = pluginPath.substring(removeIndex + 5);
        }

        File relativeInstalationPath = new File(pluginPath);
        return FileUtil.getCanonicalFile(relativeInstalationPath);
    }

    /**
     * Returns a file object from the path: $installationPath\resource
     *
     * @param plugin
     *         the plugin object
     *
     * @param resource
     *         the plugin resource
     *
     * @return
     *         a file object pointing to the path of the resource
     *
     * @throws ResourceNotAvailable
     *         throws an exception if it occurs an I/O exception with the path $installationPath\resource
     */
    public static File getPluginResource(Plugin plugin, String resource)
                                  throws SequoyahException
    {
		File canonicalFile = null;
    	try {
    		File pluginPath    = getPluginInstallationPath(plugin);
    		File resourceFile  = new File(pluginPath, resource);

    		canonicalFile      = FileUtil.getCanonicalFile(resourceFile);
    	} catch (Throwable t){
    		throw SequoyahExceptionHandler.exception(SequoyahExceptionStatus.CODE_ERROR_RESOURCE_NOT_AVAILABLE,t);
    	}
    	return canonicalFile;
    }

    /**
     * Checks if an extension is installed using the extension point id and extension id as parameters.
     *
     * @param extensionPointId
     *         the id of the extension point
     * @param extensionId
     *         the id of the extension
     *
     * @return
     *         true if the extension is installed or false otherwise
     */
    public static boolean isInstalled(String extensionPointId, String extensionId)
    {
        return getExtension(extensionPointId, extensionId) != null;
    }

    /**
     * Checks if an extension is installed using the extension id as parameter.
     *
     * @param extensionId
     *         the id of the extension
     *
     * @return
     *         true if the extension is installed or false otherwise
     */
    public static boolean isInstalled(String extensionId)
    {
        return getExtension(extensionId) != null;
    }
}
