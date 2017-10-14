/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.internal.wizards.templates;

import static org.junit.Assert.*;
import static com.android.SdkConstants.CURRENT_PLATFORM;
import static com.android.SdkConstants.FD_TOOLS;
import static com.android.SdkConstants.PLATFORM_WINDOWS;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_MIN_API;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_MIN_BUILD_API;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_ID;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.SdkVersionInfo;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.integration.tests.SdkLoadingTestCase;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.sdk.Sdk;

import com.android.sdklib.IAndroidTarget;
import com.android.utils.GrabProcessOutput;
import com.android.utils.GrabProcessOutput.IProcessOutput;
import com.android.utils.GrabProcessOutput.Wait;
import com.android.tools.lint.checks.BuiltinIssueRegistry;
import com.android.tools.lint.checks.ManifestDetector;
import com.android.tools.lint.checks.SecurityDetector;
import com.android.tools.lint.client.api.Configuration;
import com.android.tools.lint.client.api.DefaultConfiguration;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.client.api.XmlParser;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.TextFormat;
import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import org.eclipse.andmore.internal.lint.EclipseLintClient;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Unit tests for template instantiation.
 * <p>
 * Note: This test can take multiple hours to run!
 *
 * <p>
 * TODO: Test all permutations of variables (it currently just varies one at a
 * time with the rest of the defaults) TODO: Test trying to change strings
 * arguments (currently just varies enums and booleans) TODO: Test adding
 * multiple instances of the templates (to look for resource conflicts)
 */
@SuppressWarnings("javadoc")
@Ignore("Causing garbage collection issues in JVM.")
public class TemplateHandlerTest extends SdkLoadingTestCase {
	/**
	 * Flag used to quickly check each template once (for one version), to get
	 * quicker feedback on whether something is broken instead of waiting for
	 * all the versions for each template first
	 */
	private static final boolean TEST_FEWER_API_VERSIONS = true;
	private static final boolean TEST_JUST_ONE_MIN_SDK = false;
	private static final boolean TEST_JUST_ONE_BUILD_TARGET = true;
	private static final boolean TEST_JUST_ONE_TARGET_SDK_VERSION = true;
	private QualifiedName ERROR_KEY = new QualifiedName(AndmoreAndroidPlugin.PLUGIN_ID, "JobErrorKey");
	private static int sCount = 0;
	/**
	 * If true, check this template with all the interesting (
	 * {@link #isInterestingApiLevel(int)}) api versions
	 */
	private boolean mApiSensitiveTemplate;
	/**
	 * Set of templates already tested with separate unit test; remainder is
	 * checked in {@link #testCreateRemainingProjects()}
	 */
	private static final Set<File> sProjectTestedSeparately = Sets.newHashSet();
	/**
	 * Set of templates already tested with separate unit test; remainder is
	 * checked in {@link #testCreateRemainingTemplates()}
	 */
	private static final Set<File> sTemplateTestedSeparately = Sets.newHashSet();

	@Before
	public void setUp() throws Exception {
		mApiSensitiveTemplate = true;
	}
	
	@After
	public void deleteProjects() throws Exception {
		if (project != null) {
			project.delete(true, true, new NullProgressMonitor());
		}
	}

	/**
	 * Is the given api level interesting for testing purposes? This is used to
	 * skip gaps, such that we for example only check say api 8, 9, 11, 14, etc
	 * -- versions where the <b>templates</b> are doing conditional changes. To
	 * be EXTRA comprehensive, occasionally try returning true unconditionally
	 * here to test absolutely everything.
	 */
	private boolean isInterestingApiLevel(int api) {
		// For templates that aren't API sensitive, only test with API = 16
		if (!mApiSensitiveTemplate) {
			return api == 16;
		}

		switch (api) {
		case 1:
		case 8:
			return true;
		case 11:
			return true;
		case 14:
			return true;
		case 9:
		case 16:
			return !TEST_FEWER_API_VERSIONS;
		default:
			return false;
		}
	}

