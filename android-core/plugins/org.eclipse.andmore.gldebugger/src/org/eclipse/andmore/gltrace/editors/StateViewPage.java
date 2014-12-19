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

package org.eclipse.andmore.gltrace.editors;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.eclipse.andmore.gltrace.GlTracePlugin;
import org.eclipse.andmore.gltrace.editors.GLCallGroups.GLCallNode;
import org.eclipse.andmore.gltrace.model.GLCall;
import org.eclipse.andmore.gltrace.model.GLTrace;
import org.eclipse.andmore.gltrace.state.GLState;
import org.eclipse.andmore.gltrace.state.IGLProperty;
import org.eclipse.andmore.gltrace.state.StatePrettyPrinter;
import org.eclipse.andmore.gltrace.state.transforms.IStateTransform;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.Page;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A tree view of the OpenGL state. It listens to the current GLCall that is
 * selected in the Function Trace view, and updates its view to reflect the
 * state as of the selected call.
 */
public class StateViewPage extends Page implements ISelectionListener, ISelectionProvider {
	public static final String ID = "org.eclipse.andmore.gltrace.views.GLState"; //$NON-NLS-1$
	private static String sLastUsedPath;
	private static final ILock sGlStateLock = Job.getJobManager().newLock();

	private GLTrace mTrace;
	private List<GLCall> mGLCalls;

	/** OpenGL State as of call {@link #mCurrentStateIndex}. */
	private IGLProperty mState;
	private int mCurrentStateIndex;

	private String[] TREE_PROPERTIES = { "Name", "Value" };
	private TreeViewer mTreeViewer;
	private StateLabelProvider mLabelProvider;

	public StateViewPage(GLTrace trace) {
		setInput(trace);
	}

	public void setInput(GLTrace trace) {
		mTrace = trace;
		if (trace != null) {
			mGLCalls = trace.getGLCalls();
		} else {
			mGLCalls = null;
		}

		mState = GLState.createDefaultState();
		mCurrentStateIndex = -1;

		if (mTreeViewer != null) {
			mTreeViewer.setInput(mState);
			mTreeViewer.refresh();
		}
	}

