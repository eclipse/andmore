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

public class JSONValueParser {
	private JSONValueParser() {
	};

	static JSONValue parse(List<Character> json) {
		JSONValue value = null;

		boolean parsed = false;

		while (!parsed) {
			Character next = json.get(0);

			if (next == '{') {
				value = JSONValue.parse(json);
				parsed = true;
			} else if (next == '[') {
				value = JSONValue.parse(json);
				parsed = true;
			} else if (next == '"') {
				value = JSONValue.parse(json);
				parsed = true;
			} else if (next == 'n') {
				value = JSONValue.parse(json);
				parsed = true;
			} else if (next == 't' || next == 'f') {
				value = JSONValue.parse(json);
				parsed = true;
			} else if (next >= 48 && next <= 57) {
				value = JSONValue.parse(json);
				parsed = true;
			} else if (next == ' ' || next == '\r' || next == '\n') {
				json.remove(0);
			}

		}
		return value;
	}
}
