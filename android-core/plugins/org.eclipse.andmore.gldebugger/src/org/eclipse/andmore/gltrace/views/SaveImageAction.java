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

package org.eclipse.andmore.gltrace.views;

import org.eclipse.andmore.gltrace.widgets.ImageCanvas;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import java.io.File;

public class SaveImageAction extends Action {
	private static String sLastUsedPath;

	private ImageCanvas mImageCanvas;

	public SaveImageAction(ImageCanvas canvas) {
		super("Save Image", PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ETOOL_SAVEAS_EDIT));
		setToolTipText("Save Image");
		mImageCanvas = canvas;
	}

	@Override
	public void run() {
		FileDialog fd = new FileDialog(mImageCanvas.getShell(), SWT.SAVE);
		fd.setFilterExtensions(new String[] { "*.png" });
		if (sLastUsedPath != null) {
			fd.setFilterPath(sLastUsedPath);
		}

		String path = fd.open();
		if (path == null) {
			return;
		}

		File f = new File(path);
		sLastUsedPath = f.getParent();
		mImageCanvas.exportImageTo(f);
	}
}
