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

import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A Filter for logcat messages. A filter can be constructed to match
 * different fields of a logcat message. It can then be queried to see if
 * a message matches the filter's settings.
 */
public final class LogCatFilter {
    private static final String PID_KEYWORD = "pid:";   //$NON-NLS-1$
    private static final String APP_KEYWORD = "app:";   //$NON-NLS-1$
    private static final String TAG_KEYWORD = "tag:";   //$NON-NLS-1$
    private static final String TEXT_KEYWORD = "text:"; //$NON-NLS-1$

    private final String mName;
    private final String mTag;
    private final String mText;
    private final String mPid;
    private final String mAppName;
    private final LogLevel mLogLevel;

    /** Indicates the number of messages that match this filter, but have not
     * yet been read by the user. This is really metadata about this filter
     * necessary for the UI. If we ever end up needing to store more metadata,
     * then it is probably better to move it out into a separate class. */
    private int mUnreadCount;

    /** Indicates that this filter is transient, and should not be persisted
     * across Eclipse sessions. */
    private boolean mTransient;

    private boolean mCheckPid;
    private boolean mCheckAppName;
    private boolean mCheckTag;
    private boolean mCheckText;

    private Pattern mAppNamePattern;
    private Pattern mTagPattern;
    private Pattern mTextPattern;

    /**
     * Construct a filter with the provided restrictions for the logcat message. All the text
     * fields accept Java regexes as input, but ignore invalid regexes. Filters are saved and
     * restored across Eclipse sessions unless explicitly marked transient using
     * {@link LogCatFilter#setTransient}.
     * @param name name for the filter
     * @param tag value for the logcat message's tag field.
     * @param text value for the logcat message's text field.
     * @param pid value for the logcat message's pid field.
     * @param appName value for the logcat message's app name field.
     * @param logLevel value for the logcat message's log level. Only messages of
     * higher priority will be accepted by the filter.
     */
    public LogCatFilter(String name, String tag, String text, String pid, String appName,
            LogLevel logLevel) {
        mName = name.trim();
        mTag = tag.trim();
        mText = text.trim();
        mPid = pid.trim();
        mAppName = appName.trim();
        mLogLevel = logLevel;

        mUnreadCount = 0;

        // By default, all filters are persistent. Transient filters should explicitly
        // mark it so by calling setTransient.
        mTransient = false;

        mCheckPid = mPid.length() != 0;

        if (mAppName.length() != 0) {
            try {
                mAppNamePattern = Pattern.compile(mAppName, getPatternCompileFlags(mAppName));
                mCheckAppName = true;
            } catch (PatternSyntaxException e) {
                Log.e("LogCatFilter", "Ignoring invalid app name regex.");
                Log.e("LogCatFilter", e.getMessage());
                mCheckAppName = false;
            }
        }

        if (mTag.length() != 0) {
            try {
                mTagPattern = Pattern.compile(mTag, getPatternCompileFlags(mTag));
                mCheckTag = true;
            } catch (PatternSyntaxException e) {
                Log.e("LogCatFilter", "Ignoring invalid tag regex.");
                Log.e("LogCatFilter", e.getMessage());
                mCheckTag = false;
            }
        }

        if (mText.length() != 0) {
            try {
                mTextPattern = Pattern.compile(mText, getPatternCompileFlags(mText));
                mCheckText = true;
            } catch (PatternSyntaxException e) {
                Log.e("LogCatFilter", "Ignoring invalid text regex.");
                Log.e("LogCatFilter", e.getMessage());
                mCheckText = false;
            }
        }
    }

    /**
     * Obtain the flags to pass to {@link Pattern#compile(String, int)}. This method
     * tries to figure out whether case sensitive matching should be used. It is based on
     * the following heuristic: if the regex has an upper case character, then the match
     * will be case sensitive. Otherwise it will be case insensitive.
     */
    private int getPatternCompileFlags(String regex) {
        for (char c : regex.toCharArray()) {
            if (Character.isUpperCase(c)) {
                return 0;
            }
        }

        return Pattern.CASE_INSENSITIVE;
    }

