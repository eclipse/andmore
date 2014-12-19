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

package org.eclipse.andmore.android.generatecode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.generatemenucode.model.codegenerators.CodeGeneratorBasedOnMenuVisitor;
import org.eclipse.andmore.android.generatemenucode.model.codegenerators.CodeGeneratorDataBasedOnMenu;
import org.eclipse.andmore.android.generateviewbylayout.GenerateCodeBasedOnLayoutVisitor;
import org.eclipse.andmore.android.generateviewbylayout.model.CodeGeneratorDataBasedOnLayout;
import org.eclipse.andmore.android.generateviewbylayout.model.JavaLayoutData;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.osgi.util.NLS;

/**
 * Class that implements convenient methods to abstract and handle JDT related
 * operations. This class is not meant to be instantiated.
 * */
public class JDTUtils {

	private JDTUtils() {
		// does nothing.
		// prevents other objects to instantiate this class.
	}

	/**
	 * Retrieves the name of the inflated menu inside Activity or Fragment
	 * {@code type}.
	 * 
	 * @param project
	 *            The android project that contains the activity or fragment.
	 * @param compUnit
	 *            The compilation unit of the activity or fragment.
	 * @return The name of the inflated menu inside {@code compUnit}.
	 * */
	public static String getInflatedMenuFileName(IProject project, ICompilationUnit compUnit) {
		// check if type is either activity or fragment
		// check if type inflates a menu on OnCreateOptionsMenu
		// return the name of the inflated menu with ".xml" appended

		CodeGeneratorBasedOnMenuVisitor visitor = new CodeGeneratorBasedOnMenuVisitor();
		CompilationUnit cpAstNode = parse(compUnit);
		StudioLogger.info("Trying to visit code for class: " + compUnit.getResource().getName()); //$NON-NLS-1$
		try {
			cpAstNode.accept(visitor);
		} catch (IllegalArgumentException illegalArgumentException) {
			StudioLogger.error("Error while trying to visit code to get an inflated menu:"
					+ compUnit.getResource().getName());
		}
		return visitor.getInflatedMenuName();
	}

	/**
	 * Retrieves a list with the available android activities inside
	 * {@code project}.
	 * 
	 * @param project
	 *            The android project to retrieve the activities.
	 * @param monitor
	 *            A progress monitor to be used to show operation status.
	 * */
	public static List<IType> getAvailableActivities(IProject project, IProgressMonitor monitor)
			throws JavaModelException {
		return getAvailableSubclasses(project, "android.app.Activity", monitor); //$NON-NLS-1$
	}

	/**
	 * Retrieves the list of subclasses of a given {@code superclass} inside an
	 * {@code androidProject}.
	 * 
	 * @throws JavaModelException
	 *             If there are problems parsing java files.
	 * */
	public static List<IType> getAvailableSubclasses(IProject androidProject, String superclass,
			IProgressMonitor monitor) throws JavaModelException {
		List<IType> availableActivities = new ArrayList<IType>();

		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask("Resolving available types", 1000); //$NON-NLS-1$

		IJavaProject javaProject = JavaCore.create(androidProject);
		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(androidProject.findMember("src")); //$NON-NLS-1$
		ArrayList<IPackageFragment> fragments = new ArrayList<IPackageFragment>();
		for (IJavaElement element : root.getChildren()) {
			fragments.add((IPackageFragment) element);
		}

		subMonitor.worked(100);

		if (fragments.size() == 0) {
			subMonitor.worked(900);
		}

		for (IPackageFragment fragment : fragments) {
			ICompilationUnit[] units = fragment.getCompilationUnits();
			if (units.length == 0) {
				subMonitor.worked(900 / fragments.size());
			}
			for (int j = 0; j < units.length; j++) {
				ICompilationUnit unit = units[j];
				IType[] availableTypes = unit.getTypes();
				if (availableTypes.length == 0) {
					subMonitor.worked(900 / fragments.size() / units.length);
				}

				for (int k = 0; k < availableTypes.length; k++) {
					ITypeHierarchy hierarchy = availableTypes[k].newSupertypeHierarchy(subMonitor.newChild(900
							/ fragments.size() / units.length / availableTypes.length));

					if (isSubclass(hierarchy, availableTypes[k], superclass)) {
						availableActivities.add(availableTypes[k]);
					}
				}

			}
		}

		return availableActivities;
	}

