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

package org.eclipse.andmore.gltrace.model;

import com.android.utils.SparseArray;

import java.util.Collections;
import java.util.List;

import org.eclipse.andmore.gltrace.GLProtoBuf;
import org.eclipse.andmore.gltrace.GLProtoBuf.GLMessage.Function;
import org.eclipse.andmore.gltrace.state.transforms.IStateTransform;

/**
 * A GLCall is the in memory representation of a single
 * {@link GLProtoBuf.GLMessage}.
 *
 * Some protocol buffer messages have a large amount of image data packed in
 * them. Rather than storing all of that in memory, the GLCall stores a
 * thumbnail image, and an offset into the trace file corresponding to original
 * protocol buffer message. If full image data is required, the protocol buffer
 * message can be recreated by reading the trace at the specified offset.
 */
public class GLCall {
	/** Marker name provided by a {@link Function#glPushGroupMarkerEXT} call. */
	public static final int PROPERTY_MARKERNAME = 0;

	/** Size argument in a {@link Function#glVertexAttribPointerData} call. */
	public static final int PROPERTY_VERTEX_ATTRIB_POINTER_SIZE = 1;

	/** Type argument in a {@link Function#glVertexAttribPointerData} call. */
	public static final int PROPERTY_VERTEX_ATTRIB_POINTER_TYPE = 2;

	/** Data argument in a {@link Function#glVertexAttribPointerData} call. */
	public static final int PROPERTY_VERTEX_ATTRIB_POINTER_DATA = 3;

	/** Index of this call in the trace. */
	private int mIndex;

	/** Time on device when this call was invoked. */
	private final long mStartTime;

	/**
	 * Offset of the protobuf message corresponding to this call in the trace
	 * file.
	 */
	private final long mTraceFileOffset;

	/** Flag indicating whether the original protobuf message included FB data. */
	private final boolean mHasFb;

	/** Full string representation of this call. */
	private final String mDisplayString;

	/** The actual GL Function called. */
	private final Function mFunction;

	/** GL Context identifier corresponding to the context of this call. */
	private final int mContextId;

	/** Duration of this call (MONOTONIC/wall clock time). */
	private final int mWallDuration;

	/** Duration of this call (THREAD time). */
	private final int mThreadDuration;

	/** List of state transformations performed by this call. */
	private List<IStateTransform> mStateTransforms = Collections.emptyList();

	/** Error conditions while creating state transforms for this call. */
	private String mStateTransformationCreationErrorMessage;

	/** List of properties associated to this call. */
	private SparseArray<Object> mProperties;

	public GLCall(int index, long startTime, long traceFileOffset, String displayString, Function function,
			boolean hasFb, int contextId, int wallTime, int threadTime) {
		mIndex = index;
		mStartTime = startTime;
		mTraceFileOffset = traceFileOffset;
		mDisplayString = displayString;
		mFunction = function;
		mHasFb = hasFb;
		mContextId = contextId;
		mWallDuration = wallTime;
		mThreadDuration = threadTime;
	}

	public int getIndex() {
		return mIndex;
	}

	public void setIndex(int i) {
		mIndex = i;
	}

	public long getOffsetInTraceFile() {
		return mTraceFileOffset;
	}

	public Function getFunction() {
		return mFunction;
	}

	public int getContextId() {
		return mContextId;
	}

	public boolean hasFb() {
		return mHasFb;
	}

	public long getStartTime() {
		return mStartTime;
	}

	public int getWallDuration() {
		return mWallDuration;
	}

	public int getThreadDuration() {
		return mThreadDuration;
	}

	public void setStateTransformations(List<IStateTransform> transforms) {
		mStateTransforms = transforms;
	}

	public void setStateTransformationCreationError(String errorMessage) {
		mStateTransformationCreationErrorMessage = errorMessage;
	}

	public boolean hasErrors() {
		return mStateTransformationCreationErrorMessage != null;
	}

	public String getError() {
		return mStateTransformationCreationErrorMessage;
	}

	public List<IStateTransform> getStateTransformations() {
		return mStateTransforms;
	}

	@Override
	public String toString() {
		return mDisplayString;
	}

	/**
	 * Associate a certain value to the property name. Property names are
	 * defined as constants in {@link GLCall}.
	 */
	public void addProperty(int propertyName, Object value) {
		if (mProperties == null) {
			mProperties = new SparseArray<Object>(1);
		}

		mProperties.put(propertyName, value);
	}

	/**
	 * Obtain the value for the given property. Returns null if no such property
	 * is associated with this {@link GLCall}.
	 */
	public Object getProperty(int propertyName) {
		if (mProperties == null) {
			return null;
		}

		return mProperties.get(propertyName);
	}
}
