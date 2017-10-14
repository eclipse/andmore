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
package org.eclipse.andmore.internal.editors.manifest;

import static com.android.resources.ScreenSize.LARGE;
import static com.android.resources.ScreenSize.NORMAL;
import static com.android.resources.ScreenSize.XLARGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.internal.editors.layout.refactoring.AdtProjectTest;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo.ActivityAttributes;
import org.eclipse.andmore.internal.resources.ResourceHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

import com.android.annotations.NonNull;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;

@SuppressWarnings("javadoc")
public class ManifestInfoTest extends AdtProjectTest {

	@Test
	public void testGetActivityThemes1() throws Exception {
		ManifestInfo info = getManifestInfo("<manifest xmlns:android='http://schemas.android.com/apk/res/android'\n"
				+ "    package='com.android.unittest'>\n"
				+ "    <uses-sdk android:minSdkVersion='3' android:targetSdkVersion='4'/>\n" + "</manifest>\n");
		Map<String, ActivityAttributes> map = info.getActivityAttributesMap();
		assertEquals(map.toString(), 0, map.size());
		assertEquals("com.android.unittest", info.getPackage());
		assertEquals("Theme", ResourceHelper.styleToTheme(info.getDefaultTheme(null, NORMAL)));
		assertEquals("@android:style/Theme", info.getDefaultTheme(null, null));
		assertEquals("Theme", ResourceHelper.styleToTheme(info.getDefaultTheme(null, XLARGE)));
	}

	@Test
	public void testGetActivityThemes2() throws Exception {
		ManifestInfo info = getManifestInfo("<manifest xmlns:android='http://schemas.android.com/apk/res/android'\n"
				+ "    package='com.android.unittest'>\n"
				+ "    <uses-sdk android:minSdkVersion='3' android:targetSdkVersion='11'/>\n" + "</manifest>\n");
		Map<String, ActivityAttributes> map = info.getActivityAttributesMap();
		assertEquals(map.toString(), 0, map.size());
		assertEquals("com.android.unittest", info.getPackage());
		assertEquals("Theme.Holo", ResourceHelper.styleToTheme(info.getDefaultTheme(null, XLARGE)));
		assertEquals("Theme", ResourceHelper.styleToTheme(info.getDefaultTheme(null, LARGE)));
	}

	@Test
	public void testGetActivityThemes3() throws Exception {
		ManifestInfo info = getManifestInfo("<manifest xmlns:android='http://schemas.android.com/apk/res/android'\n"
				+ "    package='com.android.unittest'>\n" + "    <uses-sdk android:minSdkVersion='11'/>\n"
				+ "</manifest>\n");
		Map<String, ActivityAttributes> map = info.getActivityAttributesMap();
		assertEquals(map.toString(), 0, map.size());
		assertEquals("com.android.unittest", info.getPackage());
		assertEquals("Theme.Holo", ResourceHelper.styleToTheme(info.getDefaultTheme(null, XLARGE)));
		assertEquals("Theme", ResourceHelper.styleToTheme(info.getDefaultTheme(null, NORMAL)));
	}

	@Test
	public void testGetActivityThemes4() throws Exception {
		ManifestInfo info = getManifestInfo("<manifest xmlns:android='http://schemas.android.com/apk/res/android'\n"
				+ "    package='com.android.unittest'>\n" + "    <application\n"
				+ "        android:label='@string/app_name'\n"
				+ "        android:name='.app.TestApp' android:icon='@drawable/app_icon'>\n" + "\n"
				+ "        <activity\n" + "            android:name='.prefs.PrefsActivity'\n"
				+ "            android:label='@string/prefs_title' />\n" + "\n" + "        <activity\n"
				+ "            android:name='.app.IntroActivity'\n"
				+ "            android:label='@string/intro_title'\n"
				+ "            android:theme='@android:style/Theme.Dialog' />\n" + "    </application>\n"
				+ "    <uses-sdk android:minSdkVersion='3' android:targetSdkVersion='4'/>\n" + "</manifest>\n" + "");
		assertEquals("com.android.unittest", info.getPackage());
		assertEquals("Theme", ResourceHelper.styleToTheme(info.getDefaultTheme(null, XLARGE)));

		Map<String, ActivityAttributes> map = info.getActivityAttributesMap();
		assertEquals(map.toString(), 2, map.size());
		assertNull(map.get("com.android.unittest.prefs.PrefsActivity").getTheme());
		assertEquals("@android:style/Theme.Dialog", map.get("com.android.unittest.app.IntroActivity").getTheme());
	}

