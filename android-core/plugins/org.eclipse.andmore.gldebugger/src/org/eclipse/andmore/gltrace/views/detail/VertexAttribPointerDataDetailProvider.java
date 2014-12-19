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

import org.eclipse.andmore.gltrace.GLEnum;
import org.eclipse.andmore.gltrace.GLUtils;
import org.eclipse.andmore.gltrace.GLProtoBuf.GLMessage.Function;
import org.eclipse.andmore.gltrace.model.GLCall;
import org.eclipse.andmore.gltrace.model.GLTrace;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import java.util.Collections;
import java.util.List;

public class VertexAttribPointerDataDetailProvider implements ICallDetailProvider {
	private Text mText;

	@Override
	public boolean isApplicable(GLCall call) {
		return call.getFunction() == Function.glVertexAttribPointerData;
	}

	@Override
	public void createControl(Composite parent) {
		mText = new Text(parent, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
	}

	@Override
	public void disposeControl() {
	}

	@Override
	public Control getControl() {
		return mText;
	}

	@Override
	public void updateControl(GLTrace trace, GLCall call) {
		// void glVertexAttribPointerData(GLuint indx, GLint size, GLenum type,
		// GLboolean norm,
		// GLsizei stride, const GLvoid* ptr, int minIndex, int maxIndex)
		GLEnum type = (GLEnum) call.getProperty(GLCall.PROPERTY_VERTEX_ATTRIB_POINTER_TYPE);
		byte[] data = (byte[]) call.getProperty(GLCall.PROPERTY_VERTEX_ATTRIB_POINTER_DATA);

		mText.setText(GLUtils.formatData(data, type));
	}

	@Override
	public List<IContributionItem> getToolBarItems() {
		return Collections.emptyList();
	}
}
