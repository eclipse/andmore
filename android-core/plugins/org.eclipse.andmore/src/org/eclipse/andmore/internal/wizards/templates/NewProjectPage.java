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


import static com.android.SdkConstants.ATTR_ID;
import static org.eclipse.andmore.AdtUtils.extractClassName;
import static org.eclipse.andmore.internal.wizards.templates.NewTemplatePage.WIZARD_PAGE_WIDTH;

import com.android.annotations.Nullable;
import com.android.sdklib.SdkVersionInfo;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.internal.wizards.newproject.ApplicationInfoPage;
import org.eclipse.andmore.internal.wizards.newproject.ProjectNamePage;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * First wizard page in the "New Project From Template" wizard
 */
public class NewProjectPage extends WizardPage
        implements ModifyListener, SelectionListener, FocusListener {
    private static final int FIELD_WIDTH = 300;
    private static final String SAMPLE_PACKAGE_PREFIX = "com.example."; //$NON-NLS-1$
    /** Suffix added by default to activity names */
    static final String ACTIVITY_NAME_SUFFIX = "Activity";              //$NON-NLS-1$
    /** Prefix added to default layout names */
    static final String LAYOUT_NAME_PREFIX = "activity_";               //$NON-NLS-1$
    private static final int INITIAL_MIN_SDK = 8;

    private final NewProjectWizardState mValues;
    private Map<String, Integer> mMinNameToApi;
    private Parameter mThemeParameter;
    private Combo mThemeCombo;

    private Text mProjectText;
    private Text mPackageText;
    private Text mApplicationText;
    private Combo mMinSdkCombo;
    private Combo mTargetSdkCombo;
    private Combo mBuildSdkCombo;
    private Label mHelpIcon;
    private Label mTipLabel;

    private boolean mIgnore;
    private ControlDecoration mApplicationDec;
    private ControlDecoration mProjectDec;
    private ControlDecoration mPackageDec;
    private ControlDecoration mBuildTargetDec;
    private ControlDecoration mMinSdkDec;
    private ControlDecoration mTargetSdkDec;
    private ControlDecoration mThemeDec;

    NewProjectPage(NewProjectWizardState values) {
        super("newAndroidApp"); //$NON-NLS-1$
        mValues = values;
        setTitle("New Android Application");
        setDescription("Creates a new Android Application");
    }

    @SuppressWarnings("unused") // SWT constructors have side effects and aren't unused
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        GridLayout gl_container = new GridLayout(4, false);
        gl_container.horizontalSpacing = 10;
        container.setLayout(gl_container);

        Label applicationLabel = new Label(container, SWT.NONE);
        applicationLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
        applicationLabel.setText("Application Name:");

        mApplicationText = new Text(container, SWT.BORDER);
        GridData gdApplicationText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
        gdApplicationText.widthHint = FIELD_WIDTH;
        mApplicationText.setLayoutData(gdApplicationText);
        mApplicationText.addModifyListener(this);
        mApplicationText.addFocusListener(this);
        mApplicationDec = createFieldDecoration(mApplicationText,
                "The application name is shown in the Play Store, as well as in the " +
                "Manage Application list in Settings.");

        Label projectLabel = new Label(container, SWT.NONE);
        projectLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
        projectLabel.setText("Project Name:");
        mProjectText = new Text(container, SWT.BORDER);
        GridData gdProjectText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
        gdProjectText.widthHint = FIELD_WIDTH;
        mProjectText.setLayoutData(gdProjectText);
        mProjectText.addModifyListener(this);
        mProjectText.addFocusListener(this);
        mProjectDec = createFieldDecoration(mProjectText,
                "The project name is only used by Eclipse, but must be unique within the " +
                "workspace. This can typically be the same as the application name.");

        Label packageLabel = new Label(container, SWT.NONE);
        packageLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
        packageLabel.setText("Package Name:");

        mPackageText = new Text(container, SWT.BORDER);
        GridData gdPackageText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
        gdPackageText.widthHint = FIELD_WIDTH;
        mPackageText.setLayoutData(gdPackageText);
        mPackageText.addModifyListener(this);
        mPackageText.addFocusListener(this);
        mPackageDec = createFieldDecoration(mPackageText,
                "The package name must be a unique identifier for your application.\n" +
                "It is typically not shown to users, but it *must* stay the same " +
                "for the lifetime of your application; it is how multiple versions " +
                "of the same application are considered the \"same app\".\nThis is " +
                "typically the reverse domain name of your organization plus one or " +
                "more application identifiers, and it must be a valid Java package " +
                "name.");
        new Label(container, SWT.NONE);

        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);

        // Min SDK

        Label minSdkLabel = new Label(container, SWT.NONE);
        minSdkLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
        minSdkLabel.setText("Minimum Required SDK:");

        mMinSdkCombo = new Combo(container, SWT.READ_ONLY);
        GridData gdMinSdkCombo = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
        gdMinSdkCombo.widthHint = FIELD_WIDTH;
        mMinSdkCombo.setLayoutData(gdMinSdkCombo);

        // Pick most recent platform
        IAndroidTarget[] targets = getCompilationTargets();
        mMinNameToApi = Maps.newHashMap();
        List<String> targetLabels = new ArrayList<String>(targets.length);
        for (IAndroidTarget target : targets) {
            String targetLabel;
            if (target.isPlatform()
                    && target.getVersion().getApiLevel() <= AdtUtils.getHighestKnownApiLevel()) {
                targetLabel = AdtUtils.getAndroidName(target.getVersion().getApiLevel());
            } else {
                targetLabel = AdtUtils.getTargetLabel(target);
            }
            targetLabels.add(targetLabel);
            mMinNameToApi.put(targetLabel, target.getVersion().getApiLevel());
        }

        List<String> codeNames = Lists.newArrayList();
        int buildTargetIndex = -1;
        for (int i = 0, n = targets.length; i < n; i++) {
            IAndroidTarget target = targets[i];
            AndroidVersion version = target.getVersion();
            int apiLevel = version.getApiLevel();
            if (version.isPreview()) {
                String codeName = version.getCodename();
                String targetLabel = codeName + " Preview";
                codeNames.add(targetLabel);
                mMinNameToApi.put(targetLabel, apiLevel);
            } else if (target.isPlatform()
                    && (mValues.target == null ||
                        apiLevel > mValues.target.getVersion().getApiLevel())) {
                mValues.target = target;
                buildTargetIndex = i;
            }
        }
        List<String> labels = new ArrayList<String>(24);
        for (String label : AdtUtils.getKnownVersions()) {
            labels.add(label);
        }
        assert labels.size() >= 15; // *Known* versions to ADT, not installed/available versions
        for (String codeName : codeNames) {
            labels.add(codeName);
        }
        String[] versions = labels.toArray(new String[labels.size()]);
        mMinSdkCombo.setItems(versions);
        if (mValues.target != null && mValues.target.getVersion().isPreview()) {
            mValues.minSdk = mValues.target.getVersion().getCodename();
            mMinSdkCombo.setText(mValues.minSdk);
            mValues.iconState.minSdk = mValues.target.getVersion().getApiLevel();
            mValues.minSdkLevel = mValues.iconState.minSdk;
        } else {
            mMinSdkCombo.select(INITIAL_MIN_SDK - 1);
            mValues.minSdk = Integer.toString(INITIAL_MIN_SDK);
            mValues.minSdkLevel = INITIAL_MIN_SDK;
            mValues.iconState.minSdk = INITIAL_MIN_SDK;
        }
        mMinSdkCombo.addSelectionListener(this);
        mMinSdkCombo.addFocusListener(this);
        mMinSdkDec = createFieldDecoration(mMinSdkCombo,
                "Choose the lowest version of Android that your application will support. Lower " +
                "API levels target more devices, but means fewer features are available. By " +
                "targeting API 8 and later, you reach approximately 95% of the market.");
        new Label(container, SWT.NONE);

        // Target SDK
        Label targetSdkLabel = new Label(container, SWT.NONE);
        targetSdkLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
        targetSdkLabel.setText("Target SDK:");

        mTargetSdkCombo = new Combo(container, SWT.READ_ONLY);
        GridData gdTargetSdkCombo = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
        gdTargetSdkCombo.widthHint = FIELD_WIDTH;
        mTargetSdkCombo.setLayoutData(gdTargetSdkCombo);

        mTargetSdkCombo.setItems(versions);
        mTargetSdkCombo.select(mValues.targetSdkLevel - 1);

        mTargetSdkCombo.addSelectionListener(this);
        mTargetSdkCombo.addFocusListener(this);
        mTargetSdkDec = createFieldDecoration(mTargetSdkCombo,
                "Choose the highest API level that the application is known to work with. " +
                "This attribute informs the system that you have tested against the target " +
                "version and the system should not enable any compatibility behaviors to " +
                "maintain your app's forward-compatibility with the target version. " +
                "The application is still able to run on older versions " +
                "(down to minSdkVersion). Your application may look dated if you are not " +
                "targeting the current version.");
        new Label(container, SWT.NONE);

        // Build Version

        Label buildSdkLabel = new Label(container, SWT.NONE);
        buildSdkLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
        buildSdkLabel.setText("Compile With:");

        mBuildSdkCombo = new Combo(container, SWT.READ_ONLY);
        GridData gdBuildSdkCombo = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
        gdBuildSdkCombo.widthHint = FIELD_WIDTH;
        mBuildSdkCombo.setLayoutData(gdBuildSdkCombo);
        mBuildSdkCombo.setData(targets);
        mBuildSdkCombo.setItems(targetLabels.toArray(new String[targetLabels.size()]));
        if (buildTargetIndex != -1) {
            mBuildSdkCombo.select(buildTargetIndex);
        }

        mBuildSdkCombo.addSelectionListener(this);
        mBuildSdkCombo.addFocusListener(this);
        mBuildTargetDec = createFieldDecoration(mBuildSdkCombo,
                "Choose a target API to compile your code against, from your installed SDKs. " +
                "This is typically the most recent version, or the first version that supports " +
                "all the APIs you want to directly access without reflection.");
        new Label(container, SWT.NONE);

        TemplateMetadata metadata = mValues.template.getTemplate();
        if (metadata != null) {
            mThemeParameter = metadata.getParameter("baseTheme"); //$NON-NLS-1$
            if (mThemeParameter != null && mThemeParameter.element != null) {
                Label themeLabel = new Label(container, SWT.NONE);
                themeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
                themeLabel.setText("Theme:");

                mThemeCombo = NewTemplatePage.createOptionCombo(mThemeParameter, container,
                        mValues.parameters, this, this);
                GridData gdThemeCombo = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
                gdThemeCombo.widthHint = FIELD_WIDTH;
                mThemeCombo.setLayoutData(gdThemeCombo);
                new Label(container, SWT.NONE);

                mThemeDec = createFieldDecoration(mThemeCombo,
                        "Choose the base theme to use for the application");
            }
        }

        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);

        Label label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 4, 1));

        mHelpIcon = new Label(container, SWT.NONE);
        mHelpIcon.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));
        Image icon = IconFactory.getInstance().getIcon("quickfix");
        mHelpIcon.setImage(icon);
        mHelpIcon.setVisible(false);

        mTipLabel = new Label(container, SWT.WRAP);
        mTipLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

        // Reserve space for 6 lines
        mTipLabel.setText("\n\n\n\n\n\n"); //$NON-NLS-1$

        // Reserve enough width to accommodate the various wizard pages up front
        // (since they are created lazily, and we don't want the wizard to dynamically
        // resize itself for small size adjustments as each successive page is slightly
        // larger)
        Label dummy = new Label(container, SWT.NONE);
        GridData data = new GridData();
        data.horizontalSpan = 4;
        data.widthHint = WIZARD_PAGE_WIDTH;
        dummy.setLayoutData(data);
    }

    /**
     * Updates the theme selection such that it's valid for the current build
     * and min sdk targets. Also runs {@link #validatePage} in case no valid entry was found.
     * Does nothing if called on a template that does not supply a theme.
     */
    void updateTheme() {
        if (mThemeParameter != null) {
            // Pick the highest theme version that works for the current SDK level
            Parameter parameter = NewTemplatePage.getParameter(mThemeCombo);
            assert parameter == mThemeParameter;
            if (parameter != null) {
                String[] optionIds = (String[]) mThemeCombo.getData(ATTR_ID);
                for (int index = optionIds.length - 1; index >= 0; index--) {
                    IStatus status = NewTemplatePage.validateCombo(null, mThemeParameter,
                            index, mValues.minSdkLevel, mValues.getBuildApi());
                    if (status == null || status.isOK()) {
                        String optionId = optionIds[index];
                        parameter.value = optionId;
                        parameter.edited = optionId != null && !optionId.toString().isEmpty();
                        mValues.parameters.put(parameter.id, optionId);
                        try {
                            mIgnore = true;
                            mThemeCombo.select(index);
                        } finally {
                            mIgnore = false;
                        }
                        break;
                    }
                }
            }

            validatePage();
        }
    }

    private IAndroidTarget[] getCompilationTargets() {
        Sdk current = Sdk.getCurrent();
        if (current == null) {
            return new IAndroidTarget[0];
        }
        Collection<IAndroidTarget> targets = current.getTargets();
        List<IAndroidTarget> list = new ArrayList<IAndroidTarget>();

        for (IAndroidTarget target : targets) {
            if (target.isPlatform() == false &&
                    (target.getOptionalLibraries() == null ||
                            target.getOptionalLibraries().size() == 0)) {
                continue;
            }
            list.add(target);
        }

        return list.toArray(new IAndroidTarget[list.size()]);
    }

    private ControlDecoration createFieldDecoration(Control control, String description) {
        ControlDecoration dec = new ControlDecoration(control, SWT.LEFT);
        dec.setMarginWidth(2);
        FieldDecoration errorFieldIndicator = FieldDecorationRegistry.getDefault().
           getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
        dec.setImage(errorFieldIndicator.getImage());
        dec.setDescriptionText(description);
        control.setToolTipText(description);

        return dec;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        // DURING DEVELOPMENT ONLY
        //if (assertionsEnabled()) {
        //    String uniqueProjectName = AdtUtils.getUniqueProjectName("Test", "");
        //    mProjectText.setText(uniqueProjectName);
        //    mPackageText.setText("test.pkg");
        //}

        validatePage();
    }

    // ---- Implements ModifyListener ----

    @Override
    public void modifyText(ModifyEvent e) {
        if (mIgnore) {
            return;
        }

        Object source = e.getSource();
        if (source == mProjectText) {
            mValues.projectName = mProjectText.getText();
            updateProjectLocation(mValues.projectName);
            mValues.projectModified = true;

            try {
                mIgnore = true;
                if (!mValues.applicationModified) {
                    mValues.applicationName = mValues.projectName;
                    mApplicationText.setText(mValues.projectName);
                }
                updateActivityNames(mValues.projectName);
            } finally {
                mIgnore = false;
            }
            suggestPackage(mValues.projectName);
        } else if (source == mPackageText) {
            mValues.packageName = mPackageText.getText();
            mValues.packageModified = true;
        } else if (source == mApplicationText) {
            mValues.applicationName = mApplicationText.getText();
            mValues.applicationModified = true;

            try {
                mIgnore = true;
                if (!mValues.projectModified) {
                    mValues.projectName = appNameToProjectName(mValues.applicationName);
                    mProjectText.setText(mValues.projectName);
                    updateProjectLocation(mValues.projectName);
                }
                updateActivityNames(mValues.applicationName);
            } finally {
                mIgnore = false;
            }
            suggestPackage(mValues.applicationName);
        }

        validatePage();
    }

    private String appNameToProjectName(String appName) {
        // Strip out whitespace (and capitalize subsequent words where spaces were removed
        boolean upcaseNext = false;
        StringBuilder sb = new StringBuilder(appName.length());
        for (int i = 0, n = appName.length(); i < n; i++) {
            char c = appName.charAt(i);
            if (c == ' ') {
                upcaseNext = true;
            } else if (upcaseNext) {
                sb.append(Character.toUpperCase(c));
                upcaseNext = false;
            } else {
                sb.append(c);
            }
        }

        appName = sb.toString().trim();

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IStatus nameStatus = workspace.validateName(appName, IResource.PROJECT);
        if (nameStatus.isOK()) {
            return appName;
        }

        sb = new StringBuilder(appName.length());
        for (int i = 0, n = appName.length(); i < n; i++) {
            char c = appName.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '.' || c == '-') {
                sb.append(c);
            }
        }

        return sb.toString().trim();
    }

    /** If the project should be created in the workspace, then update the project location
     * based on the project name. */
    private void updateProjectLocation(String projectName) {
        if (projectName == null) {
            projectName = "";
        }

        if (mValues.useDefaultLocation) {
            IPath workspace = Platform.getLocation();
            String projectLocation = workspace.append(projectName).toOSString();
            mValues.projectLocation = projectLocation;
        }
    }

    private void updateActivityNames(String name) {
        try {
            mIgnore = true;
            if (!mValues.activityNameModified) {
                mValues.activityName = extractClassName(name) + ACTIVITY_NAME_SUFFIX;
            }
            if (!mValues.activityTitleModified) {
                mValues.activityTitle = name;
            }
        } finally {
            mIgnore = false;
        }
    }

    // ---- Implements SelectionListener ----

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (mIgnore) {
            return;
        }

        Object source = e.getSource();
        if (source == mMinSdkCombo) {
            mValues.minSdk = getSelectedMinSdk();
            Integer minSdk = mMinNameToApi.get(mValues.minSdk);
            if (minSdk == null) {
                try {
                    minSdk = Integer.parseInt(mValues.minSdk);
                } catch (NumberFormatException nufe) {
                    // If not a number, then the string is a codename, so treat it
                    // as a preview version.
                    minSdk = SdkVersionInfo.HIGHEST_KNOWN_API + 1;
                }
            }
            mValues.iconState.minSdk = minSdk.intValue();
            mValues.minSdkLevel = minSdk.intValue();

            // If higher than build target, adjust build target
            if (mValues.minSdkLevel > mValues.getBuildApi()) {
                // Try to find a build target with an adequate build API
                IAndroidTarget[] targets = (IAndroidTarget[]) mBuildSdkCombo.getData();
                IAndroidTarget best = null;
                int bestApi = Integer.MAX_VALUE;
                int bestTargetIndex = -1;
                for (int i = 0; i < targets.length; i++) {
                    IAndroidTarget target = targets[i];
                    if (!target.isPlatform()) {
                        continue;
                    }
                    int api = target.getVersion().getApiLevel();
                    if (api >= mValues.minSdkLevel && api < bestApi) {
                        best = target;
                        bestApi = api;
                        bestTargetIndex = i;
                    }
                }

                if (best != null) {
                    assert bestTargetIndex != -1;
                    mValues.target = best;
                    try {
                        mIgnore = true;
                        mBuildSdkCombo.select(bestTargetIndex);
                    } finally {
                        mIgnore = false;
                    }
                }
            }

            // If higher than targetSdkVersion, adjust targetSdkVersion
            if (mValues.minSdkLevel > mValues.targetSdkLevel) {
                mValues.targetSdkLevel = mValues.minSdkLevel;
                try {
                    mIgnore = true;
                    setSelectedTargetSdk(mValues.targetSdkLevel);
                } finally {
                    mIgnore = false;
                }
            }
        } else if (source == mBuildSdkCombo) {
            mValues.target = getSelectedBuildTarget();

            // If lower than min sdk target, adjust min sdk target
            if (mValues.target.getVersion().isPreview()) {
                mValues.minSdk = mValues.target.getVersion().getCodename();
                try {
                    mIgnore = true;
                    mMinSdkCombo.setText(mValues.minSdk);
                } finally {
                    mIgnore = false;
                }
            } else {
                String minSdk = mValues.minSdk;
                int buildApiLevel = mValues.target.getVersion().getApiLevel();
                if (minSdk != null && !minSdk.isEmpty()
                        && Character.isDigit(minSdk.charAt(0))
                        && buildApiLevel < Integer.parseInt(minSdk)) {
                    mValues.minSdk = Integer.toString(buildApiLevel);
                    try {
                        mIgnore = true;
                        setSelectedMinSdk(buildApiLevel);
                    } finally {
                        mIgnore = false;
                    }
                }
            }
        } else if (source == mTargetSdkCombo) {
            mValues.targetSdkLevel = getSelectedTargetSdk();
        }

        validatePage();
    }

    private String getSelectedMinSdk() {
        // If you're using a preview build, such as android-JellyBean, you have
        // to use the codename, e.g. JellyBean, as the minimum SDK as well.
        IAndroidTarget buildTarget = getSelectedBuildTarget();
        if (buildTarget != null && buildTarget.getVersion().isPreview()) {
            return buildTarget.getVersion().getCodename();
        }

        // +1: First API level (at index 0) is 1
        return Integer.toString(mMinSdkCombo.getSelectionIndex() + 1);
    }

    private int getSelectedTargetSdk() {
        // +1: First API level (at index 0) is 1
        return mTargetSdkCombo.getSelectionIndex() + 1;
    }

    private void setSelectedMinSdk(int api) {
        mMinSdkCombo.select(api - 1); // -1: First API level (at index 0) is 1
    }

    private void setSelectedTargetSdk(int api) {
        mTargetSdkCombo.select(api - 1); // -1: First API level (at index 0) is 1
    }

    @Nullable
    private IAndroidTarget getSelectedBuildTarget() {
        IAndroidTarget[] targets = (IAndroidTarget[]) mBuildSdkCombo.getData();
        int index = mBuildSdkCombo.getSelectionIndex();
        if (index >= 0 && index < targets.length) {
            return targets[index];
        } else {
            return null;
        }
    }

    private void suggestPackage(String original) {
        if (!mValues.packageModified) {
            // Create default package name
            StringBuilder sb = new StringBuilder();
            sb.append(SAMPLE_PACKAGE_PREFIX);
            appendPackage(sb, original);

            String pkg = sb.toString();
            if (pkg.endsWith(".")) { //$NON-NLS-1$
                pkg = pkg.substring(0, pkg.length() - 1);
            }
            mValues.packageName = pkg;
            try {
                mIgnore = true;
                mPackageText.setText(mValues.packageName);
            } finally {
                mIgnore = false;
            }
        }
    }

    private static void appendPackage(StringBuilder sb, String string) {
        for (int i = 0, n = string.length(); i < n; i++) {
            char c = string.charAt(i);
            if (i == 0 && Character.isJavaIdentifierStart(c)
                    || i != 0 && Character.isJavaIdentifierPart(c)) {
                sb.append(Character.toLowerCase(c));
            } else if ((c == '.')
                    && (sb.length() > 0 && sb.charAt(sb.length() - 1) != '.')) {
                sb.append('.');
            } else if (c == '-') {
                sb.append('_');
            }
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
        if (source == mApplicationText) {
            tip = mApplicationDec.getDescriptionText();
        } else if (source == mProjectText) {
            tip = mProjectDec.getDescriptionText();
        } else if (source == mBuildSdkCombo) {
            tip = mBuildTargetDec.getDescriptionText();
        } else if (source == mMinSdkCombo) {
            tip = mMinSdkDec.getDescriptionText();
        } else if (source == mPackageText) {
            tip = mPackageDec.getDescriptionText();
            if (mPackageText.getText().startsWith(SAMPLE_PACKAGE_PREFIX)) {
                int length = SAMPLE_PACKAGE_PREFIX.length();
                if (mPackageText.getText().length() > length
                        && SAMPLE_PACKAGE_PREFIX.endsWith(".")) { //$NON-NLS-1$
                    length--;
                }
                mPackageText.setSelection(0, length);
            }
        } else if (source == mTargetSdkCombo) {
            tip = mTargetSdkDec.getDescriptionText();
        } else if (source == mThemeCombo) {
            tip = mThemeDec.getDescriptionText();
        }
        mTipLabel.setText(tip);
        mHelpIcon.setVisible(tip.length() > 0);
    }

    @Override
    public void focusLost(FocusEvent e) {
        mTipLabel.setText("");
        mHelpIcon.setVisible(false);
    }

    // Validation

    private void validatePage() {
        IStatus status = mValues.template.validateTemplate(mValues.minSdkLevel,
                mValues.getBuildApi());
        if (status != null && !status.isOK()) {
            updateDecorator(mApplicationDec, null, true);
            updateDecorator(mPackageDec, null, true);
            updateDecorator(mProjectDec, null, true);
            updateDecorator(mThemeDec, null, true);
            /* These never get marked with errors:
            updateDecorator(mBuildTargetDec, null, true);
            updateDecorator(mMinSdkDec, null, true);
            updateDecorator(mTargetSdkDec, null, true);
            */
        } else {
            IStatus appStatus = validateAppName();
            if (appStatus != null && (status == null
                    || appStatus.getSeverity() > status.getSeverity())) {
                status = appStatus;
            }

            IStatus projectStatus = validateProjectName();
            if (projectStatus != null && (status == null
                    || projectStatus.getSeverity() > status.getSeverity())) {
                status = projectStatus;
            }

            IStatus packageStatus = validatePackageName();
            if (packageStatus != null && (status == null
                    || packageStatus.getSeverity() > status.getSeverity())) {
                status = packageStatus;
            }

            IStatus locationStatus = ProjectContentsPage.validateLocationInWorkspace(mValues);
            if (locationStatus != null && (status == null
                    || locationStatus.getSeverity() > status.getSeverity())) {
                status = locationStatus;
            }

            if (status == null || status.getSeverity() != IStatus.ERROR) {
                if (mValues.target == null) {
                    status = new Status(IStatus.WARNING, AndmoreAndroidPlugin.PLUGIN_ID,
                            "Select an Android build target version");
                }
            }

            if (status == null || status.getSeverity() != IStatus.ERROR) {
                if (mValues.minSdk == null || mValues.minSdk.isEmpty()) {
                    status = new Status(IStatus.WARNING, AndmoreAndroidPlugin.PLUGIN_ID,
                            "Select a minimum SDK version");
                } else {
                    AndroidVersion version = mValues.target.getVersion();
                    if (version.isPreview()) {
                        if (version.getCodename().equals(mValues.minSdk) == false) {
                            status = new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                            "Preview platforms require the min SDK version to match their codenames.");
                       }
                    } else if (mValues.target.getVersion().compareTo(
                            mValues.minSdkLevel,
                            version.isPreview() ? mValues.minSdk : null) < 0) {
                        status = new Status(IStatus.WARNING, AndmoreAndroidPlugin.PLUGIN_ID,
                            "The minimum SDK version is higher than the build target version");
                    }
                    if (status == null || status.getSeverity() != IStatus.ERROR) {
                        if (mValues.targetSdkLevel < mValues.minSdkLevel) {
                            status = new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                                "The target SDK version should be at least as high as the minimum SDK version");
                        }
                    }
                }
            }

            IStatus themeStatus = validateTheme();
            if (themeStatus != null && (status == null
                    || themeStatus.getSeverity() > status.getSeverity())) {
                status = themeStatus;
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

    private IStatus validateAppName() {
        String appName = mValues.applicationName;
        IStatus status = null;
        if (appName == null || appName.isEmpty()) {
            status = new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                    "Enter an application name (shown in launcher)");
        } else if (Character.isLowerCase(mValues.applicationName.charAt(0))) {
            status = new Status(IStatus.WARNING, AndmoreAndroidPlugin.PLUGIN_ID,
                    "The application name for most apps begins with an uppercase letter");
        }

        updateDecorator(mApplicationDec, status, true);

        return status;
    }

    private IStatus validateProjectName() {
        IStatus status = ProjectNamePage.validateProjectName(mValues.projectName);
        updateDecorator(mProjectDec, status, true);

        return status;
    }

    private IStatus validatePackageName() {
        IStatus status;
        if (mValues.packageName == null || mValues.packageName.startsWith(SAMPLE_PACKAGE_PREFIX)) {
            if (mValues.packageName != null
                    && !mValues.packageName.equals(SAMPLE_PACKAGE_PREFIX)) {
                status = ApplicationInfoPage.validatePackage(mValues.packageName);
                if (status == null || status.isOK()) {
                    status = new Status(IStatus.WARNING, AndmoreAndroidPlugin.PLUGIN_ID,
                        String.format("The prefix '%1$s' is meant as a placeholder and should " +
                                      "not be used", SAMPLE_PACKAGE_PREFIX));
                }
            } else {
                status = new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                        "Package name must be specified.");
            }
        } else {
            status = ApplicationInfoPage.validatePackage(mValues.packageName);
        }

        updateDecorator(mPackageDec, status, true);

        return status;
    }

    private IStatus validateTheme() {
        IStatus status = null;

        if (mThemeParameter != null) {
            status = NewTemplatePage.validateCombo(null, mThemeParameter,
                    mThemeCombo.getSelectionIndex(),  mValues.minSdkLevel,
                    mValues.getBuildApi());

            updateDecorator(mThemeDec, status, true);
        }

        return status;
    }

    private void updateDecorator(ControlDecoration decorator, IStatus status, boolean hasInfo) {
        if (hasInfo) {
            int severity = status != null ? status.getSeverity() : IStatus.OK;
            setDecoratorType(decorator, severity);
        } else {
            if (status == null || status.isOK()) {
                decorator.hide();
            } else {
                decorator.show();
            }
        }
    }

    private void setDecoratorType(ControlDecoration decorator, int severity) {
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
    }
}
