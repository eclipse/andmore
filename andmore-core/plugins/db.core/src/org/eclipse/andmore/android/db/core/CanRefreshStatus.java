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
package org.eclipse.andmore.android.db.core;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;

public class CanRefreshStatus extends MultiStatus {

	/**
	 * Status type severity (bit mask, value 16) indicating that a user
	 * interaction will be needed. in this case a Yes/no dialog must be
	 * displayed using this status messages as dialog message.
	 * 
	 * @see #getSeverity()
	 * @see #matches(int)
	 */
	public static final int ASK_USER = 0x10;

	/**
	 * Status type severity (bit mask, value 16) indicating that a user
	 * interaction will be needed. in this case a Yes/no/cancel dialog must be
	 * displayed using this status messages as dialog message. Yes/No response
	 * will be sent to refresh, cancel will cancel the refresh
	 * 
	 * @see #getSeverity()
	 * @see #matches(int)
	 */
	public static final int CANCELABLE = 0x20;

	/**
	 * Builds a new {@link CanRefreshStatus} instance
	 * 
	 * @param pluginId
	 *            the plug-in id.
	 * @param severity
	 *            the severity code.
	 * @param newChildren
	 *            new Status's children. (can't be null)
	 * @param message
	 *            The new status message.
	 * @param exception
	 *            The {@link Throwable} that caused the error, if any. (Can be
	 *            null).
	 */
	public CanRefreshStatus(int severity, String pluginId, IStatus[] newChildren, String message, Throwable exception) {
		super(pluginId, OK, newChildren, message, exception);
		setSeverity(severity);
	}

	/**
	 * Builds a new {@link CanRefreshStatus} instance
	 * 
	 * @param pluginId
	 *            the plug-in id.
	 * @param severity
	 *            the severity code.
	 * @param message
	 *            The new status message.
	 * @param exception
	 *            The {@link Throwable} that caused the error, if any. (Can be
	 *            null).
	 */
	public CanRefreshStatus(int severity, String pluginId, String message, Throwable exception) {
		super(pluginId, OK, message, exception);
		setSeverity(severity);
	}

	/**
	 * Builds a new {@link CanRefreshStatus} instance
	 * 
	 * @param pluginId
	 *            the plug-in id.
	 * @param severity
	 *            the severity code.
	 * @param newChildren
	 *            new Status's children. (can't be null)
	 * @param message
	 *            The new status message.
	 */
	public CanRefreshStatus(int severity, String pluginId, IStatus[] newChildren, String message) {
		super(pluginId, OK, newChildren, message, null);
		setSeverity(severity);
	}

	/**
	 * Builds a new {@link CanRefreshStatus} instance
	 * 
	 * @param pluginId
	 *            the plug-in id.
	 * @param severity
	 *            the severity code.
	 * @param message
	 *            The new status message.
	 */
	public CanRefreshStatus(int severity, String pluginId, String message) {
		super(pluginId, OK, message, null);
		setSeverity(severity);
	}

	/**
	 * Sets the severity.
	 *
	 * @param severity
	 *            the severity; one of <code>OK</code>, <code>ERROR</code>,
	 *            <code>INFO</code>, <code>WARNING</code>, or
	 *            <code>CANCEL</code>
	 */
	@Override
	protected void setSeverity(int severity) {
		Assert.isLegal((severity == OK) || (severity == ERROR) || (severity == WARNING) || (severity == INFO)
				|| (severity == CANCEL) || (severity == ASK_USER) || (severity == (ASK_USER | CANCELABLE)));

		if ((severity & ASK_USER) != 0) {
			super.setSeverity(WARNING);
		}

		setCode(severity);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Status#matches(int)
	 */
	@Override
	public boolean matches(int severityMask) {
		if ((severityMask & ASK_USER) != 0) {
			return (getCode() & severityMask) == severityMask;
		}
		return super.matches(severityMask);
	}

}
