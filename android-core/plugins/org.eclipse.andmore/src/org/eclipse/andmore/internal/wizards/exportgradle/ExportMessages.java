/*
 * Copyright (C) 2013 The Android Open Source Project
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

package org.eclipse.andmore.internal.wizards.exportgradle;

import org.eclipse.osgi.util.NLS;

public class ExportMessages extends NLS {
    private static final String BUNDLE_NAME =
            "org.eclipse.andmore.internal.wizards.exportgradle.ExportMessages";//$NON-NLS-1$

    public static String PageTitle;
    public static String PageDescription;
    public static String SelectProjects;
    public static String ConfirmOverwrite;
    public static String ConfirmOverwriteTitle;
    public static String CyclicProjectsError;
    public static String ExportFailedError;
    public static String SelectAll;
    public static String DeselectAll;
    public static String NoProjectsError;
    public static String StatusMessage;
    public static String FileStatusMessage;
    public static String WindowTitle;

    static {
        // load message values from bundle file
        NLS.initializeMessages(BUNDLE_NAME, ExportMessages.class);
    }
}
