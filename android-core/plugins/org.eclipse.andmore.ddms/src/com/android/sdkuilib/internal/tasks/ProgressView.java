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

import com.android.sdklib.internal.repository.ITask;
import com.android.sdklib.internal.repository.ITaskMonitor;
import com.android.sdklib.internal.repository.UserCredentials;
import com.android.sdkuilib.ui.AuthenticationDialog;
import com.android.sdkuilib.ui.GridDialog;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

import java.util.concurrent.atomic.AtomicReference;


/**
 * Implements a "view" that uses an existing progress bar, status button and
 * status text to display a {@link ITaskMonitor}.
 */
public final class ProgressView implements IProgressUiProvider {

    private static enum State {
        /** View created but there's no task running. Next state can only be ACTIVE. */
        IDLE,
        /** A task is currently running. Next state is either STOP_PENDING or IDLE. */
        ACTIVE,
        /** Stop button has been clicked. Waiting for thread to finish. Next state is IDLE. */
        STOP_PENDING,
    }

    /** The current mode of operation of the dialog. */
    private State mState = State.IDLE;



    // UI fields
    private final Label mLabel;
    private final Control mStopButton;
    private final ProgressBar mProgressBar;

    /** Logger object. Cannot not be null. */
    private final ILogUiProvider mLog;

