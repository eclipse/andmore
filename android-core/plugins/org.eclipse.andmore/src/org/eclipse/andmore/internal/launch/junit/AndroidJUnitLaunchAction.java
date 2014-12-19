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
package org.eclipse.andmore.internal.launch.junit;

import com.android.ddmlib.IDevice;
import com.google.common.base.Joiner;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.launch.DelayedLaunchInfo;
import org.eclipse.andmore.internal.launch.IAndroidLaunchAction;
import org.eclipse.andmore.internal.launch.LaunchMessages;
import org.eclipse.andmore.internal.launch.junit.runtime.AndroidJUnitLaunchInfo;
import org.eclipse.andmore.internal.launch.junit.runtime.RemoteAdtTestRunner;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.swt.widgets.Display;

import java.util.Collection;

/**
 * A launch action that executes a instrumentation test run on an Android device.
 */
class AndroidJUnitLaunchAction implements IAndroidLaunchAction {
    private static final Joiner JOINER = Joiner.on(',').skipNulls();
    private final AndroidJUnitLaunchInfo mLaunchInfo;

    /**
     * Creates a AndroidJUnitLaunchAction.
     *
     * @param launchInfo the {@link AndroidJUnitLaunchInfo} for the JUnit run
     */
    public AndroidJUnitLaunchAction(AndroidJUnitLaunchInfo launchInfo) {
        mLaunchInfo = launchInfo;
    }

