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

package org.eclipse.andmore.internal.preferences;

import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.internal.build.DebugKeyProvider;
import com.android.sdklib.internal.build.DebugKeyProvider.KeytoolException;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.preferences.AdtPrefs.BuildVerbosity;
import org.eclipse.andmore.internal.utils.FingerprintUtils;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Preference page for build options.
 *
 */
public class BuildPreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    private IPreferenceStore mPrefStore = null;

    // default key store
    private ReadOnlyFieldEditor mDefaultKeyStore = null;
    private LabelField mDefaultFingerprintMd5 = null;
    private LabelField mDefaultFingerprintSha1 = null;

    // custom key store
    private KeystoreFieldEditor mCustomKeyStore = null;
    private LabelField mCustomFingerprintMd5 = null;
    private LabelField mCustomFingerprintSha1 = null;

    public BuildPreferencePage() {
        super(GRID);
        mPrefStore = AdtPlugin.getDefault().getPreferenceStore();
        setPreferenceStore(mPrefStore);
        setDescription(Messages.BuildPreferencePage_Title);
    }

    @Override
    protected void createFieldEditors() {
        addField(new BooleanFieldEditor(AdtPrefs.PREFS_BUILD_RES_AUTO_REFRESH,
                Messages.BuildPreferencePage_Auto_Refresh_Resources_on_Build,
                getFieldEditorParent()));

        addField(new BooleanFieldEditor(AdtPrefs.PREFS_BUILD_FORCE_ERROR_ON_NATIVELIB_IN_JAR,
                "Force error when external jars contain native libraries",
                getFieldEditorParent()));

        addField(new BooleanFieldEditor(AdtPrefs.PREFS_BUILD_SKIP_POST_COMPILE_ON_FILE_SAVE,
                "Skip packaging and dexing until export or launch. (Speeds up automatic builds on file save)",
                getFieldEditorParent()));

        RadioGroupFieldEditor rgfe = new RadioGroupFieldEditor(
                AdtPrefs.PREFS_BUILD_VERBOSITY,
                Messages.BuildPreferencePage_Build_Output, 1, new String[][] {
                    { Messages.BuildPreferencePage_Silent, BuildVerbosity.ALWAYS.name() },
                    { Messages.BuildPreferencePage_Normal, BuildVerbosity.NORMAL.name() },
                    { Messages.BuildPreferencePage_Verbose, BuildVerbosity.VERBOSE.name() }
                    },
                getFieldEditorParent(), true);
        addField(rgfe);

        // default debug keystore fingerprints
        Fingerprints defaultFingerprints = getFingerprints(
                mPrefStore.getString(AdtPrefs.PREFS_DEFAULT_DEBUG_KEYSTORE));

        // default debug key store fields
        mDefaultKeyStore = new ReadOnlyFieldEditor(AdtPrefs.PREFS_DEFAULT_DEBUG_KEYSTORE,
                Messages.BuildPreferencePage_Default_KeyStore, getFieldEditorParent());
        mDefaultFingerprintMd5 = new LabelField(
                Messages.BuildPreferencePage_Default_Certificate_Fingerprint_MD5,
                defaultFingerprints != null ? defaultFingerprints.md5 : "",
                getFieldEditorParent());
        mDefaultFingerprintSha1 = new LabelField(
                Messages.BuildPreferencePage_Default_Certificate_Fingerprint_SHA1,
                defaultFingerprints != null ? defaultFingerprints.sha1 : "",
                getFieldEditorParent());

        addField(mDefaultKeyStore);
        addField(mDefaultFingerprintMd5);
        addField(mDefaultFingerprintSha1);

        // custom debug keystore fingerprints
        Fingerprints customFingerprints = null;

        String customDebugKeystorePath = mPrefStore.getString(AdtPrefs.PREFS_CUSTOM_DEBUG_KEYSTORE);
        if (new File(customDebugKeystorePath).isFile()) {
            customFingerprints = getFingerprints(customDebugKeystorePath);
        } else {
            // file does not exist.
            setErrorMessage("Not a valid keystore path.");
        }

        // custom debug key store fields
        mCustomKeyStore = new KeystoreFieldEditor(AdtPrefs.PREFS_CUSTOM_DEBUG_KEYSTORE,
                Messages.BuildPreferencePage_Custom_Keystore, getFieldEditorParent());
        mCustomFingerprintMd5 = new LabelField(
                Messages.BuildPreferencePage_Default_Certificate_Fingerprint_MD5,
                customFingerprints != null ? customFingerprints.md5 : "",
                getFieldEditorParent());
        mCustomFingerprintSha1 = new LabelField(
                Messages.BuildPreferencePage_Default_Certificate_Fingerprint_SHA1,
                customFingerprints != null ? customFingerprints.sha1 : "",
                getFieldEditorParent());

        // set fingerprint fields
        mCustomKeyStore.setFingerprintMd5Field(mCustomFingerprintMd5);
        mCustomKeyStore.setFingerprintSha1Field(mCustomFingerprintSha1);

        addField(mCustomKeyStore);
        addField(mCustomFingerprintMd5);
        addField(mCustomFingerprintSha1);
    }

    /**
     * MD5 & SHA1 fingerprints.
     */
    private static class Fingerprints {
        final String md5;
        final String sha1;

        Fingerprints(String md5Val, String sha1Val) {
            md5 = md5Val;
            sha1 = sha1Val;
        }
    }

    private Fingerprints getFingerprints(String keystorePath) {
        // attempt to load the debug key.
        try {
            DebugKeyProvider keyProvider = new DebugKeyProvider(keystorePath,
                    null /* storeType */, null /* key gen output */);

            return new Fingerprints(
                    FingerprintUtils.getFingerprint(keyProvider.getCertificate(), "MD5"),
                    FingerprintUtils.getFingerprint(keyProvider.getCertificate(), "SHA1"));
        } catch (GeneralSecurityException e) {
            setErrorMessage(e.getMessage());
        } catch (IOException e) {
            setErrorMessage(e.getMessage());
        } catch (KeytoolException e) {
            setErrorMessage(e.getMessage());
        } catch (AndroidLocationException e) {
            setErrorMessage(e.getMessage());
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performDefaults
     * (org.eclipse.jface.preference.PreferencePage#performDefaults)
     */
    @Override
    protected void performDefaults() {
        super.performDefaults();

        // restore the default key store fingerprints
        Fingerprints defaultFingerprints = getFingerprints(mPrefStore
                .getString(AdtPrefs.PREFS_DEFAULT_DEBUG_KEYSTORE));
        mDefaultFingerprintMd5.setStringValue(defaultFingerprints.md5);
        mDefaultFingerprintSha1.setStringValue(defaultFingerprints.sha1);

        // set custom fingerprint fields to blank
        mCustomFingerprintMd5.setStringValue("");
        mCustomFingerprintSha1.setStringValue("");
    }

    /**
     * A read-only string field editor.
     */
    private static class ReadOnlyFieldEditor extends StringFieldEditor {

        public ReadOnlyFieldEditor(String name, String labelText, Composite parent) {
            super(name, labelText, parent);
        }

        @Override
        protected void createControl(Composite parent) {
            super.createControl(parent);

            Text control = getTextControl();
            control.setEditable(false);
        }
    }

    /**
     * A read-only string field.
     */
    private static class LabelField extends StringFieldEditor {
        private String text;

        public LabelField(String labelText, String value, Composite parent) {
            super("", labelText, parent);
            text = value;
        }

        @Override
        protected void createControl(Composite parent) {
            super.createControl(parent);

            Text control = getTextControl();
            control.setEditable(false);
        }

        @Override
        protected void doLoad() {
            setStringValue(text);
        }

        @Override
        protected void doStore() {
            // Do nothing
        }
    }

    /**
     * Custom {@link FileFieldEditor} that checks that the keystore is valid.
     */
    private static class KeystoreFieldEditor extends FileFieldEditor {
        private StringFieldEditor fingerprintMd5 = null;
        private StringFieldEditor fingerprintSha1 = null;

        public void setFingerprintMd5Field(StringFieldEditor field) {
            fingerprintMd5 = field;
        }

        public void setFingerprintSha1Field(StringFieldEditor field) {
            fingerprintSha1 = field;
        }

        public KeystoreFieldEditor(String name, String label, Composite parent) {
            super(name, label, parent);
            setValidateStrategy(VALIDATE_ON_KEY_STROKE);
        }

        @Override
        protected boolean checkState() {
            String fileName = getTextControl().getText();
            fileName = fileName.trim();

            if (fingerprintMd5 != null) {
                fingerprintMd5.setStringValue("");
            }

            if (fingerprintSha1 != null) {
                fingerprintSha1.setStringValue("");
            }

            // empty values are considered ok.
            if (fileName.length() > 0) {
                File file = new File(fileName);
                if (file.isFile()) {
                    // attempt to load the debug key.
                    try {
                        DebugKeyProvider provider = new DebugKeyProvider(fileName,
                                null /* storeType */, null /* key gen output */);
                        PrivateKey key = provider.getDebugKey();
                        X509Certificate certificate = (X509Certificate)provider.getCertificate();

                        if (key == null || certificate == null) {
                            showErrorMessage("Unable to find debug key in keystore!");
                            return false;
                        }

                        if (fingerprintMd5 != null) {
                            fingerprintMd5.setStringValue(
                                    FingerprintUtils.getFingerprint(certificate, "MD5"));
                        }

                        if (fingerprintSha1 != null) {
                            fingerprintSha1.setStringValue(
                                    FingerprintUtils.getFingerprint(certificate, "SHA1"));
                        }

                        Date today = new Date();
                        if (certificate.getNotAfter().compareTo(today) < 0) {
                            showErrorMessage("Certificate is expired!");
                            return false;
                        }

                        if (certificate.getNotBefore().compareTo(today) > 0) {
                            showErrorMessage("Certificate validity is in the future!");
                            return false;
                        }

                        // we're good!
                        clearErrorMessage();
                        return true;
                    } catch (GeneralSecurityException e) {
                        handleException(e);
                        return false;
                    } catch (IOException e) {
                        handleException(e);
                        return false;
                    } catch (KeytoolException e) {
                        handleException(e);
                        return false;
                    } catch (AndroidLocationException e) {
                        handleException(e);
                        return false;
                    }


                } else {
                    // file does not exist.
                    showErrorMessage("Not a valid keystore path.");
                    return false;  // Apply/OK must be disabled
                }
            }

            clearErrorMessage();
            return true;
        }

        @Override
        public Text getTextControl(Composite parent) {
            setValidateStrategy(VALIDATE_ON_KEY_STROKE);
            return super.getTextControl(parent);
        }

        /**
         * Set the error message from a {@link Throwable}. If the exception has no message, try
         * to get the message from the cause.
         * @param t the Throwable.
         */
        private void handleException(Throwable t) {
            String msg = t.getMessage();
            if (msg == null) {
                Throwable cause = t.getCause();
                if (cause != null) {
                    handleException(cause);
                } else {
                    setErrorMessage("Uknown error when getting the debug key!");
                }

                return;
            }

            // valid text, display it.
            showErrorMessage(msg);
        }
    }
}
