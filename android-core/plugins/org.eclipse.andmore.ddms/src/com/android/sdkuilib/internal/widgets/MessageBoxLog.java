/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.annotations.NonNull;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;


/**
 * Collects all log and displays it in a message box dialog.
 * <p/>
 * This is good if only a few lines of log are expected.
 * If you pass <var>logErrorsOnly</var> to the constructor, the message box
 * will be shown only if errors were generated, which is the typical use-case.
 * <p/>
 * To use this: </br>
 * - Construct a new {@link MessageBoxLog}. </br>
 * - Pass the logger to the action. </br>
 * - Once the action is completed, call {@link #displayResult(boolean)}
 *   indicating whether the operation was successful or not.
 *
 * When <var>logErrorsOnly</var> is true, if the operation was not successful or errors
 * were generated, this will display the message box.
 */
public final class MessageBoxLog implements IMessageBoxLogger {

    final ArrayList<String> logMessages = new ArrayList<String>();
    private final String mMessage;
    private final Display mDisplay;
    private final boolean mLogErrorsOnly;

    /**
     * Creates a logger that will capture all logs and eventually display them
     * in a simple message box.
     *
     * @param message
     * @param display
     * @param logErrorsOnly
     */
    public MessageBoxLog(String message, Display display, boolean logErrorsOnly) {
        mMessage = message;
        mDisplay = display;
        mLogErrorsOnly = logErrorsOnly;
    }

    @Override
    public void error(Throwable throwable, String errorFormat, Object... arg) {
        if (errorFormat != null) {
            logMessages.add(String.format("Error: " + errorFormat, arg));
        }

        if (throwable != null) {
            logMessages.add(throwable.getMessage());
        }
    }

    @Override
    public void warning(@NonNull String warningFormat, Object... arg) {
        if (!mLogErrorsOnly) {
            logMessages.add(String.format("Warning: " + warningFormat, arg));
        }
    }

    @Override
    public void info(@NonNull String msgFormat, Object... arg) {
        if (!mLogErrorsOnly) {
            logMessages.add(String.format(msgFormat, arg));
        }
    }

    @Override
    public void verbose(@NonNull String msgFormat, Object... arg) {
        if (!mLogErrorsOnly) {
            logMessages.add(String.format(msgFormat, arg));
        }
    }

    /**
     * Displays the log if anything was captured.
     * <p/>
     * @param success Used only when the logger was constructed with <var>logErrorsOnly</var>==true.
     * In this case the dialog will only be shown either if success if false or some errors
     * where captured.
     */
    @Override
    public void displayResult(final boolean success) {
        if (logMessages.size() > 0) {
            final StringBuilder sb = new StringBuilder(mMessage + "\n\n");
            for (String msg : logMessages) {
                if (msg.length() > 0) {
                    if (msg.charAt(0) != '\n') {
                        int n = sb.length();
                        if (n > 0 && sb.charAt(n-1) != '\n') {
                            sb.append('\n');
                        }
                    }
                    sb.append(msg);
                }
            }

            // display the message
            // dialog box only run in ui thread..
            if (mDisplay != null && !mDisplay.isDisposed()) {
                mDisplay.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        // This is typically displayed at the end, so make sure the UI
                        // instances are not disposed.
                        Shell shell = null;
                        if (mDisplay != null && !mDisplay.isDisposed()) {
                            shell = mDisplay.getActiveShell();
                        }
                        if (shell == null || shell.isDisposed()) {
                            return;
                        }
                        // Use the success icon if the call indicates success.
                        // However just use the error icon if the logger was only recording errors.
                        if (success && !mLogErrorsOnly) {
                            MessageDialog.openInformation(shell, "Android Virtual Devices Manager",
                                    sb.toString());
                        } else {
                            MessageDialog.openError(shell, "Android Virtual Devices Manager",
                                        sb.toString());

                        }
                    }
                });
            }
        }
    }
}
