/*
 * Copyright (C) 2011 The Android Open Source Project
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

package org.eclipse.andmore.gltrace.format;

import org.eclipse.andmore.gltrace.GLProtoBuf.GLMessage.DataType.Type;

public class GLDataTypeSpec {
	private final String mCType;
	private final Type mType;
	private final String mName;
	private final boolean mIsPointer;

	public GLDataTypeSpec(String type, String name) {
		mCType = type;
		mName = name;

		mType = getDataType(type);
		mIsPointer = type.contains("*"); //$NON-NLS-1$
	}

	private Type getDataType(String type) {
		type = type.toLowerCase();

		// We use type.contains() rather than type.equals since we are matching
		// against
		// the type name along with qualifiers. e.g. "void", "GLvoid" and
		// "void*" should
		// all be assigned the same type.
		if (type.contains("boolean")) { //$NON-NLS-1$
			return Type.BOOL;
		} else if (type.contains("enum")) { //$NON-NLS-1$
			return Type.ENUM;
		} else if (type.contains("float") || type.contains("clampf")) { //$NON-NLS-1$ //$NON-NLS-2$
			return Type.FLOAT;
		} else if (type.contains("void")) { //$NON-NLS-1$
			return Type.VOID;
		} else if (type.contains("char")) { //$NON-NLS-1$
			return Type.CHAR;
		} else {
			// Matches all of the following types:
			// glclampx, gluint, glint, glshort, glsizei, glfixed,
			// glsizeiptr, glintptr, glbitfield, glfixed, glubyte.
			// We might do custom formatting for these types in the future.
			return Type.INT;
		}
	}

	public Type getDataType() {
		return mType;
	}

	public String getCType() {
		return mCType;
	}

	public String getArgName() {
		return mName;
	}

	public boolean isPointer() {
		return mIsPointer;
	}
}
