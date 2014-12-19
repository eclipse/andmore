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
package org.eclipse.andmore.android.emulator.core.devfrm;

import java.util.Collection;

import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;

/**
 * DESCRIPTION: Interface that must be implemented by every device framework
 * that wishes to use the Android Emulator plug-ins
 *
 * RESPONSIBILITY: Provide every information that the Android Emulator plug-ins
 * need to work with the registered framework
 *
 * COLABORATORS: None.
 *
 * USAGE: The class should be used by Eclipse only
 */
public interface IDeviceFrameworkSupport {
	/**
	 * Retrieves a collection of the Android Emulator instances managed by this
	 * device framework
	 * 
	 * @return Collection of the Android Emulator instances
	 */
	Collection<IAndroidEmulatorInstance> getAllInstances();
}
