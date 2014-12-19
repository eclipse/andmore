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

package org.eclipse.andmore.android.codeutils.db.utils;

import static org.eclipse.andmore.android.common.log.StudioLogger.info;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.db.actions.ContentProviderGeneratorByTable;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.CommonPlugin;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.andmore.android.db.deployment.DatabaseDeployer;
import org.eclipse.andmore.android.manifest.AndroidProjectManifestFile;
import org.eclipse.andmore.android.model.manifest.AndroidManifestFile;
import org.eclipse.andmore.android.model.manifest.dom.ManifestNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.datatools.connectivity.ConnectionProfileException;
import org.eclipse.datatools.connectivity.IConnection;
import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.IManagedConnection;
import org.eclipse.datatools.connectivity.ProfileManager;
import org.eclipse.datatools.connectivity.drivers.DriverManager;
import org.eclipse.datatools.connectivity.sqm.core.connection.ConnectionInfo;
import org.eclipse.datatools.connectivity.sqm.core.connection.DatabaseConnectionRegistry;
import org.eclipse.datatools.connectivity.sqm.internal.core.connection.ConnectionInfoImpl;
import org.eclipse.datatools.modelbase.sql.schema.Catalog;
import org.eclipse.datatools.modelbase.sql.schema.Database;
import org.eclipse.datatools.modelbase.sql.schema.Schema;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.datatools.sqltools.data.internal.ui.editor.TableDataEditor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

@SuppressWarnings("restriction")
public class DatabaseUtils {

	// Candidate folders that may contain .db resources
	public static final String ASSESTS_FOLDER = "assets"; //$NON-NLS-1$

	public static final String RAW_FOLDER = "raw"; //$NON-NLS-1$

	public static final String JDBC_SQLITE_PREFIX = "jdbc:sqlite:";

	public static final String URL_PROPERTY = "org.eclipse.datatools.connectivity.db.URL"; //$NON-NLS-1$

	public static final String DBNAME_PROPERTY = "org.eclipse.datatools.connectivity.db.databaseName"; //$NON-NLS-1$

	public static final String REMOTEPATH_PROPERTY = "org.eclipse.andmore.db.remotePathProperty"; //$NON-NLS-1$

	public static final String LOCALPATH_PROPERTY = "org.eclipse.andmore.db.localPathProperty"; //$NON-NLS-1$

	public static final String TYPE_PROPERTY = "org.eclipse.datatools.connectivity.db.TYPE";

	public static final String PROVIDER_ID = "org.eclipse.datatools.enablement.sqlite.connectionProfile"; //$NON-NLS-1$

	private static final String TEMPLATE_ID = "org.eclipse.datatools.enablement.sqlite.3_5_9.driver"; //$NON-NLS-1

	public static final String DB_FOLDER = "assets";

	public static Set<IConnectionProfile> profilesBeingDisconnected = new HashSet<IConnectionProfile>();

