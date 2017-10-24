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

package com.android.sdkuilib.internal.tasks;

import com.android.SdkConstants;
import com.android.sdklib.internal.repository.ITaskMonitor;
import com.android.sdklib.internal.repository.UserCredentials;
import com.android.sdkuilib.ui.AuthenticationDialog;
import com.android.sdkuilib.ui.GridDialog;
import com.android.utils.Pair;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


/**
 * Implements a {@link ProgressTaskDialog}, used by the {@link ProgressTask} class.
 * This separates the dialog UI from the task logic.
 *
 * Note: this does not implement the {@link ITaskMonitor} interface to avoid confusing
 * SWT Designer.
 */
final class ProgressTaskDialog extends Dialog implements IProgressUiProvider {

    /**
     * Min Y location for dialog. Need to deal with the menu bar on mac os.
     */
    private final static int MIN_Y = SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_DARWIN ?
            20 : 0;

    private static enum CancelMode {
        /** Cancel button says "Cancel" and is enabled. Waiting for user to cancel. */
        ACTIVE,
        /** Cancel button has been clicked. Waiting for thread to finish. */
        CANCEL_PENDING,
        /** Close pending. Close button clicked or thread finished but there were some
         * messages so the user needs to manually close. */
        CLOSE_MANUAL,
        /** Close button clicked or thread finished. The window will automatically close. */
        CLOSE_AUTO
    }

    /** The current mode of operation of the dialog. */
    private CancelMode mCancelMode = CancelMode.ACTIVE;

    /** Last dialog size for this session. */
    private static Point sLastSize;


    // UI fields
    private Shell mDialogShell;
    private Composite mRootComposite;
    private Label mLabel;
    private ProgressBar mProgressBar;
    private Button mCancelButton;
    private Text mResultText;


    /**
     * Create the dialog.
     * @param parent Parent container
     */
    public ProgressTaskDialog(Shell parent) {
        super(parent, SWT.APPLICATION_MODAL);
    }

    /**
     * Open the dialog and blocks till it gets closed
     * @param taskThread The thread to run the task. Cannot be null.
     */
    public void open(Thread taskThread) {
        createContents();
        positionShell();                        //$hide$ (hide from SWT designer)
        mDialogShell.open();
        mDialogShell.layout();

        startThread(taskThread);                //$hide$ (hide from SWT designer)

        Display display = getParent().getDisplay();
        while (!mDialogShell.isDisposed() && mCancelMode != CancelMode.CLOSE_AUTO) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        setCancelRequested();       //$hide$ (hide from SWT designer)

        if (!mDialogShell.isDisposed()) {
            sLastSize = mDialogShell.getSize();
            mDialogShell.close();
        }
    }

    /**
     * Create contents of the dialog.
     */
    private void createContents() {
        mDialogShell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.RESIZE);
        mDialogShell.addShellListener(new ShellAdapter() {
            @Override
            public void shellClosed(ShellEvent e) {
                onShellClosed(e);
            }
        });
        mDialogShell.setLayout(new GridLayout(1, false));
        mDialogShell.setSize(450, 300);
        mDialogShell.setText(getText());

        mRootComposite = new Composite(mDialogShell, SWT.NONE);
        mRootComposite.setLayout(new GridLayout(2, false));
        mRootComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        mLabel = new Label(mRootComposite, SWT.NONE);
        mLabel.setText("Task");
        mLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        mProgressBar = new ProgressBar(mRootComposite, SWT.NONE);
        mProgressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mCancelButton = new Button(mRootComposite, SWT.NONE);
        mCancelButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mCancelButton.setText("Cancel");

        mCancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onCancelSelected();  //$hide$
            }
        });

        mResultText = new Text(mRootComposite,
                SWT.BORDER | SWT.READ_ONLY | SWT.WRAP |
                SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
        mResultText.setEditable(true);
        mResultText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    }

    // -- End of UI, Start of internal logic ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    @Override
    public boolean isCancelRequested() {
        return mCancelMode != CancelMode.ACTIVE;
    }

    /**
     * Sets the mode to cancel pending.
     * The first time this grays the cancel button, to let the user know that the
     * cancel operation is pending.
     */
    public void setCancelRequested() {
        if (!mDialogShell.isDisposed()) {
            // The dialog is not disposed, make sure to run all this in the UI thread
            // and lock on the cancel button mode.
            mDialogShell.getDisplay().syncExec(new Runnable() {

                @Override
                public void run() {
                    synchronized (mCancelMode) {
                        if (mCancelMode == CancelMode.ACTIVE) {
                            mCancelMode = CancelMode.CANCEL_PENDING;

                            if (!mCancelButton.isDisposed()) {
                                mCancelButton.setEnabled(false);
                            }
                        }
                    }
                }
            });
        } else {
            // The dialog is disposed. Just set the boolean. We shouldn't be here.
            if (mCancelMode == CancelMode.ACTIVE) {
                mCancelMode = CancelMode.CANCEL_PENDING;
            }
        }
    }

    /**
     * Sets the mode to close manual.
     * The first time, this also ungrays the pause button and converts it to a close button.
     */
    public void setManualCloseRequested() {
        if (!mDialogShell.isDisposed()) {
            // The dialog is not disposed, make sure to run all this in the UI thread
            // and lock on the cancel button mode.
            mDialogShell.getDisplay().syncExec(new Runnable() {

                @Override
                public void run() {
                    synchronized (mCancelMode) {
                        if (mCancelMode != CancelMode.CLOSE_MANUAL &&
                                mCancelMode != CancelMode.CLOSE_AUTO) {
                            mCancelMode = CancelMode.CLOSE_MANUAL;

                            if (!mCancelButton.isDisposed()) {
                                mCancelButton.setEnabled(true);
                                mCancelButton.setText("Close");
                            }
                        }
                    }
                }
            });
        } else {
            // The dialog is disposed. Just set the booleans. We shouldn't be here.
            if (mCancelMode != CancelMode.CLOSE_MANUAL &&
                    mCancelMode != CancelMode.CLOSE_AUTO) {
                mCancelMode = CancelMode.CLOSE_MANUAL;
            }
        }
    }

    /**
     * Sets the mode to close auto.
     * The main loop will just exit and close the shell at the first opportunity.
     */
    public void setAutoCloseRequested() {
        synchronized (mCancelMode) {
            if (mCancelMode != CancelMode.CLOSE_AUTO) {
                mCancelMode = CancelMode.CLOSE_AUTO;
            }
        }
    }

    /**
     * Callback invoked when the cancel button is selected.
     * When in closing mode, this simply closes the shell. Otherwise triggers a cancel.
     */
    private void onCancelSelected() {
        if (mCancelMode == CancelMode.CLOSE_MANUAL) {
            setAutoCloseRequested();
        } else {
            setCancelRequested();
        }
    }

    /**
     * Callback invoked when the shell is closed either by clicking the close button
     * on by calling shell.close().
     * This does the same thing as clicking the cancel/close button unless the mode is
     * to auto close in which case we should do nothing to let the shell close normally.
     */
    private void onShellClosed(ShellEvent e) {
        if (mCancelMode != CancelMode.CLOSE_AUTO) {
            e.doit = false; // don't close directly
            onCancelSelected();
        }
    }

    /**
     * Sets the description in the current task dialog.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void setDescription(final String description) {
        mDialogShell.getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                if (!mLabel.isDisposed()) {
                    mLabel.setText(description);
                }
            }
        });
    }

    /**
     * Adds to the log in the current task dialog.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void log(final String info) {
        if (!mDialogShell.isDisposed()) {
            mDialogShell.getDisplay().syncExec(new Runnable() {
                @Override
                public void run() {
                    if (!mResultText.isDisposed()) {
                        mResultText.setVisible(true);
                        String lastText = mResultText.getText();
                        if (lastText != null &&
                                lastText.length() > 0 &&
                                !lastText.endsWith("\n") &&     //$NON-NLS-1$
                                !info.startsWith("\n")) {       //$NON-NLS-1$
                            mResultText.append("\n");           //$NON-NLS-1$
                        }
                        mResultText.append(info);
                    }
                }
            });
        }
    }

    @Override
    public void logError(String info) {
        log(info);
    }

    @Override
    public void logVerbose(String info) {
        log(info);
    }

    /**
     * Sets the max value of the progress bar.
     * This method can be invoked from a non-UI thread.
     *
     * @see ProgressBar#setMaximum(int)
     */
    @Override
    public void setProgressMax(final int max) {
        if (!mDialogShell.isDisposed()) {
            mDialogShell.getDisplay().syncExec(new Runnable() {
                @Override
                public void run() {
                    if (!mProgressBar.isDisposed()) {
                        mProgressBar.setMaximum(max);
                    }
                }
            });
        }
    }

    /**
     * Sets the current value of the progress bar.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void setProgress(final int value) {
        if (!mDialogShell.isDisposed()) {
            mDialogShell.getDisplay().syncExec(new Runnable() {
                @Override
                public void run() {
                    if (!mProgressBar.isDisposed()) {
                        mProgressBar.setSelection(value);
                    }
                }
            });
        }
    }

    /**
     * Returns the current value of the progress bar,
     * between 0 and up to {@link #setProgressMax(int)} - 1.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public int getProgress() {
        final int[] result = new int[] { 0 };

        if (!mDialogShell.isDisposed()) {
            mDialogShell.getDisplay().syncExec(new Runnable() {
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

    /**
     * Display a yes/no question dialog box.
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
        Display display = mDialogShell.getDisplay();

        // we need to ask the user what he wants to do.
        final boolean[] result = new boolean[] { false };
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                result[0] = MessageDialog.openQuestion(mDialogShell, title, message);
            }
        });
        return result[0];
    }

    /**
     * This method opens a pop-up window which requests for User Login and
     * password.
     *
     * @param title The title of the window.
     * @param message The message to displayed in the login/password window.
     * @return Returns a {@link Pair} holding the entered login and password.
     *         The information must always be in the following order:
     *         Login,Password. So in order to retrieve the <b>login</b> callers
     *         should retrieve the first element, and the second value for the
     *         <b>password</b>.
     *         If operation is <b>canceled</b> by user the return value must be <b>null</b>.
     * @see ITaskMonitor#displayLoginCredentialsPrompt(String, String)
     */
    @Override
    public UserCredentials displayLoginCredentialsPrompt(
            final String title, final String message) {
        Display display = mDialogShell.getDisplay();

        // open dialog and request login and password
        GetUserCredentialsTask task = new GetUserCredentialsTask(mDialogShell, title, message);
        display.syncExec(task);

        return task.getUserCredentials();
    }

    private static class GetUserCredentialsTask implements Runnable {
        private UserCredentials mResult = null;

        private Shell mShell;
        private String mTitle;
        private String mMessage;

        public GetUserCredentialsTask(Shell shell, String title, String message) {
            mShell = shell;
            mTitle = title;
            mMessage = message;
        }

        @Override
        public void run() {
            AuthenticationDialog authenticationDialog = new AuthenticationDialog(mShell,
                        mTitle, mMessage);
            int dlgResult= authenticationDialog.open();
            if(dlgResult == GridDialog.OK) {
                mResult = new UserCredentials(
                        authenticationDialog.getLogin(),
                        authenticationDialog.getPassword(),
                        authenticationDialog.getWorkstation(),
                        authenticationDialog.getDomain());
            }
        }

        public UserCredentials getUserCredentials() {
            return mResult;
        }
    }

    /**
     * Starts the thread that runs the task.
     * This is deferred till the UI is created.
     */
    private void startThread(Thread taskThread) {
        if (taskThread != null) {
            taskThread.start();
        }
    }

    /**
     * Centers the dialog in its parent shell.
     */
    private void positionShell() {
        // Centers the dialog in its parent shell
        Shell child = mDialogShell;
        Shell parent = getParent();
        if (child != null && parent != null) {

            // get the parent client area with a location relative to the display
            Rectangle parentArea = parent.getClientArea();
            Point parentLoc = parent.getLocation();
            int px = parentLoc.x;
            int py = parentLoc.y;
            int pw = parentArea.width;
            int ph = parentArea.height;

            // Reuse the last size if there's one, otherwise use the default
            Point childSize = sLastSize != null ? sLastSize : child.getSize();
            int cw = childSize.x;
            int ch = childSize.y;

            int x = px + (pw - cw) / 2;
            if (x < 0) x = 0;

            int y = py + (ph - ch) / 2;
            if (y < MIN_Y) y = MIN_Y;

            child.setLocation(x, y);
            child.setSize(cw, ch);
        }
    }

    // End of hiding from SWT Designer
    //$hide<<$
}
