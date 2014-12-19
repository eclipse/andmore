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
package org.eclipse.andmore.android.emulator.device.definition;

/**
 * This interface contains constants used when managing Android emulator
 * definitions
 * 
 */
public interface IAndroidEmuDefConstants {
	String EMULATOR_DEFINITION_EXTENSION_POINT = "org.eclipse.andmore.android.emulator.androidEmulatorDefinition";

	String ELEMENT_SKIN = "skin";

	String ATT_SKIN_ID = "id";

	String ATT_SKIN_SIZE = "size";

	String SKIN_SIZE_HVGA = "HVGA";

	String SKIN_SIZE_HVGAL = "HVGA-L";

	String SKIN_SIZE_HVGAP = "HVGA-P";

	String SKIN_SIZE_QVGAL = "QVGA-L";

	String SKIN_SIZE_QVGAP = "QVGA-P";
}
