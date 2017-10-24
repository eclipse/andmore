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

package com.android.sdkuilib.internal.repository.core;

import com.android.sdkuilib.internal.tasks.ILogUiProvider;
import com.android.utils.ILogger;


/**
 * Adapter that transform log from an {@link ILogUiProvider} to an {@link ILogger}.
 */
public final class SdkLogAdapter implements ILogUiProvider {

    private ILogger mSdkLog;
    private String mLastLogMsg;

    /**
     * Creates a new adapter to output log on the given {@code sdkLog}.
     *
     * @param sdkLog The logger to output to. Must not be null.
     */
    public SdkLogAdapter(ILogger sdkLog) {
        mSdkLog = sdkLog;
    }

    /**
     * Sets the description in the current task dialog.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void setDescription(final String description) {
        if (acceptLog(description)) {
            mSdkLog.info("%1$s", description);    //$NON-NLS-1$
        }
    }

    /**
     * Logs a "normal" information line.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void log(String log) {
        if (acceptLog(log)) {
            mSdkLog.info("  %1$s", log);          //$NON-NLS-1$
        }
    }

    /**
     * Logs an "error" information line.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void logError(String log) {
        if (acceptLog(log)) {
            mSdkLog.error(null, "  %1$s", log);     //$NON-NLS-1$
        }
    }

    /**
     * Logs a "verbose" information line, that is extra details which are typically
     * not that useful for the end-user and might be hidden until explicitly shown.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void logVerbose(String log) {
        if (acceptLog(log)) {
            mSdkLog.verbose("    %1$s", log);        //$NON-NLS-1$
        }
    }

    // ----

    /**
     * Filter messages displayed in the log: <br/>
     * - Messages with a % are typical part of a progress update and shouldn't be in the log. <br/>
     * - Messages that are the same as the same output message should be output a second time.
     *
     * @param msg The potential log line to print.
     * @return True if the log line should be printed, false otherwise.
     */
    private boolean acceptLog(String msg) {
        if (msg == null) {
            return false;
        }

        msg = msg.trim();
        if (msg.indexOf('%') != -1) {
            return false;
        }

        if (msg.equals(mLastLogMsg)) {
            return false;
        }

        mLastLogMsg = msg;
        return true;
    }
}
