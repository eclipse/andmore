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

import java.util.List;

import org.eclipse.andmore.android.db.core.model.TableModel;
import org.eclipse.andmore.android.db.core.ui.action.ITableCreatorNode;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.datatools.modelbase.sql.tables.Table;

/**
 * This interface is meant to be implemented by databse nodes. it defines
 * standard methods for manipulating a database.
 */
public interface IDbNode extends ITreeNode, ITableCreatorNode {

	/**
	 * Connect to the database. Implementors must connect to the database, load
	 * the tables and notify the UI.
	 * 
	 * @return
	 */
	IStatus connect();

	/**
	 * Connect to the database. Implementors must disconnect from the database,
	 * free this node children and notify the UI.
	 * 
	 * @return
	 */
	IStatus disconnect();

	/**
	 * Checks if db is connected
	 * 
	 * @return true if db is connected, false otherwise
	 */
	boolean isConnected();

	/**
	 * @param tables
	 * @return
	 */
	IStatus createTables(List<TableModel> tables);

	/**
	 * Create a table in this db. Implementors must create the table and add a
	 * TableNode representing the new table as a new child. UI must be notified
	 * after child is added.
	 * 
	 * @param table
	 *            The new table representation
	 * @return
	 */
	@Override
	IStatus createTable(TableModel table);

	/**
	 * Delete a table from this db. The table node representing this table must
	 * also be removed from this dbNode children. UI must be notified.
	 * 
	 * @param tableNode
	 *            The node table to be removed.
	 * @return
	 */
	IStatus deleteTable(ITableNode tableNode);

	/**
	 * Get all tables from this db.
	 * 
	 * @return A {@link List<Table>} with all tables from this database
	 */
	@Override
	List<Table> getTables();

	/**
	 * Get a table from this db with the specified name.
	 * 
	 * @return A {@link List<Table>} with all tables from this database
	 */
	Table getTable(String tableName);

	/**
	 * Delete the database represented by this node.
	 * 
	 * @return Status.OK if operation is successful, Status.ERROR otherwise. The
	 *         status message explains the fail reason.
	 */
	IStatus deleteDb();
}