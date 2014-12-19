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

import static com.android.tools.lint.detector.api.LintUtils.assertionsEnabled;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.AdtPlugin;

/**
 * A template handler which can evaluate simple strings. Used to evaluate
 * parameter constraints during UI wizard value editing.
 * <p>
 * Unlike the more general {@link TemplateHandler} which is used to instantiate
 * full template files (from resources, merging into existing files etc) this
 * evaluator supports only simple strings, referencing only values from the
 * provided map (and builtin functions).
 */
class StringEvaluator implements TemplateLoader {
    private Map<String, Object> mParameters;
    private Configuration mFreemarker;
    private String mCurrentExpression;

    StringEvaluator() {
        mParameters = TemplateHandler.createBuiltinMap();

        mFreemarker = new Configuration();
        mFreemarker.setObjectWrapper(new DefaultObjectWrapper());
        mFreemarker.setTemplateLoader(this);
    }

    /** Evaluates the given expression, with the given set of parameters */
    @Nullable
    String evaluate(@NonNull String expression, @NonNull List<Parameter> parameters) {
        // Render the instruction list template.
        for (Parameter parameter : parameters) {
            mParameters.put(parameter.id, parameter.value);
        }
        try {
            mCurrentExpression = expression;
            Template inputsTemplate = mFreemarker.getTemplate(expression);
            StringWriter out = new StringWriter();
            inputsTemplate.process(mParameters, out);
            out.flush();
            return out.toString();
        } catch (Exception e) {
            if (assertionsEnabled()) {
                AdtPlugin.log(e, null);
            }
            return null;
        }
    }

    // ---- Implements TemplateLoader ----

    @Override
    public Object findTemplateSource(String name) throws IOException {
        return mCurrentExpression;
    }

    @Override
    public long getLastModified(Object templateSource) {
        return 0;
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        return new StringReader(mCurrentExpression);
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
    }
}
