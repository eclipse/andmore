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
package org.eclipse.andmore.android.db.devices.model;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.DDMSUtils;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.andmore.android.db.core.CanRefreshStatus;
import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.exception.MotodevDbException;
import org.eclipse.andmore.android.db.core.model.DbModel;
import org.eclipse.andmore.android.db.core.model.TableModel;
import org.eclipse.andmore.android.db.core.ui.AbstractDbResultManagerAdapter;
import org.eclipse.andmore.android.db.core.ui.DbNode;
import org.eclipse.andmore.android.db.core.ui.IDbNode;
import org.eclipse.andmore.android.db.core.ui.ITableNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.devices.DbDevicesPlugin;
import org.eclipse.andmore.android.db.devices.i18n.DbDevicesNLS;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.datatools.sqltools.result.ResultsViewAPI;
import org.eclipse.datatools.sqltools.result.core.IResultManagerListener;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * This class represents a tree node for a given SQLite3 database file located
 * on a Android device.
 */
public class DeviceDbNode extends DbNode implements IDbNode {
	/**
     * 
     */
	private static final int REMOTE_OPERATIONS_TIMEOUT = 2000;

	private final IPath remoteDbPath;

	private final String serialNumber;

	private IResultManagerListener resultManagerListener;

	private String localFileMd5;

	public boolean isDirty;

	private class ResultManagerListener extends AbstractDbResultManagerAdapter {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.andmore.android.db.core.ui.AbstractDbResultManagerAdapter
		 * #statementExecuted(java.lang.String, java.lang.String)
		 */
		@Override
		public void statementExecuted(String profilename, String sqlStatement) {
			if ((model != null) && model.getProfileName().equals(profilename)) {
				if ((!sqlStatement.equals("Group Execution")) //$NON-NLS-1$
						&& (sqlStatement.trim().toLowerCase().indexOf("select") != 0) //$NON-NLS-1$
						&& (!sqlStatement.trim().equals(""))) //$NON-NLS-1$
				{
					IStatus status = checkMd5Sum(true);

					if (status.isOK()) {
						status = pushLocalDbFile();
					}
					if (!status.isOK()) {
						isDirty = true;
					}

				}
			}
		}

	};

	/**
	 * Creates a new DeviceDbNode for an already existent SQLite3 database
	 * 
	 * @param remoteDbPath
	 *            the SQLite3 database file location at the device
	 * @param parent
	 *            this node parent
	 */
	public DeviceDbNode(IPath remoteDbPath, String serialNumber, ITreeNode parent) {
		super(parent);
		setId(serialNumber + "." + remoteDbPath.toString()); //$NON-NLS-1$
		this.remoteDbPath = remoteDbPath;
		this.serialNumber = serialNumber;
		setName(remoteDbPath.lastSegment());
		ImageDescriptor icon = AbstractUIPlugin.imageDescriptorFromPlugin(DbCoreActivator.PLUGIN_ID, DbNode.ICON_PATH);
		setIcon(icon);
		setTooltip(NLS.bind(DbDevicesNLS.DeviceDbNode_Tootip_Prefix, remoteDbPath.toString()));
	}

	/**
	 * Creates a new DeviceDbNode by creating a new SQLite3 database file if
	 * requested. This constructor will create a local temp file with the new
	 * SQLite3 database. the temp file will then be copied to the remotePath at
	 * the device.
	 * 
	 * @param remoteDbPath
	 *            The SQLite database File location at the device
	 * @param parent
	 *            The parent of the new node.
	 * @param create
	 *            set this flag to true if you want to create a new db file, if
	 *            the flag is false the behavior is the same as the constructor
	 *            DeviceDbNode(IPath remoteDbPath, String serialNumber,
	 *            ITreeNode parent)
	 * @throws MotodevDbException
	 *             if a problem occurred during database creation.
	 */
	public DeviceDbNode(IPath remoteDbPath, String serialNumber, ITreeNode parent, boolean create)
			throws MotodevDbException {
		this(remoteDbPath, serialNumber, parent);
		if (create) {
			try {
				File tempFile = getLocalTempFile();
				Path localDbPath = null;
				if (tempFile != null) {
					localDbPath = new Path(tempFile.getAbsolutePath());
					model = new DbModel(localDbPath, create, true);
					IStatus status = pushLocalDbFile(false);
					if (!status.isOK()) {
						deleteLocalDbModel();
						throw new MotodevDbException(DbDevicesNLS.DeviceDbNode_Create_Device_Db_Failed);
					}
				} else {
					throw new MotodevDbException(DbDevicesNLS.DeviceDbNode_Could_Not_Create_DeviceDbNode);
				}

			} catch (IOException e) {
				throw new MotodevDbException(DbDevicesNLS.DeviceDbNode_Could_Not_Create_DeviceDbNode, e);
			}
		}
	}

