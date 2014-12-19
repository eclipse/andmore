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
package org.eclipse.andmore.android.db.core.ui.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.DbRootNodeReader;
import org.eclipse.andmore.android.db.core.event.DatabaseModelEvent;
import org.eclipse.andmore.android.db.core.event.DatabaseModelEventManager;
import org.eclipse.andmore.android.db.core.event.IDatabaseModelListener;
import org.eclipse.andmore.android.db.core.ui.AbstractTreeNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.core.ui.RootNode;
import org.eclipse.andmore.android.db.core.ui.tree.DatabaseExplorerTreeContentProvider;
import org.eclipse.andmore.android.db.core.ui.tree.DatabaseExplorerTreeLabelProvider;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.IEvaluationService;

/**
 * View containing a {@link TreeViewer} that is expanded as the nodes from type
 * {@link AbstractTreeNode} are double-clicked
 */
public class MOTODEVDatabaseExplorerView extends ViewPart implements IDatabaseModelListener {
	public static final String VIEW_ID = "org.eclipse.andmore.android.db.databaseView"; //$NON-NLS-1$

	public static final String DB_EXPLORER_VIEW_HELP = DbCoreActivator.PLUGIN_ID + ".dbexplorer"; //$NON-NLS-1$

	public static final String DB_EXPLORER_VIEW_CONTR_BROWSE_TABLE_ID = "org.eclipse.andmore.android.db.core.createTableCommand";

	private TreeViewer treeViewer;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 1;
		layout.marginWidth = 0;
		layout.marginHeight = 2;
		parent.setLayout(layout);

		GridData layoutData = new GridData();
		layoutData = new GridData();
		layoutData.grabExcessHorizontalSpace = true;
		layoutData.grabExcessVerticalSpace = true;
		layoutData.horizontalAlignment = GridData.FILL;
		layoutData.verticalAlignment = GridData.FILL;

		treeViewer = new TreeViewer(parent, SWT.SINGLE | SWT.VIRTUAL); // It is
																		// required
																		// due
																		// to
																		// ILazyTreeContentProvider
		treeViewer.setContentProvider(new DatabaseExplorerTreeContentProvider(treeViewer));
		treeViewer.setUseHashlookup(true);
		treeViewer.setAutoExpandLevel(0);
		treeViewer.setLabelProvider(new DatabaseExplorerTreeLabelProvider());
		ColumnViewerToolTipSupport.enableFor(treeViewer);
		treeViewer.setInput(getInitalInput());

