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
package org.eclipse.andmore.android.model;

import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.AndroidUtils;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.model.java.JavaClass;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;

/**
 * Controller for Building Blocks.
 */
public abstract class BuildingBlockModel implements IWizardModel {
	private String name = "";

	private final Set<String> intentFilterPermissions = new HashSet<String>();

	/**
	 * Constructor for Building class
	 * 
	 * @param superClass
	 *            the Building block superclass
	 */
	public BuildingBlockModel(String superClass) {
		if ((superClass == null) || (superClass.length() == 0)) {
			throw new InvalidParameterException();
		}
		this.superClass = superClass;
	}

	private String superClass;

	private IPackageFragmentRoot packageFragmentRoot;

	private IPackageFragment packageFragment;

	private IStatus nameStatus = new Status(IStatus.OK, CodeUtilsActivator.PLUGIN_ID, null);

	private IStatus packageStatus = new Status(IStatus.OK, CodeUtilsActivator.PLUGIN_ID, null);

	private IStatus packageFragmentRootStatus = new Status(IStatus.OK, CodeUtilsActivator.PLUGIN_ID, null);

	private String label;

	private IStatus labelStatus = new Status(IStatus.OK, CodeUtilsActivator.PLUGIN_ID, null);

	private int apiVersion;

	/**
	 * Return building block name
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return superclass
	 * 
	 * @return
	 */
	public String getSuperClass() {
		return superClass;
	}

	/**
	 * Set building block name
	 * 
	 * @param typeName
	 */
	public void setName(String typeName) {
		this.name = typeName;
	}

	/**
	 * Set superclass
	 * 
	 * @param superClass
	 */
	public void setSuperClass(String superClass) {
		this.superClass = superClass;
	}

	/**
	 * Change source folder
	 * 
	 * @return
	 */
	public IPackageFragmentRoot getPackageFragmentRoot() {
		return packageFragmentRoot;
	}

	/**
	 * Check if using extended Class
	 * 
	 * @return
	 */
	public boolean useExtendedClass() {
		return false;
	}

	/**
	 * Change source folder
	 * 
	 * @param pack
	 */
	public void setPackageFragmentRoot(IPackageFragmentRoot pack) {
		this.packageFragmentRoot = pack;
	}

	/**
	 * Change package
	 * 
	 * @param packageFragment
	 */
	public void setPackageFragment(IPackageFragment packageFragment) {
		if ((packageFragmentRoot != null) && (packageFragment != null)
				&& !packageFragmentRoot.getJavaProject().equals(packageFragment.getJavaProject())) {
			throw new InvalidParameterException();
		} else {
			this.packageFragment = packageFragment;
		}
	}

	/**
	 * Return package
	 * 
	 * @return
	 */
	public IPackageFragment getPackageFragment() {
		return packageFragment;
	}

	/**
	 * Configure source folder and package from workbench selection
	 * 
	 * @param selection
	 */
	public void configure(IStructuredSelection selection) {
		try {
			IPackageFragmentRoot srcFolder = extractPackageFragmentRoot(selection);
			setPackageFragmentRoot(srcFolder);
			IJavaProject javaProject = srcFolder == null ? null : srcFolder.getJavaProject();
			setPackageFragment(extractPackageFragment(selection, javaProject));
			if (javaProject != null) {
				apiVersion = AndroidUtils.getApiVersionNumberForProject(javaProject.getProject());
			}

		} catch (Exception e) {
			StudioLogger.error(BuildingBlockModel.class, "Error configuring building block from selection.", e);
		}
	}