	/**
	 * Retrieves the list of fragments of a given {@code androidProject}.
	 * 
	 * @throws JavaModelException
	 *             If there are problems parsing java files.
	 * */
	public static List<IType> getAvailableFragmentsSubclasses(IProject androidProject, IProgressMonitor monitor)
			throws JavaModelException {
		List<IType> availableFragments = new ArrayList<IType>();

		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask("Resolving available types", 1000); //$NON-NLS-1$

		IJavaProject javaProject = JavaCore.create(androidProject);
		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(androidProject.findMember("src")); //$NON-NLS-1$
		ArrayList<IPackageFragment> fragments = new ArrayList<IPackageFragment>();
		for (IJavaElement element : root.getChildren()) {
			fragments.add((IPackageFragment) element);
		}

		subMonitor.worked(100);

		if (fragments.size() == 0) {
			subMonitor.worked(900);
		}

		for (IPackageFragment fragment : fragments) {
			ICompilationUnit[] units = fragment.getCompilationUnits();
			if (units.length == 0) {
				subMonitor.worked(900 / fragments.size());
			}
			for (int j = 0; j < units.length; j++) {
				ICompilationUnit unit = units[j];
				IType[] availableTypes = unit.getTypes();
				if (availableTypes.length == 0) {
					subMonitor.worked(900 / fragments.size() / units.length);
				}

				for (int k = 0; k < availableTypes.length; k++) {
					ITypeHierarchy hierarchy = availableTypes[k].newSupertypeHierarchy(subMonitor.newChild(900
							/ fragments.size() / units.length / availableTypes.length));

					if (isFragmentSubclass(hierarchy, availableTypes[k])) {
						availableFragments.add(availableTypes[k]);
					}
				}

			}
		}

		return availableFragments;
	}

	/*
	 * Returns true if the {@code type} belongs to {@code superclass} hierarchy.
	 * Otherwise, returns false.
	 */
	private static boolean isSubclass(ITypeHierarchy hierarchy, IType type, String superclass) {
		boolean contains = false;
		IType superclasstype = hierarchy.getSuperclass(type);
		if (superclasstype != null) {
			if (hierarchy.getType().getFullyQualifiedName().equals(superclass)
					|| superclasstype.getFullyQualifiedName().equals(superclass)) {
				contains = true;
			} else {
				contains = isSubclass(hierarchy, superclasstype, superclass);
			}
		}

		return contains;
	}

	/**
	 * @return True if the {@code type} belongs to {@code superclass} hierarchy.
	 *         Otherwise, returns false.
	 * */
	public static boolean isSubclass(IType type, String superclass) throws JavaModelException {
		ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(new NullProgressMonitor());
		return isSubclass(typeHierarchy, type, superclass);
	}

	/*
	 * Verifies if a given class extends a Fragment class (from sdk after 3.0 or
	 * from compatibility pack)
	 * 
	 * @param hierarchy the hierarchy abstraction to the {@code type}
	 * 
	 * @param type the type to be checked
	 * 
	 * @return True if {@code type} has a Android Fragment in its type
	 * hierarchy.
	 */
	private static boolean isFragmentSubclass(ITypeHierarchy hierarchy, IType type) {
		boolean contains = false;
		IType superclasstype = hierarchy.getSuperclass(type);
		if (superclasstype != null) {
			if (isAndroidFragment(superclasstype.getFullyQualifiedName())) {
				contains = true;
			} else {
				contains = isFragmentSubclass(hierarchy, superclasstype);
			}
		}

		return contains;
	}

	/*
	 * Verifies if a given class extends a Fragment class (Fragment from
	 * compatibility pack)
	 * 
	 * @param hierarchy
	 * 
	 * @param type
	 * 
	 * @return
	 */
	private static boolean isCompatibilityPackFragmentsSubclass(ITypeHierarchy hierarchy, IType type) {
		boolean contains = false;
		IType superclasstype = hierarchy.getSuperclass(type);
		if (superclasstype != null) {
			if (isAndroidCompatibilityPackFragment(superclasstype.getFullyQualifiedName())) {
				contains = true;
			} else {
				contains = isCompatibilityPackFragmentsSubclass(hierarchy, superclasstype);
			}
		}

		return contains;
	}

	/*
	 * Verifies if a given class extends a Fragment class (FragmentActivity from
	 * compatibility pack)
	 * 
	 * @param hierarchy
	 * 
	 * @param type
	 * 
	 * @return
	 */
	private static boolean isCompatibilityPackFragmentActivitySubclass(ITypeHierarchy hierarchy, IType type) {
		boolean contains = false;
		IType superclasstype = hierarchy.getSuperclass(type);
		if (superclasstype != null) {
			if (isAndroidCompatibilityPackFragmentActivity(superclasstype.getFullyQualifiedName())) {
				contains = true;
			} else {
				contains = isCompatibilityPackFragmentActivitySubclass(hierarchy, superclasstype);
			}
		}

		return contains;
	}

	private static boolean isAndroidFragment(String className) {
		boolean result = false;

		if ((className != null) && (className.startsWith("android.")) && (className.endsWith(".Fragment"))) {
			result = true;
		}

		return result;
	}