	@Test
	public void testGetActivityThemes5() throws Exception {
		ManifestInfo info = getManifestInfo("<manifest xmlns:android='http://schemas.android.com/apk/res/android'\n"
				+ "    package='com.android.unittest'>\n" + "    <application\n"
				+ "        android:label='@string/app_name'\n" + "        android:theme='@style/NoBackground'\n"
				+ "        android:name='.app.TestApp' android:icon='@drawable/app_icon'>\n" + "\n"
				+ "        <activity\n" + "            android:name='.prefs.PrefsActivity'\n"
				+ "            android:label='@string/prefs_title' />\n" + "\n" + "        <activity\n"
				+ "            android:name='.app.IntroActivity'\n"
				+ "            android:label='@string/intro_title'\n"
				+ "            android:theme='@android:style/Theme.Dialog' />\n" + "    </application>\n"
				+ "    <uses-sdk android:minSdkVersion='3' android:targetSdkVersion='4'/>\n" + "</manifest>\n" + "");

		assertEquals("@style/NoBackground", info.getDefaultTheme(null, XLARGE));
		assertEquals("@style/NoBackground", info.getDefaultTheme(null, NORMAL));
		assertEquals("NoBackground", ResourceHelper.styleToTheme(info.getDefaultTheme(null, NORMAL)));

		Map<String, ActivityAttributes> map = info.getActivityAttributesMap();
		assertEquals(map.toString(), 2, map.size());
		assertNull(map.get("com.android.unittest.prefs.PrefsActivity").getTheme());
		assertEquals("@android:style/Theme.Dialog", map.get("com.android.unittest.app.IntroActivity").getTheme());
	}

	@Test
	public void testGetActivityThemes6() throws Exception {
		// Ensures that when the *rendering* target is less than version 11, we
		// don't
		// use Holo even though the manifest SDK version calls for it.
		ManifestInfo info = getManifestInfo("<manifest xmlns:android='http://schemas.android.com/apk/res/android'\n"
				+ "    package='com.android.unittest'>\n"
				+ "    <uses-sdk android:minSdkVersion='3' android:targetSdkVersion='11'/>\n" + "</manifest>\n");
		Map<String, ActivityAttributes> map = info.getActivityAttributesMap();
		assertEquals(map.toString(), 0, map.size());
		assertEquals("com.android.unittest", info.getPackage());
		assertEquals("Theme.Holo", ResourceHelper.styleToTheme(info.getDefaultTheme(null, XLARGE)));

		// Here's the check
		IAndroidTarget olderVersion = new TestAndroidTarget(4);
		assertEquals("Theme", ResourceHelper.styleToTheme(info.getDefaultTheme(olderVersion, XLARGE)));

	}

	@Test
	public void testGetApplicationLabelAndIcon() throws Exception {
		ManifestInfo info = getManifestInfo("<manifest xmlns:android='http://schemas.android.com/apk/res/android'\n"
				+ "    package='com.android.unittest'>\n" + "    <application android:icon=\"@drawable/icon\"\n"
				+ "                 android:label=\"@string/app_name\">\n" + "    </application>\n" + ""
				+ "</manifest>\n");
		Map<String, ActivityAttributes> map = info.getActivityAttributesMap();
		assertEquals(map.toString(), 0, map.size());
		assertEquals("com.android.unittest", info.getPackage());

		assertEquals("Theme", ResourceHelper.styleToTheme(info.getDefaultTheme(null, NORMAL)));
		assertEquals("@drawable/icon", info.getApplicationIcon());
		assertEquals("@string/app_name", info.getApplicationLabel());
	}

	@Test
	public void testGetApplicationNoLabelOrIcon() throws Exception {
		ManifestInfo info = getManifestInfo("<manifest xmlns:android='http://schemas.android.com/apk/res/android'\n"
				+ "    package='com.android.unittest'>\n" + "    <application>\n" + "    </application>\n" + ""
				+ "</manifest>\n");
		Map<String, ActivityAttributes> map = info.getActivityAttributesMap();
		assertEquals(map.toString(), 0, map.size());
		assertEquals("com.android.unittest", info.getPackage());

		assertEquals("Theme", ResourceHelper.styleToTheme(info.getDefaultTheme(null, NORMAL)));
		assertNull(info.getApplicationIcon());
		assertNull(info.getApplicationLabel());
	}

