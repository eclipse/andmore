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

import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_PACKAGE_NAME;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_CONSTRAINTS;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_DEFAULT;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_HELP;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_ID;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_NAME;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_SUGGEST;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.google.common.base.Splitter;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.resources.ResourceNameValidator;
import org.eclipse.andmore.internal.wizards.newproject.ApplicationInfoPage;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.widgets.Control;
import org.w3c.dom.Element;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * A template parameter editable and edited by the user.
 * <p>
 * Note that this class encapsulates not just the metadata provided by the
 * template, but the actual editing operation of that template in the wizard: it
 * also captures current values, a reference to the editing widget (such that
 * related widgets can be updated when one value depends on another etc)
 */
class Parameter {
    enum Type {
        STRING,
        BOOLEAN,
        ENUM,
        SEPARATOR;
        // TODO: Numbers?

        public static Type get(String name) {
            try {
                return Type.valueOf(name.toUpperCase(Locale.US));
            } catch (IllegalArgumentException e) {
                AdtPlugin.printErrorToConsole("Unexpected template type '" + name + "'");
                AdtPlugin.printErrorToConsole("Expected one of :");
                for (Type s : Type.values()) {
                    AdtPlugin.printErrorToConsole("  " + s.name().toLowerCase(Locale.US));
                }
            }

            return STRING;
        }
    }

    /**
     * Constraints that can be applied to a parameter which helps the UI add a
     * validator etc for user input. These are typically combined into a set
     * of constraints via an EnumSet.
     */
    enum Constraint {
        /**
         * This value must be unique. This constraint usually only makes sense
         * when other constraints are specified, such as {@link #LAYOUT}, which
         * means that the parameter should designate a name that does not
         * represent an existing layout resource name
         */
        UNIQUE,

        /**
         * This value must already exist. This constraint usually only makes sense
         * when other constraints are specified, such as {@link #LAYOUT}, which
         * means that the parameter should designate a name that already exists as
         * a resource name.
         */
        EXISTS,

        /** The associated value must not be empty */
        NONEMPTY,

        /** The associated value is allowed to be empty */
        EMPTY,

        /** The associated value should represent a fully qualified activity class name */
        ACTIVITY,

        /** The associated value should represent an API level */
        APILEVEL,

        /** The associated value should represent a valid class name */
        CLASS,

        /** The associated value should represent a valid package name */
        PACKAGE,

        /** The associated value should represent a valid layout resource name */
        LAYOUT,

        /** The associated value should represent a valid drawable resource name */
        DRAWABLE,

        /** The associated value should represent a valid id resource name */
        ID,

        /** The associated value should represent a valid string resource name */
        STRING;

        public static Constraint get(String name) {
            try {
                return Constraint.valueOf(name.toUpperCase(Locale.US));
            } catch (IllegalArgumentException e) {
                AdtPlugin.printErrorToConsole("Unexpected template constraint '" + name + "'");
                if (name.indexOf(',') != -1) {
                    AdtPlugin.printErrorToConsole("Use | to separate constraints");
                } else {
                    AdtPlugin.printErrorToConsole("Expected one of :");
                    for (Constraint s : Constraint.values()) {
                        AdtPlugin.printErrorToConsole("  " + s.name().toLowerCase(Locale.US));
                    }
                }
            }

            return NONEMPTY;
        }
    }

    /** The template defining the parameter */
    public final TemplateMetadata template;

    /** The type of parameter */
    @NonNull
    public final Type type;

    /** The unique id of the parameter (not displayed to the user) */
    @Nullable
    public final String id;

    /** The display name for this parameter */
    @Nullable
    public final String name;

    /**
     * The initial value for this parameter (see also {@link #suggest} for more
     * dynamic defaults
     */
    @Nullable
    public final String initial;

    /**
     * A template expression using other template parameters for producing a
     * default value based on other edited parameters, if possible.
     */
    @Nullable
    public final String suggest;

    /** Help for the parameter, if any */
    @Nullable
    public final String help;

    /** The currently edited value */
    @Nullable
    public Object value;

    /** The control showing this value */
    @Nullable
    public Control control;

    /** The decoration associated with the control */
    @Nullable
    public ControlDecoration decoration;

    /** Whether the parameter has been edited */
    public boolean edited;

    /** The element defining this parameter */
    @NonNull
    public final Element element;

    /** The constraints applicable for this parameter */
    @NonNull
    public final EnumSet<Constraint> constraints;

    /** The validator, if any, for this field */
    private IInputValidator mValidator;

    /** True if this field has no validator */
    private boolean mNoValidator;

    /** Project associated with this validator */
    private IProject mValidatorProject;

    Parameter(@NonNull TemplateMetadata template, @NonNull Element parameter) {
        this.template = template;
        element = parameter;

        String typeName = parameter.getAttribute(TemplateHandler.ATTR_TYPE);
        assert typeName != null && !typeName.isEmpty() : TemplateHandler.ATTR_TYPE;
        type = Type.get(typeName);

        id = parameter.getAttribute(ATTR_ID);
        initial = parameter.getAttribute(ATTR_DEFAULT);
        suggest = parameter.getAttribute(ATTR_SUGGEST);
        name = parameter.getAttribute(ATTR_NAME);
        help = parameter.getAttribute(ATTR_HELP);
        String constraintString = parameter.getAttribute(ATTR_CONSTRAINTS);
        if (constraintString != null && !constraintString.isEmpty()) {
            EnumSet<Constraint> constraintSet = null;
            for (String s : Splitter.on('|').omitEmptyStrings().split(constraintString)) {
                Constraint constraint = Constraint.get(s);
                if (constraintSet == null) {
                    constraintSet = EnumSet.of(constraint);
                } else {
                    constraintSet = EnumSet.copyOf(constraintSet);
                    constraintSet.add(constraint);
                }
            }
            constraints = constraintSet;
        } else {
            constraints = EnumSet.noneOf(Constraint.class);
        }

        if (initial != null && !initial.isEmpty() && type == Type.BOOLEAN) {
            value = Boolean.valueOf(initial);
        } else {
            value = initial;
        }
    }

