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

import org.eclipse.datatools.sqltools.result.OperationCommand;
import org.eclipse.datatools.sqltools.result.core.IResultManagerListener;
import org.eclipse.datatools.sqltools.result.model.IResultInstance;
import org.eclipse.datatools.sqltools.result.model.ResultItem;

public abstract class AbstractDbResultManagerAdapter implements IResultManagerListener {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.sqltools.result.core.IResultManagerListener#
	 * resultInstanceCreated
	 * (org.eclipse.datatools.sqltools.result.model.IResultInstance)
	 */
	@Override
	public void resultInstanceCreated(IResultInstance instance) {
		// Do nothing.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.sqltools.result.core.IResultManagerListener#
	 * resultInstanceRemoved
	 * (org.eclipse.datatools.sqltools.result.model.IResultInstance)
	 */
	@Override
	public void resultInstanceRemoved(IResultInstance instance) {
		// Do nothing.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.sqltools.result.core.IResultManagerListener#
	 * resultInstancesRemoved
	 * (org.eclipse.datatools.sqltools.result.model.IResultInstance[])
	 */
	@Override
	public void resultInstancesRemoved(IResultInstance[] instances) {
		// Do nothing.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.sqltools.result.core.IResultManagerListener#
	 * resultInstanceAppended
	 * (org.eclipse.datatools.sqltools.result.model.IResultInstance,
	 * org.eclipse.datatools.sqltools.result.model.ResultItem, int)
	 */
	@Override
	public void resultInstanceAppended(IResultInstance instance, ResultItem result, int index) {
		// Do nothing.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.sqltools.result.core.IResultManagerListener#
	 * allResultInstancesRemoved()
	 */
	@Override
	public void allResultInstancesRemoved() {
		// Do nothing.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.sqltools.result.core.IResultManagerListener#
	 * resultInstanceStatusUpdated
	 * (org.eclipse.datatools.sqltools.result.model.IResultInstance)
	 */
	@Override
	public void resultInstanceStatusUpdated(IResultInstance instance) {
		if (instance.getStatus() == OperationCommand.STATUS_SUCCEEDED) {
			OperationCommand cmd = instance.getOperationCommand();
			String profilename = cmd.getProfileName();
			String sqlStatement = cmd.getDisplayString();
			statementExecuted(profilename, sqlStatement);
		}
	}

	/**
	 * This method will be called everytime a statement is successfully executed
	 * 
	 * @param profilename
	 *            the name of the connection profile
	 * @param sqlStatement
	 *            the statement executed
	 */
	public abstract void statementExecuted(String profilename, String sqlStatement);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.sqltools.result.core.IResultManagerListener#
	 * resultInstanceReset
	 * (org.eclipse.datatools.sqltools.result.model.IResultInstance)
	 */
	@Override
	public void resultInstanceReset(IResultInstance instance) {
		// Do nothing.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.sqltools.result.core.IResultManagerListener#
	 * parametersShow
	 * (org.eclipse.datatools.sqltools.result.model.IResultInstance,
	 * java.util.List)
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public void parametersShow(IResultInstance instance, List params) {
		// Do nothing.
	}

}
