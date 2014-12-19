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

package org.eclipse.andmore.gltrace.state;

import org.eclipse.andmore.gltrace.GLEnum;

/** Properties that hold a {@link GLEnum}. */
public final class GLEnumProperty extends GLAbstractAtomicProperty {
	private GLEnum mCurrentValue;
	private final GLEnum mDefaultValue;

	public GLEnumProperty(GLStateType name, GLEnum defaultValue) {
		super(name);

		mCurrentValue = mDefaultValue = defaultValue;
	}

	@Override
	public boolean isDefault() {
		return mDefaultValue == mCurrentValue;
	}

	public void setValue(GLEnum newValue) {
		mCurrentValue = newValue;
	}

	@Override
	public String getStringValue() {
		return mCurrentValue.toString();
	}

	@Override
	public String toString() {
		return getType() + "=" + getStringValue(); //$NON-NLS-1$
	}

	@Override
	public void setValue(Object value) {
		if (value instanceof GLEnum) {
			mCurrentValue = (GLEnum) value;
		} else {
			throw new IllegalArgumentException("Attempt to set invalid value for " + getType()); //$NON-NLS-1$
		}
	}

	@Override
	public Object getValue() {
		return mCurrentValue;
	}
}
