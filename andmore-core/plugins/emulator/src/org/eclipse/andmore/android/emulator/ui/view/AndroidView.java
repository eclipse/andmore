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
package org.eclipse.andmore.android.emulator.ui.view;

import static org.eclipse.andmore.android.common.log.StudioLogger.error;

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.core.exception.InstanceStopException;
import org.eclipse.andmore.android.emulator.core.exception.SkinException;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.core.skin.IAndroidSkin;
import org.eclipse.andmore.android.emulator.core.skin.ISkinKeyXmlTags;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.ui.controls.IAndroidComposite;
import org.eclipse.andmore.android.emulator.ui.controls.maindisplay.MainDisplayComposite;
import org.eclipse.andmore.android.emulator.ui.controls.nativewindow.NativeWindowComposite;
import org.eclipse.andmore.android.emulator.ui.controls.skin.SkinComposite;
import org.eclipse.andmore.android.emulator.ui.handlers.IHandlerConstants;
import org.eclipse.andmore.android.nativeos.IDevicePropertiesOSConstants;
import org.eclipse.andmore.android.nativeos.NativeUIUtils;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.vncviewer.config.IPropertiesFileHandler;
import org.eclipse.sequoyah.vnc.vncviewer.config.VNCConfiguration;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.ISWTPainter;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.SWTRemoteDisplay;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.img.SWTRemoteDisplayImg;
import org.eclipse.sequoyah.vnc.vncviewer.network.VNCProtocolData;
import org.eclipse.sequoyah.vnc.vncviewer.registry.VNCProtocolRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.commands.ICommandService;

/**
 * DESCRIPTION: This class represents the Android Emulator view
 * 
 * RESPONSIBILITY: - Show the skin to the end user
 * 
 * COLABORATORS: None.
 * 
 * USAGE: All the public interface is extended from
 * <code>AbstractAndroidView</code>.
 */
public class AndroidView extends AbstractAndroidView {
	/**
	 * The Android Emulator view ID
	 */
	public static final String ANDROID_VIEW_ID = EmulatorPlugin.PLUGIN_ID + ".androidView";

	/**
	 * @see org.eclipse.andmore.android.emulator.ui.view.AbstractAndroidView#getViewId()
	 */
	@Override
	protected String getViewId() {
		return ANDROID_VIEW_ID;
	}

	/**
	 * Creates the skin composite, main display and CLI display related to
	 * emulator.
	 * 
	 * @param tab
	 *            the tab item that will hold the graphical elements that
	 *            represents the emulator
	 * @param instance
	 *            the emulator instance
	 * @param emulatorData
	 *            the object to be defined with the elements created.
	 */
	@Override
	protected void createWidgets(TabItem tab, final IAndroidEmulatorInstance instance, final AndroidViewData tabData)
			throws SkinException {
		tabData.loadSkin(instance);
		IAndroidSkin skin = tabData.getSkin();

		ProtocolHandle handle = instance.getProtocolHandle();
		VNCProtocolData data = VNCProtocolRegistry.getInstance().get(handle);

		final SWTRemoteDisplay mainDisplay;
		Composite parentComposite;
		if (instance.getProperties().getProperty(IDevicePropertiesOSConstants.useVnc, NativeUIUtils.getDefaultUseVnc())
				.toString().equals("true")) {
			if (data != null) {
				if (instance.getProperties().getProperty("Command_Line").contains("-no-skin")) {
					int baseWidth = skin.getSkinBean(instance.getCurrentLayout()).getSkinPropertyValue(
							ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_WIDTH);
					int baseHeight = skin.getSkinBean(instance.getCurrentLayout()).getSkinPropertyValue(
							ISkinKeyXmlTags.SKIN_INTERNAL_VIEW_HEIGHT);

					ScrolledComposite scrolledComposite = new ScrolledComposite(tab.getParent(), SWT.H_SCROLL
							| SWT.V_SCROLL | SWT.BACKGROUND);

					final MainDisplayComposite composite = new MainDisplayComposite(scrolledComposite, SWT.BACKGROUND,
							baseWidth, baseHeight, instance);
					composite.setLayout(new FillLayout());

					mainDisplay = createMainDisplay(composite, skin, instance, (ISWTPainter) data.getVncPainter());
					composite.setSize(baseWidth, baseHeight);

					scrolledComposite.setContent(composite);
					tab.setControl(scrolledComposite);

					scrolledComposite.addDisposeListener(new DisposeListener() {
						@Override
						public void widgetDisposed(DisposeEvent e) {
							composite.dispose();
						}
					});

					tabData.setCliDisplay(null);
					tabData.setComposite(composite);
					tabData.setMainDisplay(mainDisplay);
				} else {
					parentComposite = createSkinComposite(tab.getParent(), skin, instance);
					mainDisplay = createMainDisplay(parentComposite, skin, instance, (ISWTPainter) data.getVncPainter());

					tabData.setComposite((IAndroidComposite) parentComposite);
					tabData.setMainDisplay(mainDisplay);
					tab.setControl(parentComposite);
				}
			} else {
				error("The protocol object set in the device instance is not supported. Stopping the emulator instance...");
				EclipseUtils.showErrorDialog(EmulatorNLS.GEN_Error,
						EmulatorNLS.ERR_AndroidView_ProtocolImplementerNotSupported);

				try {
					instance.stop(true);
				} catch (InstanceStopException e) {
					error("Error while running service for stopping virtual machine");
					EclipseUtils.showErrorDialog(EmulatorNLS.GEN_Error, EmulatorNLS.EXC_General_CannotRunStopService);
				}
			}

		} else {
			parentComposite = createNativeWindowComposite(tab.getParent(), skin, instance);
			tabData.setComposite((IAndroidComposite) parentComposite);
			tab.setControl(parentComposite);
		}
	}

