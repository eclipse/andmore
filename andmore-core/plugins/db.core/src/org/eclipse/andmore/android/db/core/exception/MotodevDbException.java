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
package org.eclipse.andmore.android.db.core.exception;

import org.eclipse.andmore.android.common.exception.AndroidException;

public class MotodevDbException extends AndroidException {

	private static final long serialVersionUID = 1148147648131562077L;

	/**
	 * Creates a new MotodevDbException object.
	 */
	public MotodevDbException() {

	}

	/**
	 * Creates a new MotodevDbException object.
	 * 
	 * @param message
	 *            the message used by the Exception.
	 */
	public MotodevDbException(String message) {
		super(message);
	}

	/**
	 * Creates a new MotodevDbException object.
	 * 
	 * @param cause
	 *            the associated cause.
	 */
	public MotodevDbException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new MotodevDbException object.
	 * 
	 * @param message
	 *            the message used by the Exception.
	 * @param cause
	 *            the associated cause.
	 */
	public MotodevDbException(String message, Throwable cause) {
		super(message, cause);
	}

}
