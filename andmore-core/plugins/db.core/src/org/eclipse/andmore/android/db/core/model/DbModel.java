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
package org.eclipse.andmore.android.db.core.model;

import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.info;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.andmore.android.common.CommonPlugin;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.exception.MotodevDbException;
import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.model.Field.AutoIncrementType;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.datatools.connectivity.ConnectionProfileException;
import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.IManagedConnection;
import org.eclipse.datatools.connectivity.ProfileManager;
import org.eclipse.datatools.connectivity.drivers.DriverManager;
import org.eclipse.datatools.connectivity.sqm.core.connection.ConnectionInfo;
import org.eclipse.datatools.connectivity.sqm.core.connection.DatabaseConnectionRegistry;
import org.eclipse.datatools.connectivity.sqm.core.rte.ICatalogObject;
import org.eclipse.datatools.connectivity.sqm.internal.core.connection.ConnectionInfoImpl;
import org.eclipse.datatools.modelbase.sql.schema.Catalog;
import org.eclipse.datatools.modelbase.sql.schema.Database;
import org.eclipse.datatools.modelbase.sql.schema.SQLSchemaPackage;
import org.eclipse.datatools.modelbase.sql.schema.Schema;
import org.eclipse.datatools.modelbase.sql.tables.Column;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.datatools.sqltools.data.internal.ui.editor.TableDataEditor;
import org.eclipse.datatools.sqltools.internal.refresh.ICatalogObject2;
import org.eclipse.datatools.sqltools.result.OperationCommand;
import org.eclipse.datatools.sqltools.result.ResultsViewAPI;
import org.eclipse.emf.common.util.EList;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorPart;

/**
 * This class represents a datamodel. Is responsible to hold a
 * IConnectionProfile to a database and execute operations with this profile.
 */
public class DbModel {
	static final String JDBC_DRIVER_INSTANCE_NAME = CommonPlugin.JDBC_DRIVER_INSTANCE_NAME;

	public static final String DBNAME_PROPERTY = "org.eclipse.datatools.connectivity.db.databaseName"; //$NON-NLS-1$

	public static final String PROVIDER_ID = "org.eclipse.datatools.enablement.sqlite.connectionProfile"; //$NON-NLS-1$

	public static final String URL_PROPERTY = "org.eclipse.datatools.connectivity.db.URL"; //$NON-NLS-1$

	public static final String JDBC_SQLITE_PREFIX = "jdbc:sqlite:"; //$NON-NLS-1$

	public static final String LOCALPATH_PROPERTY = "org.eclipse.andmore.db.localPathProperty"; //$NON-NLS-1$

	private final IConnectionProfile connProfile;

	private final IPath dbPath;

	/**
	 * Creates a new DbModel Object based on the sqlite database at dbPath
	 * 
	 * @param dbPath
	 *            SQLite3 database file path
	 * @throws MotodevDbException
	 *             if dbPath does not contains a valid SQLite3 database file
	 */
	public DbModel(IPath dbPath) throws MotodevDbException {
		this(dbPath, false);
	}

	/**
	 * Creates a new DbModel Object creating a empty database file at dbPath if
	 * create is true. If create is false the behavior is the same as
	 * DbModel(Path dbPath)
	 * 
	 * @param dbPath
	 *            SQLite3 database file path
	 * @param create
	 *            flag indicating if the database should be created
	 * @throws MotodevDbException
	 *             if dbPath does not contains a valid SQLite3 database file
	 */
	public DbModel(IPath dbPath, boolean create) throws MotodevDbException {
		this(dbPath, create, false);
	}

	/**
	 * Creates a new DbModel Object creating a empty database file at dbPath if
	 * create is true. If create is false the behavior is the same as
	 * DbModel(Path dbPath)
	 * 
	 * @param dbPath
	 *            SQLite3 database file path
	 * @param create
	 *            flag indicating if the database should be created
	 * @throws MotodevDbException
	 *             if dbPath does not contains a valid SQLite3 database file
	 */
	public DbModel(IPath dbPath, boolean create, boolean overwrite) throws MotodevDbException {
		if (create) {
			File dbFile = dbPath.toFile();
			try {
				DbCoreActivator.getDefault().copyTemplateDbFile(dbFile, overwrite);
			} catch (IOException e) {
				throw new MotodevDbException(e);
			}
		}

		this.dbPath = dbPath;
		if (isValidSQLiteDatabase(dbPath.toFile())) {
			connProfile = getProfile(dbPath);
		} else {
			throw new MotodevDbException(NLS.bind(DbCoreNLS.DbModel_Not_Valid_Database, dbPath.toOSString()));
		}
	}