	@Test
	public void testNewBlankProject() throws Exception {
		Stopwatch stopwatch = Stopwatch.createStarted();
		checkProjectWithActivity(null);
		stopwatch.stop();
		System.out.println("Checked blank project successfully in " + stopwatch.toString());
	}

	@Test
	public void testNewBlankActivity() throws Exception {
		checkCreateTemplate("activities", "BlankActivity");
	}

	@Test
	public void testBlankActivityInProject() throws Exception {
		checkCreateActivityInProject("BlankActivity");
	}

	@Test
	public void testNewMasterDetailFlow() throws Exception {
		checkCreateTemplate("activities", "MasterDetailFlow");
	}

	@Test
	public void testMasterDetailFlowInProject() throws Exception {
		checkCreateActivityInProject("MasterDetailFlow");
	}

	@Test
	public void testNewFullscreen() throws Exception {
		checkCreateTemplate("activities", "FullscreenActivity");
	}

	@Test
	public void testFullscreenInProject() throws Exception {
		checkCreateActivityInProject("FullscreenActivity");
	}

	@Test
	public void testNewLoginActivity() throws Exception {
		checkCreateTemplate("activities", "LoginActivity");
	}

	@Test
	public void testLoginActivityInProject() throws Exception {
		checkCreateActivityInProject("MasterDetailFlow");
	}

	@Test
	public void testNewSettingsActivity() throws Exception {
		checkCreateTemplate("activities", "SettingsActivity");
	}

	@Test
	public void testSettingsActivityInProject() throws Exception {
		checkCreateActivityInProject("SettingsActivity");
	}

	@Test
	public void testNewBroadcastReceiver() throws Exception {
		// No need to try this template with multiple platforms, one is adequate
		mApiSensitiveTemplate = false;
		checkCreateTemplate("other", "BroadcastReceiver");
	}

	@Test
	public void testNewContentProvider() throws Exception {
		mApiSensitiveTemplate = false;
		checkCreateTemplate("other", "ContentProvider");
	}

	@Test
	public void testNewCustomView() throws Exception {
		mApiSensitiveTemplate = false;
		checkCreateTemplate("other", "CustomView");
	}

	@Test
	public void testNewService() throws Exception {
		mApiSensitiveTemplate = false;
		checkCreateTemplate("other", "Service");
	}

	@Test
	@Ignore
	public void testCreateRemainingTemplates() throws Exception {
		sCount = 0;
		long begin = System.currentTimeMillis();
		TemplateManager manager = new TemplateManager();
		List<File> other = manager.getTemplates("other");
		for (File templateFile : other) {
			if (sTemplateTestedSeparately.contains(templateFile)) {
				continue;
			}
			checkTemplate(templateFile);
		}
		// Also try creating templates, not as part of creating a project
		List<File> activities = manager.getTemplates("activities");
		for (File templateFile : activities) {
			if (sTemplateTestedSeparately.contains(templateFile)) {
				continue;
			}
			checkTemplate(templateFile);
		}
		long end = System.currentTimeMillis();
		System.out.println("Successfully checked " + sCount + " template permutations in "
				+ ((end - begin) / (1000 * 60)) + " minutes");
	}

	@Test
	public void testCreateRemainingProjects() throws Exception {
		sCount = 0;
		long begin = System.currentTimeMillis();
		TemplateManager manager = new TemplateManager();
		List<File> templates = manager.getTemplates("activities");
		for (File activityFile : templates) {
			if (sTemplateTestedSeparately.contains(activityFile)) {
				continue;
			}
			checkProjectWithActivity(activityFile.getName());
		}
		long end = System.currentTimeMillis();
		System.out.println("Successfully checked " + sCount + " project permutations in "
				+ ((end - begin) / (1000 * 60)) + " minutes");
	}

	// ---- Test support code below ----

	private void checkCreateActivityInProject(String activityName) throws Exception {
		Stopwatch stopwatch = Stopwatch.createStarted();
		File templateFile = findTemplate("activities", activityName);
		sProjectTestedSeparately.add(templateFile);
		checkProjectWithActivity(templateFile.getName());
		stopwatch.stop();
		System.out.println("Checked " + templateFile.getName() + " successfully in " + stopwatch.toString());
	}

