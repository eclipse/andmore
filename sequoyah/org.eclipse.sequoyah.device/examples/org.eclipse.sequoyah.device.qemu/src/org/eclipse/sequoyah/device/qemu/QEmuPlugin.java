/*******************************************************************************
 * Copyright (c) 2008-2010 MontaVista Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Yu-Fen Kuo (MontaVista) - initial API and implementation
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/
package org.eclipse.sequoyah.device.qemu;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class QEmuPlugin extends BasePlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.sequoyah.device.qemu"; //$NON-NLS-1$

	// The shared instance
	private static QEmuPlugin plugin;

	private final static String ICONS_PATH = "$nl$/icons/";//$NON-NLS-1$

	/**
	 * image key for new device instance wizard
	 */
	public static String IMAGEKEY_NEW_DEVICE_WIZARD = "$newDeviceImageKey$"; //$NON-NLS-1$

	private ImageDescriptor newDeviceWizardImageDescriptor = getImageDescriptor("full/wizban/newqemu.jpg"); //$NON-NLS-1$
	private ResourceBundle resourceBundle;

	/**
	 * The constructor
	 */
	public QEmuPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static QEmuPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle, or 'key' if not
	 * found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = QEmuPlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns a string formatted from the specified resource string and the
	 * associated arguments.
	 * 
	 * @param key
	 *            the key associated with the format string in the resource
	 *            bundle
	 * @param args
	 *            items to be expanded into the format string
	 * @return formatted string
	 */
	public static String getResourceString(String key, Object[] args) {
		return MessageFormat.format(getResourceString(key), args);
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		try {
			if (resourceBundle == null)
				resourceBundle = ResourceBundle.getBundle(PLUGIN_ID
						+ ".QEmuResources"); //$NON-NLS-1$
		} catch (MissingResourceException e) {
			logErrorMessage(e.getMessage());
			resourceBundle = null;
		}
		return resourceBundle;
	}

	/**
	 * Logs the specified status with this plug-in's log.
	 * 
	 * @param status
	 *            status to log
	 */
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	/**
	 * Logs an internal error with the specified message.
	 * 
	 * @param message
	 *            the error message to log
	 */
	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, getUniqueIdentifier(), IStatus.ERROR,
				message, null));
	}

	/**
	 * Logs an internal error with the specified throwable
	 * 
	 * @param e
	 *            the exception to be logged
	 */
	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getUniqueIdentifier(), IStatus.ERROR, e
				.getMessage(), e));
	}

	/**
	 * Convenience method which returns the unique identifier of this plugin.
	 */
	public static String getUniqueIdentifier() {
		if (getDefault() == null) {
			return PLUGIN_ID;
		}
		return getDefault().getBundle().getSymbolicName();
	}

	/**
	 * get image descriptor from relative path under plugin's icons path
	 * 
	 * @param relativePath
	 * @return ImageDescriptor
	 */
	public ImageDescriptor getImageDescriptor(String relativePath) {
		return imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH + relativePath);
	}

	/**
	 * initialize image registry
	 * 
	 * @param reg
	 */
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(IMAGEKEY_NEW_DEVICE_WIZARD, newDeviceWizardImageDescriptor);
		super.initializeImageRegistry(reg);
	}

	@Override
	protected void initializeImageRegistry() {

	}

}
