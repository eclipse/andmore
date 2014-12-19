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

package org.eclipse.andmore.gltrace.state.transforms;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.gltrace.state.GLCompositeProperty;
import org.eclipse.andmore.gltrace.state.GLListProperty;
import org.eclipse.andmore.gltrace.state.GLSparseArrayProperty;
import org.eclipse.andmore.gltrace.state.GLStateType;
import org.eclipse.andmore.gltrace.state.IGLProperty;

/**
 * GLPropertyAccessor's can be used to extract a certain property from the
 * provided OpenGL State hierarchy.
 */
public class GLPropertyAccessor implements IGLPropertyAccessor {
	private final int mContextId;
	private final List<GLPropertyExtractor> mExtractors;

	private GLPropertyAccessor(int contextId, List<GLPropertyExtractor> extractors) {
		mContextId = contextId;
		mExtractors = extractors;
	}

	@Override
	public IGLProperty getProperty(IGLProperty state) {
		IGLProperty root = ((GLListProperty) state).get(mContextId);

		for (GLPropertyExtractor e : mExtractors) {
			IGLProperty successor = e.getProperty(root);
			if (successor == null) {
				root = null;
				break;
			}
			root = successor;
		}

		return root;
	}

	/**
	 * Factory method used to construct a {@link GLPropertyAccessor}.
	 * 
	 * @param contextId
	 *            id of affected context
	 * @param accessors
	 *            list of accessor's to be used to navigate the property
	 *            hierarchy. The accessors are either Integers or
	 *            {@link GLStateType} objects. Integers are assumed to be
	 *            indexes in a {@link GLListProperty} or
	 *            {@link GLSparseArrayProperty}, and the GLStateType enum
	 *            objects are used to query {@link GLCompositeProperty}'s.
	 */
	public static IGLPropertyAccessor makeAccessor(int contextId, Object... accessors) {
		List<GLPropertyExtractor> extractors = new ArrayList<GLPropertyExtractor>();

		for (Object accessor : accessors) {
			if (accessor instanceof GLStateType) {
				extractors.add(new GLNamePropertyExtractor((GLStateType) accessor));
			} else if (accessor instanceof Integer) {
				extractors.add(new GLIndexPropertyExtractor((Integer) accessor));
			} else {
				throw new IllegalArgumentException("Unknown property (" + accessor
						+ ") used to access members of IGLProperty");
			}
		}

		return new GLPropertyAccessor(contextId, extractors);
	}

	private interface GLPropertyExtractor {
		IGLProperty getProperty(IGLProperty p);
	}

	/** Extract properties by name. */
	private static class GLNamePropertyExtractor implements GLPropertyExtractor {
		private final GLStateType mType;

		public GLNamePropertyExtractor(GLStateType type) {
			mType = type;
		}

		@Override
		public IGLProperty getProperty(IGLProperty p) {
			if (p instanceof GLCompositeProperty) {
				return ((GLCompositeProperty) p).getProperty(mType);
			}

			return null;
		}
	}

	/** Extract properties by index. */
	private static class GLIndexPropertyExtractor implements GLPropertyExtractor {
		private final int mIndex;

		public GLIndexPropertyExtractor(int index) {
			mIndex = index;
		}

		@Override
		public IGLProperty getProperty(IGLProperty p) {
			if (p instanceof GLListProperty && mIndex >= 0) {
				return ((GLListProperty) p).get(mIndex);
			}
			if (p instanceof GLSparseArrayProperty) {
				return ((GLSparseArrayProperty) p).getProperty(mIndex);
			}
			return null;
		}
	}

	@Override
	public String getPath() {
		StringBuilder sb = new StringBuilder(mExtractors.size() * 10);
		for (GLPropertyExtractor e : mExtractors) {
			if (e instanceof GLNamePropertyExtractor) {
				sb.append(((GLNamePropertyExtractor) e).mType);
			} else {
				sb.append(((GLIndexPropertyExtractor) e).mIndex);
			}
			sb.append('/');
		}

		return sb.toString();
	}
}
