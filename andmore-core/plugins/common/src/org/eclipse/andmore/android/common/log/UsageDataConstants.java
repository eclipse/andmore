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
package org.eclipse.andmore.android.common.log;

/**
 * Constants used for the UDC component to collect anonymous usage data. This
 * data is periodically sent to servers if the user agreed with that. If user
 * does not agree, the data is never used.
 * 
 * Example of the use of KIND and DESCRIPTION when logging: The KIND <emulator>
 * can be used with a set of different WHATs, thas is, <started>, <stoped>,
 * <refresh> and so on. Which means that the kind of occurrence emulator can
 * have different events, such as: the emulator was started.
 */
public interface UsageDataConstants {
	public static final String DESCRIPTION_DEFAULT = "operation_executed";

	public static final String KIND_DOWNLOAD_ADDON = "addondownload";

	public static final String VALUE_EMULATOR = "emulator";

	public static final String VALUE_HANDSET = "handset";

	public static final String KIND_DOWNLOADSDK = "sdkdownload";

	public static final String KIND_APP_MANAGEMENT = "application_management";

	public static final String WHAT_APP_MANAGEMENT_CREATE = "created";

	public static final String WHAT_APP_MANAGEMENT_PACKAGE = "package";

	public static final String KIND_EMULATOR = "emulator";

	public static final String WHAT_EMULATOR_STOP = "stopped";

	public static final String WHAT_EMULATOR_RESET = "reset";

	public static final String WHAT_EMULATOR_START = "started";

	public static final String WHAT_EMULATOR_CREATION_WIZARD = "new_device_wizard";

	public static final String WHAT_EMULATOR_LANGUAGE = "change_language";

	public static final String KIND_BUILDINGBLOCK = "building_blocks";

	public static final String WHAT_BUILDINGBLOCK_PROVIDER = "create_content_provider";

	public static final String WHAT_BUILDINGBLOCK_ACTIVITY = "create_activity";

	public static final String WHAT_BUILDINGBLOCK_RECEIVER = "create_receiver";

	public static final String WHAT_BUILDINGBLOCK_SERVICE = "create_service";

	public static final String WHAT_BUILDINGBLOCK_WIDGET_PROVIDER = "create_widget_provider";

	public static final String KIND_LOCALIZATION = "localization";

	public static final String WHAT_LOCALIZATION_AUTOMATICTRANSLATION = "automatic_translator";

	public static final String KEY_TARGET = "target=";

	public static final String KEY_TRANSLATION_PROVIDER = "provider=";

	public static final String KEY_TRANSLATION_FROM_LANG = "from_lang=";

	public static final String KEY_TRANSLATION_TO_LANG = "to_lang=";

	public static final String VALUE_GOOGLE = "google";

	public static final String KEY_PRJ_TARGET = "prj_target=";

	public static final String KEY_TARGETLIST = "target_list=";

	public static final String KEY_DEVICE_TYPE = "device=";

	public static final String KEY_USE_VDL = "use_vdl=";

	public static final String KEY_ISOPHONE = "isophone=";

	public static final String VALUE_YES = "y";

	public static final String VALUE_NO = "n";

	public static final String SEPARATOR = "|";

	public static final String WHAT_DATABASE_MANAGMT_CLASSES = "created";

	public static final String KIND_DATABASE_MANAGMT_CLASSES = "database";

	public static final String WHAT_CODESNIPPET = "inserted";

	public static final String KIND_CODESNIPPET = "codesnippet";

	public static final String WHAT_OPENHELPER = "created";

	public static final String KIND_OPENHELPER = "codesnippet";

	public static final String WHAT_TABLEWIZARD = "created";

	public static final String KIND_TABLEWIZARD = "table_wizard";

	public static final String WHAT_DBACTION = "created_device";

	public static final String KIND_DBACTION = "dbcreate_device";

	public static final String WHAT_INSTALLADDON = "install_addon";

	public static final String WHAT_INSTALLDOCS = "install_docs";

	public static final String KIND_INSTALL = "install";

	public static final String KIND_INSTALLADDON = KIND_INSTALL;

	public static final String WHAT_INSTALLSDK = "install_sdk";

	public static final String KIND_INSTALLSDK = KIND_INSTALL;

	public static final String KIND_INSTALLDOCS = KIND_INSTALL;

	public static final String WHAT_INSTALLPLATFORM = "install_platform";

	public static final String KIND_INSTALLPLATFORM = KIND_INSTALL;

	public static final String WHAT_INSTALLPLATFORM_TOOLS = "install_platform_tools";

	public static final String KIND_INSTALLPLATFORM_TOOLS = KIND_INSTALL;

	public static final String WHAT_INSTALLSAMPLE = "install_sample";

	public static final String KIND_INSTALLSAMPLE = KIND_INSTALL;

	public static final String WHAT_MONKEY_EXEC = "executed";

	public static final String KIND_MONKEY_EXEC = "monkey";

	public static final String WHAT_VIEW_BY_LAYOUT_EXEC = "what_view_by_layout_exec";

	public static final String KIND_VIEW_BY_LAYOUT_EXEC = "kind_view_by_layout_exec";

	public static final String WHAT_SAMPLE_ACTIVITY_CREATED = "created";

	public static final String KIND_SAMPLE_ACTIVITY_CREATED = "activity";

	public static final String WHAT_BUILDINGBLOCK_PERMISSION = "permission_used";

	public static final String KIND_BUILDINGBLOCK_PERMISSION = "buildingblock";

	public static final String WHAT_WIDGETPROVIDER_CREATED = "created";

	public static final String KIND_WIDGETPROVIDER = "widget_provider";

	public static final String KIND_REMOTE_DEVICE = "remote_device";

	public static final String WHAT_REMOTE_WIRELESS = "switched_to_wireless";

	public static final String WHAT_REMOTE_USB = "switched_to_usb";

	public static final String WHAT_OBFUSCATE = "obfuscation";

	public static final String KIND_OBFUSCATE = "obfuscate_project";

	public static final String KIND_DESOBFUSCATE = "desobfuscate_project";

	public static final String WHAT_VIDEOS_PLAY = "play";

	public static final String WHAT_VIDEOS_PLAYER_SUPPORT = "player_support";

	public static final String WHAT_VIDEOS_FLASH_SUPPORT = "flash_support";

	public static final String KIND_VIDEOS = "videos";

	public static final String WHAT_VIEW_BY_MENU_EXEC = "what_view_by_menu_exec";

	public static final String KIND_VIEW_BY_MENU_EXEC = "kind_view_by_menu_exec";

}
