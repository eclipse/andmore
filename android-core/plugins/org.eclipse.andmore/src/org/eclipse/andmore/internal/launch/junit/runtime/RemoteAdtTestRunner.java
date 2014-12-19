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

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner.TestSize;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.TestIdentifier;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.launch.LaunchMessages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.junit.runner.IListensToTestExecutions;
import org.eclipse.jdt.internal.junit.runner.ITestReference;
import org.eclipse.jdt.internal.junit.runner.MessageIds;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;
import org.eclipse.jdt.internal.junit.runner.TestExecution;
import org.eclipse.jdt.internal.junit.runner.TestReferenceFailure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Supports Eclipse JUnit execution of Android tests.
 * <p/>
 * Communicates back to a Eclipse JDT JUnit client via a socket connection.
 *
 * @see org.eclipse.jdt.internal.junit.runner.RemoteTestRunner for more details on the protocol
 */
@SuppressWarnings("restriction")
public class RemoteAdtTestRunner extends RemoteTestRunner {

    private static final String DELAY_MSEC_KEY = "delay_msec";
    /** the delay between each test execution when in collecting test info */
    private static final String COLLECT_TEST_DELAY_MS = "15";

    private AndroidJUnitLaunchInfo mLaunchInfo;
    private TestExecution mExecution;

    /**
     * Initialize the JDT JUnit test runner parameters from the {@code args}.
     *
     * @param args name-value pair of arguments to pass to parent JUnit runner.
     * @param launchInfo the Android specific test launch info
     */
    protected void init(String[] args, AndroidJUnitLaunchInfo launchInfo) {
        defaultInit(args);
        mLaunchInfo = launchInfo;
    }

