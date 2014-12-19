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

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * A composite property is a container for multiple named properties, kind of
 * like a dictionary.
 */
public class GLCompositeProperty implements IGLProperty {
	private final GLStateType mType;
	private final Map<GLStateType, IGLProperty> mPropertiesMap;
	private IGLProperty mParent;

	/**
	 * Construct a composite property given a list of {@link IGLProperty}
	 * objects.
	 */
	public GLCompositeProperty(GLStateType type, IGLProperty... iglProperties) {
		mType = type;
		mPropertiesMap = new EnumMap<GLStateType, IGLProperty>(GLStateType.class);

		for (IGLProperty p : iglProperties) {
			mPropertiesMap.put(p.getType(), p);
			p.setParent(this);
		}
	}

	public Collection<IGLProperty> getProperties() {
		return mPropertiesMap.values();
	}

	public IGLProperty getProperty(GLStateType name) {
		return mPropertiesMap.get(name);
	}

	@Override
	public GLCompositeProperty clone() {
		IGLProperty[] props = new IGLProperty[mPropertiesMap.size()];

		int i = 0;
		for (IGLProperty p : mPropertiesMap.values()) {
			props[i++] = p.clone();
		}

		return new GLCompositeProperty(getType(), props);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("GLCompositeProperty {"); //$NON-NLS-1$

		for (IGLProperty p : mPropertiesMap.values()) {
			sb.append(p.toString());
			sb.append(", "); //$NON-NLS-1$
		}

		sb.append("}");
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
		for (IGLProperty p : mPropertiesMap.values()) {
			if (!p.isDefault()) {
				return false;
			}
		}

		return true;
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
		for (IGLProperty p : mPropertiesMap.values()) {
			p.prettyPrint(pp);
		}
		pp.decrementIndentLevel();
	}
}
