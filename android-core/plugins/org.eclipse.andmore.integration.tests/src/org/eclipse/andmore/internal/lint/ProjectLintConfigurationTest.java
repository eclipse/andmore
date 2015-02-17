/*
 * Copyright (C) 2011 The Android Open Source Project
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
package org.eclipse.andmore.internal.lint;

import static org.junit.Assert.*;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.checks.DuplicateIdDetector;
import com.android.tools.lint.checks.UnusedResourceDetector;
import com.android.tools.lint.client.api.Configuration;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.XmlParser;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Severity;

import org.eclipse.andmore.internal.editors.layout.refactoring.AdtProjectTest;
import org.eclipse.core.resources.IProject;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

@SuppressWarnings("javadoc")
public class ProjectLintConfigurationTest extends AdtProjectTest {

	@Test
	public void testBasic() {
		Configuration parent = null;
		LintClient client = new TestClient();

		File dir = getTargetDir();
		if (!dir.exists()) {
			boolean ok = dir.mkdirs();
			assertTrue(dir.getPath(), ok);
		}
		Project project = client.getProject(dir, dir);

		ProjectLintConfiguration config = new ProjectLintConfiguration(client, project, parent, false /* fatalOnly */);

		Issue usuallyEnabledIssue = DuplicateIdDetector.WITHIN_LAYOUT;
		Issue usuallyDisabledIssue = UnusedResourceDetector.ISSUE_IDS;

		assertTrue(config.isEnabled(usuallyEnabledIssue));
		assertFalse(config.isEnabled(usuallyDisabledIssue));

		config.setSeverity(usuallyEnabledIssue, Severity.IGNORE);
		config.setSeverity(usuallyDisabledIssue, Severity.ERROR);
		assertFalse(config.isEnabled(usuallyEnabledIssue));
		assertTrue(config.isEnabled(usuallyDisabledIssue));

		// Make a NEW config object to ensure the state is persisted properly,
		// not just
		// kept on the config object!
		config = new ProjectLintConfiguration(client, project, parent, false /* fatalOnly */);
		assertFalse(config.isEnabled(usuallyEnabledIssue));
		assertTrue(config.isEnabled(usuallyDisabledIssue));
	}

	@Test
	public void testInheritance() {
		Configuration parent = null;
		LintClient client = new TestClient();

		File dir = getTargetDir();
		assertTrue(dir.mkdirs());
		Project project = client.getProject(dir, dir);

		File otherDir = new File(dir, "otherConfig");
		assertTrue(otherDir.mkdir());
		Project otherProject = client.getProject(otherDir, otherDir);

		ProjectLintConfiguration otherConfig = new ProjectLintConfiguration(client, otherProject, parent, false);

		ProjectLintConfiguration config = new ProjectLintConfiguration(client, project, otherConfig, false);

		Issue usuallyEnabledIssue = DuplicateIdDetector.WITHIN_LAYOUT;
		Issue usuallyDisabledIssue = UnusedResourceDetector.ISSUE_IDS;

		assertTrue(config.isEnabled(usuallyEnabledIssue));
		assertFalse(config.isEnabled(usuallyDisabledIssue));

		otherConfig.setSeverity(usuallyEnabledIssue, Severity.IGNORE);
		otherConfig.setSeverity(usuallyDisabledIssue, Severity.ERROR);

		// Ensure inheritance works
		assertFalse(config.isEnabled(usuallyEnabledIssue));
		assertTrue(config.isEnabled(usuallyDisabledIssue));

		// Revert
		otherConfig.setSeverity(usuallyEnabledIssue, Severity.ERROR);
		otherConfig.setSeverity(usuallyDisabledIssue, Severity.IGNORE);
		assertTrue(config.isEnabled(usuallyEnabledIssue));
		assertFalse(config.isEnabled(usuallyDisabledIssue));

		// Now override in child
		config.setSeverity(usuallyEnabledIssue, Severity.ERROR);
		config.setSeverity(usuallyDisabledIssue, Severity.IGNORE);
		assertTrue(config.isEnabled(usuallyEnabledIssue));
		assertFalse(config.isEnabled(usuallyDisabledIssue));

		// Now change in parent: no change in child
		otherConfig.setSeverity(usuallyEnabledIssue, Severity.IGNORE);
		otherConfig.setSeverity(usuallyDisabledIssue, Severity.ERROR);
		assertTrue(config.isEnabled(usuallyEnabledIssue));
		assertFalse(config.isEnabled(usuallyDisabledIssue));
		assertFalse(otherConfig.isEnabled(usuallyEnabledIssue));
		assertTrue(otherConfig.isEnabled(usuallyDisabledIssue));

		// Clear override in child
		config.setSeverity(usuallyEnabledIssue, null);
		config.setSeverity(usuallyDisabledIssue, null);
		assertFalse(config.isEnabled(usuallyEnabledIssue));
		assertTrue(config.isEnabled(usuallyDisabledIssue));
	}

	@Test
	public void testBulkEditing() {
		Configuration parent = null;
		LintClient client = new TestClient();

		File dir = getTargetDir();
		assertTrue(dir.mkdirs());
		Project project = client.getProject(dir, dir);

		ProjectLintConfiguration config = new ProjectLintConfiguration(client, project, parent, false /* fatalOnly */);

		Issue usuallyEnabledIssue = DuplicateIdDetector.WITHIN_LAYOUT;
		Issue usuallyDisabledIssue = UnusedResourceDetector.ISSUE_IDS;

		assertTrue(config.isEnabled(usuallyEnabledIssue));
		assertFalse(config.isEnabled(usuallyDisabledIssue));

		config.setSeverity(usuallyEnabledIssue, Severity.IGNORE);
		assertFalse(config.isEnabled(usuallyEnabledIssue));
		assertFalse(config.isEnabled(usuallyDisabledIssue));

		File configFile = new File(dir, "lint.xml");
		assertTrue(configFile.getPath(), configFile.exists());
		long lastModified = configFile.lastModified();

		// We need to make sure that the timestamp of the file is a couple of
		// seconds
		// after the last update or we can't tell whether the file was updated
		// or not

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			System.err.println("Sleep interrupted, test may not work.");
		}
		config.startBulkEditing();
		assertFalse(lastModified < configFile.lastModified());
		assertEquals(lastModified, configFile.lastModified());
		config.setSeverity(usuallyDisabledIssue, Severity.ERROR);
		config.finishBulkEditing();
		assertTrue(lastModified < configFile.lastModified());

		assertTrue(config.isEnabled(usuallyDisabledIssue));
	}

	@Test
	public void testPersistence() {
		// Ensure that we use the same configuration object repeatedly for a
		// single project, such that we don't recompute and parse XML for each
		// and
		// every lint run!
		IProject project = getProject();
		TestClient client = new TestClient();
		ProjectLintConfiguration config1 = ProjectLintConfiguration.get(client, project, false);
		ProjectLintConfiguration config2 = ProjectLintConfiguration.get(client, project, false);
		assertSame(config1, config2);
	}

	@Override
	protected File getTargetDir() {
		File targetDir = new File(getTempDir(), getClass().getSimpleName() + "_" + testName.getMethodName());
		addCleanupDir(targetDir);
		return targetDir;
	}

	private static class TestClient extends LintClient {
		@Override
		public void report(@NonNull Context context, @NonNull Issue issue, @NonNull Severity severity,
				@Nullable Location location, @NonNull String message, @Nullable Object data) {
		}

		@Override
		public void log(@NonNull Severity severity, @Nullable Throwable exception, @Nullable String format,
				@Nullable Object... args) {
		}

		@Override
		public XmlParser getXmlParser() {
			return null;
		}

		@Override
		public @NonNull String readFile(@NonNull File file) {
			return null;
		}

		@Override
		public JavaParser getJavaParser(@Nullable Project project) {
			return null;
		}
	}
}
