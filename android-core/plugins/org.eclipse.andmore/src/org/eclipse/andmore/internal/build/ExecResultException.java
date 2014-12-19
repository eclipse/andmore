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

package org.eclipse.andmore.internal.build;

/**
 * Base exception class containing the error code and output of an external tool failed exec.
 *
 */
class ExecResultException extends Exception {
    private static final long serialVersionUID = 1L;

    private final int mErrorCode;
    private final String[] mOutput;

    protected ExecResultException(int errorCode, String[] output) {
        mErrorCode = errorCode;
        mOutput = output;
    }

    /**
     * Returns the full output of aapt.
     */
    public String[] getOutput() {
        return mOutput;
    }

    /**
     * Returns the aapt return code.
     */
    public int getErrorCode() {
        return mErrorCode;
    }

    public String getLabel() {
        return "Command-line";
    }

    @Override
    public String toString() {
        String result = String.format("%1$s Error %2$d", getLabel(), mErrorCode);
        if (mOutput != null && mOutput.length > 0) {
            // Note : the "error detail" window in Eclipse seem to ignore the \n,
            // so we prefix them with a space. It's not optimal but it's slightly readable.
            result += " \nOutput:";
            for (String o : mOutput) {
                if (o != null) {
                    result += " \n" + o;
                }
            }
        }
        return result;
    }

    @Override
    public String getMessage() {
        return toString();
    }
}
