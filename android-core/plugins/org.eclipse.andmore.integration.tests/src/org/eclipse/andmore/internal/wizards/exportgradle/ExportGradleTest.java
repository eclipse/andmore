/*
 * Copyright (C) 2013 The Android Open Source Project
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
package org.eclipse.andmore.internal.wizards.exportgradle;


import static org.junit.Assert.*;
import static com.android.sdklib.internal.project.ProjectProperties.PROPERTY_LIBRARY;

import com.android.SdkConstants;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.internal.wizards.newproject.NewProjectCreator;
import org.eclipse.andmore.internal.wizards.newproject.NewProjectWizardState;
import org.eclipse.andmore.internal.wizards.newproject.NewProjectWizardState.Mode;

import com.android.sdklib.internal.project.ProjectPropertiesWorkingCopy;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.eclipse.andmore.internal.editors.layout.refactoring.AdtProjectTest;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

@Ignore
public class ExportGradleTest extends AdtProjectTest {
	private QualifiedName ERROR_KEY = new QualifiedName(AdtPlugin.PLUGIN_ID, "JobErrorKey");
	private Throwable mLastThrown;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		mLastThrown = null;
	}

	@Test
	public void testSimpleAndroidApp() throws Throwable {
		IProject project = getProject("simple-app");
		final IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);

		final ProjectSetupBuilder builder = new ProjectSetupBuilder();
		builder.setProject(Collections.singletonList(javaProject));

		Job job = new Job("Validate project") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					BuildFileCreator.createBuildFiles(builder, null, monitor);
					File buildfile = new File(javaProject.getResource().getLocation().toString(),
							BuildFileCreator.BUILD_FILE);
					assertTrue(buildfile.exists());
					String contents = Files.toString(buildfile, Charsets.UTF_8);
					String expectedContents = "buildscript {\n" + "    repositories {\n" + "        "
							+ BuildFileCreator.MAVEN_REPOSITORY + "\n" + "    }\n" + "    dependencies {\n"
							+ "        " + BuildFileCreator.PLUGIN_CLASSPATH + "\n" + "    }\n" + "}\n"
							+ "apply plugin: 'android'\n" + "\n" + "dependencies {\n" + "}\n" + "\n" + "android {\n"
							+ "    compileSdkVersion 16\n" + "    buildToolsVersion \"16\"\n" + "\n"
							+ "    defaultConfig {\n" + "        minSdkVersion 1\n" + "        targetSdkVersion 1\n"
							+ "    }\n" + "    sourceSets {\n" + "        main {\n"
							+ "            manifest.srcFile 'AndroidManifest.xml'\n"
							+ "            java.srcDirs = ['src']\n" + "            resources.srcDirs = ['src']\n"
							+ "            aidl.srcDirs = ['src']\n" + "            renderscript.srcDirs = ['src']\n"
							+ "            res.srcDirs = ['res']\n" + "            assets.srcDirs = ['assets']\n"
							+ "        }\n" + "        instrumentTest.setRoot('tests')\n" + "    }\n" + "}";

					assertEqualsWhitespaceInsensitive(expectedContents, contents);
				} catch (Throwable t) {
					mLastThrown = t;
				}
				return null;
			}
		};
		job.schedule(1000);
		job.join();
		Object property = job.getProperty(ERROR_KEY);
		assertNull(property);
		if (mLastThrown != null) {
			throw mLastThrown;
		}
	}

	@Test
	public void testSimpleAndroidLib() throws Throwable {
		final IProject project = getProject("simple-library");
		ProjectState projectState = Sdk.getProjectState(project.getProject());
		ProjectPropertiesWorkingCopy propertiesWorkingCopy = projectState.getProperties().makeWorkingCopy();
		propertiesWorkingCopy.setProperty(PROPERTY_LIBRARY, "true");
		propertiesWorkingCopy.save();
		IResource projectProp = project.findMember(SdkConstants.FN_PROJECT_PROPERTIES);
		if (projectProp != null) {
			projectProp.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
		}

		Job job = new Job("Validate project") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);

					final ProjectSetupBuilder builder = new ProjectSetupBuilder();
					builder.setProject(Collections.singletonList(javaProject));

					BuildFileCreator.createBuildFiles(builder, null, monitor);
					File buildfile = new File(javaProject.getResource().getLocation().toString(),
							BuildFileCreator.BUILD_FILE);
					assertTrue(buildfile.exists());
					String contents = Files.toString(buildfile, Charsets.UTF_8);
					String expectedContents = "buildscript {\n" + "    repositories {\n" + "        "
							+ BuildFileCreator.MAVEN_REPOSITORY + "\n" + "    }\n" + "    dependencies {\n"
							+ "        " + BuildFileCreator.PLUGIN_CLASSPATH + "\n" + "    }\n" + "}\n"
							+ "apply plugin: 'android-library'\n" + "\n" + "dependencies {\n" + "}\n" + "\n"
							+ "android {\n" + "    compileSdkVersion 16\n" + "    buildToolsVersion \"16\"\n" + "\n"
							+ "    defaultConfig {\n" + "        minSdkVersion 1\n" + "        targetSdkVersion 1\n"
							+ "    }\n" + "    sourceSets {\n" + "        main {\n"
							+ "            manifest.srcFile 'AndroidManifest.xml'\n"
							+ "            java.srcDirs = ['src']\n" + "            resources.srcDirs = ['src']\n"
							+ "            aidl.srcDirs = ['src']\n" + "            renderscript.srcDirs = ['src']\n"
							+ "            res.srcDirs = ['res']\n" + "            assets.srcDirs = ['assets']\n"
							+ "        }\n" + "        instrumentTest.setRoot('tests')\n" + "    }\n" + "}";

					assertEqualsWhitespaceInsensitive(expectedContents, contents);
				} catch (Throwable t) {
					mLastThrown = t;
				}
				return null;
			}
		};
		job.schedule(1000);
		job.join();
		Object property = job.getProperty(ERROR_KEY);
		assertNull(property);
		if (mLastThrown != null) {
			throw mLastThrown;
		}
	}

	@Test
	public void testPlainJavaProject() throws Throwable {
		IProject project = getJavaProject("simple-java");
		final IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);

		final ProjectSetupBuilder builder = new ProjectSetupBuilder();
		builder.setProject(Collections.singletonList(javaProject));

		BuildFileCreator.createBuildFiles(builder, null, null);
		Job job = new Job("Validate project") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					File buildfile = new File(javaProject.getResource().getLocation().toString(), "build.gradle");
					assertTrue(buildfile.exists());
					String contents = Files.toString(buildfile, Charsets.UTF_8);
					String expectedContents = "apply plugin: 'java'\n" + "sourceSets {\n"
							+ "    main.java.srcDirs = ['src']\n" + "}";

					assertEqualsWhitespaceInsensitive(expectedContents, contents);
				} catch (Throwable t) {
					mLastThrown = t;
				}
				return null;
			}
		};
		job.schedule(1000);
		job.join();
		Object property = job.getProperty(ERROR_KEY);
		assertNull(property);
		if (mLastThrown != null) {
			throw mLastThrown;
		}
	}

	protected IProject getProject(String projectName) {
		IProject project = createProject(projectName);
		assertNotNull(project);
		if (!testNeedsUniqueProject()) {
			addCleanupDir(AdtUtils.getAbsolutePath(project).toFile());
		}
		addCleanupDir(project.getFullPath().toFile());
		return project;
	}

	protected IProject getJavaProject(String projectName) {
		IProject project = createJavaProject(projectName);
		assertNotNull(project);
		if (!testNeedsUniqueProject()) {
			addCleanupDir(AdtUtils.getAbsolutePath(project).toFile());
		}
		addCleanupDir(project.getFullPath().toFile());
		return project;
	}

	protected IProject createJavaProject(String name) {
		IRunnableContext context = new IRunnableContext() {
			@Override
			public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable)
					throws InvocationTargetException, InterruptedException {
				runnable.run(new NullProgressMonitor());
			}
		};
		NewProjectWizardState state = new NewProjectWizardState(Mode.ANY);
		state.projectName = name;
		state.packageName = TEST_PROJECT_PACKAGE;
		state.activityName = name;
		state.applicationName = name;
		state.createActivity = false;
		state.useDefaultLocation = true;
		if (getMinSdk() != -1) {
			state.minSdk = Integer.toString(getMinSdk());
		}

		NewProjectCreator creator = new NewProjectCreator(state, context);
		creator.createJavaProjects();
		return validateProjectExists(name);
	}

	/**
	 * Compares two strings, disregarding whitespace. This makes the test less
	 * brittle with respect to insignificant changes.
	 */
	protected void assertEqualsWhitespaceInsensitive(String a, String b) {
		a = stripWhitespace(a);
		b = stripWhitespace(b);
		assertEquals("Expected:\n" + a + "\nbut was:\n" + b + "\n\n", a, b);
	}

	protected String stripWhitespace(String s) {
		return s.replaceAll("\\s", "");
	}
}