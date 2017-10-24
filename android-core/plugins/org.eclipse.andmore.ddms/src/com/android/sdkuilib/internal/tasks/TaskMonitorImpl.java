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

import com.android.annotations.NonNull;
import com.android.sdklib.internal.repository.ITaskMonitor;
import com.android.sdklib.internal.repository.UserCredentials;

/**
 * Internal class that implements the logic of an {@link ITaskMonitor}.
 * It doesn't deal with any UI directly. Instead it delegates the UI to
 * the provided {@link IProgressUiProvider}.
 */
class TaskMonitorImpl implements ITaskMonitor {

    private static final double MAX_COUNT = 10000.0;

    private interface ISubTaskMonitor extends ITaskMonitor {
        public void subIncProgress(double realDelta);
    }

    private double mIncCoef = 0;
    private double mValue = 0;
    private final IProgressUiProvider mUi;

    /**
     * Returns true if the given {@code monitor} is an instance of {@link TaskMonitorImpl}
     * or its private SubTaskMonitor.
     */
    public static boolean isTaskMonitorImpl(ITaskMonitor monitor) {
        return monitor instanceof TaskMonitorImpl || monitor instanceof SubTaskMonitor;
    }

    /**
     * Constructs a new {@link TaskMonitorImpl} that relies on the given
     * {@link IProgressUiProvider} to change the user interface.
     * @param ui The {@link IProgressUiProvider}. Cannot be null.
     */
    public TaskMonitorImpl(IProgressUiProvider ui) {
        mUi = ui;
    }

    /** Returns the {@link IProgressUiProvider} passed to the constructor. */
    public IProgressUiProvider getUiProvider() {
        return mUi;
    }

    /**
     * Sets the description in the current task dialog.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void setDescription(String format, Object... args) {
        final String text = String.format(format, args);
        mUi.setDescription(text);
    }

    /**
     * Logs a "normal" information line.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void log(String format, Object... args) {
        String text = String.format(format, args);
        mUi.log(text);
    }

    /**
     * Logs an "error" information line.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void logError(String format, Object... args) {
        String text = String.format(format, args);
        mUi.logError(text);
    }

    /**
     * Logs a "verbose" information line, that is extra details which are typically
     * not that useful for the end-user and might be hidden until explicitly shown.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void logVerbose(String format, Object... args) {
        String text = String.format(format, args);
        mUi.logVerbose(text);
    }

    /**
     * Sets the max value of the progress bar.
     * This method can be invoked from a non-UI thread.
     *
     * Weird things will happen if setProgressMax is called multiple times
     * *after* {@link #incProgress(int)}: we don't try to adjust it on the
     * fly.
     */
    @Override
    public void setProgressMax(int max) {
        assert max > 0;
        // Always set the dialog's progress max to 10k since it only handles
        // integers and we want to have a better inner granularity. Instead
        // we use the max to compute a coefficient for inc deltas.
        mUi.setProgressMax((int) MAX_COUNT);
        mIncCoef = max > 0 ? MAX_COUNT / max : 0;
        assert mIncCoef > 0;
    }

    @Override
    public int getProgressMax() {
        return mIncCoef > 0 ? (int) (MAX_COUNT / mIncCoef) : 0;
    }

    /**
     * Increments the current value of the progress bar.
     *
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void incProgress(int delta) {
        if (delta > 0 && mIncCoef > 0) {
            internalIncProgress(delta * mIncCoef);
        }
    }

    private void internalIncProgress(double realDelta) {
        mValue += realDelta;
        mUi.setProgress((int)mValue);
    }

    /**
     * Returns the current value of the progress bar,
     * between 0 and up to {@link #setProgressMax(int)} - 1.
     *
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public int getProgress() {
        // mIncCoef is 0 if setProgressMax hasn't been used yet.
        return mIncCoef > 0 ? (int)(mUi.getProgress() / mIncCoef) : 0;
    }

    /**
     * Returns true if the "Cancel" button was selected.
     * It is up to the task thread to pool this and exit.
     */
    @Override
    public boolean isCancelRequested() {
        return mUi.isCancelRequested();
    }

    /**
     * Displays a yes/no question dialog box.
     *
     * This implementation allow this to be called from any thread, it
     * makes sure the dialog is opened synchronously in the ui thread.
     *
     * @param title The title of the dialog box
     * @param message The error message
     * @return true if YES was clicked.
     */
    @Override
    public boolean displayPrompt(final String title, final String message) {
        return mUi.displayPrompt(title, message);
    }

    /**
     * Displays a Login/Password dialog. This implementation allows this method to be
     * called from any thread, it makes sure the dialog is opened synchronously
     * in the ui thread.
     *
     * @param title The title of the dialog box
     * @param message Message to be displayed
     * @return Pair with entered login/password. Login is always the first
     *         element and Password is always the second. If any error occurs a
     *         pair with empty strings is returned.
     */
    @Override
    public UserCredentials displayLoginCredentialsPrompt(String title, String message) {
        return mUi.displayLoginCredentialsPrompt(title, message);
    }

