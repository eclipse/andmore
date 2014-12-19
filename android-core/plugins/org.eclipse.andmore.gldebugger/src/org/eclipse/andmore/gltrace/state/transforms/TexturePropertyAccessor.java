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
 * The {@link TexturePropertyAccessor} provides the ability to access a texture
 * property. Texture properties are accessed by first identifying the active
 * texture unit ({@link GLStateType#ACTIVE_TEXTURE_UNIT}), and then identifying
 * the texture that is bound to that unit.
 */
public class TexturePropertyAccessor implements IGLPropertyAccessor {
	private final int mContextId;
	private final GLStateType mTargetUnitType;
	private final int mMipmapLevel;
	private final GLStateType mTextureType;
	private TextureUnitPropertyAccessor mTextureUnitPropertyAccessor;

	public TexturePropertyAccessor(int contextId, GLStateType textureUnitTarget, int level,
			GLStateType textureTargetName) {
		mContextId = contextId;
		mTargetUnitType = textureUnitTarget;
		mMipmapLevel = level;
		mTextureType = textureTargetName;
		mTextureUnitPropertyAccessor = new TextureUnitPropertyAccessor(mContextId, mTargetUnitType);
	}

	public TexturePropertyAccessor(int contextId, GLStateType textureUnitTarget, GLStateType textureTargetName) {
		this(contextId, textureUnitTarget, -1, textureTargetName);
	}

	@Override
	public IGLProperty getProperty(IGLProperty state) {
		// identify the texture that is bound in the current active texture unit
		IGLProperty targetTexture = mTextureUnitPropertyAccessor.getProperty(state);
		if (!(targetTexture instanceof GLIntegerProperty)) {
			return null;
		}
		Integer textureId = (Integer) targetTexture.getValue();

		// now extract the required property from the selected texture
		IGLPropertyAccessor textureAccessor;
		if (mMipmapLevel >= 0) {
			textureAccessor = GLPropertyAccessor.makeAccessor(mContextId, GLStateType.TEXTURE_STATE,
					GLStateType.TEXTURES, textureId, GLStateType.TEXTURE_MIPMAPS, Integer.valueOf(mMipmapLevel),
					mTextureType);
		} else {
			textureAccessor = GLPropertyAccessor.makeAccessor(mContextId, GLStateType.TEXTURE_STATE,
					GLStateType.TEXTURES, textureId, mTextureType);
		}

		return textureAccessor.getProperty(state);
	}

	@Override
	public String getPath() {
		return String.format("TEXTURE_STATE/TEXTURES/${activeTexture}/%s", mTextureType);
	}

}
