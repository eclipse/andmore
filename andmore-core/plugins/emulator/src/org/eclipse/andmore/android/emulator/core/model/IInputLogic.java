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
package org.eclipse.andmore.android.emulator.core.model;

import java.util.Properties;

public interface IInputLogic {
	void init(IAndroidEmulatorInstance instance);

	void dispose();

	/**
	 * Send a key press event
	 * 
	 * @param character
	 *            the correspondent character
	 * @param keycode
	 *            the keycode of the key pressed
	 * @param keyCodeMap
	 *            the skin keycode map
	 */
	void sendKey(int character, int keycode, Properties keyCodeMap);

	/**
	 * Send a click event
	 * 
	 * @param code
	 *            the code to be sent
	 * @param pressed
	 *            key pressed - yes or no
	 */
	void sendClick(int code, boolean pressed);

	/**
	 * Send a click event
	 * 
	 * @param code
	 *            the code to be sent
	 * @param pressed
	 *            key pressed - yes or no
	 */
	void sendClick(String code, boolean pressed);

	void sendMouseUp(int x, int y);

	void sendMouseDown(int x, int y);

	void sendMouseMove(int x, int y);
}
