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
package org.eclipse.andmore.android.emulator.core.skin;

/**
 * Interface that contains the tags used into skin.xml and key.xml files
 */
public interface ISkinKeyXmlTags {
	String SKIN_CONFIG = "skinConfig";

	String SKIN_DOUBLE_SCREEN = "doubleScreen";

	String SKIN_DOUBLE_VIEW = "doubleView";

	String SKIN_KEYPAD_AREAS_NUMBER = "keypadAreasNumber";

	String SKIN_VIEW_ZOOM_SCALE = "viewZoomScale";

	String SKIN_MEMORY_SIZE = "memorySize";

	String SKIN_REFRESH_RATE = "refreshRate";

	String SKIN_INTERNAL_VIEW_X = "internalViewX";

	String SKIN_INTERNAL_VIEW_Y = "internalViewY";

	String SKIN_INTERNAL_VIEW_WIDTH = "internalViewWidth";

	String SKIN_INTERNAL_VIEW_HEIGHT = "internalViewHeight";

	String SKIN_INTERNAL_VIEW_DEPTH = "internalViewDepth";

	String SKIN_OPEN_EXTERNAL_VIEW_X = "openExternalViewX";

	String SKIN_OPEN_EXTERNAL_VIEW_Y = "openExternalViewY";

	String SKIN_OPEN_EXTERNAL_VIEW_WIDTH = "openExternalViewWidth";

	String SKIN_OPEN_EXTERNAL_VIEW_HEIGHT = "openExternalViewHeight";

	String SKIN_OPEN_EXTERNAL_COLOR_DEPTH = "openExternalColorDepth";

	String SKIN_EMBEDDED_VIEW_SCALE = "embeddedViewScale";

	String SKIN_EXTERNAL_VIEW_X = "externalViewX";

	String SKIN_EXTERNAL_VIEW_Y = "externalViewY";

	String SKIN_EXTERNAL_VIEW_WIDTH = "externalViewWidth";

	String SKIN_EXTERNAL_VIEW_HEIGHT = "externalViewHeight";

	String SKIN_EXTERNAL_VIEW_DEPTH = "externalViewDepth";

	String KEY_SKIN_KEY_INFO = "skinkeyinfo";

	String KEY_DATA = "keyData";

	String KEY_NAME = "name";

	String KEY_CODE = "keycode";

	String KEY_TOOLTIP = "hint";

	String KEY_START_X = "startx";

	String KEY_START_Y = "starty";

	String KEY_END_X = "endx";

	String KEY_END_Y = "endy";

	String KEY_MORPHING_MODE = "morphingMode";

	String KEY_FLIP_ENABLED = "flipenabled";
}
