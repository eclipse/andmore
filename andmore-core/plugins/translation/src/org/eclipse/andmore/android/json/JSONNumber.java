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

public class JSONNumber extends JSONValue {

	private final int value;

	public JSONNumber(int value) {
		this.value = value;
	}

	@Override
	public Object getValue() {
		return value;
	}

	static JSONValue parseValues(List<Character> json) {
		boolean parsed = false;
		StringBuilder number = new StringBuilder();

		while (!parsed) {
			Character next = json.get(0);
			if (next >= 48 && next <= 57) {
				json.remove(0);
				number.append(next);
			} else {
				parsed = true;
			}
		}

		return new JSONNumber(Integer.parseInt(number.toString()));

	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}

}
