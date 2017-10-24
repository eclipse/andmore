package com.android.sdklib;

import java.util.Properties;

public class LegacyAndroidVersion {

	private AndroidVersion androidVersion;
	  
	public LegacyAndroidVersion(Properties properties)
	    throws AndroidVersion.AndroidVersionException
	{
	    Exception error = null;
	    
	    String apiLevel = properties.getProperty("AndroidVersion.ApiLevel", null);
	    if (apiLevel != null) {
		    try
		    {
			    String codename = sanitizeCodename(properties.getProperty("AndroidVersion.CodeName", null));
			    androidVersion = new AndroidVersion(Integer.parseInt(apiLevel), codename);
			    return;
		    }
		    catch (NumberFormatException e)
		    {
		        error = e;
		    }
	    }
	    throw new AndroidVersion.AndroidVersionException("AndroidVersion.ApiLevel not found!", error);
    }
	  
	public AndroidVersion getAndroidVersion()
	{
	    return androidVersion;
	}

    private static String sanitizeCodename(String codename)
    {
        if (codename != null)
        {
            codename = codename.trim();
            if ((codename.isEmpty()) || ("REL".equals(codename))) {
                codename = null;
          }
        }
        return codename;
    }
}