	private void checkCreateTemplate(String category, String name) throws Exception {
		Stopwatch stopwatch = Stopwatch.createStarted();
		File templateFile = findTemplate(category, name);
		assertNotNull(templateFile);
		sTemplateTestedSeparately.add(templateFile);
		checkTemplate(templateFile);
		stopwatch.stop();
		System.out.println("Checked " + templateFile.getName() + " successfully in " + stopwatch.toString());
	}

	private static File findTemplate(String category, String name) {
		File templateRootFolder = TemplateManager.getTemplateRootFolder();
		assertNotNull(templateRootFolder);
		File file = new File(templateRootFolder, category + File.separator + name);
		assertTrue(file.getPath(), file.exists());
		return file;
	}

	private void checkTemplate(File templateFile) throws Exception {
		NewProjectWizardState values = new NewProjectWizardState();
		values.applicationName = "My Application";
		values.packageName = "my.pkg2";

		values.isLibrary = false;
		values.createIcon = false;
		values.useDefaultLocation = true;
		values.createActivity = false;

		String projectNameBase = "MyTemplateProject_" + templateFile.getName();
		values.projectName = projectNameBase;
		values.createActivity = false;

		// Create the new template

		NewTemplateWizardState state = new NewTemplateWizardState();
		state.setTemplateLocation(templateFile);
		state.minSdkLevel = values.minSdkLevel;

		// Iterate over all (valid) combinations of build target, minSdk and
		// targetSdk
		Collection<IAndroidTarget> targets = Sdk.getCurrent().getTargets();
		for (IAndroidTarget target : targets) {
			if (!target.isPlatform()) {
				continue;
			}
			if (!isInterestingApiLevel(target.getVersion().getApiLevel())) {
				continue;
			}

			for (int minSdk = 1; minSdk <= SdkVersionInfo.HIGHEST_KNOWN_API; minSdk++) {
				// Don't bother checking *every* single minSdk, just pick some
				// interesting ones
				if (!isInterestingApiLevel(minSdk)) {
					continue;
				}

				for (int targetSdk = minSdk; targetSdk <= SdkVersionInfo.HIGHEST_KNOWN_API; targetSdk++) {
					if (!isInterestingApiLevel(targetSdk)) {
						continue;
					}

					// Make sure this template is supported with these versions
					IStatus status = values.template.validateTemplate(minSdk, target.getVersion().getApiLevel());
					if (status != null && !status.isOK()) {
						continue;
					}

					// Also make sure activity is enabled for these versions
					status = state.getTemplateHandler().validateTemplate(minSdk, target.getVersion().getApiLevel());
					if (status != null && !status.isOK()) {
						continue;
					}

					// Iterate over all new new project templates

					// should I try all options of theme with all platforms?
					// or just try all platforms, with one setting for each?
					// doesn't seem like I need to multiply
					// just pick the best setting that applies instead for each
					// platform
					List<Parameter> parameters = values.template.getTemplate().getParameters();
					projectParameters: for (Parameter parameter : parameters) {
						List<Element> options = parameter.getOptions();
						if (parameter.type == Parameter.Type.ENUM) {
							for (Element element : options) {
								Option option = Option.get(element);
								String optionId = option.id;
								int optionMinSdk = option.minSdk;
								int optionMinBuildApi = option.minBuild;
								if (optionMinSdk <= minSdk && optionMinBuildApi <= target.getVersion().getApiLevel()) {
									values.parameters.put(parameter.id, optionId);
									if (parameter.id.equals("baseTheme")) {
										String base = projectNameBase + "_min_" + minSdk + "_target_" + targetSdk
												+ "_build_" + target.getVersion().getApiLevel() + "_theme_" + optionId;
										System.out.println("checking base " + base);

										checkApiTarget(minSdk, targetSdk, target, values, base, state);
										break projectParameters;
									}
								}
							}
						}
					}

					if (TEST_JUST_ONE_TARGET_SDK_VERSION) {
						break;
					}
				}

				if (TEST_JUST_ONE_MIN_SDK) {
					break;
				}
			}

			if (TEST_JUST_ONE_BUILD_TARGET) {
				break;
			}
		}
	}