    /**
     * Runs a set of tests, and reports back results using parent class.
     * <p/>
     * JDT Unit expects to be sent data in the following sequence:
     * <ol>
     *   <li>The total number of tests to be executed.</li>
     *   <li>The test 'tree' data about the tests to be executed, which is composed of the set of
     *   test class names, the number of tests in each class, and the names of each test in the
     *   class.</li>
     *   <li>The test execution result for each test method. Expects individual notifications of
     *   the test execution start, any failures, and the end of the test execution.</li>
     *   <li>The end of the test run, with its elapsed time.</li>
     * </ol>
     * <p/>
     * In order to satisfy this, this method performs two actual Android instrumentation runs.
     * The first is a 'log only' run that will collect the test tree data, without actually
     * executing the tests,  and send it back to JDT JUnit. The second is the actual test execution,
     * whose results will be communicated back in real-time to JDT JUnit.
     *
     * The tests are run concurrently on all devices. The overall structure is as follows:
     * <ol>
     *   <li> First, a separate job per device is run to collect test tree data. A per device
     *        {@link TestCollector} records information regarding the tests run on the device.
     *        </li>
     *   <li> Once all the devices have finished collecting the test tree data, the tree info is
     *        collected from all of them and passed to the Junit UI </li>
     *   <li> A job per device is again launched to do the actual test run. A per device
     *        {@link TestRunListener} notifies the shared {@link TestResultsNotifier} of test
     *        status. </li>
     *   <li> As tests complete, the test run listener updates the Junit UI </li>
     * </ol>
     *
     * @param testClassNames ignored - the AndroidJUnitLaunchInfo will be used to determine which
     *     tests to run.
     * @param testName ignored
     * @param execution used to report test progress
     */
    @Override
    public void runTests(String[] testClassNames, String testName, TestExecution execution) {
        // hold onto this execution reference so it can be used to report test progress
        mExecution = execution;

        List<IDevice> devices = new ArrayList<IDevice>(mLaunchInfo.getDevices());
        List<RemoteAndroidTestRunner> runners =
                new ArrayList<RemoteAndroidTestRunner>(devices.size());

        for (IDevice device : devices) {
            RemoteAndroidTestRunner runner = new RemoteAndroidTestRunner(
                    mLaunchInfo.getAppPackage(), mLaunchInfo.getRunner(), device);

            if (mLaunchInfo.getTestClass() != null) {
                if (mLaunchInfo.getTestMethod() != null) {
                    runner.setMethodName(mLaunchInfo.getTestClass(), mLaunchInfo.getTestMethod());
                } else {
                    runner.setClassName(mLaunchInfo.getTestClass());
                }
            }

            if (mLaunchInfo.getTestPackage() != null) {
                runner.setTestPackageName(mLaunchInfo.getTestPackage());
            }

            TestSize size = mLaunchInfo.getTestSize();
            if (size != null) {
                runner.setTestSize(size);
            }

            runners.add(runner);
        }

        // Launch all test info collector jobs
        List<TestTreeCollectorJob> collectorJobs =
                new ArrayList<TestTreeCollectorJob>(devices.size());
        List<TestCollector> perDeviceCollectors = new ArrayList<TestCollector>(devices.size());
        for (int i = 0; i < devices.size(); i++) {
            RemoteAndroidTestRunner runner = runners.get(i);
            String deviceName = devices.get(i).getName();
            TestCollector collector = new TestCollector(deviceName);
            perDeviceCollectors.add(collector);

            TestTreeCollectorJob job = new TestTreeCollectorJob(
                    "Test Tree Collector for " + deviceName,
                    runner, mLaunchInfo.isDebugMode(), collector);
            job.setPriority(Job.INTERACTIVE);
            job.schedule();

            collectorJobs.add(job);
        }

        // wait for all test info collector jobs to complete
        int totalTests = 0;
        for (TestTreeCollectorJob job : collectorJobs) {
            try {
                job.join();
            } catch (InterruptedException e) {
                endTestRunWithError(e.getMessage());
                return;
            }

            if (!job.getResult().isOK()) {
                endTestRunWithError(job.getResult().getMessage());
                return;
            }

            TestCollector collector = job.getCollector();
            String err = collector.getErrorMessage();
            if (err != null) {
                endTestRunWithError(err);
                return;
            }

            totalTests += collector.getTestCaseCount();
        }

        AdtPlugin.printToConsole(mLaunchInfo.getProject(), "Sending test information to Eclipse");
        notifyTestRunStarted(totalTests);
        sendTestTrees(perDeviceCollectors);

        List<TestRunnerJob> instrumentationRunnerJobs =
                new ArrayList<TestRunnerJob>(devices.size());

        TestResultsNotifier notifier = new TestResultsNotifier(mExecution.getListener(),
                devices.size());

        // Spawn all instrumentation runner jobs
        for (int i = 0; i < devices.size(); i++) {
            RemoteAndroidTestRunner runner = runners.get(i);
            String deviceName = devices.get(i).getName();
            TestRunListener testRunListener = new TestRunListener(deviceName, notifier);
            InstrumentationRunJob job = new InstrumentationRunJob(
                    "Test Tree Collector for " + deviceName,
                    runner, mLaunchInfo.isDebugMode(), testRunListener);
            job.setPriority(Job.INTERACTIVE);
            job.schedule();

            instrumentationRunnerJobs.add(job);
        }

        // Wait for all jobs to complete
        for (TestRunnerJob job : instrumentationRunnerJobs) {
            try {
                job.join();
            } catch (InterruptedException e) {
                endTestRunWithError(e.getMessage());
                return;
            }

            if (!job.getResult().isOK()) {
                endTestRunWithError(job.getResult().getMessage());
                return;
            }
        }
    }

    /** Sends info about the test tree to be executed (ie the suites and their enclosed tests) */
    private void sendTestTrees(List<TestCollector> perDeviceCollectors) {
        for (TestCollector c : perDeviceCollectors) {
            ITestReference ref = c.getDeviceSuite();
            ref.sendTree(this);
        }
    }

    private static abstract class TestRunnerJob extends Job {
        private ITestRunListener mListener;
        private RemoteAndroidTestRunner mRunner;
        private boolean mIsDebug;

