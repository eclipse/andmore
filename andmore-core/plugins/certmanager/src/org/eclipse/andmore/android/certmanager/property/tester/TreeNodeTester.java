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
package org.eclipse.andmore.android.certmanager.property.tester;

import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;
import org.eclipse.core.expressions.PropertyTester;

/**
 * Property testers are used via extension point to test object's state
 * properties. This tester checks {@link ITreeNode} properties.
 * */
public class TreeNodeTester extends PropertyTester {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.expressions.IPropertyTester#test(Object,String,Object
	 * [],Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		boolean result = false;

		if (receiver instanceof ITreeNode) {
			ITreeNode treeNode = (ITreeNode) receiver;
			result = treeNode.testAttribute(receiver, ITreeNode.PROP_NAMESPACE.concat("." + property),
					expectedValue.toString());
		}

		return result;
	}

}
