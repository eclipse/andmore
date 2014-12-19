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

package org.eclipse.andmore.internal.editors.layout.gre;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.AUTO_URI;
import static com.android.SdkConstants.CLASS_FRAGMENT;
import static com.android.SdkConstants.CLASS_V4_FRAGMENT;
import static com.android.SdkConstants.CLASS_VIEW;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.URI_PREFIX;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.IClientRulesEngine;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.IValidator;
import com.android.ide.common.api.IViewMetadata;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.Margins;
import com.android.ide.common.api.Rect;
import com.android.ide.common.resources.ResourceRepository;
import com.android.resources.ResourceType;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.common.layout.BaseViewRule;
import org.eclipse.andmore.internal.actions.AddSupportJarAction;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.DescriptorsUtils;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationChooser;
import org.eclipse.andmore.internal.editors.layout.gle2.CanvasViewInfo;
import org.eclipse.andmore.internal.editors.layout.gle2.GraphicalEditorPart;
import org.eclipse.andmore.internal.editors.layout.gle2.LayoutCanvas;
import org.eclipse.andmore.internal.editors.layout.gle2.RenderService;
import org.eclipse.andmore.internal.editors.layout.gle2.SelectionManager;
import org.eclipse.andmore.internal.editors.layout.gle2.ViewHierarchy;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.editors.uimodel.UiDocumentNode;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.refactorings.core.RenameResult;
import org.eclipse.andmore.internal.resources.CyclicDependencyValidator;
import org.eclipse.andmore.internal.resources.ResourceNameValidator;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.internal.ui.MarginChooser;
import org.eclipse.andmore.internal.ui.ReferenceChooserDialog;
import org.eclipse.andmore.internal.ui.ResourceChooser;
import org.eclipse.andmore.internal.ui.ResourcePreviewHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.OpenNewClassWizardAction;
import org.eclipse.jdt.ui.dialogs.ITypeInfoFilterExtension;
import org.eclipse.jdt.ui.dialogs.ITypeInfoRequestor;
import org.eclipse.jdt.ui.dialogs.TypeSelectionExtension;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of {@link IClientRulesEngine}. This provides {@link IViewRule} clients
 * with a few methods they can use to access functionality from this {@link RulesEngine}.
 */
class ClientRulesEngine implements IClientRulesEngine {
    /** The return code from the dialog for the user choosing "Clear" */
    public static final int CLEAR_RETURN_CODE = -5;
    /** The dialog button ID for the user choosing "Clear" */
    private static final int CLEAR_BUTTON_ID = CLEAR_RETURN_CODE;

    private final RulesEngine mRulesEngine;
    private final String mFqcn;

    public ClientRulesEngine(RulesEngine rulesEngine, String fqcn) {
        mRulesEngine = rulesEngine;
        mFqcn = fqcn;
    }

    @Override
    public @NonNull String getFqcn() {
        return mFqcn;
    }

    @Override
    public void debugPrintf(@NonNull String msg, Object... params) {
        AdtPlugin.printToConsole(
                mFqcn == null ? "<unknown>" : mFqcn,
                String.format(msg, params)
                );
    }

    @Override
    public IViewRule loadRule(@NonNull String fqcn) {
        return mRulesEngine.loadRule(fqcn, fqcn);
    }

    @Override
    public void displayAlert(@NonNull String message) {
        MessageDialog.openInformation(
                AdtPlugin.getShell(),
                mFqcn,  // title
                message);
    }

    @Override
    public boolean rename(INode node) {
        GraphicalEditorPart editor = mRulesEngine.getEditor();
        SelectionManager manager = editor.getCanvasControl().getSelectionManager();
        RenameResult result = manager.performRename(node, null);

        return !result.isCanceled() && !result.isUnavailable();
    }

