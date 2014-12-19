/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.resources.manager;

import com.android.SdkConstants;
import com.android.ide.common.resources.ResourceFile;
import com.android.ide.common.resources.ResourceFolder;
import com.android.ide.common.resources.ResourceItem;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.resources.SingleResourceFile;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.io.IAbstractFolder;
import com.android.io.IAbstractResource;
import com.android.resources.Keyboard;
import com.android.resources.KeyboardState;
import com.android.resources.Navigation;
import com.android.resources.NavigationState;
import com.android.resources.NightMode;
import com.android.resources.ResourceFolderType;
import com.android.resources.ScreenOrientation;
import com.android.resources.TouchScreen;
import com.android.resources.UiMode;

import org.eclipse.andmore.internal.resources.manager.ProjectResources;
import org.eclipse.andmore.io.IFileWrapper;
import org.eclipse.andmore.io.IFolderWrapper;
import org.eclipse.andmore.mock.Mocks;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import junit.framework.TestCase;

public class ConfigMatchTest extends TestCase {
    private static final String SEARCHED_FILENAME = "main.xml"; //$NON-NLS-1$
    private static final String MISC1_FILENAME = "foo.xml"; //$NON-NLS-1$
    private static final String MISC2_FILENAME = "bar.xml"; //$NON-NLS-1$

    private FolderConfiguration mDefaultConfig;
    private ResourceRepository mResources;
    private FolderConfiguration config4;
    private FolderConfiguration config3;
    private FolderConfiguration config2;
    private FolderConfiguration config1;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create a default config with all qualifiers.
        mDefaultConfig = new FolderConfiguration();
        mDefaultConfig.createDefault();

        IAbstractFolder folder = Mocks.createAbstractFolder(
                SdkConstants.FD_RESOURCES, new IAbstractResource[0]);

        // create the project resources.
        mResources = new ResourceRepository(folder, false) {
            @Override
            protected ResourceItem createResourceItem(String name) {
                return new ResourceItem(name);
            }
        };

        // create 2 arrays of IResource. one with the filename being looked up, and one without.
        // Since the required API uses IResource, we can use MockFolder for them.
        IFile[] validMemberList = new IFile[] {
                Mocks.createFile(MISC1_FILENAME),
                Mocks.createFile(SEARCHED_FILENAME),
                Mocks.createFile(MISC2_FILENAME),
        };
        IFile[] invalidMemberList = new IFile[] {
                Mocks.createFile(MISC1_FILENAME),
                Mocks.createFile(MISC2_FILENAME),
        };

        // add multiple ResourceFolder to the project resource.
        FolderConfiguration defaultConfig = getConfiguration(
                null, // country code
                null, // network code
                null, // language
                null, // region
                null, // smallest width dp
                null, // width dp
                null, // height dp
                null, // screen size
                null, // screen ratio
                null, // screen orientation
                null, // dock mode
                null, // night mode
                null, // dpi
                null, // touch mode
                null, // keyboard state
                null, // text input
                null, // navigation state
                null, // navigation method
                null, // screen dimension
                null);// version

        addFolder(mResources, defaultConfig, validMemberList);

        config1 = getConfiguration(
                null, // country code
                null, // network code
                "en", // language
                null, // region
                null, // smallest width dp
                null, // width dp
                null, // height dp
                null, // screen size
                null, // screen ratio
                null, // screen orientation
                null, // dock mode
                null, // night mode
                null, // dpi
                null, // touch mode
                KeyboardState.EXPOSED.getResourceValue(), // keyboard state
                null, // text input
                null, // navigation state
                null, // navigation method
                null, // screen dimension
                null);// version

        addFolder(mResources, config1, validMemberList);

        config2 = getConfiguration(
                null, // country code
                null, // network code
                "en", // language
                null, // region
                null, // smallest width dp
                null, // width dp
                null, // height dp
                null, // screen size
                null, // screen ratio
                null, // screen orientation
                null, // dock mode
                null, // night mode
                null, // dpi
                null, // touch mode
                KeyboardState.HIDDEN.getResourceValue(), // keyboard state
                null, // text input
                null, // navigation state
                null, // navigation method
                null, // screen dimension
                null);// version

        addFolder(mResources, config2, validMemberList);

