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

/**
 * DESCRIPTION: This class lists all default constants contained into a layout
 * file
 *
 * RESPONSIBILITY: Support on parsing of layout files
 *
 * COLABORATORS: None.
 *
 * USAGE: This class is intended to be used by the LayoutFileParser class only
 */
interface ILayoutConstants {
	String OPEN_BRACKET = "{";

	String CLOSE_BRACKET = "}";

	String MAIN_LEVEL_PARTS = "parts";

	String MAIN_LEVEL_LAYOUTS = "layouts";

	String MAIN_LEVEL_KEYBOARD = "keyboard";

	String MAIN_LEVEL_NETWORK = "network";

	String MAIN_LEVEL_BACKGROUND = "background";

	String MAIN_LEVEL_DISPLAY = "display";

	String MAIN_LEVEL_BUTTON = "button";

	String PART_BUTTONS = "buttons";

	String KEYBOARD_CHARMAP = "charmap";

	String NETWORK_SPEED = "speed";

	String NETWORK_DELAY = "delay";

	String ATTR_WIDTH = "width";

	String ATTR_HEIGHT = "height";

	String ATTR_X = "x";

	String ATTR_Y = "y";

	String ATTR_IMAGE = "image";

	String ATTR_NAME = "name";

	String LAYOUT_COLOR = "color";

	String LAYOUT_EVENT = "event";

	String DPAD_ROTATION = "dpad-rotation";

	String PARTREF_ROTATION = "rotation";
}