	@Override
	public void createControl(Composite parent) {
		final Tree tree = new Tree(parent, SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);

		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));

		TreeColumn col1 = new TreeColumn(tree, SWT.LEFT);
		col1.setText(TREE_PROPERTIES[0]);
		col1.setWidth(200);

		TreeColumn col2 = new TreeColumn(tree, SWT.LEFT);
		col2.setText(TREE_PROPERTIES[1]);
		col2.setWidth(200);

		mTreeViewer = new TreeViewer(tree);
		mTreeViewer.setContentProvider(new StateContentProvider());
		mLabelProvider = new StateLabelProvider();
		mTreeViewer.setLabelProvider(mLabelProvider);
		mTreeViewer.setInput(mState);
		mTreeViewer.refresh();

		final IToolBarManager manager = getSite().getActionBars().getToolBarManager();
		manager.add(new Action("Save to File", PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ETOOL_SAVEAS_EDIT)) {
			@Override
			public void run() {
				saveCurrentState();
			}
		});
	}

	private void saveCurrentState() {
		final Shell shell = mTreeViewer.getTree().getShell();
		FileDialog fd = new FileDialog(shell, SWT.SAVE);
		fd.setFilterExtensions(new String[] { "*.txt" });
		if (sLastUsedPath != null) {
			fd.setFilterPath(sLastUsedPath);
		}

		String path = fd.open();
		if (path == null) {
			return;
		}

		File f = new File(path);
		sLastUsedPath = f.getParent();

		// export state to f
		StatePrettyPrinter pp = new StatePrettyPrinter();
		synchronized (sGlStateLock) {
			mState.prettyPrint(pp);
		}

		try {
			Files.write(pp.toString(), f, Charsets.UTF_8);
		} catch (IOException e) {
			ErrorDialog.openError(shell, "Export GL State", "Unexpected error while writing GL state to file.",
					new Status(IStatus.ERROR, GlTracePlugin.PLUGIN_ID, e.toString()));
		}
	}

	@Override
	public void init(IPageSite pageSite) {
		super.init(pageSite);
		pageSite.getPage().addSelectionListener(this);
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		super.dispose();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (!(part instanceof GLFunctionTraceViewer)) {
			return;
		}

		if (((GLFunctionTraceViewer) part).getTrace() != mTrace) {
			return;
		}

		if (!(selection instanceof TreeSelection)) {
			return;
		}

		GLCall selectedCall = null;

		Object data = ((TreeSelection) selection).getFirstElement();
		if (data instanceof GLCallNode) {
			selectedCall = ((GLCallNode) data).getCall();
		}

		if (selectedCall == null) {
			return;
		}

		final int selectedCallIndex = selectedCall.getIndex();

		// Creation of texture images takes a few seconds on the first run. So
		// run
		// the update task as an Eclipse job.
		Job job = new Job("Updating GL State") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Set<IGLProperty> changedProperties = null;

				try {
					sGlStateLock.acquire();
					changedProperties = updateState(mCurrentStateIndex, selectedCallIndex);
					mCurrentStateIndex = selectedCallIndex;
				} catch (Exception e) {
					GlTracePlugin.getDefault().logMessage("Unexpected error while updating GL State.");
					GlTracePlugin.getDefault().logMessage(e.getMessage());
					return new Status(IStatus.ERROR, GlTracePlugin.PLUGIN_ID,
							"Unexpected error while updating GL State.", e);
				} finally {
					sGlStateLock.release();
				}

				mLabelProvider.setChangedProperties(changedProperties);
				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						if (!mTreeViewer.getTree().isDisposed()) {
							mTreeViewer.refresh();
						}
					}
				});

				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.SHORT);
		job.schedule();
	}

	@Override
	public Control getControl() {
		if (mTreeViewer == null) {
			return null;
		}

		return mTreeViewer.getControl();
	}

	@Override
	public void setFocus() {
	}

	/**
	 * Update GL state from GL call at fromIndex to the call at toIndex. If
	 * fromIndex < toIndex, the GL state will be updated by applying all the
	 * transformations corresponding to calls from (fromIndex + 1) to toIndex
	 * (inclusive). If fromIndex > toIndex, the GL state will be updated by
	 * reverting all the calls from fromIndex (inclusive) to (toIndex + 1).
	 * 
	 * @return GL state properties that changed as a result of this update.
	 */
	private Set<IGLProperty> updateState(int fromIndex, int toIndex) {
		assert fromIndex >= -1 && fromIndex < mGLCalls.size();
		assert toIndex >= 0 && toIndex < mGLCalls.size();

		if (fromIndex < toIndex) {
			return applyTransformations(fromIndex, toIndex);
		} else if (fromIndex > toIndex) {
			return revertTransformations(fromIndex, toIndex);
		} else {
			return Collections.emptySet();
		}
	}

	private Set<IGLProperty> applyTransformations(int fromIndex, int toIndex) {
		int setSizeHint = 3 * (toIndex - fromIndex) + 10;
		Set<IGLProperty> changedProperties = new HashSet<IGLProperty>(setSizeHint);

		for (int i = fromIndex + 1; i <= toIndex; i++) {
			GLCall call = mGLCalls.get(i);
			for (IStateTransform f : call.getStateTransformations()) {
				try {
					f.apply(mState);
					IGLProperty changedProperty = f.getChangedProperty(mState);
					if (changedProperty != null) {
						changedProperties.addAll(getHierarchy(changedProperty));
					}
				} catch (Exception e) {
					GlTracePlugin.getDefault().logMessage("Error applying transformations for " + call);
					GlTracePlugin.getDefault().logMessage(e.toString());
				}
			}
		}

		return changedProperties;
	}

	private Set<IGLProperty> revertTransformations(int fromIndex, int toIndex) {
		int setSizeHint = 3 * (fromIndex - toIndex) + 10;
		Set<IGLProperty> changedProperties = new HashSet<IGLProperty>(setSizeHint);

		for (int i = fromIndex; i > toIndex; i--) {
			List<IStateTransform> transforms = mGLCalls.get(i).getStateTransformations();
			// When reverting transformations, iterate from the last to first so
			// that the reversals
			// are performed in the correct sequence.
			for (int j = transforms.size() - 1; j >= 0; j--) {
				IStateTransform f = transforms.get(j);
				f.revert(mState);

				IGLProperty changedProperty = f.getChangedProperty(mState);
				if (changedProperty != null) {
					changedProperties.addAll(getHierarchy(changedProperty));
				}
			}
		}

		return changedProperties;
	}

	/**
	 * Obtain the list of properties starting from the provided property up to
	 * the root of GL state.
	 */
	private List<IGLProperty> getHierarchy(IGLProperty changedProperty) {
		List<IGLProperty> changedProperties = new ArrayList<IGLProperty>(5);
		changedProperties.add(changedProperty);

		// add the entire parent chain until we reach the root
		IGLProperty prop = changedProperty;
		while ((prop = prop.getParent()) != null) {
			changedProperties.add(prop);
		}

		return changedProperties;
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		mTreeViewer.addSelectionChangedListener(listener);
	}

	@Override
	public ISelection getSelection() {
		return mTreeViewer.getSelection();
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		mTreeViewer.removeSelectionChangedListener(listener);
	}

	@Override
	public void setSelection(ISelection selection) {
		mTreeViewer.setSelection(selection);
	}
}
