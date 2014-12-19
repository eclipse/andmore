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

package org.eclipse.andmore.android.json;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This class is responsible to parse a JSON string into objects Visit
 * {@link http://www.json.org/} for more information
 *
 */
public class Jason {
	private final Set<JSONObject> objects;

	public Jason(String value) {
		objects = new HashSet<JSONObject>();
		stip(value);
	}

	public Set<JSONObject> getJSON() {
		return objects;
	}

	private void stip(String value) {
		List<Character> json = new ArrayList<Character>();
		for (char c : value.toCharArray()) {
			json.add(c);
		}

		while (json.size() > 0) {
			Character next = json.get(0);

			if (next == '{') {
				JSONObject object = (JSONObject) JSONValue.parse(json);
				objects.add(object);
			} else if ((next == ' ') || (next == '\r') || (next == '\n') || (next == ',')) {
				json.remove(0);
			}
		}
	}

	@Override
	public String toString() {
		String string = "";
		Iterator<JSONObject> objectIterator = objects.iterator();
		while (objectIterator.hasNext()) {
			string += objectIterator.next().toString();
			if (objectIterator.hasNext()) {
				string += ",";
			}
		}
		return string;
	}
}
