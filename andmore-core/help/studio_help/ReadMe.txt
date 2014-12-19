Setting up to build:
1. Obtain the DITA Open Toolkit from http://dita-ot.sourceforge.net. Install it into the tools folder. Note that there is already a ditaot plugin in this folder; that plugin needs to reside in the plugins folder within your ditaot installation.
2. Obtain DITAReports from http://dita-ot.sourceforge.net/doc/ot-userguide131/ditaotug131-18042007-tools.zip. Install it so that you wind up with a ditareports folder inside tools (parallel to the ditaot folder).
3. Obtain Linklint from http://www.linklint.org and install it into the tools folder, so that you wind up with a linklint folder inside tools. You should now have three folders inside the tools folder: "ditaot", "ditareports", and "linklint", and within the plugins folder inside ditaot you should see cshelp (part of ditaot) and com.mot.mdb.deved.xhtml. If the cshelp plugin isn't there, you'll need to obtain it from http://dita-ot.sourceforge.net and install it separately.

Instructions for building the online help on Mac OS X:

1. In Finder, double-click docs_dita/studio_help/tools/ditaot/startcmd_mac.command. The terminal window will open and the environment will be set up.
2. In the terminal window, do:
	cd ../../etc/buildfiles/android_studio-help
	ant all -f build.xml
	
There are Windows batch files (in tools/ditaot) that can be used in place of step 1 on a Windows machine. 
