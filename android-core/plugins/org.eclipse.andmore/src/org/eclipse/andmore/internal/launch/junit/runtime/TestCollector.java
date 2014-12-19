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

package org.eclipse.andmore.internal.launch.junit.runtime;

import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;

import java.util.Map;

/**
 * Collects info about tests to be executed by listening to the results of an Android test run.
 */
class TestCollector implements ITestRunListener {
    private final String mDeviceName;
    private final TestSuiteReference mDeviceSuiteRef;

    private int mTotalTestCount;
    /** test name to test suite reference map. */

    private String mErrorMessage = null;

    TestCollector(String deviceName) {
        mDeviceName = deviceName;
        mDeviceSuiteRef = new TestSuiteReference(deviceName);

        mTotalTestCount = 0;
    }

    @Override
    public synchronized void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
        // ignore
    }

    /* (non-Javadoc)
     * @see com.android.ddmlib.testrunner.ITestRunListener#testFailed(com.android.ddmlib.testrunner.ITestRunListener.TestFailure, com.android.ddmlib.testrunner.TestIdentifier, java.lang.String)
     */
    @Override
    public synchronized void testFailed(TestFailure status, TestIdentifier test, String trace) {
        // ignore - should be impossible since this is only collecting test information
    }

    /* (non-Javadoc)
     * @see com.android.ddmlib.testrunner.ITestRunListener#testRunEnded(long, Map<String, String>)
     */
    @Override
    public synchronized void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
        // ignore
    }

    /* (non-Javadoc)
     * @see com.android.ddmlib.testrunner.ITestRunListener#testRunFailed(java.lang.String)
     */
    @Override
    public synchronized void testRunFailed(String errorMessage) {
        mErrorMessage = errorMessage;
    }

    /* (non-Javadoc)
     * @see com.android.ddmlib.testrunner.ITestRunListener#testRunStarted(int)
     */
    @Override
    public synchronized void testRunStarted(String ignoredRunName, int testCount) {
        mTotalTestCount = testCount;
    }

    /* (non-Javadoc)
     * @see com.android.ddmlib.testrunner.ITestRunListener#testRunStopped(long)
     */
    @Override
    public synchronized void testRunStopped(long elapsedTime) {
        // ignore
    }

    /* (non-Javadoc)
     * @see com.android.ddmlib.testrunner.ITestRunListener#testStarted(com.android.ddmlib.testrunner.TestIdentifier)
     */
    @Override
    public synchronized void testStarted(TestIdentifier test) {
        TestSuiteReference suiteRef = mDeviceSuiteRef.getTestSuite(test.getClassName());
        if (suiteRef == null) {
            suiteRef = new TestSuiteReference(test.getClassName());
            mDeviceSuiteRef.addTest(suiteRef);
        }

        suiteRef.addTest(new TestCaseReference(mDeviceName, test));
    }

    /**
     * Returns the total test count in the test run.
     */
    public synchronized int getTestCaseCount() {
        return mTotalTestCount;
    }

    /**
     * Returns the error message that was reported when collecting test info.
     * Returns <code>null</code> if no error occurred.
     */
    public synchronized String getErrorMessage() {
        return mErrorMessage;
    }

    public TestSuiteReference getDeviceSuite() {
        return mDeviceSuiteRef;
    }
}