	private ManifestInfo getManifestInfo(String manifestContents) throws Exception {
		InputStream bstream = new ByteArrayInputStream(manifestContents.getBytes("UTF-8")); //$NON-NLS-1$

		IFile file = getProject().getFile("AndroidManifest.xml");
		if (file.exists()) {
			file.setContents(bstream, IResource.FORCE, new NullProgressMonitor());
		} else {
			file.create(bstream, false /* force */, new NullProgressMonitor());
		}
		ManifestInfo info = ManifestInfo.get(getProject());
		info.clear();
		return info;
	}

	@Test
	public void testGetMinSdkVersionName() throws Exception {
		ManifestInfo info;

		info = getManifestInfo("<manifest xmlns:android='http://schemas.android.com/apk/res/android'\n"
				+ "    package='com.android.unittest'>\n"
				+ "    <uses-sdk android:minSdkVersion='3' android:targetSdkVersion='4'/>\n" + "</manifest>\n");
		assertEquals(3, info.getMinSdkVersion());
		assertEquals("3", info.getMinSdkName());
		assertEquals(4, info.getTargetSdkVersion());
		assertNull(info.getMinSdkCodeName());

		info = getManifestInfo("<manifest xmlns:android='http://schemas.android.com/apk/res/android'\n"
				+ "    package='com.android.unittest'>\n" + "    <uses-sdk android:targetSdkVersion='4'/>\n"
				+ "</manifest>\n");
		assertEquals("1", info.getMinSdkName());
		assertEquals(1, info.getMinSdkVersion());
		assertEquals(4, info.getTargetSdkVersion());
		assertNull(info.getMinSdkCodeName());

		info = getManifestInfo("<manifest xmlns:android='http://schemas.android.com/apk/res/android'\n"
				+ "    package='com.android.unittest'>\n" + "    <uses-sdk android:minSdkVersion='JellyBean' />\n"
				+ "</manifest>\n");
		assertEquals("JellyBean", info.getMinSdkName());
		assertEquals("JellyBean", info.getMinSdkCodeName());
	}

	private static class TestAndroidTarget implements IAndroidTarget {
		private final int mApiLevel;

		public TestAndroidTarget(int apiLevel) {
			mApiLevel = apiLevel;
		}

		@Override
		public boolean canRunOn(IAndroidTarget target) {
			return false;
		}

		@Override
		public String getClasspathName() {
			return null;
		}

		@Override
		public File getDefaultSkin() {
			return null;
		}

		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public String getFullName() {
			return null;
		}

		@Override
		public String getLocation() {
			return null;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public List<OptionalLibrary> getOptionalLibraries() {
			return null;
		}

		@Override
		public IAndroidTarget getParent() {
			return null;
		}

		@Override
		public String getPath(int pathId) {
			return null;
		}

		@Override
		public File getFile(int pathId) {
			return null;
		}

		@Override
		public String[] getPlatformLibraries() {
			return null;
		}

		@Override
		public Map<String, String> getProperties() {
			return null;
		}

		@Override
		public String getProperty(String name) {
			return null;
		}

		@Override
		public int getRevision() {
			return 0;
		}

		@Override
		public File[] getSkins() {
			return null;
		}

		@Override
		public String getVendor() {
			return null;
		}

		@Override
		public AndroidVersion getVersion() {
			return new AndroidVersion(mApiLevel, null);
		}

		@Override
		public String getVersionName() {
			return null;
		}

		@Override
		public String hashString() {
			return null;
		}

		@Override
		public boolean isPlatform() {
			return false;
		}

		@Override
		public int compareTo(IAndroidTarget o) {
			return 0;
		}

		@Override
		public boolean hasRenderingLibrary() {
			return false;
		}

		@Override
		public String getShortClasspathName() {
			return null;
		}

		@Override
		@NonNull
		public List<String> getBootClasspath() {
			return new ArrayList<String>();
		}

		@Override
		public BuildToolInfo getBuildToolInfo() {
			return null;
		}

		@Override
		public List<OptionalLibrary> getAdditionalLibraries() {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
