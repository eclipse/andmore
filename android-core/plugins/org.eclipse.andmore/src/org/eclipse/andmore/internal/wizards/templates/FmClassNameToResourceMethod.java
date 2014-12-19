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

import static org.eclipse.andmore.internal.wizards.templates.NewProjectPage.ACTIVITY_NAME_SUFFIX;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import java.util.List;

import org.eclipse.andmore.AdtUtils;

/**
 * Similar to {@link FmCamelCaseToUnderscoreMethod}, but strips off common class
 * suffixes such as "Activity", "Fragment", etc.
 */
public class FmClassNameToResourceMethod implements TemplateMethodModel {
    @Override
    public TemplateModel exec(List args) throws TemplateModelException {
        if (args.size() != 1) {
            throw new TemplateModelException("Wrong arguments");
        }

        String name = args.get(0).toString();

        if (name.isEmpty()) {
            return new SimpleScalar("");
        }

        name = stripSuffix(name, ACTIVITY_NAME_SUFFIX);
        name = stripSuffix(name, "Fragment");              //$NON-NLS-1$
        name = stripSuffix(name, "Service");               //$NON-NLS-1$
        name = stripSuffix(name, "Provider");              //$NON-NLS-1$

        return new SimpleScalar(AdtUtils.camelCaseToUnderlines(name));
    }

    // Strip off the end portion of the activity name. The user might be typing
    // the activity name such that only a portion has been entered so far (e.g.
    // "MainActivi") and we want to chop off that portion too such that we don't
    private static String stripSuffix(String name, String suffix) {
        int suffixStart = name.lastIndexOf(suffix.charAt(0));
        if (suffixStart != -1 && name.regionMatches(suffixStart, suffix, 0,
                name.length() - suffixStart)) {
            name = name.substring(0, suffixStart);
        }
        assert !name.endsWith(suffix) : name;

        return name;
    }
}