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

package org.eclipse.andmore.gltrace.state;

/** Properties that hold float values. */
public class GLFloatProperty extends GLAbstractAtomicProperty {
	private final Float mDefaultValue;
	private Float mCurrentValue;

	public GLFloatProperty(GLStateType name, Float defaultValue) {
		super(name);

		mDefaultValue = mCurrentValue = defaultValue;
	}

	@Override
	public boolean isDefault() {
		return Math.abs(mCurrentValue - mDefaultValue) < 0.000000001;
	}

	public void setValue(Float newValue) {
		mCurrentValue = newValue;
	}

	@Override
	public void setValue(Object value) {
		if (value instanceof Float) {
			mCurrentValue = (Float) value;
		} else {
			throw new IllegalArgumentException("Attempt to set non float value for " + getType());
		}
	}

	@Override
	public Object getValue() {
		return mCurrentValue;
	}

	@Override
	public String getStringValue() {
		return mCurrentValue.toString();
	}

	@Override
	public String toString() {
		return getType() + "=" + getStringValue(); //$NON-NLS-1$
	}
}