	/**
	 * Check if a file is a valid SQLite Database by analyzing the first 16
	 * bytes block, since every SQLite database begins with the byte sequence:
	 *
	 * 0x53 0x51 0x4c 0x69 0x74 0x65 0x20 0x66 0x6f 0x72 0x6d 0x61 0x74 0x20
	 * 0x33 0x00
	 * 
	 * Source: http://www.sqlite.org/fileformat.html#database_header
	 * 
	 * @param databaseFile
	 *            The file to be checked.
	 * @return True if {@code databaseFile} is a valid SQLite database file.
	 *         Otherwise, returns false.
	 * @throws IOException
	 *             Thrown if there were errors reading the file.
	 */
	public static boolean isValidSQLiteDatabase(File databaseFile) throws IOException {
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
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					StudioLogger.error("Could not close stream while checking if a file is a sqlite valid database"
							+ e.getMessage());
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
					StudioLogger.error("Could not close stream while checking if a file is a sqlite valid database"
							+ e.getMessage());
				}
			}
		}

		return result;
	}

	/**
	 * Retrieve a collections of .db resources declared inside an IProject in
	 * the workspace.
	 * 
	 * @param project
	 *            The project to be considered.
	 */
	public static Set<IFile> getDbFilesFromProject(IProject project) {
		// Result containing the .db resources
		HashSet<IFile> dbCollection = new HashSet<IFile>();

		// List of candidate folder to be inspected
		HashSet<IFolder> folderCollection = new HashSet<IFolder>();

		// First, retrieve and check if the folders likely to contain the .db
		// files exist
		folderCollection.add(project.getFolder(ASSESTS_FOLDER));
		folderCollection.add(project.getFolder(RAW_FOLDER));

		// Iterate through the folders and retrieve the .db IFiles
		for (IFolder folder : folderCollection) {
			if (folder.exists()) {
				// Get a list of files in the folder and try to find the .db
				// files
				try {
					for (IResource resource : folder.members()) {
						// Check if it's a file
						if (resource.getType() == IResource.FILE) {
							IFile file = (IFile) resource;

							// Check if the file is a valid database
							try {
								if (file.exists() & isValidSQLiteDatabase(file.getLocation().toFile())) {
									dbCollection.add(file);
								}
							} catch (IOException e) {
								StudioLogger.warn(DatabaseUtils.class,
										"It was not possible verify if the file is a valid SQLite database", e);
							}
						}
					}
				} catch (CoreException e) {
					// Log error
					StudioLogger.error(DatabaseUtils.class, "An error ocurred while looking for .db files.", e); //$NON-NLS-1$
				}
			}
		}
		return dbCollection;

	}

	/**
	 * @param database
	 *            The database that will have its table retrieved.
	 * @return All tables in the {@code database}.
	 * */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Set<Table> getTables(Database database) {
		HashSet<Table> tableSet = new HashSet<Table>();
		ListIterator<Catalog> catalogIter = database.getCatalogs().listIterator();
		while (catalogIter.hasNext()) {
			Catalog catalog = catalogIter.next();
			EList schemas = catalog.getSchemas();
			if ((schemas != null) && (schemas.size() > 0)) {
				ListIterator<Schema> schemasIter = schemas.listIterator();
				while (schemasIter.hasNext()) {
					Schema schema = schemasIter.next();
					EList tables = schema.getTables();
					if ((tables != null) && (tables.size() > 0)) {
						ListIterator<Table> tablesIter = tables.listIterator();
						while (tablesIter.hasNext()) {
							tableSet.add(tablesIter.next());
						}
					}
				}
			}
		}
		return tableSet;
	}

	/**
	 * Creates Database management classes
	 * 
	 * @param project
	 *            Target Project
	 * @param databaseName
	 *            Database name
	 * @param generateSQLOpenHelperClases
	 *            <code>true</code> for generating SQL Open Helper classes
	 * @param generateContentProviderClasses
	 *            <code>true</code> in case it is desired to create Content
	 *            Provider classes, <code>false</code> otherwise
	 * @param openHelperPackageName
	 *            Open Helper Package Name
	 * @param contentProvidersPackageName
	 *            Content provider Package Name
	 * @param sqlOpenHelperClassName
	 *            SQL Open Helper Class name
	 * @param overrideContentProviders
	 *            <code>true</code> in order to override the Content Providers
	 * @param generateDAO
	 *            <code>true</code> in case one wishes to create DAO classes,
	 *            <code>false</code> otherwise
	 * @param monitor
	 *            Monitor of the process
	 * 
	 * @throws AndroidException
	 *             Exception thrown when there are problems handling Android
	 *             files
	 * @throws CoreException
	 *             Exception thrown when there are problems handling Eclipse
	 * @throws IOException
	 *             Exception thrown when there are I/O problems with the
	 *             Database
	 * @throws ConnectionProfileException
	 *             Exception thrown when there are problems handling the
	 *             database connection
	 * @throws SQLException
	 *             Exception thrown when there are errors dealing with SQL
	 */
	public static void createDatabaseManagementClasses(IProject project, String databaseName,
			boolean generateSQLOpenHelperClases, boolean generateContentProviderClasses, String openHelperPackageName,
			String contentProvidersPackageName, String sqlOpenHelperClassName, boolean overrideContentProviders,
			boolean generateDAO, IProgressMonitor monitor, boolean showSuccessDialog) throws IOException,
			CoreException, AndroidException, ConnectionProfileException, SQLException {
		// get sub monitor
		SubMonitor subMonitor = SubMonitor.convert(monitor, 10);

		// begin
		subMonitor.beginTask(null, 10);

		// create parameters and copy database deployer class to the android
		// project
		Map<String, String> dbParameters = new HashMap<String, String>();

		AndroidManifestFile androidManifestFile = AndroidProjectManifestFile.getFromProject(project);

		ManifestNode manifestNode = androidManifestFile != null ? androidManifestFile.getManifestNode() : null;

		String appNamespace = manifestNode.getPackageName().toLowerCase();
		String packageName = ""; //$NON-NLS-1$
		if (openHelperPackageName != null) {
			// user-defined package for deployer
			packageName = openHelperPackageName;
		} else {
			// use default package for deployer
			packageName = appNamespace + ".deployer"; //$NON-NLS-1$
		}

		dbParameters.put(DatabaseDeployer.DATABASE_NAME, databaseName);
		dbParameters.put(DatabaseDeployer.APPLICATION_DATABASE_NAMESPACE, appNamespace);
		dbParameters.put(DatabaseDeployer.ANDROID_PROJECT_PACKAGE_NAME, packageName);
		dbParameters.put(DatabaseDeployer.DATABASE_HELPER_CLASS_NAME, sqlOpenHelperClassName);

		subMonitor.worked(1);

		if (generateSQLOpenHelperClases) {
			DatabaseDeployer.copyDataBaseDeployerClassToProject(project, dbParameters, subMonitor.newChild(2));
			StudioLogger.info("Finished creating Deployer classes"); //$NON-NLS-1$

			// Creates UDC log reporting that an OpenHelper class was created
			StudioLogger.collectUsageData(UsageDataConstants.WHAT_OPENHELPER, UsageDataConstants.KIND_OPENHELPER,
					"generated SQLOpenHelper class", //$NON-NLS-1$
					CodeUtilsActivator.PLUGIN_ID, CodeUtilsActivator.getDefault().getBundle().getVersion().toString());

		}

		// variables for connecting to the database
		IConnectionProfile profile = null;
		boolean isConnectionOK = false;
		boolean isConnectionStartedHere = false;
		IStatus status = null;
		try {
			// get local database profile
			profile = DatabaseUtils.getLocalDbProfile(project.getName(), databaseName);
			// continue in case there is a profile
			if (profile != null) {
				// assert driver
				DatabaseUtils.assertDriverExistsAtModel();
				// if the connection is established, set the flag of the
				// connection to true
				if (profile.getConnectionState() == IConnectionProfile.CONNECTED_STATE) {
					// set the flag
					isConnectionOK = true;
				}
				// the connection is not established, therefore connect
				else if (profile.getConnectionState() == IConnectionProfile.DISCONNECTED_STATE) {
					// execute connection and get the status
					status = profile.connectWithoutJob();
					// state that the connection was established here, thus the
					// disconnection is required
					isConnectionStartedHere = true;
					// set the connection flag to OK
					if ((status != null) && (status.getCode() == IStatus.OK)) {
						isConnectionOK = true;
					}
				}

				subMonitor.worked(4);

				// proceed in case the connection is established and OK
				if (isConnectionOK) {
					String appendMsg = CodeUtilsNLS.DATABASE_DEPLOY_SUCCESS_MESSAGE;
					boolean createMetadata = true;
					String query = "SELECT * FROM \"android_metadata\""; //$NON-NLS-1$
					ResultSet rs = null;

					try {
						rs = DatabaseUtils.executeSqliteQuery(profile, query);

						if (rs != null) {
							if (rs.next()) {
								createMetadata = false;
							}
						}
					} finally {
						if (rs != null) {
							rs.close();
						}
					}
					if (createMetadata) {
						// create the tables and insert data
						DatabaseUtils.executeSqliteStatement(profile,
								"CREATE TABLE IF NOT EXISTS \"android_metadata\" (\"locale\" TEXT DEFAULT 'en_US');"); //$NON-NLS-1$
						DatabaseUtils.executeSqliteStatement(profile,
								"insert into DEFAULT.ANDROID_METADATA (\"locale\") values('en_US');"); //$NON-NLS-1$
					}

					subMonitor.worked(2);

					if (generateContentProviderClasses) {
						Database database = DatabaseUtils.getDatabaseForProfile(profile);
						DatabaseUtils.createPersistenceClassesForDatabase(project, database, false, false,
								contentProvidersPackageName, packageName, sqlOpenHelperClassName,
								overrideContentProviders, generateDAO);

						subMonitor.worked(2);
					}

					subMonitor.worked(1);

					if (showSuccessDialog) {
						// show success message
						EclipseUtils.showInformationDialog(CodeUtilsNLS.DATABASE_DEPLOY_SUCCESS_MESSAGE_TITLE,
								appendMsg);
					}
				} else {
					// retrieve status error message
					String errorMessage = ""; //$NON-NLS-1$
					Throwable exception = null;
					if (status != null) {
						exception = status.getException();
						if (exception != null) {
							// get error message
							errorMessage = exception.getLocalizedMessage();
						}
					}
					// print and log database connection error message
					StudioLogger.error(DatabaseUtils.class, CodeUtilsNLS.DATABASE_DEPLOY_ERROR_CONNECTING_DATABASE,
							exception);
					EclipseUtils
							.showErrorDialog(
									CodeUtilsNLS.DATABASE_DEPLOY_CREATING_ANDROID_METADATA_TABLE,
									CodeUtilsNLS.DATABASE_DEPLOY_ERROR_CONNECTING_DATABASE
											+ ((errorMessage != null) && (errorMessage.length() > 0) ? "\r\n" + errorMessage : ""), status); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		} finally {
			// in case the connection was established, here, close it
			if (isConnectionStartedHere && (profile != null // there is a
															// profile
					) && (profile.getConnectionState() == IConnectionProfile.CONNECTED_STATE // the
																								// connection
																								// is
																								// up
																								// and
																								// running
					) && DatabaseUtils.prepareConnectionToClose(profile, false)) // prepare
																					// the
																					// connection
																					// to
																					// close
			{
				// close the connection
				profile.disconnect(null);
			}
		}
	}

	/**
	 * @return The connection profile to the given database. Create the
	 *         connection profile if it does not exist yet.
	 * */
	public static IConnectionProfile getLocalDbProfile(String projectName, String databaseName)
			throws ConnectionProfileException {
		ProfileManager pm = ProfileManager.getInstance();
		String profileName = projectName + "." + databaseName; //$NON-NLS-1$
		IConnectionProfile profile = pm.getProfileByName(profileName);
		if (profile == null) {
			String driverPath = CommonPlugin.getDefault().getDriverPath();
			Properties prop = DatabaseUtils.getBaseConnProperties(driverPath, databaseName);

			String providerId = DatabaseUtils.PROVIDER_ID;
			profile = pm.createProfile(profileName, "", providerId, prop); //$NON-NLS-1$

			String dbPath = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).getFolder(DB_FOLDER)
					.getFile(databaseName).getLocation().toString();
			prop.put(DatabaseUtils.URL_PROPERTY, DatabaseUtils.JDBC_SQLITE_PREFIX + dbPath);
			prop.put(DatabaseUtils.LOCALPATH_PROPERTY, dbPath);
			profile.setBaseProperties(prop);
		}
		return profile;
	}

	/**
	 * Return the following set of properties to be used in a connection
	 * profile:
	 * <ul>
	 * <li>org.eclipse.datatools.connectivity.db.vendor = "SQLITE"</li>
	 * <li>org.eclipse.datatools.connectivity.db.password = ""</li>
	 * <li>org.eclipse.datatools.connectivity.driverDefinitionID =
	 * "DriverDefn.org.eclipse.datatools.enablement.sqlite.3_5_9.driver." +
	 * {@link CommonPlugin#JDBC_DRIVER_INSTANCE_NAME}</li>
	 * <li>org.eclipse.datatools.connectivity.drivers.defnType =
	 * "org.eclipse.datatools.enablement.sqlite.3_5_9.driver"</li>
	 * <li>org.eclipse.datatools.connectivity.db.savePWD = "false"</li>
	 * <li>org.eclipse.datatools.connectivity.db.connectionProperties = ""</li>
	 * <li>org.eclipse.datatools.connectivity.db.version = "3.5.9"</li>
	 * <li>org.eclipse.datatools.connectivity.db.databaseName = {@code dbName}</li>
	 * <li>jarList = {@code driverPath}</li>
	 * <li>org.eclipse.datatools.connectivity.db.username = ""</li>
	 * <li>org.eclipse.datatools.connectivity.db.driverClass = "org.sqlite.JDBC"
	 * </li>
	 * </ul>
	 * */
	public static Properties getBaseConnProperties(String driverPath, String dbName) {
		Properties prop = new Properties();
		prop.put("org.eclipse.datatools.connectivity.db.vendor", "SQLITE"); //$NON-NLS-1$ //$NON-NLS-2$
		prop.put("org.eclipse.datatools.connectivity.db.password", ""); //$NON-NLS-1$ //$NON-NLS-2$
		prop.put("org.eclipse.datatools.connectivity.driverDefinitionID", //$NON-NLS-1$
				"DriverDefn.org.eclipse.datatools.enablement.sqlite.3_5_9.driver." //$NON-NLS-1$
						+ CommonPlugin.JDBC_DRIVER_INSTANCE_NAME);
		prop.put("org.eclipse.datatools.connectivity.drivers.defnType", //$NON-NLS-1$
				"org.eclipse.datatools.enablement.sqlite.3_5_9.driver"); //$NON-NLS-1$
		prop.put("org.eclipse.datatools.connectivity.db.savePWD", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		prop.put("org.eclipse.datatools.connectivity.db.connectionProperties", ""); //$NON-NLS-1$ //$NON-NLS-2$
		prop.put("org.eclipse.datatools.connectivity.db.version", "3.5.9"); //$NON-NLS-1$ //$NON-NLS-2$
		prop.put(DatabaseUtils.DBNAME_PROPERTY, dbName);
		prop.put("jarList", driverPath); //$NON-NLS-1$
		prop.put("org.eclipse.datatools.connectivity.db.username", ""); //$NON-NLS-1$ //$NON-NLS-2$
		prop.put("org.eclipse.datatools.connectivity.db.driverClass", "org.sqlite.JDBC"); //$NON-NLS-1$ //$NON-NLS-2$

		return prop;
	}

	/**
	 * Checks if a compatible JDBC driver is registered. If not, registers one.
	 */
	public static void assertDriverExistsAtModel() {
		DriverManager driverMan = DriverManager.getInstance();
		String allDrivers = driverMan.getFullJarList();
		String driverPath = CommonPlugin.getDefault().getDriverPath();
		if ((allDrivers == null) || (!allDrivers.contains(driverPath))) {
			String templateId = DatabaseUtils.TEMPLATE_ID;
			driverMan.createNewDriverInstance(templateId, CommonPlugin.JDBC_DRIVER_INSTANCE_NAME, driverPath);
			info("Created a MOTODEV Studio JDBC driver instance at Data Tools."); //$NON-NLS-1$
		}
	}

	/**
	 * Executes Sqlite query and return a {@link ResultSet}.
	 * 
	 * @param profile
	 *            The profile representing the database to be queried.
	 * @param query
	 *            SQL statement (select).
	 * @return SQL Results (columns and values).
	 */
	public static ResultSet executeSqliteQuery(IConnectionProfile profile, String query) {
		ResultSet resultSet = null;
		java.sql.Connection conn = DatabaseUtils.getJavaConnectionForProfile(profile);
		if (conn != null) {
			try {
				java.sql.Statement stmt = conn.createStatement();
				resultSet = stmt.executeQuery(query);
			} catch (java.sql.SQLException sqle) {
				StudioLogger.error(DatabaseUtils.class, "Problems executing query", sqle); //$NON-NLS-1$
			}
		}
		return resultSet;
	}

	/**
	 * @param profile
	 *            A datatools connection profile.
	 * @return A java sql connection to make create, insert, delete, update
	 *         calls to database.
	 */
	public static java.sql.Connection getJavaConnectionForProfile(IConnectionProfile profile) {
		IManagedConnection managedConnection = (profile).getManagedConnection("java.sql.Connection"); //$NON-NLS-1$
		if (managedConnection != null) {
			return (java.sql.Connection) managedConnection.getConnection().getRawConnection();
		}
		return null;
	}

	/**
	 * Executes Sqlite statements that does not return items (create, update,
	 * delete).
	 * 
	 * @param profile
	 *            A datatools connection profile.
	 * @param query
	 *            SQL statement (create, update, delete)
	 * @return Same as {@link Statement#executeUpdate(String)}.
	 */
	public static int executeSqliteStatement(IConnectionProfile profile, String query) {
		int count = 0;
		java.sql.Connection conn = getJavaConnectionForProfile(profile);
		if (conn != null) {
			try {
				java.sql.Statement stmt = conn.createStatement();
				count = stmt.executeUpdate(query);
			} catch (java.sql.SQLException sqle) {
				StudioLogger.error(DatabaseUtils.class, CodeUtilsNLS.DATABASE_ERROR_EXECUTING_STATEMENT, sqle);
			}
		}
		return count;
	}

	/**
	 * Search for database to get model (tables and colums definitions).
	 * WARNING: check return after proceeding, because if the database is not
	 * connected, it will be null.
	 * 
	 * @param profile
	 *            A datatools connection profile.
	 * @return A datatools database abstraction.
	 */
	public static Database getDatabaseForProfile(IConnectionProfile profile) {
		IManagedConnection managedConnection = (profile)
				.getManagedConnection("org.eclipse.datatools.connectivity.sqm.core.connection.ConnectionInfo"); //$NON-NLS-1$
		if (managedConnection != null) {
			try {
				IConnection conn = managedConnection.getConnection();
				if (conn != null) {
					ConnectionInfo connectionInfo = (ConnectionInfo) conn.getRawConnection();
					if (connectionInfo != null) {
						return connectionInfo.getSharedDatabase();
					}
				}
			} catch (Exception e) {
				StudioLogger.error(DatabaseUtils.class, "Problems executing query", e); //$NON-NLS-1$
			}
		}
		return null;
	}

	/**
	 * Generates Persistence classes for the tables on db
	 * 
	 * @param project
	 * @param database
	 * @param addCreateTableStatement
	 *            add create table statement on helper
	 * @param addDropTableStatementOnUpdate
	 *            add drop statement on helper
	 * @param persistencePackageName
	 *            package where to place the persistence classes on project
	 * @param databaseOpenHelperPackageName
	 *            Database Open Helper Package Name
	 * @param databaseOpenHelperClassName
	 *            Database open Helper Class Name
	 * @param overrideContentProviders
	 *            <code>true</code> in case one whishes to override the Content
	 *            Providers in case they exist
	 * @param generateDAO
	 *            false create Content Provider, true create DAO (DAO should NOT
	 *            be used now)
	 * @throws IOException
	 * @throws CoreException
	 * @throws AndroidException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void createPersistenceClassesForDatabase(IProject project, Database database,
			boolean addCreateTableStatement, boolean addDropTableStatementOnUpdate, String persistencePackageName,
			String databaseOpenHelperPackageName, String databaseOpenHelperClassName, boolean overrideContentProviders,
			boolean generateDAO) throws IOException, CoreException, AndroidException {
		ListIterator<Catalog> catalogIter = database.getCatalogs().listIterator();
		while (catalogIter.hasNext()) {
			Catalog catalog = catalogIter.next();
			EList schemas = catalog.getSchemas();
			if ((schemas != null) && (schemas.size() > 0)) {
				ListIterator<Schema> schemasIter = schemas.listIterator();
				while (schemasIter.hasNext()) {
					Schema schema = schemasIter.next();
					EList tables = schema.getTables();

					// this list will be created to control the classes names.
					// The name for each table will be put in CamelCase and
					// all underscores "_" will be removed. If two name are
					// equals, we will have to put a counter in the end o the
					// name.
					// This list will hold all the names that were created.
					List<String> tableNameForClasses = new ArrayList<String>();
					if ((tables != null) && (tables.size() > 0)) {
						ListIterator<Table> tablesIter = tables.listIterator();
						while (tablesIter.hasNext()) {
							Table table = tablesIter.next();
							StudioLogger.info("Start creating persistence classes for table " //$NON-NLS-1$
									+ table.getName());

							// generate Content Provider
							DatabaseUtils.generateContentProvider(project, table, database, addCreateTableStatement,
									addDropTableStatementOnUpdate, overrideContentProviders, persistencePackageName,
									databaseOpenHelperPackageName, databaseOpenHelperClassName, tableNameForClasses);
							StudioLogger.collectUsageData("generateContentProviderClasses", //$NON-NLS-1$
									"database", UsageDataConstants.DESCRIPTION_DEFAULT, //$NON-NLS-1$
									CodeUtilsActivator.PLUGIN_ID, CodeUtilsActivator.getDefault().getBundle()
											.getVersion().toString());

							StudioLogger.info("Finished creating persistence classes for table " //$NON-NLS-1$
									+ table.getName());
						}
					}
				}
			}

		}
	}

	/**
	 * Generates Content Provider class for the table
	 * 
	 * @param project
	 * @param table
	 * @param database
	 * @param addCreateTableStatement
	 * @param addDropTableStatementOnUpdate
	 * @param overrideContentProviders
	 *            <code>true</code> in case one wishes to override the Content
	 *            Providers
	 * @param persistencePackageName
	 * @param databaseOpenHelperPackageName
	 *            Database Open Helper package name
	 * @param databaseOpenHelperClassName
	 *            Database Open Helper class name
	 * @param beanName
	 * @throws IOException
	 * @throws CoreException
	 * @throws AndroidException
	 */
	public static void generateContentProvider(IProject project, Table table, Database database,
			boolean addCreateTableStatement, boolean addDropTableStatementOnUpdate, boolean overrideContentProviders,
			String persistencePackageName, String databaseOpenHelperPackageName, String databaseOpenHelperClassName,
			List<String> tableNameForClasses) throws IOException, CoreException, AndroidException {
		String dbName = database.getName();
		ContentProviderGeneratorByTable contentProviderGeneratorByTable = new ContentProviderGeneratorByTable(table,
				dbName);
		contentProviderGeneratorByTable.createContentProvider(project, addCreateTableStatement,
				addDropTableStatementOnUpdate, overrideContentProviders, persistencePackageName,
				databaseOpenHelperPackageName, databaseOpenHelperClassName, tableNameForClasses);
	}

	/**
	 * Prepare a connection profile to close means closing all its opened
	 * editor. If the device related to the connection profile will be
	 * disconnected, set {@code willDiscDevice} to true.
	 * 
	 * @return True if the connection profile can be safely closed. Otherwise,
	 *         returns false.
	 * */
	public static boolean prepareConnectionToClose(IConnectionProfile profile, boolean willDiscDevice) {
		final boolean[] success = new boolean[] { true };

		DatabaseUtils.profilesBeingDisconnected.add(profile);
		Set<IEditorPart> editorsSet = DatabaseUtils.getEditorsForProfile(profile);

		for (final IEditorPart editor : editorsSet) {
			final IWorkbenchPage page = EclipseUtils.getPageForEditor(editor);
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					page.bringToTop(editor);
					if (!page.closeEditor(editor, true)) {
						success[0] = false;
					}
				}
			});

			if (!success[0]) {
				break;
			}
		}

		if (!willDiscDevice) {
			DatabaseUtils.profilesBeingDisconnected.remove(profile);
		}

		return success[0];
	}

	/**
	 * Retrieves the open editors that is used to edit the given profile, if
	 * any.
	 * 
	 * @param profile
	 *            The profile that owns the requested editor.
	 * @return The open editors for the given profile, or <code>null</code> if
	 *         there is no opened editor for this profile.
	 */
	public static Set<IEditorPart> getEditorsForProfile(IConnectionProfile profile) {
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
					if (editorProfile == profile) {
						selectedEditors.add(e);
					}
				}
			}
		}

		return selectedEditors;
	}

	/**
	 * @param projectName
	 *            The project which contains the database.
	 * @param dbNameWithExtension
	 *            The complete database name, including its extension.
	 * @return A datatools abstraction for the database of the given project.
	 * */
	public static Database getDatabase(String projectName, String dbNameWithExtension)
			throws ConnectionProfileException, IOException {
		// variables for connecting to the database
		Database database = null;
		IConnectionProfile profile = null;
		boolean isConnectionOK = false;
		IStatus status = null;

		// get local database profile
		profile = getLocalDbProfile(projectName, dbNameWithExtension);
		// continue in case there is a profile
		if (profile != null) {
			// assert driver
			assertDriverExistsAtModel();
			// if the connection is established, set the flag of the connection
			// to true
			if (profile.getConnectionState() == IConnectionProfile.CONNECTED_STATE) {
				// set the flag
				isConnectionOK = true;
			}
			// the connection is not established, therefore connect
			else if (profile.getConnectionState() == IConnectionProfile.DISCONNECTED_STATE) {
				// execute connection and get the status
				status = profile.connectWithoutJob();
				// set the connection flag to OK
				if ((status != null) && (status.getCode() == IStatus.OK)) {
					isConnectionOK = true;
				}
			}

			// proceed in case the connection is established and OK
			if (isConnectionOK) {
				database = getDatabaseForProfile(profile);

			}
		}

		return database;
	}

	/**
	 * Given a Database file, provided by an {@link IPath}, in case it does not
	 * exist in the project�s asset�s directory, the file is to be copied to the
	 * mentioned directory. The project is represented by an {@link IProject}.
	 * 
	 * @param databaseFilePath
	 *            Database file path.
	 * @param targetProject
	 *            Project in which the database is to be copied, in
	 * @param monitor
	 *            Monitor for measuring the progress of the operation. case it
	 *            does not exist.
	 * 
	 * @throws FileNotFoundException
	 *             Exception thrown in case the entered path points to an
	 *             invalid path our file.
	 * @throws IllegalArgumentException
	 *             Exception thrown in case the targetProject is null.
	 * @throws IOException
	 *             Exception thrown when there are problems handling files in
	 *             the copying process.
	 * @throws CoreException
	 *             Exception thrown when there are problems creating the assets
	 *             folder.
	 */
	public static void copyDatabaseFileToAssetsFolder(IPath databaseFilePath, IProject targetProject,
			IProgressMonitor monitor) throws IOException, CoreException {
		// get sub monitor
		SubMonitor subMonitor = SubMonitor.convert(monitor, 10);

		// begin
		subMonitor.beginTask(null, 10);

		// validate the path
		if ((databaseFilePath == null) || !databaseFilePath.toFile().exists()) {
			throw new FileNotFoundException("The file entered by the databaseFilePath does not exists."); //$NON-NLS-1$
		}

		// validate the project
		if (targetProject == null) {
			throw new IllegalArgumentException("The argument targetProject cannot be null."); //$NON-NLS-1$
		}

		// get database file
		File databaseFile = databaseFilePath.toFile();

		// get assets folder
		IFolder assetsFolder = targetProject.getFolder(DatabaseUtils.ASSESTS_FOLDER);

		subMonitor.worked(3);

		if (assetsFolder.exists()) {
			// get the file matching the one entered on the Path
			IResource foundDatabaseFile = assetsFolder.findMember(databaseFile.getName());
			subMonitor.worked(5);
			// in case there is no file, or the resource is not a FILE, or the
			// found file does not actually exists, copy it
			if ((foundDatabaseFile == null) || (foundDatabaseFile.getType() != IResource.FILE)
					|| !foundDatabaseFile.exists()) {
				// copy the file
				FileUtil.copyFile(databaseFile, assetsFolder.getFile(databaseFile.getName()).getLocation().toFile());
				// refresh assets
				assetsFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			}
		} else {
			// create the assets folder
			assetsFolder.create(true, true, monitor);
			subMonitor.worked(5);
			// copy the file
			FileUtil.copyFile(databaseFile, assetsFolder.getFile(databaseFile.getName()).getLocation().toFile());
			// refresh assets
			assetsFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
		subMonitor.worked(10);
	}

	/**
	 * Formats the code using the Eclipse Java settings if possible, otherwise
	 * returns original document not indented.
	 * 
	 * @param destinationFile
	 *            Destination file.
	 * @param databaseHelperText
	 *            Text to generate.
	 * @param monitor
	 *            A progress monitor to be used to show operation status.
	 * @return Created document.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static IDocument formatCode(IFile destinationFile, String databaseHelperText, IProgressMonitor monitor) {
		IDocument document = new Document();
		File file = new File(destinationFile.getLocation().toOSString());

		try {
			document.set(databaseHelperText);

			try {
				IJavaProject p = JavaCore.create(destinationFile.getProject());
				Map mapOptions = p.getOptions(true);

				TextEdit textEdit = CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT
						| CodeFormatter.F_INCLUDE_COMMENTS, document.get(), 0, System.getProperty("line.separator"),
						mapOptions);

				if (textEdit != null) {
					textEdit.apply(document);
				}
			} catch (Exception ex) {
				// do nothing
			}

			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			try {
				out.write(document.get());
				out.flush();
			} finally {
				try {
					out.close();
				} catch (IOException e) {
					/* ignore */
				}
			}
			// the refresh is needed in order to avoid the user to have to press
			// F5
			destinationFile.getParent().refreshLocal(IResource.DEPTH_INFINITE, monitor);
		} catch (Exception e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_JavaClass_ErrorFormattingSourceCode, destinationFile.getName());
			StudioLogger.error(DatabaseUtils.class, errMsg, e);
		}
		return document;

	}

}
