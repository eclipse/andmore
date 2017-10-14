/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.andmore.internal.editors.layout.refactoring;

import static com.android.SdkConstants.FD_RES;
import static com.android.SdkConstants.FD_RES_LAYOUT;
import static com.android.SdkConstants.FD_RES_VALUES;
import static org.junit.Assert.*;

import com.android.ide.common.sdk.LoadStatus;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.integration.tests.DialogMonitor;
import org.eclipse.andmore.integration.tests.SdkLoadingTestCase;
import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.descriptors.LayoutDescriptors;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiDocumentNode;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.internal.wizards.newproject.NewProjectCreator;
import org.eclipse.andmore.internal.wizards.newproject.NewProjectWizardState;
import org.eclipse.andmore.internal.wizards.newproject.NewProjectWizardState.Mode;

import com.android.sdklib.IAndroidTarget;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.junit.After;
import org.junit.Before;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({ "restriction", "javadoc" })
public abstract class AdtProjectTest extends SdkLoadingTestCase {
	private static final int TARGET_API_LEVEL = 16;
	public static final String TEST_PROJECT_PACKAGE = "com.android.eclipse.tests"; //$NON-NLS-1$
	private static final long TESTS_START_TIME = System.currentTimeMillis();
	private static final String PROJECTNAME_PREFIX = "testproject-";
	
	DialogMonitor dialogMonitor = new DialogMonitor();
	
	
	@Before
	public void setUp() throws Exception {
		startMonitoringJob();
		
		System.out.println("AdtProject: setup");
		System.out.println("Starting Test: " + testName.getMethodName());
		// Prevent preview icon computation during plugin test to make test
		// faster
		
		if (AndmoreAndroidPlugin.getDefault() == null) {
			fail("This test must be run as an Eclipse plugin test, not a plain JUnit test!");
		}
				
		AdtPrefs.getPrefs().setPaletteModes("ICON_TEXT"); //$NON-NLS-1$

		getProject();

		Sdk current = Sdk.getCurrent();
		assertNotNull(current);
		LoadStatus sdkStatus = AndmoreAndroidPlugin.getDefault().getSdkLoadStatus();
		assertSame(LoadStatus.LOADED, sdkStatus);
		IAndroidTarget target = current.getTarget(getProject());
		IJavaProject javaProject = BaseProjectHelper.getJavaProject(getProject());
		assertNotNull(javaProject);
		int iterations = 0;
		while (true) {
			if (iterations == 100) {
				fail("Couldn't load target; ran out of time");
			}
			LoadStatus status = current.checkAndLoadTargetData(target, javaProject);
			if (status == LoadStatus.FAILED) {
				fail("Couldn't load target " + target);
			}
			if (status != LoadStatus.LOADING) {
				break;
			}
			Thread.sleep(250);
			iterations++;
		}
//		AndroidTargetData targetData = current.getTargetData(target);
//		assertNotNull(targetData);
//		LayoutDescriptors layoutDescriptors = targetData.getLayoutDescriptors();
//		assertNotNull(layoutDescriptors);
//		List<ViewElementDescriptor> viewDescriptors = layoutDescriptors.getViewDescriptors();
//		assertNotNull(viewDescriptors);
//		assertTrue(viewDescriptors.size() > 0);
//		List<ViewElementDescriptor> layoutParamDescriptors = layoutDescriptors.getLayoutDescriptors();
//		assertNotNull(layoutParamDescriptors);
//		assertTrue(layoutParamDescriptors.size() > 0);
	}

	protected void startMonitoringJob() {
		dialogMonitor.startMonitoring();
	}
	
	@Override
	@After
	public void tearDownProjects() throws Exception {
		IProject project = getProject();
		project.delete(true, new NullProgressMonitor());
		System.out.println("Deleting projet " + project.getName());
		super.tearDownProjects();
	}

	/**
	 * We don't stash the project used by each test case as a field such that
	 * test cases can share a single project instance (which is typically much
	 * faster). However, see {@link #getProjectName()} for exceptions to this
	 * sharing scheme.
	 */
	private static Map<String, IProject> sProjectMap = new HashMap<String, IProject>();


	@Override
	protected String getTestDataRelPath() {
		return "eclipse/plugins/org.eclipse.andmore.integration.tests/src/org/eclipse/andmore/"
				+ "internal/editors/layout/refactoring/testdata";
	}

	@Override
	protected InputStream getTestResource(String relativePath, boolean expectExists) {
		String path = "testdata" + File.separator + relativePath; //$NON-NLS-1$
		InputStream stream = AdtProjectTest.class.getResourceAsStream(path);
		if (!expectExists && stream == null) {
			return null;
		}
		return stream;
	}


	protected boolean testNeedsUniqueProject() {
		return true;
	}
	

	@Override
	protected boolean validateSdk(IAndroidTarget target) {
		// Not quite working yet. When enabled will make tests run faster.
		// if (target.getVersion().getApiLevel() < TARGET_API_LEVEL) {
		// return false;
		// }

		return true;
	}

