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

import java.util.Properties;

import org.eclipse.andmore.android.emulator.core.model.AbstractInputLogic;
import org.eclipse.sequoyah.vnc.protocol.PluginProtocolActionDelegate;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolMessage;

public class VncAndroidInput extends AbstractInputLogic {
	private static final int VNC_KEYEVENT_MESSAGE_CODE = 0x04;

	private static final int VNC_POINTEREVENT_MESSAGE_CODE = 0x05;

	private boolean buttonPressed;

	private void sendAndroidMouseEventMessage(int x, int y) {
		ProtocolMessage message = new ProtocolMessage(VNC_POINTEREVENT_MESSAGE_CODE);
		message.setFieldValue("buttonMask", (buttonPressed ? 1 : 0));
		message.setFieldValue("x-position", x);
		message.setFieldValue("y-position", y);

		try {
			PluginProtocolActionDelegate.sendMessageToServer(getInstance().getProtocolHandle(), message);
		} catch (Exception e) {
			// Do nothing
		}
	}

	@Override
	public void sendKey(int character, int keycode, Properties keyCodeMap) {
		ProtocolMessage message = new ProtocolMessage(VNC_KEYEVENT_MESSAGE_CODE);
		message.setFieldValue("padding", 0);
		message.setFieldValue("downFlag", 1);
		message.setFieldValue("key", keycode);

		try {
			PluginProtocolActionDelegate.sendMessageToServer(getInstance().getProtocolHandle(), message);
		} catch (Exception e) {
			// Do nothing
		}
	}

	@Override
	public void sendClick(int code, boolean pressed) {
		// do nothing
	}

	@Override
	public void sendClick(String code, boolean pressed) {
		// do nothing
	}

	@Override
	public void sendMouseDown(int x, int y) {
		buttonPressed = true;
		sendAndroidMouseEventMessage(x, y);
	}

	@Override
	public void sendMouseMove(int x, int y) {
		sendAndroidMouseEventMessage(x, y);
	}

	@Override
	public void sendMouseUp(int x, int y) {
		buttonPressed = false;
		sendAndroidMouseEventMessage(x, y);
	}

}
