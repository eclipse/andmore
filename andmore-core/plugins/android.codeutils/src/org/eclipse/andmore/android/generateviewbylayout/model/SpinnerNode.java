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
package org.eclipse.andmore.android.generateviewbylayout.model;

/**
 * Extends {@link LayoutNode} to describe save/restore method names and the name
 * of the node type (specific from Spinner)
 */
public class SpinnerNode extends LayoutNode {

	public SpinnerNode() {
		properties.put(ViewProperties.ViewStateSetMethod, ViewSetMethods.setSelection.name());
		properties.put(ViewProperties.ViewStateGetMethod, ViewGetMethods.getSelectedItemPosition.name());
		properties.put(ViewProperties.PreferenceSetMethod, PreferenceSetMethods.putInt.name());
		properties.put(ViewProperties.PreferenceGetMethod, PreferenceGetMethods.getInt.name());
		properties.put(ViewProperties.ViewStateValueType, Integer.class.toString());
	}

	@Override
	public String getNodeType() {
		return LayoutNodeViewType.Spinner.name();
	}

}
