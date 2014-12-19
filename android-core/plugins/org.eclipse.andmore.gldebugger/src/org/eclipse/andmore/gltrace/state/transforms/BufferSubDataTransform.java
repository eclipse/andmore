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

package org.eclipse.andmore.gltrace.state.transforms;

import java.nio.ByteBuffer;

import org.eclipse.andmore.gltrace.GLProtoBuf.GLMessage.Function;
import org.eclipse.andmore.gltrace.state.IGLProperty;

/**
 * A {@link BufferSubDataTransform} updates a portion of the buffer data as
 * specified by the {@link Function#glBufferSubData} function.
 */
public class BufferSubDataTransform implements IStateTransform {
	private final IGLPropertyAccessor mAccessor;
	private final int mOffset;

	private final byte[] mSubData;
	private byte[] mOldData;
	private byte[] mNewData;

	public BufferSubDataTransform(IGLPropertyAccessor accessor, int offset, byte[] data) {
		mAccessor = accessor;
		mOffset = offset;
		mSubData = data;
	}

	@Override
	public void apply(IGLProperty state) {
		IGLProperty property = mAccessor.getProperty(state);
		mOldData = (byte[]) property.getValue();

		if (mOldData != null) {
			mNewData = new byte[mOldData.length];
			ByteBuffer bb = ByteBuffer.wrap(mNewData);

			// copy all of the old buffer
			bb.put(mOldData);
			bb.rewind();

			// update with the sub buffer data at specified offset
			bb.position(mOffset);
			bb.put(mSubData);
		}

		property.setValue(mNewData);
	}

	@Override
	public void revert(IGLProperty state) {
		if (mOldData != null) {
			IGLProperty property = mAccessor.getProperty(state);
			property.setValue(mOldData);
			mOldData = null;
		}
	}

	@Override
	public IGLProperty getChangedProperty(IGLProperty state) {
		return mAccessor.getProperty(state);
	}
}