	/**
	 * Checks if a compatible JDBC driver is registered. If not, registers one
	 */
	public static void assertDriverExistsAtModel() {
		DriverManager driverMan = DriverManager.getInstance();
		String allDrivers = driverMan.getFullJarList();
		String driverPath = getDriverPath();
		if ((allDrivers == null) || (!allDrivers.contains(driverPath))) {
			String templateId = "org.eclipse.datatools.enablement.sqlite.3_5_9.driver"; //$NON-NLS-1$
			driverMan.createNewDriverInstance(templateId, JDBC_DRIVER_INSTANCE_NAME, driverPath);
			info("Created a MOTODEV Studio JDBC driver instance at Data Tools."); //$NON-NLS-1$
		}
	}

	/**
	 * If a compatible JDBC driver is registered, removes it
	 */
	public static void deleteDriverFromModel() {
		DriverManager driverMan = DriverManager.getInstance();
		String jarList = driverMan.getFullJarList();
		if ((jarList != null) && (jarList.contains(DbModel.JDBC_DRIVER_INSTANCE_NAME))) {
			driverMan.removeDriverInstance(DbModel.JDBC_DRIVER_INSTANCE_NAME);
			info("Removed the MOTODEV Studio JDBC driver instance from Data Tools."); //$NON-NLS-1$
		}
	}

	private IConnectionProfile getProfile(IPath dbPath) throws MotodevDbException {
		String fullPath = dbPath.toOSString();
		IConnectionProfile profile = null;
		profile = ProfileManager.getInstance().getProfileByFullPath(fullPath);

		if (profile == null) {

			Properties prop = getBaseConnProperties(getDriverPath(), dbPath.lastSegment(), dbPath.toOSString());

			try {
				profile = ProfileManager.getInstance().createProfile(fullPath, "", PROVIDER_ID, prop); //$NON-NLS-1$
				profile.setBaseProperties(prop);
			} catch (ConnectionProfileException e) {
				throw new MotodevDbException(NLS.bind("Unable to create Profile for db {0}", dbPath.toOSString()));
			}

		}

		return profile;
	}

	/**
	 * Retrieves the location of the driver
	 * 
	 * @return
	 */
	private static String getDriverPath() {
		String driverPath = null;

		driverPath = CommonPlugin.getDefault().getDriverPath();
		if (driverPath == null) {
			driverPath = "";
		}
		return driverPath;
	}

	// /*
	// * Retrieves the JDBC Sqlite3 driver file
	// */
	// private static File getDriver(String pathAtPlugin)
	// {
	// URL location =
	// DbCoreActivator.getDefault().getBundle().getEntry(pathAtPlugin);
	//
	//        debug("JDBC Driver Location:" + location + " JDBC Driver getBundle().getLocation():" //$NON-NLS-1$ //$NON-NLS-2$
	// + DbCoreActivator.getDefault().getBundle().getLocation());
	//
	// File file = null;
	// try
	// {
	// IPath p = new Path(FileLocator.toFileURL(location).getFile());
	//            debug("JDBC Driver Path:" + p.toOSString()); //$NON-NLS-1$
	// file = p.toFile();
	// }
	// catch (IOException e)
	// {
	//            error("Error while trying to locate jdbc driver into db plugin:" + e.getMessage()); //$NON-NLS-1$
	// }
	// return file;
	//
	// }

