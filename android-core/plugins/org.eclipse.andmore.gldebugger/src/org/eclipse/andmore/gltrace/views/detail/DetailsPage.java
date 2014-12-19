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

package org.eclipse.andmore.gltrace.views.detail;

import org.eclipse.andmore.gltrace.editors.GLFunctionTraceViewer;
import org.eclipse.andmore.gltrace.editors.GLCallGroups.GLCallNode;
import org.eclipse.andmore.gltrace.model.GLCall;
import org.eclipse.andmore.gltrace.model.GLTrace;
import org.eclipse.andmore.gltrace.state.IGLProperty;
import org.eclipse.andmore.gltrace.views.StateView;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.Page;

import java.util.Arrays;
import java.util.List;

public class DetailsPage extends Page implements ISelectionListener {
	private GLTrace mTrace;

	private IToolBarManager mToolBarManager;
	private Composite mTopComposite;
	private StackLayout mStackLayout;
	private Composite mBlankComposite;

	private List<IDetailProvider> mDetailProviders = Arrays.asList(new ShaderSourceDetailsProvider(),
			new ShaderUniformDetailsProvider(), new TextureImageDetailsProvider(), new VboDetailProvider(),
			new GlDrawCallDetailProvider(), new VertexAttribPointerDataDetailProvider());

	public DetailsPage(GLTrace trace) {
		mTrace = trace;
	}

	public void setInput(GLTrace trace) {
		mTrace = trace;
	}

	@Override
	public void createControl(Composite parent) {
		mTopComposite = new Composite(parent, SWT.NONE);
		mStackLayout = new StackLayout();
		mTopComposite.setLayout(mStackLayout);
		mTopComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

		mBlankComposite = new Composite(mTopComposite, SWT.NONE);

		mToolBarManager = getSite().getActionBars().getToolBarManager();

		for (IDetailProvider provider : mDetailProviders) {
			provider.createControl(mTopComposite);

			for (IContributionItem item : provider.getToolBarItems()) {
				mToolBarManager.add(item);
			}
		}

		setDetailsProvider(null);
	}

	private void setDetailsProvider(IDetailProvider provider) {
		for (IContributionItem item : mToolBarManager.getItems()) {
			item.setVisible(false);
		}

		if (provider == null) {
			setTopControl(mBlankComposite);
		} else {
			setTopControl(provider.getControl());

			for (IContributionItem item : provider.getToolBarItems()) {
				item.setVisible(true);
			}
		}

		mToolBarManager.update(true);
	}

	private void setTopControl(Control c) {
		mStackLayout.topControl = c;
		mTopComposite.layout();
	}

	@Override
	public Control getControl() {
		return mTopComposite;
	}

	@Override
	public void init(IPageSite pageSite) {
		super.init(pageSite);
		pageSite.getPage().addSelectionListener(this);
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);

		for (IDetailProvider provider : mDetailProviders) {
			provider.disposeControl();
		}

		super.dispose();
	}

	@Override
	public void setFocus() {
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part instanceof GLFunctionTraceViewer) {
			GLCall selectedCall = getSelectedCall((GLFunctionTraceViewer) part, selection);
			if (selectedCall == null) {
				return;
			}

			callSelected(selectedCall);
			return;
		} else if (part instanceof StateView) {
			IGLProperty selectedProperty = getSelectedProperty((StateView) part, selection);
			if (selectedProperty == null) {
				return;
			}

			stateVariableSelected(selectedProperty);
			return;
		}

		return;
	}

	private void stateVariableSelected(IGLProperty property) {
		for (IDetailProvider p : mDetailProviders) {
			if (!(p instanceof IStateDetailProvider)) {
				continue;
			}

			IStateDetailProvider sp = (IStateDetailProvider) p;
			if (sp.isApplicable(property)) {
				sp.updateControl(property);
				setDetailsProvider(sp);
				return;
			}
		}

		setDetailsProvider(null);
		return;
	}

	private void callSelected(GLCall selectedCall) {
		for (IDetailProvider p : mDetailProviders) {
			if (!(p instanceof ICallDetailProvider)) {
				continue;
			}

			ICallDetailProvider cp = (ICallDetailProvider) p;
			if (cp.isApplicable(selectedCall)) {
				cp.updateControl(mTrace, selectedCall);
				setDetailsProvider(cp);
				return;
			}
		}

		setDetailsProvider(null);
		return;
	}

	private GLCall getSelectedCall(GLFunctionTraceViewer part, ISelection selection) {
		if (part.getTrace() != mTrace) {
			return null;
		}

		if (!(selection instanceof TreeSelection)) {
			return null;
		}

		Object data = ((TreeSelection) selection).getFirstElement();
		if (data instanceof GLCallNode) {
			return ((GLCallNode) data).getCall();
		} else {
			return null;
		}
	}

	private IGLProperty getSelectedProperty(StateView view, ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			return null;
		}

		IStructuredSelection ssel = (IStructuredSelection) selection;
		@SuppressWarnings("rawtypes")
		List objects = ssel.toList();
		if (objects.size() > 0) {
			Object data = objects.get(0);
			if (data instanceof IGLProperty) {
				return (IGLProperty) data;
			}
		}

		return null;
	}
}
