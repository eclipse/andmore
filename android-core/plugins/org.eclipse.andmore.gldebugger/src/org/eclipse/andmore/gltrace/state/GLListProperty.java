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

package org.eclipse.andmore.gltrace.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A list property is a container for a list of properties, addressed by index.
 */
public class GLListProperty implements IGLProperty {
	private final List<IGLProperty> mList;
	private final GLStateType mType;
	private IGLProperty mParent;
	private IGLProperty mTemplate;

	/**
	 * Construct a list of properties of given size from the provided template.
	 * 
	 * @param template
	 *            property that will be cloned and used as members of the list
	 * @param size
	 *            size of the list
	 */
	public GLListProperty(GLStateType type, IGLProperty template, int size) {
		mType = type;
		mTemplate = template;

		mList = new ArrayList<IGLProperty>(size);
		for (int i = 0; i < size; i++) {
			IGLProperty p = template.clone();
			mList.add(p);

			p.setParent(this);
		}
	}

	private GLListProperty(GLStateType type, List<IGLProperty> props) {
		mList = props;
		mType = type;

		for (IGLProperty p : mList) {
			p.setParent(this);
		}
	}

	public List<IGLProperty> getList() {
		return mList;
	}

	public IGLProperty get(int index) {
		return mList.get(index);
	}

	public boolean add(IGLProperty property) {
		property.setParent(this);
		return mList.add(property);
	}

	public boolean remove(IGLProperty property) {
		return mList.remove(property);
	}

	public void set(int index, IGLProperty property) {
		ensureCapacity(index + 1);
		mList.set(index, property);
		property.setParent(this);
	}

	private void ensureCapacity(int capactiy) {
		for (int i = mList.size(); i < capactiy; i++) {
			mList.add(mTemplate);
		}
	}

	@Override
	public GLListProperty clone() {
		List<IGLProperty> props = new ArrayList<IGLProperty>(mList.size());

		for (IGLProperty p : mList) {
			props.add(p.clone());
		}

		return new GLListProperty(getType(), props);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("GLListProperty ["); //$NON-NLS-1$

		int i = 0;
		for (IGLProperty p : mList) {
			sb.append(i);
			sb.append(':');
			sb.append(p.toString());
			sb.append(", "); //$NON-NLS-1$
			i++;
		}

		sb.append("]");
		return sb.toString();
	}

	@Override
	public String getStringValue() {
		// This method is called for displaying objects in the UI.
		// We do not display any values for composites in the UI as they are
		// only intermediate
		// nodes in the tree.
		return "";
	}

	@Override
	public GLStateType getType() {
		return mType;
	}

	@Override
	public boolean isComposite() {
		return true;
	}

	@Override
	public boolean isDefault() {
		return false;
	}

	@Override
	public IGLProperty getParent() {
		return mParent;
	}

	@Override
	public void setParent(IGLProperty parent) {
		mParent = parent;
	}

	public int indexOf(IGLProperty property) {
		return mList.indexOf(property);
	}

	@Override
	public void setValue(Object value) {
		throw new UnsupportedOperationException("Values cannot be set for composite properties."); //$NON-NLS-1$
	}

	@Override
	public Object getValue() {
		throw new UnsupportedOperationException("Values cannot be obtained for composite properties."); //$NON-NLS-1$
	}

	public int size() {
		return mList.size();
	}

	@Override
	public void prettyPrint(StatePrettyPrinter pp) {
		pp.prettyPrint(mType, null);
		pp.incrementIndentLevel();
		for (int i = 0; i < mList.size(); i++) {
			pp.prettyPrint(String.format(Locale.US, "Index %d:", i));
			IGLProperty p = mList.get(i);
			p.prettyPrint(pp);
		}
		pp.decrementIndentLevel();
	}
}
