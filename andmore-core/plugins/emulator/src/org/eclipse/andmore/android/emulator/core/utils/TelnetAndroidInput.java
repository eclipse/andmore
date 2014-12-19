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
package org.eclipse.andmore.android.emulator.core.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.emulator.core.model.AbstractInputLogic;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.utilities.TelnetFrameworkAndroid;

/**
 * This class is responsible for sending input events to the Android Emulator
 */
@SuppressWarnings("serial")
public class TelnetAndroidInput extends AbstractInputLogic {
	/*
	 * Event types
	 */
	public static final String EV_TYPE_SYN = "EV_SYN";

	public static final String EV_TYPE_KEY = "EV_KEY";

	public static final String EV_TYPE_ABS = "EV_ABS";

	public static final String EV_TYPE_SW = "EV_SW";

	public static final int EV_SW_LID = 0x00;

	public static final String EV_SYN_REPORT = "0";

	public static final String EV_ABS_X = "ABS_X";

	public static final String EV_ABS_Y = "ABS_Y";

	private static final String EV_TOUCH = "BTN_TOUCH";

	private final Map<Integer, String> SPECIAL_KEY_MAPPING = new HashMap<Integer, String>() {
		{
			put(16777219, "KEY_LEFT"); // left
			put(16777217, "KEY_UP"); // top
			put(16777220, "KEY_RIGHT"); // right
			put(16777218, "KEY_DOWN"); // bottom
		}
	};

	/*
	 * Event types
	 */
	public static final int OPHONE_EV_TYPE_SYN = 0x00;

	public static final int OPHONE_EV_TYPE_KEY = 0x01;

	public static final int OPHONE_EV_TYPE_ABS = 0x03;

	public static final int OPHONE_EV_TYPE_SW = 0x05;

	public static final int OPHONE_EV_SW_LID = 0x00;

	public static final int OPHONE_EV_SYN_REPORT = 0x00;

	public static final int OPHONE_EV_ABS_X = 0x00;

	public static final int OPHONE_EV_ABS_Y = 0x01;

	private final Map<Integer, Integer> OPHONE_SPECIAL_KEY_MAPPING = new HashMap<Integer, Integer>() {
		{
			put(16777219, 0x069); // left
			put(16777217, 0x067); // top
			put(16777220, 0x06a); // right
			put(16777218, 0x06c); // bottom

		}
	};

	private static final int OPHONE_KEY_BACKSPACE = 0x00e;

	private static final int OPHONE_KEY_SPACE = 0x039;

	private static final int OPHONE_KEY_ENTER = 0x01c;

	private static final int OPHONE_EV_TOUCH = 0x14A;

	// mouse position x
	private int oldX;

	// mouse position y
	private int oldY;

	// telnet connection
	private final TelnetFrameworkAndroid telnet = new TelnetFrameworkAndroid();

	/**
	 * Open the Telnet connection and initialize the communication
	 * 
	 * @see 
	 *      org.eclipse.andmore.android.emulator.core.model.AbstractInputLogic#init
	 *      (
	 *      "org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance
	 *      )
	 */
	@Override
	public void init(IAndroidEmulatorInstance instance) {
		super.init(instance);

		String deviceSerial = instance.getInstanceIdentifier();

		String serial = deviceSerial.substring(deviceSerial.length() - 4, deviceSerial.length());
		try {
			telnet.connect("localhost", Integer.parseInt(serial));
		} catch (IOException e) {
			// Do nothing
		}
	}

	/**
	 * Close Telnet connection
	 * 
	 * @see org.eclipse.andmore.android.emulator.core.model.AbstractInputLogic#dispose()
	 */
	@Override
	public void dispose() {
		try {
			telnet.disconnect();
		} catch (IOException e) {
			// Do nothing
		}
	}

	/**
	 * Send mouse down event
	 * 
	 * @see org.eclipse.andmore.android.emulator.core.model.IInputLogic#sendMouseDown(int,
	 *      int)
	 */
	@Override
	public void sendMouseDown(int x, int y) {
		sendAndroidEvent(EV_TYPE_ABS, EV_ABS_X, x);
		sendAndroidEvent(EV_TYPE_ABS, EV_ABS_Y, y);
		if (SdkUtils.isOphoneSDK()) {
			sendAndroidEvent(EV_TYPE_KEY, OPHONE_EV_TOUCH, true);
			sendAndroidEvent(EV_TYPE_SYN, OPHONE_EV_SYN_REPORT, 0);
		} else {
			sendAndroidEvent(EV_TYPE_KEY, EV_TOUCH, true);
			sendAndroidEvent(EV_TYPE_SYN, EV_SYN_REPORT, 0);
		}

		oldX = x;
		oldY = y;
	}

	/**
	 * Send mouse up event
	 * 
	 * @see org.eclipse.andmore.android.emulator.core.model.IInputLogic#sendMouseUp(int,
	 *      int)
	 */
	@Override
	public void sendMouseUp(int x, int y) {
		if (SdkUtils.isOphoneSDK()) {
			sendAndroidEvent(EV_TYPE_KEY, OPHONE_EV_TOUCH, false);
			sendAndroidEvent(EV_TYPE_SYN, OPHONE_EV_SYN_REPORT, 0);
		} else {
			sendAndroidEvent(EV_TYPE_KEY, EV_TOUCH, false);
			sendAndroidEvent(EV_TYPE_SYN, EV_SYN_REPORT, 0);
		}
	}

