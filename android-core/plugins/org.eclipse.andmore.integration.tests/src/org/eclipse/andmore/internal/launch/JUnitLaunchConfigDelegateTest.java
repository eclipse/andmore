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

package org.eclipse.andmore.internal.launch;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.andmore.internal.launch.JUnitLaunchConfigDelegate;
import org.junit.Test;


public class JUnitLaunchConfigDelegateTest {

	@Test
	public void testAbleToFetchJunitJar() throws IOException {
		assertTrue(JUnitLaunchConfigDelegate.getJunitJarLocation().endsWith("junit.jar"));
	}

	@Test
	public void testFixBootpathExtWithAndroidJar() {
		String[][] testArray = { null, {}, { "android.jar" }, null, { "some_other_jar.jar" }, };

		String[][] expectedArray = { null, {}, null, null, { "some_other_jar.jar" }, };

		assertEqualsArrays(expectedArray, JUnitLaunchConfigDelegate.fixBootpathExt(testArray));
	}

	@Test
	public void testFixBootpathExtWithNoAndroidJar() {
		String[][] testArray = { null, { "somejar.jar" }, null, };

		String[][] expectedArray = { null, { "somejar.jar" }, null, };

		assertEqualsArrays(expectedArray, JUnitLaunchConfigDelegate.fixBootpathExt(testArray));
	}

	@Test
	public void testFixClasspathWithJunitJar() throws IOException {
		String[] testArray = { JUnitLaunchConfigDelegate.getJunitJarLocation(), };

		String[] expectedArray = { JUnitLaunchConfigDelegate.getJunitJarLocation(), };

		assertEqualsArrays(expectedArray, JUnitLaunchConfigDelegate.fixClasspath(testArray, "test"));
	}

	@Test
	public void testFixClasspathWithoutJunitJar() throws IOException {
		String[] testArray = { "random.jar", };

		String[] expectedArray = { "random.jar", JUnitLaunchConfigDelegate.getJunitJarLocation(), };

		assertEqualsArrays(expectedArray, JUnitLaunchConfigDelegate.fixClasspath(testArray, "test"));
	}

	@Test
	public void testFixClasspathWithNoJars() throws IOException {
		String[] testArray = {};

		String[] expectedArray = { JUnitLaunchConfigDelegate.getJunitJarLocation(), };

		assertEqualsArrays(expectedArray, JUnitLaunchConfigDelegate.fixClasspath(testArray, "test"));
	}

	private void assertEqualsArrays(String[][] a1, String[][] a2) {
		assertTrue(Arrays.deepEquals(a1, a2));
	}

	private void assertEqualsArrays(String[] a1, String[] a2) {
		assertTrue(Arrays.deepEquals(a1, a2));
	}
}
