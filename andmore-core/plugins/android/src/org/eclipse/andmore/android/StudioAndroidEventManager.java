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
package org.eclipse.andmore.android;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IClientChangeListener;
import com.android.ddmlib.AndroidDebugBridge.IDebugBridgeChangeListener;

public class StudioAndroidEventManager {
	private static final HashMap<EventType, List<DdmsRunnable>> listeners = new HashMap<EventType, List<DdmsRunnable>>();

	public enum EventType {
		DEVICE_CONNECTED, DEVICE_DISCONNECTED, PACKAGE_INSTALLED, PACKAGE_UNINSTALLED, LANGUAGE_CHANGED
	};

	public static void addEventListener(EventType type, DdmsRunnable listener) {
		synchronized (listeners) {
			List<DdmsRunnable> typeListeners = listeners.get(type);
			if (typeListeners == null) {
				typeListeners = new LinkedList<DdmsRunnable>();
				listeners.put(type, typeListeners);
			}
			typeListeners.add(listener);
		}
	}

	public static void removeEventListener(EventType type, DdmsRunnable listener) {
		synchronized (listeners) {
			List<DdmsRunnable> typeListeners = listeners.get(type);
			if (typeListeners == null) {
				typeListeners = new LinkedList<DdmsRunnable>();
				listeners.put(type, typeListeners);
			}
			typeListeners.remove(listener);
		}
	}

	/**
	 * Adds a IClientChangeListener asynchronously
	 * 
	 * @param listener
	 *            The listener to add
	 */
	public static void asyncAddClientChangeListener(final IClientChangeListener listener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				AndroidDebugBridge.addClientChangeListener(listener);
			}
		}).start();
	}

	/**
	 * Removes a IClientChangeListener asynchronously
	 * 
	 * @param listener
	 *            The listener to remove
	 */
	public static void asyncRemoveClientChangeListener(final IClientChangeListener listener) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				AndroidDebugBridge.removeClientChangeListener(listener);

			}
		}).start();
	}

	/**
	 * Adds a IDebugBridgeChangeListener asynchronously
	 * 
	 * @param listener
	 *            The listener to add
	 */
	public static void asyncAddDebugBridgeChangeListener(final IDebugBridgeChangeListener listener) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				AndroidDebugBridge.addDebugBridgeChangeListener(listener);

			}
		}).start();
	}

	/**
	 * Removes a IDebugBridgeChangeListener asynchronously
	 * 
	 * @param listener
	 *            The listener to remove
	 */
	public static void asyncRemoveDebugBridgeChangeListener(final IDebugBridgeChangeListener listener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				AndroidDebugBridge.removeDebugBridgeChangeListener(listener);

			}
		}).start();
	}

	/**
	 * Adds a IDeviceChangeListener asynchronously
	 * 
	 * @param listener
	 *            The listener to add
	 */
	public static void asyncAddDeviceChangeListeners(final DdmsRunnable connectedListener,
			final DdmsRunnable disconnectedListener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (connectedListener != null) {
					addEventListener(EventType.DEVICE_CONNECTED, connectedListener);
				}
				if (disconnectedListener != null) {
					addEventListener(EventType.DEVICE_DISCONNECTED, disconnectedListener);
				}
			}
		}).start();
	}

	/**
	 * Removes a IDeviceChangeListener asynchronously
	 * 
	 * @param listener
	 *            The listener to remove
	 */
	public static void asyncRemoveDeviceChangeListeners(final DdmsRunnable connectedListener,
			final DdmsRunnable disconnectedListener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (connectedListener != null) {
					removeEventListener(EventType.DEVICE_CONNECTED, connectedListener);
				}
				if (disconnectedListener != null) {
					removeEventListener(EventType.DEVICE_DISCONNECTED, disconnectedListener);
				}
			}
		}).start();
	}

	public static void fireEvent(EventType type, final String serialNumber) {
		synchronized (listeners) {
			if (listeners.get(type) != null) {
				for (final DdmsRunnable runnable : listeners.get(type)) {
					SafeRunner.run(new SafeRunnable() {
						@Override
						public void run() throws Exception {
							runnable.run(serialNumber);
						}
					});
				}
			}
		}
	}
}
