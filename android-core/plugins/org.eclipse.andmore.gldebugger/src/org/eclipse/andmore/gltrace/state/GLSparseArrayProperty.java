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

package org.eclipse.andmore.gltrace.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.android.utils.SparseArray;

public class GLSparseArrayProperty implements IGLProperty {
	private final GLStateType mType;
	private final IGLProperty mDefaultValue;
	private final boolean mCreateOnAccess;
	private final SparseArray<IGLProperty> mSparseArray;
	private IGLProperty mParent;

	public GLSparseArrayProperty(GLStateType type, IGLProperty defaultValue) {
		this(type, defaultValue, false);
	}

	/**
	 * Constructs a sparse array property.
	 * 
	 * @param type
	 *            GL state corresponding to this property
	 * @param defaultValue
	 *            default value of each item
	 * @param createOnAccess
	 *            create an item on access if it is not present
	 */
	public GLSparseArrayProperty(GLStateType type, IGLProperty defaultValue, boolean createOnAccess) {
		mType = type;
		mDefaultValue = defaultValue;
		mCreateOnAccess = createOnAccess;
		mSparseArray = new SparseArray<IGLProperty>(20);
	}

	private GLSparseArrayProperty(GLStateType type, IGLProperty defaultValue, boolean createOnAccess,
			SparseArray<IGLProperty> contents) {
		mType = type;
		mDefaultValue = defaultValue;
		mCreateOnAccess = createOnAccess;
		mSparseArray = contents;
	}

	public List<IGLProperty> getValues() {
		List<IGLProperty> values = new ArrayList<IGLProperty>(mSparseArray.size());

		for (int i = 0; i < mSparseArray.size(); i++) {
			values.add(mSparseArray.valueAt(i));
		}

		return values;
	}

	public IGLProperty getProperty(int key) {
		IGLProperty p = mSparseArray.get(key);
		if (p == null && mCreateOnAccess) {
			add(key);
			p = mSparseArray.get(key);
		}
		return p;
	}

	public int keyFor(IGLProperty element) {
		int index = mSparseArray.indexOfValue(element);
		return mSparseArray.keyAt(index);
	}

	public void put(int key, IGLProperty element) {
		element.setParent(this);
		mSparseArray.put(key, element);
	}

	public void add(int key) {
		IGLProperty prop = mDefaultValue.clone();
		prop.setParent(this);
		mSparseArray.put(key, prop);
	}

	public void delete(int key) {
		mSparseArray.delete(key);
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

	@Override
	public GLSparseArrayProperty clone() {
		SparseArray<IGLProperty> copy = new SparseArray<IGLProperty>(mSparseArray.size());
		for (int i = 0; i < mSparseArray.size(); i++) {
			int key = mSparseArray.keyAt(i);
			IGLProperty value = mSparseArray.get(key);
			copy.put(key, value);
		}

		return new GLSparseArrayProperty(mType, mDefaultValue, mCreateOnAccess, copy);
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
	public void setValue(Object value) {
		throw new UnsupportedOperationException("Values cannot be set for composite properties."); //$NON-NLS-1$
	}

	@Override
	public Object getValue() {
		throw new UnsupportedOperationException("Values cannot be obtained for composite properties."); //$NON-NLS-1$
	}

	@Override
	public void prettyPrint(StatePrettyPrinter pp) {
		pp.prettyPrint(mType, null);
		pp.incrementIndentLevel();
		for (int i = 0; i < mSparseArray.size(); i++) {
			int key = mSparseArray.keyAt(i);
			pp.prettyPrint(String.format(Locale.US, "Index %d:", key));
			IGLProperty prop = mSparseArray.get(key);

			assert prop != null;
			if (prop != null) {
				prop.prettyPrint(pp);
			}
		}
		pp.decrementIndentLevel();
	}
}
