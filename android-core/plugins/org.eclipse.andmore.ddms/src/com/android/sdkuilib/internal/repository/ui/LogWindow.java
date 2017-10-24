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

package com.android.sdkuilib.internal.repository.ui;

import com.android.sdkuilib.internal.tasks.ILogUiProvider;
import com.android.sdkuilib.ui.GridDataBuilder;
import com.android.sdkuilib.ui.GridLayoutBuilder;
import com.android.utils.ILogger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;


/**
 * A floating log window that can be displayed or hidden by the main SDK Manager 2 window.
 * It displays a log of the sdk manager operation (listing, install, delete) including
 * any errors (e.g. network error or install/delete errors.)
 * <p/>
 * Since the SDK Manager will direct all log to this window, its purpose is to be
 * opened by the main window at startup and left open all the time. When not needed
 * the floating window is hidden but not closed. This way it can easily accumulate
 * all the log.
 */
class LogWindow implements ILogUiProvider {

    private Shell mParentShell;
    private Shell mShell;
    private Composite mRootComposite;
    private StyledText mStyledText;
    private Label mLogDescription;
    private Button mCloseButton;

    private final ILogger mSecondaryLog;
    private boolean mCloseRequested;
    private boolean mInitPosition = true;
    private String mLastLogMsg = null;

    private enum TextStyle {
        DEFAULT,
        TITLE,
        ERROR
    }

    /**
     * Creates the floating window. Callers should use {@link #open()} later.
     *
     * @param parentShell Parent container
     * @param secondaryLog An optional logger where messages will <em>also</em> be output.
     */
    public LogWindow(Shell parentShell, ILogger secondaryLog) {
        mParentShell = parentShell;
        mSecondaryLog = secondaryLog;
    }

