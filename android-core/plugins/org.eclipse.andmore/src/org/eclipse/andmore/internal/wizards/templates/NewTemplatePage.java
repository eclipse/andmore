/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.internal.wizards.templates;

import static com.android.SdkConstants.CLASS_ACTIVITY;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_MIN_API;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_MIN_BUILD_API;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_DEFAULT;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_ID;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_NAME;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.PREVIEW_PADDING;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.PREVIEW_WIDTH;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.LintUtils;
import com.google.common.collect.Lists;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.layout.gle2.ImageControl;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.project.ProjectChooserHelper;
import org.eclipse.andmore.internal.project.ProjectChooserHelper.ProjectCombo;
import org.eclipse.andmore.internal.wizards.templates.Parameter.Constraint;
import org.eclipse.andmore.internal.wizards.templates.Parameter.Type;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.dialogs.ITypeInfoFilterExtension;
import org.eclipse.jdt.ui.dialogs.ITypeInfoRequestor;
import org.eclipse.jdt.ui.dialogs.TypeSelectionExtension;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * First wizard page in the "New Project From Template" wizard (which is parameterized
 * via template.xml files)
 */
public class NewTemplatePage extends WizardPage
        implements ModifyListener, SelectionListener, FocusListener {
    /** The default width to use for the wizard page */
    static final int WIZARD_PAGE_WIDTH = 600;

    private final NewTemplateWizardState mValues;
    private final boolean mChooseProject;
    private int mCustomMinSdk = -1;
    private int mCustomBuildApi = -1;
    private boolean mIgnore;
    private boolean mShown;
    private Control mFirst;
    // TODO: Move decorators to the Parameter objects?
    private Map<String, ControlDecoration> mDecorations = new HashMap<String, ControlDecoration>();
    private Label mHelpIcon;
    private Label mTipLabel;
    private ImageControl mPreview;
    private Image mPreviewImage;
    private boolean mDisposePreviewImage;
    private ProjectCombo mProjectButton;
    private StringEvaluator mEvaluator;

    private TemplateMetadata mShowingTemplate;

    /**
     * Creates a new {@link NewTemplatePage}
     *
     * @param values the wizard state
     * @param chooseProject whether the wizard should present a project chooser,
     *            and update {@code values}' project field
     */
    NewTemplatePage(NewTemplateWizardState values, boolean chooseProject) {
        super("newTemplatePage"); //$NON-NLS-1$
        mValues = values;
        mChooseProject = chooseProject;
    }

    /**
     * @param minSdk a minimum SDK to use, provided chooseProject is false. If
     *            it is true, then the minimum SDK used for validation will be
     *            the one of the project
     * @param buildApi the build API to use
     */
    void setCustomMinSdk(int minSdk, int buildApi) {
        assert !mChooseProject;
        //assert buildApi >= minSdk;
        mCustomMinSdk = minSdk;
        mCustomBuildApi = buildApi;
    }

    @Override
    public void createControl(Composite parent2) {
        Composite parent = new Composite(parent2, SWT.NULL);
        setControl(parent);
        GridLayout parentLayout = new GridLayout(3, false);
        parentLayout.verticalSpacing = 0;
        parentLayout.marginWidth = 0;
        parentLayout.marginHeight = 0;
        parentLayout.horizontalSpacing = 0;
        parent.setLayout(parentLayout);

        // Reserve enough width (since the panel is created lazily later)
        Label label = new Label(parent, SWT.NONE);
        GridData data = new GridData();
        data.widthHint = WIZARD_PAGE_WIDTH;
        label.setLayoutData(data);
    }

    @SuppressWarnings("unused") // SWT constructors have side effects and aren't unused
    private void onEnter() {
        TemplateMetadata template = mValues.getTemplateHandler().getTemplate();
        if (template == mShowingTemplate) {
            return;
        }
        mShowingTemplate = template;

        Composite parent = (Composite) getControl();

        Control[] children = parent.getChildren();
        if (children.length > 0) {
            for (Control c : parent.getChildren()) {
                c.dispose();
            }
            for (ControlDecoration decoration : mDecorations.values()) {
                decoration.dispose();
            }
            mDecorations.clear();
        }

        Composite container = new Composite(parent, SWT.NULL);
        container.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        GridLayout gl_container = new GridLayout(3, false);
        gl_container.horizontalSpacing = 10;
        container.setLayout(gl_container);

        if (mChooseProject) {
            // Project: [button]
            String tooltip = "The Android Project where the new resource will be created.";
            Label projectLabel = new Label(container, SWT.NONE);
            projectLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
            projectLabel.setText("Project:");
            projectLabel.setToolTipText(tooltip);

            ProjectChooserHelper helper =
                    new ProjectChooserHelper(getShell(), null /* filter */);
            mProjectButton = new ProjectCombo(helper, container, mValues.project);
            mProjectButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
            mProjectButton.setToolTipText(tooltip);
            mProjectButton.addSelectionListener(this);

            //Label projectSeparator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
            //projectSeparator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));
        }

        // Add parameters
        mFirst = null;
        String thumb = null;
        if (template != null) {
            thumb = template.getThumbnailPath();
            String title = template.getTitle();
            if (title != null && !title.isEmpty()) {
                setTitle(title);
            }
            String description = template.getDescription();
            if (description != null && !description.isEmpty()) {
                setDescription(description);
            }

            Map<String, String> defaults = mValues.defaults;
            Set<String> seen = null;
            if (LintUtils.assertionsEnabled()) {
                seen = new HashSet<String>();
            }

            List<Parameter> parameters = template.getParameters();
            for (Parameter parameter : parameters) {
                Parameter.Type type = parameter.type;

                if (type == Parameter.Type.SEPARATOR) {
                    Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
                    separator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));
                    continue;
                }

                String id = parameter.id;
                assert id != null && !id.isEmpty() : ATTR_ID;
                Object value = defaults.get(id);
                if (value == null) {
                    value = parameter.value;
                }

                String name = parameter.name;
                String help = parameter.help;

                // Required
                assert name != null && !name.isEmpty() : ATTR_NAME;
                // Ensure id's are unique:
                assert seen != null && seen.add(id) : id;

                // Skip attributes that were already provided by the surrounding
                // context. For example, when adding into an existing project,
                // provide the minimum SDK automatically from the project.
                if (mValues.hidden != null && mValues.hidden.contains(id)) {
                    continue;
                }

                switch (type) {
                    case STRING: {
                        // TODO: Look at the constraints to add validators here
                        // TODO: If I type.equals("layout") add resource validator for layout
                        // names
                        // TODO: If I type.equals("class") make class validator

                        // TODO: Handle package and id better later
                        Label label = new Label(container, SWT.NONE);
                        label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false,
                                1, 1));
                        label.setText(name);

                        Text text = new Text(container, SWT.BORDER);
                        text.setData(parameter);
                        parameter.control = text;

                        if (parameter.constraints.contains(Constraint.EXISTS)) {
                            text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,
                                    1, 1));

                            Button button = new Button(container, SWT.FLAT);
                            button.setData(parameter);
                            button.setText("...");
                            button.addSelectionListener(this);
                        } else {
                            text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,
                                    2, 1));
                        }

                        boolean hasValue = false;
                        if (value instanceof String) {
                            String stringValue = (String) value;
                            hasValue = !stringValue.isEmpty();
                            text.setText(stringValue);
                            mValues.parameters.put(id, value);
                        }

                        if (!hasValue) {
                            if (parameter.constraints.contains(Constraint.EMPTY)) {
                                text.setMessage("Optional");
                            } else if (parameter.constraints.contains(Constraint.NONEMPTY)) {
                                text.setMessage("Required");
                            }
                        }

                        text.addModifyListener(this);
                        text.addFocusListener(this);

                        if (mFirst == null) {
                            mFirst = text;
                        }

                        if (help != null && !help.isEmpty()) {
                            text.setToolTipText(help);
                            ControlDecoration decoration = createFieldDecoration(id, text, help);
                        }
                        break;
                    }
                    case BOOLEAN: {
                        Label label = new Label(container, SWT.NONE);
                        label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false,
                                1, 1));

                        Button checkBox = new Button(container, SWT.CHECK);
                        checkBox.setText(name);
                        checkBox.setData(parameter);
                        parameter.control = checkBox;
                        checkBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,
                                2, 1));

                        if (value instanceof Boolean) {
                            Boolean selected = (Boolean) value;
                            checkBox.setSelection(selected);
                            mValues.parameters.put(id, value);
                        }

                        checkBox.addSelectionListener(this);
                        checkBox.addFocusListener(this);

                        if (mFirst == null) {
                            mFirst = checkBox;
                        }

                        if (help != null && !help.isEmpty()) {
                            checkBox.setToolTipText(help);
                            ControlDecoration decoration = createFieldDecoration(id, checkBox,
                                    help);
                        }
                        break;
                    }
                    case ENUM: {
                        Label label = new Label(container, SWT.NONE);
                        label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false,
                                1, 1));
                        label.setText(name);

                        Combo combo = createOptionCombo(parameter, container, mValues.parameters,
                                this, this);
                        combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,
                                2, 1));

                        if (mFirst == null) {
                            mFirst = combo;
                        }

                        if (help != null && !help.isEmpty()) {
                            ControlDecoration decoration = createFieldDecoration(id, combo, help);
                        }
                        break;
                    }
                    case SEPARATOR:
                        // Already handled above
                        assert false : type;
                        break;
                    default:
                        assert false : type;
                }
            }
        }

        // Preview
        mPreview = new ImageControl(parent, SWT.NONE, null);
        mPreview.setDisposeImage(false); // Handled manually in this class
        GridData gd_mImage = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
        gd_mImage.widthHint = PREVIEW_WIDTH + 2 * PREVIEW_PADDING;
        mPreview.setLayoutData(gd_mImage);

        Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData separatorData = new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1);
        separatorData.heightHint = 16;
        separator.setLayoutData(separatorData);

        // Generic help
        mHelpIcon = new Label(parent, SWT.NONE);
        mHelpIcon.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));
        Image icon = IconFactory.getInstance().getIcon("quickfix");
        mHelpIcon.setImage(icon);
        mHelpIcon.setVisible(false);
        mTipLabel = new Label(parent, SWT.WRAP);
        mTipLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        setPreview(thumb);

        parent.layout(true, true);
        // TODO: This is a workaround for the fact that (at least on OSX) you end up
        // with some visual artifacts from the control decorations in the upper left corner
        // (outside the parent widget itself) from the initial control decoration placement
        // prior to layout. Therefore, perform a redraw. A better solution would be to
        // delay creation of the control decorations until layout has been performed.
        // Let's do that soon.
        parent.getParent().redraw();
    }

    @NonNull
    static Combo createOptionCombo(
            @NonNull Parameter parameter,
            @NonNull Composite container,
            @NonNull Map<String, Object> valueMap,
            @NonNull SelectionListener selectionListener,
            @NonNull FocusListener focusListener) {
        Combo combo = new Combo(container, SWT.READ_ONLY);

        List<Element> options = parameter.getOptions();
        assert options.size() > 0;
        int selected = 0;
        List<String> ids = Lists.newArrayList();
        List<Integer> minSdks = Lists.newArrayList();
        List<Integer> minBuildApis = Lists.newArrayList();
        List<String> labels = Lists.newArrayList();
        for (int i = 0, n = options.size(); i < n; i++) {
            Element option = options.get(i);
            String optionId = option.getAttribute(ATTR_ID);
            assert optionId != null && !optionId.isEmpty() : ATTR_ID;
            String isDefault = option.getAttribute(ATTR_DEFAULT);
            if (isDefault != null && !isDefault.isEmpty() &&
                    Boolean.valueOf(isDefault)) {
                selected = i;
            }
            NodeList childNodes = option.getChildNodes();
            assert childNodes.getLength() == 1 &&
                    childNodes.item(0).getNodeType() == Node.TEXT_NODE;
            String optionLabel = childNodes.item(0).getNodeValue().trim();

            String minApiString = option.getAttribute(ATTR_MIN_API);
            int minSdk = 1;
            if (minApiString != null && !minApiString.isEmpty()) {
                try {
                    minSdk = Integer.parseInt(minApiString);
                } catch (NumberFormatException nufe) {
                    // Templates aren't allowed to contain codenames, should
                    // always be an integer
                    AdtPlugin.log(nufe, null);
                    minSdk = 1;
                }
            }
            String minBuildApiString = option.getAttribute(ATTR_MIN_BUILD_API);
            int minBuildApi = 1;
            if (minBuildApiString != null && !minBuildApiString.isEmpty()) {
                try {
                    minBuildApi = Integer.parseInt(minBuildApiString);
                } catch (NumberFormatException nufe) {
                    // Templates aren't allowed to contain codenames, should
                    // always be an integer
                    AdtPlugin.log(nufe, null);
                    minBuildApi = 1;
                }
            }
            minSdks.add(minSdk);
            minBuildApis.add(minBuildApi);
            ids.add(optionId);
            labels.add(optionLabel);
        }
        combo.setData(parameter);
        parameter.control = combo;
        combo.setData(ATTR_ID, ids.toArray(new String[ids.size()]));
        combo.setData(ATTR_MIN_API, minSdks.toArray(new Integer[minSdks.size()]));
        combo.setData(ATTR_MIN_BUILD_API, minBuildApis.toArray(
                new Integer[minBuildApis.size()]));
        assert labels.size() > 0;
        combo.setItems(labels.toArray(new String[labels.size()]));
        combo.select(selected);

        combo.addSelectionListener(selectionListener);
        combo.addFocusListener(focusListener);

        valueMap.put(parameter.id, ids.get(selected));

        if (parameter.help != null && !parameter.help.isEmpty()) {
            combo.setToolTipText(parameter.help);
        }

        return  combo;
    }

    private void setPreview(String thumb) {
        Image oldImage = mPreviewImage;
        boolean dispose = mDisposePreviewImage;
        mPreviewImage = null;

        if (thumb == null || thumb.isEmpty()) {
            mPreviewImage = TemplateMetadata.getDefaultTemplateIcon();
            mDisposePreviewImage = false;
        } else {
            byte[] data = mValues.getTemplateHandler().readTemplateResource(thumb);
            if (data != null) {
                try {
                    mPreviewImage = new Image(getControl().getDisplay(),
                            new ByteArrayInputStream(data));
                    mDisposePreviewImage = true;
                } catch (Exception e) {
                    AdtPlugin.log(e, null);
                }
            }
            if (mPreviewImage == null) {
                return;
            }
        }

        mPreview.setImage(mPreviewImage);
        mPreview.fitToWidth(PREVIEW_WIDTH);

        if (oldImage != null && dispose) {
            oldImage.dispose();
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        if (mPreviewImage != null && mDisposePreviewImage) {
            mDisposePreviewImage = false;
            mPreviewImage.dispose();
            mPreviewImage = null;
        }
    }

    private ControlDecoration createFieldDecoration(String id, Control control,
            String description) {
        ControlDecoration decoration = new ControlDecoration(control, SWT.LEFT);
        decoration.setMarginWidth(2);
        FieldDecoration errorFieldIndicator = FieldDecorationRegistry.getDefault().
           getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
        decoration.setImage(errorFieldIndicator.getImage());
        decoration.setDescriptionText(description);
        control.setToolTipText(description);
        mDecorations.put(id, decoration);

        return decoration;
    }

    @Override
    public boolean isPageComplete() {
        // Force user to reach this page before hitting Finish
        return mShown && super.isPageComplete();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            onEnter();
        }

        super.setVisible(visible);

        if (mFirst != null) {
            mFirst.setFocus();
        }

        if (visible) {
            mShown = true;
        }

        validatePage();
    }

    /** Returns the parameter associated with the given control */
    @Nullable
    static Parameter getParameter(Control control) {
        return (Parameter) control.getData();
    }

    /**
     * Returns the current string evaluator, if any
     *
     * @return the evaluator or null
     */
    @Nullable
    public StringEvaluator getEvaluator() {
        return mEvaluator;
    }

    // ---- Validation ----

    private void validatePage() {
        int minSdk = getMinSdk();
        int buildApi = getBuildApi();
        IStatus status = mValues.getTemplateHandler().validateTemplate(minSdk, buildApi);

        if (status == null || status.isOK()) {
            if (mChooseProject && mValues.project == null) {
                status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                        "Please select an Android project.");
            }
        }

        for (Parameter parameter : mShowingTemplate.getParameters()) {
            if (parameter.type == Parameter.Type.SEPARATOR) {
                continue;
            }
            IInputValidator validator = parameter.getValidator(mValues.project);
            if (validator != null) {
               ControlDecoration decoration = mDecorations.get(parameter.id);
               String value = parameter.value == null ? "" : parameter.value.toString();
               String error = validator.isValid(value);
               if (error != null) {
                   IStatus s = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, error);
                   if (decoration != null) {
                       updateDecorator(decoration, s, parameter.help);
                   }
                   if (status == null || status.isOK()) {
                       status = s;
                   }
               } else if (decoration != null) {
                   updateDecorator(decoration, null, parameter.help);
               }
            }

            if (status == null || status.isOK()) {
                if (parameter.control instanceof Combo) {
                    status = validateCombo(status, parameter, minSdk, buildApi);
                }
            }
        }

        setPageComplete(status == null || status.getSeverity() != IStatus.ERROR);
        if (status != null) {
            setMessage(status.getMessage(),
                    status.getSeverity() == IStatus.ERROR
                        ? IMessageProvider.ERROR : IMessageProvider.WARNING);
        } else {
            setErrorMessage(null);
            setMessage(null);
        }
    }

    /** Validates the given combo */
    static IStatus validateCombo(IStatus status, Parameter parameter, int minSdk, int buildApi) {
        Combo combo = (Combo) parameter.control;
        int index = combo.getSelectionIndex();
        return validateCombo(status, parameter, index, minSdk, buildApi);
    }

    /** Validates the given combo assuming the value at the given index is chosen */
    static IStatus validateCombo(IStatus status, Parameter parameter, int index,
            int minSdk, int buildApi) {
        Combo combo = (Combo) parameter.control;
        Integer[] optionIds = (Integer[]) combo.getData(ATTR_MIN_API);
        // Check minSdk
        if (index != -1 && index < optionIds.length) {
            Integer requiredMinSdk = optionIds[index];
            if (requiredMinSdk > minSdk) {
                status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    String.format(
                            "%1$s \"%2$s\" requires a minimum SDK version of at " +
                            "least %3$d, and the current min version is %4$d",
                            parameter.name, combo.getItems()[index], requiredMinSdk, minSdk));
            }
        }

        // Check minimum build target
        optionIds = (Integer[]) combo.getData(ATTR_MIN_BUILD_API);
        if (index != -1 && index < optionIds.length) {
            Integer requiredBuildApi = optionIds[index];
            if (requiredBuildApi > buildApi) {
                status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    String.format(
                        "%1$s \"%2$s\"  requires a build target API version of at " +
                        "least %3$d, and the current version is %4$d",
                        parameter.name, combo.getItems()[index], requiredBuildApi, buildApi));
            }
        }
        return status;
    }

    private int getMinSdk() {
        return mChooseProject ? mValues.getMinSdk() : mCustomMinSdk;
    }

    private int getBuildApi() {
        return mChooseProject ? mValues.getBuildApi() : mCustomBuildApi;
    }

    private void updateDecorator(ControlDecoration decorator, IStatus status, String help) {
        if (help != null && !help.isEmpty()) {
            decorator.setDescriptionText(status != null ? status.getMessage() : help);

            int severity = status != null ? status.getSeverity() : IStatus.OK;
            String id;
            if (severity == IStatus.ERROR) {
                id = FieldDecorationRegistry.DEC_ERROR;
            } else if (severity == IStatus.WARNING) {
                id = FieldDecorationRegistry.DEC_WARNING;
            } else {
                id = FieldDecorationRegistry.DEC_INFORMATION;
            }
            FieldDecoration errorFieldIndicator = FieldDecorationRegistry.getDefault().
                    getFieldDecoration(id);
            decorator.setImage(errorFieldIndicator.getImage());
        } else {
            if (status == null || status.isOK()) {
                decorator.hide();
            } else {
                decorator.show();
            }
        }
    }

    // ---- Implements ModifyListener ----

    @Override
    public void modifyText(ModifyEvent e) {
        if (mIgnore) {
            return;
        }

        Object source = e.getSource();
        if (source instanceof Text) {
            Text text = (Text) source;
            editParameter(text, text.getText().trim());
        }

        validatePage();
    }

    // ---- Implements SelectionListener ----

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (mIgnore) {
            return;
        }

        Object source = e.getSource();
        if (source == mProjectButton) {
            mValues.project = mProjectButton.getSelectedProject();
        } else if (source instanceof Combo) {
            Combo combo = (Combo) source;
            String[] optionIds = (String[]) combo.getData(ATTR_ID);
            int index = combo.getSelectionIndex();
            if (index != -1 && index < optionIds.length) {
                String optionId = optionIds[index];
                editParameter(combo, optionId);
                TemplateMetadata template = mValues.getTemplateHandler().getTemplate();
                if (template != null) {
                    setPreview(template.getThumbnailPath());
                }
            }
        } else if (source instanceof Button) {
            Button button = (Button) source;
            Parameter parameter = (Parameter) button.getData();
            if (parameter.type == Type.BOOLEAN) {
                // Checkbox parameter
                editParameter(button, button.getSelection());

                TemplateMetadata template = mValues.getTemplateHandler().getTemplate();
                if (template != null) {
                    setPreview(template.getThumbnailPath());
                }
            } else {
                // Choose button for some other parameter, usually a text
                String activity = chooseActivity();
                if (activity != null) {
                    setValue(parameter, activity);
                }
            }
        }

        validatePage();
    }

    private String chooseActivity() {
        try {
            // Compute a search scope: We need to merge all the subclasses
            // android.app.Fragment and android.support.v4.app.Fragment
            IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
            IProject project = mValues.project;
            IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);
            IType activityType = null;

            if (javaProject != null) {
                activityType = javaProject.findType(CLASS_ACTIVITY);
            }
            if (activityType == null) {
                IJavaProject[] projects = BaseProjectHelper.getAndroidProjects(null);
                for (IJavaProject p : projects) {
                    activityType = p.findType(CLASS_ACTIVITY);
                    if (activityType != null) {
                        break;
                    }
                }
            }
            if (activityType != null) {
                NullProgressMonitor monitor = new NullProgressMonitor();
                ITypeHierarchy hierarchy = activityType.newTypeHierarchy(monitor);
                IType[] classes = hierarchy.getAllSubtypes(activityType);
                scope = SearchEngine.createJavaSearchScope(classes, IJavaSearchScope.SOURCES);
            }

            Shell parent = AdtPlugin.getShell();
            final SelectionDialog dialog = JavaUI.createTypeDialog(
                    parent,
                    new ProgressMonitorDialog(parent),
                    scope,
                    IJavaElementSearchConstants.CONSIDER_CLASSES, false,
                    // Use ? as a default filter to fill dialog with matches
                    "?", //$NON-NLS-1$
                    new TypeSelectionExtension() {
                        @Override
                        public ITypeInfoFilterExtension getFilterExtension() {
                            return new ITypeInfoFilterExtension() {
                                @Override
                                public boolean select(ITypeInfoRequestor typeInfoRequestor) {
                                    int modifiers = typeInfoRequestor.getModifiers();
                                    if (!Flags.isPublic(modifiers)
                                            || Flags.isInterface(modifiers)
                                            || Flags.isEnum(modifiers)) {
                                        return false;
                                    }
                                    return true;
                                }
                            };
                        }
                    });

            dialog.setTitle("Choose Activity Class");
            dialog.setMessage("Select an Activity class (? = any character, * = any string):");
            if (dialog.open() == IDialogConstants.CANCEL_ID) {
                return null;
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

    private void editParameter(Control control, Object value) {
        Parameter parameter = getParameter(control);
        if (parameter != null) {
            String id = parameter.id;
            parameter.value = value;
            parameter.edited = value != null && !value.toString().isEmpty();
            mValues.parameters.put(id, value);

            // Update dependent variables, if any
            List<Parameter> parameters = mShowingTemplate.getParameters();
            for (Parameter p : parameters) {
                if (p == parameter || p.suggest == null || p.edited ||
                        p.type == Parameter.Type.SEPARATOR) {
                    continue;
                }
                if (!p.suggest.contains(id)) {
                    continue;
                }

                try {
                    if (mEvaluator == null) {
                        mEvaluator = new StringEvaluator();
                    }
                    String updated = mEvaluator.evaluate(p.suggest, parameters);
                    if (updated != null && !updated.equals(p.value)) {
                        setValue(p, updated);
                    }
                } catch (Throwable t) {
                    // Pass: Ignore updating if something wrong happens
                    t.printStackTrace(); // during development only
                }
            }
        }
    }

    private void setValue(Parameter p, String value) {
        p.value = value;
        mValues.parameters.put(p.id, value);

        // Update form widgets
        boolean prevIgnore = mIgnore;
        try {
            mIgnore = true;
            if (p.control instanceof Text) {
                ((Text) p.control).setText(value);
            } else if (p.control instanceof Button) {
                // TODO: Handle
            } else if (p.control instanceof Combo) {
                // TODO: Handle
            } else if (p.control != null) {
                assert false : p.control;
            }
        } finally {
            mIgnore = prevIgnore;
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    // ---- Implements FocusListener ----

    @Override
    public void focusGained(FocusEvent e) {
        Object source = e.getSource();
        String tip = "";

        if (source instanceof Control) {
            Control control = (Control) source;
            Parameter parameter = getParameter(control);
            if (parameter != null) {
                ControlDecoration decoration = mDecorations.get(parameter.id);
                if (decoration != null) {
                    tip = decoration.getDescriptionText();
                }
            }
        }

        mTipLabel.setText(tip);
        mHelpIcon.setVisible(tip.length() > 0);
    }

    @Override
    public void focusLost(FocusEvent e) {
        mTipLabel.setText("");
        mHelpIcon.setVisible(false);
    }
}