    /**
     * Launch a instrumentation test run on given Android devices.
     * Reuses JDT JUnit launch delegate so results can be communicated back to JDT JUnit UI.
     * <p/>
     * Note: Must be executed on non-UI thread.
     *
     * @see IAndroidLaunchAction#doLaunchActions(DelayedLaunchInfo, IDevice)
     */
    @Override
    public boolean doLaunchAction(DelayedLaunchInfo info, Collection<IDevice> devices) {
        String msg = String.format(LaunchMessages.AndroidJUnitLaunchAction_LaunchInstr_2s,
                mLaunchInfo.getRunner(), JOINER.join(devices));
        AdtPlugin.printToConsole(info.getProject(), msg);

        try {
           mLaunchInfo.setDebugMode(info.isDebugMode());
           mLaunchInfo.setDevices(devices);
           JUnitLaunchDelegate junitDelegate = new JUnitLaunchDelegate(mLaunchInfo);
           final String mode = info.isDebugMode() ? ILaunchManager.DEBUG_MODE :
               ILaunchManager.RUN_MODE;

           junitDelegate.launch(info.getLaunch().getLaunchConfiguration(), mode, info.getLaunch(),
                   info.getMonitor());

           // TODO: need to add AMReceiver-type functionality somewhere
        } catch (CoreException e) {
            AdtPlugin.printErrorToConsole(info.getProject(),
                    LaunchMessages.AndroidJUnitLaunchAction_LaunchFail);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLaunchDescription() {
        return String.format(LaunchMessages.AndroidJUnitLaunchAction_LaunchDesc_s,
                mLaunchInfo.getRunner());
    }

    /**
     * Extends the JDT JUnit launch delegate to allow for JUnit UI reuse.
     */
    private static class JUnitLaunchDelegate extends JUnitLaunchConfigurationDelegate {

        private AndroidJUnitLaunchInfo mLaunchInfo;

        public JUnitLaunchDelegate(AndroidJUnitLaunchInfo launchInfo) {
            mLaunchInfo = launchInfo;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
         */
        @Override
        public synchronized void launch(ILaunchConfiguration configuration, String mode,
                ILaunch launch, IProgressMonitor monitor) throws CoreException {
            // TODO: is progress monitor adjustment needed here?
            super.launch(configuration, mode, launch, monitor);
        }

        /**
         * {@inheritDoc}
         * @see org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate#verifyMainTypeName(org.eclipse.debug.core.ILaunchConfiguration)
         */
        @Override
        public String verifyMainTypeName(ILaunchConfiguration configuration) {
            return "org.eclipse.andmore.junit.internal.runner.RemoteAndroidTestRunner"; //$NON-NLS-1$
        }

        /**
         * Overrides parent to return a VM Runner implementation which launches a thread, rather
         * than a separate VM process
         */
        @Override
        public IVMRunner getVMRunner(ILaunchConfiguration configuration, String mode) {
            return new VMTestRunner(mLaunchInfo);
        }

        /**
         * {@inheritDoc}
         * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate#getLaunch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
         */
        @Override
        public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) {
            return mLaunchInfo.getLaunch();
        }
    }

    /**
     * Provides a VM runner implementation which starts a inline implementation of a launch process
     */
    private static class VMTestRunner implements IVMRunner {

        private final AndroidJUnitLaunchInfo mJUnitInfo;

        VMTestRunner(AndroidJUnitLaunchInfo info) {
            mJUnitInfo = info;
        }

        /**
         * {@inheritDoc}
         * @throws CoreException
         */
        @Override
        public void run(final VMRunnerConfiguration config, ILaunch launch,
                IProgressMonitor monitor) throws CoreException {

            TestRunnerProcess runnerProcess =
                new TestRunnerProcess(config, mJUnitInfo);
            launch.addProcess(runnerProcess);
            runnerProcess.run();
        }
    }

    /**
     * Launch process that executes the tests.
     */
    private static class TestRunnerProcess implements IProcess  {

        private final VMRunnerConfiguration mRunConfig;
        private final AndroidJUnitLaunchInfo mJUnitInfo;
        private RemoteAdtTestRunner mTestRunner = null;
        private boolean mIsTerminated = false;

        TestRunnerProcess(VMRunnerConfiguration runConfig, AndroidJUnitLaunchInfo info) {
            mRunConfig = runConfig;
            mJUnitInfo = info;
        }

        /* (non-Javadoc)
         * @see org.eclipse.debug.core.model.IProcess#getAttribute(java.lang.String)
         */
        @Override
        public String getAttribute(String key) {
            return null;
        }

        /**
         * {@inheritDoc}
         * @see org.eclipse.debug.core.model.IProcess#getExitValue()
         */
        @Override
        public int getExitValue() {
            return 0;
        }

        /* (non-Javadoc)
         * @see org.eclipse.debug.core.model.IProcess#getLabel()
         */
        @Override
        public String getLabel() {
            return mJUnitInfo.getLaunch().getLaunchMode();
        }

        /* (non-Javadoc)
         * @see org.eclipse.debug.core.model.IProcess#getLaunch()
         */
        @Override
        public ILaunch getLaunch() {
            return mJUnitInfo.getLaunch();
        }

        /* (non-Javadoc)
         * @see org.eclipse.debug.core.model.IProcess#getStreamsProxy()
         */
        @Override
        public IStreamsProxy getStreamsProxy() {
            return null;
        }

        /* (non-Javadoc)
         * @see org.eclipse.debug.core.model.IProcess#setAttribute(java.lang.String,
         * java.lang.String)
         */
        @Override
        public void setAttribute(String key, String value) {
            // ignore
        }

        /* (non-Javadoc)
         * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
         */
        @Override
        public Object getAdapter(Class adapter) {
            return null;
        }

        /* (non-Javadoc)
         * @see org.eclipse.debug.core.model.ITerminate#canTerminate()
         */
        @Override
        public boolean canTerminate() {
            return true;
        }

        /* (non-Javadoc)
         * @see org.eclipse.debug.core.model.ITerminate#isTerminated()
         */
        @Override
        public boolean isTerminated() {
            return mIsTerminated;
        }

        /**
         * {@inheritDoc}
         * @see org.eclipse.debug.core.model.ITerminate#terminate()
         */
        @Override
        public void terminate() {
            if (mTestRunner != null) {
                mTestRunner.terminate();
            }
            mIsTerminated = true;
        }

        /**
         * Launches a test runner that will communicate results back to JDT JUnit UI.
         * <p/>
         * Must be executed on a non-UI thread.
         */
        public void run() {
            if (Display.getCurrent() != null) {
                AdtPlugin.log(IStatus.ERROR, "Adt test runner executed on UI thread");
                AdtPlugin.printErrorToConsole(mJUnitInfo.getProject(),
                        "Test launch failed due to internal error: Running tests on UI thread");
                terminate();
                return;
            }
            mTestRunner = new RemoteAdtTestRunner();
            mTestRunner.runTests(mRunConfig.getProgramArguments(), mJUnitInfo);
        }
    }
}