    /**
     * For testing only. See {@link #open()} and {@link #close()} for normal usage.
     * @wbp.parser.entryPoint
     */
    void openBlocking() {
        open();
        Display display = Display.getDefault();
        while (!mShell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        close();
    }

    /**
     * Opens the window.
     * This call does not block and relies on the fact that the main window is
     * already running an SWT event dispatch loop.
     * Caller should use {@link #close()} later.
     */
    public void open() {
        createShell();
        createContents();
        mShell.open();
        mShell.layout();
        mShell.setVisible(false);
    }

    /**
     * Closes and <em>destroys</em> the window.
     * This must be called just before quitting the app.
     * <p/>
     * To simply hide/show the window, use {@link #setVisible(boolean)} instead.
     */
    public void close() {
        if (mShell != null && !mShell.isDisposed()) {
            mCloseRequested = true;
            mShell.close();
            mShell = null;
        }
    }

    /**
     * Determines whether the window is currently shown or not.
     *
     * @return True if the window is shown.
     */
    public boolean isVisible() {
        return mShell != null && !mShell.isDisposed() && mShell.isVisible();
    }

    /**
     * Toggles the window visibility.
     *
     * @param visible True to make the window visible, false to hide it.
     */
    public void setVisible(boolean visible) {
        if (mShell != null && !mShell.isDisposed()) {
            mShell.setVisible(visible);
            if (visible && mInitPosition) {
                mInitPosition = false;
                positionWindow();
            }
        }
    }

    private void createShell() {
        mShell = new Shell(mParentShell, SWT.SHELL_TRIM | SWT.TOOL);
        mShell.setMinimumSize(new Point(600, 300));
        mShell.setSize(450, 300);
        mShell.setText("Android SDK Manager Log");
        GridLayoutBuilder.create(mShell);

        mShell.addShellListener(new ShellAdapter() {
            @Override
            public void shellClosed(ShellEvent e) {
                if (!mCloseRequested) {
                    e.doit = false;
                    setVisible(false);
                }
            }
        });
    }

    /**
     * Create contents of the dialog.
     */
    private void createContents() {
        mRootComposite = new Composite(mShell, SWT.NONE);
        GridLayoutBuilder.create(mRootComposite).columns(2);
        GridDataBuilder.create(mRootComposite).fill().grab();

        mStyledText = new StyledText(mRootComposite,
                SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
        GridDataBuilder.create(mStyledText).hSpan(2).fill().grab();

        mLogDescription = new Label(mRootComposite, SWT.NONE);
        GridDataBuilder.create(mLogDescription).hFill().hGrab();

        mCloseButton = new Button(mRootComposite, SWT.NONE);
        mCloseButton.setText("Close");
        mCloseButton.setToolTipText("Closes the log window");
        mCloseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setVisible(false);  //$hide$
            }
        });
    }

    // --- Implementation of ILogUiProvider ---


    /**
     * Sets the description in the current task dialog.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void setDescription(final String description) {
        syncExec(mLogDescription, new Runnable() {
            @Override
            public void run() {
                mLogDescription.setText(description);

                if (acceptLog(description, true /*isDescription*/)) {
                    appendLine(TextStyle.TITLE, description);

                    if (mSecondaryLog != null) {
                        mSecondaryLog.info("%1$s", description);  //$NON-NLS-1$
                    }
                }
            }
        });
    }

    /**
     * Logs a "normal" information line.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void log(final String log) {
        if (acceptLog(log, false /*isDescription*/)) {
            syncExec(mLogDescription, new Runnable() {
                @Override
                public void run() {
                    appendLine(TextStyle.DEFAULT, log);
                }
            });

            if (mSecondaryLog != null) {
                mSecondaryLog.info("  %1$s", log);                //$NON-NLS-1$
            }
        }
    }

    /**
     * Logs an "error" information line.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void logError(final String log) {
        if (acceptLog(log, false /*isDescription*/)) {
            syncExec(mLogDescription, new Runnable() {
                @Override
                public void run() {
                    appendLine(TextStyle.ERROR, log);
                }
            });

            if (mSecondaryLog != null) {
                mSecondaryLog.error(null, "%1$s", log);             //$NON-NLS-1$
            }
        }
    }

    /**
     * Logs a "verbose" information line, that is extra details which are typically
     * not that useful for the end-user and might be hidden until explicitly shown.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void logVerbose(final String log) {
        if (acceptLog(log, false /*isDescription*/)) {
            syncExec(mLogDescription, new Runnable() {
                @Override
                public void run() {
                    appendLine(TextStyle.DEFAULT, "  " + log);      //$NON-NLS-1$
                }
            });

            if (mSecondaryLog != null) {
                mSecondaryLog.info("    %1$s", log);              //$NON-NLS-1$
            }
        }
    }


    // ----


    /**
     * Centers the dialog in its parent shell.
     */
    private void positionWindow() {
        // Centers the dialog in its parent shell
        Shell child = mShell;
        if (child != null && mParentShell != null) {
            // get the parent client area with a location relative to the display
            Rectangle parentArea = mParentShell.getClientArea();
            Point parentLoc = mParentShell.getLocation();
            int px = parentLoc.x;
            int py = parentLoc.y;
            int pw = parentArea.width;
            int ph = parentArea.height;

            Point childSize = child.getSize();
            int cw = Math.max(childSize.x, pw);
            int ch = childSize.y;

            int x = 30 + px + (pw - cw) / 2;
            if (x < 0) x = 0;

            int y = py + (ph - ch) / 2;
            if (y < py) y = py;

            child.setLocation(x, y);
            child.setSize(cw, ch);
        }
    }

    private void appendLine(TextStyle style, String text) {
        if (!text.endsWith("\n")) {                                 //$NON-NLS-1$
            text += '\n';
        }

        int start = mStyledText.getCharCount();

        if (style == TextStyle.DEFAULT) {
            mStyledText.append(text);

        } else {
            mStyledText.append(text);

            StyleRange sr = new StyleRange();
            sr.start = start;
            sr.length = text.length();
            sr.fontStyle = SWT.BOLD;
            if (style == TextStyle.ERROR) {
                sr.foreground = mStyledText.getDisplay().getSystemColor(SWT.COLOR_DARK_RED);
            }
            sr.underline = false;
            mStyledText.setStyleRange(sr);
        }

        // Scroll caret if it was already at the end before we added new text.
        // Ideally we would scroll if the scrollbar is at the bottom but we don't
        // have direct access to the scrollbar without overriding the SWT impl.
        if (mStyledText.getCaretOffset() >= start) {
            mStyledText.setSelection(mStyledText.getCharCount());
        }
    }


    private void syncExec(final Widget widget, final Runnable runnable) {
        if (widget != null && !widget.isDisposed()) {
            widget.getDisplay().syncExec(runnable);
        }
    }

    /**
     * Filter messages displayed in the log: <br/>
     * - Messages with a % are typical part of a progress update and shouldn't be in the log. <br/>
     * - Messages that are the same as the same output message should be output a second time.
     *
     * @param msg The potential log line to print.
     * @return True if the log line should be printed, false otherwise.
     */
    private boolean acceptLog(String msg, boolean isDescription) {
        if (msg == null) {
            return false;
        }

        msg = msg.trim();

        // Descriptions also have the download progress status (0..100%) which we want to avoid
        if (isDescription && msg.indexOf('%') != -1) {
            return false;
        }

        if (msg.equals(mLastLogMsg)) {
            return false;
        }

        mLastLogMsg = msg;
        return true;
    }
}
