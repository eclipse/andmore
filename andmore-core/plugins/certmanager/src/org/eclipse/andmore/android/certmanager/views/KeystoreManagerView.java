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
package org.eclipse.andmore.android.certmanager.views;

import java.util.Collections;
import java.util.List;

import org.eclipse.andmore.android.certmanager.event.IKeyStoreModelListener;
import org.eclipse.andmore.android.certmanager.event.KeyStoreModelEvent;
import org.eclipse.andmore.android.certmanager.event.KeyStoreModelEventManager;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;
import org.eclipse.andmore.android.certmanager.ui.model.KeyStoreRootNode;
import org.eclipse.andmore.android.certmanager.ui.model.SigningAndKeysModelManager;
import org.eclipse.andmore.android.certmanager.ui.tree.ExpiresInColumnLabelProvider;
import org.eclipse.andmore.android.certmanager.ui.tree.KeystoreManagerTreeContentProvider;
import org.eclipse.andmore.android.certmanager.ui.tree.LastBackupDateColumnLabelProvider;
import org.eclipse.andmore.android.certmanager.ui.tree.NameAliasColumnLabelProvider;
import org.eclipse.andmore.android.certmanager.ui.tree.PathColumnLabelProvider;
import org.eclipse.andmore.android.certmanager.ui.tree.TypeColumnLabelProvider;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

/**
 * View to manage certificates under MOTODEV Studio for Android
 */
public class KeystoreManagerView extends ViewPart implements IKeyStoreModelListener {
	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "org.eclipse.andmore.android.packaging.ui.signingview"; //$NON-NLS-1$

