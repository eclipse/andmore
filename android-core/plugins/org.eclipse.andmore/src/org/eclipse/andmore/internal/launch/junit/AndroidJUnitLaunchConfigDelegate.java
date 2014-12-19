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

import com.android.SdkConstants;
import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner.TestSize;
import com.android.ide.common.xml.ManifestData;
import com.android.ide.common.xml.ManifestData.Instrumentation;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.launch.AndroidLaunch;
import org.eclipse.andmore.internal.launch.AndroidLaunchConfiguration;
import org.eclipse.andmore.internal.launch.AndroidLaunchController;
import org.eclipse.andmore.internal.launch.IAndroidLaunchAction;
import org.eclipse.andmore.internal.launch.LaunchConfigDelegate;
import org.eclipse.andmore.internal.launch.LaunchMessages;
import org.eclipse.andmore.internal.launch.junit.runtime.AndroidJUnitLaunchInfo;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.swt.widgets.Display;

/**
 * Run configuration that can execute JUnit tests on an Android platform.
 * <p/>
 * Will deploy apps on target Android platform by reusing functionality from ADT
 * LaunchConfigDelegate, and then run JUnits tests by reusing functionality from JDT
 * JUnitLaunchConfigDelegate.
 */
@SuppressWarnings("restriction")
public class AndroidJUnitLaunchConfigDelegate extends LaunchConfigDelegate {

    /** Launch config attribute that stores instrumentation runner. */
    static final String ATTR_INSTR_NAME = AdtPlugin.PLUGIN_ID + ".instrumentation"; //$NON-NLS-1$

    /** Launch config attribute that stores the test size annotation to run. */
    static final String ATTR_TEST_SIZE = AdtPlugin.PLUGIN_ID + ".testSize"; //$NON-NLS-1$

    private static final String EMPTY_STRING = ""; //$NON-NLS-1$

    @Override
    protected void doLaunch(final ILaunchConfiguration configuration, final String mode,
            final IProgressMonitor monitor, final IProject project,
            final AndroidLaunch androidLaunch, final AndroidLaunchConfiguration config,
            final AndroidLaunchController controller, final IFile applicationPackage,
            final ManifestData manifestData) {

        String runner = getRunner(project, configuration, manifestData);
        if (runner == null) {
            AdtPlugin.displayError(LaunchMessages.LaunchDialogTitle,
                    String.format(LaunchMessages.AndroidJUnitDelegate_NoRunnerMsg_s,
                            project.getName()));
            androidLaunch.stopLaunch();
            return;
        }
        // get the target app's package
        final String targetAppPackage = getTargetPackage(manifestData, runner);
        if (targetAppPackage == null) {
            AdtPlugin.displayError(LaunchMessages.LaunchDialogTitle,
                    String.format(LaunchMessages.AndroidJUnitDelegate_NoTargetMsg_3s,
                            project.getName(), runner, SdkConstants.FN_ANDROID_MANIFEST_XML));
            androidLaunch.stopLaunch();
            return;
        }
        final String testAppPackage = manifestData.getPackage();
        AndroidJUnitLaunchInfo junitLaunchInfo = new AndroidJUnitLaunchInfo(project,
                testAppPackage, runner);
        junitLaunchInfo.setTestClass(getTestClass(configuration));
        junitLaunchInfo.setTestPackage(getTestPackage(configuration));
        junitLaunchInfo.setTestMethod(getTestMethod(configuration));
        junitLaunchInfo.setLaunch(androidLaunch);
        junitLaunchInfo.setTestSize(getTestSize(configuration));
        final IAndroidLaunchAction junitLaunch = new AndroidJUnitLaunchAction(junitLaunchInfo);

        // launch on a separate thread if currently on the display thread
        if (Display.getCurrent() != null) {
            Job job = new Job("Junit Launch") {     //$NON-NLS-1$
                @Override
                protected IStatus run(IProgressMonitor m) {
                    controller.launch(project, mode, applicationPackage, testAppPackage,
                            targetAppPackage, manifestData.getDebuggable(),
                            manifestData.getMinSdkVersionString(),
                            junitLaunch, config, androidLaunch, monitor);
                    return Status.OK_STATUS;
                }
            };
            job.setPriority(Job.INTERACTIVE);
            job.schedule();
        } else {
            controller.launch(project, mode, applicationPackage, testAppPackage, targetAppPackage,
                    manifestData.getDebuggable(), manifestData.getMinSdkVersionString(),
                    junitLaunch, config, androidLaunch, monitor);
        }
    }

    /**
     * Get the target Android application's package for the given instrumentation runner, or
     * <code>null</code> if it could not be found.
     *
     * @param manifestParser the {@link ManifestData} for the test project
     * @param runner the instrumentation runner class name
     * @return the target package or <code>null</code>
     */
    private String getTargetPackage(ManifestData manifestParser, String runner) {
        for (Instrumentation instr : manifestParser.getInstrumentations()) {
            if (instr.getName().equals(runner)) {
                return instr.getTargetPackage();
            }
        }
        return null;
    }

