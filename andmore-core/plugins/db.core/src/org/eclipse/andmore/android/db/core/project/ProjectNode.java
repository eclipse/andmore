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
package org.eclipse.andmore.android.db.core.project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.event.DatabaseModelEvent;
import org.eclipse.andmore.android.db.core.event.DatabaseModelEventManager;
import org.eclipse.andmore.android.db.core.exception.MotodevDbException;
import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.model.DbModel;
import org.eclipse.andmore.android.db.core.model.TableModel;
import org.eclipse.andmore.android.db.core.ui.AbstractTreeNode;
import org.eclipse.andmore.android.db.core.ui.DbNode;
import org.eclipse.andmore.android.db.core.ui.IDbNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.core.ui.action.IDbCreatorNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;

/**
 * Implements the Project Node of the database tree view. It listens for changes
 * in project such as .db deletion/rename/addition that reflect in database
 * model.
 */
public class ProjectNode extends AbstractTreeNode implements IDbCreatorNode, IResourceChangeListener {

	public static final String DB_FOLDER = "assets"; //$NON-NLS-1$

	private IProject project;

	@SuppressWarnings("unused")
	private ProjectNode() {
		// Forcing user to use a proper constructor
	}

	public ProjectNode(IProject project, ITreeNode parent) {
		this(project.getName(), parent);
		this.project = project;
	}

