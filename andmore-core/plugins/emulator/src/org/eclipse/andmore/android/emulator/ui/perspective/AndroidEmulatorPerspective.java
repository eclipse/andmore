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
package org.eclipse.andmore.android.emulator.ui.perspective;

import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.warn;

import java.util.Collection;
import java.util.TreeSet;

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.emulator.ui.perspective.extension.AndroidPerspectiveExtensionBean;
import org.eclipse.andmore.android.emulator.ui.perspective.extension.AndroidPerspectiveExtensionReader;
import org.eclipse.andmore.android.emulator.ui.perspective.extension.IAndroidPerspectiveExtensionConstants;
import org.eclipse.andmore.android.emulator.ui.perspective.extension.AndroidPerspectiveExtensionBean.PerspectiveAreas;
import org.eclipse.andmore.android.emulator.ui.view.AndroidView;
import org.eclipse.andmore.android.emulator.ui.view.MainDisplayView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.PartInitException;

/**
 * DESCRIPTION: This class represents the Android Emulator perspective.
 * 
 * RESPONSIBILITY: Create the Android Emulator perspective.
 * 
 * COLABORATORS: None
 * 
 * USAGE: This class is referenced by the plugin.xml file of this plugin.
 */
public class AndroidEmulatorPerspective implements IPerspectiveFactory {
	private static String LAUNCH_COOLBAR_SHORTCUT = "org.eclipse.debug.ui.launchActionSet";

	/**
	 * Creates the initial layout for a page.
	 *
	 * @param layout
	 *            the page layout
	 *
	 * @see IPerspectiveFactory#createInitialLayout(IPageLayout)
	 */
	@Override
	public void createInitialLayout(IPageLayout layout) {
		// ---------------HOW THE PERSPECTIVE IS LAID OUT---------------
		//
		// The Android Perspective will be dynamically populated according to
		// the contributions declared to it through the
		// androidPerspectiveExtension
		// extension point.
		// The perspective consists of three areas:
		// - the area where the Android Emulator View is placed, on the right
		// side
		// - the area where device management related views are placed, on the
		// left side
		// - the area for emulation views, on the middle
		// The areas that are dynamically populated are the two last ones, and
		// they will
		// be referred to as 'dynamic areas' by methods.
		//
		// The first area is filled by the code on this method by itself, and
		// the two other
		// ones are filled depending on what is found for the extension point.
		// The following drawing illustrates the position of each area on the
		// workbench:
		//
		// ____________________________________
		// | | | |
		// | | | |
		// | Dev | | Moto |
		// | Mgt | | magx |
		// | Views | Emu Views Area | Emu |
		// | Area | | View |
		// | | | Area |
		// | | | |
		// |_______|___________________|_______|
		//
		// Device Management views are placed atop of each other on their
		// respective area.
		// Emulation views are laid out depending on the number of views
		// declared. If there
		// is only one emulation view, it is placed occupying the entire
		// emulation views area.
		// If there are two emulation views, they divide the emulation views
		// area in half and
		// occupy the area from bottom to top, as seen on the following drawing:
		//
		// ____________________________________
		// | | | |
		// | | | |
		// | Dev | Emu Views part 2 | Moto |
		// | Mgt | | magx |
		// | Views |___________________| Emu |
		// | Area | | View |
		// | | Emu Views part 1 | Area |
		// | | | |
		// |_______|___________________|_______|
		//
		// If there are three emulation views or more, they divide the emulation
		// views area in
		// three parts, which are occupied from bottom to top, as seen on the
		// following drawing:
		//
		// ____________________________________
		// | | | |
		// | | Emu Views part 3 | |
		// | Dev |___________________| Moto |
		// | Mgt | | magx |
		// | Views | Emu Views part 2 | Emu |
		// | Area |___________________| View |
		// | | | Area |
		// | | Emu Views part 1 | |
		// |_______|___________________|_______|
		//
		// If there are no device management or no emulation views (or both),
		// their folders are
		// always created so that the workbench is always divided into the
		// correct areas for
		// better user experience.
		//
		// -------------------------------------------------------------

		addEmulatorView(layout);
		addRunCoolbar(layout);
		createAndPopulateDynamicAreas(layout);

		// hide the editor area (not necessary on this perspective)
		layout.setEditorAreaVisible(false);
	}

