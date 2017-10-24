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

package com.android.sdkuilib.internal.tasks;


/**
 * Interface for a user interface that displays the log from a task monitor.
 */
public interface ILogUiProvider {

    /**
     * Sets the description in the current task dialog.
     * This method can be invoked from a non-UI thread.
     */
    public abstract void setDescription(String description);

    /**
     * Logs a "normal" information line.
     * This method can be invoked from a non-UI thread.
     */
    public abstract void log(String log);

    /**
     * Logs an "error" information line.
     * This method can be invoked from a non-UI thread.
     */
    public abstract void logError(String log);

    /**
     * Logs a "verbose" information line, that is extra details which are typically
     * not that useful for the end-user and might be hidden until explicitly shown.
     * This method can be invoked from a non-UI thread.
     */
    public abstract void logVerbose(String log);

}
