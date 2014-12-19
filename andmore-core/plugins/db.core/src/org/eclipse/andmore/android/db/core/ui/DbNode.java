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
package org.eclipse.andmore.android.db.core.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.exception.MotodevDbException;
import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.model.DbModel;
import org.eclipse.andmore.android.db.core.model.TableModel;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.datatools.connectivity.ConnectionProfileException;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.datatools.sqltools.result.ResultsViewAPI;
import org.eclipse.datatools.sqltools.result.core.IResultManagerListener;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * This class represents a database node on the DB Explorer tree view
 */
public class DbNode extends AbstractTreeNode implements IDbNode {
	/**
	 * Properties name space.
	 */
	public static final String PROP_NAMESPACE = "org.eclipse.andmore.android.db.core"; //$NON-NLS-1$

	/**
	 * Property value used to check if the database is disconnected.
	 */
	public static final String PROP_VALUE_DB_DISCONNECTED = "org.eclipse.andmore.android.db.core.databaseDisconnected"; //$NON-NLS-1$

	/**
	 * Property value used to check if the database is connected.
	 */
	public static final String PROP_VALUE_DB_CONNECTED = "org.eclipse.andmore.android.db.core.databaseConnected"; //$NON-NLS-1$

	/**
	 * Property name used to check database connection status
	 * (connected/disconnected).
	 */
	public static final String PROP_NAME_DB_CONNECTION = "org.eclipse.andmore.android.db.core.databaseConnection"; //$NON-NLS-1$

	/**
	 * Property name used to check database connection status
	 * (connected/disconnected).
	 */
	public static final String PROP_NAME_DB_NODE_TYPE = "org.eclipse.andmore.android.db.core.IDbNodeType"; //$NON-NLS-1$

	/**
	 * Property value used to check if the database is connected.
	 */
	public static final String PROP_VALUE_DB_NODE_IS_EXT_STORAGE = "org.eclipse.andmore.android.db.core.isExternalStorage"; //$NON-NLS-1$

	private class ResultManagerAdapter extends AbstractDbResultManagerAdapter {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.andmore.android.db.core.ui.AbstractDbResultManagerAdapter
		 * #statementExecuted(java.lang.String, java.lang.String)
		 */
		@Override
		public void statementExecuted(String profileName, String sqlStatement) {
			if (model.getProfileName().equals(profileName)) {
				// Ignore group execution and read access to table(Select).
				// We'll handle only db changes.
				if ((!sqlStatement.equals("Group Execution")) //$NON-NLS-1$
						&& (sqlStatement.indexOf("select") != 0) && (!sqlStatement.equals(""))) //$NON-NLS-1$ //$NON-NLS-2$
				{
					if (sqlStatement.startsWith("drop table") //$NON-NLS-1$
							|| sqlStatement.startsWith("create table")) //$NON-NLS-1$
					{
						// A new table has been created let's refresh the dbNode
						// in order to get a update copy
						refreshAsync();
					} else if (sqlStatement.startsWith("alter table")) //$NON-NLS-1$
					{
						if (!sqlStatement.contains("rename")) //$NON-NLS-1$
						{
							// Table has been altered but not renamed, let's
							// refresh the table node, loading the possible
							// changes
							String tableName = sqlStatement.replace("alter table ", ""); //$NON-NLS-1$ //$NON-NLS-2$
							tableName.substring(0, tableName.indexOf(" ")); //$NON-NLS-1$
							List<ITreeNode> children = getChildren();
							for (ITreeNode child : children) {
								if (child.getName().equalsIgnoreCase(tableName)) {
									child.refreshAsync();
									break;
								}
							}
						} else {
							// Since a name has been renamed a refresh on this
							// db node will force to use the latest information.
							refreshAsync();
						}
					}
				}
			}
		}
	};

	public static final String ICON_PATH = "icons/obj16/dbplate.gif"; //$NON-NLS-1$

	protected DbModel model;

	private IResultManagerListener resultManagerListener;

	protected boolean forceCloseEditors;

	@SuppressWarnings("unused")
	private DbNode() {
		// Forcing user to use a proper constructor (with a parent)
	}

	protected DbNode(ITreeNode parent) {
		super(parent);
	}

	/**
	 * Creates a new DBNode by using a existent db file.
	 * 
	 * @param dbFilePath
	 *            The SQLite database File
	 * @param parent
	 *            The parent of the new node.
	 * @throws MotodevDbException
	 */
	public DbNode(IPath dbFilePath, ITreeNode parent) throws MotodevDbException {
		this(parent);
		init(dbFilePath);
		model = new DbModel(dbFilePath);
	}

