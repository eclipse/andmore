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

/**
 * The GL state is modeled as a hierarchical set of properties, all of which
 * implement this interface.
 */
public interface IGLProperty extends Cloneable {
	/** Obtain the type of the property. */
	GLStateType getType();

	/**
	 * Is this a composite property?
	 * 
	 * @return true if it is a list or structure of properties, false if it is a
	 *         leaf level atomic property
	 * */
	boolean isComposite();

	/**
	 * Is the currently set value the default?
	 * 
	 * @return true if current value matches the default (initial) value
	 */
	boolean isDefault();

	/** Set the current value for this property. */
	void setValue(Object value);

	/** Get the current value for this property. */
	Object getValue();

	/** Get the string representation for this property. */
	String getStringValue();

	/**
	 * Get the parent property that holds this property.
	 * 
	 * @return null if this property is at the top level, parent otherwise
	 */
	IGLProperty getParent();

	/** Set the parent property that holds this property. */
	void setParent(IGLProperty parent);

	/** Deep clone this property. */
	IGLProperty clone();

	/** Pretty print current property value to the given writer. */
	void prettyPrint(StatePrettyPrinter pp);
}