	/**
	 * Creates the skin composite used by this instance
	 * 
	 * @param parent
	 *            The parent composite where to place the widgets
	 * @param skin
	 *            The object containing skin information used to draw the
	 *            widgets
	 */
	private SkinComposite createSkinComposite(Composite parent, IAndroidSkin skin, IAndroidEmulatorInstance instance) {

		SkinComposite skinComposite = new SkinComposite(parent, skin, instance);
		skinComposite.setBackground(new Color(skinComposite.getDisplay(), 255, 255, 255));

		return skinComposite;
	}

	private NativeWindowComposite createNativeWindowComposite(Composite parent, IAndroidSkin skin,
			IAndroidEmulatorInstance instance) {
		NativeWindowComposite nativeWindowComposite = new NativeWindowComposite(parent, skin, instance);
		// nativeWindowComposite.setBackground(new
		// Color(nativeWindowComposite.getDisplay(), 255, 255,
		// 255));

		return nativeWindowComposite;
	}

	/**
	 * Creates the main display object used by this instance.
	 * 
	 * @param parent
	 *            The parent composite where to place the widgets
	 * @param skin
	 *            The object containing skin information used to draw the
	 *            widgets
	 * @param instance
	 *            The emulator instance
	 * @param painter
	 *            the painter used to draw the images
	 */
	private SWTRemoteDisplay createMainDisplay(Composite parent, IAndroidSkin skin, IAndroidEmulatorInstance instance,
			ISWTPainter painter)

	{
		// gets and reads the configuration files of the VNC session
		IPropertiesFileHandler handler = IAndroidSkin.DEFAULT_PROPS_HANDLER;
		String configFile = IAndroidSkin.DEFAULT_VNC_CONFIG_FILE;

		VNCConfiguration config = new VNCConfiguration(configFile, handler);
		final SWTRemoteDisplay mainDisplay;
		mainDisplay = new SWTRemoteDisplayImg(parent, config.getConfigurationProperties(), handler, painter);

		mainDisplay.setBackground(new Color(mainDisplay.getDisplay(), 0, 0, 0));
		mainDisplay.setLayout(new FillLayout());

		return mainDisplay;
	}

	/**
	 * Forces the refreshing of the menu elements.
	 */
	@Override
	protected void refreshMenuElements() {
		IViewSite viewSite = getViewSite();

		// Update the radio button selection in the zoom menu
		ICommandService service = (ICommandService) viewSite.getService(ICommandService.class);
		service.refreshElements(IHandlerConstants.CHANGE_EMULATOR_ORIENTATION_COMMAND, null);
		service.refreshElements(IHandlerConstants.CHANGE_EMULATOR_ZOOM_COMMAND, null);

	}

}