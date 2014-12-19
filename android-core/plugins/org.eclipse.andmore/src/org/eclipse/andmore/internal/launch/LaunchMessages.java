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

package org.eclipse.andmore.internal.launch;

import org.eclipse.osgi.util.NLS;

public class LaunchMessages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.andmore.internal.launch.messages"; //$NON-NLS-1$

    // generic messages that could be used by multiple classes
    public static String LaunchDialogTitle;
    public static String NonAndroidProjectError;
    public static String ParseFileFailure_s;

    // specialized, class-specific messages
    public static String AndroidJUnitLaunchAction_LaunchDesc_s;
    public static String AndroidJUnitLaunchAction_LaunchFail;
    public static String AndroidJUnitLaunchAction_LaunchInstr_2s;
    public static String AndroidJUnitDelegate_NoRunnerConfigMsg_s;
    public static String AndroidJUnitDelegate_NoRunnerConsoleMsg_4s;
    public static String AndroidJUnitDelegate_NoRunnerMsg_s;
    public static String AndroidJUnitDelegate_NoTargetMsg_3s;
    public static String AndroidJUnitTab_LoaderLabel;
    public static String AndroidJUnitTab_LoadInstrError_s;
    public static String AndroidJUnitTab_NoRunnerError;
    public static String AndroidJUnitTab_SizeLabel;
    public static String AndroidJUnitTab_TestContainerText;
    public static String InstrValidator_NoTestLibMsg_s;
    public static String InstrValidator_WrongRunnerTypeMsg_s;
    public static String RemoteAdtTestRunner_RunCompleteMsg;
    public static String RemoteAdtTestRunner_RunFailedMsg_s;

    public static String RemoteAdtTestRunner_RunIOException_s;
    public static String RemoteAdtTestRunner_RunTimeoutException;
    public static String RemoteAdtTestRunner_RunAdbCommandRejectedException_s;
    public static String RemoteAdtTestRunner_RunShellCommandUnresponsiveException;
    public static String RemoteAdtTestRunner_RunStoppedMsg;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, LaunchMessages.class);
    }

    private LaunchMessages() {
    }
}
