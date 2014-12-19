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

import org.eclipse.andmore.gltrace.state.GLListProperty;
import org.eclipse.andmore.gltrace.state.IGLProperty;

/**
 * A {@link ListElementAddTransform} provides the ability to add a new property
 * to a list of properties in the GL State.
 */
public class ListElementAddTransform implements IStateTransform {
	private final IGLPropertyAccessor mAccessor;
	private final IGLProperty mElement;

	public ListElementAddTransform(IGLPropertyAccessor accessor, IGLProperty element) {
		mAccessor = accessor;
		mElement = element;
	}

	@Override
	public void apply(IGLProperty currentState) {
		GLListProperty list = getList(currentState);
		if (list != null) {
			list.add(mElement);
		}
	}

	@Override
	public void revert(IGLProperty currentState) {
		GLListProperty list = getList(currentState);
		if (list != null) {
			list.remove(mElement);
		}
	}

	@Override
	public IGLProperty getChangedProperty(IGLProperty currentState) {
		return getList(currentState);
	}

	private GLListProperty getList(IGLProperty state) {
		IGLProperty p = state;

		if (mAccessor != null) {
			p = mAccessor.getProperty(p);
		}

		if (p instanceof GLListProperty) {
			return (GLListProperty) p;
		} else {
			return null;
		}
	}
}
