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

/**
 * Abstract implementation of {@link IGLProperty}. This provides the basics that
 * can be used by leaf level properties.
 */
public abstract class GLAbstractAtomicProperty implements IGLProperty {
	private final GLStateType mType;
	private IGLProperty mParent;

	public GLAbstractAtomicProperty(GLStateType type) {
		mType = type;
	}

	@Override
	public GLStateType getType() {
		return mType;
	}

	@Override
	public IGLProperty getParent() {
		return mParent;
	}

	@Override
	public void setParent(IGLProperty parent) {
		mParent = parent;
	}

	@Override
	public boolean isComposite() {
		return false;
	}

	@Override
	public IGLProperty clone() {
		try {
			return (IGLProperty) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	@Override
	public void prettyPrint(StatePrettyPrinter pp) {
		pp.prettyPrint(mType, getStringValue());
	}
}
