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

package com.android.ddmuilib.logcat;

import com.android.ddmlib.Log.LogLevel;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;

/**
 * A JFace Column label provider for the LogCat log messages. It expects elements of type
 * {@link LogCatMessage}.
 */
public final class LogCatMessageLabelProvider extends ColumnLabelProvider {
    private static final int INDEX_LOGLEVEL = 0;
    private static final int INDEX_LOGTIME = 1;
    private static final int INDEX_PID = 2;
    private static final int INDEX_APPNAME = 3;
    private static final int INDEX_TAG = 4;
    private static final int INDEX_TEXT = 5;

    /* Default Colors for different log levels. */
    private static final Color INFO_MSG_COLOR =    new Color(null, 0, 127, 0);
    private static final Color DEBUG_MSG_COLOR =   new Color(null, 0, 0, 127);
    private static final Color ERROR_MSG_COLOR =   new Color(null, 255, 0, 0);
    private static final Color WARN_MSG_COLOR =    new Color(null, 255, 127, 0);
    private static final Color VERBOSE_MSG_COLOR = new Color(null, 0, 0, 0);

    /** Amount of pixels to shift the tooltip by. */
    private static final Point LOGCAT_TOOLTIP_SHIFT = new Point(10, 10);

    private Font mLogFont;
    private int mWrapWidth = 100;

    /**
     * Construct a column label provider for the logcat table.
     * @param font default font to use
     */
    public LogCatMessageLabelProvider(Font font) {
        mLogFont = font;
    }

    private String getCellText(LogCatMessage m, int columnIndex) {
        switch (columnIndex) {
            case INDEX_LOGLEVEL:
                return Character.toString(m.getLogLevel().getPriorityLetter());
            case INDEX_LOGTIME:
                return m.getTime();
            case INDEX_PID:
                return m.getPid();
            case INDEX_APPNAME:
                return m.getAppName();
            case INDEX_TAG:
                return m.getTag();
            case INDEX_TEXT:
                return m.getMessage();
            default:
                return "";
        }
    }

    @Override
    public void update(ViewerCell cell) {
        Object element = cell.getElement();
        if (!(element instanceof LogCatMessage)) {
            return;
        }
        LogCatMessage m = (LogCatMessage) element;

        String text = getCellText(m, cell.getColumnIndex());
        cell.setText(text);
        cell.setFont(mLogFont);
        cell.setForeground(getForegroundColor(m));
    }

    private Color getForegroundColor(LogCatMessage m) {
        LogLevel l = m.getLogLevel();

        if (l.equals(LogLevel.VERBOSE)) {
            return VERBOSE_MSG_COLOR;
        } else if (l.equals(LogLevel.INFO)) {
            return INFO_MSG_COLOR;
        } else if (l.equals(LogLevel.DEBUG)) {
            return DEBUG_MSG_COLOR;
        } else if (l.equals(LogLevel.ERROR)) {
            return ERROR_MSG_COLOR;
        } else if (l.equals(LogLevel.WARN)) {
            return WARN_MSG_COLOR;
        }

        return null;
    }

    public void setFont(Font preferredFont) {
        if (mLogFont != null) {
            mLogFont.dispose();
        }

        mLogFont = preferredFont;
    }

    public void setMinimumLengthForToolTips(int widthInChars) {
        mWrapWidth  = widthInChars;
    }

    /**
     * Obtain the tool tip to show for a particular logcat message.
     * We display a tool tip only for messages longer than the width set by
     * {@link #setMinimumLengthForToolTips(int)}.
     */
    @Override
    public String getToolTipText(Object element) {
        String text = element.toString();
        if (text.length() > mWrapWidth) {
            return text;
        } else {
            return null;
        }
    }

    @Override
    public Point getToolTipShift(Object object) {
        // The only reason we override this method is that the default shift amounts
        // don't seem to work on OS X Lion.
        return LOGCAT_TOOLTIP_SHIFT;
    }
}
