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

package org.eclipse.andmore.gltrace.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.eclipse.andmore.gltrace.GLProtoBuf.GLMessage.Function;
import org.eclipse.andmore.gltrace.model.GLCall;
import org.eclipse.andmore.gltrace.model.GLTrace;

public class GLCallGroups {
	/**
	 * A {@link GLCallNode} is a simple wrapper around a {@link GLCall} that
	 * adds the notion of hierarchy.
	 */
	public interface GLCallNode {
		/** Does this call have child nodes? */
		boolean hasChildren();

		/** Returns a list of child nodes of this call. */
		List<GLCallNode> getChildren();

		/** Returns the {@link GLCall} that is wrapped by this node. */
		GLCall getCall();

		/**
		 * Returns the parent of this node, the parent is null if this is a top
		 * level node
		 */
		GLCallNode getParent();

		/** Set the parent node. */
		void setParent(GLCallNode parent);
	}

	private static class GLTreeNode implements GLCallNode {
		private final GLCall mCall;
		private GLCallNode mParent;
		private List<GLCallNode> mGLCallNodes;

		public GLTreeNode(GLCall call) {
			mCall = call;
			mGLCallNodes = new ArrayList<GLCallNode>();
		}

		@Override
		public boolean hasChildren() {
			return true;
		}

		@Override
		public GLCallNode getParent() {
			return mParent;
		}

		@Override
		public void setParent(GLCallNode parent) {
			mParent = parent;
		}

		@Override
		public List<GLCallNode> getChildren() {
			return mGLCallNodes;
		}

		public void addChild(GLCallNode n) {
			mGLCallNodes.add(n);
			n.setParent(this);
		}

		@Override
		public GLCall getCall() {
			return mCall;
		}
	}

	private static class GLLeafNode implements GLCallNode {
		private final GLCall mCall;
		private GLCallNode mParent;

		public GLLeafNode(GLCall call) {
			mCall = call;
		}

		@Override
		public boolean hasChildren() {
			return false;
		}

		@Override
		public List<GLCallNode> getChildren() {
			return null;
		}

		@Override
		public GLCallNode getParent() {
			return mParent;
		}

		@Override
		public void setParent(GLCallNode parent) {
			mParent = parent;
		}

		@Override
		public GLCall getCall() {
			return mCall;
		}
	}

	/**
	 * Impose a hierarchy on a list of {@link GLCall}'s based on the presence of
	 * {@link Function#glPushGroupMarkerEXT} and
	 * {@link Function#glPopGroupMarkerEXT} calls. Such a hierarchy is possible
	 * only if calls from a single context are considered.
	 * 
	 * @param trace
	 *            trace to look at
	 * @param start
	 *            starting call index
	 * @param end
	 *            ending call index
	 * @param contextToGroup
	 *            context from which calls should be grouped. If no such context
	 *            is present, then all calls in the given range will be returned
	 *            back as a flat list.
	 * @return a tree structured list of {@link GLCallNode} objects
	 */
	public static List<GLCallNode> constructCallHierarchy(GLTrace trace, int start, int end, int contextToGroup) {
		if (trace == null) {
			return Collections.emptyList();
		}

		if (contextToGroup < 0 || contextToGroup > trace.getContexts().size()) {
			return flatHierarchy(trace, start, end);
		}

		List<GLCall> calls = trace.getGLCalls();

		Stack<GLTreeNode> hierarchyStack = new Stack<GLTreeNode>();
		List<GLCallNode> items = new ArrayList<GLCallNode>();

		for (int i = start; i < end; i++) {
			GLCall c = calls.get(i);
			if (c.getContextId() != contextToGroup) {
				// skip this call if it is not part of the context we need to
				// display
				continue;
			}

			if (c.getFunction() == Function.glPushGroupMarkerEXT) {
				GLTreeNode group = new GLTreeNode(c);
				if (hierarchyStack.size() > 0) {
					hierarchyStack.peek().addChild(group);
				} else {
					items.add(group);
				}
				hierarchyStack.push(group);
			} else if (c.getFunction() == Function.glPopGroupMarkerEXT) {
				if (hierarchyStack.size() > 0) {
					hierarchyStack.pop();
				} else {
					// FIXME: If we are attempting to pop from an empty stack,
					// that implies that a push marker was seen in a prior frame
					// (in a call before @start). In such a case, we simply
					// continue
					// adding further calls to the root of the hierarchy rather
					// than
					// searching backwards in the call list for the
					// corresponding
					// push markers.
					items.add(new GLLeafNode(c));
				}
			} else {
				GLLeafNode leaf = new GLLeafNode(c);
				if (hierarchyStack.size() > 0) {
					hierarchyStack.peek().addChild(leaf);
				} else {
					items.add(leaf);
				}
			}
		}

		return items;
	}

	private static List<GLCallNode> flatHierarchy(GLTrace trace, int start, int end) {
		List<GLCallNode> items = new ArrayList<GLCallNode>();

		List<GLCall> calls = trace.getGLCalls();
		for (int i = start; i < end; i++) {
			items.add(new GLLeafNode(calls.get(i)));
		}

		return items;
	}
}
