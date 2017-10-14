/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import org.eclipse.andmore.internal.editors.layout.RenderSecurityManager;
import com.android.ide.common.rendering.api.LayoutLog;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.core.runtime.IStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link LayoutLog} which records the problems it encounters and offers them as a
 * single summary at the end
 */
public class RenderLogger extends LayoutLog {
    static final String TAG_MISSING_DIMENSION = "missing.dimension";     //$NON-NLS-1$

    private final String mName;
    private List<String> mFidelityWarnings;
    private List<String> mWarnings;
    private List<String> mErrors;
    private boolean mHaveExceptions;
    private List<String> mTags;
    private List<Throwable> mTraces;
    private static Set<String> sIgnoredFidelityWarnings;
    private final Object mCredential;

    /** Construct a logger for the given named layout */
    RenderLogger(String name, Object credential) {
        mName = name;
        mCredential = credential;
    }

    /**
     * Are there any logged errors or warnings during the render?
     *
     * @return true if there were problems during the render
     */
    public boolean hasProblems() {
        return mFidelityWarnings != null || mErrors != null || mWarnings != null ||
            mHaveExceptions;
    }

    /**
     * Returns a list of traces encountered during rendering, or null if none
     *
     * @return a list of traces encountered during rendering, or null if none
     */
    @Nullable
    public List<Throwable> getFirstTrace() {
        return mTraces;
    }

    /**
     * Returns a (possibly multi-line) description of all the problems
     *
     * @param includeFidelityWarnings if true, include fidelity warnings in the problem
     *            summary
     * @return a string describing the rendering problems
     */
    @NonNull
    public String getProblems(boolean includeFidelityWarnings) {
        StringBuilder sb = new StringBuilder();

        if (mErrors != null) {
            for (String error : mErrors) {
                sb.append(error).append('\n');
            }
        }

        if (mWarnings != null) {
            for (String warning : mWarnings) {
                sb.append(warning).append('\n');
            }
        }

        if (includeFidelityWarnings && mFidelityWarnings != null) {
            sb.append("The graphics preview in the layout editor may not be accurate:\n");
            for (String warning : mFidelityWarnings) {
                sb.append("* ");
                sb.append(warning).append('\n');
            }
        }

        if (mHaveExceptions) {
            sb.append("Exception details are logged in Window > Show View > Error Log");
        }

        return sb.toString();
    }

    /**
     * Returns the fidelity warnings
     *
     * @return the fidelity warnings
     */
    @Nullable
    public List<String> getFidelityWarnings() {
        return mFidelityWarnings;
    }

    // ---- extends LayoutLog ----

    @Override
    public void error(String tag, String message, Object data) {
        String description = describe(message);

        appendToIdeLog(null, IStatus.ERROR, description);

        // Workaround: older layout libraries don't provide a tag for this error
        if (tag == null && message != null
                && message.startsWith("Failed to find style ")) { //$NON-NLS-1$
            tag = LayoutLog.TAG_RESOURCES_RESOLVE_THEME_ATTR;
        }

        addError(tag, description);
    }

    @Override
    public void error(String tag, String message, Throwable throwable, Object data) {
        String description = describe(message);
        appendToIdeLog(throwable, IStatus.ERROR, description);

        if (throwable != null) {
            if (throwable instanceof ClassNotFoundException) {
                // The project callback is given a chance to resolve classes,
                // and when it fails, it will record it in its own list which
                // is displayed in a special way (with action hyperlinks etc).
                // Therefore, include these messages in the visible render log,
                // especially since the user message from a ClassNotFoundException
                // is really not helpful (it just lists the class name without
                // even mentioning that it is a class-not-found exception.)
                return;
            }

            if (description.equals(throwable.getLocalizedMessage()) ||
                    description.equals(throwable.getMessage())) {
                description = "Exception raised during rendering: " + description;
            }
            recordThrowable(throwable);
            mHaveExceptions = true;
        }

        addError(tag, description);
    }

