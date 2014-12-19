/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.andmore.android.installer;

import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.andmore.android.installer.policy.MotodevPolicy;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class InstallerPlugin extends AbstractUIPlugin {

	private static InstallerPlugin plugin;

	public static final String PLUGIN_ID = "org.eclipse.andmore.android.installer"; //$NON-NLS-1$

	private static final String CONTRIBUTED_PAGE_EXTENSION_POINT_ID = PLUGIN_ID + ".configuration"; //$NON-NLS-1$

	private static final String CONTRIBUTED_PAGE_EXTENSION_ELEMENT = "page"; //$NON-NLS-1$

	public static final String CONTRIBUTED_PAGE_EXTENSION_PAGENAME = "name"; //$NON-NLS-1$

	public static final String CONTRIBUTED_PAGE_EXTENSION_PAGEDESCRIPTION = "description"; //$NON-NLS-1$

	public static final String CONTRIBUTED_PAGE_EXTENSION_PAGEID = "id"; //$NON-NLS-1$

	public static final String CONTRIBUTED_PAGE_EXTENSION_PAGECLASS = "class"; //$NON-NLS-1$

	public static final String DEFAULT_P2_PROFILE_NAME = "MOTODEV_Profile_Android"; //$NON-NLS-1$

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		if (canRegisterPolicy()) {
			Hashtable<String, Object> serviceProperties = new Hashtable<String, Object>();
			serviceProperties.put("service.ranking", new Integer(1500));
			context.registerService(Policy.class.getName(), new MotodevPolicy(), serviceProperties);
		}
	}

	private boolean canRegisterPolicy() {
		return "org.eclipse.andmore.android.product.android".equals(Platform.getProduct().getId());
	}

	/**
	 * Return the BundleContext for this bundle.
	 * 
	 * @return BundleContext
	 */
	public static BundleContext getContext() {
		return plugin.getBundle().getBundleContext();
	}

	/**
	 * Return the plugin instance.
	 * 
	 * @return the plugin instance
	 */
	public static InstallerPlugin getDefault() {
		return plugin;
	}

	/**
	 * Retrieve the map of configuration elements of contribution pages
	 * 
	 * @return the elements of contribution pages, where the Key is the id of
	 *         the page declared on extension
	 */
	public static Map<String, IConfigurationElement> loadContributedPages() {
		Map<String, IConfigurationElement> pages = new LinkedHashMap<String, IConfigurationElement>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		if (registry != null) {
			IExtensionPoint extensionPoint = registry.getExtensionPoint(CONTRIBUTED_PAGE_EXTENSION_POINT_ID);
			for (IExtension extension : extensionPoint.getExtensions()) {
				for (IConfigurationElement configElement : extension.getConfigurationElements()) {
					if (configElement.getName().equals(CONTRIBUTED_PAGE_EXTENSION_ELEMENT)) {
						pages.put(configElement.getAttribute(CONTRIBUTED_PAGE_EXTENSION_PAGEID), configElement);
					}
				}
			}
		}

		return pages;
	}
}
