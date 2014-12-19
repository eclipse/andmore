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
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;

/**
 * {@link GLPageBookView} is an abstract {@link PageBookView} that can be used
 * to provide page book view's whose main part is a
 * {@link GLFunctionTraceViewer}.
 */
public abstract class GLPageBookView extends PageBookView {
	private final String mDefaultMessage;

	public GLPageBookView(String defaultMessage) {
		super();

		mDefaultMessage = defaultMessage;
	}

	@Override
	protected IPage createDefaultPage(PageBook book) {
		MessagePage page = new MessagePage();
		initPage(page);
		page.createControl(book);
		page.setMessage(mDefaultMessage);
		return page;
	}

	@Override
	protected IWorkbenchPart getBootstrapPart() {
		IWorkbenchPage page = getSite().getPage();
		if (page != null) {
			return page.getActiveEditor();
		}

		return null;
	}

	@Override
	protected boolean isImportant(IWorkbenchPart part) {
		return part instanceof GLFunctionTraceViewer;
	}

	@Override
	public void partBroughtToTop(IWorkbenchPart part) {
		partActivated(part);
	}

	@Override
	protected void showPageRec(PageRec pageRec) {
		IPageSite pageSite = getPageSite(pageRec.page);
		if (pageRec.page instanceof ISelectionProvider) {
			pageSite.setSelectionProvider((ISelectionProvider) pageRec.page);
		} else {
			pageSite.setSelectionProvider(null); // clear selection provider
		}

		super.showPageRec(pageRec);
	}
}
