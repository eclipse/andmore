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

import org.eclipse.andmore.gltrace.state.GLSparseArrayProperty;
import org.eclipse.andmore.gltrace.state.IGLProperty;

/**
 * A {@link SparseArrayElementAddTransform} changes given state by adding an
 * element to a sparse array, if there is no item with the same key already.
 */
public class SparseArrayElementAddTransform implements IStateTransform {
	private IGLPropertyAccessor mAccessor;
	private int mKey;
	private IGLProperty mOldValue;

	public SparseArrayElementAddTransform(IGLPropertyAccessor accessor, int key) {
		mAccessor = accessor;
		mKey = key;
	}

	@Override
	public void apply(IGLProperty currentState) {
		GLSparseArrayProperty propertyArray = getArray(currentState);
		if (propertyArray != null) {
			mOldValue = propertyArray.getProperty(mKey);
			if (mOldValue == null) {
				// add only if there is no item with this key already present
				propertyArray.add(mKey);
			}
		}
	}

	@Override
	public void revert(IGLProperty currentState) {
		GLSparseArrayProperty propertyArray = getArray(currentState);
		if (propertyArray != null) {
			if (mOldValue == null) {
				// delete only if we actually added this key
				propertyArray.delete(mKey);
			}
		}
	}

	@Override
	public IGLProperty getChangedProperty(IGLProperty currentState) {
		return getArray(currentState);
	}

	private GLSparseArrayProperty getArray(IGLProperty state) {
		IGLProperty p = state;

		if (mAccessor != null) {
			p = mAccessor.getProperty(p);
		}

		if (p instanceof GLSparseArrayProperty) {
			return (GLSparseArrayProperty) p;
		} else {
			return null;
		}
	}
}
