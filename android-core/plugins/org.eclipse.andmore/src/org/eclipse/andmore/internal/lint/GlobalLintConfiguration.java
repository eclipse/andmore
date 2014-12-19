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
package org.eclipse.andmore.internal.lint;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.Configuration;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Severity;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.jface.preference.IPreferenceStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Global (non-project-specific) configuration for Lint in Eclipse */
class GlobalLintConfiguration extends Configuration {
    private static final GlobalLintConfiguration sInstance = new GlobalLintConfiguration();

    private Map<Issue, Severity> mSeverities;
    private boolean mBulkEditing;

    private GlobalLintConfiguration() {
    }

    /**
     * Obtain a reference to the singleton
     *
     * @return the singleton configuration
     */
    @NonNull
    public static GlobalLintConfiguration get() {
        return sInstance;
    }

    @Override
    public Severity getSeverity(@NonNull Issue issue) {
        if (mSeverities == null) {
            IssueRegistry registry = EclipseLintClient.getRegistry();
            mSeverities = new HashMap<Issue, Severity>();
            IPreferenceStore store = getStore();
            String assignments = store.getString(AdtPrefs.PREFS_LINT_SEVERITIES);
            if (assignments != null && assignments.length() > 0) {
                for (String assignment : assignments.split(",")) { //$NON-NLS-1$
                    String[] s = assignment.split("="); //$NON-NLS-1$
                    if (s.length == 2) {
                        Issue d = registry.getIssue(s[0]);
                        if (d != null) {
                            Severity severity = Severity.valueOf(s[1]);
                            if (severity != null) {
                                mSeverities.put(d, severity);
                            }
                        }
                    }
                }
            }
        }

        Severity severity = mSeverities.get(issue);
        if (severity != null) {
            return severity;
        }

        if (!issue.isEnabledByDefault()) {
            return Severity.IGNORE;
        }

        return issue.getDefaultSeverity();
    }

    private IPreferenceStore getStore() {
        IPreferenceStore store = AdtPlugin.getDefault().getPreferenceStore();
        return store;
    }

    @Override
    public void ignore(@NonNull Context context, @NonNull Issue issue,
            @Nullable Location location, @NonNull String message,
            @Nullable Object data) {
        throw new UnsupportedOperationException(
                "Can't ignore() in global configurations"); //$NON-NLS-1$
    }

    @Override
    public void setSeverity(@NonNull Issue issue, @Nullable Severity severity) {
        if (mSeverities == null) {
            // Force initialization
            getSeverity(issue);
        }

        if (severity == null) {
            mSeverities.remove(issue);
        } else {
            mSeverities.put(issue, severity);
        }

        if (!mBulkEditing) {
            setSeverities(mSeverities);
        }
    }

    /**
     * Sets the custom severities for the given issues, in bulk.
     *
     * @param severities a map from detector to severity to use from now on
     * @return true if something changed from the current settings
     */
    private boolean setSeverities(Map<Issue, Severity> severities) {
        mSeverities = severities;

        String value = "";
        if (severities.size() > 0) {
            List<Issue> sortedKeys = new ArrayList<Issue>(severities.keySet());
            Collections.sort(sortedKeys);

            StringBuilder sb = new StringBuilder(severities.size() * 20);
            for (Issue issue : sortedKeys) {
                Severity severity = severities.get(issue);
                if (severity != issue.getDefaultSeverity()) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(issue.getId());
                    sb.append('=');
                    sb.append(severity.name());
                }
            }

            value = sb.toString();
        }

        IPreferenceStore store = getStore();
        String previous = store.getString(AdtPrefs.PREFS_LINT_SEVERITIES);
        boolean changed = !value.equals(previous);
        if (changed) {
            if (value.length() == 0) {
                store.setToDefault(AdtPrefs.PREFS_LINT_SEVERITIES);
            } else {
                store.setValue(AdtPrefs.PREFS_LINT_SEVERITIES, value);
            }
        }

        return changed;
    }

    @Override
    public void startBulkEditing() {
        mBulkEditing = true;
    }

    @Override
    public void finishBulkEditing() {
        mBulkEditing = false;
        setSeverities(mSeverities);
    }
}