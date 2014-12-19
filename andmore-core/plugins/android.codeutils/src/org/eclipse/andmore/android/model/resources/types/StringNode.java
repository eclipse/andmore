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
package org.eclipse.andmore.android.model.resources.types;

/**
 * Class that represents a <string> node on a resource file
 * 
 * Format: <string name="StringName">String Value</string>
 */
public class StringNode extends AbstractSimpleNameResourceNode {
	/**
	 * Default constructor
	 * 
	 * @param name
	 *            The string name. It must not be null.
	 */
	public StringNode(String name) {
		super(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.model.resources.AbstractResourceNode#getNodeType
	 * ()
	 */
	@Override
	public NodeType getNodeType() {
		return NodeType.String;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.resources.types.
	 * AbstractSimpleNameResourceNode#setNodeValue(java.lang.String)
	 */
	@Override
	public void setNodeValue(String nodeValue) {
		super.setNodeValue(escapeString(nodeValue));
	}

	/**
	 * Escape invalid characters for the strings resource file
	 * 
	 * @param str
	 *            The string to be escaped
	 * @return The escaped string
	 */
	private String escapeString(String str) {
		String newStr = str;

		if (newStr != null) {
			newStr = newStr.replace("\\'", "'");
			newStr = newStr.replace("\\@", "@");

			newStr = newStr.replace("'", "\\'");
			newStr = newStr.replace("@", "\\@");
		}

		return newStr;
	}
}