    Parameter(
            @NonNull TemplateMetadata template,
            @NonNull Type type,
            @NonNull String id,
            @NonNull String initialValue) {
        this.template = template;
        this.type = type;
        this.id = id;
        this.value = initialValue;
        element = null;
        initial = null;
        suggest = null;
        name = id;
        help = null;
        constraints = EnumSet.noneOf(Constraint.class);
    }

    List<Element> getOptions() {
        if (element != null) {
            return DomUtilities.getChildren(element);
        } else {
            return Collections.emptyList();
        }
    }

    @Nullable
    public IInputValidator getValidator(@Nullable final IProject project) {
        if (mNoValidator) {
            return null;
        }

        if (project != mValidatorProject) {
            // Force update of validators if the project changes, since the validators
            // are often tied to project metadata (for example, the resource name validators
            // which look for name conflicts)
            mValidator = null;
            mValidatorProject = project;
        }

        if (mValidator == null) {
            if (constraints.contains(Constraint.LAYOUT)) {
                if (project != null && constraints.contains(Constraint.UNIQUE)) {
                    mValidator = ResourceNameValidator.create(false, project, ResourceType.LAYOUT);
                } else {
                    mValidator = ResourceNameValidator.create(false, ResourceFolderType.LAYOUT);
                }
                return mValidator;
            } else if (constraints.contains(Constraint.STRING)) {
                if (project != null && constraints.contains(Constraint.UNIQUE)) {
                    mValidator = ResourceNameValidator.create(false, project, ResourceType.STRING);
                } else {
                    mValidator = ResourceNameValidator.create(false, ResourceFolderType.VALUES);
                }
                return mValidator;
            } else if (constraints.contains(Constraint.ID)) {
                if (project != null && constraints.contains(Constraint.UNIQUE)) {
                    mValidator = ResourceNameValidator.create(false, project, ResourceType.ID);
                } else {
                    mValidator = ResourceNameValidator.create(false, ResourceFolderType.VALUES);
                }
                return mValidator;
            } else if (constraints.contains(Constraint.DRAWABLE)) {
                if (project != null && constraints.contains(Constraint.UNIQUE)) {
                    mValidator = ResourceNameValidator.create(false, project,
                            ResourceType.DRAWABLE);
                } else {
                    mValidator = ResourceNameValidator.create(false, ResourceFolderType.DRAWABLE);
                }
                return mValidator;
            } else if (constraints.contains(Constraint.PACKAGE)
                    || constraints.contains(Constraint.CLASS)
                    || constraints.contains(Constraint.ACTIVITY)) {
                mValidator = new IInputValidator() {
                    @Override
                    public String isValid(String newText) {
                        newText = newText.trim();
                        if (newText.isEmpty()) {
                            if (constraints.contains(Constraint.EMPTY)) {
                                return null;
                            } else if (constraints.contains(Constraint.NONEMPTY)) {
                                return String.format("Enter a value for %1$s", name);
                            } else {
                                // Compatibility mode: older templates might not specify;
                                // in that case, accept empty
                                if (!"activityClass".equals(id)) { //$NON-NLS-1$
                                    return null;
                                }
                            }
                        }
                        IStatus status;
                        if (constraints.contains(Constraint.ACTIVITY)) {
                            status = ApplicationInfoPage.validateActivity(newText);
                        } else if (constraints.contains(Constraint.PACKAGE)) {
                            status = ApplicationInfoPage.validatePackage(newText);
                        } else {
                            assert constraints.contains(Constraint.CLASS);
                            status = ApplicationInfoPage.validateClass(newText);
                        }
                        if (status != null && !status.isOK()) {
                            return status.getMessage();
                        }

                        // Uniqueness
                        if (project != null && constraints.contains(Constraint.UNIQUE)) {
                            try {
                                // Determine the package.
                                // If there is a package info

                                IJavaProject p = BaseProjectHelper.getJavaProject(project);
                                if (p != null) {
                                    String fqcn = newText;
                                    if (fqcn.indexOf('.') == -1) {
                                        String pkg = null;
                                        Parameter parameter = template.getParameter(
                                                ATTR_PACKAGE_NAME);
                                        if (parameter != null && parameter.value != null) {
                                            pkg = parameter.value.toString();
                                        } else {
                                            pkg = ManifestInfo.get(project).getPackage();
                                        }
                                        fqcn = pkg.isEmpty() ? newText : pkg + '.' + newText;
                                    }

                                    IType t = p.findType(fqcn);
                                    if (t != null && t.exists()) {
                                        return String.format("%1$s already exists", newText);
                                    }
                                }
                            } catch (CoreException e) {
                                AdtPlugin.log(e, null);
                            }
                        }

                        return null;
                    }
                };
                return mValidator;
            } else if (constraints.contains(Constraint.NONEMPTY)) {
                mValidator = new IInputValidator() {
                    @Override
                    public String isValid(String newText) {
                        if (newText.trim().isEmpty()) {
                            return String.format("Enter a value for %1$s", name);
                        }

                        return null;
                    }
                };
                return mValidator;
            }

            // TODO: Handle EXISTS, APILEVEL (which is currently handled manually in the
            // new project wizard, and never actually input by the user in a templated
            // wizard)

            mNoValidator = true;
        }

        return mValidator;
    }
}