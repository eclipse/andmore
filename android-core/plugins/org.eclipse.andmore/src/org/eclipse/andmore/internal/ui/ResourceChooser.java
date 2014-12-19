/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.eclipse.andmore.internal.ui;

import static com.android.SdkConstants.ANDROID_PREFIX;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.resources.ResourceItem;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.resources.ResourceResolver;
import com.android.resources.ResourceType;
import com.android.utils.Pair;
import com.google.common.collect.Maps;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.assetstudio.OpenCreateAssetSetWizardAction;
import org.eclipse.andmore.internal.editors.layout.gle2.GraphicalEditorPart;
import org.eclipse.andmore.internal.editors.layout.properties.PropertyFactory;
import org.eclipse.andmore.internal.refactorings.extractstring.ExtractStringRefactoring;
import org.eclipse.andmore.internal.refactorings.extractstring.ExtractStringWizard;
import org.eclipse.andmore.internal.resources.ResourceHelper;
import org.eclipse.andmore.internal.resources.ResourceNameValidator;
import org.eclipse.andmore.internal.resources.manager.ProjectResources;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.window.Window;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.AbstractElementListSelectionDialog;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A dialog to let the user select a resource based on a resource type.
 */
public class ResourceChooser extends AbstractElementListSelectionDialog implements ModifyListener {
    /** The return code from the dialog for the user choosing "Clear" */
    public static final int CLEAR_RETURN_CODE = -5;
    /** The dialog button ID for the user choosing "Clear" */
    private static final int CLEAR_BUTTON_ID = CLEAR_RETURN_CODE;

    private Pattern mProjectResourcePattern;
    private ResourceType mResourceType;
    private final List<ResourceRepository> mProjectResources;
    private final ResourceRepository mFrameworkResources;
    private Pattern mSystemResourcePattern;
    private Button mProjectButton;
    private Button mSystemButton;
    private Button mNewButton;
    private String mCurrentResource;
    private final IProject mProject;
    private IInputValidator mInputValidator;

    /** Helper object used to draw previews for drawables and colors. */
    private ResourcePreviewHelper mPreviewHelper;

    /**
     * Textfield for editing the actual returned value, updated when selection
     * changes. Only shown if {@link #mShowValueText} is true.
     */
    private Text mEditValueText;

    /**
     * Whether the {@link #mEditValueText} textfield should be shown when the dialog is created.
     */
    private boolean mShowValueText;

    /**
     * Flag indicating whether it's the first time {@link #handleSelectionChanged()} is called.
     * This is used to filter out the first selection event, always called by the superclass
     * when the widget is created, to distinguish between "the dialog was created" and
     * "the user clicked on a selection result", since only the latter should wipe out the
     * manual user edit shown in the value text.
     */
    private boolean mFirstSelect = true;

    /**
     * Label used to show the resolved value in the resource chooser. Only shown
     * if the {@link #mResourceResolver} field is set.
     */
    private Label mResolvedLabel;

    /** Resource resolver used to show actual values for resources selected. (Optional). */
    private ResourceResolver mResourceResolver;

    /**
     * Creates a Resource Chooser dialog.
     * @param project Project being worked on
     * @param type The type of the resource to choose
     * @param projectResources The repository for the project
     * @param frameworkResources The Framework resource repository
     * @param parent the parent shell
     */
    private ResourceChooser(
            @NonNull IProject project,
            @NonNull ResourceType type,
            @NonNull List<ResourceRepository> projectResources,
            @Nullable ResourceRepository frameworkResources,
            @NonNull Shell parent) {
        super(parent, new ResourceLabelProvider());
        mProject = project;

        mResourceType = type;
        mProjectResources = projectResources;
        mFrameworkResources = frameworkResources;

        mProjectResourcePattern = Pattern.compile(
                PREFIX_RESOURCE_REF + mResourceType.getName() + "/(.+)"); //$NON-NLS-1$

        mSystemResourcePattern = Pattern.compile(
                ANDROID_PREFIX + mResourceType.getName() + "/(.+)"); //$NON-NLS-1$

        setTitle("Resource Chooser");
        setMessage(String.format("Choose a %1$s resource",
                mResourceType.getDisplayName().toLowerCase(Locale.US)));
    }