	/**
	 * Send mouse move event
	 * 
	 * @see org.eclipse.andmore.android.emulator.core.model.IInputLogic#sendMouseMove(int,
	 *      int)
	 */
	@Override
	public void sendMouseMove(int x, int y) {
		if (oldX != x) {
			sendAndroidEvent(EV_TYPE_ABS, EV_ABS_X, x);
			oldX = x;
		}
		if (oldY != y) {
			sendAndroidEvent(EV_TYPE_ABS, EV_ABS_Y, y);
			oldY = y;
		}
		if (SdkUtils.isOphoneSDK()) {
			sendAndroidEvent(EV_TYPE_SYN, OPHONE_EV_SYN_REPORT, 0);
		} else {
			sendAndroidEvent(EV_TYPE_SYN, EV_SYN_REPORT, 0);
		}
	}

	/**
	 * Send a generic event to the emulator
	 * 
	 * @param type
	 *            event type
	 * @param keysym
	 *            event definition
	 * @param pressed
	 *            key pressed - yes or no
	 */
	public void sendAndroidEvent(String type, String keysym, boolean pressed) {
		sendAndroidEvent(type, keysym, pressed ? 1 : 0);
	}

	public void sendAndroidEvent(String type, int keysym, boolean pressed) {
		sendAndroidEvent(type, keysym, pressed ? 1 : 0);
	}

	private void sendAndroidEvent(String type, int keysym, int i) {
		try {
			telnet.write("event send " + type + ":" + keysym + ":" + i, null);
		} catch (IOException e) {
			StudioLogger.error("Failed to send generic event to Emulator");
		}
	}

	/**
	 * Send a complete event to the Emulator Some events, like a key press, need
	 * two events (key pressed/released) to be executed. This method send both
	 * in order to execute the event.
	 * 
	 * @param type
	 *            event type
	 * @param keysym
	 *            event definition
	 */
	public void sendAndroidEvent(String type, String keysym) {
		sendAndroidEvent(type, keysym, true);
		sendAndroidEvent(type, keysym, false);
	}

	/**
	 * Send a complete event to the Emulator Some events, like a key press, need
	 * two events (key pressed/released) to be executed. This method send both
	 * in order to execute the event.
	 * 
	 * @param type
	 *            event type
	 * @param keysym
	 *            event definition
	 */
	public void sendAndroidEvent(String type, int keysym) {
		sendAndroidEvent(type, keysym, true);
		sendAndroidEvent(type, keysym, false);
	}

	/**
	 * Send a generic event to the emulator
	 * 
	 * @param type
	 *            event type
	 * @param keysym
	 *            event definition
	 * @param value
	 *            parameter
	 */
	private void sendAndroidEvent(String type, String keysym, int value) {
		try {
			telnet.write("event send " + type + ":" + keysym + ":" + value, null);
		} catch (IOException e) {
			StudioLogger.error("Failed to send generic event to Emulator");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.emulator.core.model.IInputLogic#sendKey(int,
	 * int)
	 */
	@Override
	public void sendKey(int character, int keycode, Properties keyCodeMap) {
		/*
		 * Check if it's a character
		 */
		if (character > 0) {
			String text = String.valueOf((char) character);
			// check it's a blank space
			if (text.equals(" ")) {
				if (SdkUtils.isOphoneSDK()) {
					sendAndroidEvent(EV_TYPE_KEY, OPHONE_KEY_SPACE);
				} else {
					sendAndroidEvent(EV_TYPE_KEY, "KEY_SPACE");
				}
			}
			// check if it's a backspace
			else if (text.equals("\b")) {
				if (SdkUtils.isOphoneSDK()) {
					sendAndroidEvent(EV_TYPE_KEY, OPHONE_KEY_BACKSPACE);
				} else {
					sendAndroidEvent(EV_TYPE_KEY, "KEY_BACKSPACE");
				}
			}
			// check if it's an enter
			else if (text.equals("\r")) {
				if (SdkUtils.isOphoneSDK()) {
					sendAndroidEvent(EV_TYPE_KEY, OPHONE_KEY_ENTER);
				} else {
					sendAndroidEvent(EV_TYPE_KEY, "KEY_ENTER");
				}
			} else {
				if (keyCodeMap != null) {
					String keyCode = keyCodeMap.getProperty(text.toUpperCase().trim());
					if (keyCode != null) {
						sendAndroidEvent(EV_TYPE_KEY, keyCode);
					}
				}
			}
		} else {
			if (!SdkUtils.isOphoneSDK()) {
				String keycode_str = null;
				if (SPECIAL_KEY_MAPPING.containsKey(keycode)) {
					keycode_str = SPECIAL_KEY_MAPPING.get(keycode);
				}
				if (keycode_str != null) {
					sendAndroidEvent(EV_TYPE_KEY, keycode_str);
				}
			} else {
				if (OPHONE_SPECIAL_KEY_MAPPING.containsKey(keycode)) {
					keycode = OPHONE_SPECIAL_KEY_MAPPING.get(keycode);
				}
				if (keycode != -1) {
					sendAndroidEvent(EV_TYPE_KEY, keycode);
				}
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.emulator.core.model.IInputLogic#sendClick
	 * (int, boolean)
	 */
	@Override
	public void sendClick(String code, boolean pressed) {
		sendAndroidEvent(EV_TYPE_KEY, code, pressed ? 1 : 0);
	}

	@Override
	public void sendClick(int code, boolean pressed) {
		sendAndroidEvent(EV_TYPE_KEY, code, pressed ? 1 : 0);
	}

	public void sendWindowScale(double zoomFactor) {
		try {
			telnet.write("window scale " + zoomFactor, null);
		} catch (IOException e) {
			StudioLogger.error("Failed to send window scale to Emulator");
		}

	}

}