	private static boolean isAndroidCompatibilityPackFragment(String className) {
		boolean result = false;

		if ((className != null) && (className.startsWith("android.support.")) && (className.endsWith(".Fragment"))) {
			result = true;
		}

		return result;
	}

	private static boolean isAndroidCompatibilityPackFragmentActivity(String className) {
		boolean result = false;

		if ((className != null) && (className.startsWith("android.support."))
				&& (className.endsWith(".FragmentActivity"))) {
			result = true;
		}

		return result;
	}

	/**
	 * Parses source code.
	 *
	 * @param lwUnit
	 *            the Java Model handle for the compilation unit
	 * @return the root AST node of the parsed source
	 */
	public static CompilationUnit parse(ICompilationUnit lwUnit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(lwUnit); // set source
		parser.setResolveBindings(true); // we need bindings later on
		return (CompilationUnit) parser.createAST(null /* IProgressMonitor */); // parse
	}

	/**
	 * Creates the representation of a layout file based on the compilation unit
	 * 
	 * @param visitor
	 * @param layout
	 * @return layout file
	 * @throws AndroidException
	 *             if layout xml is malformed
	 */
	public static CodeGeneratorDataBasedOnLayout createLayoutFile(IProject project, ICompilationUnit compUnit)
			throws AndroidException {
		GenerateCodeBasedOnLayoutVisitor visitor = new GenerateCodeBasedOnLayoutVisitor();
		CompilationUnit cpAstNode = parse(compUnit);
		StudioLogger.info("Trying to visit code for class: " + compUnit.getResource().getName()); //$NON-NLS-1$
		try {
			cpAstNode.accept(visitor);
		} catch (IllegalArgumentException illegalArgumentException) {
			String msg = CodeUtilsNLS.JDTUtils_FragmentOnCreateViewWithProblemsOrWithWrongFormat;
			throw new AndroidException(msg, illegalArgumentException);
		}
		IFile layout = project.getFile(File.separator + IAndroidConstants.FD_RES + File.separator
				+ IAndroidConstants.FD_LAYOUT + File.separator + visitor.getLayoutName() + ".xml"); //$NON-NLS-1$
		CodeGeneratorDataBasedOnLayout codeGeneratorData = null;
		if (visitor.getLayoutName() == null) {
			// layout set or inflate not declared
			throw new AndroidException(CodeUtilsNLS.UI_ChooseLayoutItemsDialog_Error_onCreate_Not_Declared);
		} else {
			StudioLogger.info("Trying to read layout: " + layout); //$NON-NLS-1$            
			try {
				codeGeneratorData = new CodeGeneratorDataBasedOnLayout();
				codeGeneratorData.init(visitor.getLayoutName(), layout.getLocation().toFile());
				codeGeneratorData.setAssociatedType(visitor.getTypeAssociatedToLayout());

				JavaLayoutData javaLayoutData = new JavaLayoutData();
				javaLayoutData.setInflatedViewName(visitor.getInflatedViewName());
				javaLayoutData.setDeclaredViewIdsOnCode(visitor.getDeclaredViewIds());
				javaLayoutData.setSavedViewIds(visitor.getSavedViewIds());
				javaLayoutData.setRestoredViewIds(visitor.getRestoredViewIds());
				javaLayoutData.setVisitor(visitor);
				javaLayoutData.setCompUnit(compUnit);
				javaLayoutData.setCompUnitAstNode(cpAstNode);

				codeGeneratorData.setJavaLayoutData(javaLayoutData);
			} catch (AndroidException ae) {
				String errorsMsg = visitor.getLayoutName() != null ? NLS.bind(
						CodeUtilsNLS.JDTUtils_MalformedXMLWhenFilenameAvailable_Error, layout.getFullPath().toFile())
						: CodeUtilsNLS.JDTUtils_MalformedXMLWhenFilenameNotAvailable_Error;
				throw new AndroidException(errorsMsg, ae);
			}
		}
		return codeGeneratorData;
	}

