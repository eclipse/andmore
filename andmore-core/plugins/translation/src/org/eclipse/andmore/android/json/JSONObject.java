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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class JSONObject extends JSONValue {
	private final Set<JSONPair> value;

	public JSONObject(Set<JSONPair> value) {
		this.value = value;
	}

	@Override
	public Set<JSONPair> getValue() {
		return value;
	}

	static JSONValue parseValues(List<Character> json) {
		Stack<Character> stack = new Stack<Character>();
		Set<JSONPair> values = new HashSet<JSONPair>();
		boolean parsed = false;
		while (!parsed) {
			Character next = json.get(0);

			if (next == '{') {
				json.remove(0);
				stack.push('{');
			} else if (next == '}') {
				json.remove(0);
				if (stack.pop() != '{') {
					throw new IllegalArgumentException();
				} else {
					parsed = true;
				}
			} else if (next == ' ' || next == '\r' || next == '\n' || next == ',') {
				json.remove(0);
			} else if (next == '"') {
				values.add(JSONPair.parse(json));
			}
		}
		return new JSONObject(values);
	}

	@Override
	public String toString() {
		String string = "{";
		Iterator<JSONPair> objectIterator = value.iterator();
		while (objectIterator.hasNext()) {
			string += objectIterator.next().toString();
			if (objectIterator.hasNext()) {
				string += ",";
			}
		}
		string += "}";

		return string;
	}

}
