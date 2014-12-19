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
package org.eclipse.andmore.android.monkey.options;

import java.util.HashMap;
import java.util.Map;

/**
 * This interface contains constants used for the Monkey Options Management
 */
@SuppressWarnings("serial")
public interface IMonkeyOptionsConstants {

	/*
	 * XML Path
	 */
	public final String MONKEY_OPTIONS_XML_PATH = "resources/monkey_options.xml";

	/*
	 * XML tags
	 */
	public final String ROOT_TAG = "monkeyOptions";

	public final String GROUP_TAG = "group";

	public final String GROUP_TAG_ID = "id";

	public final String MONKEY_OPT_TAG = "monkeyOption";

	public final String MONKEY_OPT_TAG_NAME = "name";

	public final String MONKEY_OPT_TAG_FRIENDLY_NAME = "fName";

	public final String MONKEY_OPT_TAG_TYPE = "type";

	public final String MONKEY_OPT_TAG_TYPE_DETAILS = "typeDetails";

	public final String MONKEY_OPT_TAG_DESCRIPTION = "description";

	public final String PREDEFINED_VALUES_TAG = "values";

	public final String PREDEFINED_VALUE_TAG = "value";

	/*
	 * Monkey option value type
	 */
	public final int TYPE_NONE = 0;

	public final int TYPE_TEXT = 1;

	public final int TYPE_PATH = 2;

	public final int TYPE_NUMBER = 3;

	public final String TYPE_PATH_DIR = "dir";

	public final Map<String, Integer> TYPE_MAP = new HashMap<String, Integer>() {
		{
			put("none", TYPE_NONE);
			put("text", TYPE_TEXT);
			put("path", TYPE_PATH);
			put("int", TYPE_NUMBER);
		}

	};

	/*
	 * Other options
	 */
	public final String OTHERS_GROUP = "Others";

	public final String OTHERS_OTHER = "other";

	/*
	 * Categories options
	 */
	public final String CATEGORY_OPTION = "-c";
}