	private void checkProjectWithActivity(String activity) throws Exception {
		NewProjectWizardState values = new NewProjectWizardState();
		values.applicationName = "My Application";
		values.packageName = "my.pkg";

		values.isLibrary = false;
		values.createIcon = false;
		values.useDefaultLocation = true;

		// These are basically unused; passed as defaults
		values.activityName = activity == null ? "Blank" : activity;
		values.activityTitle = "My Activity Title";

		String projectNameBase = "MyProject_" + values.activityName;
		values.projectName = projectNameBase;

		values.createActivity = activity != null;
		NewTemplateWizardState activityValues = values.activityValues;
		assertNotNull(activityValues);
		activityValues.minSdkLevel = values.minSdkLevel;

		// Iterate over all (valid) combinations of build target, minSdk and
		// targetSdk
		Collection<IAndroidTarget> targets = Sdk.getCurrent().getTargets();
		for (IAndroidTarget target : targets) {
			if (!target.isPlatform()) {
				continue;
			}
			if (!isInterestingApiLevel(target.getVersion().getApiLevel())) {
				continue;
			}

			for (int minSdk = 1; minSdk <= SdkVersionInfo.HIGHEST_KNOWN_API; minSdk++) {
				// Don't bother checking *every* single minSdk, just pick some
				// interesting ones
				if (!isInterestingApiLevel(minSdk)) {
					continue;
				}

				for (int targetSdk = minSdk; targetSdk <= SdkVersionInfo.HIGHEST_KNOWN_API; targetSdk++) {
					if (!isInterestingApiLevel(targetSdk)) {
						continue;
					}

					// Make sure this template is supported with these versions
					IStatus status = values.template.validateTemplate(values.minSdkLevel, values.getBuildApi());
					if (status != null && !status.isOK()) {
						continue;
					}

					// Also make sure activity is enabled for these versions
					status = values.activityValues.getTemplateHandler().validateTemplate(values.minSdkLevel,
							values.getBuildApi());
					if (status != null && !status.isOK()) {
						continue;
					}

					// Iterate over all new new project templates

					// should I try all options of theme with all platforms?
					// or just try all platforms, with one setting for each?
					// doesn't seem like I need to multiply
					// just pick the best setting that applies instead for each
					// platform
					List<Parameter> parameters = values.template.getTemplate().getParameters();
					for (Parameter parameter : parameters) {
						List<Element> options = parameter.getOptions();
						if (parameter.type == Parameter.Type.ENUM) {
							for (Element element : options) {
								Option option = Option.get(element);
								String optionId = option.id;
								int optionMinSdk = option.minSdk;
								int optionMinBuildApi = option.minBuild;
								if (optionMinSdk <= minSdk && optionMinBuildApi <= target.getVersion().getApiLevel()) {
									values.parameters.put(parameter.id, optionId);
									if (parameter.id.equals("baseTheme")) {
										String base = projectNameBase + "_min_" + minSdk + "_target_" + targetSdk
												+ "_build_" + target.getVersion().getApiLevel() + "_theme_" + optionId;
										System.out.println("checking base " + base);

										checkApiTarget(minSdk, targetSdk, target, values, base, null);

									}
								}
							}
						}
					}

					if (TEST_JUST_ONE_TARGET_SDK_VERSION) {
						break;
					}
				}

				if (TEST_JUST_ONE_MIN_SDK) {
					break;
				}
			}

			if (TEST_JUST_ONE_BUILD_TARGET) {
				break;
			}
		}
	}

