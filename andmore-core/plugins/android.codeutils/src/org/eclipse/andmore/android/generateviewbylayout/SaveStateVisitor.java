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
package org.eclipse.andmore.android.generateviewbylayout;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.generatecode.JDTUtils;
import org.eclipse.andmore.android.generateviewbylayout.codegenerators.SaveStateCodeGenerator;
import org.eclipse.andmore.android.generateviewbylayout.model.LayoutNode.ViewProperties;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * Visitor responsible to identify already saved or restored views inside a View
 * (generally Activity / Fragment)
 */
public class SaveStateVisitor extends ASTVisitor {
	private final Set<String> viewIds = new HashSet<String>();

	public Set<String> getViewIds() {
		return viewIds;
	}

	@Override
	public boolean visit(MethodInvocation node) {
		boolean handled = false;
		int i = 0;
		while (!handled && (i < SaveStateCodeGenerator.saveStateNodeTypes.length)) {
			if ((node.getName().toString().equals(SaveStateCodeGenerator.saveStateNodeTypes[i]
					.getProperty(ViewProperties.ViewStateGetMethod)))) {
				identifySavedView(node);
				handled = true;
			} else if (node
					.getName()
					.toString()
					.equals(SaveStateCodeGenerator.saveStateNodeTypes[i].getProperty(ViewProperties.ViewStateSetMethod))) {
				identifyRestoredView(node);
				handled = true;
			}
			i++;
		}

		return super.visit(node);
	}

	/**
	 * Restored views are in form
	 * <variable>.<viewSetMethod>(preferences.<preferenceGetMethod>("property",
	 * <defaultValue>));
	 * 
	 * @param node
	 */
	private void identifyRestoredView(MethodInvocation node) {
		Expression expression = node.getExpression();

		if (expression instanceof SimpleName) {
			ITypeBinding binding = ((SimpleName) expression).resolveTypeBinding();
			IJavaElement javaElement = binding.getJavaElement();
			if (javaElement != null) {
				try {
					IType type = (IType) javaElement.getAdapter(IType.class);
					if (JDTUtils.isSubclass(type, "android.view.View")) {
						viewIds.add(((SimpleName) expression).getFullyQualifiedName());
					}
				} catch (JavaModelException e) {
					StudioLogger.warn(CodeUtilsActivator.PLUGIN_ID, "Unable to identify if " + binding.getName()
							+ " is a subclass of android.view.View", e);
				}
			}
		}
	}

	/**
	 * Saved views are in form editor.<propertySetMethod>("property",
	 * <variable>.<viewGetMethod>);
	 * 
	 * @param node
	 */
	private void identifySavedView(MethodInvocation node) {
		Expression expression = node.getExpression();

		if (expression instanceof SimpleName) {
			ITypeBinding binding = ((SimpleName) expression).resolveTypeBinding();
			IJavaElement javaElement = binding.getJavaElement();
			if (javaElement != null) {
				try {
					IType type = (IType) javaElement.getAdapter(IType.class);
					if (JDTUtils.isSubclass(type, "android.view.View")) {
						viewIds.add(((SimpleName) expression).getFullyQualifiedName());
					}
				} catch (JavaModelException e) {
					StudioLogger.warn(CodeUtilsActivator.PLUGIN_ID, "Unable to identify if " + binding.getName()
							+ " is a subclass of android.view.View", e);
				}
			}
		}
	}
}