	/**
	 * Extract Package from selection
	 * 
	 * @param selection
	 * @param javaProject
	 * @return
	 * @throws CoreException
	 */
	private IPackageFragment extractPackageFragment(IStructuredSelection selection, IJavaProject javaProject)
			throws CoreException {
		IPackageFragment pack = null;
		Object object = selection.getFirstElement();
		if ((object instanceof IPackageFragment) && ((IPackageFragment) object).getJavaProject().equals(javaProject)) {
			pack = (IPackageFragment) object;
		} else if ((object instanceof IJavaElement) && ((IJavaElement) object).getJavaProject().equals(javaProject)) {
			pack = (IPackageFragment) ((IJavaElement) object).getAncestor(IJavaElement.PACKAGE_FRAGMENT);
			if (pack == null) {
				pack = EclipseUtils.getDefaultPackageFragment(((IJavaElement) object).getJavaProject());
			}
		} else if (object instanceof IResource) {
			pack = EclipseUtils.getDefaultPackageFragment(javaProject == null ? JavaCore.create(((IResource) object)
					.getProject()) : javaProject);
		} else {
			if (javaProject != null) {
				pack = EclipseUtils.getDefaultPackageFragment(javaProject);
			} else {
				IJavaProject[] prjs = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
				if (prjs.length > 0) {
					pack = EclipseUtils.getDefaultPackageFragment(prjs[0]);
				}
			}

		}

		if (pack != null) {
			if (!pack.getJavaProject().getProject().hasNature(IAndroidConstants.ANDROID_NATURE)) {
				pack = extractPackageFragment(new TreeSelection(), javaProject);
			}
		}
		return pack;
	}

	/**
	 * Extract source folder from selection.
	 * 
	 * @param selection
	 * @return
	 * @throws CoreException
	 */
	private static IPackageFragmentRoot extractPackageFragmentRoot(IStructuredSelection selection) throws CoreException {
		IPackageFragmentRoot pack = null;
		Object selectionElement = selection.getFirstElement();

		if (selectionElement instanceof IPackageFragmentRoot) {
			pack = (IPackageFragmentRoot) selectionElement;
		} else if (selectionElement instanceof IJavaElement) {
			pack = (IPackageFragmentRoot) ((IJavaElement) selectionElement)
					.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			if (pack == null) {
				IJavaProject element = ((IJavaElement) selectionElement).getJavaProject();

				for (IPackageFragmentRoot root : element.getPackageFragmentRoots()) {
					if (root.getResource() != null) {
						boolean isSrc = (root.getElementType() & IPackageFragmentRoot.K_SOURCE) == IPackageFragmentRoot.K_SOURCE;
						boolean isGen = root.getElementName().equals(IAndroidConstants.GEN_SRC_FOLDER)
								&& (root.getParent() instanceof IJavaProject);
						if (isSrc && !isGen) {
							pack = root;
							break;
						}
					}
				}
			}
		} else if (selectionElement instanceof IResource) {
			IJavaProject element = JavaCore.create(((IResource) selectionElement).getProject());

			if (element.isOpen()) {
				for (IPackageFragmentRoot root : element.getPackageFragmentRoots()) {
					if (root.getResource() != null) {
						boolean isSrc = (root.getElementType() & IPackageFragmentRoot.K_SOURCE) == IPackageFragmentRoot.K_SOURCE;
						boolean isGen = root.getElementName().equals(IAndroidConstants.GEN_SRC_FOLDER)
								&& (root.getParent() instanceof IJavaProject);
						if (isSrc && !isGen) {
							pack = root;
							break;
						}
					}
				}
			}
		} else {
			IJavaProject[] allProjects = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
			if ((allProjects != null) && (allProjects.length > 0)) {
				for (IJavaProject project : allProjects) {
					if (project.getResource().getProject().hasNature(IAndroidConstants.ANDROID_NATURE)) {
						IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();

						if ((roots != null) && (roots.length > 0)) {
							boolean found = false;

							for (IPackageFragmentRoot root : roots) {
								boolean isSrc = (root.getElementType() & IPackageFragmentRoot.K_SOURCE) == IPackageFragmentRoot.K_SOURCE;
								boolean isGen = root.getElementName().equals(IAndroidConstants.GEN_SRC_FOLDER)
										&& (root.getParent() instanceof IJavaProject);
								if (isSrc && !isGen) {
									found = true;
									pack = root;
									break;
								}
							}

							if (found) {
								break;
							}
						}
					}
				}
			}
		}

		if (pack != null) {
			if (!pack.getJavaProject().getProject().hasNature(IAndroidConstants.ANDROID_NATURE)) {
				pack = extractPackageFragmentRoot(new TreeSelection());
			}
		}
		return pack;
	}