	public ProjectNode(String id, ITreeNode parent) {

		super(parent);
		init(id);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	@Override
	public void refresh() {
		clear();
		loadContent();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.IDbCreatorNode#createDb(java.lang
	 * .String)
	 */
	@Override
	public IStatus createDb(String dbName) {
		return createDb(dbName, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.IDbCreatorNode#createDb(java.lang
	 * .String, java.util.List)
	 */
	@Override
	public IStatus createDb(String dbName, List<TableModel> tables) {
		IStatus status = Status.OK_STATUS;
		dbName = !dbName.endsWith(".db") ? dbName + ".db" : dbName; //$NON-NLS-1$ //$NON-NLS-2$
		IPath projectPath = project.getLocation();
		File assetsFolder = new File(projectPath.toFile(), "assets"); //$NON-NLS-1$
		if (!assetsFolder.exists()) {
			assetsFolder.mkdirs();
		}
		IPath dbPath = new Path(assetsFolder.getAbsolutePath()).append(File.separator + dbName);
		try {
			DbNode dbNode = new DbNode(dbPath, this, true);
			if (tables != null) {
				status = dbNode.createTables(tables);
			}
			putChild(dbNode);
		} catch (MotodevDbException e) {
			status = new Status(IStatus.ERROR, DbCoreActivator.PLUGIN_ID, NLS.bind(
					DbCoreNLS.ProjectNode_Error_While_Creating_DB, dbName), e);
		}

		return status;
	}

	private void init(String id) {
		setId(id);
		setName(id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#getIcon()
	 */
	@Override
	public ImageDescriptor getIcon() {
		return getSpecificIcon("org.eclipe.andmore", "icons/android.png"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Loads the db files from the Android projects of the current workspace
	 * 
	 * @return list of db nodes
	 */
	private void loadContent() {
		File folder = ResourcesPlugin.getWorkspace().getRoot().getProject(getName()).getFile(DB_FOLDER).getLocation()
				.toFile();
		if (folder.exists() && (folder.list().length > 0)) {
			File[] foundFiles = folder.listFiles();
			List<ITreeNode> dbNodes = new ArrayList<ITreeNode>(foundFiles.length);
			for (File file : foundFiles) {
				if (DbModel.isValidSQLiteDatabase(file)) {
					DbNode dbNode;
					try {
						dbNode = new DbNode(new Path(file.getAbsolutePath()), this);
						dbNodes.add(dbNode);
					} catch (MotodevDbException e) {
						// Invalid db file found do nothing with it.
					}
				}
			}
			putChildren(dbNodes);
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

	/**
	 * @return the project
	 */
	protected IProject getProject() {
		return project;
	}

	/**
	 * 
	 * @param file
	 * @return
	 * @throws MotodevDbException
	 *             if dbPath does not contains a valid SQLite3 database file
	 */
	public void addDb(File file) {
		try {
			if (DbModel.isValidSQLiteDatabase(file)) {
				IDbNode dN = null;
				Path dbPath = new Path(file.getAbsolutePath());
				dN = getDatabaseNodeFromFile(file);
				if (dN == null) {
					dN = new DbNode(dbPath, this);
					putChild(dN);
				}
			}
		} catch (MotodevDbException e) {
			String message = NLS.bind(DbCoreNLS.ProjectNode_Failed_ToVerify_If_DB_Is_Valid, file.getName());
			StudioLogger.error(ProjectNode.class, message, e);
		}
	}

	/**
	 * Deletes the database from filesystem
	 */
	@Override
	public IStatus deleteDb(IDbNode dbNode) {
		IStatus status = dbNode.deleteDb();

		if (status.isOK()) {
			removeDb(dbNode);
		}
		return status;
	}

	/**
	 * Removes the node from database (e.g. when project is closed)
	 * 
	 * @param dbNode
	 * @param dbName
	 */
	public void removeDb(IDbNode dbNode) {
		removeChild(dbNode);
	}

	protected void updateDatabase(File dbFile) {
		ITreeNode databaseNode = getDatabaseNodeFromFile(dbFile);
		if (databaseNode != null) {
			DatabaseModelEventManager.getInstance().fireEvent(databaseNode, DatabaseModelEvent.EVENT_TYPE.UPDATE);
		}
	}

	private IDbNode getDatabaseNodeFromFile(File dbFile) {
		Path dbFilePath = new Path(dbFile.getAbsolutePath());
		String dbFileName = dbFilePath.lastSegment();
		String id = dbFilePath.toFile().getParent() + "." + dbFileName; //$NON-NLS-1$
		IDbNode databaseNode = null;
		ITreeNode node = getChildById(id);
		if (node instanceof IDbNode) {
			databaseNode = (IDbNode) node;
		}
		return databaseNode;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResource res = event.getResource();
		switch (event.getType()) {
		case IResourceChangeEvent.PRE_DELETE:
			// listener warns user about an opened database connection related
			// to this project (when it tries to delete the project).
			// The user has to close the connection itself.
			boolean openedConnection = false;
			try {
				if (res instanceof IProject) {
					IProject project = (IProject) res;
					IFolder assetsFolder = project.getFolder(DB_FOLDER);
					if (assetsFolder.exists()) {
						IResource assetsRes[] = assetsFolder.members();
						for (int i = 0; i < assetsRes.length; i++) {
							if (assetsRes[i] instanceof IFile) {
								IFile f = (IFile) assetsRes[i];
								IDbNode dbNode = getDatabaseNodeFromFile(f.getLocation().toFile());
								if (dbNode.isConnected()) {
									openedConnection = true;
								}
							}
						}
					}
				}
			} catch (Exception e) {
				// do nothing
			}
			if (openedConnection) {
				EclipseUtils.showWarningDialog(DbCoreNLS.UI_DeleteProjectDialogTitle,
						DbCoreNLS.UI_DeleteProjectDialogMsg);
			}

			break;
		case IResourceChangeEvent.POST_CHANGE:
			try {
				event.getDelta().accept(new ResourceDeltaVisior(this));
			} catch (CoreException e) {
				StudioLogger.error(ProjectNode.class, "Error listening to changes in resources", e); //$NON-NLS-1$
			}
			break;
		}

	}

	/**
	 * Refresh assets folder under the android project.
	 * 
	 * @return true if success false otherwise.
	 */
	public boolean refreshAssetsFolder() {
		IFolder assetsFolder = this.project.getFolder(DB_FOLDER);
		if (assetsFolder.exists()) {
			try {
				assetsFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (CoreException e) {
				return false;
			}
		}
		return true;
	}

	class ResourceDeltaVisior implements IResourceDeltaVisitor {
		private final ProjectNode projectNode;

		ResourceDeltaVisior(ProjectNode projectNode) {
			this.projectNode = projectNode;
		}

		@Override
		public boolean visit(IResourceDelta delta) {
			IResource res = delta.getResource();
			if ((res instanceof IResource) && (res.getFileExtension() != null)
					&& res.getProject().equals(projectNode.getProject())
					&& res.getFileExtension().equalsIgnoreCase("db")) //$NON-NLS-1$
			{
				// avoid to add db to the wrong project - it may happen in case
				// of copying db from one project to another
				switch (delta.getKind()) {
				case IResourceDelta.ADDED:
					StudioLogger.info("Database added: " + res.getFullPath()); //$NON-NLS-1$
					addDb(res.getLocation().toFile());
					break;
				case IResourceDelta.REMOVED:
					StudioLogger.info("Database deleted: " + res.getFullPath()); //$NON-NLS-1$
					IDbNode dbNode = getDatabaseNodeFromFile(res.getLocation().toFile());
					if (dbNode != null) {
						removeDb(dbNode);
					}
					break;
				}
			}

			return true; // visit the children
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#clean()
	 */
	@Override
	public void cleanUp() {
		super.cleanUp();
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

}
