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
package org.eclipse.andmore.android.emulator.ui.perspective.extension;

import static org.eclipse.andmore.android.common.log.StudioLogger.warn;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;

/**
 * DESCRIPTION: <br>
 * This class is the reader for the androidPerspectiveExtension extension point
 * declarations. <br>
 * RESPONSIBILITY: <br>
 * Retrieve information from plugins declaring the androidPerspectiveExtension. <br>
 * COLABORATORS: <br>
 * IAndroidPerspectiveExtensionConstants: implements this interface for using
 * the constants directly <br>
 * USAGE: <br>
 * This class methods should be called statically from within the implementation
 * of the Android Emulator Perspective.
 */
public class AndroidPerspectiveExtensionReader implements IAndroidPerspectiveExtensionConstants {

	/**
	 * Reads the information from plug-ins declaring extensions to
	 * androidPerspectiveExtension and stores it into beans.
	 * 
	 * @return a collection of beans containing androidPerspectiveExtension
	 *         information
	 */
	public static Collection<AndroidPerspectiveExtensionBean> readAndroidPerspectiveExtensions() {
		Collection<AndroidPerspectiveExtensionBean> beans = new LinkedHashSet<AndroidPerspectiveExtensionBean>();

		IExtension[] extensions = EclipseUtils.getInstalledPlugins(EXTENSION_POINT_ID);

		for (IExtension extension : extensions) {
			IConfigurationElement[] viewElements = extension.getConfigurationElements();

			for (IConfigurationElement viewElement : viewElements) {
				if (ELEMENT_VIEW.equals(viewElement.getName())) {
					String viewId = viewElement.getAttribute(ATT_ID);
					String area = viewElement.getAttribute(ATT_AREA);

					// create the bean only if information could be read
					if ((viewId != null) && (!viewId.equals("")) && (area != null) && (!area.equals(""))) {
						beans.add(new AndroidPerspectiveExtensionBean(viewId, area));
					} else {
						warn("View not added to Android Emulator Perspective area for extension "
								+ extension.getExtensionPointUniqueIdentifier());
					}
				}
			}
		}

		return beans;
	}
}
