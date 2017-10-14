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

package org.eclipse.andmore.internal.editors.manifest;

import static com.android.xml.AndroidManifest.ATTRIBUTE_MIN_SDK_VERSION;
import static com.android.xml.AndroidManifest.ATTRIBUTE_TARGET_SDK_VERSION;

import com.android.annotations.VisibleForTesting;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.AndroidContentAssist;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Content Assist Processor for AndroidManifest.xml
 */
@VisibleForTesting
public final class ManifestContentAssist extends AndroidContentAssist {

    /**
     * Constructor for ManifestContentAssist
     */
    public ManifestContentAssist() {
        super(AndroidTargetData.DESCRIPTOR_MANIFEST);
    }

    @Override
    protected boolean computeAttributeValues(List<ICompletionProposal> proposals, int offset,
            String parentTagName, String attributeName, Node node, String wordPrefix,
            boolean skipEndTag, int replaceLength) {
        if (attributeName.endsWith(ATTRIBUTE_MIN_SDK_VERSION)
                || attributeName.endsWith(ATTRIBUTE_TARGET_SDK_VERSION)) {
            // The user is completing the minSdkVersion attribute: it should be
            // an integer for the API version, but we'll add full Android version
            // names to make it more obvious what they're selecting

            List<Pair<String, String>> choices = new ArrayList<Pair<String, String>>();
            int max = AdtUtils.getHighestKnownApiLevel();
            // Look for any more recent installed versions the user may have
            Sdk sdk = Sdk.getCurrent();
            if (sdk == null) {
                return false;
            }
            for (IAndroidTarget target : sdk.getTargets()) {
                AndroidVersion version = target.getVersion();
                int apiLevel = version.getApiLevel();
                if (apiLevel > max) {
                    if (version.isPreview()) {
                        // Use codename, not API level, as version string for preview versions
                        choices.add(Pair.of(version.getCodename(), version.getCodename()));
                    } else {
                        choices.add(Pair.of(Integer.toString(apiLevel), target.getFullName()));
                    }
                }
            }
            for (int api = max; api >= 1; api--) {
                String name = AdtUtils.getAndroidName(api);
                choices.add(Pair.of(Integer.toString(api), name));
            }
            char needTag = 0;
            addMatchingProposals(proposals, choices.toArray(), offset, node, wordPrefix,
                    needTag, true /* isAttribute */, false /* isNew */,
                    skipEndTag /* skipEndTag */, replaceLength);
            return true;
        } else {
            return super.computeAttributeValues(proposals, offset, parentTagName, attributeName,
                    node, wordPrefix, skipEndTag, replaceLength);
        }
    }
}
