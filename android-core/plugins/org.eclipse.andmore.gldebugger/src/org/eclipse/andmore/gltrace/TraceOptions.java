/*
 * Copyright (C) 2011 The Android Open Source Project
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

package org.eclipse.andmore.gltrace;

public class TraceOptions {
	/** Device on which the application should be run. */
	public final String device;

	/** Application to trace. */
	public final String appToTrace;

	/** Activity to trace. */
	public final String activityToTrace;

	public final boolean isActivityNameFullyQualified;

	/** Path where the trace file should be saved. */
	public final String traceDestination;

	/** Flag indicating whether Framebuffer should be captured on eglSwap() */
	public final boolean collectFbOnEglSwap;

	/** Flag indicating whether Framebuffer should be captured on glDraw*() */
	public final boolean collectFbOnGlDraw;

	/** Flag indicating whether texture data should be captured on glTexImage*() */
	public final boolean collectTextureData;

	public TraceOptions(String device, String appPackage, String activity, boolean isActivityNameFullyQualified,
			String destinationPath, boolean collectFbOnEglSwap, boolean collectFbOnGlDraw, boolean collectTextureData) {
		this.device = device;
		this.appToTrace = appPackage;
		this.activityToTrace = activity;
		this.isActivityNameFullyQualified = isActivityNameFullyQualified;
		this.traceDestination = destinationPath;
		this.collectFbOnEglSwap = collectFbOnEglSwap;
		this.collectFbOnGlDraw = collectFbOnGlDraw;
		this.collectTextureData = collectTextureData;
	}
}
