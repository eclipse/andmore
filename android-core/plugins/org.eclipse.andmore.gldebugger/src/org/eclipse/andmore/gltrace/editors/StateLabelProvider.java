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

package org.eclipse.andmore.gltrace.editors;

import org.eclipse.andmore.gltrace.state.GLListProperty;
import org.eclipse.andmore.gltrace.state.GLSparseArrayProperty;
import org.eclipse.andmore.gltrace.state.GLStateType;
import org.eclipse.andmore.gltrace.state.IGLProperty;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import java.util.Set;

public class StateLabelProvider extends ColumnLabelProvider {
	private Set<IGLProperty> mChangedProperties;

	private Color mHighlightForegroundColor;
	private Color mNormalForegroundColor;

	public StateLabelProvider() {
		mHighlightForegroundColor = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
		mNormalForegroundColor = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
	}

	public String getColumnText(IGLProperty property, int columnIndex) {
		switch (columnIndex) {
		case 0:
			return getName(property);
		case 1:
			return getValue(property);
		default:
			return "";
		}
	}

	private String getValue(IGLProperty element) {
		return element.getStringValue();
	}

	private String getName(IGLProperty element) {
		IGLProperty parent = element.getParent();
		if (parent instanceof GLListProperty) {
			// For members of list, use the index in the list as the name as
			// opposed to
			// the property type
			int index = ((GLListProperty) parent).indexOf(element);
			if (element.getType() == GLStateType.GL_STATE_ES1) {
				return String.format("Context %d (ES1)", index);
			} else if (element.getType() == GLStateType.GL_STATE_ES2) {
				return String.format("Context %d (ES2)", index);
			} else {
				return Integer.toString(index);
			}
		} else if (parent instanceof GLSparseArrayProperty) {
			// For members of sparse array, use the key as the name as opposed
			// to
			// the property type
			int index = ((GLSparseArrayProperty) parent).keyFor(element);
			return Integer.toString(index);
		}

		return element.getType().getDescription();
	}

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		if (!(element instanceof IGLProperty)) {
			return;
		}

		IGLProperty prop = (IGLProperty) element;

		String text = getColumnText(prop, cell.getColumnIndex());
		cell.setText(text);

		if (mChangedProperties != null && mChangedProperties.contains(prop)) {
			cell.setForeground(mHighlightForegroundColor);
		} else {
			cell.setForeground(mNormalForegroundColor);
		}
	}

	public void setChangedProperties(Set<IGLProperty> changedProperties) {
		mChangedProperties = changedProperties;
	}
}