    /**
     * Construct a list of {@link LogCatFilter} objects by decoding the query.
     * @param query encoded search string. The query is simply a list of words (can be regexes)
     * a user would type in a search bar. These words are searched for in the text field of
     * each collected logcat message. To search in a different field, the word could be prefixed
     * with a keyword corresponding to the field name. Currently, the following keywords are
     * supported: "pid:", "tag:" and "text:". Invalid regexes are ignored.
     * @param minLevel minimum log level to match
     * @return list of filter settings that fully match the given query
     */
    public static List<LogCatFilter> fromString(String query, LogLevel minLevel) {
        List<LogCatFilter> filterSettings = new ArrayList<LogCatFilter>();

        for (String s : query.trim().split(" ")) {
            String tag = "";
            String text = "";
            String pid = "";
            String app = "";

            if (s.startsWith(PID_KEYWORD)) {
                pid = s.substring(PID_KEYWORD.length());
            } else if (s.startsWith(APP_KEYWORD)) {
                app = s.substring(APP_KEYWORD.length());
            } else if (s.startsWith(TAG_KEYWORD)) {
                tag = s.substring(TAG_KEYWORD.length());
            } else {
                if (s.startsWith(TEXT_KEYWORD)) {
                    text = s.substring(TEXT_KEYWORD.length());
                } else {
                    text = s;
                }
            }
            filterSettings.add(new LogCatFilter("livefilter-" + s,
                    tag, text, pid, app, minLevel));
        }

        return filterSettings;
    }

    public String getName() {
        return mName;
    }

    public String getTag() {
        return mTag;
    }

    public String getText() {
        return mText;
    }

    public String getPid() {
        return mPid;
    }

    public String getAppName() {
        return mAppName;
    }

    public LogLevel getLogLevel() {
        return mLogLevel;
    }

    /**
     * Check whether a given message will make it through this filter.
     * @param m message to check
     * @return true if the message matches the filter's conditions.
     */
    public boolean matches(LogCatMessage m) {
        /* filter out messages of a lower priority */
        if (m.getLogLevel().getPriority() < mLogLevel.getPriority()) {
            return false;
        }

        /* if pid filter is enabled, filter out messages whose pid does not match
         * the filter's pid */
        if (mCheckPid && !m.getPid().equals(mPid)) {
            return false;
        }

        /* if app name filter is enabled, filter out messages not matching the app name */
        if (mCheckAppName) {
            Matcher matcher = mAppNamePattern.matcher(m.getAppName());
            if (!matcher.find()) {
                return false;
            }
        }

        /* if tag filter is enabled, filter out messages not matching the tag */
        if (mCheckTag) {
            Matcher matcher = mTagPattern.matcher(m.getTag());
            if (!matcher.find()) {
                return false;
            }
        }

        if (mCheckText) {
            Matcher matcher = mTextPattern.matcher(m.getMessage());
            if (!matcher.find()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Update the unread count based on new messages received. The unread count
     * is incremented by the count of messages in the received list that will be
     * accepted by this filter.
     * @param newMessages list of new messages.
     */
    public void updateUnreadCount(List<LogCatMessage> newMessages) {
        for (LogCatMessage m : newMessages) {
            if (matches(m)) {
                mUnreadCount++;
            }
        }
    }

    /**
     * Reset count of unread messages.
     */
    public void resetUnreadCount() {
        mUnreadCount = 0;
    }

    /**
     * Get current value for the unread message counter.
     */
    public int getUnreadCount() {
        return mUnreadCount;
    }

    /** Make this filter transient: It will not be persisted across sessions. */
    public void setTransient() {
        mTransient = true;
    }

    public boolean isTransient() {
        return mTransient;
    }
}