	private TreeViewer viewer;

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL
				| SWT.MULTI); // Virtual is required due to
								// ILazyTreeContentProvider
		viewer.setUseHashlookup(true);
		viewer.setAutoExpandLevel(0);

		viewer.setContentProvider(new KeystoreManagerTreeContentProvider(viewer));
		viewer.setInput(getInitalInput());
		viewer.expandToLevel(getInitalInput(), 1);
		ColumnViewerToolTipSupport.enableFor(viewer);

		getSite().setSelectionProvider(viewer);

		Tree tree = viewer.getTree();
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		{
			TreeViewerColumn treeViewerColumn = new TreeViewerColumn(viewer, SWT.NONE);
			treeViewerColumn.setLabelProvider(new NameAliasColumnLabelProvider());
			TreeColumn trclmnNewColumn = treeViewerColumn.getColumn();
			trclmnNewColumn.setWidth(250);
			trclmnNewColumn.setText(CertificateManagerNLS.CertificateManagerView_NameAlias_ColumnName);

			tree.setSortColumn(treeViewerColumn.getColumn());
			tree.setSortDirection(SWT.DOWN);
		}
		{
			TreeViewerColumn treeViewerColumn = new TreeViewerColumn(viewer, SWT.NONE);
			treeViewerColumn.setLabelProvider(new TypeColumnLabelProvider());
			TreeColumn trclmnNewColumn = treeViewerColumn.getColumn();
			trclmnNewColumn.setWidth(75);
			trclmnNewColumn.setText(CertificateManagerNLS.CertificateManagerView_Type_ColumnName);
		}
		{
			TreeViewerColumn treeViewerColumn = new TreeViewerColumn(viewer, SWT.NONE);
			treeViewerColumn.setLabelProvider(new ExpiresInColumnLabelProvider());
			TreeColumn trclmnExpiresIn = treeViewerColumn.getColumn();
			trclmnExpiresIn.setWidth(100);
			trclmnExpiresIn.setText(CertificateManagerNLS.CertificateManagerView_ExpiresIn_ColumnName);
		}
		{
			TreeViewerColumn treeViewerColumn = new TreeViewerColumn(viewer, SWT.NONE);
			treeViewerColumn.setLabelProvider(new LastBackupDateColumnLabelProvider());
			TreeColumn trclmnLastBackupDate = treeViewerColumn.getColumn();
			trclmnLastBackupDate.setWidth(125);
			trclmnLastBackupDate.setText(CertificateManagerNLS.CertificateManagerView_LastBackupDate_ColumnName);
		}
		{
			TreeViewerColumn treeViewerColumn = new TreeViewerColumn(viewer, SWT.LEFT);
			treeViewerColumn.setLabelProvider(new PathColumnLabelProvider());
			TreeColumn trclmnPath = treeViewerColumn.getColumn();
			trclmnPath.setWidth(500);
			trclmnPath.setText(CertificateManagerNLS.CertificateManagerView_Path_ColumnName);
		}

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(viewer.getControl(), "org.eclipse.andmore.android.certmanager.viewer"); //$NON-NLS-1$

		hookContextMenu();

		getSite().setSelectionProvider(viewer);

		// register listener for model changes
		KeyStoreModelEventManager.getInstance().addListener(this);
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
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void fillContextMenu(IMenuManager manager) {
		// Other plug-ins can contribute there actions here
		// manager.add(openClose);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	/**
	 * Loads the keystores from preference and
	 * <user_home>\motodevstudio\tools\motodev.keystore
	 * 
	 * @return root node (invisible) that contains as children the keystores.
	 */
	private Object getInitalInput() {
		return SigningAndKeysModelManager.getInstance().getKeyStoresRootNode();
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	public TreeViewer getTreeViewer() {
		return viewer;
	}

	/**
	 * Closing the view
	 */
	@Override
	public void dispose() {
		super.dispose();
		// remove listener for model changes
		KeyStoreModelEventManager.getInstance().removeListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.certmanager.event.IKeyStoreModelListener#
	 * handleNodeAdditionEvent
	 * (org.eclipse.andmore.android.certmanager.event.KeyStoreModelEvent)
	 */
	@Override
	public void handleNodeAdditionEvent(final KeyStoreModelEvent keyStoreModeEvent) {
		Display display = Display.getDefault();
		if (!display.isDisposed()) {
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					ITreeNode treeNodeItem = keyStoreModeEvent.getTreeNodeItem();
					ITreeNode parentNode = treeNodeItem.getParent();

					/*
					 * (parentNode instanceof KeyStoreRootNode) is a workaround
					 * to add nodes to the root node since
					 * getTreeViewer().getExpandedState(parentNode) always
					 * return false when the parentNode is the root node
					 * (KeyStoreRootNode in this case)
					 */
					if (getTreeViewer().getExpandedState(parentNode) || (parentNode instanceof KeyStoreRootNode)) {
						getTreeViewer().add(parentNode, treeNodeItem);
					} else {
						List<ITreeNode> children;
						try {
							children = parentNode.getChildren();
						} catch (KeyStoreManagerException e) {
							children = Collections.emptyList();
						}

						if (children.size() > 0) {
							getTreeViewer().setChildCount(parentNode, children.size());
						}
					}
				}
			});
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.certmanager.event.IKeyStoreModelListener#
	 * handleNodeRemovalEvent
	 * (org.eclipse.andmore.android.certmanager.event.KeyStoreModelEvent)
	 */
	@Override
	public void handleNodeRemovalEvent(final KeyStoreModelEvent keyStoreModeEvent) {
		Display display = Display.getDefault();
		if (!display.isDisposed()) {
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					if (!getTreeViewer().getTree().isDisposed()) {
						getTreeViewer().remove(keyStoreModeEvent.getTreeNodeItem());
					}
				}
			});
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.certmanager.event.IKeyStoreModelListener#
	 * handleNodeUpdateEvent
	 * (org.eclipse.andmore.android.certmanager.event.KeyStoreModelEvent)
	 */
	@Override
	public void handleNodeUpdateEvent(final KeyStoreModelEvent keyStoreModeEvent) {
		Display display = Display.getDefault();
		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					ITreeNode treeNode = keyStoreModeEvent.getTreeNodeItem();
					getTreeViewer().update(treeNode, null);
				}
			});
		}
	}

	@Override
	public void handleNodeCollapseEvent(final KeyStoreModelEvent keyStoreModelEvent) {
		Display display = Display.getDefault();
		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					ITreeNode treeNode = keyStoreModelEvent.getTreeNodeItem();
					// Ugly workaround to avoid JFace treeViewer to enter in a
					// infinite loop asking for children all the time.
					getTreeViewer().remove(treeNode);
					getTreeViewer().add(treeNode.getParent(), treeNode);
				}
			});
		}
	}

	@Override
	public void handleNodeRefreshEvent(final KeyStoreModelEvent keyStoreModelEvent) {
		Display display = Display.getDefault();
		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					ITreeNode treeNode = keyStoreModelEvent.getTreeNodeItem();
					if (treeNode != null) {
						try {
							getTreeViewer().remove(treeNode, treeNode.getChildren().toArray());
						} catch (KeyStoreManagerException e) {
							StudioLogger.error("Error while accessing keystore manager. " + e.getMessage());
						}

						getTreeViewer().refresh(treeNode, true);
					}
				}
			});
		}
	}

	@Override
	public void handleNodeClearEvent(final KeyStoreModelEvent keyStoreModelEvent) {
		Display display = Display.getDefault();
		if (!display.isDisposed()) {
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					ITreeNode treeNode = keyStoreModelEvent.getTreeNodeItem();
					if (treeNode != null) {
						try {
							getTreeViewer().remove(treeNode, treeNode.getChildren().toArray());
							getTreeViewer().setChildCount(treeNode, 0);
						} catch (KeyStoreManagerException e) {
							StudioLogger.error("Error while accessing keystore manager. " + e.getMessage());
						}
					}
				}
			});
		}
	}
}