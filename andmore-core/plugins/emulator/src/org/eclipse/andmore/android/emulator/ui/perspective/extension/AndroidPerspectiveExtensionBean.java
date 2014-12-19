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

/**
 * DESCRIPTION: <br>
 * This class is a bean carrying information on a particular declaration of the
 * androidPerspectiveExtension extension point. <br>
 * RESPONSIBILITY: <br>
 * Carry information about a declaration of androidPerspectiveExtension. <br>
 * COLABORATORS: <br>
 * None <br>
 * USAGE: <br>
 * This class should be instantiated only by the reader of the extension points.
 * Clients may only get the values stored through the getter methods.
 */
public class AndroidPerspectiveExtensionBean {
	private String viewId;

	private PerspectiveAreas area;

	/**
	 * Creates a new AndroidPerspectiveExtensionBean object with the given
	 * information.
	 * 
	 * @param viewId
	 *            the id of the view
	 * @param area
	 *            the area to which it should be placed
	 */
	AndroidPerspectiveExtensionBean(String viewId, String area) {
		this.viewId = viewId;

		if (IAndroidPerspectiveExtensionConstants.ATT_AREA_DEVMGT_VALUE.equals(area)) {
			this.area = PerspectiveAreas.DEVICE_MANAGEMENT_VIEWS_AREA;
		} else if (IAndroidPerspectiveExtensionConstants.ATT_AREA_EMU_VALUE.equals(area)) {
			this.area = PerspectiveAreas.EMULATION_VIEWS_AREA;
		} else {
			this.area = PerspectiveAreas.UNKNOWN_AREA;
		}
	}

	/**
	 * Retrieves the id of the view.
	 * 
	 * @return the id of the view
	 */
	public String getViewId() {
		return viewId;
	}

	/**
	 * Retrieves the area to which the view should be added.
	 * 
	 * @return the area on the perspective
	 */
	public PerspectiveAreas getArea() {
		return area;
	}

	/**
	 * 
	 * This enum represents the available areas to which a view can be added to
	 * the Android Emulator Perspective. It has a value for UNKNOWN_AREA for
	 * robustness purpose.
	 *
	 */
	public static enum PerspectiveAreas {
		DEVICE_MANAGEMENT_VIEWS_AREA, EMULATION_VIEWS_AREA, UNKNOWN_AREA
	}
}
