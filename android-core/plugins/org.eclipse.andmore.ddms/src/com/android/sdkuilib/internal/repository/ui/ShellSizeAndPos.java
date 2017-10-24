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

package com.android.sdkuilib.internal.repository.ui;


import com.android.prefs.AndroidLocation;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Utility to save & restore the size and position on a window
 * using a common config file.
 */
public class ShellSizeAndPos {

    private static final String SETTINGS_FILENAME = "androidwin.cfg";   //$NON-NLS-1$
    private static final String PX = "_px";                             //$NON-NLS-1$
    private static final String PY = "_py";                             //$NON-NLS-1$
    private static final String SX = "_sx";                             //$NON-NLS-1$
    private static final String SY = "_sy";                             //$NON-NLS-1$

    public static void loadSizeAndPos(Shell shell, String prefix) {
        Properties props = loadProperties();

        try {
            int px = Integer.parseInt(props.getProperty(prefix + PX));
            int py = Integer.parseInt(props.getProperty(prefix + PY));
            int sx = Integer.parseInt(props.getProperty(prefix + SX));
            int sy = Integer.parseInt(props.getProperty(prefix + SY));

            Point p1 = new Point(px, py);
            Point p2 = new Point(px + sx, py + sy);
            Rectangle r = new Rectangle(px, py, sy, sy);

            Monitor bestMatch = null;
            int bestSurface = -1;
            for (Monitor monitor : shell.getDisplay().getMonitors()) {
                Rectangle area = monitor.getClientArea();
                if (area.contains(p1) && area.contains(p2)) {
                    // The shell is fully visible on this monitor. Just use that.
                    bestMatch = monitor;
                    bestSurface = Integer.MAX_VALUE;
                    break;
                } else {
                    // Find which monitor displays the largest surface of the window.
                    // We'll use this one to center the window there, to make sure we're not
                    // starting split between several monitors.
                    Rectangle i = area.intersection(r);
                    int surface = i.width * i.height;
                    if (surface > bestSurface) {
                        bestSurface = surface;
                        bestMatch = monitor;
                    }
                }
            }

            if (bestMatch != null && bestSurface != Integer.MAX_VALUE) {
                // Recenter the window on this monitor and make sure it fits
                Rectangle area = bestMatch.getClientArea();

                sx = Math.min(sx, area.width);
                sy = Math.min(sy, area.height);
                px = area.x + (area.width - sx) / 2;
                py = area.y + (area.height - sy) / 2;
            }

            shell.setLocation(px, py);
            shell.setSize(sx, sy);

        } catch ( Exception e) {
            // Ignore exception. We could typically get NPE from the getProperty
            // or NumberFormatException from parseInt calls. Either way, do
            // nothing if anything goes wrong.
        }
    }

    public static void saveSizeAndPos(Shell shell, String prefix) {
        Properties props = loadProperties();

        Point loc = shell.getLocation();
        Point size = shell.getSize();

        props.setProperty(prefix + PX, Integer.toString(loc.x));
        props.setProperty(prefix + PY, Integer.toString(loc.y));
        props.setProperty(prefix + SX, Integer.toString(size.x));
        props.setProperty(prefix + SY, Integer.toString(size.y));

        saveProperties(props);
    }

    /**
     * Load properties saved in {@link #SETTINGS_FILENAME}.
     * If the file does not exists or doesn't load properly, just return an
     * empty set of properties.
     */
    private static Properties loadProperties() {
        Properties props = new Properties();
        FileInputStream fis = null;

        try {
            String folder = AndroidLocation.getFolder();
            File f = new File(folder, SETTINGS_FILENAME);
            if (f.exists()) {
                fis = new FileInputStream(f);

                props.load(fis);
            }
        } catch (Exception e) {
            // Ignore
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }

        return props;
    }

    private static void saveProperties(Properties props) {
        FileOutputStream fos = null;

        try {
            String folder = AndroidLocation.getFolder();
            File f = new File(folder, SETTINGS_FILENAME);
            fos = new FileOutputStream(f);

            props.store(fos, "## Size and Pos for SDK Manager Windows");  //$NON-NLS-1$

        } catch (Exception e) {
            // ignore
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
