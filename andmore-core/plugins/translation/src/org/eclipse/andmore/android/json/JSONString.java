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

import java.util.List;

public class JSONString extends JSONValue {
	private final String value;

	public JSONString(String value) {
		this.value = value;
	}

	@Override
	public String getValue() {
		return value;
	}

	public static JSONValue parseValues(List<Character> json) {
		String value = null;
		boolean parsed = false;
		while (!parsed) {
			Character next = json.get(0);
			if (next == '"') {

				value = parseName(json);
				parsed = true;
			}
		}

		return new JSONString(value);
	}

	private static String parseName(List<Character> json) {
		String name = null;
		StringBuilder nameBuilder = new StringBuilder();
		boolean specialChar = false;
		Character next;
		json.remove(0);
		while (name == null) {
			next = json.remove(0);
			if (next == '"') {
				if (specialChar) {
					specialChar = false;
					nameBuilder.append(next);
				} else {
					name = nameBuilder.toString();
				}
			} else if (next == '\\') {
				specialChar = true;
			} else {
				specialChar = false;
				nameBuilder.append(next);
			}
		}
		return name;
	}

	@Override
	public String toString() {
		return "\"" + value + "\"";
	}

}