	/**
	 * Returns a name to use for the project used in this test. Subclasses do
	 * not need to override this if they can share a project with others - which
	 * is the case if they do not modify the project in a way that does not
	 * affect other tests. For example the resource quickfix test will create
	 * new resources which affect what shows up in the code completion results,
	 * so the quickfix tests will override this method to produce a unique
	 * project for its own tests.
	 */
	private String getProjectName() {
		if (testNeedsUniqueProject()) {
			return PROJECTNAME_PREFIX + getClass().getSimpleName() + "-" + testName.getMethodName();
		} else {
			return PROJECTNAME_PREFIX + TESTS_START_TIME;
		}
	}

	protected IProject getProject() {
		String projectName = getProjectName();
		IProject project = sProjectMap.get(projectName);
		if (project == null) {
			project = createProject(projectName);
			assertNotNull(project);
			sProjectMap.put(projectName, project);
		}
		if (!testNeedsUniqueProject()) {
			addCleanupDir(AdtUtils.getAbsolutePath(project).toFile());
		}
		addCleanupDir(project.getFullPath().toFile());
		return project;
	}

	protected IFile getTestDataFile(IProject project, String name) throws Exception {
		return getTestDataFile(project, name, name);
	}

	protected IFile getLayoutFile(IProject project, String name) throws Exception {
		return getTestDataFile(project, name, FD_RES + "/" + FD_RES_LAYOUT + "/" + name);
	}

	protected IFile getValueFile(IProject project, String name) throws Exception {
		return getTestDataFile(project, name, FD_RES + "/" + FD_RES_VALUES + "/" + name);
	}

	protected IFile getTestDataFile(IProject project, String sourceName, String destPath) throws Exception {
		return getTestDataFile(project, sourceName, destPath, false);
	}

	protected IFile getTestDataFile(IProject project, String sourceName, String destPath, boolean overwrite)
			throws Exception {
		String[] split = destPath.split("/"); //$NON-NLS-1$
		IContainer parent;
		String name;
		if (split.length == 1) {
			parent = project;
			name = destPath;
		} else {
			IFolder folder = project.getFolder(split[0]);
			NullProgressMonitor monitor = new NullProgressMonitor();
			if (!folder.exists()) {
				folder.create(true /* force */, true /* local */, monitor);
			}
			for (int i = 1, n = split.length; i < n - 1; i++) {
				IFolder subFolder = folder.getFolder(split[i]);
				if (!subFolder.exists()) {
					subFolder.create(true /* force */, true /* local */, monitor);
				}
				folder = subFolder;
			}
			name = split[split.length - 1];
			parent = folder;
		}
		IFile file = parent.getFile(new Path(name));
		if (overwrite && file.exists()) {
			String currentContents = AndmoreAndroidPlugin.readFile(file);
			String newContents = readTestFile(sourceName, true);
			if (currentContents == null || !currentContents.equals(newContents)) {
				file.delete(true, new NullProgressMonitor());
			} else {
				return file;
			}
		}
		if (!file.exists()) {
			String xml = readTestFile(sourceName, true);
			InputStream bstream = new ByteArrayInputStream(xml.getBytes("UTF-8")); //$NON-NLS-1$
			NullProgressMonitor monitor = new NullProgressMonitor();
			file.create(bstream, false /* force */, monitor);
		}

		return file;
	}

	protected IProject createProject(String name) {
		IAndroidTarget target = null;

		for (IAndroidTarget t : getSdk().getTargets()) {
			if (!t.isPlatform()) {
				continue;
			}
			if (t.getVersion().getApiLevel() >= TARGET_API_LEVEL) {
				target = t;
				break;
			}
		}
		assertNotNull(target);

		IRunnableContext context = new IRunnableContext() {
			@Override
			public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable)
					throws InvocationTargetException, InterruptedException {
				runnable.run(new NullProgressMonitor());
			}
		};
		NewProjectWizardState state = new NewProjectWizardState(Mode.ANY);
		state.projectName = name;
		state.target = target;
		state.packageName = TEST_PROJECT_PACKAGE;
		state.activityName = name;
		state.applicationName = name;
		state.createActivity = false;
		state.useDefaultLocation = true;
		if (getMinSdk() != -1) {
			state.minSdk = Integer.toString(getMinSdk());
		}

