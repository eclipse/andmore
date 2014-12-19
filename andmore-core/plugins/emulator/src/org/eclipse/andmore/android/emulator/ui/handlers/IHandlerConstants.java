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
package org.eclipse.andmore.android.emulator.ui.handlers;

/**
 * This interface contain constants that are used by the action handlers
 */
public interface IHandlerConstants {
	/**
	 * Constant used in plugin.xml file to identify change orientation command
	 */
	String CHANGE_EMULATOR_ORIENTATION_COMMAND = "org.eclipse.andmore.android.emulator.ui.change.layout";

	/**
	 * Constant used in plugin.xml file to identify change zoom command
	 */
	String CHANGE_EMULATOR_ZOOM_COMMAND = "org.eclipse.andmore.android.emulator.ui.change.zoom";

	/**
	 * Parameter that determines to which view the command will be applied
	 */
	String ACTIVE_VIEW_PARAMETER = "activeViewId";

	/**
	 * Parameter to determines the zoom fact to be set in the
	 */
	String ZOOM_FACTOR_PARAMETER = "zoomFactor";

	/**
	 * Parameter to determine the increment/decrement to be applied in the
	 * current zoonFactor
	 */
	String ZOOM_CHANGE_FACTOR_PARAMETER = "zoomChangeFactor";

	/**
	 * Parameter to determine the emulator display orientation (next, previous,
	 * setlayout) to be set.
	 */
	String EMULATOR_ORIENTATION_PARAMETER = "emulatorOrientation";

	/**
	 * Parameter to determine the layout to be set, if
	 * EMULATOR_ORIENTATION_PARAMETER value is setlayout
	 */
	String LAYOUT_TO_SET_PARAMETER = "layoutToSet";

	// Zoom constants
	double ZOOM_FIT = 0;

	double MINIMUM_ZOOM = 0.25;

	double MAXIMUM_ZOOM = 2.0;

	double DEFAULT_ZOOM = 1.00;

	double STEP_ZOOM = 0.25;
}