    @Override
    public String displayInput(@NonNull String message, @Nullable String value,
            final @Nullable IValidator filter) {
        IInputValidator validator = null;
        if (filter != null) {
            validator = new IInputValidator() {
                @Override
                public String isValid(String newText) {
                    // IValidator has the same interface as SWT's IInputValidator
                    try {
                        return filter.validate(newText);
                    } catch (Exception e) {
                        AdtPlugin.log(e, "Custom validator failed: %s", e.toString());
                        return ""; //$NON-NLS-1$
                    }
                }
            };
        }

        InputDialog d = new InputDialog(
                    AdtPlugin.getShell(),
                    mFqcn,  // title
                    message,
                    value == null ? "" : value, //$NON-NLS-1$
                    validator) {
            @Override
            protected void createButtonsForButtonBar(Composite parent) {
                createButton(parent, CLEAR_BUTTON_ID, "Clear", false /*defaultButton*/);
                super.createButtonsForButtonBar(parent);
            }

            @Override
            protected void buttonPressed(int buttonId) {
                super.buttonPressed(buttonId);

                if (buttonId == CLEAR_BUTTON_ID) {
                    assert CLEAR_RETURN_CODE != Window.OK && CLEAR_RETURN_CODE != Window.CANCEL;
                    setReturnCode(CLEAR_RETURN_CODE);
                    close();
                }
            }
        };
        int result = d.open();
        if (result == ResourceChooser.CLEAR_RETURN_CODE) {
            return "";
        } else if (result == Window.OK) {
            return d.getValue();
        }
        return null;
    }

    @Override
    @Nullable
    public Object getViewObject(@NonNull INode node) {
        ViewHierarchy views = mRulesEngine.getEditor().getCanvasControl().getViewHierarchy();
        CanvasViewInfo vi = views.findViewInfoFor(node);
        if (vi != null) {
            return vi.getViewObject();
        }

        return null;
    }

    @Override
    public @NonNull IViewMetadata getMetadata(final @NonNull String fqcn) {
        return new IViewMetadata() {
            @Override
            public @NonNull String getDisplayName() {
                // This also works when there is no "."
                return fqcn.substring(fqcn.lastIndexOf('.') + 1);
            }

            @Override
            public @NonNull FillPreference getFillPreference() {
                return ViewMetadataRepository.get().getFillPreference(fqcn);
            }

            @Override
            public @NonNull Margins getInsets() {
                return mRulesEngine.getEditor().getCanvasControl().getInsets(fqcn);
            }

            @Override
            public @NonNull List<String> getTopAttributes() {
                return ViewMetadataRepository.get().getTopAttributes(fqcn);
            }
        };
    }

    @Override
    public int getMinApiLevel() {
        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk != null) {
            IAndroidTarget target = currentSdk.getTarget(mRulesEngine.getEditor().getProject());
            if (target != null) {
                return target.getVersion().getApiLevel();
            }
        }

