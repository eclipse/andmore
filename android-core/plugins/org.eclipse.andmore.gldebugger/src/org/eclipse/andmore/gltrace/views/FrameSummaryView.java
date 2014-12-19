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

import org.eclipse.andmore.gltrace.editors.GLFunctionTraceViewer;
import org.eclipse.ui.IWorkbenchPart;

/**
 * The {@link FrameSummaryView} is a page book view with pages of type
 * {@link FrameSummaryViewPage}.
 */
public class FrameSummaryView extends GLPageBookView {
	public static final String ID = "org.eclipse.andmore.gltrace.views.FrameBuffer"; //$NON-NLS-1$

	public FrameSummaryView() {
		super("Open a GL Trace file to view the framebuffer contents.");
	}

	@Override
	protected PageRec doCreatePage(IWorkbenchPart part) {
		if (!(part instanceof GLFunctionTraceViewer)) {
			return null;
		}

		GLFunctionTraceViewer viewer = (GLFunctionTraceViewer) part;
		FrameSummaryViewPage page = viewer.getFrameSummaryViewPage();
		initPage(page);
		page.createControl(getPageBook());

		return new PageRec(part, page);
	}

	@Override
	protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
		FrameSummaryViewPage page = (FrameSummaryViewPage) pageRecord.page;
		page.dispose();
		pageRecord.dispose();
	}
}
