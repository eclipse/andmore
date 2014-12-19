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
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class JSONArray extends JSONValue {
	private final List<JSONValue> value;

	public JSONArray(List<JSONValue> value) {
		this.value = value;
	}

	@Override
	public List<JSONValue> getValue() {
		return value;
	}

	static JSONValue parseValues(List<Character> json) {
		Stack<Character> stack = new Stack<Character>();
		List<JSONValue> values = new ArrayList<JSONValue>();
		boolean parsed = false;
		while (!parsed) {
			Character next = json.get(0);
			if (next == '[') {
				json.remove(0);
				stack.push('[');
			} else if (next == ']') {
				json.remove(0);
				if (stack.pop() != '[') {
					throw new IllegalArgumentException();
				} else {
					parsed = true;
				}
			} else if (next == ' ' || next == '\r' || next == '\n' || next == ',') {
				json.remove(0);
			} else {
				values.add(JSONValueParser.parse(json));
			}
		}
		return new JSONArray(values);
	}

	@Override
	public String toString() {
		String string = "[";
		Iterator<JSONValue> objectIterator = value.iterator();
		while (objectIterator.hasNext()) {
			string += objectIterator.next().toString();
			if (objectIterator.hasNext()) {
				string += ",";
			}
		}
		string += "]";

		return string;
	}
}
