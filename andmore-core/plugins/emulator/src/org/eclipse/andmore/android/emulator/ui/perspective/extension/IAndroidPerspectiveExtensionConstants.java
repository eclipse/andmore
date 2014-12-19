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
package org.eclipse.andmore.android.emulator.ui.perspective.extension;

import org.eclipse.andmore.android.emulator.EmulatorPlugin;

/**
 * 
 * This interface provides the String values used by the
 * androidPerspectiveExtension extension point for purpose of reading the
 * declared extensions.
 *
 */
public interface IAndroidPerspectiveExtensionConstants {
	String EXTENSION_POINT_ID = EmulatorPlugin.PLUGIN_ID + ".androidPerspectiveExtension";

	String ELEMENT_VIEW = "view";

	String ATT_ID = "id";

	String ATT_AREA = "area";

	String ATT_AREA_DEVMGT_VALUE = "devicemanagementviews";

	String ATT_AREA_EMU_VALUE = "emulationviews";
}