		treeViewer.getControl().setLayoutData(layoutData);

		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof TreeSelection) {
					TreeSelection treeSelection = (TreeSelection) selection;
					Object element = treeSelection.getFirstElement();
					if (element instanceof AbstractTreeNode) {
						if (!treeViewer.getExpandedState(element)) {
							treeViewer.expandToLevel(element, 1);
						} else {
							treeViewer.collapseToLevel(element, 1);
						}
					}
				}
			}
		});
		hookContextMenu();

		getSite().setSelectionProvider(treeViewer);

		// register listener for model changes
		DatabaseModelEventManager.getInstance().addListener(this);

		// add context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, DB_EXPLORER_VIEW_HELP);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(treeViewer.getTree(), DB_EXPLORER_VIEW_HELP);
	}

	/**
	 * Loads the contributions from extension point
	 * org.eclipse.andmore.android.db.core.dbRootNode
	 * 
	 * @return root node (invisible) that contains as children the contributed
	 *         tree nodes subclassing {@link AbstractTreeNode}.
	 */
	private Object getInitalInput() {
		HashMap<String, AbstractTreeNode> model = new HashMap<String, AbstractTreeNode>();
		RootNode rootNode = new RootNode();
		try {
			DbRootNodeReader.loadRootNode(model);
			Set<Entry<String, AbstractTreeNode>> modelEntries = model.entrySet();
			List<ITreeNode> childrenList = new ArrayList<ITreeNode>(modelEntries.size());
			for (Map.Entry<String, AbstractTreeNode> childNode : modelEntries) {
				childrenList.add(childNode.getValue());
			}

			rootNode.putChildren(childrenList);
		} catch (PartInitException e) {
			StudioLogger.error(MOTODEVDatabaseExplorerView.class, "Problem creating MOTODEV Database Explorer Tree", e); //$NON-NLS-1$
		}
		return rootNode;
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
		treeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, treeViewer);
	}

	private void fillContextMenu(IMenuManager manager) {
		// Other plug-ins can contribute there actions here
		// manager.add(openClose);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		treeViewer.getTree().setFocus();
	}

	/**
	 * @return the treeViewer
	 */
	public TreeViewer getTreeViewer() {
		return treeViewer;
	}

	/**
	 * Get items selected in the tree
	 * 
	 * @return
	 */
	public ITreeNode getSelectedItemOnTree() {
		ITreeNode result = null;
		ITreeSelection treeSelection = null;
		ISelection selection = treeViewer.getSelection();

		if (selection instanceof ITreeSelection) {
			treeSelection = (ITreeSelection) selection;
			result = (ITreeNode) treeSelection.getFirstElement();
		}

		return result;

	}

	/**
	 * Collapse all nodes of the tree viewer.
	 **/
	public void collapseAllTreeItems() {
		treeViewer.collapseAll();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.ViewPart#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento memento) {
		SaveStateManager.getInstance().saveState();
		super.saveState(memento);
	}

	@Override
	public void handleNodeAdditionEvent(final DatabaseModelEvent databaseModelEvent) {
		Display display = Display.getDefault();
		if (!display.isDisposed()) {
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					ITreeNode treeNodeItem = databaseModelEvent.getTreeNodeItem();
					ITreeNode parentNode = treeNodeItem.getParent();
					if (getTreeViewer().getExpandedState(parentNode)) {
						getTreeViewer().add(parentNode, treeNodeItem);
					} else {
						List<ITreeNode> children = parentNode.getChildren();
						if (children.size() > 0) {
							getTreeViewer().setChildCount(parentNode, children.size());
						}
					}
				}
			});
		}
	}

	@Override
	public void handleNodeRemovalEvent(final DatabaseModelEvent databaseModelEvent) {
		Display display = Display.getDefault();
		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!treeViewer.getTree().isDisposed()) {
						getTreeViewer().remove(databaseModelEvent.getTreeNodeItem());
						ITreeNode parentNode = databaseModelEvent.getTreeNodeItem().getParent();
						getTreeViewer().setChildCount(parentNode, parentNode.getChildren().size());
					}
				}
			});
		}
	}

	@Override
	public void handleNodeUpdateEvent(final DatabaseModelEvent databaseModelEvent) {
		Display display = Display.getDefault();
		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					ITreeNode treeNode = databaseModelEvent.getTreeNodeItem();
					getTreeViewer().update(treeNode, null);

					// re-evaluate all properties to properly update commands
					// status
					// it's required since nodes may change theirs state/status
					// without firing SelectionChanged events
					for (String property : DbCoreActivator.getPluginProperties()) {
						IEvaluationService service = (IEvaluationService) PlatformUI.getWorkbench().getService(
								IEvaluationService.class);
						service.requestEvaluation(property); // properties are
																// defined in
																// plugin.xml
																// (propertyTesters)
					}
				}
			});
		}
	}

	@Override
	public void handleNodeClearEvent(final DatabaseModelEvent databaseModelEvent) {
		Display display = Display.getDefault();
		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					ITreeNode parentNode = databaseModelEvent.getTreeNodeItem();
					if (parentNode.isLoading()) {
						treeViewer.remove(parentNode, parentNode.getChildren().toArray()); // Removing
																							// all
																							// children
																							// before
																							// calling
																							// refresh
																							// seems
																							// to
																							// resolve
																							// the
																							// AccessViolation
																							// -
																							// see
																							// bug
																							// https://bugs.eclipse.org/bugs/show_bug.cgi?id=131209
						treeViewer.refresh(parentNode);
					} else {
						treeViewer.collapseToLevel(parentNode, AbstractTreeViewer.ALL_LEVELS);
					}
				}
			});
		}
	}

	@Override
	public void handleNodeRefreshEvent(final DatabaseModelEvent databaseModelEvent) {
		Display display = Display.getDefault();
		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					ITreeNode node = databaseModelEvent.getTreeNodeItem();
					getTreeViewer().refresh(node);
				}
			});
		}
	}

	/**
	 * Closing the view
	 */
	@Override
	public void dispose() {
		super.dispose();
		// remove listener for model changes
		DatabaseModelEventManager.getInstance().removeListeners(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.event.IDatabaseModelListener#
	 * handleNodeExpandEvent
	 * (org.eclipse.andmore.android.db.core.event.DatabaseModelEvent)
	 */
	@Override
	public void handleNodeExpandEvent(final DatabaseModelEvent databaseModelEvent) {
		Display display = Display.getDefault();
		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					treeViewer.expandToLevel(databaseModelEvent.getTreeNodeItem(), 1);
				}
			});
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.event.IDatabaseModelListener#
	 * handleNodeSelectEvent
	 * (org.eclipse.andmore.android.db.core.event.DatabaseModelEvent)
	 */
	@Override
	public void handleNodeSelectEvent(final DatabaseModelEvent databaseModelEvent) {
		Display display = Display.getDefault();
		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					treeViewer.setSelection(new StructuredSelection(databaseModelEvent.getTreeNodeItem()), true);
				}
			});
		}
	}
}