    /**
     * Creates a sub-monitor that will use up to tickCount on the progress bar.
     * tickCount must be 1 or more.
     */
    @Override
    public ITaskMonitor createSubMonitor(int tickCount) {
        assert mIncCoef > 0;
        assert tickCount > 0;
        return new SubTaskMonitor(this, null, mValue, tickCount * mIncCoef);
    }

    // ----- ILogger interface ----

    @Override
    public void error(Throwable throwable, String errorFormat, Object... arg) {
        if (errorFormat != null) {
            logError("Error: " + errorFormat, arg);
        }

        if (throwable != null) {
            logError("%s", throwable.getMessage()); //$NON-NLS-1$
        }
    }

    @Override
    public void warning(@NonNull String warningFormat, Object... arg) {
        log("Warning: " + warningFormat, arg);
    }

    @Override
    public void info(@NonNull String msgFormat, Object... arg) {
        log(msgFormat, arg);
    }

    @Override
    public void verbose(@NonNull String msgFormat, Object... arg) {
        log(msgFormat, arg);
    }

    // ----- Sub Monitor -----

    private static class SubTaskMonitor implements ISubTaskMonitor {

        private final TaskMonitorImpl mRoot;
        private final ISubTaskMonitor mParent;
        private final double mStart;
        private final double mSpan;
        private double mSubValue;
        private double mSubCoef;

        /**
         * Creates a new sub task monitor which will work for the given range [start, start+span]
         * in its parent.
         *
         * @param taskMonitor The ProgressTask root
         * @param parent The immediate parent. Can be the null or another sub task monitor.
         * @param start The start value in the root's coordinates
         * @param span The span value in the root's coordinates
         */
        public SubTaskMonitor(TaskMonitorImpl taskMonitor,
                ISubTaskMonitor parent,
                double start,
                double span) {
            mRoot = taskMonitor;
            mParent = parent;
            mStart = start;
            mSpan = span;
            mSubValue = start;
        }

        @Override
        public boolean isCancelRequested() {
            return mRoot.isCancelRequested();
        }

        @Override
        public void setDescription(String format, Object... args) {
            mRoot.setDescription(format, args);
        }

        @Override
        public void log(String format, Object... args) {
            mRoot.log(format, args);
        }

        @Override
        public void logError(String format, Object... args) {
            mRoot.logError(format, args);
        }

        @Override
        public void logVerbose(String format, Object... args) {
            mRoot.logVerbose(format, args);
        }

        @Override
        public void setProgressMax(int max) {
            assert max > 0;
            mSubCoef = max > 0 ? mSpan / max : 0;
            assert mSubCoef > 0;
        }

        @Override
        public int getProgressMax() {
            return mSubCoef > 0 ? (int) (mSpan / mSubCoef) : 0;
        }

        @Override
        public int getProgress() {
            // subCoef can be 0 if setProgressMax() and incProgress() haven't been called yet
            assert mSubValue == mStart || mSubCoef > 0;
            return mSubCoef > 0 ? (int)((mSubValue - mStart) / mSubCoef) : 0;
        }

        @Override
        public void incProgress(int delta) {
            if (delta > 0 && mSubCoef > 0) {
                subIncProgress(delta * mSubCoef);
            }
        }

        @Override
        public void subIncProgress(double realDelta) {
            mSubValue += realDelta;
            if (mParent != null) {
                mParent.subIncProgress(realDelta);
            } else {
                mRoot.internalIncProgress(realDelta);
            }
        }

        @Override
        public boolean displayPrompt(String title, String message) {
            return mRoot.displayPrompt(title, message);
        }

        @Override
        public UserCredentials displayLoginCredentialsPrompt(String title, String message) {
            return mRoot.displayLoginCredentialsPrompt(title, message);
        }

        @Override
        public ITaskMonitor createSubMonitor(int tickCount) {
            assert mSubCoef > 0;
            assert tickCount > 0;
            return new SubTaskMonitor(mRoot,
                    this,
                    mSubValue,
                    tickCount * mSubCoef);
        }

        // ----- ILogger interface ----

        @Override
        public void error(Throwable throwable, String errorFormat, Object... arg) {
            mRoot.error(throwable, errorFormat, arg);
        }

        @Override
        public void warning(@NonNull String warningFormat, Object... arg) {
            mRoot.warning(warningFormat, arg);
        }

        @Override
        public void info(@NonNull String msgFormat, Object... arg) {
            mRoot.info(msgFormat, arg);
        }

        @Override
        public void verbose(@NonNull String msgFormat, Object... arg) {
            mRoot.verbose(msgFormat, arg);
        }
    }
}
