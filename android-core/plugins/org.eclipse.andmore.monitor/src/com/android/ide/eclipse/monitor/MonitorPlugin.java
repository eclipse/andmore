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

package com.android.ide.eclipse.monitor;

import com.android.sdkstats.DdmsPreferenceStore;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import java.io.File;

public class MonitorPlugin extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "com.android.ide.eclipse.monitor"; //$NON-NLS-1$
    private static MonitorPlugin sPlugin;
    private static final DdmsPreferenceStore sDdmsPreferenceStore = new DdmsPreferenceStore();
    private File mSdkFolder;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        sPlugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        sPlugin = null;
        super.stop(context);
    }

    public static MonitorPlugin getDefault() {
        return sPlugin;
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public static DdmsPreferenceStore getDdmsPreferenceStore() {
        return sDdmsPreferenceStore;
    }

    public void setSdkFolder(File sdkFolder) {
        mSdkFolder = sdkFolder;
    }

    public File getSdkFolder() {
        return mSdkFolder;
    }
}
