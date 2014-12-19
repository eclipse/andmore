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

package org.eclipse.andmore.gltrace.views.detail;

import org.eclipse.andmore.gltrace.state.GLCompositeProperty;
import org.eclipse.andmore.gltrace.state.GLSparseArrayProperty;
import org.eclipse.andmore.gltrace.state.GLStateType;
import org.eclipse.andmore.gltrace.state.GLStringProperty;
import org.eclipse.andmore.gltrace.state.IGLProperty;
import org.eclipse.andmore.gltrace.views.FitToCanvasAction;
import org.eclipse.andmore.gltrace.views.SaveImageAction;
import org.eclipse.andmore.gltrace.widgets.ImageCanvas;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import java.util.Arrays;
import java.util.List;

public class TextureImageDetailsProvider implements IStateDetailProvider {
	private ImageCanvas mImageCanvas;
	private FitToCanvasAction mFitToCanvasAction;
	private SaveImageAction mSaveImageAction;
	private List<IContributionItem> mToolBarItems;

	@Override
	public boolean isApplicable(IGLProperty state) {
		return getTextureImageProperty(state) != null;
	}

	@Override
	public void createControl(Composite parent) {
		mImageCanvas = new ImageCanvas(parent);
		mImageCanvas.setFitToCanvas(false);

		mFitToCanvasAction = new FitToCanvasAction(false, mImageCanvas);
		mSaveImageAction = new SaveImageAction(mImageCanvas);

		mToolBarItems = Arrays.asList((IContributionItem) new ActionContributionItem(mFitToCanvasAction),
				(IContributionItem) new ActionContributionItem(mSaveImageAction));
	}

	@Override
	public void disposeControl() {
		mImageCanvas.dispose();
		mImageCanvas = null;
	}

	@Override
	public Control getControl() {
		return mImageCanvas;
	}

	@Override
	public void updateControl(IGLProperty state) {
		IGLProperty imageProperty = getTextureImageProperty(state);
		if (imageProperty == null) {
			return;
		}

		String texturePath = ((GLStringProperty) imageProperty).getStringValue();
		if (texturePath != null) {
			mImageCanvas.setImage(new Image(Display.getDefault(), texturePath));
			mImageCanvas.setFitToCanvas(false);
			return;
		}
	}

	/**
	 * Get the {@link GLStateType#TEXTURE_IMAGE} property given a node in the
	 * state hierarchy.
	 * 
	 * @param state
	 *            any node in the GL state hierarchy
	 * @return The {@link GLStateType#TEXTURE_IMAGE} property if a unique
	 *         instance of it can be accessed from the given node. A unique
	 *         instance can be accessed if the given node is either the
	 *         requested node itself, or its parent or sibling. In cases where a
	 *         unique instance cannot be accessed, but one of the texture mipmap
	 *         levels can be accessed, then return the first texture mipmap
	 *         level. This happens if the selected state is a child of
	 *         {@link GLStateType#PER_TEXTURE_STATE}. Returns null otherwise.
	 */
	private IGLProperty getTextureImageProperty(IGLProperty state) {
		if (state.getType() == GLStateType.TEXTURE_IMAGE) {
			// given node is the requested node
			return state;
		}

		IGLProperty img = getImageFromPerTextureLevelState(state);
		if (img != null) {
			return img;
		}

		return getFirstMipmapImage(state);
	}

	/**
	 * Returns the {@link GLStateType#TEXTURE_IMAGE} if the provided state is
	 * either {@link GLStateType#PER_TEXTURE_LEVEL_STATE} or one of its
	 * children. Returns null otherwise.
	 */
	private IGLProperty getImageFromPerTextureLevelState(IGLProperty state) {
		if (state != null && state.getType() != GLStateType.PER_TEXTURE_LEVEL_STATE) {
			state = state.getParent();
		}

		if (state == null || state.getType() != GLStateType.PER_TEXTURE_LEVEL_STATE) {
			return null;
		}

		return ((GLCompositeProperty) state).getProperty(GLStateType.TEXTURE_IMAGE);
	}

	/**
	 * Returns the first mipmap level's image entry if the provided state is
	 * either {@link GLStateType#PER_TEXTURE_STATE} or one of its immediate
	 * children, null otherwise.
	 */
	private IGLProperty getFirstMipmapImage(IGLProperty state) {
		if (state != null && state.getType() != GLStateType.PER_TEXTURE_STATE) {
			state = state.getParent();
		}

		if (state == null || state.getType() != GLStateType.PER_TEXTURE_STATE) {
			return null;
		}

		IGLProperty mipmaps = ((GLCompositeProperty) state).getProperty(GLStateType.TEXTURE_MIPMAPS);
		if (!(mipmaps instanceof GLSparseArrayProperty)) {
			return null;
		}

		IGLProperty perTextureLevelState = ((GLSparseArrayProperty) mipmaps).getProperty(0);
		return getImageFromPerTextureLevelState(perTextureLevelState);
	}

	@Override
	public List<IContributionItem> getToolBarItems() {
		return mToolBarItems;
	}
}
