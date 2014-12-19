/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.andmore.AdtPlugin;

/**
 * Launches the given activity
 */
public class ActivityLaunchAction implements IAndroidLaunchAction {

    private final String mActivity;
    private final ILaunchController mLaunchController;

    /**
     * Creates a ActivityLaunchAction
     *
     * @param activity fully qualified activity name to launch
     * @param controller the {@link ILaunchController} that performs launch
     */
    public ActivityLaunchAction(String activity, ILaunchController controller) {
        mActivity = activity;
        mLaunchController = controller;
    }

    public boolean doLaunchAction(DelayedLaunchInfo info, IDevice device) {
        String command = "am start" //$NON-NLS-1$
            + (info.isDebugMode() ? " -D" //$NON-NLS-1$
                    : "") //$NON-NLS-1$
            + " -n " //$NON-NLS-1$
            + info.getPackageName() + "/" //$NON-NLS-1$
            + mActivity.replaceAll("\\$", "\\\\\\$") //$NON-NLS-1$ //$NON-NLS-2$
            + " -a android.intent.action.MAIN"  //$NON-NLS-1$
            + " -c android.intent.category.LAUNCHER";
        try {
            String msg = String.format("Starting activity %1$s on device %2$s", mActivity,
                    device);
            AdtPlugin.printToConsole(info.getProject(), msg);

            // In debug mode, we need to add the info to the list of application monitoring
            // client changes.
            // increment launch attempt count, to handle retries and timeouts
            info.incrementAttemptCount();

            // now we actually launch the app.
            device.executeShellCommand(command, new AMReceiver(info, device, mLaunchController));

            // if the app is not a debug app, we need to do some clean up, as
            // the process is done!
            if (info.isDebugMode() == false) {
                // stop the launch object, since there's no debug, and it can't
                // provide any control over the app
                return false;
            }
        } catch (TimeoutException e) {
            AdtPlugin.printErrorToConsole(info.getProject(), "Launch error: timeout");
            return false;
        } catch (AdbCommandRejectedException e) {
            AdtPlugin.printErrorToConsole(info.getProject(), String.format(
                    "Launch error: adb rejected command: %1$s", e.getMessage()));
            return false;
        } catch (ShellCommandUnresponsiveException e) {
            // we didn't get the output but that's ok, just log it
            AdtPlugin.log(e, "No command output when running: '%1$s' on device %2$s", command,
                    device);
        } catch (IOException e) {
            // something went wrong trying to launch the app.
            // lets stop the Launch
            AdtPlugin.printErrorToConsole(info.getProject(),
                    String.format("Launch error: %s", e.getMessage()));
            return false;
        }
        return true;
    }

    /**
     * Launches the activity on targeted device
     *
     * @param info the {@link DelayedLaunchInfo} that contains launch details
     * @param devices list of Android devices on which the activity will be launched
     */
    @Override
    public boolean doLaunchAction(DelayedLaunchInfo info, Collection<IDevice> devices) {
        boolean result = true;
        for (IDevice d : devices) {
            // Note that this expression should not short circuit - even if an action fails
            // on a device, it should still be performed on all other devices.
            result = doLaunchAction(info, d) && result;
        }

        return result;
    }

    /**
     * Returns a description of the activity being launched
     *
     * @see IAndroidLaunchAction#getLaunchDescription()
     */
    @Override
    public String getLaunchDescription() {
       return String.format("%1$s activity launch", mActivity);
    }
}