		NewProjectCreator creator = new NewProjectCreator(state, context);
		creator.createAndroidProjects();
		return validateProjectExists(name);
	}

	protected int getMinSdk() {
		return -1;
	}

	public void createTestProject() {
		IAndroidTarget target = null;

		for (IAndroidTarget t : getSdk().getTargets()) {
			if (t.getVersion().getApiLevel() >= TARGET_API_LEVEL) {
				target = t;
				break;
			}
		}
		assertNotNull(target);
	}

	protected static IProject validateProjectExists(String name) {
		IProject iproject = getProject(name);
		assertTrue(String.format("%s project not created", name), iproject.exists());
		assertTrue(String.format("%s project not opened", name), iproject.isOpen());
		return iproject;
	}

	private static IProject getProject(String name) {
		IProject iproject = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		return iproject;
	}

	protected int getCaretOffset(IFile file, String caretLocation) {
		assertTrue(caretLocation, caretLocation.contains("^"));

		String fileContent = AndmoreAndroidPlugin.readFile(file);
		return getCaretOffset(fileContent, caretLocation);
	}

	/**
	 * If the given caret location string contains a selection range, select
	 * that range in the given viewer
	 *
	 * @param viewer
	 *            the viewer to contain the selection
	 * @param caretLocation
	 *            the location string
	 */
	protected int updateCaret(ISourceViewer viewer, String caretLocation) {
		assertTrue(caretLocation, caretLocation.contains("^")); //$NON-NLS-1$

		int caretDelta = caretLocation.indexOf("^"); //$NON-NLS-1$
		assertTrue(caretLocation, caretDelta != -1);
		String text = viewer.getTextWidget().getText();

		int length = 0;

		// String around caret/range without the range and caret marker
		// characters
		String caretContext;

		if (caretLocation.contains("[^")) { //$NON-NLS-1$
			caretDelta--;
			assertTrue(caretLocation, caretLocation.startsWith("[^", caretDelta)); //$NON-NLS-1$

			int caretRangeEnd = caretLocation.indexOf(']', caretDelta + 2);
			assertTrue(caretLocation, caretRangeEnd != -1);
			length = caretRangeEnd - caretDelta - 2;
			assertTrue(length > 0);
			caretContext = caretLocation.substring(0, caretDelta)
					+ caretLocation.substring(caretDelta + 2, caretRangeEnd)
					+ caretLocation.substring(caretRangeEnd + 1);
		} else {
			caretContext = caretLocation.substring(0, caretDelta) + caretLocation.substring(caretDelta + 1); // +1:
																												// skip
																												// "^"
		}

		int caretContextIndex = text.indexOf(caretContext);

		assertTrue("Caret content " + caretContext + " not found in file", caretContextIndex != -1);

		int offset = caretContextIndex + caretDelta;
		viewer.setSelectedRange(offset, length);

		return offset;
	}

	protected String addSelection(String newFileContents, Point selectedRange) {
		int selectionBegin = selectedRange.x;
		int selectionEnd = selectionBegin + selectedRange.y;
		return addSelection(newFileContents, selectionBegin, selectionEnd);
	}

	@Override
	protected String removeSessionData(String data) {
		data = super.removeSessionData(data);
		if (getProject() != null) {
			data = data.replace(getProject().getName(), "PROJECTNAME");
		}

		return data;
	}

	public static ViewElementDescriptor createDesc(String name, String fqn, boolean hasChildren) {
		if (hasChildren) {
			return new ViewElementDescriptor(name, name, fqn, "", "", new AttributeDescriptor[0],
					new AttributeDescriptor[0], new ElementDescriptor[1], false);
		} else {
			return new ViewElementDescriptor(name, fqn);
		}
	}

	public static UiViewElementNode createNode(UiViewElementNode parent, String fqn, boolean hasChildren) {
		String name = fqn.substring(fqn.lastIndexOf('.') + 1);
		ViewElementDescriptor descriptor = createDesc(name, fqn, hasChildren);
		if (parent == null) {
			// All node hierarchies should be wrapped inside a document node at
			// the root
			parent = new UiViewElementNode(createDesc("doc", "doc", true));
		}
		return (UiViewElementNode) parent.appendNewUiChild(descriptor);
	}

	public static UiViewElementNode createNode(String fqn, boolean hasChildren) {
		return createNode(null, fqn, hasChildren);
	}

	/** Special editor context set on the model to be rendered */
	protected static class TestLayoutEditorDelegate extends LayoutEditorDelegate {

		public TestLayoutEditorDelegate(IFile file, IStructuredDocument structuredDocument, UiDocumentNode uiRootNode) {
			super(new TestAndroidXmlCommonEditor(file, structuredDocument, uiRootNode));
		}

		static class TestAndroidXmlCommonEditor extends CommonXmlEditor {

			private final IFile mFile;
			private final IStructuredDocument mStructuredDocument;
			private UiDocumentNode mUiRootNode;

			TestAndroidXmlCommonEditor(IFile file, IStructuredDocument structuredDocument, UiDocumentNode uiRootNode) {
				mFile = file;
				mStructuredDocument = structuredDocument;
				mUiRootNode = uiRootNode;
			}

			@Override
			public IFile getInputFile() {
				return mFile;
			}

			@Override
			public IProject getProject() {
				return mFile.getProject();
			}

			@Override
			public IStructuredDocument getStructuredDocument() {
				return mStructuredDocument;
			}

			@Override
			public UiDocumentNode getUiRootNode() {
				return mUiRootNode;
			}

			@Override
			public void editorDirtyStateChanged() {
			}

			@Override
			public IStructuredModel getModelForRead() {
				IModelManager mm = StructuredModelManager.getModelManager();
				if (mm != null) {
					try {
						return mm.getModelForRead(mFile);
					} catch (Exception e) {
						fail(e.toString());
					}
				}

				return null;
			}
		}
	}
}