        return -1;
    }

    @Override
    public IValidator getResourceValidator(
            @NonNull final String resourceTypeName, final boolean uniqueInProject,
            final boolean uniqueInLayout, final boolean exists, final String... allowed) {
        return new IValidator() {
            private ResourceNameValidator mValidator;

            @Override
            public String validate(@NonNull String text) {
                if (mValidator == null) {
                    ResourceType type = ResourceType.getEnum(resourceTypeName);
                    if (uniqueInLayout) {
                        assert !uniqueInProject;
                        assert !exists;
                        Set<String> existing = new HashSet<String>();
                        Document doc = mRulesEngine.getEditor().getModel().getXmlDocument();
                        if (doc != null) {
                            addIds(doc, existing);
                        }
                        for (String s : allowed) {
                            existing.remove(s);
                        }
                        mValidator = ResourceNameValidator.create(false, existing, type);
                    } else {
                        assert allowed.length == 0;
                        IProject project = mRulesEngine.getEditor().getProject();
                        mValidator = ResourceNameValidator.create(false, project, type);
                        if (uniqueInProject) {
                            mValidator.unique();
                        }
                    }
                    if (exists) {
                        mValidator.exist();
                    }
                }

                return mValidator.isValid(text);
            }
        };
    }

    /** Find declared ids under the given DOM node */
    private static void addIds(Node node, Set<String> ids) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            String id = element.getAttributeNS(ANDROID_URI, ATTR_ID);
            if (id != null && id.startsWith(NEW_ID_PREFIX)) {
                ids.add(BaseViewRule.stripIdPrefix(id));
            }
        }

        NodeList children = node.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node child = children.item(i);
            addIds(child, ids);
        }
    }

    @Override
    public String displayReferenceInput(@Nullable String currentValue) {
        GraphicalEditorPart graphicalEditor = mRulesEngine.getEditor();
        LayoutEditorDelegate delegate = graphicalEditor.getEditorDelegate();
        IProject project = delegate.getEditor().getProject();
        if (project != null) {
            // get the resource repository for this project and the system resources.
            ResourceRepository projectRepository =
                ResourceManager.getInstance().getProjectResources(project);
            Shell shell = AdtPlugin.getShell();
            if (shell == null) {
                return null;
            }
            ReferenceChooserDialog dlg = new ReferenceChooserDialog(
                    project,
                    projectRepository,
                    shell);
            dlg.setPreviewHelper(new ResourcePreviewHelper(dlg, graphicalEditor));

            dlg.setCurrentResource(currentValue);

            if (dlg.open() == Window.OK) {
                return dlg.getCurrentResource();
            }
        }

        return null;
    }

    @Override
    public String displayResourceInput(@NonNull String resourceTypeName,
            @Nullable String currentValue) {
        return displayResourceInput(resourceTypeName, currentValue, null);
    }

    private String displayResourceInput(String resourceTypeName, String currentValue,
            IInputValidator validator) {
        ResourceType type = ResourceType.getEnum(resourceTypeName);
        GraphicalEditorPart graphicalEditor = mRulesEngine.getEditor();
        return ResourceChooser.chooseResource(graphicalEditor, type, currentValue, validator);
    }

    @Override
    public String[] displayMarginInput(@Nullable String all, @Nullable String left,
            @Nullable String right, @Nullable String top, @Nullable String bottom) {
        GraphicalEditorPart editor = mRulesEngine.getEditor();
        IProject project = editor.getProject();
        if (project != null) {
            Shell shell = AdtPlugin.getShell();
            if (shell == null) {
                return null;
            }
            AndroidTargetData data = editor.getEditorDelegate().getEditor().getTargetData();
            MarginChooser dialog = new MarginChooser(shell, editor, data, all, left, right,
                    top, bottom);
            if (dialog.open() == Window.OK) {
                return dialog.getMargins();
            }
        }

        return null;
    }

    @Override
    public String displayIncludeSourceInput() {
        AndroidXmlEditor editor = mRulesEngine.getEditor().getEditorDelegate().getEditor();
        IInputValidator validator = CyclicDependencyValidator.create(editor.getInputFile());
        return displayResourceInput(ResourceType.LAYOUT.getName(), null, validator);
    }

    @Override
    public void select(final @NonNull Collection<INode> nodes) {
        LayoutCanvas layoutCanvas = mRulesEngine.getEditor().getCanvasControl();
        final SelectionManager selectionManager = layoutCanvas.getSelectionManager();
        selectionManager.select(nodes);
        // ALSO run an async select since immediately after nodes are created they
        // may not be selectable. We can't ONLY run an async exec since
        // code may depend on operating on the selection.
        layoutCanvas.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                selectionManager.select(nodes);
            }
        });
    }

    @Override
    public String displayFragmentSourceInput() {
        try {
            // Compute a search scope: We need to merge all the subclasses
            // android.app.Fragment and android.support.v4.app.Fragment
            IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
            IProject project = mRulesEngine.getProject();
            final IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);
            if (javaProject != null) {
                IType oldFragmentType = javaProject.findType(CLASS_V4_FRAGMENT);

                // First check to make sure fragments are available, and if not,
                // warn the user.
                IAndroidTarget target = Sdk.getCurrent().getTarget(project);
                // No, this should be using the min SDK instead!
                if (target.getVersion().getApiLevel() < 11 && oldFragmentType == null) {
                    // Compatibility library must be present
                    MessageDialog dialog =
                        new MessageDialog(
                                Display.getCurrent().getActiveShell(),
                          "Fragment Warning",
                          null,
                          "Fragments require API level 11 or higher, or a compatibility "
                          + "library for older versions.\n\n"
                          + " Do you want to install the compatibility library?",
                          MessageDialog.QUESTION,
                          new String[] { "Install", "Cancel" },
                          1 /* default button: Cancel */);
                      int answer = dialog.open();
                      if (answer == 0) {
                          if (!AddSupportJarAction.install(project)) {
                              return null;
                          }
                      } else {
                          return null;
                      }
                }

                // Look up sub-types of each (new fragment class and compatibility fragment
                // class, if any) and merge the two arrays - then create a scope from these
                // elements.
                IType[] fragmentTypes = new IType[0];
                IType[] oldFragmentTypes = new IType[0];
                if (oldFragmentType != null) {
                    ITypeHierarchy hierarchy =
                        oldFragmentType.newTypeHierarchy(new NullProgressMonitor());
                    oldFragmentTypes = hierarchy.getAllSubtypes(oldFragmentType);
                }
                IType fragmentType = javaProject.findType(CLASS_FRAGMENT);
                if (fragmentType != null) {
                    ITypeHierarchy hierarchy =
                        fragmentType.newTypeHierarchy(new NullProgressMonitor());
                    fragmentTypes = hierarchy.getAllSubtypes(fragmentType);
                }
                IType[] subTypes = new IType[fragmentTypes.length + oldFragmentTypes.length];
                System.arraycopy(fragmentTypes, 0, subTypes, 0, fragmentTypes.length);
                System.arraycopy(oldFragmentTypes, 0, subTypes, fragmentTypes.length,
                        oldFragmentTypes.length);
                scope = SearchEngine.createJavaSearchScope(subTypes, IJavaSearchScope.SOURCES);
            }

            Shell parent = AdtPlugin.getShell();
            final AtomicReference<String> returnValue =
                new AtomicReference<String>();
            final AtomicReference<SelectionDialog> dialogHolder =
                new AtomicReference<SelectionDialog>();
            final SelectionDialog dialog = JavaUI.createTypeDialog(
                    parent,
                    new ProgressMonitorDialog(parent),
                    scope,
                    IJavaElementSearchConstants.CONSIDER_CLASSES, false,
                    // Use ? as a default filter to fill dialog with matches
                    "?", //$NON-NLS-1$
                    new TypeSelectionExtension() {
                        @Override
                        public Control createContentArea(Composite parentComposite) {
                            Composite composite = new Composite(parentComposite, SWT.NONE);
                            composite.setLayout(new GridLayout(1, false));
                            Button button = new Button(composite, SWT.PUSH);
                            button.setText("Create New...");
                            button.addSelectionListener(new SelectionAdapter() {
                                @Override
                                public void widgetSelected(SelectionEvent e) {
                                    String fqcn = createNewFragmentClass(javaProject);
                                    if (fqcn != null) {
                                        returnValue.set(fqcn);
                                        dialogHolder.get().close();
                                    }
                                }
                            });
                            return composite;
                        }

                        @Override
                        public ITypeInfoFilterExtension getFilterExtension() {
                            return new ITypeInfoFilterExtension() {
                                @Override
                                public boolean select(ITypeInfoRequestor typeInfoRequestor) {
                                    int modifiers = typeInfoRequestor.getModifiers();
                                    if (!Flags.isPublic(modifiers)
                                            || Flags.isInterface(modifiers)
                                            || Flags.isEnum(modifiers)
                                            || Flags.isAbstract(modifiers)) {
                                        return false;
                                    }
                                    return true;
                                }
                            };
                        }
                    });
            dialogHolder.set(dialog);

            dialog.setTitle("Choose Fragment Class");
            dialog.setMessage("Select a Fragment class (? = any character, * = any string):");
            if (dialog.open() == IDialogConstants.CANCEL_ID) {
                return null;
            }
            if (returnValue.get() != null) {
                return returnValue.get();
            }

            Object[] types = dialog.getResult();
            if (types != null && types.length > 0) {
                return ((IType) types[0]).getFullyQualifiedName();
            }
        } catch (JavaModelException e) {
            AdtPlugin.log(e, null);
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
        }
        return null;
    }

    @Override
    public String displayCustomViewClassInput() {
        try {
            IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
            IProject project = mRulesEngine.getProject();
            final IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);
            if (javaProject != null) {
                // Look up sub-types of each (new fragment class and compatibility fragment
                // class, if any) and merge the two arrays - then create a scope from these
                // elements.
                IType[] viewTypes = new IType[0];
                IType fragmentType = javaProject.findType(CLASS_VIEW);
                if (fragmentType != null) {
                    ITypeHierarchy hierarchy =
                        fragmentType.newTypeHierarchy(new NullProgressMonitor());
                    viewTypes = hierarchy.getAllSubtypes(fragmentType);
                }
                scope = SearchEngine.createJavaSearchScope(viewTypes, IJavaSearchScope.SOURCES);
            }

            Shell parent = AdtPlugin.getShell();
            final AtomicReference<String> returnValue =
                new AtomicReference<String>();
            final AtomicReference<SelectionDialog> dialogHolder =
                new AtomicReference<SelectionDialog>();
            final SelectionDialog dialog = JavaUI.createTypeDialog(
                    parent,
                    new ProgressMonitorDialog(parent),
                    scope,
                    IJavaElementSearchConstants.CONSIDER_CLASSES, false,
                    // Use ? as a default filter to fill dialog with matches
                    "?", //$NON-NLS-1$
                    new TypeSelectionExtension() {
                        @Override
                        public Control createContentArea(Composite parentComposite) {
                            Composite composite = new Composite(parentComposite, SWT.NONE);
                            composite.setLayout(new GridLayout(1, false));
                            Button button = new Button(composite, SWT.PUSH);
                            button.setText("Create New...");
                            button.addSelectionListener(new SelectionAdapter() {
                                @Override
                                public void widgetSelected(SelectionEvent e) {
                                    String fqcn = createNewCustomViewClass(javaProject);
                                    if (fqcn != null) {
                                        returnValue.set(fqcn);
                                        dialogHolder.get().close();
                                    }
                                }
                            });
                            return composite;
                        }

                        @Override
                        public ITypeInfoFilterExtension getFilterExtension() {
                            return new ITypeInfoFilterExtension() {
                                @Override
                                public boolean select(ITypeInfoRequestor typeInfoRequestor) {
                                    int modifiers = typeInfoRequestor.getModifiers();
                                    if (!Flags.isPublic(modifiers)
                                            || Flags.isInterface(modifiers)
                                            || Flags.isEnum(modifiers)
                                            || Flags.isAbstract(modifiers)) {
                                        return false;
                                    }
                                    return true;
                                }
                            };
                        }
                    });
            dialogHolder.set(dialog);

            dialog.setTitle("Choose Custom View Class");
            dialog.setMessage("Select a Custom View class (? = any character, * = any string):");
            if (dialog.open() == IDialogConstants.CANCEL_ID) {
                return null;
            }
            if (returnValue.get() != null) {
                return returnValue.get();
            }

            Object[] types = dialog.getResult();
            if (types != null && types.length > 0) {
                return ((IType) types[0]).getFullyQualifiedName();
            }
        } catch (JavaModelException e) {
            AdtPlugin.log(e, null);
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
        }
        return null;
    }

    @Override
    public void redraw() {
        mRulesEngine.getEditor().getCanvasControl().redraw();
    }

    @Override
    public void layout() {
        mRulesEngine.getEditor().recomputeLayout();
    }

    @Override
    public Map<INode, Rect> measureChildren(@NonNull INode parent,
            @Nullable IClientRulesEngine.AttributeFilter filter) {
        RenderService renderService = RenderService.create(mRulesEngine.getEditor());
        Map<INode, Rect> map = renderService.measureChildren(parent, filter);
        if (map == null) {
            map = Collections.emptyMap();
        }
        return map;
    }

    @Override
    public int pxToDp(int px) {
        ConfigurationChooser chooser = mRulesEngine.getEditor().getConfigurationChooser();
        float dpi = chooser.getConfiguration().getDensity().getDpiValue();
        return (int) (px * 160 / dpi);
    }

    @Override
    public int dpToPx(int dp) {
        ConfigurationChooser chooser = mRulesEngine.getEditor().getConfigurationChooser();
        float dpi = chooser.getConfiguration().getDensity().getDpiValue();
        return (int) (dp * dpi / 160);
    }

    @Override
    public int screenToLayout(int pixels) {
        return (int) (pixels / mRulesEngine.getEditor().getCanvasControl().getScale());
    }

    private String createNewFragmentClass(IJavaProject javaProject) {
        NewClassWizardPage page = new NewClassWizardPage();

        IProject project = mRulesEngine.getProject();
        Sdk sdk = Sdk.getCurrent();
        if (sdk == null) {
            return null;
        }
        IAndroidTarget target = sdk.getTarget(project);
        String superClass;
        if (target == null || target.getVersion().getApiLevel() < 11) {
            superClass = CLASS_V4_FRAGMENT;
        } else {
            superClass = CLASS_FRAGMENT;
        }
        page.setSuperClass(superClass, true /* canBeModified */);
        IPackageFragmentRoot root = ManifestInfo.getSourcePackageRoot(javaProject);
        if (root != null) {
            page.setPackageFragmentRoot(root, true /* canBeModified */);
        }
        ManifestInfo manifestInfo = ManifestInfo.get(project);
        IPackageFragment pkg = manifestInfo.getPackageFragment();
        if (pkg != null) {
            page.setPackageFragment(pkg, true /* canBeModified */);
        }
        OpenNewClassWizardAction action = new OpenNewClassWizardAction();
        action.setConfiguredWizardPage(page);
        action.run();
        IType createdType = page.getCreatedType();
        if (createdType != null) {
            return createdType.getFullyQualifiedName();
        } else {
            return null;
        }
    }

    private String createNewCustomViewClass(IJavaProject javaProject) {
        NewClassWizardPage page = new NewClassWizardPage();

        IProject project = mRulesEngine.getProject();
        String superClass = CLASS_VIEW;
        page.setSuperClass(superClass, true /* canBeModified */);
        IPackageFragmentRoot root = ManifestInfo.getSourcePackageRoot(javaProject);
        if (root != null) {
            page.setPackageFragmentRoot(root, true /* canBeModified */);
        }
        ManifestInfo manifestInfo = ManifestInfo.get(project);
        IPackageFragment pkg = manifestInfo.getPackageFragment();
        if (pkg != null) {
            page.setPackageFragment(pkg, true /* canBeModified */);
        }
        OpenNewClassWizardAction action = new OpenNewClassWizardAction();
        action.setConfiguredWizardPage(page);
        action.run();
        IType createdType = page.getCreatedType();
        if (createdType != null) {
            return createdType.getFullyQualifiedName();
        } else {
            return null;
        }
    }

    @Override
    public @NonNull String getUniqueId(@NonNull String fqcn) {
        UiDocumentNode root = mRulesEngine.getEditor().getModel();
        String prefix = fqcn.substring(fqcn.lastIndexOf('.') + 1);
        prefix = Character.toLowerCase(prefix.charAt(0)) + prefix.substring(1);
        return DescriptorsUtils.getFreeWidgetId(root, prefix);
    }

    @Override
    public @NonNull String getAppNameSpace() {
        IProject project = mRulesEngine.getEditor().getProject();

        ProjectState projectState = Sdk.getProjectState(project);
        if (projectState != null && projectState.isLibrary()) {
            return AUTO_URI;
        }

        ManifestInfo info = ManifestInfo.get(project);
        return URI_PREFIX + info.getPackage();
    }
}
