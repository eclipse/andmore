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
package org.eclipse.andmore.internal.lint;


import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.checks.AccessibilityDetector;
import com.android.tools.lint.checks.DetectMissingPrefix;
import com.android.tools.lint.checks.DosLineEndingDetector;
import com.android.tools.lint.checks.HardcodedValuesDetector;
import com.android.tools.lint.checks.InefficientWeightDetector;
import com.android.tools.lint.checks.ManifestDetector;
import com.android.tools.lint.checks.MissingIdDetector;
import com.android.tools.lint.checks.ObsoleteLayoutParamsDetector;
import com.android.tools.lint.checks.PxUsageDetector;
import com.android.tools.lint.checks.ScrollViewChildDetector;
import com.android.tools.lint.checks.SecurityDetector;
import com.android.tools.lint.checks.TextFieldDetector;
import com.android.tools.lint.checks.TranslationDetector;
import com.android.tools.lint.checks.TypoDetector;
import com.android.tools.lint.checks.TypographyDetector;
import com.android.tools.lint.checks.UseCompoundDrawableDetector;
import com.android.tools.lint.checks.UselessViewDetector;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Issue.OutputFormat;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class LintFix implements ICompletionProposal {
    protected final IMarker mMarker;
    protected final String mId;

    protected LintFix(String id, IMarker marker) {
        mId = id;
        mMarker = marker;
    }

    /**
     * Returns true if this fix needs focus (which means that when the fix is
     * performed from for example a {@link LintListDialog}'s Fix button) the
     * editor needs to be given focus.
     *
     * @return true if this fix needs focus after being applied
     */
    public boolean needsFocus() {
        return true;
    }

    /**
     * Returns true if this fix can be performed along side other fixes
     *
     * @return true if this fix can be performed in a bulk operation with other
     *         fixes
     */
    public boolean isBulkCapable() {
        return false;
    }

    /**
     * Returns true if this fix can be cancelled once it's invoked. This is the case
     * for fixes which shows a confirmation dialog (such as the Extract String etc).
     * This will be used to determine whether the marker can be deleted immediately
     * (for non-cancelable fixes) or if it should be left alone and detected fix
     * on the next save.
     *
     * @return true if the fix can be cancelled
     */
    public boolean isCancelable() {
        return true;
    }

    // ---- Implements ICompletionProposal ----

    @Override
    public String getDisplayString() {
        return null;
    }

    @Override
    public String getAdditionalProposalInfo() {
        Issue issue = EclipseLintClient.getRegistry().getIssue(mId);
        if (issue != null) {
            return issue.getExplanation(OutputFormat.HTML);
        }

        return null;
    }

    public void deleteMarker() {
        try {
            mMarker.delete();
        } catch (PartInitException e) {
            AdtPlugin.log(e, null);
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
        }
    }

    @Override
    public Point getSelection(IDocument document) {
        return null;
    }

    @Override
    public Image getImage() {
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        return sharedImages.getImage(ISharedImages.IMG_OBJS_WARN_TSK);
    }

    @Override
    public IContextInformation getContextInformation() {
        return null;
    }

    // --- Access to available fixes ---

    private static final Map<String, Class<? extends LintFix>> sFixes =
            new HashMap<String, Class<? extends LintFix>>();
    // Keep this map in sync with BuiltinIssueRegistry's hasAutoFix() data
    static {
        sFixes.put(InefficientWeightDetector.INEFFICIENT_WEIGHT.getId(),
                LinearLayoutWeightFix.class);
        sFixes.put(AccessibilityDetector.ISSUE.getId(), SetAttributeFix.class);
        sFixes.put(InefficientWeightDetector.BASELINE_WEIGHTS.getId(), SetAttributeFix.class);
        sFixes.put(ManifestDetector.ALLOW_BACKUP.getId(), SetAttributeFix.class);
        sFixes.put(MissingIdDetector.ISSUE.getId(), SetAttributeFix.class);
        sFixes.put(HardcodedValuesDetector.ISSUE.getId(), ExtractStringFix.class);
        sFixes.put(UselessViewDetector.USELESS_LEAF.getId(), RemoveUselessViewFix.class);
        sFixes.put(UselessViewDetector.USELESS_PARENT.getId(), RemoveUselessViewFix.class);
        sFixes.put(PxUsageDetector.PX_ISSUE.getId(), ConvertToDpFix.class);
        sFixes.put(TextFieldDetector.ISSUE.getId(), SetAttributeFix.class);
        sFixes.put(SecurityDetector.EXPORTED_SERVICE.getId(), SetAttributeFix.class);
        sFixes.put(TranslationDetector.MISSING.getId(), SetAttributeFix.class);
        sFixes.put(DetectMissingPrefix.MISSING_NAMESPACE.getId(), AddPrefixFix.class);
        sFixes.put(ScrollViewChildDetector.ISSUE.getId(), SetScrollViewSizeFix.class);
        sFixes.put(ObsoleteLayoutParamsDetector.ISSUE.getId(), ObsoleteLayoutParamsFix.class);
        sFixes.put(TypographyDetector.DASHES.getId(), TypographyFix.class);
        sFixes.put(TypographyDetector.ELLIPSIS.getId(), TypographyFix.class);
        sFixes.put(TypographyDetector.FRACTIONS.getId(), TypographyFix.class);
        sFixes.put(TypographyDetector.OTHER.getId(), TypographyFix.class);
        sFixes.put(TypographyDetector.QUOTES.getId(), TypographyFix.class);
        sFixes.put(UseCompoundDrawableDetector.ISSUE.getId(),
                UseCompoundDrawableDetectorFix.class);
        sFixes.put(TypoDetector.ISSUE.getId(), TypoFix.class);
        sFixes.put(DosLineEndingDetector.ISSUE.getId(), DosLineEndingsFix.class);
        // ApiDetector.UNSUPPORTED is provided as a marker resolution rather than
        // a quick assistant (the marker resolution adds a suitable @TargetApi annotation)
    }

    public static boolean hasFix(String id) {
        return sFixes.containsKey(id);
    }

    /**
     * Returns one or more fixes for the given issue, or null if no fixes are available
     *
     * @param id the id o the issue to obtain a fix for (see {@link Issue#getId()})
     * @param marker the marker corresponding to the error
     * @return a nonempty list of fix, or null
     */
    @Nullable
    public static List<LintFix> getFixes(@NonNull String id, @NonNull IMarker marker) {
        Class<? extends LintFix> clazz = sFixes.get(id);
        if (clazz != null) {
            try {
                Constructor<? extends LintFix> constructor = clazz.getDeclaredConstructor(
                        String.class, IMarker.class);
                constructor.setAccessible(true);
                LintFix fix = constructor.newInstance(id, marker);
                List<LintFix> alternatives = fix.getAllFixes();
                if (alternatives != null) {
                    return alternatives;
                } else {
                    return Collections.singletonList(fix);
                }
            } catch (Throwable t) {
                AdtPlugin.log(t, null);
            }
        }

        return null;
    }

    /**
     * Returns a full list of fixes for this issue. This will produce a list of
     * multiple fixes, in the desired order, which provide alternative ways of
     * fixing the issue.
     *
     * @return a list of fixes to fix this issue, or null if there are no
     *         variations
     */
    @Nullable
    protected List<LintFix> getAllFixes() {
        return null;
    }
}