    /**
     * Creates a new {@link ResourceChooser}
     *
     * @param editor the associated layout editor
     * @param type the resource type to choose
     * @return a new {@link ResourceChooser}
     */
    @NonNull
    public static ResourceChooser create(
            @NonNull GraphicalEditorPart editor,
            @NonNull ResourceType type) {
        IProject project = editor.getProject();
        Shell parent = editor.getCanvasControl().getShell();
        AndroidTargetData targetData = editor.getEditorDelegate().getEditor().getTargetData();
        ResourceChooser chooser = create(project, type, targetData, parent);

        // When editing Strings, allow editing the value text directly. When we
        // get inline editing support (where values entered directly into the
        // textual widget are translated automatically into a resource) this can
        // go away.
        if (type == ResourceType.STRING) {
            chooser.setResourceResolver(editor.getResourceResolver());
            chooser.setShowValueText(true);
        } else if (type == ResourceType.DIMEN || type == ResourceType.INTEGER) {
            chooser.setResourceResolver(editor.getResourceResolver());
        }

        chooser.setPreviewHelper(new ResourcePreviewHelper(chooser, editor));
        return chooser;
    }

    /**
     * Creates a new {@link ResourceChooser}
     *
     * @param project the associated project
     * @param type the resource type to choose
     * @param targetData the associated framework target data
     * @param parent the target shell
     * @return a new {@link ResourceChooser}
     */
    @NonNull
    public static ResourceChooser create(
            @NonNull IProject project,
            @NonNull ResourceType type,
            @Nullable AndroidTargetData targetData,
            @NonNull Shell parent) {
        ResourceManager manager = ResourceManager.getInstance();

        List<ResourceRepository> projectResources = new ArrayList<ResourceRepository>();
        ProjectResources resources = manager.getProjectResources(project);
        projectResources.add(resources);

        // Add in library project resources
        ProjectState projectState = Sdk.getProjectState(project);
        if (projectState != null) {
            List<IProject> libraries = projectState.getFullLibraryProjects();
            if (libraries != null && !libraries.isEmpty()) {
                for (IProject library : libraries) {
                    projectResources.add(manager.getProjectResources(library));
                }
            }
        }

        ResourceRepository frameworkResources =
                targetData != null ? targetData.getFrameworkResources() : null;
        return new ResourceChooser(project, type, projectResources, frameworkResources, parent);
    }

    /**
     * Sets whether this dialog should show the value field as a separate text
     * value (and take the resulting value of the dialog from this text field
     * rather than from the selection)
     *
     * @param showValueText if true, show the value text field
     * @return this, for constructor chaining
     */
    public ResourceChooser setShowValueText(boolean showValueText) {
        mShowValueText = showValueText;

        return this;
    }

    /**
     * Sets the resource resolver to use to show resolved values for the current
     * selection
     *
     * @param resourceResolver the resource resolver to use
     * @return this, for constructor chaining
     */
    public ResourceChooser setResourceResolver(ResourceResolver resourceResolver) {
        mResourceResolver = resourceResolver;

        return this;
    }

    /**
     * Sets the {@link ResourcePreviewHelper} to use to preview drawable
     * resources, if any
     *
     * @param previewHelper the helper to use
     * @return this, for constructor chaining
     */
    public ResourceChooser setPreviewHelper(ResourcePreviewHelper previewHelper) {
        mPreviewHelper = previewHelper;

        return this;
    }

    /**
     * Sets the initial dialog size
     *
     * @param width the initial width
     * @param height the initial height
     * @return this, for constructor chaining
     */
    public ResourceChooser setInitialSize(int width, int height) {
        setSize(width, height);

        return this;
    }

    @Override
    public void create() {
        super.create();

        if (mShowValueText) {
            mEditValueText.selectAll();
            mEditValueText.setFocus();
        }
    }

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

    /**
     * Sets the currently selected item
     *
     * @param resource the resource url for the currently selected item
     * @return this, for constructor chaining
     */
    public ResourceChooser setCurrentResource(@Nullable String resource) {
        mCurrentResource = resource;

        if (mShowValueText && mEditValueText != null) {
            mEditValueText.setText(resource);
        }

        return this;
    }