	public static Properties getBaseConnProperties(String driverPath, String dbName, String dbPath) {

		Properties prop = new Properties();
		prop.put("org.eclipse.datatools.connectivity.db.vendor", "SQLITE"); //$NON-NLS-1$ //$NON-NLS-2$
		prop.put("org.eclipse.datatools.connectivity.db.password", ""); //$NON-NLS-1$ //$NON-NLS-2$
		prop.put("org.eclipse.datatools.connectivity.driverDefinitionID", //$NON-NLS-1$
				"DriverDefn.org.eclipse.datatools.enablement.sqlite.3_5_9.driver." //$NON-NLS-1$
						+ JDBC_DRIVER_INSTANCE_NAME);
		prop.put("org.eclipse.datatools.connectivity.drivers.defnType", //$NON-NLS-1$
				"org.eclipse.datatools.enablement.sqlite.3_5_9.driver"); //$NON-NLS-1$
		prop.put("org.eclipse.datatools.connectivity.db.savePWD", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		prop.put("org.eclipse.datatools.connectivity.db.connectionProperties", ""); //$NON-NLS-1$ //$NON-NLS-2$
		prop.put("org.eclipse.datatools.connectivity.db.version", "3.5.9"); //$NON-NLS-1$ //$NON-NLS-2$
		prop.put(DBNAME_PROPERTY, dbName);
		prop.put("jarList", driverPath); //$NON-NLS-1$
		prop.put("org.eclipse.datatools.connectivity.db.username", ""); //$NON-NLS-1$ //$NON-NLS-2$
		prop.put("org.eclipse.datatools.connectivity.db.driverClass", "org.sqlite.JDBC"); //$NON-NLS-1$ //$NON-NLS-2$

		prop.put(URL_PROPERTY, JDBC_SQLITE_PREFIX + dbPath);
		prop.put(LOCALPATH_PROPERTY, dbPath);
		return prop;
	}

	public IStatus connect() {
		IPath dbPath = getDbPath();
		File file = dbPath.toFile();
		if (file.exists() && isValidSQLiteDatabase(file)) {
			return connProfile.connect();
		} else {
			return new Status(IStatus.ERROR, DbCoreActivator.PLUGIN_ID, NLS.bind(DbCoreNLS.Invalid_Db_Error, dbPath));
		}
	}

	public IStatus disconnect() {
		return connProfile.disconnect();
	}

	/**
	 * Create a table based on the infomration provided via param table, on this
	 * db model.
	 * 
	 * @param table
	 *            The {@link TableModel}
	 * @return IStatus.OK if the table was created successfully, IStatus.ERROR
	 *         otherwise. The status message explains the fail reason.
	 */
	public IStatus createTable(TableModel table) {
		StringBuilder strBuilder = new StringBuilder("CREATE TABLE "); //$NON-NLS-1$
		strBuilder.append(table.getName());
		strBuilder.append("("); //$NON-NLS-1$

		List<Field> fields = table.getFields();

		boolean firstField = true;
		for (Field field : fields) {
			if (firstField) {
				firstField = false;
			} else {
				strBuilder.append(", "); //$NON-NLS-1$
			}

			strBuilder.append(field.getName());
			strBuilder.append(" "); //$NON-NLS-1$
			strBuilder.append(field.getType().toString());
			if (field.isPrimaryKey()) {
				strBuilder.append(" PRIMARY KEY"); //$NON-NLS-1$
				if (field.getAutoIncrementType() != AutoIncrementType.NONE) {
					strBuilder.append(" "); //$NON-NLS-1$
					strBuilder.append(field.getAutoIncrementType().toString());
				}
			}
			if ((field.getDefaultValue() != null) && (!field.getDefaultValue().equals(""))) //$NON-NLS-1$
			{
				strBuilder.append(" default \'"); //$NON-NLS-1$
				strBuilder.append(field.getDefaultValue());
				strBuilder.append("\'"); //$NON-NLS-1$
			}
		}

		strBuilder.append(")"); //$NON-NLS-1$

		return executeSingleStatement(strBuilder.toString());
	}

	/**
	 * Delete a table from this dbModel with the name tableName
	 * 
	 * @param tableName
	 *            The name of the table to be deleted
	 * @return IStatus.OK if the table was deleted successfully, IStatus.ERROR
	 *         otherwise. The status message explains the fail reason.
	 */
	public IStatus deleteTable(String tableName) {
		return executeSingleStatement("DROP TABLE " + tableName); //$NON-NLS-1$
	}

	/**
	 * Retrieves the {@link Table} with name tableName from this dbModel
	 * 
	 * @param tableName
	 *            the name of the table within this dbModel to be retrieved.
	 * @return The {@link Table} object if existent, null otherwise.
	 */
	public Table getTable(String tableName) {
		List<Table> tables = getTables();
		Table table = null;

		for (Table t : tables) {
			if (t.getName().toUpperCase().equals(tableName.toUpperCase())) {
				table = t;
				break;
			}
		}

		return table;
	}

	/**
	 * Retrieves all tables from this dbModel.
	 * 
	 * @return all tables from found on this dbModel. All catalogs and schemas
	 *         are used during the search. An empty list is returned if there's
	 *         no table on this dbModel.
	 */
	@SuppressWarnings("unchecked")
	public List<Table> getTables() {
		List<Table> tables = new ArrayList<Table>();
		ConnectionInfo connectionInfo = getConnectionInfo();
		if (connectionInfo != null) {
			Database database = connectionInfo.getSharedDatabase();

			connectionInfo = DatabaseConnectionRegistry.getConnectionForDatabase(database);
			EList<Catalog> catalogs = database.getCatalogs();
			for (Catalog catalog : catalogs) {
				EList<Schema> schemas = catalog.getSchemas();
				schemas.addAll(database.getSchemas());
				for (Schema schema : schemas) {
					// Schema must be refreshed in order to retrieve the latest
					// information, instead of the cached info.
					if (schema instanceof ICatalogObject2) {
						String context = ((ICatalogObject2) schema).getRefreshContext(new Integer(
								SQLSchemaPackage.SCHEMA__TABLES));
						((ICatalogObject2) schema).refresh(context);
					} else {
						((ICatalogObject) schema).refresh();
					}

					EList<Table> schemaTables = schema.getTables();
					tables.addAll(schemaTables);
				}
			}
		}

		return tables;
	}

	/**
	 * Delete the db file represented by this dbModel.
	 * 
	 * @return Status.OK if operation is successful, Status.ERROR otherwise. The
	 *         status message explains the fail reason.
	 */
	public IStatus deleteDb() {
		IStatus status = Status.OK_STATUS;
		if (connProfile.getConnectionState() != IConnectionProfile.DISCONNECTED_STATE) {
			disconnect();
		}
		try {
			cleanModel();
			boolean deleteSuccesfull = dbPath.toFile().delete();
			if (!deleteSuccesfull) {
				status = new Status(IStatus.ERROR, DbCoreActivator.PLUGIN_ID, NLS.bind(
						DbCoreNLS.DbModel_Could_Not_Delete_DbFile, dbPath.toOSString()));
			}
		} catch (ConnectionProfileException e) {
			status = new Status(IStatus.ERROR, DbCoreActivator.PLUGIN_ID, NLS.bind(
					DbCoreNLS.DbModel_Could_Not_Disconnect_Profile, connProfile.getName()), e);
		}

		return status;
	}

	/**
	 * @throws ConnectionProfileException
	 */
	public void cleanModel() throws ConnectionProfileException {
		disconnect();
		ProfileManager.getInstance().deleteProfile(connProfile);
	}

	/**
	 * Verifies if the databaseFile is a valid SQLite3 file.
	 * 
	 * @param databaseFile
	 *            the SQLIte3 db file to be verified
	 * @return true if the file is a valid SQLite3 file or false otherwise.
	 */
	public static boolean isValidSQLiteDatabase(File databaseFile) {
		boolean result = true;

		final int BYTE_ARRAY_SIZE = 16;

		byte[] headerByteArray = new byte[16];
		headerByteArray[0] = 0x53;
		headerByteArray[1] = 0x51;
		headerByteArray[2] = 0x4c;
		headerByteArray[3] = 0x69;
		headerByteArray[4] = 0x74;
		headerByteArray[5] = 0x65;
		headerByteArray[6] = 0x20;
		headerByteArray[7] = 0x66;
		headerByteArray[8] = 0x6f;
		headerByteArray[9] = 0x72;
		headerByteArray[10] = 0x6d;
		headerByteArray[11] = 0x61;
		headerByteArray[12] = 0x74;
		headerByteArray[13] = 0x20;
		headerByteArray[14] = 0x33;
		headerByteArray[15] = 0x00;

		byte[] fileByteArray = new byte[BYTE_ARRAY_SIZE];
		FileInputStream fis = null;

		try {
			fis = new FileInputStream(databaseFile);
			fis.read(fileByteArray);
		} catch (Exception e) {
			result = false;
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					// Do nothing.
				}
			}
		}

