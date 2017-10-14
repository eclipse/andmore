/*
 * Copyright (C) 2012 The Android Open Source Project
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

package org.eclipse.andmore.internal.launch;

import org.eclipse.andmore.internal.sdk.Sdk;

import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.avd.AvdInfo;

public class AvdCompatibility {
    public enum Compatibility {
        YES,
        NO,
        UNKNOWN,
    };

    /**
     * Returns whether the specified AVD can run the given project that is built against
     * a particular SDK and has the specified minApiLevel.
     * @param avd AVD to check compatibility for
     * @param projectTarget project build target
     * @param minApiVersion project min api level
     * @return whether the given AVD can run the given application
     */
    public static Compatibility canRun(AvdInfo avd, IAndroidTarget projectTarget,
            AndroidVersion minApiVersion) {
        if (avd == null) {
            return Compatibility.UNKNOWN;
        }

        Sdk currentSdk = Sdk.getCurrent();
        IAndroidTarget avdTarget = currentSdk == null ? null : currentSdk.getAndroidTargetFor(avd);
        if (avdTarget == null) {
            return Compatibility.UNKNOWN;
        }

        // for platform targets, we only need to check the min api version
        if (projectTarget.isPlatform()) {
            return avdTarget.getVersion().canRun(minApiVersion) ?
                    Compatibility.YES : Compatibility.NO;
        }

        // for add-on targets, delegate to the add on target to check for compatibility
        return projectTarget.canRunOn(avdTarget) ? Compatibility.YES : Compatibility.NO;
    }
}
