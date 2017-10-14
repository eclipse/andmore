/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.eclipse.andmore.integration.tests;

import static org.junit.Assert.*;

import java.io.File;

import com.android.ide.common.sdk.LoadStatus;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.AndmoreAndroidPlugin.CheckSdkErrorHandler;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.sdk.AndroidTargetParser;
import org.eclipse.andmore.internal.sdk.Sdk;

import com.android.sdklib.IAndroidTarget;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.After;
import org.junit.BeforeClass;

/**
 * A test case which uses the SDK loaded by the ADT plugin.
 */
public abstract class SdkLoadingTestCase extends SdkTestCase {

	private Sdk mSdk;
		

	/**
	 * Retrieve the {@link Sdk} under test.
	 */
	protected Sdk getSdk() {
		System.out.println("getSdk");
		if (mSdk == null) {
			mSdk = loadSdk();
			assertNotNull(mSdk);
			validateSdk(mSdk);
		}
		return mSdk;
	}

	/**
	 * Gets the current SDK from ADT, waiting if necessary.
	 */
	private Sdk loadSdk() {
		AndmoreAndroidPlugin adt = AndmoreAndroidPlugin.getDefault();

		// We'll never get an AdtPlugin object when running this with the
		// non-Eclipse jUnit test runner.
		if (adt == null) {
			return null;
		}

		// We'll never break out of the SDK load-wait-loop if the AdtPlugin
		// doesn't
		// actually have a valid SDK location because it won't have started an
		// async load:
		//String sdkLocation = AdtPrefs.getPrefs().getOsSdkFolder();
		String sdkLocation = System.getenv("ANDROID_HOME");
		if (sdkLocation == null || sdkLocation.length() == 0) {
			sdkLocation = System.getenv("ADT_TEST_SDK_PATH");
		}
		assertTrue("No valid SDK installation is set; for tests you typically need to set the"
				+ " environment variable ADT_TEST_SDK_PATH to point to an SDK folder", sdkLocation != null
				&& sdkLocation.length() > 0);
		AdtPrefs.getPrefs().setSdkLocation(new File(sdkLocation));

		Object sdkLock = Sdk.getLock();
		LoadStatus loadStatus = LoadStatus.LOADING;
		// wait for ADT to load the SDK on a separate thread
		// loop max of 600 times * 200 ms = 2 minutes
		final int maxWait = 600;
		for (int i = 0; i < maxWait && loadStatus == LoadStatus.LOADING; i++) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// ignore
			}
			synchronized (sdkLock) {
				loadStatus = adt.getSdkLoadStatus();
			}
		}
		Sdk sdk = null;
		synchronized (sdkLock) {
			assertEquals(LoadStatus.LOADED, loadStatus);
			sdk = Sdk.getCurrent();
		}
		
		if (sdk.getTargets().size() == 0) {
			System.out.println("Did not find any valid targets. Reloading SDK from " + sdkLocation);
			sdk = Sdk.loadSdk(sdkLocation);
		}
		assertNotNull(sdk);
		return sdk;	}

	protected boolean validateSdk(IAndroidTarget target) {
		return true;
	}

	/**
	 * Checks that the provided sdk contains one or more valid targets.
	 * 
	 * @param sdk
	 *            the {@link Sdk} to validate.
	 */
	@SuppressWarnings("unused")
	private void validateSdk(Sdk sdk) {
		assertTrue("sdk has no targets", sdk.getTargets().size() > 0);
		for (IAndroidTarget target : sdk.getTargets()) {
			if (!validateSdk(target)) {
				continue;
			}
			if (false) { // This takes forEVER
				IStatus status = new AndroidTargetParser(target).run(new NullProgressMonitor());
				if (status.getCode() != IStatus.OK) {
					fail("Failed to parse targets data");
				}
			}
		}
	}
}
