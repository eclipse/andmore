/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdkuilib.internal.repository;

import com.android.sdklib.internal.repository.DownloadCache;
import com.android.sdklib.internal.repository.DownloadCache.Strategy;
import com.android.sdklib.internal.repository.updater.ISettingsPage;
import com.android.sdklib.internal.repository.updater.SettingsController;
import com.android.sdklib.util.FormatUtils;
import com.android.sdkuilib.ui.GridDataBuilder;
import com.android.sdkuilib.ui.GridLayoutBuilder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.util.Properties;


public class SettingsDialog extends UpdaterBaseDialog implements ISettingsPage {


    // data members
    private final DownloadCache mDownloadCache = new DownloadCache(Strategy.SERVE_CACHE);
    private final SettingsController mSettingsController;
    private SettingsChangedCallback mSettingsChangedCallback;

    // UI widgets
    private Text mTextProxyServer;
    private Text mTextProxyPort;
    private Text mTextCacheSize;
    private Button mCheckUseCache;
    private Button mCheckForceHttp;
    private Button mCheckAskAdbRestart;
    private Button mCheckEnablePreviews;

    private SelectionAdapter mApplyOnSelected = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
            applyNewSettings(); //$hide$
        }
    };

    private ModifyListener mApplyOnModified = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
            applyNewSettings(); //$hide$
        }
    };

    public SettingsDialog(Shell parentShell, SwtUpdaterData swtUpdaterData) {
        super(parentShell, swtUpdaterData, "Settings" /*title*/);
        assert swtUpdaterData != null;
        mSettingsController = swtUpdaterData.getSettingsController();
    }

    @Override
    protected void createShell() {
        super.createShell();
        Shell shell = getShell();
        shell.setMinimumSize(new Point(450, 370));
        shell.setSize(450, 400);
    }

    @Override
    protected void createContents() {
        super.createContents();
        Shell shell = getShell();

        Group group = new Group(shell, SWT.NONE);
        group.setText("Proxy Settings");
        GridDataBuilder.create(group).fill().grab().hSpan(2);
        GridLayoutBuilder.create(group).columns(2);

        Label label = new Label(group, SWT.NONE);
        GridDataBuilder.create(label).hRight().vCenter();
        label.setText("HTTP Proxy Server");
        String tooltip = "The hostname or IP of the HTTP & HTTPS proxy server to use (e.g. proxy.example.com).\n" +
                         "When empty, the default Java proxy setting is used.";
        label.setToolTipText(tooltip);

        mTextProxyServer = new Text(group, SWT.BORDER);
        GridDataBuilder.create(mTextProxyServer).hFill().hGrab().vCenter();
        mTextProxyServer.addModifyListener(mApplyOnModified);
        mTextProxyServer.setToolTipText(tooltip);

        label = new Label(group, SWT.NONE);
        GridDataBuilder.create(label).hRight().vCenter();
        label.setText("HTTP Proxy Port");
        tooltip = "The port of the HTTP & HTTPS proxy server to use (e.g. 3128).\n" +
                  "When empty, the default Java proxy setting is used.";
        label.setToolTipText(tooltip);

        mTextProxyPort = new Text(group, SWT.BORDER);
        GridDataBuilder.create(mTextProxyPort).hFill().hGrab().vCenter();
        mTextProxyPort.addModifyListener(mApplyOnModified);
        mTextProxyPort.setToolTipText(tooltip);

        // ----
        group = new Group(shell, SWT.NONE);
        group.setText("Manifest Cache");
        GridDataBuilder.create(group).fill().grab().hSpan(2);
        GridLayoutBuilder.create(group).columns(3);

        label = new Label(group, SWT.NONE);
        GridDataBuilder.create(label).hRight().vCenter();
        label.setText("Directory:");

        Text text = new Text(group, SWT.NONE);
        GridDataBuilder.create(text).hFill().hGrab().vCenter().hSpan(2);
        text.setEnabled(false);
        text.setText(mDownloadCache.getCacheRoot().getAbsolutePath());

        label = new Label(group, SWT.NONE);
        GridDataBuilder.create(label).hRight().vCenter();
        label.setText("Current Size:");

        mTextCacheSize = new Text(group, SWT.NONE);
        GridDataBuilder.create(mTextCacheSize).hFill().hGrab().vCenter().hSpan(2);
        mTextCacheSize.setEnabled(false);
        updateDownloadCacheSize();

        mCheckUseCache = new Button(group, SWT.CHECK);
        GridDataBuilder.create(mCheckUseCache).vCenter().hSpan(1);
        mCheckUseCache.setText("Use download cache");
        mCheckUseCache.setToolTipText("When checked, small manifest files are cached locally.\n" +
                                      "Large binary files are never cached locally.");
        mCheckUseCache.addSelectionListener(mApplyOnSelected);

        label = new Label(group, SWT.NONE);
        GridDataBuilder.create(label).hFill().hGrab().hSpan(1);

        Button button = new Button(group, SWT.PUSH);
        GridDataBuilder.create(button).vCenter().hSpan(1);
        button.setText("Clear Cache");
        button.setToolTipText("Deletes all cached files.");
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                mDownloadCache.clearCache();
                updateDownloadCacheSize();
            }
        });

        // ----
        group = new Group(shell, SWT.NONE);
        group.setText("Others");
        GridDataBuilder.create(group).fill().grab().hSpan(2);
        GridLayoutBuilder.create(group).columns(2);

        mCheckForceHttp = new Button(group, SWT.CHECK);
        GridDataBuilder.create(mCheckForceHttp).hFill().hGrab().vCenter().hSpan(2);
        mCheckForceHttp.setText("Force https://... sources to be fetched using http://...");
        mCheckForceHttp.setToolTipText(
            "If you are not able to connect to the official Android repository using HTTPS,\n" +
            "enable this setting to force accessing it via HTTP.");
        mCheckForceHttp.addSelectionListener(mApplyOnSelected);

        mCheckAskAdbRestart = new Button(group, SWT.CHECK);
        GridDataBuilder.create(mCheckAskAdbRestart).hFill().hGrab().vCenter().hSpan(2);
        mCheckAskAdbRestart.setText("Ask before restarting ADB");
        mCheckAskAdbRestart.setToolTipText(
                "When checked, the user will be asked for permission to restart ADB\n" +
                "after updating an addon-on package or a tool package.");
        mCheckAskAdbRestart.addSelectionListener(mApplyOnSelected);

        mCheckEnablePreviews = new Button(group, SWT.CHECK);
        GridDataBuilder.create(mCheckEnablePreviews).hFill().hGrab().vCenter().hSpan(2);
        mCheckEnablePreviews.setText("Enable Preview Tools");
        mCheckEnablePreviews.setToolTipText(
            "When checked, the package list will also display preview versions of the tools.\n" +
            "These are optional future release candidates that the Android tools team\n" +
            "publishes from time to time for early feedback.");
        mCheckEnablePreviews.addSelectionListener(mApplyOnSelected);

        Label filler = new Label(shell, SWT.NONE);
        GridDataBuilder.create(filler).hFill().hGrab();

        createCloseButton();
    }

    @Override
    protected void postCreate() {
        super.postCreate();
        // This tells the controller to load the settings into the page UI.
        mSettingsController.setSettingsPage(this);
    }

    @Override
    protected void close() {
        // Dissociate this page from the controller
        mSettingsController.setSettingsPage(null);
        super.close();
    }


    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    /** Loads settings from the given {@link Properties} container and update the page UI. */
    @Override
    public void loadSettings(Properties inSettings) {
        mTextProxyServer.setText(inSettings.getProperty(KEY_HTTP_PROXY_HOST, ""));  //$NON-NLS-1$
        mTextProxyPort.setText(  inSettings.getProperty(KEY_HTTP_PROXY_PORT, ""));  //$NON-NLS-1$
        mCheckForceHttp.setSelection(
                Boolean.parseBoolean(inSettings.getProperty(KEY_FORCE_HTTP)));
        mCheckAskAdbRestart.setSelection(
                Boolean.parseBoolean(inSettings.getProperty(KEY_ASK_ADB_RESTART)));
        mCheckUseCache.setSelection(
                Boolean.parseBoolean(inSettings.getProperty(KEY_USE_DOWNLOAD_CACHE)));
        mCheckEnablePreviews.setSelection(
                Boolean.parseBoolean(inSettings.getProperty(KEY_ENABLE_PREVIEWS)));

    }

    /** Called by the application to retrieve settings from the UI and store them in
     * the given {@link Properties} container. */
    @Override
    public void retrieveSettings(Properties outSettings) {
        outSettings.setProperty(KEY_HTTP_PROXY_HOST, mTextProxyServer.getText());
        outSettings.setProperty(KEY_HTTP_PROXY_PORT, mTextProxyPort.getText());
        outSettings.setProperty(KEY_FORCE_HTTP,
                Boolean.toString(mCheckForceHttp.getSelection()));
        outSettings.setProperty(KEY_ASK_ADB_RESTART,
                Boolean.toString(mCheckAskAdbRestart.getSelection()));
        outSettings.setProperty(KEY_USE_DOWNLOAD_CACHE,
                Boolean.toString(mCheckUseCache.getSelection()));
        outSettings.setProperty(KEY_ENABLE_PREVIEWS,
                Boolean.toString(mCheckEnablePreviews.getSelection()));

    }

    /**
     * Called by the application to give a callback that the page should invoke when
     * settings must be applied. The page does not apply the settings itself, instead
     * it notifies the application.
     */
    @Override
    public void setOnSettingsChanged(SettingsChangedCallback settingsChangedCallback) {
        mSettingsChangedCallback = settingsChangedCallback;
    }

    /**
     * Callback invoked when user touches one of the settings.
     * There is no "Apply" button, settings are applied immediately as they are changed.
     * Notify the application that settings have changed.
     */
    private void applyNewSettings() {
        if (mSettingsChangedCallback != null) {
            mSettingsChangedCallback.onSettingsChanged(this);
        }
    }

    private void updateDownloadCacheSize() {
        long size = mDownloadCache.getCurrentSize();
        String str = FormatUtils.byteSizeToString(size);
        mTextCacheSize.setText(str);
    }


    // End of hiding from SWT Designer
    //$hide<<$
}