        public TestRunnerJob(String name, RemoteAndroidTestRunner runner,
                boolean isDebug, ITestRunListener listener) {
            super(name);

            mRunner = runner;
            mIsDebug = isDebug;
            mListener = listener;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try {
                setupRunner();
                mRunner.run(mListener);
            } catch (TimeoutException e) {
                return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                        LaunchMessages.RemoteAdtTestRunner_RunTimeoutException,
                        e);
            } catch (IOException e) {
                return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                        String.format(LaunchMessages.RemoteAdtTestRunner_RunIOException_s,
                                e.getMessage()),
                        e);
            } catch (AdbCommandRejectedException e) {
                return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                        String.format(
                                LaunchMessages.RemoteAdtTestRunner_RunAdbCommandRejectedException_s,
                                e.getMessage()),
                        e);
            } catch (ShellCommandUnresponsiveException e) {
                return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                        LaunchMessages.RemoteAdtTestRunner_RunTimeoutException,
                        e);
            }

            return Status.OK_STATUS;
        }

        public RemoteAndroidTestRunner getRunner() {
            return mRunner;
        }

        public boolean isDebug() {
            return mIsDebug;
        }

        public ITestRunListener getListener() {
            return mListener;
        }

        protected abstract void setupRunner();
    }

    private static class TestTreeCollectorJob extends TestRunnerJob {
        public TestTreeCollectorJob(String name, RemoteAndroidTestRunner runner, boolean isDebug,
                TestCollector listener) {
            super(name, runner, isDebug, listener);
        }

        @Override
        protected void setupRunner() {
            RemoteAndroidTestRunner runner = getRunner();

            // set log only to just collect test case info,
            // so Eclipse has correct test case count/tree info
            runner.setLogOnly(true);

            // add a small delay between each test. Otherwise for large test suites framework may
            // report Binder transaction failures
            runner.addInstrumentationArg(DELAY_MSEC_KEY, COLLECT_TEST_DELAY_MS);
        }

        public TestCollector getCollector() {
            return (TestCollector) getListener();
        }
    }

    private static class InstrumentationRunJob extends TestRunnerJob {
        public InstrumentationRunJob(String name, RemoteAndroidTestRunner runner, boolean isDebug,
                ITestRunListener listener) {
            super(name, runner, isDebug, listener);
        }

        @Override
        protected void setupRunner() {
            RemoteAndroidTestRunner runner = getRunner();
            runner.setLogOnly(false);
            runner.removeInstrumentationArg(DELAY_MSEC_KEY);
            if (isDebug()) {
                runner.setDebug(true);
            }
        }
    }

    /**
     * Main entry method to run tests
     *
     * @param programArgs JDT JUnit program arguments to be processed by parent
     * @param junitInfo the {@link AndroidJUnitLaunchInfo} containing info about this test ru
     */
    public void runTests(String[] programArgs, AndroidJUnitLaunchInfo junitInfo) {
        init(programArgs, junitInfo);
        run();
    }

    /**
     * Stop the current test run.
     */
    public void terminate() {
        stop();
    }

    @Override
    protected void stop() {
        if (mExecution != null) {
            mExecution.stop();
        }
    }

    private void notifyTestRunEnded(long elapsedTime) {
        // copy from parent - not ideal, but method is private
        sendMessage(MessageIds.TEST_RUN_END + elapsedTime);
        flush();
        //shutDown();
    }

    /**
     * @param errorMessage
     */
    private void reportError(String errorMessage) {
        AdtPlugin.printErrorToConsole(mLaunchInfo.getProject(),
                String.format(LaunchMessages.RemoteAdtTestRunner_RunFailedMsg_s, errorMessage));
        // is this needed?
        //notifyTestRunStopped(-1);
    }

    private void endTestRunWithError(String message) {
        reportError(message);
        notifyTestRunEnded(0);
    }

    /**
     * This class provides the interface to notify the JDT UI regarding the status of tests.
     * When running tests on multiple devices, there is a {@link TestRunListener} that listens
     * to results from each device. Rather than all such listeners directly notifying JDT
     * from different threads, they all notify this class which notifies JDT. In addition,
     * the {@link #testRunEnded(String, long)} method make sure that JDT is notified that the
     * test run has completed only when tests on all devices have completed.
     * */
    private class TestResultsNotifier {
        private final IListensToTestExecutions mListener;
        private final int mDeviceCount;

        private int mCompletedRuns;
        private long mMaxElapsedTime;

        public TestResultsNotifier(IListensToTestExecutions listener, int nDevices) {
            mListener = listener;
            mDeviceCount = nDevices;
        }

        public synchronized void testEnded(TestCaseReference ref) {
            mListener.notifyTestEnded(ref);
        }

        public synchronized void testFailed(TestReferenceFailure ref) {
            mListener.notifyTestFailed(ref);
        }

        public synchronized void testRunEnded(String mDeviceName, long elapsedTime) {
            mCompletedRuns++;

            if (elapsedTime > mMaxElapsedTime) {
                mMaxElapsedTime = elapsedTime;
            }

            if (mCompletedRuns == mDeviceCount) {
                notifyTestRunEnded(mMaxElapsedTime);
            }
        }

        public synchronized void testStarted(TestCaseReference testId) {
            mListener.notifyTestStarted(testId);
        }
    }

    /**
     * TestRunListener that communicates results in real-time back to JDT JUnit via the
     * {@link TestResultsNotifier}.
     * */
    private class TestRunListener implements ITestRunListener {
        private final String mDeviceName;
        private TestResultsNotifier mNotifier;

        /**
         * Constructs a {@link ITestRunListener} that listens for test results on given device.
         * @param deviceName device on which the tests are being run
         * @param notifier notifier to inform of test status
         */
        public TestRunListener(String deviceName, TestResultsNotifier notifier) {
            mDeviceName = deviceName;
            mNotifier = notifier;
        }

        @Override
        public void testEnded(TestIdentifier test, Map<String, String> ignoredTestMetrics) {
            mNotifier.testEnded(new TestCaseReference(mDeviceName, test));
        }

        @Override
        public void testFailed(TestFailure status, TestIdentifier test, String trace) {
            String statusString;
            if (status == TestFailure.ERROR) {
                statusString = MessageIds.TEST_ERROR;
            } else {
                statusString = MessageIds.TEST_FAILED;
            }
            TestReferenceFailure failure =
                new TestReferenceFailure(new TestCaseReference(mDeviceName, test),
                        statusString, trace, null);
            mNotifier.testFailed(failure);
        }

        @Override
        public synchronized void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
            mNotifier.testRunEnded(mDeviceName, elapsedTime);
            AdtPlugin.printToConsole(mLaunchInfo.getProject(),
                    LaunchMessages.RemoteAdtTestRunner_RunCompleteMsg);
        }

        @Override
        public synchronized void testRunFailed(String errorMessage) {
            reportError(errorMessage);
        }

        @Override
        public synchronized void testRunStarted(String runName, int testCount) {
            // ignore
        }

        @Override
        public synchronized void testRunStopped(long elapsedTime) {
            notifyTestRunStopped(elapsedTime);
            AdtPlugin.printToConsole(mLaunchInfo.getProject(),
                    LaunchMessages.RemoteAdtTestRunner_RunStoppedMsg);
        }

        @Override
        public synchronized void testStarted(TestIdentifier test) {
            TestCaseReference testId = new TestCaseReference(mDeviceName, test);
            mNotifier.testStarted(testId);
        }
    }

    /** Override parent to get extra logs. */
    @Override
    protected boolean connect() {
        boolean result = super.connect();
        if (!result) {
            AdtPlugin.printErrorToConsole(mLaunchInfo.getProject(),
                    "Connect to Eclipse test result listener failed");
        }
        return result;
    }

    /** Override parent to dump error message to console. */
    @Override
    public void runFailed(String message, Exception exception) {
        if (exception != null) {
            AdtPlugin.logAndPrintError(exception, mLaunchInfo.getProject().getName(),
                    "Test launch failed: %s", message);
        } else {
            AdtPlugin.printErrorToConsole(mLaunchInfo.getProject(), "Test launch failed: %s",
                    message);
        }
    }
}