	/**
	 * Return selected project.
	 * 
	 * @return
	 */
	public IProject getProject() {
		IProject project = null;
		if (packageFragmentRoot != null) {
			project = packageFragmentRoot.getJavaProject().getProject();
		} else if (packageFragment != null) {
			project = packageFragment.getJavaProject().getProject();
		}
		return project;
	}

	/**
	 * Change status from name. Use JDT validation.
	 * 
	 * @param nameStatus
	 */
	public void setNameStatus(IStatus nameStatus) {
		this.nameStatus = nameStatus;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.IWizardModel#getStatus()
	 */
	@Override
	public IStatus getStatus() {
		return getMostSevere(new IStatus[] { packageFragmentRootStatus, nameStatus, packageStatus, labelStatus });
	}

	/**
	 * Find most severe status.
	 * 
	 * @param status
	 * @return
	 */
	protected static IStatus getMostSevere(IStatus[] status) {
		IStatus max = null;
		for (int i = 0; i < status.length; i++) {
			IStatus curr = status[i];
			if (curr.matches(IStatus.ERROR)) {
				return curr;
			}
			if ((max == null) || (curr.getSeverity() > max.getSeverity())) {
				max = curr;
			}
		}

		return max;
	}

	/**
	 * Change package status. Use JDT Validation
	 * 
	 * @param packageStatus
	 */
	public void setPackageStatus(IStatus packageStatus) {
		this.packageStatus = packageStatus;
	}

	/**
	 * Change source folder status. Use JDT Validation
	 * 
	 * @param packageFragmentRootStatus
	 */
	public void setPackageFragmentRootStatus(IStatus packageFragmentRootStatus) {
		this.packageFragmentRootStatus = packageFragmentRootStatus;
	}

	/**
	 * Change label.
	 * 
	 * @param label
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Return Label.
	 * 
	 * @return
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Change Label Status. Don't uses JDT validation.
	 * 
	 * @param labelStatus
	 */
	public void setLabelStatus(IStatus labelStatus) {
		this.labelStatus = labelStatus;
	}

	/**
	 * Creates the Java Class file
	 * 
	 * @param javaClass
	 *            The Java Class model
	 * @param monitor
	 *            The progress monitor
	 * @throws JavaModelException
	 * @throws AndroidException
	 */
	protected void createJavaClassFile(JavaClass javaClass, IProgressMonitor monitor) throws JavaModelException,
			AndroidException {
		final String JAVA_EXTENSION = ".java";

		IPackageFragment targetPackage = getPackageFragmentRoot().getPackageFragment(
				getPackageFragment().getElementName());

		if (!targetPackage.exists()) {
			getPackageFragmentRoot().createPackageFragment(targetPackage.getElementName(), true, monitor);
		}

		targetPackage.createCompilationUnit(getName() + JAVA_EXTENSION, javaClass.getClassContent().get(), true,
				monitor);
	}

	/**
	 * Return all Filter Permissions as an Array.
	 * 
	 * @return
	 */
	public String[] getIntentFilterPermissionsAsArray() {
		return intentFilterPermissions.toArray(new String[intentFilterPermissions.size()]);
	}

	/**
	 * Returns all intent filter permissions.
	 * 
	 * @return
	 */
	public Set<String> getIntentFilterPermissions() {
		return intentFilterPermissions;
	}

	/**
	 * Adds an intent filter permission to this launcher.
	 * 
	 * @param category
	 */
	public void addIntentFilterPermissions(String permission) {
		intentFilterPermissions.add(permission);
	}

	/**
	 * Remove intent filter permission.
	 * 
	 * @param category
	 */
	public void removeIntentFilterPermissions(String permission) {
		intentFilterPermissions.remove(permission);
	}

	/**
	 * @return the apiVersion
	 */
	public int getApiVersion() {
		return apiVersion;
	}
}
