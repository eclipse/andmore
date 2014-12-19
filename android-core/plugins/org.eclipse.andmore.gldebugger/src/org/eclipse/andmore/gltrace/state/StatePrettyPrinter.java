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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.utils.SdkUtils;

public class StatePrettyPrinter {
	private static final int SPACES_PER_INDENT = 4;
	private final String mLineSeparator = SdkUtils.getLineSeparator();

	private StringBuilder mSb = new StringBuilder(1000);
	private int mIndentLevel = 0;

	public void prettyPrint(@NonNull GLStateType name, @Nullable String value) {
		indentLine(mIndentLevel * SPACES_PER_INDENT);

		mSb.append(name.toString());

		if (value != null) {
			mSb.append(':');
			mSb.append(value);
		}
		mSb.append(mLineSeparator);
	}

	public void prettyPrint(@NonNull String s) {
		indentLine(mIndentLevel * SPACES_PER_INDENT);

		mSb.append(s);
		mSb.append(mLineSeparator);
	}

	private void indentLine(int spaces) {
		for (int i = 0; i < spaces; i++) {
			mSb.append(' ');
		}
	}

	public void incrementIndentLevel() {
		mIndentLevel++;
	}

	public void decrementIndentLevel() {
		mIndentLevel--;
	}

	@Override
	public String toString() {
		return mSb.toString();
	}
}
