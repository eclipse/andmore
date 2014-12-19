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
package org.eclipse.andmore.internal.editors.layout.configuration;

import static com.android.ide.common.resources.configuration.LanguageQualifier.FAKE_LANG_VALUE;
import static com.android.ide.common.resources.configuration.RegionQualifier.FAKE_REGION_VALUE;

import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.LanguageQualifier;
import com.android.resources.Density;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.DeviceManager;
import com.android.sdklib.devices.Screen;
import com.android.utils.StdLogger;

import java.lang.reflect.Constructor;
import java.util.List;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class ConfigurationTest extends TestCase {
    private Configuration createConfiguration() throws Exception {
        // Using reflection instead since we want to pass null to
        // a constructor marked with @NonNull, so the test won't compile.
        Constructor<Configuration> constructor =
                Configuration.class.getDeclaredConstructor(ConfigurationChooser.class);
        constructor.setAccessible(true);
        ConfigurationChooser chooser = null;
        return constructor.newInstance(chooser);
    }

    public void test() throws Exception {
        Configuration configuration = createConfiguration();
        assertNotNull(configuration);
        configuration.setTheme("@style/Theme");
        assertEquals("@style/Theme", configuration.getTheme());

        DeviceManager deviceManager = DeviceManager.createInstance(
                                                        null /*osSdkPath*/,
                                                        new StdLogger(StdLogger.Level.VERBOSE));
        List<Device> devices = deviceManager.getDevices(DeviceManager.DeviceFilter.DEFAULT);
        assertNotNull(devices);
        assertTrue(devices.size() > 0);
        configuration.setDevice(devices.get(0), false);

        // Check syncing
        FolderConfiguration folderConfig = configuration.getFullConfig();
        assertEquals(FAKE_LANG_VALUE, folderConfig.getLanguageQualifier().getValue());
        assertEquals(FAKE_REGION_VALUE, folderConfig.getRegionQualifier().getValue());
        assertEquals(Locale.ANY, configuration.getLocale());

        Locale language = Locale.create(new LanguageQualifier("nb"));
        configuration.setLocale(language, true /* skipSync */);
        assertEquals(FAKE_LANG_VALUE, folderConfig.getLanguageQualifier().getValue());
        assertEquals(FAKE_REGION_VALUE, folderConfig.getRegionQualifier().getValue());

        configuration.setLocale(language, false /* skipSync */);
        assertEquals(FAKE_REGION_VALUE, folderConfig.getRegionQualifier().getValue());
        assertEquals("nb", folderConfig.getLanguageQualifier().getValue());

        assertEquals("2.7in QVGA::nb-__:+Theme::notnight::", configuration.toPersistentString());

        configuration.setActivity("foo.bar.FooActivity");
        configuration.setTheme("@android:style/Theme.Holo.Light");

        assertEquals("2.7in QVGA",
                ConfigurationChooser.getDeviceLabel(configuration.getDevice(), true));
        assertEquals("2.7in QVGA",
                ConfigurationChooser.getDeviceLabel(configuration.getDevice(), false));
        assertEquals("Light",
                ConfigurationChooser.getThemeLabel(configuration.getTheme(), true));
        assertEquals("Theme.Holo.Light",
                ConfigurationChooser.getThemeLabel(configuration.getTheme(), false));
        assertEquals("nb",
                ConfigurationChooser.getLocaleLabel(null, configuration.getLocale(), true));
        assertEquals("Norwegian Bokm\u00e5l (nb)",
                ConfigurationChooser.getLocaleLabel(null, configuration.getLocale(), false));

        assertEquals("FooActivity",
                ConfigurationChooser.getActivityLabel(configuration.getActivity(), true));
        assertEquals("foo.bar.FooActivity",
                ConfigurationChooser.getActivityLabel(configuration.getActivity(), false));

        assertEquals("2.7in QVGA::nb-__:-Theme.Holo.Light::notnight::foo.bar.FooActivity",
                configuration.toPersistentString());

        assertEquals(Density.MEDIUM, configuration.getDensity());
        Screen screen = configuration.getDevice().getDefaultHardware().getScreen();
        assertEquals(145.0f, screen.getXdpi(), 0.001);
        assertEquals(145.0f, screen.getYdpi(), 0.001);
    }

    public void testCopy() throws Exception {
        Configuration configuration = createConfiguration();
        assertNotNull(configuration);
        configuration.setTheme("@style/Theme");
        assertEquals("@style/Theme", configuration.getTheme());
        DeviceManager deviceManager = DeviceManager.createInstance(
                                            null /*osSdkPath*/,
                                            new StdLogger(StdLogger.Level.VERBOSE));
        List<Device> devices = deviceManager.getDevices(DeviceManager.DeviceFilter.DEFAULT);
        assertNotNull(devices);
        assertTrue(devices.size() > 0);
        configuration.setDevice(devices.get(0), false);
        configuration.setActivity("foo.bar.FooActivity");
        configuration.setTheme("@android:style/Theme.Holo.Light");
        Locale locale = Locale.create(new LanguageQualifier("nb"));
        configuration.setLocale(locale, false /* skipSync */);

        Configuration copy = Configuration.copy(configuration);
        assertEquals(locale, copy.getLocale());
        assertEquals("foo.bar.FooActivity", copy.getActivity());
        assertEquals("@android:style/Theme.Holo.Light", copy.getTheme());
        assertEquals(devices.get(0), copy.getDevice());

        // Make sure edits to master does not affect the child
        configuration.setLocale(Locale.ANY, false);
        configuration.setTheme("@android:style/Theme.Holo");
        configuration.setDevice(devices.get(1), true);

        assertTrue(copy.getFullConfig().getLanguageQualifier().equals(locale.language));
        assertEquals(locale, copy.getLocale());
        assertEquals("foo.bar.FooActivity", copy.getActivity());
        assertEquals("@android:style/Theme.Holo.Light", copy.getTheme());
        assertEquals(devices.get(0), copy.getDevice());
    }
}