	/**
	 * Creates the representation of a menu file based on the compilation unit
	 * 
	 * @param project
	 * @param compUnit
	 * @param menuFileName
	 * @param typeAssociated
	 * @return
	 * @throws AndroidException
	 *             if menu xml is malformed
	 */
	public static CodeGeneratorDataBasedOnMenu createMenuFile(IProject project, ICompilationUnit compUnit,
			String menuFileName, CodeGeneratorDataBasedOnMenu.TYPE typeAssociated) throws AndroidException {
		CodeGeneratorBasedOnMenuVisitor visitor = new CodeGeneratorBasedOnMenuVisitor();
		CompilationUnit cpAstNode = parse(compUnit);
		StudioLogger.info("Trying to visit code for class: " + compUnit.getResource().getName()); //$NON-NLS-1$
		try {
			cpAstNode.accept(visitor);
		} catch (IllegalArgumentException illegalArgumentException) {
			String msg = CodeUtilsNLS.JDTUtils_GenerateCodeForMenuVisitingCode_Error;
			throw new AndroidException(msg, illegalArgumentException);
		}
		IFile menu = project.getFile(File.separator + IAndroidConstants.FD_RES + File.separator
				+ IAndroidConstants.FD_MENU + File.separator + menuFileName);
		CodeGeneratorDataBasedOnMenu codeGeneratorData = null;
		StudioLogger.info("Trying to read menu: " + menu); //$NON-NLS-1$            
		try {
			codeGeneratorData = new CodeGeneratorDataBasedOnMenu();
			codeGeneratorData.init(menuFileName, menu.getLocation().toFile());
			codeGeneratorData.setAssociatedType(typeAssociated);
			codeGeneratorData.setAbstractCodeVisitor(visitor);
			codeGeneratorData.setICompilationUnit(compUnit);
			codeGeneratorData.setCompilationUnit(cpAstNode);
		} catch (AndroidException ae) {
			String errorsMsg = NLS.bind(CodeUtilsNLS.JDTUtils_MalformedMenuXMLWhenFilenameAvailable_Error, menu
					.getLocation().toFile().toString());
			throw new AndroidException(errorsMsg, ae);
		}

		return codeGeneratorData;
	}

	/**
	 * Given a Java {@link IFile}, this method returns <code>true</code> in case
	 * the qualified name entered as parameter represents its superclass.
	 * 
	 * @param javaFile
	 *            Java {@link IFile}.
	 * @param superClassFullyQualifiedName
	 *            Super class fully qualified naem.
	 * 
	 * @return Returns <code>true</code> in case the Java {@link IFile} class
	 *         inherits from the super class represented by its full qualified
	 *         name.
	 * 
	 * @throws JavaModelException
	 *             Exception thrown in case there are problems retrieving
	 *             classes hierarchy.
	 */
	public static boolean isSubclass(IFile javaFile, String superClassFullyQualifiedName) throws JavaModelException {
		ICompilationUnit compUnit = JavaCore.createCompilationUnitFrom(javaFile);

		IType type = compUnit.getType(javaFile.getName().split("." + javaFile.getFileExtension())[0]); //$NON-NLS-1$

		ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(new NullProgressMonitor());

		return isSubclass(typeHierarchy, type, superClassFullyQualifiedName);

	}

	/**
	 * Verifies if a java class extends a Fragment class from a compatibility
	 * pack
	 * 
	 * @param javaFile
	 * @return
	 * @throws JavaModelException
	 */
	public static boolean isFragmentSubclass(IFile javaFile) throws JavaModelException {
		ICompilationUnit compUnit = JavaCore.createCompilationUnitFrom(javaFile);

		IType type = compUnit.getType(javaFile.getName().split("." + javaFile.getFileExtension())[0]); //$NON-NLS-1$

		ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(new NullProgressMonitor());

		return isFragmentSubclass(typeHierarchy, type);
	}

	/**
	 * Verifies if a java class extends a Fragment class from a compatibility
	 * pack
	 * 
	 * @param javaFile
	 * @return
	 * @throws JavaModelException
	 */
	public static boolean isCompatibilityFragmentSubclass(IFile javaFile) throws JavaModelException {
		ICompilationUnit compUnit = JavaCore.createCompilationUnitFrom(javaFile);

		IType type = compUnit.getType(javaFile.getName().split("." + javaFile.getFileExtension())[0]); //$NON-NLS-1$

		ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(new NullProgressMonitor());

		return isCompatibilityPackFragmentsSubclass(typeHierarchy, type);
	}

	/**
	 * Verifies if a java class extends a FragmentActivity class from a
	 * compatibility pack
	 * 
	 * @param javaFile
	 * @return
	 * @throws JavaModelException
	 */
	public static boolean isCompatibilityFragmentActivitySubclass(IFile javaFile) throws JavaModelException {
		ICompilationUnit compUnit = JavaCore.createCompilationUnitFrom(javaFile);

		IType type = compUnit.getType(javaFile.getName().split("." + javaFile.getFileExtension())[0]); //$NON-NLS-1$

		ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(new NullProgressMonitor());

		return isCompatibilityPackFragmentActivitySubclass(typeHierarchy, type);
	}

	/**
	 * @return true if AST have at least one error (warnings are not
	 *         considered), false otherwise.
	 */
	public static boolean hasErrorInCompilationUnitAstUtils(CompilationUnit cpUnit) {
		boolean hasError = false;
		if (cpUnit != null) {
			IProblem[] problems = cpUnit.getProblems();
			for (IProblem probl : problems) {
				if (probl.isError()) {
					hasError = true;
					break;
				}
			}
		}
		return hasError;
	}

}
