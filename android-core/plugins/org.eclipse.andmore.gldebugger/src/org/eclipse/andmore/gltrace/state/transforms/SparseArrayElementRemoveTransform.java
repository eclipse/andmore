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
 * A {@link SparseArrayElementRemoveTransform} changes given state by removing
 * an element from a sparse array.
 */
public class SparseArrayElementRemoveTransform implements IStateTransform {
	private final SparseArrayElementAddTransform mAddTransform;

	public SparseArrayElementRemoveTransform(IGLPropertyAccessor accessor, int key) {
		mAddTransform = new SparseArrayElementAddTransform(accessor, key);
	}

	@Override
	public void apply(IGLProperty currentState) {
		// applying a RemoveTransform is the same as reverting an AddTransform.
		mAddTransform.revert(currentState);
	}

	@Override
	public void revert(IGLProperty currentState) {
		// reverting a RemoveTransform is the same as applying an AddTransform.
		mAddTransform.apply(currentState);
	}

	@Override
	public IGLProperty getChangedProperty(IGLProperty currentState) {
		return mAddTransform.getChangedProperty(currentState);
	}

}
