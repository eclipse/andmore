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
package org.eclipse.andmore.android.emulator.core.utils;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.andmore.android.emulator.core.devfrm.DeviceFrameworkManager;
import org.eclipse.andmore.android.emulator.core.exception.InstanceNotFoundException;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.core.model.IEmulatorView;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * DESCRIPTION: Utilities for Android Emulator restricted use
 * 
 * RESPONSIBILITY: Provide common utility methods that can be used by any
 * Android Emulator plugin.
 *
 * COLABORATORS: None
 *
 * USAGE: This class should not be instantiated and its methods should be called
 * statically.
 */
public class EmulatorCoreUtils {
	/**
	 * Retrieves all views that implement IEmulatorView interface
	 * 
	 * @return All views that implement IEmulatorView interface
	 */
	public static Collection<IEmulatorView> getAllAndroidViews() {
		final Collection<IEmulatorView> allViews = new HashSet<IEmulatorView>();

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				if (activeWindow != null) {
					IWorkbenchPage[] activePages = activeWindow.getPages();
					if (activePages != null) {
						for (IWorkbenchPage activePage : activePages) {
							if (activePage != null) {
								IViewReference[] allReferences = activePage.getViewReferences();
								for (IViewReference ref : allReferences) {
									IViewPart aView = ref.getView(false);
									if ((aView != null) && (aView instanceof IEmulatorView)) {
										allViews.add((IEmulatorView) aView);
									}
								}
							}
						}
					}
				}
			}
		});

		return allViews;
	}

	/**
	 * Refresh all Emulator views
	 */
	public static void refreshEmulatorViews() {
		Collection<IEmulatorView> allViews = EmulatorCoreUtils.getAllAndroidViews();
		for (IEmulatorView view : allViews) {
			view.refreshView();
		}
	}

	/**
	 * Retrieves a Android Emulator instance mapped by the provided IP address
	 * 
	 * @param identifier
	 *            The IP address associated with the desired instance
	 * 
	 * @return The Android Emulator instance that is working on the given
	 *         address
	 * 
	 * @throws InstanceNotFoundException
	 *             If the instance is not found at device framework
	 */
	public static IAndroidEmulatorInstance getAndroidInstanceByIdentifier(String identifier)
			throws InstanceNotFoundException {
		IAndroidEmulatorInstance desiredDevice = null;

		Collection<IAndroidEmulatorInstance> instanceList = DeviceFrameworkManager.getInstance().getAllInstances();
		for (IAndroidEmulatorInstance instance : instanceList) {
			String instanceIdentifier = instance.getInstanceIdentifier();
			if ((instanceIdentifier != null) && instanceIdentifier.equals(identifier)) {
				desiredDevice = instance;
				break;
			}
		}

		if (desiredDevice == null) {
			throw new InstanceNotFoundException();
		}

		return desiredDevice;
	}

	/**
	 * Retrieves a Android Emulator instance mapped by the provided protocol
	 * handle object
	 * 
	 * @param handle
	 *            The protocol handle object associated with the desired
	 *            instance
	 * 
	 * @return The Android Emulator instance that is working with the given
	 *         protocol handle object
	 * 
	 * @throws InstanceNotFoundException
	 *             If the instance is not found at device framework
	 */
	public static IAndroidEmulatorInstance getAndroidInstanceByHandle(ProtocolHandle handle)
			throws InstanceNotFoundException {
		IAndroidEmulatorInstance desiredDevice = null;
		Collection<IAndroidEmulatorInstance> instanceList = DeviceFrameworkManager.getInstance().getAllInstances();
		for (IAndroidEmulatorInstance instance : instanceList) {
			ProtocolHandle instanceHandle = instance.getProtocolHandle();
			if ((instanceHandle != null) && instanceHandle.equals(handle)) {
				desiredDevice = instance;
				break;
			}
		}

		if (desiredDevice == null) {
			throw new InstanceNotFoundException();
		}

		return desiredDevice;
	}
}