	/**
	 * @return
	 * @throws IOException
	 */
	private File getLocalTempFile() throws IOException {
		IPreferenceStore preferenceStore = DbDevicesPlugin.getDefault().getPreferenceStore();
		File tempLocationFile = null;

		if (!preferenceStore.isDefault(DbDevicesPlugin.DB_TEMP_PATH_PREFERENCE)) {
			String tempLocation = preferenceStore.getString(DbDevicesPlugin.DB_TEMP_PATH_PREFERENCE);
			tempLocationFile = new File(tempLocation);

			if (!tempLocationFile.isDirectory() || !FileUtil.canWrite(tempLocationFile)) {
				EclipseUtils.showErrorDialog(DbDevicesNLS.ERR_DbUtils_Local_Db_Title,
						NLS.bind(DbDevicesNLS.ERR_DbUtils_Local_Db_Msg, tempLocation));
				preferenceStore.setToDefault(DbDevicesPlugin.DB_TEMP_PATH_PREFERENCE);
			}

		}

		// If tempLocationFile is null the file will be created on system's
		// default temp dir.
		File tempFile = File.createTempFile(serialNumber + "_" + remoteDbPath.segment(1) + "_" + getName(), //$NON-NLS-1$ //$NON-NLS-2$
				"db", tempLocationFile); //$NON-NLS-1$
		tempFile.deleteOnExit();
		return tempFile;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.IDbNode#connect()
	 */
	@Override
	public IStatus connect() {
		IStatus status = null;

		File tempFile = null;
		try {
			tempFile = getLocalTempFile();
			status = pullRemoteTempFile(tempFile);
		} catch (IOException e) {
			status = new Status(IStatus.ERROR, DbDevicesPlugin.PLUGIN_ID,
					DbDevicesNLS.DeviceDbNode_Create_Temp_Local_Db_Failed, e);
		}

		if ((model != null) && status.isOK()) // Local model already exists, we
												// must verify the md5 and
												// update the localDbModel if
												// needed.
		{
			try {
				String newMd5Sum = FileUtil.calculateMd5Sum(tempFile);
				if (!newMd5Sum.equals(localFileMd5)) {
					deleteLocalDbModel(); // Remote file has been changed.
											// localDbModel must be updated
				}
			} catch (IOException e) {
				status = new Status(IStatus.ERROR, DbDevicesPlugin.PLUGIN_ID,
						DbDevicesNLS.DeviceDbNode_Calculate_Local_Md5_Failed, e);
			}

		}

		// model will be null if the remote file has been changed.
		if ((model == null) && status.isOK()) {
			try {
				model = new DbModel(Path.fromOSString(tempFile.getAbsolutePath()));
			} catch (MotodevDbException e) {
				status = new Status(IStatus.ERROR, DbDevicesPlugin.PLUGIN_ID, e.getMessage());
			}
		}

		if ((model != null) && status.isOK()) {
			try {
				localFileMd5 = getLocalMd5Sum();
				model.connect();
			} catch (IOException e) {
				status = new Status(IStatus.ERROR, DbDevicesPlugin.PLUGIN_ID,
						DbDevicesNLS.DeviceDbNode_Calculate_Local_Md5_Failed, e);
			}
		}

		if (status.isOK()) {
			if (resultManagerListener == null) {
				resultManagerListener = new ResultManagerListener();
				ResultsViewAPI.getInstance().getResultManager().addResultManagerListener(resultManagerListener);
			}
			isDirty = false;
		}

		setNodeStatus(status);

		return status != null ? status : Status.OK_STATUS;
	}

	/**
	 * @return
	 * @throws IOException
	 */
	private String getLocalMd5Sum() throws IOException {
		return FileUtil.calculateMd5Sum(model.getDbPath().toFile());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.IDbNode#disconnect()
	 */
	@Override
	public IStatus disconnect() {
		IStatus status = Status.OK_STATUS;
		if ((model != null) && model.isConnected()) {

			boolean canDisconnect = true;
			status = closeAssociatedEditors();

			canDisconnect = status.isOK();
			if (canDisconnect) {
				status = model.disconnect();
				if (status.isOK()) {
					deleteLocalDbModel();
					if (resultManagerListener != null) {
						ResultsViewAPI.getInstance().getResultManager()
								.removeResultManagerListener(resultManagerListener);
						resultManagerListener = null;
					}
				}
				clear();
				setNodeStatus(status);
			}
		}

		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.IDbNode#createTables(java.util
	 * .List)
	 */
	@Override
	public IStatus createTables(List<TableModel> tables) {
		IStatus status;
		status = checkMd5Sum(true);

		if (status.isOK()) {
			status = super.createTables(tables);
			if (status.isOK()) {
				pushLocalDbFile();
			}
		}

		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.IDbNode#createTable(org.eclipse
	 * .andmore.android.db.core.model.TableModel)
	 */
	@Override
	public IStatus createTable(TableModel table) {
		IStatus status;
		status = checkMd5Sum(true);

		if (status.isOK()) {
			status = super.createTable(table);
			if (status.isOK()) {
				pushLocalDbFile();
			}
		}

		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.IDbNode#deleteTable(java.lang.
	 * String)
	 */
	@Override
	public IStatus deleteTable(ITableNode tableNode) {
		IStatus status;
		status = checkMd5Sum(true);

		if (status.isOK()) {
			status = super.deleteTable(tableNode);
			if (status.isOK()) {
				pushLocalDbFile();
			}
		}

		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.IDbNode#deleteDb()
	 */
	@Override
	public IStatus deleteDb() {
		IStatus status = null;
		try {
			closeAssociatedEditors(true, forceCloseEditors);
			DDMSFacade.deleteFile(serialNumber, remoteDbPath.toString());
			disconnect();
		} catch (IOException e) {
			status = new Status(IStatus.ERROR, DbDevicesPlugin.PLUGIN_ID, NLS.bind(
					DbDevicesNLS.DeviceDbNode_Delete_Remote_File_Failed, remoteDbPath.toString(),
					DDMSFacade.getNameBySerialNumber(serialNumber)));
		}
		return status != null ? status : Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#canRefresh()
	 */
	@Override
	public IStatus canRefresh() {
		IStatus status = null;
		if (isDirty) {
			status = checkMd5Sum(false);
			if (!status.isOK()) {
				status = new CanRefreshStatus(CanRefreshStatus.ASK_USER | CanRefreshStatus.CANCELABLE,
						DbDevicesPlugin.PLUGIN_ID, NLS.bind(DbDevicesNLS.DeviceDbNode_DBOutOfSync_Refresh_Message,
								getName()));
			}
		} else {
			Set<IEditorPart> associatedEditors = getAssociatedEditors();
			if (!associatedEditors.isEmpty()) {
				status = new CanRefreshStatus(CanRefreshStatus.ASK_USER, DbDevicesPlugin.PLUGIN_ID, NLS.bind(
						DbDevicesNLS.DeviceDbNode_RefreshQuestion, getName()));
			}
		}
		return status != null ? status : Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#refresh()
	 */
	@Override
	public void refresh() {
		if (model != null) {
			if (model.isConnected()) {
				IStatus checkMd5Sum = checkMd5Sum(false);
				if (!checkMd5Sum.isOK()) {
					model.disconnect();
					deleteLocalDbModel();
					clear();
				}
			}
		}

		IStatus status = Status.OK_STATUS;
		if ((model == null) || !model.isConnected()) {
			status = connect(); // Force getting a fresh device db file
		}

		if (status.isOK()) {
			super.refresh();
		}
	}

	private boolean deleteLocalDbModel() {
		IStatus deleteDb = model.deleteDb();
		model = null;

		return deleteDb.isOK();
	}

	private IStatus pullRemoteTempFile(File tempFile) {
		IStatus status = null;
		IOConsoleOutputStream stream = null;
		try {
			IPath localDbPath = new Path(tempFile.getAbsolutePath());
			List<File> localList = Arrays.asList(new File[] { localDbPath.toFile() });
			List<String> remoteList = Arrays.asList(new String[] { remoteDbPath.toString() });

			stream = EclipseUtils.getStudioConsoleOutputStream(false);
			status = DDMSFacade.pullFiles(serialNumber, localList, remoteList, REMOTE_OPERATIONS_TIMEOUT,
					new NullProgressMonitor(), stream);
		} catch (Exception e) {
			status = new Status(IStatus.ERROR, DbDevicesPlugin.PLUGIN_ID,
					DbDevicesNLS.DeviceDbNode_Create_Temp_Local_Db_Failed, e);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					StudioLogger.error("Could not close stream: ", e.getMessage()); //$NON-NLS-1$
				}
			}
		}

		return status != null ? status : Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#refresh(boolean)
	 */
	@Override
	public void refresh(boolean canRefreshYesResponse) {
		if (canRefreshYesResponse) {
			closeAssociatedEditors(false, true);
		} else {
			pushLocalDbFile(false);
		}

		refresh();
	}

	private IStatus pushLocalDbFile() {
		return pushLocalDbFile(true);
	}

	private IStatus pushLocalDbFile(boolean warnUser) {
		IStatus status = null;
		IOConsoleOutputStream stream = null;
		try {
			IPath localDbPath = model.getDbPath();
			File localDbFile = localDbPath.toFile();
			List<File> localList = Arrays.asList(new File[] { localDbFile });
			List<String> remoteList = Arrays.asList(new String[] { remoteDbPath.toString() });
			stream = EclipseUtils.getStudioConsoleOutputStream(false);
			status = DDMSFacade.pushFiles(serialNumber, localList, remoteList, REMOTE_OPERATIONS_TIMEOUT,
					new NullProgressMonitor(), stream);
			if (status.isOK()) {
				isDirty = false;
			}

			// Update the local Md5Sum everytime the file is pushed to the
			// device.
			localFileMd5 = FileUtil.calculateMd5Sum(localDbFile);

			String appName = getParent().getName();
			if (warnUser) {
				boolean applicationRunning = DDMSFacade.isApplicationRunning(serialNumber, appName);

				if (applicationRunning) {
					EclipseUtils.showInformationDialog(DbDevicesNLS.DeviceDbNode_Application_Running_Msg_Title,
							NLS.bind(DbDevicesNLS.DeviceDbNode_Application_Running_Msg_Text, appName));
				}
			}
		} catch (Exception e) {
			status = new Status(IStatus.ERROR, DbDevicesPlugin.PLUGIN_ID, NLS.bind(
					DbDevicesNLS.DeviceDbNode_Push_Local_File_To_Device_Failed, serialNumber), e);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					StudioLogger.error("Could not close stream: ", e.getMessage()); //$NON-NLS-1$
				}
			}
		}

		return status != null ? status : Status.OK_STATUS;
	}

	/**
	 * @param status
	 * @return
	 */
	private IStatus checkMd5Sum(boolean warnUser) {
		File tempFile = null;
		IStatus status = null;
		if (localFileMd5 != null) // It will be null during create Db process.
		{
			try {
				tempFile = getLocalTempFile(); // Create a new tempFile,
												// different from the local db
												// model file, in order to
												// compare MD5 sum.
				status = pullRemoteTempFile(tempFile);
				String newMd5Sum = FileUtil.calculateMd5Sum(tempFile);
				if (!localFileMd5.equals(newMd5Sum)) {
					if (warnUser) {
						boolean canOverwrite = EclipseUtils.showQuestionDialog(
								DbDevicesNLS.DeviceDbNode_Remote_File_Modified_Title,
								NLS.bind(DbDevicesNLS.DeviceDbNode_Remote_File_Modified_Msg, getName()));
						if (!canOverwrite) {
							status = new Status(IStatus.CANCEL, DbDevicesPlugin.PLUGIN_ID,
									DbDevicesNLS.DeviceDbNode_User_Canceled_Overwrite);
						}
					} else {
						status = new Status(IStatus.ERROR, DbDevicesPlugin.PLUGIN_ID,
								DbDevicesNLS.DeviceDbNode_Md5Sum_Differs);
					}
				}

			} catch (IOException e) {
				status = new Status(IStatus.ERROR, DbDevicesPlugin.PLUGIN_ID,
						DbDevicesNLS.DeviceDbNode_Create_Temp_Local_Db_Failed, e);
			} finally {
				if (tempFile != null) {
					tempFile.delete();
				}
			}
		}

		return status != null ? status : Status.OK_STATUS;
	}

	public boolean remoteFileExists() {
		boolean remoteFileExists = false;
		try {
			remoteFileExists = DDMSUtils.remoteFileExists(serialNumber, remoteDbPath.toString());
		} catch (IOException e) {
			// Return false on error
		}
		return remoteFileExists;
	}

	/**
	 * @return the remoteDbPath
	 */
	public IPath getRemoteDbPath() {
		return remoteDbPath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.DbNode#clean()
	 */
	@Override
	public void cleanUp() {
		if (DDMSFacade.isDeviceOnline(serialNumber)) {
			super.cleanUp();
		} else {
			closeAssociatedEditors(true, forceCloseEditors);
			clear();
		}
	}
}
