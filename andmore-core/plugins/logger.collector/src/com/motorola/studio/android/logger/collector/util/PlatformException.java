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

package com.motorola.studio.android.logger.collector.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * This class represents a basic platform exception that all Studios should
 * extend and implement other functions. The error is logged when the status
 * severity is <code>IStatus.CANCEL</code>.
 * 
 * @see org.eclipse.core.runtime.CoreException
 */

public class PlatformException extends CoreException {

	/**
	 * Universal version identifier for a Serializable object.
	 */
	private static final long serialVersionUID = 5165818604668097411L;

	/**
	 * Construct a platform exception with the given status.
	 * 
	 * @param status
	 *            The IStatus.
	 */
	public PlatformException(final IStatus status) {
		super(status);
		if (status.getSeverity() == IStatus.CANCEL) {
			PlatformLogger.getInstance(PlatformException.class).error(this.getMessage(), this);
		}
	}
}
