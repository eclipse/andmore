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
import org.eclipse.andmore.gltrace.state.GLStateType;
import org.eclipse.andmore.gltrace.state.GLStringProperty;
import org.eclipse.andmore.gltrace.state.IGLProperty;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import java.util.Collections;
import java.util.List;

public class ShaderSourceDetailsProvider implements IStateDetailProvider {
	private Text mTextControl;

	@Override
	public boolean isApplicable(IGLProperty state) {
		return getShaderSourceProperty(state) != null;
	}

	@Override
	public void createControl(Composite parent) {
		mTextControl = new Text(parent, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
		mTextControl.setEditable(false);
	}

	@Override
	public void disposeControl() {
	}

	@Override
	public Control getControl() {
		return mTextControl;
	}

	@Override
	public void updateControl(IGLProperty state) {
		IGLProperty shaderSrcProperty = getShaderSourceProperty(state);
		if (shaderSrcProperty instanceof GLStringProperty) {
			String shaderSrc = ((GLStringProperty) shaderSrcProperty).getStringValue();
			mTextControl.setText(shaderSrc);
			mTextControl.setEnabled(true);
		} else {
			mTextControl.setText(""); //$NON-NLS-1$
			mTextControl.setEnabled(false);
		}
	}

	/**
	 * Get the {@link GLStateType#SHADER_SOURCE} property given a node in the
	 * state hierarchy.
	 * 
	 * @param state
	 *            any node in the GL state hierarchy
	 * @return The {@link GLStateType#SHADER_SOURCE} property if a unique
	 *         instance of it can be accessed from the given node, null
	 *         otherwise. A unique instance can be accessed if the given node is
	 *         either the requested node itself, or its parent or sibling.
	 */
	private IGLProperty getShaderSourceProperty(IGLProperty state) {
		if (state.getType() == GLStateType.SHADER_SOURCE) {
			// given node is the requested node
			return state;
		}

		if (state.getType() != GLStateType.PER_SHADER_STATE) {
			// if it is not the parent, then it could be a sibling, in which
			// case
			// we go up a level to its parent
			state = state.getParent();
		}

		if (state != null && state.getType() == GLStateType.PER_SHADER_STATE) {
			// if it is the parent, we can access the required property
			return ((GLCompositeProperty) state).getProperty(GLStateType.SHADER_SOURCE);
		}

		return null;
	}

	@Override
	public List<IContributionItem> getToolBarItems() {
		return Collections.emptyList();
	}
}
