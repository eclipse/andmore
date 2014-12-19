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

package org.eclipse.andmore.android.db.core.command;

import org.eclipse.andmore.android.db.core.event.DatabaseModelEventManager;
import org.eclipse.andmore.android.db.core.event.DatabaseModelEvent.EVENT_TYPE;
import org.eclipse.andmore.android.db.core.ui.IDbNode;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

/**
 * This class implements the command to connect to a database using an IDbNode
 * object.
 * */
public class DbConnectHandler extends AbstractHandler implements IHandler {

	private IDbNode dbNode;

	public DbConnectHandler() {

	}

	public DbConnectHandler(IDbNode dbNode) {
		this.dbNode = dbNode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.IHandler#execute(ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		DatabaseModelEventManager.getInstance().fireEvent(dbNode, EVENT_TYPE.EXPAND);
		return null;
	}
}