	private void checkApiTarget(int minSdk, int targetSdk, @NonNull IAndroidTarget target,
			@NonNull NewProjectWizardState projectValues, @NonNull String projectNameBase,
			@Nullable NewTemplateWizardState templateValues) throws Exception {
		NewTemplateWizardState values = projectValues.createActivity ? projectValues.activityValues : templateValues;

		projectValues.minSdk = Integer.toString(minSdk);
		projectValues.minSdkLevel = minSdk;
		projectValues.targetSdkLevel = targetSdk;
		projectValues.target = target;

		if (values == null) {
			checkProject(projectValues, templateValues);
			return;
		}

		// Next check all other parameters, cycling through booleans and enums.
		TemplateHandler templateHandler = values.getTemplateHandler();
		TemplateMetadata template = templateHandler.getTemplate();
		assertNotNull(template);
		List<Parameter> parameters = template.getParameters();

		if (!projectValues.createActivity) {
			for (Parameter parameter : parameters) {
				values.parameters.put(parameter.id, parameter.value);
			}
		}

		for (Parameter parameter : parameters) {
			if (parameter.type == Parameter.Type.SEPARATOR || parameter.type == Parameter.Type.STRING) {
				// TODO: Consider whether we should attempt some strings here
				continue;
			}

			// The initial (default value); revert to this one after cycling,
			Object initial = values.parameters.get(parameter.id);

			if (parameter.type == Parameter.Type.ENUM) {
				List<Element> options = parameter.getOptions();
				for (Element element : options) {
					Option option = Option.get(element);
					String optionId = option.id;
					int optionMinSdk = option.minSdk;
					int optionMinBuildApi = option.minBuild;
					if (projectValues.minSdkLevel >= optionMinSdk && projectValues.getBuildApi() >= optionMinBuildApi) {
						values.parameters.put(parameter.id, optionId);
						projectValues.projectName = projectNameBase + "_" + parameter.id + "_" + optionId;
						checkProject(projectValues, templateValues);
					}
				}
			} else {
				assert parameter.type == Parameter.Type.BOOLEAN;
				if (parameter.id.equals("isLauncher") && projectValues.createActivity) {
					// Skipping this one: always true when launched from new
					// project
					continue;
				}
				boolean value = false;
				values.parameters.put(parameter.id, value);
				projectValues.projectName = projectNameBase + "_" + parameter.id + "_" + value;
				checkProject(projectValues, templateValues);

				value = true;
				values.parameters.put(parameter.id, value);
				projectValues.projectName = projectNameBase + "_" + parameter.id + "_" + value;
				checkProject(projectValues, templateValues);
			}

			values.parameters.put(parameter.id, initial);
		}
	}

	private final class OutputGrabber implements IProcessOutput {
		private final List<String> output = Lists.newArrayList();
		private final List<String> error = Lists.newArrayList();

		@Override
		public void out(@Nullable String line) {
			if (line != null) {
				output.add(line);
			}
		}

		@Override
		public void err(@Nullable String line) {
			if (line != null) {
				error.add(line);
			}
		}

		@NonNull
		private List<String> getOutput() {
			return output;
		}

		@NonNull
		private List<String> getError() {
			return error;
		}
	}

	private static class Option {
		private String id;
		private int minSdk;
		private int minBuild;

		public Option(String id, int minSdk, int minBuild) {
			this.id = id;
			this.minSdk = minSdk;
			this.minBuild = minBuild;
		}

		private static Option get(Element option) {
			String optionId = option.getAttribute(ATTR_ID);
			String minApiString = option.getAttribute(ATTR_MIN_API);
			int optionMinSdk = 1;
			if (minApiString != null && !minApiString.isEmpty()) {
				try {
					optionMinSdk = Integer.parseInt(minApiString);
				} catch (NumberFormatException nufe) {
					// Templates aren't allowed to contain codenames, should
					// always be an integer
					AndmoreAndroidPlugin.log(nufe, null);
					optionMinSdk = 1;
				}
			}
			String minBuildApiString = option.getAttribute(ATTR_MIN_BUILD_API);
			int optionMinBuildApi = 1;
			if (minBuildApiString != null && !minBuildApiString.isEmpty()) {
				try {
					optionMinBuildApi = Integer.parseInt(minBuildApiString);
				} catch (NumberFormatException nufe) {
					// Templates aren't allowed to contain codenames, should
					// always be an integer
					AndmoreAndroidPlugin.log(nufe, null);
					optionMinBuildApi = 1;
				}
			}

			return new Option(optionId, optionMinSdk, optionMinBuildApi);
		}
	}

