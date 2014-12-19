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

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.ui.view.AbstractAndroidView;
import org.eclipse.andmore.android.emulator.ui.view.AndroidViewData;
import org.eclipse.ui.IViewPart;

/**
 * This class is responsible for increasing the default zoom factor to the
 * current viewer of the Main Display View.
 */
public class ZoomInOutHandler extends AbstractZoomHandler {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.emulator.ui.handlers.AbstractZoomHandler#
	 * getZoomFactor(java.util.Map)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected double getZoomFactor(Map parameters) {
		double zoomFactor = DEFAULT_ZOOM;
		IAndroidEmulatorInstance instance = AbstractAndroidView.getActiveInstance();
		String viewId = (String) parameters.get(ACTIVE_VIEW_PARAMETER);
		String changeFactorString = (String) parameters.get(ZOOM_CHANGE_FACTOR_PARAMETER);

		if ((instance != null) && (viewId != null) && (changeFactorString != null)) {
			IViewPart viewPart = EclipseUtils.getActiveView(viewId);
			if (viewPart instanceof AbstractAndroidView) {
				AbstractAndroidView view = (AbstractAndroidView) viewPart;
				double currentZoomFactor = view.getZoomFactor(instance);

				try {
					double changeZoomFactor = Double.parseDouble(changeFactorString);
					zoomFactor = currentZoomFactor + changeZoomFactor;
				} catch (Exception e) {
					zoomFactor = currentZoomFactor;
				}
			}
		}

		if (zoomFactor < MINIMUM_ZOOM) {
			zoomFactor = MINIMUM_ZOOM;
		} else if (zoomFactor > MAXIMUM_ZOOM) {
			zoomFactor = MAXIMUM_ZOOM;
		}

		return zoomFactor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.emulator.ui.handlers.AbstractZoomHandler#
	 * testZoomFactor
	 * (com.motorola.studio.android.emulator.ui.view.AndroidViewData,
	 * java.util.Map, double)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected boolean testZoomFactor(AndroidViewData viewData, Map parameters, double zoomFactor) {
		// It does not make sense to set as checked any of the UI Elements that
		// use the zoom in/out command
		// Those elements do not represent states, but actions.
		return false;
	}
}
