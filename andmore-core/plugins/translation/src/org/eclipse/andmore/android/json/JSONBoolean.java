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

public class JSONBoolean extends JSONValue {
	public final boolean value;

	public JSONBoolean(boolean value) {
		this.value = value;
	}

	@Override
	public Object getValue() {
		return value;
	}

	static JSONValue parseValues(List<Character> json) {
		boolean parsed = false;
		boolean value = false;

		while (!parsed) {
			Character next = json.get(0);

			if (next == 't' && json.get(1) == 'r' && json.get(2) == 'u' && json.get(3) == 'e') {
				for (int i = 0; i < 4; i++) {
					json.remove(0);
				}
				parsed = true;
				value = true;
			} else if (next == 'f' && json.get(1) == 'a' && json.get(2) == 'l' && json.get(3) == 's'
					&& json.get(4) == 'e') {
				for (int i = 0; i < 5; i++) {
					json.remove(0);
				}
				parsed = true;
			} else {
				throw new IllegalArgumentException();
			}

		}
		return new JSONBoolean(value);
	}

	@Override
	public String toString() {
		return Boolean.toString(value);
	}

}
