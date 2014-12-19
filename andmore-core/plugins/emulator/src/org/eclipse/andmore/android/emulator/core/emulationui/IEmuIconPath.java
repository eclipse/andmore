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
package org.eclipse.andmore.android.emulator.core.emulationui;

/**
 * This interface contains the paths to the icons that are used by the emulation
 * views
 */
public interface IEmuIconPath {
	// Emulation root node icon
	String EMULATOR_ICON_PATH = "resource/emulator.png";

	// Emulation intermediate nodes icons
	String SENT_TO_ICON_PATH = "resource/sentbyemulator.png";

	String RECEIVE_FROM_ICON_PATH = "resource/receivebyemulator.png";
}
