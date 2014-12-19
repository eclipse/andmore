/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.internal.editors.layout.gle2;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * A dedicated tooltip used during gestures, for example to show the resize dimensions.
 * <p>
 * This is necessary because {@link org.eclipse.jface.window.ToolTip} causes flicker when
 * used to dynamically update the position and text of the tip, and it does not seem to
 * have setter methods to update the text or position without recreating the tip.
 */
public class GestureToolTip {
    /** Minimum number of milliseconds to wait between alignment changes */
    private static final int TIMEOUT_MS = 750;

    /**
     * The alpha to use for the tooltip window (which sadly will apply to the tooltip text
     * as well.)
     */
    private static final int SHELL_TRANSPARENCY = 220;

    /** The size of the font displayed in the tooltip */
    private static final int FONT_SIZE = 9;

    /** Horizontal delta from the mouse cursor to shift the tooltip by */
    private static final int OFFSET_X = 20;

    /** Vertical delta from the mouse cursor to shift the tooltip by */
    private static final int OFFSET_Y = 20;

    /** The label which displays the tooltip */
    private CLabel mLabel;

    /** The shell holding the tooltip */
    private Shell mShell;

    /** The font shown in the label; held here such that it can be disposed of after use */
    private Font mFont;

    /** Is the tooltip positioned below the given anchor? */
    private boolean mBelow;

    /** Is the tooltip positioned to the right of the given anchor? */
    private boolean mToRightOf;

    /** Is an alignment change pending? */
    private boolean mTimerPending;

    /** The new value for {@link #mBelow} when the timer expires */
    private boolean mPendingBelow;

    /** The new value for {@link #mToRightOf} when the timer expires */
    private boolean mPendingRight;

    /** The time stamp (from {@link System#currentTimeMillis()} of the last alignment change */
    private long mLastAlignmentTime;

    /**
     * Creates a new tooltip over the given parent with the given relative position.
     *
     * @param parent the parent control
     * @param below if true, display the tooltip below the mouse cursor otherwise above
     * @param toRightOf if true, display the tooltip to the right of the mouse cursor,
     *            otherwise to the left
     */
    public GestureToolTip(Composite parent, boolean below, boolean toRightOf) {
        mBelow = below;
        mToRightOf = toRightOf;
        mLastAlignmentTime = System.currentTimeMillis();

        mShell = new Shell(parent.getShell(), SWT.ON_TOP | SWT.TOOL | SWT.NO_FOCUS);
        mShell.setLayout(new FillLayout());
        mShell.setAlpha(SHELL_TRANSPARENCY);

        Display display = parent.getDisplay();
        mLabel = new CLabel(mShell, SWT.SHADOW_NONE);
        mLabel.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        mLabel.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));

        Font systemFont = display.getSystemFont();
        FontData[] fd = systemFont.getFontData();
        for (int i = 0; i < fd.length; i++) {
            fd[i].setHeight(FONT_SIZE);
        }
        mFont = new Font(display, fd);
        mLabel.setFont(mFont);

        mShell.setVisible(false);
    }

    /**
     * Show the tooltip at the given position and with the given text. Note that the
     * position may not be applied immediately; to prevent flicker alignment changes
     * are queued up with a timer (unless it's been a while since the last change, in
     * which case the update is applied immediately.)
     *
     * @param text the new text to be displayed
     * @param below if true, display the tooltip below the mouse cursor otherwise above
     * @param toRightOf if true, display the tooltip to the right of the mouse cursor,
     *            otherwise to the left
     */
    public void update(final String text, boolean below, boolean toRightOf) {
        // If the alignment has not changed recently, just apply the change immediately
        // instead of within a delay
        if (!mTimerPending && (below != mBelow || toRightOf != mToRightOf)
                && (System.currentTimeMillis() - mLastAlignmentTime >= TIMEOUT_MS)) {
            mBelow = below;
            mToRightOf = toRightOf;
            mLastAlignmentTime = System.currentTimeMillis();
        }

        Point location = mShell.getDisplay().getCursorLocation();

        mLabel.setText(text);

        // Pack the label to its minimum size -- unless we are positioning the tooltip
        // on the left. Because of the way SWT works (at least on the OSX) this sometimes
        // creates flicker, because when we switch to a longer string (such as when
        // switching from "52dp" to "wrap_content" during a resize) the window size will
        // change first, and then the location will update later - so there will be a
        // brief flash of the longer label before it is moved to the right position on the
        // left. To work around this, we simply pass false to pack such that it will reuse
        // its cached size, which in practice means that for labels on the right, the
        // label will grow but not shrink.
        // This workaround is disabled because it doesn't work well in Eclipse 3.5; the
        // labels don't grow when they should. Re-enable when we drop 3.5 support.
        //boolean changed = mToRightOf;
        boolean changed = true;

        mShell.pack(changed);
        Point size = mShell.getSize();

        // Position the tooltip to the left or right, and above or below, according
        // to the saved state of these flags, not the current parameters. We don't want
        // to flicker, instead we react on a timer to changes in alignment below.
        if (mBelow) {
            location.y += OFFSET_Y;
        } else {
            location.y -= OFFSET_Y;
            location.y -= size.y;
        }

        if (mToRightOf) {
            location.x += OFFSET_X;
        } else {
            location.x -= OFFSET_X;
            location.x -= size.x;
        }

        mShell.setLocation(location);

        if (!mShell.isVisible()) {
            mShell.setVisible(true);
        }

        // Has the orientation changed?
        mPendingBelow = below;
        mPendingRight = toRightOf;
        if (below != mBelow || toRightOf != mToRightOf) {
            // Yes, so schedule a timer (unless one is already scheduled)
            if (!mTimerPending) {
                mTimerPending = true;
                final Runnable timer = new Runnable() {
                    @Override
                    public void run() {
                        mTimerPending = false;
                        // Check whether the alignment is still different than the target
                        // (since we may change back and forth repeatedly during the timeout)
                        if (mBelow != mPendingBelow || mToRightOf != mPendingRight) {
                            mBelow = mPendingBelow;
                            mToRightOf = mPendingRight;
                            mLastAlignmentTime = System.currentTimeMillis();
                            if (mShell != null && mShell.isVisible()) {
                                update(text, mBelow, mToRightOf);
                            }
                        }
                    }
                };
                mShell.getDisplay().timerExec(TIMEOUT_MS, timer);
            }
        }
    }

    /** Hide the tooltip and dispose of any associated resources */
    public void dispose() {
        mShell.dispose();
        mFont.dispose();

        mShell = null;
        mFont = null;
        mLabel = null;
    }
}
