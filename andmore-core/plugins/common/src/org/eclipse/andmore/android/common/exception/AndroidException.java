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
package org.eclipse.andmore.android.common.exception;

/**
 * Represents the base exception to be used in MOTODEV Studio for Android
 */
public class AndroidException extends Exception {
	private static final long serialVersionUID = 1854601362994563511L;

	/**
	 * Creates a new AndroidException object.
	 */
	public AndroidException() {
	}

	/**
	 * Creates a new AndroidException object.
	 * 
	 * @param message
	 *            the message used by the Exception.
	 */
	public AndroidException(String message) {
		super(message);
	}

	/**
	 * Creates a new AndroidException object.
	 * 
	 * @param cause
	 *            the associated cause.
	 */
	public AndroidException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new AndroidException object.
	 * 
	 * @param message
	 *            the message used by the Exception.
	 * @param cause
	 *            the associated cause.
	 */
	public AndroidException(String message, Throwable cause) {
		super(message, cause);
	}
}
