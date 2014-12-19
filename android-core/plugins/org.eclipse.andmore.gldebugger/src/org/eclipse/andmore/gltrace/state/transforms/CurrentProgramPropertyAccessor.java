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

import org.eclipse.andmore.gltrace.state.GLIntegerProperty;
import org.eclipse.andmore.gltrace.state.GLStateType;
import org.eclipse.andmore.gltrace.state.IGLProperty;

public class CurrentProgramPropertyAccessor implements IGLPropertyAccessor {
	private final int mContextId;
	private final GLStateType mStateCategory;
	private final int mLocation;
	private final GLStateType mStateType;
	private final IGLPropertyAccessor mCurrentProgramAccessor;

	public CurrentProgramPropertyAccessor(int contextid, GLStateType stateCategory, int location, GLStateType stateType) {
		mContextId = contextid;
		mStateCategory = stateCategory;
		mLocation = location;
		mStateType = stateType;

		mCurrentProgramAccessor = GLPropertyAccessor.makeAccessor(contextid, GLStateType.PROGRAM_STATE,
				GLStateType.CURRENT_PROGRAM);
	}

	@Override
	public IGLProperty getProperty(IGLProperty state) {
		// obtain the current program
		IGLProperty currentProgramProperty = mCurrentProgramAccessor.getProperty(state);
		if (!(currentProgramProperty instanceof GLIntegerProperty)) {
			return null;
		}

		Integer program = (Integer) currentProgramProperty.getValue();

		// now access the required program property
		return GLPropertyAccessor.makeAccessor(mContextId, GLStateType.PROGRAM_STATE, GLStateType.PROGRAMS, program,
				mStateCategory, Integer.valueOf(mLocation), mStateType).getProperty(state);
	}

	@Override
	public String getPath() {
		return String.format("PROGRAM_STATE/PROGRAMS/${program}/%s/%d/%s", mStateCategory, mLocation, mStateType);
	}
}
