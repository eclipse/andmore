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

import com.google.common.base.Joiner;

import org.eclipse.andmore.gltrace.GLEnum;
import org.eclipse.andmore.gltrace.state.GLCompositeProperty;
import org.eclipse.andmore.gltrace.state.GLStateType;
import org.eclipse.andmore.gltrace.state.IGLProperty;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import java.util.Collections;
import java.util.List;

public class ShaderUniformDetailsProvider implements IStateDetailProvider {
	private Text mTextControl;
	private static final Joiner JOINER = Joiner.on(", ");

	@Override
	public boolean isApplicable(IGLProperty state) {
		return getShaderUniformProperty(state) != null;
	}

	@Override
	public void createControl(Composite parent) {
		mTextControl = new Text(parent, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
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
		IGLProperty uniform = getShaderUniformProperty(state);
		if (uniform instanceof GLCompositeProperty) {
			GLCompositeProperty uniformProperty = (GLCompositeProperty) uniform;
			IGLProperty nameProperty = uniformProperty.getProperty(GLStateType.UNIFORM_NAME);
			IGLProperty typeProperty = uniformProperty.getProperty(GLStateType.UNIFORM_TYPE);
			IGLProperty valueProperty = uniformProperty.getProperty(GLStateType.UNIFORM_VALUE);

			String name = (String) nameProperty.getValue();
			GLEnum type = (GLEnum) typeProperty.getValue();
			Object value = valueProperty.getValue();
			mTextControl.setText(formatUniform(name, type, value));
			mTextControl.setEnabled(true);
		} else {
			mTextControl.setText(""); //$NON-NLS-1$
			mTextControl.setEnabled(false);
		}
	}

	private String formatUniform(String name, GLEnum type, Object value) {
		String valueText;

		switch (type) {
		case GL_FLOAT:
		case GL_FLOAT_VEC2:
		case GL_FLOAT_VEC3:
		case GL_FLOAT_VEC4:
		case GL_INT:
		case GL_INT_VEC2:
		case GL_INT_VEC3:
		case GL_INT_VEC4:
		case GL_BOOL:
		case GL_BOOL_VEC2:
		case GL_BOOL_VEC3:
		case GL_BOOL_VEC4:
			valueText = formatVector(value);
			break;
		case GL_FLOAT_MAT2:
			valueText = formatMatrix(2, value);
			break;
		case GL_FLOAT_MAT3:
			valueText = formatMatrix(3, value);
			break;
		case GL_FLOAT_MAT4:
			valueText = formatMatrix(4, value);
			break;
		case GL_SAMPLER_2D:
		case GL_SAMPLER_CUBE:
		default:
			valueText = value.toString();
			break;
		}

		return String.format("%s %s = %s", type, name, valueText); //$NON-NLS-1$
	}

	private String formatVector(Object value) {
		if (value instanceof List<?>) {
			List<?> list = (List<?>) value;
			StringBuilder sb = new StringBuilder(list.size() * 4);
			sb.append('[');
			JOINER.appendTo(sb, list);
			sb.append(']');
			return sb.toString();
		}

		return value.toString();
	}

	private String formatMatrix(int dimension, Object value) {
		if (value instanceof List<?>) {
			List<?> list = (List<?>) value;
			if (list.size() != dimension * dimension) {
				// Uniforms can only be square matrices, so this scenario should
				// not occur.
				return formatVector(value);
			}

			StringBuilder sb = new StringBuilder(list.size() * 4);
			sb.append('[');
			sb.append('\n');
			for (int i = 0; i < dimension; i++) {
				sb.append("    "); //$NON-NLS-1$
				JOINER.appendTo(sb, list.subList(i * dimension, (i + 1) * dimension));
				sb.append('\n');
			}
			sb.append(']');
			return sb.toString();
		}

		return value.toString();
	}

	/**
	 * Get the {@link GLStateType#PER_UNIFORM_STATE} property given a node in
	 * the state hierarchy.
	 */
	private IGLProperty getShaderUniformProperty(IGLProperty state) {
		if (state.getType() == GLStateType.PER_UNIFORM_STATE) {
			return state;
		}

		state = state.getParent();
		if (state != null && state.getType() == GLStateType.PER_UNIFORM_STATE) {
			return state;
		}

		return null;
	}

	@Override
	public List<IContributionItem> getToolBarItems() {
		return Collections.emptyList();
	}
}