    /**
     * Creates a new {@link ProgressView} object, a simple "holder" for the various
     * widgets used to display and update a progress + status bar.
     *
     * @param label The label to display titles of status updates (e.g. task titles and
     *      calls to {@link #setDescription(String)}.) Must not be null.
     * @param progressBar The progress bar to update during a task. Must not be null.
     * @param stopButton The stop button. It will be disabled when there's no task that can
     *      be interrupted. A selection listener will be attached to it. Optional. Can be null.
     * @param log A <em>mandatory</em> logger object that will be used to report all the log.
     *      Must not be null.
     */
    public ProgressView(
            Label label,
            ProgressBar progressBar,
            Control stopButton,
            ILogUiProvider log) {
        mLabel = label;
        mProgressBar = progressBar;
        mLog = log;
        mProgressBar.setEnabled(false);

        mStopButton = stopButton;
        if (mStopButton != null) {
            mStopButton.addListener(SWT.Selection, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    if (mState == State.ACTIVE) {
                        changeState(State.STOP_PENDING);
                    }
                }
            });
        }
    }

    /**
     * Starts the task and block till it's either finished or canceled.
     * This can be called from a non-UI thread safely.
     * <p/>
     * When a task is started from within a monitor, it reuses the thread
     * from the parent. Otherwise it starts a new thread and runs it own
     * UI loop. This means the task can perform UI operations using
     * {@link Display#asyncExec(Runnable)}.
     * <p/>
     * In either case, the method only returns when the task has finished.
     */
    public void startTask(
            final String title,
            final ITaskMonitor parentMonitor,
            final ITask task) {
        if (task != null) {
            try {
                if (parentMonitor == null && !mProgressBar.isDisposed()) {
                    mLabel.setText(title);
                    mProgressBar.setSelection(0);
                    mProgressBar.setEnabled(true);
                    changeState(ProgressView.State.ACTIVE);
                }

                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        if (parentMonitor == null) {
                            task.run(new TaskMonitorImpl(ProgressView.this));

                        } else {
                            // Use all the reminder of the parent monitor.
                            if (parentMonitor.getProgressMax() == 0) {
                                parentMonitor.setProgressMax(1);
                            }
                            ITaskMonitor sub = parentMonitor.createSubMonitor(
                                    parentMonitor.getProgressMax() - parentMonitor.getProgress());
                            try {
                                task.run(sub);
                            } finally {
                                int delta =
                                    sub.getProgressMax() - sub.getProgress();
                                if (delta > 0) {
                                    sub.incProgress(delta);
                                }
                            }
                        }
                    }
                };

                // If for some reason the UI has been disposed, just abort the thread.
                if (mProgressBar.isDisposed()) {
                    return;
                }

                if (TaskMonitorImpl.isTaskMonitorImpl(parentMonitor)) {
                    // If there's a parent monitor and it's our own class, we know this parent
                    // is already running a thread and the base one is running an event loop.
                    // We should thus not run a second event loop and we can process the
                    // runnable right here instead of spawning a thread inside the thread.
                    r.run();

                } else {
                    // No parent monitor. This is the first one so we need a thread and
                    // we need to process UI events.

                    final Thread t = new Thread(r, title);
                    t.start();

                    // Process the app's event loop whilst we wait for the thread to finish
                    while (!mProgressBar.isDisposed() && t.isAlive()) {
                        Display display = mProgressBar.getDisplay();
                        if (!mProgressBar.isDisposed() && !display.readAndDispatch()) {
                            display.sleep();
                        }
                    }
                }
            } catch (Exception e) {
                // TODO log

            } finally {
                if (parentMonitor == null && !mProgressBar.isDisposed()) {
                    changeState(ProgressView.State.IDLE);
                    mProgressBar.setSelection(0);
                    mProgressBar.setEnabled(false);
                }
            }
        }
    }

    private void syncExec(final Widget widget, final Runnable runnable) {
        if (widget != null && !widget.isDisposed()) {
            widget.getDisplay().syncExec(new Runnable() {
                @Override
                public void run() {
                    // Check again whether the widget got disposed between the time where
                    // we requested the syncExec and the time it actually happened.
                    if (!widget.isDisposed()) {
                        runnable.run();
                    }
                }
            });
        }
    }

    private void changeState(State state) {
        if (mState != null ) {
            mState = state;
        }

        syncExec(mStopButton, new Runnable() {
            @Override
            public void run() {
                mStopButton.setEnabled(mState == State.ACTIVE);
            }
        });

    }

    // --- Implementation of ITaskUiProvider ---

    @Override
    public boolean isCancelRequested() {
        return mState != State.ACTIVE;
    }

    /**
     * Sets the description in the current task dialog.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void setDescription(final String description) {
        syncExec(mLabel, new Runnable() {
            @Override
            public void run() {
                mLabel.setText(description);
            }
        });

        mLog.setDescription(description);
    }

    /**
     * Logs a "normal" information line.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void log(String log) {
        mLog.log(log);
    }

    /**
     * Logs an "error" information line.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void logError(String log) {
        mLog.logError(log);
    }

    /**
     * Logs a "verbose" information line, that is extra details which are typically
     * not that useful for the end-user and might be hidden until explicitly shown.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void logVerbose(String log) {
        mLog.logVerbose(log);
    }

    /**
     * Sets the max value of the progress bar.
     * This method can be invoked from a non-UI thread.
     *
     * @see ProgressBar#setMaximum(int)
     */
    @Override
    public void setProgressMax(final int max) {
        syncExec(mProgressBar, new Runnable() {
            @Override
            public void run() {
                mProgressBar.setMaximum(max);
            }
        });
    }

    /**
     * Sets the current value of the progress bar.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void setProgress(final int value) {
        syncExec(mProgressBar, new Runnable() {
            @Override
            public void run() {
                mProgressBar.setSelection(value);
            }
        });
    }

    /**
     * Returns the current value of the progress bar,
     * between 0 and up to {@link #setProgressMax(int)} - 1.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public int getProgress() {
        final int[] result = new int[] { 0 };

        if (!mProgressBar.isDisposed()) {
            mProgressBar.getDisplay().syncExec(new Runnable() {
                @Override
                public void run() {
                    if (!mProgressBar.isDisposed()) {
                        result[0] = mProgressBar.getSelection();
                    }
                }
            });
        }

        return result[0];
    }

    @Override
    public boolean displayPrompt(final String title, final String message) {
        final boolean[] result = new boolean[] { false };

        syncExec(mProgressBar, new Runnable() {
            @Override
            public void run() {
                Shell shell = mProgressBar.getShell();
                result[0] = MessageDialog.openQuestion(shell, title, message);
            }
        });

        return result[0];
    }

    /**
     * This method opens a pop-up window which requests for User Credentials.
     *
     * @param title The title of the window.
     * @param message The message to displayed in the login/password window.
     * @return Returns user provided credentials.
     *         If operation is <b>canceled</b> by user the return value must be <b>null</b>.
     * @see ITaskMonitor#displayLoginCredentialsPrompt(String, String)
     */
    @Override
    public UserCredentials
            displayLoginCredentialsPrompt(final String title, final String message) {
        final AtomicReference<UserCredentials> result = new AtomicReference<UserCredentials>(null);

        // open dialog and request login and password
        syncExec(mProgressBar, new Runnable() {
            @Override
            public void run() {
                Shell shell = mProgressBar.getShell();
                AuthenticationDialog authenticationDialog = new AuthenticationDialog(shell,
                        title,
                        message);
                int dlgResult = authenticationDialog.open();
                if (dlgResult == GridDialog.OK) {
                    result.set(new UserCredentials(
                        authenticationDialog.getLogin(),
                        authenticationDialog.getPassword(),
                        authenticationDialog.getWorkstation(),
                        authenticationDialog.getDomain()));
                }
            }
        });

        return result.get();
    }
}

