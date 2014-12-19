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

package org.eclipse.andmore.mock;


import com.android.utils.ILogger;

import junit.framework.Assert;

/**
 * Implementation of {@link ILogger} suitable for test use; will fail the current test if
 * {@link #error} is called, and prints everything else to standard error.
 */
public class TestLogger implements ILogger {

    @Override
    public void error(Throwable t, String errorFormat, Object... args) {
        String message = String.format(errorFormat, args);
        if (t != null) {
            message = t.toString() + ":" + message; //$NON-NLS-1$
        }
        Assert.fail(message);
    }

    @Override
    public void info(String msgFormat, Object... args) {
        System.out.println(String.format(msgFormat, args));
    }

    @Override
    public void verbose(String msgFormat, Object... args) {
        info(msgFormat, args);
    }

    @Override
    public void warning(String warningFormat, Object... args) {
        System.err.println(String.format(warningFormat, args));
    }

}
