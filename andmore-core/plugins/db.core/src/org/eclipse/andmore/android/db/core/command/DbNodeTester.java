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

package org.eclipse.andmore.android.db.core.command;

import org.eclipse.andmore.android.db.core.ui.IDbNode;
import org.eclipse.core.expressions.PropertyTester;

public class DbNodeTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		boolean result = false;
		if (receiver instanceof IDbNode) {
			IDbNode dbNode = (IDbNode) receiver;
			result = dbNode.testAttribute(dbNode, property, expectedValue.toString());
		}

		return result;
	}
}
