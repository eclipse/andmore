/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.ddms.systrace;

import com.android.ddmlib.CollectingOutputReceiver;
import com.android.ddmlib.IDevice;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystraceVersionDetector implements IRunnableWithProgress {
	public static final int SYSTRACE_V1 = 1;
	public static final int SYSTRACE_V2 = 2;

	private final IDevice mDevice;
	private List<SystraceTag> mTags;

	public SystraceVersionDetector(IDevice device) {
		mDevice = device;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		monitor.beginTask("Checking systrace version on device..", IProgressMonitor.UNKNOWN);

		CountDownLatch setTagLatch = new CountDownLatch(1);
		CollectingOutputReceiver receiver = new CollectingOutputReceiver(setTagLatch);
		try {
			String cmd = "atrace --list_categories";
			mDevice.executeShellCommand(cmd, receiver);
			setTagLatch.await(5, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}

		String shellOutput = receiver.getOutput();
		mTags = parseSupportedTags(shellOutput);

		monitor.done();
	}

	public int getVersion() {
		if (mTags == null) {
			return SYSTRACE_V1;
		} else {
			return SYSTRACE_V2;
		}
	}

	public List<SystraceTag> getTags() {
		return mTags;
	}

	private List<SystraceTag> parseSupportedTags(String listCategoriesOutput) {
		if (listCategoriesOutput == null) {
			return null;
		}

		if (listCategoriesOutput.contains("unknown option")) {
			return null;
		}

		String[] categories = listCategoriesOutput.split("\n");
		List<SystraceTag> tags = new ArrayList<SystraceTag>(categories.length);

		Pattern p = Pattern.compile("([^-]+) - (.*)"); //$NON-NLS-1$
		for (String category : categories) {
			Matcher m = p.matcher(category);
			if (m.find()) {
				tags.add(new SystraceTag(m.group(1).trim(), m.group(2).trim()));
			}
		}

		return tags;
	}
}
