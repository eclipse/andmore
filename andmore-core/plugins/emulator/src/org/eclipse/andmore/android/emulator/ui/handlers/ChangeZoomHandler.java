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

import java.util.Map;

import org.eclipse.andmore.android.emulator.ui.controls.IAndroidComposite;
import org.eclipse.andmore.android.emulator.ui.view.AndroidViewData;

public class ChangeZoomHandler extends AbstractZoomHandler {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.emulator.ui.handlers.AbstractZoomHandler#
	 * getZoomFactor(java.util.Map)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected double getZoomFactor(Map parameter) {
		double zoomFactor = DEFAULT_ZOOM;
		String factorString = (String) parameter.get(ZOOM_FACTOR_PARAMETER);
		try {
			zoomFactor = Double.parseDouble(factorString);
		} catch (Exception e) {
			// Do nothing
			// The parameter can always be parsed
		}
		return zoomFactor;
	}

	/**
	 * Tests if the current zoom factor is the one handled by this zoom handler
	 * 
	 * @param zoomFactor
	 *            The active instance current zoom factor
	 * 
	 * @return True if this handler handles the current zoom factor; false
	 *         otherwise
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected boolean testZoomFactor(AndroidViewData viewData, Map parameters, double zoomFactor) {
		boolean testResult = false;
		double expectedZoomFactor = getZoomFactor(parameters);
		if (expectedZoomFactor == zoomFactor) {
			testResult = true;
		}

		if (expectedZoomFactor == ZOOM_FIT) {
			if (viewData != null) {
				IAndroidComposite composite = viewData.getComposite();

				if (composite.isFitToWindowSelected()) {
					testResult = true;
				}
			}
		}

		return testResult;
	}
}
