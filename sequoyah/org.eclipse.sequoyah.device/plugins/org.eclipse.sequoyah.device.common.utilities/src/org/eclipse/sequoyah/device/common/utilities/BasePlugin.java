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
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.common.utilities;


import java.net.URL;
import java.util.Hashtable;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.sequoyah.device.common.utilities.logger.ILogger;
import org.eclipse.sequoyah.device.common.utilities.logger.LoggerConstants;
import org.eclipse.sequoyah.device.common.utilities.logger.LoggerFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * A base plugin class offering common operations.
 */
public abstract class BasePlugin extends AbstractUIPlugin 
{

	// static variables
    private static BasePlugin baseInst = null;

    /**
     * Logger object for logging messages for servicing purposes.
     */
	protected static ILogger log = null;		
    
	// instance variables
    private Hashtable<String, ImageDescriptor> imageDescriptorRegistry = null;	
    private boolean headless;
    private boolean headlessSet;

    /**
     * Returns the singleton object representing the base plugin.
     * @return the singleton object.
     */
    public static BasePlugin getBaseDefault() {
	    return baseInst;
    }
    
	/**
	 * Returns the active workbench shell.
	 * @return the active workbench shell.
	 */
	public static Shell getActiveWorkbenchShell() {
	    
	    IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			return window.getShell();
		}
		Display d = Display.getCurrent();
		if (d!=null) return d.getActiveShell();
		d = Display.getDefault();
		if (d!=null) return d.getActiveShell();
		return null;
	}

	/**
	 * Returns the active workbench window.
	 * @return the active workbench window. 
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
	    
		IWorkbench wb = null;
		
		try {
		    wb = getBaseDefault().getWorkbench();
		}
		catch (Exception exc) {
		    // in headless mode
		    wb = null;
		}
		
		// if we are not in headless mode
		if (wb != null) {
		    
		    // if in user interface thread, return the workbench active window
			if (Display.getCurrent() != null) {
				return wb.getActiveWorkbenchWindow();
			}
			// otherwise, get a list of all the windows, and simply return the first one
			// KM: why do we need this??
			else {
				IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
				
				if (windows != null && windows.length > 0) {
					return windows[0];
				}
			}
			
			return null;
		}
		else {
			return null;
		}
	}

	/**
	 * Returns the workspace root.
	 * @return the workspace root.
	 */
	public static IWorkspaceRoot getWorkspaceRoot() {
	    return getWorkspace().getRoot();
	}

	/**
	 * Returns the workspace.
	 * @return the workspace.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * @return the prefix of the path for icons, i.e. "icons/".
	 */
	public static String getIconPath() {
		return "icons/"; //$NON-NLS-1$
	}

	/**
	 * Retrieve image in any plugin's directory tree, given its file name.
	 * The file name should be qualified relative to this plugin's bundle. Eg "icons/myicon.gif"
	 */
	public static ImageDescriptor getPluginImage(Bundle bundle, String fileName)
	{
	   URL path = bundle.getEntry("/" + fileName); //$NON-NLS-1$
	   ImageDescriptor descriptor = ImageDescriptor.createFromURL(path);
	   return descriptor;
	}
	
	
	public static void logInfo(String message) 
	{
		getLogger().info(message);
	}

	
	public static void logWarning(String message) 
	{
		getLogger().warn(message);
	}

	
	public static void logError(String message) 
	{
		getLogger().error(message);
	}

	
	public static void logError(String message, Throwable exception) 
	{
		getLogger().error(message, exception);
	}

	
	public static void logDebugMessage(String prefix, String message) 
	{		
		getLogger().debug(prefix+"-"+message); //$NON-NLS-1$
	}

	/**
	 * Constructor.
	 */
	public BasePlugin() {
	    if (baseInst == null) {
	        baseInst = this;
	    }
	    
		headless = false;
		headlessSet = false;
	}
	
	// ------------------------
	// STATIC HELPER METHODS...
	// ------------------------

    /**
     * Returns the symbolic name of the bundle.
     * @return the symbolic name of the bundle.
     */   
	public String getSymbolicName() {
		return getBundle().getSymbolicName();
	}
	
  
	// -------------------------------------
	// ABSTRACTUIPLUGIN LIFECYCLE METHODS...
	// -------------------------------------
    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {     
        super.start(context);		
    }
    
    public void setLogger(ILogger logger) {
    	log = logger;
    }
    
    public static void setDefaultLogger() {
    	// logger
	    if (log == null) {
	    	log = LoggerFactory.getLogger(LoggerConstants.LOG_SIMPLE);
	    }
    }
     
    
    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
    	logDebugMessage(this.getClass().getName(), "SHUTDOWN"); //$NON-NLS-1$
        super.stop(context);
    }
    
	/**
	 * Returns the Platform UI workbench.  
	 * <p> 
	 * This method exists as a convenience for plugin implementors.  The
	 * workbench can also be accessed by invoking <code>PlatformUI.getWorkbench()</code>.
	 * </p>
	 * <p>
	 * This is an intercept of the AbstractUIPlugin method, so we can do a try/catch around
	 *  it, as it will throw an exception if we are running headless, in which case the 
	 *  workbench has not even been started.
	 * </p>
	 */
	public IWorkbench getWorkbench() 
	{
		IWorkbench wb = null;
		if (headlessSet && headless) // already been here?
		 	return wb;
		try {
			wb = PlatformUI.getWorkbench();
				headless = false;			
		} 
		catch (Exception exc)
		{
			headless = true;
		}
		headlessSet = true;
		return wb;
	}

    /**
	 * Initialize the image registry by declaring all of the required graphics.
	 * Typically this is a series of calls to putImageInRegistry. Use
	 * getIconPath() to qualify the file name of the icons with their relative
	 * path.
	 */
	protected abstract void initializeImageRegistry();

	/**
     * Construct an image descriptor from a file name and place it in the 
     * image descriptor registry. Actual image construction is delayed until first use.
     * @param id - an arbitrary ID to assign to this image. Used later when retrieving it.
     * @param fileName - the name of the icon file, with extension, relative to this plugin's folder.
     * @return the image descriptor for this particular id.
     */
    protected ImageDescriptor putImageInRegistry(String id, String fileName)
    {
	   ImageDescriptor fid = getPluginImage(fileName);
	   Hashtable<String, ImageDescriptor> t = getImageDescriptorRegistry();
	   t.put(id, fid);
	   return fid;
    }
    
    /**
	 * Retrieve an image descriptor in this plugin's directory tree given its file name. The
	 * file name should be qualified relative to this plugin's bundle. 
	 * For example "icons/myicon.gif"
	 * @param imagePath the path name to the image relative to this bundle
	 * @return the image descriptor
	 */
	public ImageDescriptor getPluginImage(String imagePath) {
		return getPluginImage(getBundle(), imagePath);
	}

    /**
	 * Retrieves or creates an image based on its id. The image is then stored
	 * in the image registry if it is created so that it may be retrieved again.
	 * Thus, image resources retrieved in this way need not be disposed by the
	 * caller.
	 * 
	 * @param key the id of the image to retrieve.
	 * @return the Image resource for this id.
	 */
    public Image getImage(String key)
    {
    	// First check the image registry
    	ImageRegistry imageRegistry = getImageRegistry();
		Image image = imageRegistry.get(key);
		if (image == null) { // check the image descriptor registry
			ImageDescriptor descriptor = getImageDescriptor(key);
			if (descriptor != null) {
				imageRegistry.put(key, descriptor);
				image = imageRegistry.get(key);
			} else {
				logError("...error retrieving image for key: " + key); //$NON-NLS-1$
			}
		}
		return image;
    }
    
    /**
	 * Returns the image descriptor that has been registered to this id.
	 * @param key the id of the image descriptor to retrieve
	 * @return an ImageDescriptor
	 */
	public ImageDescriptor getImageDescriptor(String key) {
		Hashtable<String, ImageDescriptor> t = getImageDescriptorRegistry();
		ImageDescriptor descriptor = t.get(key);
		return descriptor;
	}  
	
	/**
	 * Gets the hashtable that is the image descriptor registry. Creates and populates
	 * it if necessary.
	 * @return The image descriptor registry hashtable.
	 */
	private Hashtable<String, ImageDescriptor> getImageDescriptorRegistry() {
		if (imageDescriptorRegistry == null) {
			imageDescriptorRegistry = new Hashtable<String, ImageDescriptor>();
			initializeImageRegistry();
		}
		return imageDescriptorRegistry;
	}

	/**
	 * Returns an image descriptor from the base IDE. Looks only in the "icons/full/" directories.
	 * 
	 * @see org.eclipse.ui.views.navigator.ResourceNavigatorActionGroup#getImageDescriptor(java.lang.String)
	 */
	public ImageDescriptor getImageDescriptorFromIDE(String relativePath) 
	{
		Hashtable<String, ImageDescriptor> registry = getImageDescriptorRegistry();
		ImageDescriptor descriptor = registry.get(relativePath);
		if (descriptor == null) {
			String iconPath = "icons/full/"; //$NON-NLS-1$
			String key = iconPath + relativePath;
			String[] bundleNames = new String[] {"org.eclipse.ui", "org.eclipse.ui.ide"}; //$NON-NLS-1$ //$NON-NLS-2$
			for (int i = 0; (i < bundleNames.length) && (descriptor == null); i++) {
				String bundleName = bundleNames[i];
			    Bundle bundle = Platform.getBundle(bundleName);
			    URL url = bundle.getResource(key);
			    if (url != null) {
			    	descriptor = ImageDescriptor.createFromURL(url);
			    }
			}
			if (descriptor == null) {
				descriptor = ImageDescriptor.getMissingImageDescriptor();
			}
			registry.put(relativePath, descriptor);
		}
		return descriptor;
	}    
            
    // -----------------    
    // LOGGER METHODS...
    // -----------------
          
	/**
     * Get the logger for this plugin. You should not have to directly access
     * the logger, since helper methods are already provided in this class.
     * Use with care.
     */
    public static ILogger getLogger() 
    {
    	// logger
	    if (log == null) {
	    	setDefaultLogger();
	    }
    	return log;
    }        

	// -------------------------    
	// MISCELLANEOUS METHODS...
	// -------------------------
	
	/**
	 * Return true if we are running in a headless environment. We equate this
	 *  to mean that the workbench is not running.
	 */
	public boolean isHeadless()
	{
		if (!headlessSet)
			getWorkbench();
		return headless;
	}
}