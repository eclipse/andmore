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

package com.android.ide.eclipse.monitor;

import com.android.SdkConstants;
import com.android.sdkstats.SdkStatsService;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IStartup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class MonitorStartup implements IStartup {
    @Override
    public void earlyStartup() {
        Job pingJob = new Job("Android SDK Ping") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                SdkStatsService stats = new SdkStatsService();
                File sdkFolder = MonitorPlugin.getDefault().getSdkFolder();
                if (sdkFolder == null) {
                    return Status.OK_STATUS;
                }

                String toolsPath = new File(sdkFolder, SdkConstants.FD_TOOLS).getAbsolutePath();
                ping(stats, toolsPath);
                return Status.OK_STATUS;
            }
        };
        pingJob.setPriority(Job.DECORATE); // lowest priority
        pingJob.schedule();
    }

    private static void ping(SdkStatsService stats, String toolsLocation) {
        Properties p = new Properties();
        try{
            File sourceProp;
            if (toolsLocation != null && toolsLocation.length() > 0) {
                sourceProp = new File(toolsLocation, "source.properties"); //$NON-NLS-1$
            } else {
                sourceProp = new File("source.properties"); //$NON-NLS-1$
            }
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(sourceProp);
                p.load(fis);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ignore) {
                    }
                }
            }

            String revision = p.getProperty("Pkg.Revision"); //$NON-NLS-1$
            if (revision != null && revision.length() > 0) {
                stats.ping("ddms", revision);  //$NON-NLS-1$
            }
        } catch (FileNotFoundException e) {
            // couldn't find the file? don't ping.
        } catch (IOException e) {
            // couldn't find the file? don't ping.
        }
    }
}
