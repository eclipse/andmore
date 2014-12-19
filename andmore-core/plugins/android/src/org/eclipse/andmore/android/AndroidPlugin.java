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

package org.eclipse.andmore.android;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class AndroidPlugin extends AbstractUIPlugin {
	// Listening to this job, instead of loading sdk job, which seems to don't
	// exist anymore.
	private static final String ANDROID_SDK_CONTENT_LOADER_JOB = "Android SDK Content Loader";

	private final LinkedList<Runnable> listeners = new LinkedList<Runnable>();

	protected boolean sdkLoaded = false;

	/**
	 * The plug-in ID
	 */
	public static final String PLUGIN_ID = "org.eclipse.andmore.android";

	/**
	 * Studio for Android Perspective ID
	 */
	public static final String PERSPECTIVE_ID = "org.eclipse.andmore.android.perspective";

	/**
	 * Nature of Android projects
	 */
	public static final String Android_Nature = IAndroidConstants.ANDROID_NATURE;

	/**
	 * The Motorola Android Branding icon
	 */
	public static final String ANDROID_MOTOROLA_BRAND_ICON_PATH = "icons/obj16/plate16.png";

	public static final String SHALL_UNEMBED_EMULATORS_PREF_KEY = "shallUnembedEmulators";

	// The shared instance
	private static AndroidPlugin plugin;

	public static final String NDK_LOCATION_PREFERENCE = PLUGIN_ID + ".ndkpath";

	public static final String CYGWIN_LOCATION_PREFERENCE = PLUGIN_ID + ".cigwinpath";

	public static final String WARN_ABOUT_HPROF_PREFERENCE = PLUGIN_ID + ".warnAboutHprofSaveAction";

	public static final String GCC_VERSION_PROPERTY = "gccversion";

	public static final String PLATFORM_PROPERTY = "platform";

	public static final String SRC_LOCATION_PROPERTY = "srclocation";

	public static final String OBJ_LOCATION_PROPERTY = "objlocation";

	public static final String LIB_LOCATION_PROPERTY = "liblocation";

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		StudioLogger.debug(AndroidPlugin.class, "Starting MOTODEV Android Plugin...");

		super.start(context);
		plugin = this;

		Thread t = new Thread("DDMS Setup") {
			@Override
			public void run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

				/*
				 * Linux problem with e4. Check if workbench window is done
				 * before calling DDMSFacade#setup(). If the workbench window is
				 * not done yet, then add a listener to the workbench window
				 * that will be responsible to call DDMSFacade#setup(). It seems
				 * that e4 changed its behavior when loading plugins, which was
				 * causing deadlocks on linux startup. This workaround works in
				 * any OS.
				 */
				if (window != null) {
					StudioLogger.debug(AndroidPlugin.class, "Starting DDMS facade WITHOUT using listener...");

					DDMSFacade.setup();

					StudioLogger.debug(AndroidPlugin.class, "DDMS facade started WITHOUT using listener.");
				} else {
					workbench.addWindowListener(new IWindowListener() {

						@Override
						public void windowActivated(IWorkbenchWindow window) {
							// do nothing
						}

						@Override
						public void windowDeactivated(IWorkbenchWindow window) {
							// do nothing
						}

						@Override
						public void windowClosed(IWorkbenchWindow window) {
							// do nothing
						}

						@Override
						public void windowOpened(IWorkbenchWindow window) {
							StudioLogger.debug(AndroidPlugin.class, "Starting DDMS facade using listener...");

							DDMSFacade.setup();

							StudioLogger.debug(AndroidPlugin.class, "DDMS facade started using listener.");
						}
					});
				}
			};
		};

		getPreferenceStore().setDefault(AndroidPlugin.SHALL_UNEMBED_EMULATORS_PREF_KEY, true);

		// every time the Android SDK Job finishes its execution
		IJobManager manager = Job.getJobManager();
		manager.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				Job job = event.getJob();
				if (job != null) {
					String jobName = job.getName();
					if (jobName != null) {
						if (jobName.equals(ANDROID_SDK_CONTENT_LOADER_JOB)) {

							sdkLoaded = true;

							/*
							 * Workaround The Listener should be copied in this
							 * set, to avoid exceptions in the loop. The
							 * exception occurs when a listener remove itself.
							 */
							StudioLogger.debug(AndroidPlugin.this, "Notify SDK loader listeners");
							Set<Runnable> setListeners = new HashSet<Runnable>(listeners);
							for (Runnable listener : setListeners) {
								try {
									listener.run();
								} catch (Throwable e) {
									// Log error of this listener and keep
									// handling the next listener...
									StudioLogger.error(AndroidPlugin.class,
											"Error while handling SDK loader procedure.", e);
								}
							}
						}
					}
				}
			}
		});
		t.start();

		StudioLogger.debug(AndroidPlugin.class, "MOTODEV Android Plugin started.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Add a Listener that will be executed after any SDK loader action.
	 * 
	 * @param listener
	 */
	public void addSDKLoaderListener(Runnable listener) {
		listeners.addLast(listener);

		if (sdkLoaded) {
			listener.run();
		}
	}

	/**
	 * Remove the given Listener.
	 * 
	 * @param listener
	 */
	public void removeSDKLoaderListener(Runnable listener) {
		if (listeners.contains(listener)) {
			listeners.remove(listener);
		}
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static AndroidPlugin getDefault() {
		return plugin;
	}

	/**
	 * Creates and returns a new image descriptor for an image file in this
	 * plug-in.
	 * 
	 * @param path
	 *            the relative path of the image file, relative to the root of
	 *            the plug-in; the path must be legal
	 * @return an image descriptor, or null if no image could be found
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
