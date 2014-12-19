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

import org.eclipse.andmore.gltrace.state.GLIntegerProperty;
import org.eclipse.andmore.gltrace.state.GLStateType;
import org.eclipse.andmore.gltrace.state.IGLProperty;

/**
 * The {@link TextureUnitPropertyAccessor} provides the ability to access a
 * texture unit property that is indexed based on the run time value of the
 * {@link GLStateType#ACTIVE_TEXTURE_UNIT} property.
 */
public class TextureUnitPropertyAccessor implements IGLPropertyAccessor {
	private final int mContextId;
	private final IGLPropertyAccessor mActiveTextureAccessor;
	private final GLStateType mTargetType;

	public TextureUnitPropertyAccessor(int contextId, GLStateType targetPropertyType) {
		mContextId = contextId;
		mTargetType = targetPropertyType;

		mActiveTextureAccessor = GLPropertyAccessor.makeAccessor(mContextId, GLStateType.TEXTURE_STATE,
				GLStateType.ACTIVE_TEXTURE_UNIT);
	}

	@Override
	public IGLProperty getProperty(IGLProperty state) {
		// first extract the current active texture unit
		IGLProperty activeTextureProperty = mActiveTextureAccessor.getProperty(state);
		if (!(activeTextureProperty instanceof GLIntegerProperty)) {
			return null;
		}
		Integer activeTexture = (Integer) activeTextureProperty.getValue();

		// extract the required property for the current texture unit
		IGLPropertyAccessor targetAccessor = GLPropertyAccessor.makeAccessor(mContextId, GLStateType.TEXTURE_STATE,
				GLStateType.TEXTURE_UNITS, activeTexture, mTargetType);
		return targetAccessor.getProperty(state);
	}

	@Override
	public String getPath() {
		return String.format("TEXTURE_STATE/TEXTURE_UNITS/${activeTextureUnit}/%s", mTargetType);
	}
}
