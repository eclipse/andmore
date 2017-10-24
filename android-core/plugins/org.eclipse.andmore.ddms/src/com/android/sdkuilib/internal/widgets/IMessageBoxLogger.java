/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.sdkuilib.internal.widgets;

import com.android.utils.ILogger;


/**
 * Collects all log and displays it in a message box dialog.
 * <p/>
 * This is good if only a few lines of log are expected.
 * If you pass <var>logErrorsOnly</var> to the constructor, the message box
 * will be shown only if errors were generated, which is the typical use-case.
 * <p/>
 * To use this: </br>
 * - Construct a new {@link IMessageBoxLogger}. </br>
 * - Pass the logger to the action. </br>
 * - Once the action is completed, call {@link #displayResult(boolean)}
 *   indicating whether the operation was successful or not.
 *
 * When <var>logErrorsOnly</var> is true, if the operation was not successful or errors
 * were generated, this will display the message box.
 */
public interface IMessageBoxLogger extends ILogger {

    /**
     * Displays the log if anything was captured.
     * <p/>
     * @param success Used only when the logger was constructed with <var>logErrorsOnly</var>==true.
     * In this case the dialog will only be shown either if success if false or some errors
     * where captured.
     */
    public void displayResult(final boolean success);
}
