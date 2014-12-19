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

package org.eclipse.andmore.gltrace.state.transforms;

import org.eclipse.andmore.gltrace.state.IGLProperty;

/**
 * An {@link IGLPropertyAccessor} provides an interface to extract a specific
 * property from a composite property.
 */
public interface IGLPropertyAccessor {
	/** Obtain a specific property from the given state. */
	IGLProperty getProperty(IGLProperty state);

	/** Returns the string representation of this property accessor. */
	String getPath();
}
