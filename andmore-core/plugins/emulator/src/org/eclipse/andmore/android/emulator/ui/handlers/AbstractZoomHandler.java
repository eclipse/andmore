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

import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.info;

import java.util.Map;

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.ui.view.AbstractAndroidView;
import org.eclipse.andmore.android.emulator.ui.view.AndroidViewData;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

/**
 * DESCRIPTION: This class is a handler for the zoom actions
 *
 * RESPONSIBILITY: Execute the zoom operations on demand
 *
 * COLABORATORS: None.
 *
 * USAGE: This class is intended to be used by Eclipse only
 */
public abstract class AbstractZoomHandler extends AbstractHandler implements IHandlerConstants, IElementUpdater {
	/**
	 * @see org.eclipse.core.commands.IHandler#execute(ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String viewId = event.getParameter(ACTIVE_VIEW_PARAMETER);
		double zoomFactor = getZoomFactor(event.getParameters());

		info("Setting zoom factor for " + viewId + " = " + zoomFactor);

		IAndroidEmulatorInstance instance = AbstractAndroidView.getActiveInstance();
		if (instance != null) {
			IViewPart viewPart = EclipseUtils.getActiveView(viewId);
			if (viewPart instanceof AbstractAndroidView) {
				AbstractAndroidView view = (AbstractAndroidView) viewPart;
				if (view.getZoomFactor(instance) != zoomFactor) {
					view.setZoomFactor(instance, zoomFactor);
					view.updateActiveViewer();
				}
			}
		} else {
			error("The host being presented at the viewer is not available");
			EclipseUtils.showErrorDialog(EmulatorNLS.GEN_Error, EmulatorNLS.EXC_AbstractZoomHandler_InstanceNotFound);
		}
		return null;
	}

	/**
	 * When a different emulator tab have focus, then each of the zoom factor
	 * menu items need to update itself so that the radio selection continues at
	 * the correct place
	 * 
	 * @see org.eclipse.ui.commands.IElementUpdater#updateElement(UIElement,
	 *      Map)
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public void updateElement(UIElement element, Map parameters) {
		boolean checked = false;
		IAndroidEmulatorInstance instance = AbstractAndroidView.getActiveInstance();
		String viewId = (String) parameters.get(ACTIVE_VIEW_PARAMETER);

		if ((instance != null) && (viewId != null)) {
			IViewPart viewPart = EclipseUtils.getActiveView(viewId);
			if (viewPart instanceof AbstractAndroidView) {
				AbstractAndroidView view = (AbstractAndroidView) viewPart;
				AndroidViewData viewData = view.getViewData(instance);

				double actualZoomFactor = view.getZoomFactor(instance);
				if (testZoomFactor(viewData, parameters, actualZoomFactor)) {
					checked = true;
				}
			}
		}

		element.setChecked(checked);
	}

	/**
	 * The zoom actions are enabled when a phone skin is being presented only
	 * 
	 * @see org.eclipse.core.commands.IHandler#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		return AbstractAndroidView.getActiveInstance() != null;
	}

	/**
	 * Retrieves the zoom factor associated to this zoom handler instance
	 * 
	 * @return The zoom factor, as defined in IEmulatorActionConstants class
	 */
	@SuppressWarnings("rawtypes")
	protected abstract double getZoomFactor(Map paramters);

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
	protected abstract boolean testZoomFactor(AndroidViewData viewData, Map parameters, double zoomFactor);
}