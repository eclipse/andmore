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
package org.eclipse.andmore.android.db.core.junit;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.db.core.exception.MotodevDbException;
import org.eclipse.andmore.android.db.core.model.DbModel;
import org.eclipse.andmore.android.db.core.model.Field;
import org.eclipse.andmore.android.db.core.model.TableModel;
import org.eclipse.andmore.android.db.core.model.Field.AutoIncrementType;
import org.eclipse.andmore.android.db.core.model.Field.DataType;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.junit.Test;

public class DbModelTest {

	@Test
	public void testCreateTable() {

		Path path = new Path("/Users/danielbfranco/temp/ranking.db");
		DbModel model = null;
		try {
			model = new DbModel(path);
		} catch (MotodevDbException e) {
			e.printStackTrace();
		}
		IStatus s = model.connect();
		assertTrue(s.getCode() == IStatus.OK);

		Field idField = new Field("_id", DataType.INTEGER, true, AutoIncrementType.ASCENDING, null);
		Field textField = new Field("_text", DataType.TEXT, false, AutoIncrementType.NONE, "DanDan");
		Field numField = new Field("_num", DataType.INTEGER, false, AutoIncrementType.NONE, "5");

		List<Field> fields = new ArrayList<Field>(2);
		fields.add(idField);
		fields.add(textField);
		fields.add(numField);

		TableModel table = new TableModel("mablinhos3", fields);

		s = model.createTable(table);
		assertTrue(s.getCode() == IStatus.OK);
	}
}