	/**
	 * Creates a new DBNode by creating a new SQLite3 database file if
	 * requested.
	 * 
	 * @param dbPath
	 *            The SQLite database File
	 * @param parent
	 *            The parent of the new node.
	 * @param create
	 *            set this flag to true if you want to create a new db file, if
	 *            the flag is false the behavior is the same as the constructor
	 *            DbNode(Path dbFilePath, AbstractTreeNode parent)
	 * @throws MotodevDbException
	 */
	public DbNode(IPath dbPath, ITreeNode parent, boolean create) throws MotodevDbException {
		this(parent);
		init(dbPath);
		try {
			model = new DbModel(dbPath, create);
		} catch (MotodevDbException e) {
			throw new MotodevDbException("Could not create DBNode", e); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.IDBNode#connect()
	 */
	@Override
	public IStatus connect() {
		IStatus status = model.connect();
		if (status.isOK()) {
			if (resultManagerListener == null) {
				resultManagerListener = new ResultManagerAdapter();
				ResultsViewAPI.getInstance().getResultManager().addResultManagerListener(resultManagerListener);
			}
		}

		setNodeStatus(status);
		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.IDBNode#disconnect()
	 */
	@Override
	public IStatus disconnect() {
		IStatus status = closeAssociatedEditors();
		if (status.isOK()) {
			status = model.disconnect();
			if (status.isOK()) {
				if (resultManagerListener != null) {
					ResultsViewAPI.getInstance().getResultManager().removeResultManagerListener(resultManagerListener);
					resultManagerListener = null;
				}
				clear();
			}
			if (status.getSeverity() != IStatus.CANCEL) {
				setNodeStatus(status);
			}
		}

		if (status.getSeverity() != IStatus.CANCEL) {
			setNodeStatus(status);
		}

		return status;
	}

	@Override
	public IStatus createTables(List<TableModel> tables) {
		IStatus status = Status.OK_STATUS;
		List<ITreeNode> tableNodes = new ArrayList<ITreeNode>(tables.size());
		for (TableModel table : tables) {
			status = model.createTable(table);
			if (status.isOK()) {
				TableNode tableNode = new TableNode(getTable(table.getName()), model, this);
				tableNodes.add(tableNode);
			} else {
				break;
			}
		}

		if (status.isOK()) {
			putChildren(tableNodes);
		}

		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.IDBNode#createTable(java.lang.
	 * String, java.lang.String)
	 */
	@Override
	public IStatus createTable(TableModel table) {
		IStatus status = model.createTable(table);
		if (status.isOK()) {
			TableNode tableNode = new TableNode(getTable(table.getName()), model, this);
			putChild(tableNode);
		}

		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.IDBNode#deleteTable(java.lang.
	 * String)
	 */
	@Override
	public IStatus deleteTable(ITableNode tableNode) {
		IStatus status = model.deleteTable(tableNode.getName());
		if (status.isOK()) {
			removeChild(tableNode);
		}

		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.IDBNode#getTables()
	 */
	@Override
	public List<Table> getTables() {
		return model.getTables();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#refresh()
	 */
	@Override
	public void refresh() {
		if (!model.isConnected()) {
			connect();
		}
		clear();
		List<Table> tables = getTables();
		List<ITreeNode> tableNodes = new ArrayList<ITreeNode>(tables.size());
		for (Table table : tables) {
			TableNode tableNode = new TableNode(table, model, this);
			tableNodes.add(tableNode);
		}
		putChildren(tableNodes);
	}

	protected IStatus closeAssociatedEditors(final boolean quiet, final boolean forceClose) {
		Set<IEditorPart> associatedEditors = getAssociatedEditors();
		IStatus status = Status.OK_STATUS;
		if (!associatedEditors.isEmpty()) {
			final boolean[] success = new boolean[] { true };
			for (final IEditorPart editor : associatedEditors) {
				final IWorkbenchPage page = EclipseUtils.getPageForEditor(editor);
				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						if (!quiet) {
							page.bringToTop(editor);
							if (!forceClose) {
								// Use the default Eclipse behavior
								if (!page.closeEditor(editor, true)) {
									success[0] = false;
								}
							} else {
								if (editor.isDirty()) {
									// Use our dialog, because the operation
									// can't be cancelled.
									boolean shallSave = EclipseUtils.showQuestionDialog(
											DbCoreNLS.DbNode_Close_Editor_Msg_Title,
											NLS.bind(DbCoreNLS.DbNode_Close_Editor_Msg, getName()));
									if (shallSave) {
										editor.doSave(new NullProgressMonitor());
									}
								}
								page.closeEditor(editor, false);
							}
						} else {
							page.closeEditor(editor, false);
						}
					}
				});

				if (!success[0]) {
					break;
				}
			}

			if (!success[0]) {
				status = new Status(IStatus.CANCEL, DbCoreActivator.PLUGIN_ID, DbCoreNLS.DbNode_Canceled_Save_Operation);
			}
		}
		return status;
	}

	/**
	 * @param status
	 * @return
	 */
	protected IStatus closeAssociatedEditors() {
		return closeAssociatedEditors(false, forceCloseEditors);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.IDBNode#getTable(java.lang.String)
	 */
	@Override
	public Table getTable(String tableName) {
		return model.getTable(tableName);
	}

	private void init(IPath dbFilePath) {
		String dbFileName = dbFilePath.lastSegment();
		String id = dbFilePath.toFile().getParent() + "." + dbFileName; //$NON-NLS-1$

		setId(id);
		setName(dbFileName);
		ImageDescriptor icon = AbstractUIPlugin.imageDescriptorFromPlugin(DbCoreActivator.PLUGIN_ID, ICON_PATH);
		setIcon(icon);
		setToolTip(dbFilePath);
	}

	/*
	 * Sets the tool tip for the node given its path.
	 */
	private void setToolTip(IPath dbPath) {
		// for mapped nodes (i.e., children of IDbMapperNode) the tool tip is
		// its path
		if (getParent() instanceof IDbMapperNode) {
			setTooltip(NLS.bind(DbCoreNLS.DbNode_Tooltip_Prefix, dbPath.toString()));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#isLeaf()
	 */
	@Override
	public boolean isLeaf() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.IDbNode#deleteDb()
	 */
	@Override
	public IStatus deleteDb() {
		closeAssociatedEditors(true, forceCloseEditors);
		disconnect();
		return model.deleteDb();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionFilter#testAttribute(java.lang.Object,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public boolean testAttribute(Object target, String name, String value) {
		boolean result = false;

		// check if 'name' is a specific DbNode property
		if (name.equals(PROP_NAME_DB_CONNECTION) || PROP_NAME_DB_CONNECTION.equals(PROP_NAMESPACE + '.' + name)) {
			if (value.equals(PROP_VALUE_DB_CONNECTED)) {
				result = isConnected();
			} else if (value.equals(PROP_VALUE_DB_DISCONNECTED)) {
				result = !isConnected();
			}
		} else if (name.equals(PROP_NAME_DB_NODE_TYPE)) {
			if (value.equals(PROP_VALUE_DB_NODE_IS_EXT_STORAGE)) {
				result = (getParent() instanceof IDbMapperNode);
			}
		} else {
			// check if 'name' is a generic ITreeNode property
			result = super.testAttribute(target, name, value);
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#getIcon()
	 */
	@Override
	public ImageDescriptor getIcon() {
		return getSpecificIcon("org.eclipse.datatools.connectivity.sqm.core.ui", //$NON-NLS-1$
				"icons/database.gif"); //$NON-NLS-1$
	}

	@Override
	public boolean isConnected() {
		return model != null ? model.isConnected() : false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#clean()
	 */
	@Override
	public void cleanUp() {
		setForceCloseEditors(true);
		disconnect();
		if (model != null) {
			try {
				model.cleanModel();
			} catch (ConnectionProfileException e) {
				StudioLogger.debug(this, "Unable to cleanup db model.");
			}
		}
		super.cleanUp();
	}

	/**
	 * Retrieves the open editors that is used to edit the given profile, if any
	 * 
	 * @param profile
	 *            The profile that owns the requested editor
	 * @return The open dirty editor for the given profile, or <code>null</code>
	 *         if there is no editor in this condition
	 */
	public Set<IEditorPart> getAssociatedEditors() {
		return model != null ? model.getAssociatedEditors() : new HashSet<IEditorPart>(0);
	}

	/**
	 * Checks if the db file exists in filesystem
	 * 
	 * @return
	 */
	public boolean existsDbFile() {
		return model != null ? model.getDbPath().toFile().exists() : false;
	}

	public IPath getPath() {
		return model != null ? model.getDbPath() : null;
	}

	/**
	 * @param forceCloseEditors
	 *            the forceCloseEditors to set
	 */
	protected void setForceCloseEditors(boolean forceCloseEditors) {
		this.forceCloseEditors = forceCloseEditors;
	}
}
