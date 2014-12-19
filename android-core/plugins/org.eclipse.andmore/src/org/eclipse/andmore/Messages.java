
package org.eclipse.andmore;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.andmore.messages"; //$NON-NLS-1$

    public static String AdtPlugin_Android_SDK_Content_Loader;

    public static String AdtPlugin_Android_SDK_Resource_Parser;

    public static String AdtPlugin_Failed_To_Parse_s;

    public static String AdtPlugin_Failed_To_Start_s;

    public static String Console_Data_Project_Tag;

    public static String Console_Date_Tag;

    public static String Could_Not_Find;

    public static String Could_Not_Find_Folder;

    public static String Could_Not_Find_Folder_In_SDK;

    public static String VersionCheck_Plugin_Too_Old;

    public static String VersionCheck_Plugin_Version_Failed;

    public static String VersionCheck_Build_Tool_Missing;

    public static String VersionCheck_Tools_Too_Old;

    public static String VersionCheck_Unable_To_Parse_Version_s;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
