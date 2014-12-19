/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * A PropertyChangeTransform object provides the ability to alter the value of a
 * single GL State variable. An optional predicate provides the ability to
 * perform the change only if the predicate succeeds.
 */
public class PropertyChangeTransform implements IStateTransform {
	private final IGLPropertyAccessor mAccessor;
	private final Object mNewValue;
	private final IPredicate mPredicate;
	private Object mOldValue;

	/**
	 * Construct a state transform that will extract the property using the
	 * accessor, and modify its value to the provided value.
	 */
	PropertyChangeTransform(IGLPropertyAccessor accessor, Object newValue) {
		this(accessor, newValue, null);
	}

	/**
	 * Construct a state transform that will extract the property using the
	 * accessor, check if the predicate function accepts the current value, and
	 * if so modify its value to the provided value.
	 */
	public PropertyChangeTransform(IGLPropertyAccessor accessor, Object newValue, IPredicate predicate) {
		mAccessor = accessor;
		mNewValue = newValue;
		mPredicate = predicate;
		mOldValue = null;
	}

	/** Apply the state transformation on the given OpenGL state. */
	@Override
	public void apply(IGLProperty state) {
		IGLProperty property = mAccessor.getProperty(state);

		assert mOldValue == null : "Transform cannot be applied multiple times";
		if (mPredicate != null) {
			// if predicate is not null, then first check if the current value
			// passes the predicate function.
			if (!mPredicate.apply(property.getValue())) {
				return;
			}
		}

		if (property != null) {
			mOldValue = property.getValue();
			property.setValue(mNewValue);
		} else {
			throw new RuntimeException("No such property: " + mAccessor.getPath());
		}
	}

	/**
	 * Reverses the effect of this state transform. It restores the property's
	 * value to the same state as it was before this transformation was applied.
	 * If this transform was never {@link #apply(IGLProperty)}'ed, then
	 * performing a revert has no effect.
	 */
	@Override
	public void revert(IGLProperty state) {
		if (mOldValue != null) {
			IGLProperty property = mAccessor.getProperty(state);
			property.setValue(mOldValue);
			mOldValue = null;
		}
	}

	/** Gets the property that will be affected by applying this transformation. */
	@Override
	public IGLProperty getChangedProperty(IGLProperty state) {
		if (mPredicate != null) {
			Object value = mOldValue == null ? mNewValue : mOldValue;
			if (!mPredicate.apply(value)) {
				// if the value doesn't match the predicate, then this property
				// is not altered.
				return null;
			}
		}

		return mAccessor.getProperty(state);
	}
}
