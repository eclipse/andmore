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
package org.eclipse.andmore.android.emulator.ui.view;

import static org.eclipse.andmore.android.common.log.StudioLogger.debug;

import java.io.File;
import java.util.Collection;

import org.eclipse.andmore.android.emulator.core.exception.SkinException;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.core.skin.IAndroidSkin;
import org.eclipse.andmore.android.emulator.core.skin.SkinFramework;
import org.eclipse.andmore.android.emulator.ui.controls.IAndroidComposite;
import org.eclipse.andmore.android.emulator.ui.controls.RemoteCLIDisplay;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.SWTRemoteDisplay;
import org.eclipse.swt.events.DisposeListener;

/**
 * AndroidViewData: this class is responsible for having the information about
 * Main Display, CLI Display and the composite where these displays will be
 * drawn.
 */
public class AndroidViewData {
	/**
	 * Implementation of the
	 */
	private IAndroidSkin skin;

	/**
	 * Composite that shows the contents of the mobile main display
	 */
	private SWTRemoteDisplay mainDisplay;

	/**
	 * Composite that shows the contents of the mobile CLI display
	 */
	private RemoteCLIDisplay cliDisplay;

	/**
	 * Composite for view's components.
	 */
	private IAndroidComposite composite;

	/**
	 * Dispose listener of this instance
	 */
	private DisposeListener disposeListener;

	/**
	 * Loads the Android emulator skin of the given the AVD/instance
	 * 
	 * @param instance
	 *            whose skin will be loaded
	 * @throws SkinException
	 *             if it is no possible to load the skin
	 */
	public synchronized void loadSkin(IAndroidEmulatorInstance instance) throws SkinException {
		String skinId = instance.getSkinId();
		SkinFramework skinFw = new SkinFramework();
		File skinPath = instance.getSkinPath();
		skin = skinFw.getSkinById(skinId, skinPath);
		Collection<String> layoutNames = skin.getAvailableLayouts();
		String currentLayout = instance.getCurrentLayout();
		if ((currentLayout == null) && (!layoutNames.isEmpty())) {
			String firstLayout = layoutNames.iterator().next();
			instance.setCurrentLayout(firstLayout);
			debug("The skin has multiple layouts. Setting " + firstLayout + " as the current one.");
		}

	}

	/**
	 * Retrieves the loaded IAndroidSkin. Returns null if no skin is loaded.
	 * 
	 * @return
	 */
	public synchronized IAndroidSkin getSkin() {
		return skin;
	}

	/**
	 * Gets the dispose listener
	 * 
	 * @return dispose listener
	 */
	DisposeListener getDisposeListener() {
		return disposeListener;
	}

	/**
	 * Sets the dispose listener
	 * 
	 * @param disposeListener
	 *            dispose listener
	 */
	void setDisposeListener(DisposeListener disposeListener) {
		this.disposeListener = disposeListener;
	}

	/**
	 * Gets Main Display
	 * 
	 * @return main display
	 */
	public SWTRemoteDisplay getMainDisplay() {
		return mainDisplay;
	}

	/**
	 * Sets main Display
	 * 
	 * @param mainDisplay
	 *            main display
	 */
	void setMainDisplay(SWTRemoteDisplay mainDisplay) {
		this.mainDisplay = mainDisplay;
	}

	/**
	 * Gets CLI Display
	 * 
	 * @return CLI display
	 */
	RemoteCLIDisplay getCliDisplay() {
		return cliDisplay;
	}

	/**
	 * Sets CLI Display
	 * 
	 * @param cliDisplay
	 *            CLI display
	 */
	void setCliDisplay(RemoteCLIDisplay cliDisplay) {
		this.cliDisplay = cliDisplay;
	}

	/**
	 * Gets view composite
	 * 
	 * @return composite
	 */
	public IAndroidComposite getComposite() {
		return composite;
	}

	/**
	 * Sets view composite
	 * 
	 * @param composite
	 *            composite
	 */
	void setComposite(IAndroidComposite composite) {
		this.composite = composite;
	}
}