    /**
     * Returns the test package stored in the launch configuration, or <code>null</code> if not
     * specified.
     *
     * @param configuration the {@link ILaunchConfiguration} to retrieve the test package info from
     * @return the test package or <code>null</code>.
     */
    private String getTestPackage(ILaunchConfiguration configuration) {
        // try to retrieve a package name from the JUnit container attribute
        String containerHandle = getStringLaunchAttribute(
                JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, configuration);
        if (containerHandle != null && containerHandle.length() > 0) {
            IJavaElement element = JavaCore.create(containerHandle);
            // containerHandle could be a IProject, check if its a java package
            if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
                return element.getElementName();
            }
        }
        return null;
    }

    /**
     * Returns the test class stored in the launch configuration.
     *
     * @param configuration the {@link ILaunchConfiguration} to retrieve the test class info from
     * @return the test class. <code>null</code> if not specified.
     */
    private String getTestClass(ILaunchConfiguration configuration) {
        return getStringLaunchAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
                configuration);
    }

    /**
     * Returns the test method stored in the launch configuration.
     *
     * @param configuration the {@link ILaunchConfiguration} to retrieve the test method info from
     * @return the test method. <code>null</code> if not specified.
     */
    private String getTestMethod(ILaunchConfiguration configuration) {
        return getStringLaunchAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_METHOD_NAME,
                configuration);
    }

    /**
     * Returns the test sizes to run as saved in the launch configuration.
     * @return {@link TestSize} if only tests of specific sizes should be run,
     *         null if all tests should be run
     */
    private TestSize getTestSize(ILaunchConfiguration configuration) {
        String testSizeAnnotation = getStringLaunchAttribute(
                AndroidJUnitLaunchConfigDelegate.ATTR_TEST_SIZE,
                configuration);
        if (AndroidJUnitLaunchConfigurationTab.SMALL_TEST_ANNOTATION.equals(
                    testSizeAnnotation)){
            return TestSize.SMALL;
        } else if (AndroidJUnitLaunchConfigurationTab.MEDIUM_TEST_ANNOTATION.equals(
                    testSizeAnnotation)) {
            return TestSize.MEDIUM;
        } else if (AndroidJUnitLaunchConfigurationTab.LARGE_TEST_ANNOTATION.equals(
                    testSizeAnnotation)) {
            return TestSize.LARGE;
        } else {
            return null;
        }
    }

    /**
     * Gets a instrumentation runner for the launch.
     * <p/>
     * If a runner is stored in the given <code>configuration</code>, will return that.
     * Otherwise, will try to find the first valid runner for the project.
     * If a runner can still not be found, will return <code>null</code>, and will log an error
     * to the console.
     *
     * @param project the {@link IProject} for the app
     * @param configuration the {@link ILaunchConfiguration} for the launch
     * @param manifestData the {@link ManifestData} for the project
     *
     * @return <code>null</code> if no instrumentation runner can be found, otherwise return
     *   the fully qualified runner name.
     */
    private String getRunner(IProject project, ILaunchConfiguration configuration,
            ManifestData manifestData) {
        try {
            String runner = getRunnerFromConfig(configuration);
            if (runner != null) {
                return runner;
            }
            final InstrumentationRunnerValidator instrFinder = new InstrumentationRunnerValidator(
                    BaseProjectHelper.getJavaProject(project), manifestData);
            runner = instrFinder.getValidInstrumentationTestRunner();
            if (runner != null) {
                AdtPlugin.printErrorToConsole(project, String.format(
                        LaunchMessages.AndroidJUnitDelegate_NoRunnerConfigMsg_s, runner));
                return runner;
            }
            AdtPlugin.printErrorToConsole(project, String.format(
                    LaunchMessages.AndroidJUnitDelegate_NoRunnerConsoleMsg_4s,
                    project.getName(),
                    SdkConstants.CLASS_INSTRUMENTATION_RUNNER,
                    AdtConstants.LIBRARY_TEST_RUNNER,
                    SdkConstants.FN_ANDROID_MANIFEST_XML));
            return null;
        } catch (CoreException e) {
            AdtPlugin.log(e, "Error when retrieving instrumentation info"); //$NON-NLS-1$
        }

        return null;
    }

    private String getRunnerFromConfig(ILaunchConfiguration configuration) {
        return getStringLaunchAttribute(ATTR_INSTR_NAME, configuration);
    }

    /**
     * Helper method to retrieve a string attribute from the launch configuration
     *
     * @param attributeName name of the launch attribute
     * @param configuration the {@link ILaunchConfiguration} to retrieve the attribute from
     * @return the attribute's value. <code>null</code> if not found.
     */
    private String getStringLaunchAttribute(String attributeName,
            ILaunchConfiguration configuration) {
        try {
            String attrValue = configuration.getAttribute(attributeName, EMPTY_STRING);
            if (attrValue.length() < 1) {
                return null;
            }
            return attrValue;
        } catch (CoreException e) {
            AdtPlugin.log(e, String.format("Error when retrieving launch info %1$s",  //$NON-NLS-1$
                    attributeName));
        }
        return null;
    }

    /**
     * Helper method to set JUnit-related attributes expected by JDT JUnit runner
     *
     * @param config the launch configuration to modify
     */
    static void setJUnitDefaults(ILaunchConfigurationWorkingCopy config) {
        // set the test runner to JUnit3 to placate JDT JUnit runner logic
        config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND,
                TestKindRegistry.JUNIT3_TEST_KIND_ID);
    }
}