		ByteArrayInputStream bais = null;
		try {
			bais = new ByteArrayInputStream(fileByteArray);
			for (int aux = 0; aux < BYTE_ARRAY_SIZE; aux++) {
				int myByte = bais.read();
				if (myByte != headerByteArray[aux]) {
					result = false;
				}
			}
		} finally {
			if (bais != null) {
				try {
					bais.close();
				} catch (IOException e) {
					// Do nothing.
				}
			}
		}
		return result;
	}

	private ConnectionInfo getConnectionInfo() {
		if (!isConnected()) {
			connProfile.connect();
		}
		IManagedConnection managedConnection = connProfile.getManagedConnection(ConnectionInfo.class.getName());
		ConnectionInfo connectionInfo = null;
		if (managedConnection != null) {
			connectionInfo = (ConnectionInfo) managedConnection.getConnection().getRawConnection();
		}
		return connectionInfo;
	}

	/**
	 * @return true if the profile is connected, false otherwise
	 */
	public boolean isConnected() {
		return connProfile.getConnectionState() == IConnectionProfile.CONNECTED_STATE;
	}

	private IStatus executeSingleStatement(String statement) {
		IStatus status = Status.OK_STATUS;

		Connection connection = getManagedSqlConnection();
		try {
			Statement sqlStatement = connection.createStatement();
			sqlStatement.execute(statement);
		} catch (Exception e) {
			status = new Status(IStatus.ERROR, DbCoreActivator.PLUGIN_ID, NLS.bind(
					DbCoreNLS.DbModel_Could_Not_Execute_Statement, statement), e);
		}

		return status;
	}

	private Connection getManagedSqlConnection() {
		if (!isConnected()) {
			connProfile.connect();
		}
		IManagedConnection managedConnection = connProfile.getManagedConnection(Connection.class.getName());
		Connection connection = (Connection) managedConnection.getConnection().getRawConnection();
		return connection;
	}

	/**
	 * @return the absolute path for the db file
	 */
	public IPath getDbPath() {
		return dbPath;
	}

	/**
	 * @return the name of the associated connection profile
	 */
	public String getProfileName() {
		return connProfile.getName();
	}

	/**
	 * @return
	 */
	public Set<IEditorPart> getAssociatedEditors() {
		Collection<IEditorPart> allEditors = EclipseUtils.getAllOpenedEditors();
		Set<IEditorPart> selectedEditors = new HashSet<IEditorPart>();
		for (IEditorPart e : allEditors) {
			if (e instanceof TableDataEditor) {
				TableDataEditor tde = (TableDataEditor) e;
				Table table = tde.getSqlTable();
				Catalog cat = table.getSchema().getCatalog();
				Database database = cat != null ? cat.getDatabase() : table.getSchema().getDatabase();
				ConnectionInfo connInfo = DatabaseConnectionRegistry.getConnectionForDatabase(database);
				if (connInfo != null) {
					IConnectionProfile editorProfile = ((ConnectionInfoImpl) connInfo).getConnectionProfile();
					if (editorProfile == connProfile) {
						selectedEditors.add(e);
					}
				}
			}
		}

		return selectedEditors;
	}

	private void sampleContents(Table table, Column column) {
		String tableName = table.getName();
		StringBuilder queryBuilder = new StringBuilder("select "); //$NON-NLS-1$
		if (column != null) {
			queryBuilder.append(column.getName());
		} else {
			queryBuilder.append("*"); //$NON-NLS-1$
		}
		queryBuilder.append(" from "); //$NON-NLS-1$
		queryBuilder.append(tableName);

		String queryString = queryBuilder.toString();
		executeSingleStatement(queryString);
		String cosummerName = null;
		String dbName = null;
		OperationCommand cmd = new OperationCommand(OperationCommand.ACTION_EXECUTE, queryString, cosummerName,
				connProfile.getName(), dbName);
		ResultsViewAPI resultsView = ResultsViewAPI.getInstance();
		resultsView.createNewInstance(cmd, null);
		resultsView.appendStatusMessage(cmd, DbCoreNLS.DbModel_Sampling_Contents_From + tableName);
		Connection managedSqlConnection = getManagedSqlConnection();
		try {
			Statement statement = managedSqlConnection.createStatement();
			ResultSet resultSet = statement.executeQuery(queryString);
			resultsView.appendResultSet(cmd, resultSet);
			resultsView.updateStatus(cmd, OperationCommand.STATUS_SUCCEEDED);
		} catch (SQLException e) {
			resultsView.appendThrowable(cmd, e);
			resultsView.updateStatus(cmd, OperationCommand.STATUS_FAILED);
		}
	}

	/**
	 * @param table
	 */
	public void sampleContents(Table table) {
		sampleContents(table, null);
	}

	/**
	 * @param column
	 */
	public void sampleContents(Column column) {
		sampleContents(column.getTable(), column);
	}

	/**
     * 
     */
	public static void cleanPreviousProfiles() {
		ProfileManager profileManager = ProfileManager.getInstance();
		IConnectionProfile[] profiles = profileManager.getProfiles();
		for (IConnectionProfile profile : profiles) {
			try {
				profileManager.deleteProfile(profile);
			} catch (ConnectionProfileException e) {
				error(DbModel.class, "Could not delete all profiles", e); //$NON-NLS-1$
			}
		}
	}

}
