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

package com.android.sdkuilib.ui;

import com.android.SdkConstants;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.util.HashMap;
import java.util.Map;

/**
 * A base class for an SWT Dialog.
 * <p/>
 * The base class offers the following goodies: <br/>
 * - Dialog is automatically centered on its parent. <br/>
 * - Dialog size is reused during the session. <br/>
 * - A simple API with an {@link #open()} method that returns a boolean. <br/>
 * <p/>
 * A typical usage is:
 * <pre>
 *   MyDialog extends SwtBaseDialog { ... }
 *   MyDialog d = new MyDialog(parentShell, "My Dialog Title");
 *   if (d.open()) {
 *      ...do something like refresh parent list view
 *   }
 * </pre>
 * We also have a JFace-base {@link GridDialog}.
 * The JFace dialog is good when you just want a typical OK/Cancel layout with the
 * buttons all managed for you.
 * This SWT base dialog has little decoration.
 * It's up to you to manage whatever buttons you want, if any.
 */
public abstract class SwtBaseDialog extends Dialog {

    /**
     * Min Y location for dialog. Need to deal with the menu bar on mac os.
     */
    private final static int MIN_Y =
        SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_DARWIN ? 20 : 0;

    /** Last dialog size for this session, different for each dialog class. */
    private static Map<Class<?>, Point> sLastSizeMap = new HashMap<Class<?>, Point>();

    private volatile boolean mQuitRequested = false;
    private boolean mReturnValue;
    private Shell mShell;

    /**
     * Create the dialog.
     *
     * @param parent The parent's shell
     * @param title The dialog title. Can be null.
     */
    public SwtBaseDialog(Shell parent, int swtStyle, String title) {
        super(parent, swtStyle);
        if (title != null) {
            setText(title);
        }
    }

    /**
     * Open the dialog.
     *
     * @return The last value set using {@link #setReturnValue(boolean)} or false by default.
     */
    public boolean open() {
        if (!mQuitRequested) {
            createShell();
        }
        if (!mQuitRequested) {
            createContents();
        }
        if (!mQuitRequested) {
            positionShell();
        }
        if (!mQuitRequested) {
            postCreate();
        }
        if (!mQuitRequested) {
            mShell.open();
            mShell.layout();
            eventLoop();
        }

        return mReturnValue;
    }

    /**
     * Creates the shell for this dialog.
     * The default shell has a size of 450x300, which is also its minimum size.
     * You might want to override these values.
     * <p/>
     * Called before {@link #createContents()}.
     */
    protected void createShell() {
        mShell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
        mShell.setMinimumSize(new Point(450, 300));
        mShell.setSize(450, 300);
        if (getText() != null) {
            mShell.setText(getText());
        }
        mShell.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                saveSize();
            }
        });
    }

    /**
     * Creates the content and attaches it to the current shell (cf. {@link #getShell()}).
     * <p/>
     * Derived classes should consider creating the UI here and initializing their
     * state in {@link #postCreate()}.
     */
    protected abstract void createContents();

    /**
     * Called after {@link #createContents()} and after {@link #positionShell()}
     * just before the dialog is actually shown on screen.
     * <p/>
     * Derived classes should consider creating the UI in {@link #createContents()} and
     * initialize it here.
     */
    protected abstract void postCreate();

    /**
     * Run the event loop.
     * This is called from {@link #open()} after {@link #postCreate()} and
     * after the window has been shown on screen.
     * Derived classes might want to use this as a place to start automated
     * tasks that will update the UI.
     */
    protected void eventLoop() {
        Display display = getParent().getDisplay();
        while (!mQuitRequested && !mShell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    /**
     * Returns the current value that {@link #open()} will return to the caller.
     * Default is false.
     */
    protected boolean getReturnValue() {
        return mReturnValue;
    }

    /**
     * Sets the value that {@link #open()} will return to the caller.
     * @param returnValue The new value to be returned by {@link #open()}.
     */
    protected void setReturnValue(boolean returnValue) {
        mReturnValue = returnValue;
    }

    /**
     * Returns the shell created by {@link #createShell()}.
     * @return The current {@link Shell}.
     */
    protected Shell getShell() {
        return mShell;
    }

    /**
     * Saves the dialog size and close the dialog.
     * The {@link #open()} method will given return value (see {@link #setReturnValue(boolean)}.
     * <p/>
     * It's safe to call this method before the shell is initialized,
     * in which case the dialog will close as soon as possible.
     */
    protected void close() {
        if (mShell != null && !mShell.isDisposed()) {
            saveSize();
            getShell().close();
        }
        mQuitRequested = true;
    }

    //-------

    /**
     * Centers the dialog in its parent shell.
     */
    private void positionShell() {
        // Centers the dialog in its parent shell
        Shell child = mShell;
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
            Point childSize = sLastSizeMap.get(this.getClass());
            if (childSize == null) {
                childSize = child.getSize();
            }
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

    private void saveSize() {
        if (mShell != null && !mShell.isDisposed()) {
            sLastSizeMap.put(this.getClass(), mShell.getSize());
        }
    }

}
