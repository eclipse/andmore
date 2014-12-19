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
package org.eclipse.andmore.android.mat.panes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.mat.i18n.MatNLS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.query.registry.ArgumentSet;
import org.eclipse.mat.query.registry.QueryDescriptor;
import org.eclipse.mat.query.registry.QueryRegistry;
import org.eclipse.mat.query.registry.QueryResult;
import org.eclipse.mat.snapshot.IOQLQuery;
import org.eclipse.mat.snapshot.OQLParseException;
import org.eclipse.mat.snapshot.SnapshotFactory;
import org.eclipse.mat.ui.Messages;
import org.eclipse.mat.ui.editor.AbstractEditorPane;
import org.eclipse.mat.ui.editor.AbstractPaneJob;
import org.eclipse.mat.ui.editor.CompositeHeapEditorPane;
import org.eclipse.mat.ui.editor.EditorPaneRegistry;
import org.eclipse.mat.ui.util.ErrorHelper;
import org.eclipse.mat.ui.util.PaneState;
import org.eclipse.mat.ui.util.PaneState.PaneType;
import org.eclipse.mat.ui.util.ProgressMonitorWrapper;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

@SuppressWarnings("restriction")
public class MotodevPane extends CompositeHeapEditorPane {

	// Select statement
	private String queryFirstPart = "select * from \"";

	private String querySecondPart = ".*\"";

	// Query to be executed
	private String queryString;

	// Pane title
	private static final String PANE_TITLE = MatNLS.Motodev_Pane_Title;

	// Pane ID
	public final static String MOTODEV_PANE_ID = "org.eclipse.andmore.android.mat.MotodevPane";

	private Action executeAction;

	// Action to be executed
	@Override
	public void createPartControl(Composite parent) {
		createContainer(parent);

		// Retrive the selected app by the user and construct the query
		String selectedApp = getEditorInput().getName().substring(
				getEditorInput().getName().lastIndexOf(File.separator) + 1);

		queryString = queryFirstPart + AndroidPlugin.getDefault().getPreferenceStore().getString(selectedApp)
				+ querySecondPart;

		makeActions();

	}

	private void makeActions() {
		executeAction = new ExecuteQueryAction();

		executeAction.run();
	}

	@Override
	public String getTitle() {
		return PANE_TITLE;
	}

	@Override
	public void initWithArgument(final Object param) {
		if (param instanceof String) {
			queryString = (String) param;
			executeAction.run();
		} else if (param instanceof QueryResult) {
			QueryResult queryResult = (QueryResult) param;
			initQueryResult(queryResult, null);
		} else if (param instanceof PaneState) {
			queryString = ((PaneState) param).getIdentifier();
			new ExecuteQueryAction((PaneState) param).run();
		}
	}

	private void initQueryResult(QueryResult queryResult, PaneState state) {
		IOQLQuery.Result subject = (IOQLQuery.Result) queryResult.getSubject();
		queryString = subject.getOQLQuery();

		AbstractEditorPane pane = EditorPaneRegistry.instance().createNewPane(subject, this.getClass());

		if (state == null) {
			for (PaneState child : getPaneState().getChildren()) {
				if (queryString.equals(child.getIdentifier())) {
					state = child;
					break;
				}
			}

			if (state == null) {
				state = new PaneState(PaneType.COMPOSITE_CHILD, getPaneState(), queryString, true);
				state.setImage(getTitleImage());
			}
		}

		pane.setPaneState(state);

		createResultPane(pane, queryResult);
	}

	// //////////////////////////////////////////////////////////////
	// job to execute query
	// //////////////////////////////////////////////////////////////

	class OQLJob extends AbstractPaneJob {
		String queryString;

		PaneState state;

		public OQLJob(AbstractEditorPane pane, String queryString, PaneState state) {
			super(queryString.toString(), pane);
			this.queryString = queryString;
			this.state = state;
			this.setUser(true);
		}

		@Override
		protected IStatus doRun(IProgressMonitor monitor) {
			try {
				QueryDescriptor descriptor = QueryRegistry.instance().getQuery("oql");//$NON-NLS-1$
				ArgumentSet argumentSet = descriptor.createNewArgumentSet(getEditor().getQueryContext());
				argumentSet.setArgumentValue("queryString", queryString);//$NON-NLS-1$
				final QueryResult result = argumentSet.execute(new ProgressMonitorWrapper(monitor));

				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						initQueryResult(result, state);
					}
				});
			} catch (final Exception e) {
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						try {
							createExceptionPane(e, queryString);
						} catch (PartInitException pie) {
							ErrorHelper.logThrowable(pie);
						}
					}
				});
			}

			return Status.OK_STATUS;
		}
	}

	public void createExceptionPane(Exception cause, String queryString) throws PartInitException {
		StringBuilder buf = new StringBuilder(256);
		buf.append(Messages.OQLPane_ExecutedQuery);
		buf.append(queryString);

		Throwable t = null;
		if (cause instanceof SnapshotException) {
			buf.append(Messages.OQLPane_ProblemReported);
			buf.append(cause.getMessage());
			t = cause.getCause();
		} else {
			t = cause;
		}

		if (t != null) {
			StringWriter w = null;
			PrintWriter o = null;
			try {
				buf.append("\n\n");//$NON-NLS-1$
				w = new StringWriter();
				o = new PrintWriter(w);
				t.printStackTrace(o);
				o.flush();

				buf.append(w.toString());
			} finally {
				try {
					w.close();
					o.close();
				} catch (IOException e) {
					StudioLogger.error(e.getMessage());
				}
			}

		}

		try {
			AbstractEditorPane pane = EditorPaneRegistry.instance().createNewPane("TextViewPane");//$NON-NLS-1$
			if (pane == null) {
				throw new PartInitException(Messages.OQLPane_PaneNotFound);
			}

			// no pane state -> do not include in navigation history
			createResultPane(pane, buf.toString());
		} catch (CoreException e) {
			throw new PartInitException(ErrorHelper.createErrorStatus(e));
		}
	}

	private class ExecuteQueryAction extends Action {
		private PaneState state;

		public ExecuteQueryAction() {
			this(null);
		}

		public ExecuteQueryAction(PaneState state) {
			this.state = state;
		}

		@Override
		public void run() {
			try {
				String query = queryString;

				try {
					// force parsing of OQL query
					SnapshotFactory.createQuery(query);
					new OQLJob(MotodevPane.this, query, state).schedule();
				} catch (final OQLParseException e) {
					createExceptionPane(e, query);
				} catch (Exception e) {
					createExceptionPane(e, query);
				}
			} catch (PartInitException e1) {
				ErrorHelper.logThrowableAndShowMessage(e1, Messages.OQLPane_ErrorExecutingQuery);
			}
		}

	}

}
