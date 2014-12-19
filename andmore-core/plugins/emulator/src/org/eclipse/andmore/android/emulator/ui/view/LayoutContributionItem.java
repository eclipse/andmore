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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.core.skin.IAndroidSkin;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.ui.handlers.IHandlerConstants;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

/**
 * This class builds dynamically the Emulator Layouts submenu from the Android
 * Emulator and Main Display views
 **/
public class LayoutContributionItem extends CompoundContributionItem {
	// Layout dynamic constants
	public static final String ANDROID_VIEW_LAYOUT_DYNAMIC_ID = "androidView.layout.dynamic";

	public static final String MAIN_DISPLAY_VIEW_LAYOUT_DYNAMIC_ID = "mainDisplayView.layout.dynamic";

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.actions.CompoundContributionItem#getContributionItems()
	 */
	@Override
	protected IContributionItem[] getContributionItems() {
		IContributionItem[] itemsArray;

		AbstractAndroidView view = null;
		String viewId = null;
		if (getId().equals(ANDROID_VIEW_LAYOUT_DYNAMIC_ID)) {
			viewId = AndroidView.ANDROID_VIEW_ID;
			view = (AbstractAndroidView) EclipseUtils.getActiveView(viewId);
		} else if (getId().equals(MAIN_DISPLAY_VIEW_LAYOUT_DYNAMIC_ID)) {
			viewId = MainDisplayView.EMULATOR_MAIN_DISPLAY_VIEW_ID;
			view = (AbstractAndroidView) EclipseUtils.getActiveView(viewId);
		}

		if (view != null) {
			IAndroidEmulatorInstance instance = AbstractAndroidView.getActiveInstance();
			IAndroidSkin skin = view.getSkin(instance);
			if (skin != null) {
				Collection<String> layoutNames = skin.getAvailableLayouts();
				itemsArray = new IContributionItem[layoutNames.size()];
				populateContributionList(itemsArray, layoutNames, viewId);
			} else {
				itemsArray = new IContributionItem[1];
				populateWithEmpty(itemsArray);
			}
		} else {
			itemsArray = new IContributionItem[1];
			populateWithEmpty(itemsArray);
		}

		return itemsArray;
	}

	/**
	 * Populates the array with a command item per layout name
	 * 
	 * @param itemsArray
	 *            The array to be populated
	 * @param layoutNames
	 *            The items to be included at the array
	 * @param viewId
	 *            The view that is active at the moment
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void populateContributionList(IContributionItem[] itemsArray, Collection<String> layoutNames, String viewId) {
		int i = 0;
		for (String layoutName : layoutNames) {
			Map params = new HashMap();
			params.put(IHandlerConstants.ACTIVE_VIEW_PARAMETER, viewId);

			String id = EmulatorPlugin.PLUGIN_ID + ".layoutcmd." + layoutName;
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			CommandContributionItemParameter itemParam = new CommandContributionItemParameter(window, id,
					IHandlerConstants.CHANGE_EMULATOR_ORIENTATION_COMMAND, params, null, null, null, layoutName, null,
					null, CommandContributionItem.STYLE_RADIO, null, true);
			itemsArray[i++] = new CommandContributionItem(itemParam);
		}
	}

	/**
	 * Populates the array with a single disabled command, indicating that there
	 * are no layouts to choose
	 * 
	 * @param itemsArray
	 *            The array to be populated
	 */
	@SuppressWarnings("rawtypes")
	private void populateWithEmpty(IContributionItem[] itemsArray) {
		String id = EmulatorPlugin.PLUGIN_ID + ".emptylayout";
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		CommandContributionItemParameter itemParam = new CommandContributionItemParameter(window, id,
				IHandlerConstants.CHANGE_EMULATOR_ORIENTATION_COMMAND, new HashMap(), null, null, null,
				EmulatorNLS.UI_LayoutContributionItem_NoLayoutsAvailable, null, null,
				CommandContributionItem.STYLE_RADIO, null, false);
		itemsArray[0] = new CommandContributionItem(itemParam);
	}
}
