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
package org.eclipse.andmore.android.emulator.core.skin;

import java.util.HashMap;
import java.util.Map;

/**
 * DESCRIPTION: This bean holds data from the skin.xml file
 *
 * RESPONSIBILITY: - Provide an easy way to retrieve data read from skin.xml
 * files
 *
 * COLABORATORS: None.
 *
 * USAGE: Call any of the interface methods to add or retrieve data to the class
 * model
 */
public class AndroidSkinBean {
	private final Map<String, Integer> skinPropertiesMap = new HashMap<String, Integer>();

	/**
	 * Adds a skin property to the bean
	 * 
	 * @param key
	 *            The skin property key to use
	 * @param value
	 *            The value of the skin property
	 */
	public void addSkinPropertyValue(String key, int value) {
		skinPropertiesMap.put(key, value);
	}

	/**
	 * Retrieves a value of a skin property identified by key
	 * 
	 * @param key
	 *            The key that identifies the desired property
	 * 
	 * @return The value of the desired property
	 */
	public int getSkinPropertyValue(String key) {
		if (skinPropertiesMap.get(key) != null) {
			return skinPropertiesMap.get(key);
		} else {
			return 0;
		}
	}

	/**
	 * Tests if open external display information is available at the skin which
	 * properties are stored at this bean
	 *
	 * @return True if open external display information is available; false
	 *         otherwise
	 */
	public boolean isOpenExternalDisplayAvailable() {
		boolean result = true;
		Integer testObj1 = skinPropertiesMap.get(ISkinKeyXmlTags.SKIN_OPEN_EXTERNAL_VIEW_WIDTH);
		Integer testObj2 = skinPropertiesMap.get(ISkinKeyXmlTags.SKIN_OPEN_EXTERNAL_VIEW_HEIGHT);

		// If any of the width and height information is not available
		// it is considered that there is not enough information about
		// the open external display
		if ((testObj1 == null) || (testObj2 == null)) {
			result = false;
		}

		return result;
	}

	/**
	 * Tests if external display information is available at the skin which
	 * properties are stored at this bean
	 *
	 * @return True if external display information is available; false
	 *         otherwise
	 */
	public boolean isExternalDisplayAvailable() {
		boolean result = true;
		Integer testObj1 = skinPropertiesMap.get(ISkinKeyXmlTags.SKIN_EXTERNAL_VIEW_WIDTH);
		Integer testObj2 = skinPropertiesMap.get(ISkinKeyXmlTags.SKIN_EXTERNAL_VIEW_HEIGHT);

		// If any of the width and height information is not available
		// it is considered that there is not enough information about
		// the external display
		if ((testObj1 == null) || (testObj2 == null)) {
			result = false;
		}

		return result;
	}

	public double getEmbeddedViewScale() {
		Integer testObj1 = skinPropertiesMap.get("embeddedViewScale");

		return testObj1.intValue() / 10.0;
	}

}
