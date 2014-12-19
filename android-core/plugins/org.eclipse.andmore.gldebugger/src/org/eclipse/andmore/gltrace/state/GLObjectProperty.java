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

import com.google.common.base.Joiner;

import java.util.List;

public class GLObjectProperty extends GLAbstractAtomicProperty {
	private final Object mDefaultValue;
	private Object mCurrentValue;

	private static final Joiner JOINER = Joiner.on(", "); //$NON-NLS-1$

	public GLObjectProperty(GLStateType type, Object defaultValue) {
		super(type);

		mDefaultValue = mCurrentValue = defaultValue;
	}

	@Override
	public boolean isDefault() {
		return mDefaultValue != null & mDefaultValue.equals(mCurrentValue);
	}

	@Override
	public void setValue(Object newValue) {
		mCurrentValue = newValue;
	}

	@Override
	public String getStringValue() {
		if (mCurrentValue == null) {
			return "null";
		} else {
			if (mCurrentValue instanceof List<?>) {
				return "[" + JOINER.join((List<?>) mCurrentValue) + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			return mCurrentValue.toString();
		}
	}

	@Override
	public String toString() {
		return getType() + "=" + getStringValue(); //$NON-NLS-1$
	}

	@Override
	public Object getValue() {
		return mCurrentValue;
	}
}