    /**
     * Returns the currently selected url
     *
     * @return the currently selected url
     */
    @Nullable
    public String getCurrentResource() {
        return mCurrentResource;
    }

    /**
     * Sets the input validator to use, if any
     *
     * @param inputValidator the validator
     * @return this, for constructor chaining
     */
    public ResourceChooser setInputValidator(@Nullable IInputValidator inputValidator) {
        mInputValidator = inputValidator;

        return this;
    }

    @Override
    protected void computeResult() {
        if (mShowValueText) {
            mCurrentResource = mEditValueText.getText();
            if (mCurrentResource.length() == 0) {
                mCurrentResource = null;
            }
            return;
        }

        computeResultFromSelection();
    }

    private void computeResultFromSelection() {
        if (getSelectionIndex() == -1) {
            mCurrentResource = null;
            return;
        }

        Object[] elements = getSelectedElements();
        if (elements.length == 1 && elements[0] instanceof ResourceItem) {
            ResourceItem item = (ResourceItem)elements[0];

            mCurrentResource = item.getXmlString(mResourceType, mSystemButton.getSelection());

            if (mInputValidator != null && mInputValidator.isValid(mCurrentResource) != null) {
                mCurrentResource = null;
            }
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite top = (Composite)super.createDialogArea(parent);

        createMessageArea(top);

        createButtons(top);
        createFilterText(top);
        createFilteredList(top);

        // create the "New Resource" button
        createNewResButtons(top);

        // Optionally create the value text field, if {@link #mShowValueText} is true
        createValueField(top);

        setupResourceList();
        selectResourceString(mCurrentResource);

        return top;
    }

    /**
     * Creates the radio button to switch between project and system resources.
     * @param top the parent composite
     */
    private void createButtons(Composite top) {
        mProjectButton = new Button(top, SWT.RADIO);
        mProjectButton.setText("Project Resources");
        mProjectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                if (mProjectButton.getSelection()) {
                    // Clear selection before changing the list contents. This works around
                    // a bug in the superclass where switching to the framework resources,
                    // choosing one of the last resources, then switching to the project
                    // resources would cause an exception when calling getSelection() because
                    // selection state doesn't get cleared when we set new contents on
                    // the filtered list.
                    fFilteredList.setSelection(new int[0]);
                    setupResourceList();
                    updateNewButton(false /*isSystem*/);
                    updateValue();
                }
            }
        });
        mSystemButton = new Button(top, SWT.RADIO);
        mSystemButton.setText("System Resources");
        mSystemButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                if (mSystemButton.getSelection()) {
                    fFilteredList.setSelection(new int[0]);
                    setupResourceList();
                    updateNewButton(true /*isSystem*/);
                    updateValue();
                }
            }
        });
        if (mFrameworkResources == null) {
            mSystemButton.setVisible(false);
        }
    }

    /**
     * Creates the "New Resource" button.
     * @param top the parent composite
     */
    private void createNewResButtons(Composite top) {
        mNewButton = new Button(top, SWT.NONE);

        String title = String.format("New %1$s...", mResourceType.getDisplayName());
        if (mResourceType == ResourceType.DRAWABLE) {
            title = "Create New Icon...";
        }
        mNewButton.setText(title);

        mNewButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);

                if (mResourceType == ResourceType.STRING) {
                    // Special case: Use Extract String refactoring wizard UI
                    String newName = createNewString();
                    selectAddedItem(newName);
                } else if (mResourceType == ResourceType.DRAWABLE) {
                    // Special case: Use the "Create Icon Set" wizard
                    OpenCreateAssetSetWizardAction action =
                            new OpenCreateAssetSetWizardAction(mProject);
                    action.run();
                    List<IResource> files = action.getCreatedFiles();
                    if (files != null && files.size() > 0) {
                        String newName = AdtUtils.stripAllExtensions(files.get(0).getName());
                        // Recompute the "current resource" to select the new id
                        ResourceItem[] items = setupResourceList();
                        selectItemName(newName, items);
                    }
                } else {
                    if (ResourceHelper.isValueBasedResourceType(mResourceType)) {
                        String newName = createNewValue(mResourceType);
                        if (newName != null) {
                            selectAddedItem(newName);
                        }
                    } else {
                        String newName = createNewFile(mResourceType);
                        if (newName != null) {
                            selectAddedItem(newName);
                        }
                    }
                }
            }

            private void selectAddedItem(@NonNull String newName) {
                // Recompute the "current resource" to select the new id
                ResourceItem[] items = setupResourceList();

                // Ensure that the name is in the list. There's a delay after
                // an item is added (until the builder runs and processes the delta)
                // so if it's not in the list, add it
                boolean found = false;
                for (ResourceItem item : items) {
                    if (newName.equals(item.getName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    ResourceItem[] newItems = new ResourceItem[items.length + 1];
                    System.arraycopy(items, 0, newItems, 0, items.length);
                    newItems[items.length] = new ResourceItem(newName);
                    items = newItems;
                    Arrays.sort(items);
                    setListElements(items);
                    fFilteredList.setEnabled(newItems.length > 0);
                }

                selectItemName(newName, items);
            }
        });
    }

    /**
     * Creates the value text field.
     *
     * @param top the parent composite
     */
    private void createValueField(Composite top) {
        if (mShowValueText) {
            mEditValueText = new Text(top, SWT.BORDER);
            if (mCurrentResource != null) {
                mEditValueText.setText(mCurrentResource);
            }
            mEditValueText.addModifyListener(this);

            GridData data = new GridData();
            data.grabExcessVerticalSpace = false;
            data.grabExcessHorizontalSpace = true;
            data.horizontalAlignment = GridData.FILL;
            data.verticalAlignment = GridData.BEGINNING;
            mEditValueText.setLayoutData(data);
            mEditValueText.setFont(top.getFont());
        }

        if (mResourceResolver != null) {
            mResolvedLabel = new Label(top, SWT.NONE);
            GridData data = new GridData();
            data.grabExcessVerticalSpace = false;
            data.grabExcessHorizontalSpace = true;
            data.horizontalAlignment = GridData.FILL;
            data.verticalAlignment = GridData.BEGINNING;
            mResolvedLabel.setLayoutData(data);
        }

        Composite workaround = PropertyFactory.addWorkaround(top);
        if (workaround != null) {
            workaround.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
        }
    }

    private void updateResolvedLabel() {
        if (mResourceResolver == null) {
            return;
        }

        String v = null;
        if (mCurrentResource != null) {
            v = mCurrentResource;
            if (mCurrentResource.startsWith(PREFIX_RESOURCE_REF)) {
                ResourceValue value = mResourceResolver.findResValue(mCurrentResource, false);
                if (value != null) {
                    v = value.getValue();
                }
            }
        }

        if (v == null) {
            v = "";
        }

        mResolvedLabel.setText(String.format("Resolved Value: %1$s", v));
    }

    @Override
    protected void handleSelectionChanged() {
        super.handleSelectionChanged();
        if (mInputValidator != null) {
            Object[] elements = getSelectedElements();
            if (elements.length == 1 && elements[0] instanceof ResourceItem) {
                ResourceItem item = (ResourceItem)elements[0];
                String current = item.getXmlString(mResourceType, mSystemButton.getSelection());
                String error = mInputValidator.isValid(current);
                IStatus status;
                if (error != null) {
                    status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, error);
                } else {
                    status = new Status(IStatus.OK, AdtPlugin.PLUGIN_ID, null);
                }
                updateStatus(status);
            }
        }

        updateValue();
    }

    private void updateValue() {
        if (mPreviewHelper != null) {
            computeResult();
            mPreviewHelper.updatePreview(mResourceType, mCurrentResource);
        }

        if (mShowValueText) {
            if (mFirstSelect) {
                mFirstSelect = false;
                mEditValueText.selectAll();
            } else {
                computeResultFromSelection();
                mEditValueText.setText(mCurrentResource != null ? mCurrentResource : "");
            }
        }

        if (mResourceResolver != null) {
            if (!mShowValueText) {
                computeResultFromSelection();
            }
            updateResolvedLabel();
        }
    }

    @Nullable
    private String createNewFile(ResourceType type) {
        // Show a name/value dialog entering the key name and the value
        Shell shell = AdtPlugin.getShell();
        if (shell == null) {
            return null;
        }

        ResourceNameValidator validator = ResourceNameValidator.create(true /*allowXmlExtension*/,
                mProject, mResourceType);
        InputDialog d = new InputDialog(
                AdtPlugin.getShell(),
                "Enter name",  // title
                "Enter name",
                "", //$NON-NLS-1$
                validator);
        if (d.open() == Window.OK) {
            String name = d.getValue().trim();
            if (name.length() == 0) {
                return null;
            }

            Pair<IFile, IRegion> resource = ResourceHelper.createResource(mProject, type, name,
                    null);
            if (resource != null) {
                return name;
            }
        }

        return null;
    }


    @Nullable
    private String createNewValue(ResourceType type) {
        // Show a name/value dialog entering the key name and the value
        Shell shell = AdtPlugin.getShell();
        if (shell == null) {
            return null;
        }
        NameValueDialog dialog = new NameValueDialog(shell, getFilter());
        if (dialog.open() != Window.OK) {
            return null;
        }

        String name = dialog.getName();
        String value = dialog.getValue();
        if (name.length() == 0 || value.length() == 0) {
            return null;
        }

        Pair<IFile, IRegion> resource = ResourceHelper.createResource(mProject, type, name, value);
        if (resource != null) {
            return name;
        }

        return null;
    }

    private String createNewString() {
        ExtractStringRefactoring ref = new ExtractStringRefactoring(
                mProject, true /*enforceNew*/);
        RefactoringWizard wizard = new ExtractStringWizard(ref, mProject);
        RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
        try {
            IWorkbench w = PlatformUI.getWorkbench();
            if (op.run(w.getDisplay().getActiveShell(), wizard.getDefaultPageTitle()) ==
                    IDialogConstants.OK_ID) {
                return ref.getXmlStringId();
            }
        } catch (InterruptedException ex) {
            // Interrupted. Pass.
        }

        return null;
    }

    /**
     * Setups the current list.
     */
    private ResourceItem[] setupResourceList() {
        Collection<ResourceItem> items = null;
        if (mProjectButton.getSelection()) {
            if (mProjectResources.size() == 1) {
                items = mProjectResources.get(0).getResourceItemsOfType(mResourceType);
            } else {
                Map<String, ResourceItem> merged = Maps.newHashMapWithExpectedSize(200);
                for (ResourceRepository repository : mProjectResources) {
                    for (ResourceItem item : repository.getResourceItemsOfType(mResourceType)) {
                        if (!merged.containsKey(item.getName())) {
                            merged.put(item.getName(), item);
                        }
                    }
                }
                items = merged.values();
            }
        } else if (mSystemButton.getSelection()) {
            items = mFrameworkResources.getResourceItemsOfType(mResourceType);
        }

        if (items == null) {
            items = Collections.emptyList();
        }

        ResourceItem[] arrayItems = items.toArray(new ResourceItem[items.size()]);

        // sort the array
        Arrays.sort(arrayItems);

        setListElements(arrayItems);
        fFilteredList.setEnabled(arrayItems.length > 0);

        return arrayItems;
    }

    /**
     * Select an item by its name, if possible.
     */
    private void selectItemName(String itemName, ResourceItem[] items) {
        if (itemName == null || items == null) {
            return;
        }

        for (ResourceItem item : items) {
            if (itemName.equals(item.getName())) {
                setSelection(new Object[] { item });
                break;
            }
        }
    }

    /**
     * Select an item by its full resource string.
     * This also selects between project and system repository based on the resource string.
     */
    private void selectResourceString(String resourceString) {
        boolean isSystem = false;
        String itemName = null;

        if (resourceString != null) {
            // Is this a system resource?
            // If not a system resource or if they are not available, this will be a project res.
            Matcher m = mSystemResourcePattern.matcher(resourceString);
            if (m.matches()) {
                itemName = m.group(1);
                isSystem = true;
            }

            if (!isSystem && itemName == null) {
                // Try to match project resource name
                m = mProjectResourcePattern.matcher(resourceString);
                if (m.matches()) {
                    itemName = m.group(1);
                }
            }
        }

        // Update the repository selection
        mProjectButton.setSelection(!isSystem);
        mSystemButton.setSelection(isSystem);
        updateNewButton(isSystem);

        // Update the list
        ResourceItem[] items = setupResourceList();

        // If we have a selection name, select it
        if (itemName != null) {
            selectItemName(itemName, items);
        }
    }

    private void updateNewButton(boolean isSystem) {
        mNewButton.setEnabled(!isSystem && ResourceHelper.canCreateResourceType(mResourceType));
    }

    // ---- Implements ModifyListener ----

    @Override
    public void modifyText(ModifyEvent e) {
       if (e.getSource() == mEditValueText && mResourceResolver != null) {
           mCurrentResource = mEditValueText.getText();

           if (mCurrentResource.startsWith(PREFIX_RESOURCE_REF)) {
               if (mProjectResourcePattern.matcher(mCurrentResource).matches() ||
                       mSystemResourcePattern.matcher(mCurrentResource).matches()) {
                   updateResolvedLabel();
               }
           } else {
               updateResolvedLabel();
           }
       }
    }

    /** Dialog asking for a Name/Value pair */
    private class NameValueDialog extends SelectionStatusDialog implements Listener {
        private org.eclipse.swt.widgets.Text mNameText;
        private org.eclipse.swt.widgets.Text mValueText;
        private String mInitialName;
        private String mName;
        private String mValue;
        private ResourceNameValidator mValidator;

        public NameValueDialog(Shell parent, String initialName) {
            super(parent);
            mInitialName = initialName;
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(2, false));
            GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
            // Wide enough to accommodate the error label
            gridData.widthHint = 500;
            container.setLayoutData(gridData);


            Label nameLabel = new Label(container, SWT.NONE);
            nameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
            nameLabel.setText("Name:");

            mNameText = new org.eclipse.swt.widgets.Text(container, SWT.BORDER);
            mNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
            if (mInitialName != null) {
                mNameText.setText(mInitialName);
                mNameText.selectAll();
            }

            Label valueLabel = new Label(container, SWT.NONE);
            valueLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
            valueLabel.setText("Value:");

            mValueText = new org.eclipse.swt.widgets.Text(container, SWT.BORDER);
            mValueText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

            mNameText.addListener(SWT.Modify, this);
            mValueText.addListener(SWT.Modify, this);

            validate();

            return container;
        }

        @Override
        protected void computeResult() {
            mName = mNameText.getText().trim();
            mValue = mValueText.getText().trim();
        }

        private String getName() {
            return mName;
        }

        private String getValue() {
            return mValue;
        }

        @Override
        public void handleEvent(Event event) {
            validate();
        }

        private void validate() {
            IStatus status;
            computeResult();
            if (mName.length() == 0) {
                status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, "Enter a name");
            } else if (mValue.length() == 0) {
                status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, "Enter a value");
            } else {
                if (mValidator == null) {
                    mValidator = ResourceNameValidator.create(false, mProject, mResourceType);
                }
                String error = mValidator.isValid(mName);
                if (error != null) {
                    status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, error);
                } else {
                    status = new Status(IStatus.OK, AdtPlugin.PLUGIN_ID, null);
                }
            }
            updateStatus(status);
        }
    }

    /**
     * Open the resource chooser for the given type, associated with the given
     * editor
     *
     * @param graphicalEditor the editor associated with the resource to be
     *            chosen (used to find the associated Android target to be used
     *            for framework resources etc)
     * @param type the resource type to be chosen
     * @param currentValue the current value, or null
     * @param validator a validator to be used, or null
     * @return the chosen resource, null if cancelled and "" if value should be
     *         cleared
     */
    public static String chooseResource(
            @NonNull GraphicalEditorPart graphicalEditor,
            @NonNull ResourceType type,
            String currentValue, IInputValidator validator) {
        ResourceChooser chooser = create(graphicalEditor, type).
                setCurrentResource(currentValue);
        if (validator != null) {
            // Ensure wide enough to accommodate validator error message
            chooser.setSize(85, 10);
            chooser.setInputValidator(validator);
        }
        int result = chooser.open();
        if (result == ResourceChooser.CLEAR_RETURN_CODE) {
            return ""; //$NON-NLS-1$
        } else if (result == Window.OK) {
            return chooser.getCurrentResource();
        }

        return null;
    }
}