    /**
     * Record that the given exception was encountered during rendering
     *
     * @param throwable the exception that was raised
     */
    public void recordThrowable(@NonNull Throwable throwable) {
        if (mTraces == null) {
            mTraces = new ArrayList<Throwable>();
        }
        mTraces.add(throwable);
    }

    @Override
    public void warning(String tag, String message, Object data) {
        String description = describe(message);

        boolean log = true;
        if (TAG_RESOURCES_FORMAT.equals(tag)) {
            if (description.equals("You must supply a layout_width attribute.")       //$NON-NLS-1$
                || description.equals("You must supply a layout_height attribute.")) {//$NON-NLS-1$
                tag = TAG_MISSING_DIMENSION;
                log = false;
            }
        }

        if (log) {
            appendToIdeLog(null, IStatus.WARNING, description);
        }

        addWarning(tag, description);
    }

    @Override
    public void fidelityWarning(String tag, String message, Throwable throwable, Object data) {
        if (sIgnoredFidelityWarnings != null && sIgnoredFidelityWarnings.contains(message)) {
            return;
        }

        String description = describe(message);
        appendToIdeLog(throwable, IStatus.ERROR, description);

        if (throwable != null) {
            mHaveExceptions = true;
        }

        addFidelityWarning(tag, description);
    }

    /**
     * Ignore the given render fidelity warning for the current session
     *
     * @param message the message to be ignored for this session
     */
    public static void ignoreFidelityWarning(String message) {
        if (sIgnoredFidelityWarnings == null) {
            sIgnoredFidelityWarnings = new HashSet<String>();
        }
        sIgnoredFidelityWarnings.add(message);
    }

    @NonNull
    private String describe(@Nullable String message) {
        if (message == null) {
            return "";
        } else {
            return message;
        }
    }

    private void addWarning(String tag, String description) {
        if (mWarnings == null) {
            mWarnings = new ArrayList<String>();
        } else if (mWarnings.contains(description)) {
            // Avoid duplicates
            return;
        }
        mWarnings.add(description);
        addTag(tag);
    }

    private void addError(String tag, String description) {
        if (mErrors == null) {
            mErrors = new ArrayList<String>();
        } else if (mErrors.contains(description)) {
            // Avoid duplicates
            return;
        }
        mErrors.add(description);
        addTag(tag);
    }

    private void addFidelityWarning(String tag, String description) {
        if (mFidelityWarnings == null) {
            mFidelityWarnings = new ArrayList<String>();
        } else if (mFidelityWarnings.contains(description)) {
            // Avoid duplicates
            return;
        }
        mFidelityWarnings.add(description);
        addTag(tag);
    }

    // ---- Tags ----

    private void addTag(String tag) {
        if (tag != null) {
            if (mTags == null) {
                mTags = new ArrayList<String>();
            }
            mTags.add(tag);
        }
    }

    /**
     * Returns true if the given tag prefix has been seen
     *
     * @param prefix the tag prefix to look for
     * @return true iff any tags with the given prefix was seen during the render
     */
    public boolean seenTagPrefix(String prefix) {
        if (mTags != null) {
            for (String tag : mTags) {
                if (tag.startsWith(prefix)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns true if the given tag has been seen
     *
     * @param tag the tag to look for
     * @return true iff the tag was seen during the render
     */
    public boolean seenTag(String tag) {
        if (mTags != null) {
            return mTags.contains(tag);
        } else {
            return false;
        }
    }

    // Append the given message to the ADT log. Bypass the sandbox if necessary
    // such that we can write to the log file.
    private void appendToIdeLog(Throwable throwable, int severity, String description) {
        boolean token = RenderSecurityManager.enterSafeRegion(mCredential);
        try {
            if (throwable != null) {
                AndmoreAndroidPlugin.log(throwable, "%1$s: %2$s", mName, description);
            } else {
                AndmoreAndroidPlugin.log(severity, "%1$s: %2$s", mName, description);
            }
        } finally {
            RenderSecurityManager.exitSafeRegion(token);
        }
    }
}
