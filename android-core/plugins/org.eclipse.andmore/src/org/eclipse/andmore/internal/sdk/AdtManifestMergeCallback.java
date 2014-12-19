/*
 * Copyright (C) 2012 The Android Open Source Project
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

package org.eclipse.andmore.internal.sdk;

import com.android.annotations.NonNull;
import com.android.manifmerger.ICallback;
import com.android.manifmerger.ManifestMerger;
import com.android.sdklib.AndroidTargetHash;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;

/**
 * A {@link ManifestMerger} {@link ICallback} that returns the
 * proper API level for known API codenames.
 */
public class AdtManifestMergeCallback implements ICallback {
    @Override
    public int queryCodenameApiLevel(@NonNull String codename) {
        Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            try {
                AndroidVersion version = new AndroidVersion(codename);
                String hashString = AndroidTargetHash.getPlatformHashString(version);
                IAndroidTarget t = sdk.getTargetFromHashString(hashString);
                if (t != null) {
                    return t.getVersion().getApiLevel();
                }
            } catch (AndroidVersion.AndroidVersionException ignore) {}
        }
        return ICallback.UNKNOWN_CODENAME;
    }
}
