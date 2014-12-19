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

import org.eclipse.andmore.gltrace.GLEnum;
import org.eclipse.andmore.gltrace.state.GLIntegerProperty;
import org.eclipse.andmore.gltrace.state.GLStateType;
import org.eclipse.andmore.gltrace.state.IGLProperty;

/**
 * An {@link IGLPropertyAccessor} that retrieves the requested property in the
 * currently bound {@link GLEnum#GL_ARRAY_BUFFER} or
 * {@link GLEnum#GL_ELEMENT_ARRAY_BUFFER}.
 */
public class CurrentVboPropertyAccessor implements IGLPropertyAccessor {
	private final int mContextId;
	private final IGLPropertyAccessor mVboBindingAccessor;
	private final GLStateType mVboProperty;

	public CurrentVboPropertyAccessor(int contextId, GLEnum target, GLStateType vboProperty) {
		mContextId = contextId;
		mVboProperty = vboProperty;

		GLStateType vboType;
		if (target == GLEnum.GL_ARRAY_BUFFER) {
			vboType = GLStateType.ARRAY_BUFFER_BINDING;
		} else {
			vboType = GLStateType.ELEMENT_ARRAY_BUFFER_BINDING;
		}

		mVboBindingAccessor = GLPropertyAccessor.makeAccessor(contextId, GLStateType.VERTEX_ARRAY_DATA,
				GLStateType.BUFFER_BINDINGS, vboType);
	}

	@Override
	public IGLProperty getProperty(IGLProperty state) {
		// obtain the current bound buffer
		IGLProperty currentBinding = mVboBindingAccessor.getProperty(state);
		if (!(currentBinding instanceof GLIntegerProperty)) {
			return null;
		}

		Integer buffer = (Integer) currentBinding.getValue();

		return GLPropertyAccessor.makeAccessor(mContextId, GLStateType.VERTEX_ARRAY_DATA, GLStateType.VBO, buffer,
				mVboProperty).getProperty(state);
	}

	@Override
	public String getPath() {
		return String.format("VERTEX_ARRAY_DATA/VBO/${currentBuffer}/%s", mVboProperty);
	}
}
