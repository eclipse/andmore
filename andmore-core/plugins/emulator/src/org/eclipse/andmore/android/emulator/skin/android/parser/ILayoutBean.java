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
package org.eclipse.andmore.android.emulator.skin.android.parser;

public interface ILayoutBean {
	/**
	 * Sets the key/value pair at the bean, according to the rules defined by
	 * each bean
	 * 
	 * @param key
	 *            The key that represents the item to be set
	 * @param value
	 *            The value to be set to the item represented by key
	 */
	void setKeyValue(String key, String value);
}
