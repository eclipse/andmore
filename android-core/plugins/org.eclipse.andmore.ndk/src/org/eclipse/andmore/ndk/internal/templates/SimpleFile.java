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

package org.eclipse.andmore.ndk.internal.templates;

import org.eclipse.andmore.ndk.internal.Activator;
import org.eclipse.andmore.ndk.internal.Messages;
import org.eclipse.cdt.core.templateengine.TemplateCore;
import org.eclipse.cdt.core.templateengine.process.ProcessArgument;
import org.eclipse.cdt.core.templateengine.process.ProcessFailureException;
import org.eclipse.cdt.core.templateengine.process.ProcessRunner;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SimpleFile extends ProcessRunner {

	private static final class FileOp {
		public String source;
		public String destination;
	}

	@Override
	public void process(TemplateCore template, ProcessArgument[] args, String processId, IProgressMonitor monitor)
			throws ProcessFailureException {

		// Fetch the args
		String projectName = null;
		List<FileOp> fileOps = new ArrayList<FileOp>();

		for (ProcessArgument arg : args) {
			if (arg.getName().equals("projectName")) //$NON-NLS-1$
				projectName = arg.getSimpleValue();
			else if (arg.getName().equals("files")) { //$NON-NLS-1$
				ProcessArgument[][] files = arg.getComplexArrayValue();
				for (ProcessArgument[] file : files) {
					FileOp op = new FileOp();
					for (ProcessArgument fileArg : file) {
						if (fileArg.getName().equals("source")) //$NON-NLS-1$
							op.source = fileArg.getSimpleValue();
						else if (fileArg.getName().equals("destination")) //$NON-NLS-1$
							op.destination = fileArg.getSimpleValue();
					}
					if (op.source == null || op.destination == null)
						throw new ProcessFailureException(Messages.SimpleFile_Bad_file_operation);
					fileOps.add(op);
				}
			}
		}

		if (projectName == null)
			throw new ProcessFailureException(Messages.SimpleFile_No_project_name);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (!project.exists())
			throw new ProcessFailureException(Messages.SimpleFile_Project_does_not_exist);

		// Find bundle to find source files
		Bundle bundle = Activator.getBundle(template.getTemplateInfo().getPluginId());
		if (bundle == null)
			throw new ProcessFailureException(Messages.SimpleFile_Bundle_not_found);

		try {
			for (FileOp op : fileOps) {
				IFile destFile = project.getFile(new Path(op.destination));
				if (destFile.exists())
					// don't overwrite files if they exist already
					continue;

				// Make sure parent folders are created
				mkDirs(project, destFile.getParent(), monitor);

				URL sourceURL = FileLocator.find(bundle, new Path(op.source), null);
				if (sourceURL == null)
					throw new ProcessFailureException(Messages.SimpleFile_Could_not_fine_source + op.source);

				TemplatedInputStream in = new TemplatedInputStream(sourceURL.openStream(), template.getValueStore());
				destFile.create(in, true, monitor);
				in.close();
			}
		} catch (IOException e) {
			throw new ProcessFailureException(e);
		} catch (CoreException e) {
			throw new ProcessFailureException(e);
		}

	}

	private void mkDirs(IProject project, IContainer container, IProgressMonitor monitor) throws CoreException {
		if (container.exists())
			return;
		mkDirs(project, container.getParent(), monitor);
		((IFolder) container).create(true, true, monitor);
	}

}