        config3 = getConfiguration(
                null, // country code
                null, // network code
                "en", // language
                null, // region
                null, // smallest width dp
                null, // width dp
                null, // height dp
                null, // screen size
                null, // screen ratio
                ScreenOrientation.LANDSCAPE.getResourceValue(), // screen orientation
                null, // dock mode
                null, // night mode
                null, // dpi
                null, // touch mode
                null, // keyboard state
                null, // text input
                null, // navigation state
                null, // navigation method
                null, // screen dimension
                null);// version

        addFolder(mResources, config3, validMemberList);

        config4 = getConfiguration(
                "mcc310", // country code
                "mnc435", // network code
                "en", // language
                "rUS", // region
                null, // smallest width dp
                null, // width dp
                null, // height dp
                "normal", // screen size
                "notlong", // screen ratio
                ScreenOrientation.LANDSCAPE.getResourceValue(), // screen orientation
                UiMode.DESK.getResourceValue(), // dock mode
                NightMode.NIGHT.getResourceValue(), // night mode
                "mdpi", // dpi
                TouchScreen.FINGER.getResourceValue(), // touch mode
                KeyboardState.EXPOSED.getResourceValue(), // keyboard state
                Keyboard.QWERTY.getResourceValue(), // text input
                NavigationState.EXPOSED.getResourceValue(), // navigation state
                Navigation.DPAD.getResourceValue(), // navigation method
                "480x320", // screen dimension
                "v3"); // version

        addFolder(mResources, config4, invalidMemberList);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mResources = null;
    }

    public void test1() {
        FolderConfiguration testConfig = getConfiguration(
                "mcc310", // country code
                "mnc435", // network code
                "en", // language
                "rUS", // region
                null, // smallest width dp
                null, // width dp
                null, // height dp
                "normal", // screen size
                "notlong", // screen ratio
                ScreenOrientation.LANDSCAPE.getResourceValue(), // screen orientation
                UiMode.DESK.getResourceValue(), // dock mode
                NightMode.NIGHT.getResourceValue(), // night mode
                "mdpi", // dpi
                TouchScreen.FINGER.getResourceValue(), // touch mode
                KeyboardState.EXPOSED.getResourceValue(), // keyboard state
                Keyboard.QWERTY.getResourceValue(), // text input
                NavigationState.EXPOSED.getResourceValue(), // navigation state
                Navigation.DPAD.getResourceValue(), // navigation method
                "480x320", // screen dimension
                "v3"); // version

        ResourceFile result = mResources.getMatchingFile(SEARCHED_FILENAME,
                ResourceFolderType.LAYOUT, testConfig);

        boolean bresult = result.getFolder().getConfiguration().equals(config3);
        assertEquals(bresult, true);
    }

    /**
     * Creates a {@link FolderConfiguration}.
     * @param qualifierValues The list of qualifier values. The length must equals the total number
     * of Qualifiers. <code>null</code> is permitted and will make the FolderConfiguration not use
     * this particular qualifier.
     */
    private FolderConfiguration getConfiguration(String... qualifierValues) {
        // FolderConfiguration.getQualifierCount is always valid and up to date.
        final int count = FolderConfiguration.getQualifierCount();

        // Check we have the right number of qualifier.
        assertEquals(qualifierValues.length, count);

        FolderConfiguration config = new FolderConfiguration();

        for (int i = 0 ; i < count ; i++) {
            String value = qualifierValues[i];
            if (value != null) {
                assertTrue(mDefaultConfig.getQualifier(i).checkAndSet(value, config));
            }
        }

        return config;
    }

    /**
     * Adds a folder to the given {@link ProjectResources} with the given
     * {@link FolderConfiguration}. The folder is filled with files from the provided list.
     * @param resources the {@link ResourceRepository} in which to add the folder.
     * @param config the {@link FolderConfiguration} for the created folder.
     * @param memberList the list of files for the folder.
     */
    private void addFolder(ResourceRepository resources, FolderConfiguration config,
            IFile[] memberList) throws Exception {

        // figure out the folder name based on the configuration
        String folderName = config.getFolderName(ResourceFolderType.LAYOUT);

        // create the folder mock
        IFolder folder = Mocks.createFolder(folderName, memberList);

        // add it to the resource, and get back a ResourceFolder object.
        ResourceFolder resFolder = resources.processFolder(new IFolderWrapper(folder));

        // and fill it with files from the list.
        for (IFile file : memberList) {
            resFolder.addFile(new SingleResourceFile(new IFileWrapper(file), resFolder));
        }
    }
}