	IProject project;
	
	private void checkProject(@NonNull NewProjectWizardState projectValues,
			@Nullable NewTemplateWizardState templateValues) throws Exception {
		NewTemplateWizardState values = projectValues.createActivity ? projectValues.activityValues : templateValues;
		if (values != null) { // if not, creating blank project
			// Validate that a template is only being used in a context it is
			// compatible with!
			IStatus status = values.getTemplateHandler().validateTemplate(projectValues.minSdkLevel,
					projectValues.getBuildApi());
			if (status != null && !status.isOK()) {
				fail(status.toString());
			}
		}

		assertNotNull(projectValues.projectName);
		projectValues.projectName = AdtUtils.getUniqueProjectName(projectValues.projectName, "");
		IPath workspace = Platform.getLocation();
		String projectLocation = workspace.append(projectValues.projectName).toOSString();
		projectValues.projectLocation = projectLocation;

		// Create project with the given parameter map
		project = createProject(projectValues);
		assertNotNull(project);

		if (templateValues != null) {
			templateValues.project = project;
			List<Change> changes = templateValues.computeChanges();
			if (!changes.isEmpty()) {
				try {
					CompositeChange composite = new CompositeChange("", changes.toArray(new Change[changes.size()]));
					composite.perform(new NullProgressMonitor());
				} catch (CoreException e) {
					fail(e.getLocalizedMessage());
				}
			}
		}

		// Project creation has some async hooks so don't attempt to build it
		// *right* away
		Job job = new Job("Validate project") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					ensureValidProject(this, project);
					return Status.OK_STATUS;
				} catch (Exception e) {
					fail(e.toString());
				}
				return null;
			}
		};
		job.schedule(1000);
		job.join();
		Object property = job.getProperty(ERROR_KEY);
		assertNull(property);
	}

	private IProject createProject(NewProjectWizardState values) throws InvocationTargetException {
		NewProjectWizard wizard = new NewProjectWizard();
		wizard.setValues(values);
		wizard.performFinish(new NullProgressMonitor());

		if (TemplateHandler.sMostRecentException != null) {
			fail(values.projectName + ": " + TemplateHandler.sMostRecentException.toString());
		}

		IProject project = wizard.getProject();
		assertNotNull(project);
		assertTrue(project.exists());
		System.out.println("Created project " + project + " : " + AdtUtils.getAbsolutePath(project));
		return project;
	}

	private void ensureValidProject(@NonNull Job job, @NonNull IProject project) throws Exception {
		System.out.println("Begin build error check");
		ensureNoBuildErrors(job, project);
		System.out.println("Finished build error check");

		System.out.println("Begin lint check");
		ensureNoLintWarnings(job, project);
		System.out.println("Finished lint check");

		sCount++;
	}

	private void ensureNoLintWarnings(final Job job, IProject project) {
		System.setProperty("com.android.tools.lint.bindir", AdtPrefs.getPrefs().getOsSdkFolder() + File.separator
				+ FD_TOOLS);

		LintDriver driver = new LintDriver(new BuiltinIssueRegistry(), new LintClient() {
			@Override
			public void report(@NonNull Context context, @NonNull Issue issue, @NonNull Severity severity,
					@Nullable Location location, @NonNull String message, @Nullable TextFormat format) {
				String s = "Found lint error: " + issue.getId() + ": " + message + " at " + location;
				job.setProperty(ERROR_KEY, s);
				fail(s);
			}

			// TODO remove this?
			public Configuration getConfiguration(@NonNull Project p) {
				return new DefaultConfiguration(this, p, null, new File("dummy.xml")) {
					@Override
					public boolean isEnabled(@NonNull Issue issue) {
						// Doesn't work: hangs in unit test context, something
						// about
						// loading native libs.
						if (issue.getCategory() == Category.ICONS) {
							return false;
						}

						if (issue == ManifestDetector.TARGET_NEWER) {
							// Don't complain about targetSdk < latest: we're
							// deliberately
							// testing that (to make sure templates compile etc
							// in compat
							// mode)
							return false;
						}

						if (issue == SecurityDetector.EXPORTED_SERVICE || issue == SecurityDetector.EXPORTED_PROVIDER
								|| issue == SecurityDetector.EXPORTED_RECEIVER) {
							// Don't complain about missing permissions when
							// exporting: the
							// unit test is deliberately turning on exported
							return false;
						}

						return true;
					}
				};
			}

			@Override
			@NonNull
			public String readFile(@NonNull File file) {
				try {
					return Files.toString(file, Charsets.UTF_8);
				} catch (IOException e) {
					fail(e.toString() + " for " + file.getPath());
					return "";
				}
			}

			@Override
			public void log(@NonNull Severity severity, @Nullable Throwable exception, @Nullable String format,
					@Nullable Object... args) {
				if (exception != null) {
					exception.printStackTrace();
				}
				if (format != null) {
					if (args != null) {
						System.err.println("Log: " + String.format(format, args));
					} else {
						System.err.println("Unexpected log message " + format);
					}
				}
			}

			@Override
			@Nullable
			public JavaParser getJavaParser(@Nullable Project project) {
				return new EclipseLintClient(null, null, null, false).getJavaParser(project);
			}

			@Override
			public XmlParser getXmlParser() {
				return new EclipseLintClient(null, null, null, false).getXmlParser();
			}
		});
		File projectDir = AdtUtils.getAbsolutePath(project).toFile();
		assertNotNull(projectDir);
		assertTrue(projectDir.getPath(), projectDir.isDirectory());
		driver.analyze(Collections.singletonList(projectDir), Scope.ALL);
	}

	// Wait for test build support.
	// This is copied from {@link SampleProjectTest}

	private void ensureNoBuildErrors(final Job job, final IProject project) throws Exception {
		File projectDir = AdtUtils.getAbsolutePath(project).toFile();

		// Checking the build in Eclipse doesn't work well, because of
		// asynchronous issues
		// (it looks like not all necessary changes are applied, and even adding
		// waits works
		// unpredictably.)
		//
		// So instead we do it via the command line.
		// First add ant support:
		// $ android update project -p .
		// Then we run ant and look at the exit code to make sure it worked.

		List<String> command = new ArrayList<String>();
		command.add(AndmoreAndroidPlugin.getOsSdkToolsFolder() + "android" + (CURRENT_PLATFORM == PLATFORM_WINDOWS ? ".bat" : ""));
		command.add("update");
		command.add("project");
		command.add("-p");
		command.add(projectDir.getPath());

		// launch the command line process
		Process process = Runtime.getRuntime().exec(command.toArray(new String[command.size()]));

		OutputGrabber processOutput = new OutputGrabber();
		int status = GrabProcessOutput.grabProcessOutput(process, Wait.WAIT_FOR_READERS, // we
																							// really
																							// want
																							// to
																							// make
																							// sure
																							// we
																							// get
																							// all
																							// the
																							// output!
				processOutput);
		if (status != 0) {
			fail(processOutput.getOutput().toString() + processOutput.getError().toString());
		}
		assertEquals(0, status);

		// Run ant
		String antCmd = "ant" + (CURRENT_PLATFORM == PLATFORM_WINDOWS ? ".bat" : "");
		String antTarget = "debug";
		process = Runtime.getRuntime().exec(antCmd + " " + antTarget, null, projectDir);
		processOutput = new OutputGrabber();
		status = GrabProcessOutput.grabProcessOutput(process, Wait.WAIT_FOR_READERS, // we
																						// really
																						// want
																						// to
																						// make
																						// sure
																						// we
																						// get
																						// all
																						// the
																						// output!
				processOutput);
		if (status != 0) {
			fail(processOutput.getOutput().toString() + processOutput.getError().toString());
		}
		assertEquals(0, status);
		System.out.println("Ant succeeded (code=" + status + ")");
	}
}
