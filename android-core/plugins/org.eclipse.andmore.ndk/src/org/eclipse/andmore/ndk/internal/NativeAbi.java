/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.ndk.internal;

import com.android.SdkConstants;

public enum NativeAbi {
	armeabi(SdkConstants.ABI_ARMEABI), armeabi_v7a(SdkConstants.ABI_ARMEABI_V7A), mips(SdkConstants.ABI_MIPS), x86(
			SdkConstants.ABI_INTEL_ATOM);

	private final String mAbi;

	private NativeAbi(String abi) {
		mAbi = abi;
	}

	public String getAbi() {
		return mAbi;
	}

	public static NativeAbi getByString(String abi) {
		for (NativeAbi a : values()) {
			if (a.getAbi().equals(abi)) {
				return a;
			}
		}

		throw new IllegalArgumentException("Unknown abi: " + abi);
	}
}