	private void addEmulatorView(IPageLayout layout) {
		// emulator view is a sticky view, no place holder is necessary (only
		// open it)
		try {
			EclipseUtils.showView(AndroidView.ANDROID_VIEW_ID);
		} catch (PartInitException e) {
			error("Unable to open Android Emulator View on Android Emulator Perspective.");
		}

		layout.addShowViewShortcut(AndroidView.ANDROID_VIEW_ID);
		layout.addShowViewShortcut(MainDisplayView.EMULATOR_MAIN_DISPLAY_VIEW_ID);
	}

	private void addRunCoolbar(IPageLayout layout) {
		layout.addActionSet(LAUNCH_COOLBAR_SHORTCUT);
	}

	private void createAndPopulateDynamicAreas(IPageLayout layout) {
		// read the extensions for this perspective, as declared by the
		// androidPerspectiveExtension
		// extension point
		Collection<AndroidPerspectiveExtensionBean> perspectiveExtensionBeans = AndroidPerspectiveExtensionReader
				.readAndroidPerspectiveExtensions();

		// the folder for placing device management related views
		IFolderLayout devMgtArea = createDeviceMgtViewsArea(layout);

		// collection of emuy area view ids for later placement
		// it is alphabetically ordered (so that a group of defined
		// views always open on the same location)
		Collection<String> emuAreaViewIds = new TreeSet<String>();

		for (AndroidPerspectiveExtensionBean extensionBean : perspectiveExtensionBeans) {
			if (PerspectiveAreas.DEVICE_MANAGEMENT_VIEWS_AREA.equals(extensionBean.getArea())) {
				// put dev mgt views atop of each other on the folder
				devMgtArea.addView(extensionBean.getViewId());
			} else if (PerspectiveAreas.EMULATION_VIEWS_AREA.equals(extensionBean.getArea())) {
				// collect emu views for later placement
				emuAreaViewIds.add(extensionBean.getViewId());
			} else {
				// in case of something not expected, log the problem
				warn("View of id " + extensionBean.getViewId() + " could not be added to Android Emulator perspective");
			}
		}

		// the number of emu views, for defining the number of folders necessary
		int numEmuViews = emuAreaViewIds.size();

		// create the emu area folders, which will be at most size 3
		IFolderLayout[] emuAreaFolders = createEmuArea(layout, numEmuViews);

		// place the views on the correct folder by using the leftover of
		// dividing the
		// number of the current view by number of maximum folders (3)
		int i = 0;
		for (String viewId : emuAreaViewIds) {
			emuAreaFolders[i % 3].addView(viewId);
			i++; // next view
		}
	}

	private IFolderLayout createDeviceMgtViewsArea(IPageLayout layout) {
		return layout.createFolder(IAndroidPerspectiveExtensionConstants.ATT_AREA_DEVMGT_VALUE, IPageLayout.LEFT,
				0.30f, IPageLayout.ID_EDITOR_AREA);
	}

	private IFolderLayout[] createEmuArea(IPageLayout layout, int numEmuViews) {
		// at most 3 folders are necessary
		IFolderLayout[] emuAreaFolders = new IFolderLayout[3];

		// the number of divisions the emu views area will be divided into
		int div = (numEmuViews >= 3 ? 3 : (numEmuViews == 2 ? 2 : 1));

		// the ratio for the first folder to be placed (the bottom one, which
		// may turn
		// to be the only one if div is equal to 1)
		float ratio = 1.00f / div;

		// the bottom folder (#1) is always necessary
		// for 3 folders, ratio = 0.33f; for 2 folders, ratio = 0.5f
		emuAreaFolders[0] = layout.createFolder("emuAreaBottom", IPageLayout.BOTTOM, ratio, IPageLayout.ID_EDITOR_AREA);

		// create folder #2 only if necessary
		if (numEmuViews >= 2) {
			// adjust ratio depending on the number of folders in total:
			// for 3 folders, ratio = 0.67f; for 2 folders, ratio = 0.5f
			if (numEmuViews > 2) {
				ratio = 2 * ratio;
			}
			emuAreaFolders[1] = layout.createFolder("emuAreaMiddle", IPageLayout.TOP, ratio, "emuAreaBottom");
		}

		// create folder #3 only if necessary
		if (numEmuViews >= 3) {
			// ratio is always half of folder #2 space
			emuAreaFolders[2] = layout.createFolder("emuAreaTop", IPageLayout.TOP, 0.50f, "emuAreaMiddle");
		}

		return emuAreaFolders;
	}
}
