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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.registry.EditorRegistry;
import org.eclipse.ui.internal.registry.FileEditorMapping;

/**
 * Associates Proguard editor to proguard.cfg file on studio start-up.
 */
@SuppressWarnings("restriction")
public class AssociateProguardEditor implements IStartup {
	// The plug-in ID
	public static final String EDITOR_ID = "net.certiv.proguarddt.editor.ProGuardDTEditor"; //$NON-NLS-1$

	@Override
	public void earlyStartup() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				try {
					if (!System.getProperty("java.version").startsWith("1.5")) {
						// code valid for Java 1.6 or higher
						EditorRegistry registry = (EditorRegistry) WorkbenchPlugin.getDefault().getEditorRegistry(); // cast
																														// to
																														// allow
																														// save
																														// to
																														// be
																														// called
						FileEditorMapping[] mappings = (FileEditorMapping[]) registry.getFileEditorMappings();
						List<FileEditorMapping> listMappings = new ArrayList<FileEditorMapping>();
						listMappings.addAll(Arrays.asList(mappings));
						listMappings.add(new FileEditorMapping("proguard", "cfg"));
						FileEditorMapping[] newMappings = listMappings.toArray(new FileEditorMapping[0]);
						registry.setFileEditorMappings(newMappings);

						registry.setDefaultEditor("proguard.cfg", EDITOR_ID);
						registry.saveAssociations();
					}
				} catch (Throwable t) {
					// do nothing - let association for proguard.cfg file to be
					// with text editor
				}
			}
		});
	}
}
