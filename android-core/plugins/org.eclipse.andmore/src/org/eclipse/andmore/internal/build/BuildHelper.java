/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.eclipse.andmore.internal.build;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.andmore.AndmoreAndroidConstants;
import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.AndroidPrintStream;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.preferences.AdtPrefs.BuildVerbosity;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.launching.DefaultProjectClasspathEntry;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.dx.command.dexer.DxContext;
import com.android.dx.command.dexer.Main;
import com.android.dx.command.dexer.Main.Arguments;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.IAndroidTarget.OptionalLibrary;
import com.android.sdklib.build.ApkBuilder;
import com.android.sdklib.build.ApkBuilder.JarStatus;
import com.android.sdklib.build.ApkBuilder.SigningInfo;
import com.android.sdklib.build.ApkCreationException;
import com.android.sdklib.build.DuplicateFileException;
import com.android.sdklib.build.RenderScriptProcessor;
import com.android.sdklib.build.SealedApkException;
import com.android.sdklib.internal.build.DebugKeyProvider;
import com.android.sdklib.internal.build.DebugKeyProvider.KeytoolException;
import com.android.utils.GrabProcessOutput;
import com.android.utils.GrabProcessOutput.IProcessOutput;
import com.android.utils.GrabProcessOutput.Wait;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * Helper with methods for the last 3 steps of the generation of an APK.
 *
 * {@link #packageResources(IFile, IProject[], String, int, String, String)} packages the
 * application resources using aapt into a zip file that is ready to be integrated into the apk.
 *
 * {@link #executeDx(IJavaProject, String, String, IJavaProject[])} will convert the Java byte
 * code into the Dalvik bytecode.
 *
 * {@link #finalPackage(String, String, String, boolean, IJavaProject, IProject[], IJavaProject[], String, boolean)}
 * will make the apk from all the previous components.
 *
 * This class only executes the 3 above actions. It does not handle the errors, and simply sends
 * them back as custom exceptions.
 *
 * Warnings are handled by the {@link ResourceMarker} interface.
 *
 * Console output (verbose and non verbose) is handled through the {@link AndroidPrintStream} passed
 * to the constructor.
 *
 */
public class BuildHelper {

    private class DebugEventListener implements IDebugEventSetListener {
        IProcess process;
        ILaunchConfiguration config;
 
        public DebugEventListener(ILaunchConfiguration config)
        {
            this.config = config;
        }
        
        public IProcess getProcess()
        {
            return process;
        }

        @Override
        public void handleDebugEvents(DebugEvent[] events)
        {
            if (events!=null) {
                for (DebugEvent debugEvent: events) {
                    if ((debugEvent.getSource() instanceof IProcess) && 
                        (debugEvent.getKind() == DebugEvent.TERMINATE) &&
                         handleDebugEvent(debugEvent)) 
                        break;
                }
            }
        }

        boolean handleDebugEvent(DebugEvent event)
        {
            process = (IProcess) event.getSource();
            if (config != process.getLaunch().getLaunchConfiguration()) 
                return false;
            DebugPlugin.getDefault().removeDebugEventListener(this);
            synchronized(this)
            {
                this.notifyAll();
            }
            return true;
         }
    }
    
    private static final String CONSOLE_PREFIX_DX = "Dx";   //$NON-NLS-1$
    private final static String TEMP_PREFIX = "android_";   //$NON-NLS-1$

    private static final String COMMAND_CRUNCH = "crunch";  //$NON-NLS-1$
    private static final String COMMAND_PACKAGE = "package"; //$NON-NLS-1$

    private static final String MULTIDEX_ENABLED_PROPERTY = "multidex.enabled";
    private static final String MULTIDEX_MAIN_DEX_LIST_PROPERTY = "multidex.main-dex-list";
        
    @NonNull
    private final ProjectState mProjectState;
    @NonNull
    private final IProject mProject;
    @NonNull
    private final BuildToolInfo mBuildToolInfo;
    @NonNull
    private final AndroidPrintStream mOutStream;
    @NonNull
    private final AndroidPrintStream mErrStream;
    private final boolean mForceJumbo;
    private final boolean mDisableDexMerger;
    private final boolean mVerbose;
    private final boolean mDebugMode;

    private final Set<String> mCompiledCodePaths = new HashSet<String>();

    public static final boolean BENCHMARK_FLAG = false;
    public static long sStartOverallTime = 0;
    public static long sStartJavaCTime = 0;

    private final static int MILLION = 1000000;
    private String mProguardFile;
   
    /**
     * An object able to put a marker on a resource.
     */
    public interface ResourceMarker {
        void setWarning(IResource resource, String message);
    }

    /**
     * Creates a new post-compiler helper
     * @param project
     * @param outStream
     * @param errStream
     * @param debugMode whether this is a debug build
     * @param verbose
     * @throws CoreException
     */
    public BuildHelper(@NonNull ProjectState projectState,
            @NonNull BuildToolInfo buildToolInfo,
            @NonNull AndroidPrintStream outStream,
            @NonNull AndroidPrintStream errStream,
            boolean forceJumbo, boolean disableDexMerger, boolean debugMode,
            boolean verbose, ResourceMarker resMarker) throws CoreException {
        mProjectState = projectState;
        mProject = projectState.getProject();
        mBuildToolInfo = buildToolInfo;
        mOutStream = outStream;
        mErrStream = errStream;
        mDebugMode = debugMode;
        mVerbose = verbose;
        mForceJumbo = forceJumbo;
        mDisableDexMerger = disableDexMerger;

        gatherPaths(resMarker);
    }

    public void updateCrunchCache() throws AaptExecException, AaptResultException {
        // Benchmarking start
        long startCrunchTime = 0;
        if (BENCHMARK_FLAG) {
            String msg = "BENCHMARK ADT: Starting Initial Packaging (.ap_)"; //$NON-NLS-1$
            AndmoreAndroidPlugin.printBuildToConsole(BuildVerbosity.ALWAYS, mProject, msg);
            startCrunchTime = System.nanoTime();
        }

        // Get the resources folder to crunch from
        IFolder resFolder = mProject.getFolder(AndmoreAndroidConstants.WS_RESOURCES);
        List<String> resPaths = new ArrayList<String>();
        resPaths.add(resFolder.getLocation().toOSString());

        // Get the output folder where the cache is stored.
        IFolder binFolder = BaseProjectHelper.getAndroidOutputFolder(mProject);
        IFolder cacheFolder = binFolder.getFolder(AndmoreAndroidConstants.WS_BIN_RELATIVE_CRUNCHCACHE);
        String cachePath = cacheFolder.getLocation().toOSString();

        /* For crunching, we don't need the osManifestPath, osAssetsPath, or the configFilter
         * parameters for executeAapt
         */
        executeAapt(COMMAND_CRUNCH, "", resPaths, "", cachePath, "", 0);

        // Benchmarking end
        if (BENCHMARK_FLAG) {
            String msg = "BENCHMARK ADT: Ending Initial Package (.ap_). \nTime Elapsed: " //$NON-NLS-1$
                            + ((System.nanoTime() - startCrunchTime)/MILLION) + "ms";     //$NON-NLS-1$
            AndmoreAndroidPlugin.printBuildToConsole(BuildVerbosity.ALWAYS, mProject, msg);
        }
    }

    /**
     * Packages the resources of the projet into a .ap_ file.
     * @param manifestFile the manifest of the project.
     * @param libProjects the list of library projects that this project depends on.
     * @param resFilter an optional resource filter to be used with the -c option of aapt. If null
     * no filters are used.
     * @param versionCode an optional versionCode to be inserted in the manifest during packaging.
     * If the value is <=0, no values are inserted.
     * @param outputFolder where to write the resource ap_ file.
     * @param outputFilename the name of the resource ap_ file.
     * @throws AaptExecException
     * @throws AaptResultException
     */
    public void packageResources(IFile manifestFile, List<IProject> libProjects, String resFilter,
            int versionCode, String outputFolder, String outputFilename)
            throws AaptExecException, AaptResultException {

        // Benchmarking start
        long startPackageTime = 0;
        if (BENCHMARK_FLAG) {
            String msg = "BENCHMARK ADT: Starting Initial Packaging (.ap_)";    //$NON-NLS-1$
            AndmoreAndroidPlugin.printBuildToConsole(BuildVerbosity.ALWAYS, mProject, msg);
            startPackageTime = System.nanoTime();
        }

        // need to figure out some path before we can execute aapt;
        IFolder binFolder = BaseProjectHelper.getAndroidOutputFolder(mProject);

        // get the cache folder
        IFolder cacheFolder = binFolder.getFolder(AndmoreAndroidConstants.WS_BIN_RELATIVE_CRUNCHCACHE);

        // get the BC folder
        IFolder bcFolder = binFolder.getFolder(AndmoreAndroidConstants.WS_BIN_RELATIVE_BC);

        // get the resource folder
        IFolder resFolder = mProject.getFolder(AndmoreAndroidConstants.WS_RESOURCES);

        // and the assets folder
        IFolder assetsFolder = mProject.getFolder(AndmoreAndroidConstants.WS_ASSETS);

        // we need to make sure this one exists.
        if (assetsFolder.exists() == false) {
            assetsFolder = null;
        }

        // list of res folder (main project + maybe libraries)
        ArrayList<String> osResPaths = new ArrayList<String>();

        IPath resLocation = resFolder.getLocation();
        IPath manifestLocation = manifestFile.getLocation();

        if (resLocation != null && manifestLocation != null) {

            // png cache folder first.
            addFolderToList(osResPaths, cacheFolder);
            addFolderToList(osResPaths, bcFolder);

            // regular res folder next.
            osResPaths.add(resLocation.toOSString());

            // then libraries
            if (libProjects != null) {
                for (IProject lib : libProjects) {
                    // png cache folder first
                    IFolder libBinFolder = BaseProjectHelper.getAndroidOutputFolder(lib);

                    IFolder libCacheFolder = libBinFolder.getFolder(AndmoreAndroidConstants.WS_BIN_RELATIVE_CRUNCHCACHE);
                    addFolderToList(osResPaths, libCacheFolder);

                    IFolder libBcFolder = libBinFolder.getFolder(AndmoreAndroidConstants.WS_BIN_RELATIVE_BC);
                    addFolderToList(osResPaths, libBcFolder);

                    // regular res folder next.
                    IFolder libResFolder = lib.getFolder(AndmoreAndroidConstants.WS_RESOURCES);
                    addFolderToList(osResPaths, libResFolder);
                }
            }

            String osManifestPath = manifestLocation.toOSString();

            String osAssetsPath = null;
            if (assetsFolder != null) {
                osAssetsPath = assetsFolder.getLocation().toOSString();
            }

            // build the default resource package
            executeAapt(COMMAND_PACKAGE, osManifestPath, osResPaths, osAssetsPath,
                    outputFolder + File.separator + outputFilename, resFilter,
                    versionCode);
        }

        // Benchmarking end
        if (BENCHMARK_FLAG) {
            String msg = "BENCHMARK ADT: Ending Initial Package (.ap_). \nTime Elapsed: " //$NON-NLS-1$
                            + ((System.nanoTime() - startPackageTime)/MILLION) + "ms";    //$NON-NLS-1$
            AndmoreAndroidPlugin.printBuildToConsole(BuildVerbosity.ALWAYS, mProject, msg);
        }
    }

    /**
     * Adds os path of a folder to a list only if the folder actually exists.
     * @param pathList
     * @param folder
     */
    private void addFolderToList(List<String> pathList, IFolder folder) {
        // use a File instead of the IFolder API to ignore workspace refresh issue.
        File testFile = new File(folder.getLocation().toOSString());
        if (testFile.isDirectory()) {
            pathList.add(testFile.getAbsolutePath());
        }
    }

    /**
     * Makes a final package signed with the debug key.
     *
     * Packages the dex files, the temporary resource file into the final package file.
     *
     * Whether the package is a debug package is controlled with the <var>debugMode</var> parameter
     * in {@link #PostCompilerHelper(IProject, PrintStream, PrintStream, boolean, boolean)}
     *
     * @param intermediateApk The path to the temporary resource file.
     * @param dex The path to the dex file.
     * @param output The path to the final package file to create.
     * @param libProjects an optional list of library projects (can be null)
     * @return true if success, false otherwise.
     * @throws ApkCreationException
     * @throws AndroidLocationException
     * @throws KeytoolException
     * @throws NativeLibInJarException
     * @throws CoreException
     * @throws DuplicateFileException
     */
    public void finalDebugPackage(String intermediateApk, List<File> dexFiles, String output,
            List<IProject> libProjects, ResourceMarker resMarker)
            throws ApkCreationException, KeytoolException, AndroidLocationException,
            NativeLibInJarException, DuplicateFileException, CoreException {

        AndmoreAndroidPlugin adt = AndmoreAndroidPlugin.getDefault();
        if (adt == null) {
            return;
        }

        // get the debug keystore to use.
        IPreferenceStore store = adt.getPreferenceStore();
        String keystoreOsPath = store.getString(AdtPrefs.PREFS_CUSTOM_DEBUG_KEYSTORE);
        if (keystoreOsPath == null || new File(keystoreOsPath).isFile() == false) {
            keystoreOsPath = DebugKeyProvider.getDefaultKeyStoreOsPath();
            AndmoreAndroidPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, mProject,
                    Messages.ApkBuilder_Using_Default_Key);
        } else {
            AndmoreAndroidPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, mProject,
                    String.format(Messages.ApkBuilder_Using_s_To_Sign, keystoreOsPath));
        }

        // from the keystore, get the signing info
        SigningInfo info = ApkBuilder.getDebugKey(keystoreOsPath, mVerbose ? mOutStream : null);

        finalPackage(intermediateApk, dexFiles, output, libProjects,
                info != null ? info.key : null, info != null ? info.certificate : null, resMarker);
    }

    /**
     * List a dex files in the bin directory.
     * 
     * @param javaProject
     * @return
     */
    public List<File> listDexFiles(IJavaProject javaProject)
    {    	
    	IFolder binFolder = BaseProjectHelper.getAndroidOutputFolder(javaProject.getProject());
    	File binFile = binFolder.getLocation().toFile();
    	
    	File[] dexFiles = binFile.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(SdkConstants.DOT_DEX);
			}
		});
    	
    	// return a mutable list.
    	return new ArrayList<File>(Arrays.asList(dexFiles));
    }
    
    /**
     * Makes the final package.
     *
     * Packages the dex files, the temporary resource file into the final package file.
     *
     * Whether the package is a debug package is controlled with the <var>debugMode</var> parameter
     * in {@link #PostCompilerHelper(IProject, PrintStream, PrintStream, boolean, boolean)}
     *
     * @param intermediateApk The path to the temporary resource file.
     * @param dex The path to the dex file.
     * @param output The path to the final package file to create.
     * @param debugSign whether the apk must be signed with the debug key.
     * @param libProjects an optional list of library projects (can be null)
     * @param abiFilter an optional filter. If not null, then only the matching ABI is included in
     * the final archive
     * @return true if success, false otherwise.
     * @throws NativeLibInJarException
     * @throws ApkCreationException
     * @throws CoreException
     * @throws DuplicateFileException
     */
    public void finalPackage(String intermediateApk, List<File> dexFiles, String output,
            List<IProject> libProjects,
            PrivateKey key, X509Certificate certificate, ResourceMarker resMarker)
            throws NativeLibInJarException, ApkCreationException, DuplicateFileException,
            CoreException {

        try {
        	if(dexFiles.size() < 1) {
        		throw new ApkCreationException("No DEX file found.");
        	}
        	
        	File mainDexFile = dexFiles.get(0);
        	dexFiles.remove(0);
        	
            ApkBuilder apkBuilder = new ApkBuilder(output, intermediateApk, mainDexFile.getAbsolutePath(),
                    key, certificate,
                    mVerbose ? mOutStream: null);
            apkBuilder.setDebugMode(mDebugMode);
            
            for (File dexFile : dexFiles) {
            	apkBuilder.addFile(dexFile, dexFile.getName());
            }
            
            // either use the full compiled code paths or just the proguard file
            // if present
            Collection<String> pathsCollection = mCompiledCodePaths;
            if (mProguardFile != null) {
                pathsCollection = Collections.singletonList(mProguardFile);
                mProguardFile = null;
            }

            // Now we write the standard resources from all the output paths.
            for (String path : pathsCollection) {
                File file = new File(path);
                if (file.isFile()) {
                    JarStatus jarStatus = apkBuilder.addResourcesFromJar(file);

                    // check if we found native libraries in the external library. This
                    // constitutes an error or warning depending on if they are in lib/
                    if (jarStatus.getNativeLibs().size() > 0) {
                        String libName = file.getName();

                        String msg = String.format(
                                "Native libraries detected in '%1$s'. See console for more information.",
                                libName);

                        ArrayList<String> consoleMsgs = new ArrayList<String>();

                        consoleMsgs.add(String.format(
                                "The library '%1$s' contains native libraries that will not run on the device.",
                                libName));

                        if (jarStatus.hasNativeLibsConflicts()) {
                            consoleMsgs.add("Additionally some of those libraries will interfer with the installation of the application because of their location in lib/");
                            consoleMsgs.add("lib/ is reserved for NDK libraries.");
                        }

                        consoleMsgs.add("The following libraries were found:");

                        for (String lib : jarStatus.getNativeLibs()) {
                            consoleMsgs.add(" - " + lib);
                        }

                        String[] consoleStrings = consoleMsgs.toArray(new String[consoleMsgs.size()]);

                        // if there's a conflict or if the prefs force error on any native code in jar
                        // files, throw an exception
                        if (jarStatus.hasNativeLibsConflicts() ||
                                AdtPrefs.getPrefs().getBuildForceErrorOnNativeLibInJar()) {
                            throw new NativeLibInJarException(jarStatus, msg, libName, consoleStrings);
                        } else {
                            // otherwise, put a warning, and output to the console also.
                            if (resMarker != null) {
                                resMarker.setWarning(mProject, msg);
                            }

                            for (String string : consoleStrings) {
                                mOutStream.println(string);
                            }
                        }
                    }
                } else if (file.isDirectory()) {
                    // this is technically not a source folder (class folder instead) but since we
                    // only care about Java resources (ie non class/java files) this will do the
                    // same
                    apkBuilder.addSourceFolder(file);
                }
            }

            // now write the native libraries.
            // First look if the lib folder is there.
            IResource libFolder = mProject.findMember(SdkConstants.FD_NATIVE_LIBS);
            if (libFolder != null && libFolder.exists() &&
                    libFolder.getType() == IResource.FOLDER) {
                // get a File for the folder.
                apkBuilder.addNativeLibraries(libFolder.getLocation().toFile());
            }

            // next the native libraries for the renderscript support mode.
            if (mProjectState.getRenderScriptSupportMode()) {
                IFolder androidOutputFolder = BaseProjectHelper.getAndroidOutputFolder(mProject);
                IResource rsLibFolder = androidOutputFolder.getFolder(
                        AndmoreAndroidConstants.WS_BIN_RELATIVE_RS_LIBS);
                File rsLibFolderFile = rsLibFolder.getLocation().toFile();
                if (rsLibFolderFile.isDirectory()) {
                    apkBuilder.addNativeLibraries(rsLibFolderFile);
                }

                File rsLibs = RenderScriptProcessor.getSupportNativeLibFolder(
                        mBuildToolInfo.getLocation().getAbsolutePath());
                if (rsLibs.isDirectory()) {
                    apkBuilder.addNativeLibraries(rsLibs);
                }
            }

            // write the native libraries for the library projects.
            if (libProjects != null) {
                for (IProject lib : libProjects) {
                    libFolder = lib.findMember(SdkConstants.FD_NATIVE_LIBS);
                    if (libFolder != null && libFolder.exists() &&
                            libFolder.getType() == IResource.FOLDER) {
                        apkBuilder.addNativeLibraries(libFolder.getLocation().toFile());
                    }
                }
            }

            // seal the APK.
            apkBuilder.sealApk();
        } catch (SealedApkException e) {
            // this won't happen as we control when the apk is sealed.
        }
    }

    public void setProguardOutput(String proguardFile) {
        mProguardFile = proguardFile;
    }

    public Collection<String> getCompiledCodePaths() {
        return mCompiledCodePaths;
    }

    public void runProguard(List<File> proguardConfigs, File inputJar, Collection<String> jarFiles,
                            File obfuscatedJar, File logOutput)
            throws ProguardResultException, ProguardExecException, IOException {
        IAndroidTarget target = Sdk.getCurrent().getTarget(mProject);

        // prepare the command line for proguard
        List<String> command = new ArrayList<String>();
        command.add(AndmoreAndroidPlugin.getOsAbsoluteProguard());

        for (File configFile : proguardConfigs) {
            command.add("-include"); //$NON-NLS-1$
            command.add(quotePath(configFile.getAbsolutePath()));
        }

        command.add("-injars"); //$NON-NLS-1$
        StringBuilder sb = new StringBuilder(quotePath(inputJar.getAbsolutePath()));
        for (String jarFile : jarFiles) {
            sb.append(File.pathSeparatorChar);
            sb.append(quotePath(jarFile));
        }
        command.add(quoteWinArg(sb.toString()));

        command.add("-outjars"); //$NON-NLS-1$
        command.add(quotePath(obfuscatedJar.getAbsolutePath()));

        command.add("-libraryjars"); //$NON-NLS-1$
        sb = new StringBuilder(quotePath(target.getPath(IAndroidTarget.ANDROID_JAR)));
        List<OptionalLibrary> libraries = target.getOptionalLibraries();
        if (libraries != null) {
            for (OptionalLibrary lib : libraries) {
                sb.append(File.pathSeparatorChar);
                sb.append(quotePath(lib.getJar().getAbsolutePath()));
            }
        }
        command.add(quoteWinArg(sb.toString()));

        if (logOutput != null) {
            if (logOutput.isDirectory() == false) {
                logOutput.mkdirs();
            }

            command.add("-dump");                                              //$NON-NLS-1$
            command.add(new File(logOutput, "dump.txt").getAbsolutePath());    //$NON-NLS-1$

            command.add("-printseeds");                                        //$NON-NLS-1$
            command.add(new File(logOutput, "seeds.txt").getAbsolutePath());   //$NON-NLS-1$

            command.add("-printusage");                                        //$NON-NLS-1$
            command.add(new File(logOutput, "usage.txt").getAbsolutePath());   //$NON-NLS-1$

            command.add("-printmapping");                                      //$NON-NLS-1$
            command.add(new File(logOutput, "mapping.txt").getAbsolutePath()); //$NON-NLS-1$
        }

        String commandArray[] = null;

        if (SdkConstants.currentPlatform() == SdkConstants.PLATFORM_WINDOWS) {
            commandArray = createWindowsProguardConfig(command);
        }

        if (commandArray == null) {
            // For Mac & Linux, use a regular command string array.
            commandArray = command.toArray(new String[command.size()]);
        }

        // Define PROGUARD_HOME to point to $SDK/tools/proguard if it's not yet defined.
        // The Mac/Linux proguard.sh can infer it correctly but not the proguard.bat one.
        String[] envp = null;
        Map<String, String> envMap = new TreeMap<String, String>(System.getenv());
        if (!envMap.containsKey("PROGUARD_HOME")) {                                    //$NON-NLS-1$
            envMap.put("PROGUARD_HOME", Sdk.getCurrent().getSdkOsLocation() +          //$NON-NLS-1$
                                        SdkConstants.FD_TOOLS + File.separator +
                                        SdkConstants.FD_PROGUARD);
            envp = new String[envMap.size()];
            int i = 0;
            for (Map.Entry<String, String> entry : envMap.entrySet()) {
                envp[i++] = String.format("%1$s=%2$s",                                 //$NON-NLS-1$
                                          entry.getKey(),
                                          entry.getValue());
            }
        }

        if (AdtPrefs.getPrefs().getBuildVerbosity() == BuildVerbosity.VERBOSE) {
            sb = new StringBuilder();
            for (String c : commandArray) {
                sb.append(c).append(' ');
            }
            AndmoreAndroidPlugin.printToConsole(mProject, sb.toString());
        }

        // launch
        int execError = 1;
        try {
            // launch the command line process
            Process process = Runtime.getRuntime().exec(commandArray, envp);

            // list to store each line of stderr
            ArrayList<String> results = new ArrayList<String>();

            // get the output and return code from the process
            execError = grabProcessOutput(mProject, process, results);

            if (mVerbose) {
                for (String resultString : results) {
                    mOutStream.println(resultString);
                }
            }

            if (execError != 0) {
                throw new ProguardResultException(execError,
                        results.toArray(new String[results.size()]));
            }

        } catch (IOException e) {
            String msg = String.format(Messages.Proguard_Exec_Error, commandArray[0]);
            throw new ProguardExecException(msg, e);
        } catch (InterruptedException e) {
            String msg = String.format(Messages.Proguard_Exec_Error, commandArray[0]);
            throw new ProguardExecException(msg, e);
        }
    }

    /**
     * For tools R8 up to R11, the proguard.bat launcher on Windows only accepts
     * arguments %1..%9. Since we generally have about 15 arguments, we were working
     * around this by generating a temporary config file for proguard and then using
     * that.
     * Starting with tools R12, the proguard.bat launcher has been fixed to take
     * all arguments using %* so we no longer need this hack.
     *
     * @param command
     * @return
     * @throws IOException
     */
    private String[] createWindowsProguardConfig(List<String> command) throws IOException {

        // Arg 0 is the proguard.bat path and arg 1 is the user config file
        String launcher = AndmoreAndroidPlugin.readFile(new File(command.get(0)));
        if (launcher.contains("%*")) {                                      //$NON-NLS-1$
            // This is the launcher from Tools R12. Don't work around it.
            return null;
        }

        // On Windows, proguard.bat can only pass %1...%9 to the java -jar proguard.jar
        // call, but we have at least 15 arguments here so some get dropped silently
        // and quoting is a big issue. So instead we'll work around that by writing
        // all the arguments to a temporary config file.

        String[] commandArray = new String[3];

        commandArray[0] = command.get(0);
        commandArray[1] = command.get(1);

        // Write all the other arguments to a config file
        File argsFile = File.createTempFile(TEMP_PREFIX, ".pro");           //$NON-NLS-1$
        // TODO FIXME this may leave a lot of temp files around on a long session.
        // Should have a better way to clean up e.g. before each build.
        argsFile.deleteOnExit();

        FileWriter fw = new FileWriter(argsFile);

        for (int i = 2; i < command.size(); i++) {
            String s = command.get(i);
            fw.write(s);
            fw.write(s.startsWith("-") ? ' ' : '\n');                       //$NON-NLS-1$
        }

        fw.close();

        commandArray[2] = "@" + argsFile.getAbsolutePath();                 //$NON-NLS-1$
        return commandArray;
    }

    /**
     * Quotes a single path for proguard to deal with spaces.
     *
     * @param path The path to quote.
     * @return The original path if it doesn't contain a space.
     *   Or the original path surrounded by single quotes if it contains spaces.
     */
    private String quotePath(String path) {
        if (path.indexOf(' ') != -1) {
            path = '\'' + path + '\'';
        }
        return path;
    }

    /**
     * Quotes a compound proguard argument to deal with spaces.
     * <p/>
     * Proguard takes multi-path arguments such as "path1;path2" for some options.
     * When the {@link #quotePath} methods adds quotes for such a path if it contains spaces,
     * the proguard shell wrapper will absorb the quotes, so we need to quote around the
     * quotes.
     *
     * @param path The path to quote.
     * @return The original path if it doesn't contain a single quote.
     *   Or on Windows the original path surrounded by double quotes if it contains a quote.
     */
    private String quoteWinArg(String path) {
        if (path.indexOf('\'') != -1 &&
                SdkConstants.currentPlatform() == SdkConstants.PLATFORM_WINDOWS) {
            path = '"' + path + '"';
        }
        return path;
    }


    /**
     * Execute the Dx tool for dalvik code conversion.
     * @param javaProject The java project
     * @param inputPaths the input paths for DX
     * @param osOutFilePath the path of the dex file to create.
     *
     * @throws CoreException
     * @throws DexException
     */
    public void executeDx(IJavaProject javaProject, Collection<String> inputPaths,
            String osOutFilePath)
            throws CoreException, DexException {

        // get the dex wrapper
        Sdk sdk = Sdk.getCurrent();
        //DexWrapper wrapper = sdk.getDexWrapper(mBuildToolInfo);

        //if (wrapper == null) {
        //    throw new CoreException(new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
        //            Messages.ApkBuilder_UnableBuild_Dex_Not_loaded));
        //}

        try {
            // set a temporary prefix on the print streams.
            mOutStream.setPrefix(CONSOLE_PREFIX_DX);
            mErrStream.setPrefix(CONSOLE_PREFIX_DX);

            IFolder binFolder = BaseProjectHelper.getAndroidOutputFolder(javaProject.getProject());
            File binFile = binFolder.getLocation().toFile();
            File dexedLibs = new File(binFile, "dexedLibs");
            if (dexedLibs.exists() == false) {
                dexedLibs.mkdir();
            }

            boolean multiDexEnabled = false;
            String mainDexListFileLocation = null;
            
            if (mProjectState.getProperty(MULTIDEX_ENABLED_PROPERTY) != null) {
            	multiDexEnabled = Boolean.parseBoolean(mProjectState.getProperty(MULTIDEX_ENABLED_PROPERTY));
            	
            	if(multiDexEnabled && mProjectState.getProperty(MULTIDEX_MAIN_DEX_LIST_PROPERTY) != null) {
            		// inform the user
            		mOutStream.println("Using --multi-dex");
            		
            		IFile mainDexListFile = mProject.getFile(
	            			mProjectState.getProperty(MULTIDEX_MAIN_DEX_LIST_PROPERTY));
	            	if(mainDexListFile != null) {
	            		mainDexListFileLocation = mainDexListFile.getRawLocation().toOSString();
	            		
	            		// For multidex output to a folder
	                	osOutFilePath = new File(osOutFilePath).getParent();
	            	}
            	}
            }
            
            // replace the libs by their dexed versions (dexing them if needed.)
            List<String> finalInputPaths = new ArrayList<String>(inputPaths.size());
            if (mDisableDexMerger || multiDexEnabled || inputPaths.size() == 1) {
                // only one input, no need to put a pre-dexed version, even if this path is
                // just a jar file (case for proguard'ed builds)
                finalInputPaths.addAll(inputPaths);
            } else {

                for (String input : inputPaths) {
                    File inputFile = new File(input);
                    if (inputFile.isDirectory()) {
                        finalInputPaths.add(input);
                    } else if (inputFile.isFile()) {
                        String fileName = getDexFileName(inputFile);

                        File dexedLib = new File(dexedLibs, fileName);
                        String dexedLibPath = dexedLib.getAbsolutePath();

                        if (dexedLib.isFile() == false ||
                                dexedLib.lastModified() < inputFile.lastModified()) {

                            if (mVerbose) {
                                mOutStream.println(
                                        String.format("Pre-Dexing %1$s -> %2$s", input, fileName));
                            }

                            if (dexedLib.isFile()) {
                                dexedLib.delete();
                            }

                            int res = runDx(dexedLibPath, Collections.singleton(input),
                                    mForceJumbo, false, null, false, mVerbose, true, mOutStream, mErrStream);

                            if (res != 0) {
                                // output error message and mark the project.
                                String message = String.format(Messages.Dalvik_Error_d, res);
                                throw new DexException(message);
                            }
                        } else {
                            if (mVerbose) {
                                mOutStream.println(
                                        String.format("Using Pre-Dexed %1$s <- %2$s",
                                                fileName, input));
                            }
                        }

                        finalInputPaths.add(dexedLibPath);
                    }
                }
            }

            if (mVerbose) {
                for (String input : finalInputPaths) {
                    mOutStream.println("Input: " + input);
                }
            }
            
            int res = runDx(osOutFilePath,
                    finalInputPaths,
                    mForceJumbo,
                    multiDexEnabled,
                    mainDexListFileLocation,
                    true, // minimalMainDex is true, only used for multidex
                    mVerbose,
                    false,
                    mOutStream, mErrStream);

            mOutStream.setPrefix(null);
            mErrStream.setPrefix(null);

            if (res != 0) {
                // output error message and marker the project.
                String message = String.format(Messages.Dalvik_Error_d, res);
                throw new DexException(message);
            }
        } catch (DexException e) {
            throw e;
        } catch (Throwable t) {
            String message = t.getMessage();
            if (message == null) {
                message = t.getClass().getCanonicalName();
            }
            message = String.format(Messages.Dalvik_Error_s, message);

            throw new DexException(message, t);
        }
    }

    /**
     * TODO - Determine if DexWrapper should be retained for build tools version 21 - 25
     * Runs the dex command.
     * @param osOutFilePath the OS path to the outputfile (classes.dex
     * @param osFilenames list of input source files (.class and .jar files)
     * @param forceJumbo force jumbo mode.
     * @param enableMultiDex
     * @param mainDexListFile
     * @param minimalMainDex
     * @param verbose verbose mode
     * @param predex predex
     * @param outStream the stdout console
     * @param errStream the stderr console
     * @return the integer return code of com.android.dx.command.dexer.Main.run()
     * @throws CoreException
     */
    private int runDx(
            String osOutFilePath, 
            Collection<String> osFilenames,
            boolean forceJumbo, 
            boolean enableMultiDex,
            String mainDexListFile, 
            boolean minimalMainDex, 
            boolean verbose,
            boolean predex,
            AndroidPrintStream outStream, 
            AndroidPrintStream errStream) throws CoreException
    {
        List<String> command = new ArrayList<String>();
        command.add("--dex");
        if (verbose)
            command.add("--verbose"); // --num-threads=4 
        command.add("--output");
        command.add(osOutFilePath);
        for (String fileName: osFilenames)
            command.add(fileName);
        List<Path> dxJarPath = Collections.singletonList(new Path(mBuildToolInfo.getPath(BuildToolInfo.PathId.DX_JAR)));
        return runJavaMain("dex", "com.android.dx.command.Main", "-Djava.awt.headless=true -Xmx1024M -Dfile.encoding=UTF-8", dxJarPath , command);
        /*
        // prepare the command line for dx
        // TODO - Create Dex configuration
        List<String> command = new ArrayList<String>();
        // Use JAVA_HOME environment variable, if available
        command.add("java");
        command.add("-Djava.awt.headless=true");
        command.add("-Xmx1024M");
        command.add("-Dfile.encoding=UTF-8");
        String dexLocation = mBuildToolInfo.getPath(BuildToolInfo.PathId.DX_JAR);
        command.add("-cp");
        command.add(dexLocation);
        command.add("com.android.dx.command.Main");
        command.add("--dex");
        if (verbose)
            command.add("--verbose"); // --num-threads=4 
        command.add("--output");
        command.add(osOutFilePath);
        for (String fileName: osFilenames)
            command.add(fileName);
        String commandArray[] = command.toArray(new String[command.size()]);
        // launch
        int returnCode = 1;
        try {
            // launch the command line process
            Process process = Runtime.getRuntime().exec(commandArray);

            // list to store each line of stderr
            ArrayList<String> results = new ArrayList<String>();

            // get the output and return code from the process
            returnCode = grabProcessOutput(mProject, process, results);

            if (mVerbose) {
                for (String resultString : results) {
                    mOutStream.println(resultString);
                }
            }

        } catch (IOException e) {
            String message = String.format(Messages.DexWrapper_Unable_To_Execute_Dex_s, e.getMessage());
            AndmoreAndroidPlugin.log(e, message);
            AndmoreAndroidPlugin.printErrorToConsole(Messages.DexWrapper_Dex_Loader, message);
            throw new CoreException(new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID, message, e));
        } catch (InterruptedException e) {
            String message = String.format(Messages.DexWrapper_Unable_To_Execute_Dex_s, e.getMessage());
            AndmoreAndroidPlugin.log(e, message);
            AndmoreAndroidPlugin.printErrorToConsole(Messages.DexWrapper_Dex_Loader, message);
            throw new CoreException(new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID, message, e));
        }
        return returnCode;
        */
        /*
        Main main = new Main(new DxContext());
        Arguments arguments = new Arguments();
        arguments.fileNames = osFilenames.toArray(new String[osFilenames.size()]);
        arguments.outName = osOutFilePath;
        arguments.jarOutput = osOutFilePath.endsWith(SdkConstants.DOT_JAR);
        arguments.forceJumbo = forceJumbo;
        arguments.verbose = verbose;
        arguments.multiDex = enableMultiDex;
        // Pre dex libraries may not contain any class files
        if (predex)
            arguments.emptyOk = true;
        if (enableMultiDex)
        {
            arguments.mainDexListFile = mainDexListFile;
            arguments.minimalMainDex = minimalMainDex;
        }
        try
        {
            int returnCode = main.run(arguments);
            if (returnCode != 0)
            {
                String nl = System.getProperty("line.separator");
                StringBuilder builder = new StringBuilder();
                if (predex)
                    builder.append("Pre dex:").append(nl);
                builder.append("outName").append('=').append('"').append(osOutFilePath).append('"').append(nl);
                builder.append("forceJumbo").append('=').append(forceJumbo).append(nl);
                builder.append("multiDex").append('=').append(enableMultiDex).append(nl);
                if(enableMultiDex)
                {
                    builder.append("mainDexListFile").append('=').append('"').append(mainDexListFile).append('"').append(nl);
                    builder.append("minimalMainDex").append('=').append(minimalMainDex).append(nl);
                }
                builder.append("verbose").append('=').append(verbose).append(nl);
                for (String filename: osFilenames)
                    builder.append('"').append(filename).append('"').append(nl);
                mOutStream.println(builder.toString());
            }
            return returnCode;
        }
        catch (IOException e)
        {
            String message = String.format(Messages.DexWrapper_Unable_To_Execute_Dex_s, e.getMessage());
            AndmoreAndroidPlugin.log(e, message);
            AndmoreAndroidPlugin.printErrorToConsole(Messages.DexWrapper_Dex_Loader, message);
            throw new CoreException(new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID, message, e));
        }
        */
    }

    /**
     * Launch a Java program
     * @param cfg the name of the launch configuration to use
     * @param main Main class
     * @throws CoreException
     */
    @SuppressWarnings("restriction")
    private int runJavaMain(String configName, String main, String vmArgs, List<Path> classpathList, Collection<String> arguments) throws CoreException {
        IJavaProject javaProject = JavaCore.create(mProject);
        javaProject.open(null);
        StringBuilder builder = new StringBuilder();
        for (String arg: arguments)
            builder.append(arg).append(' ');
        DebugPlugin plugin = DebugPlugin.getDefault();
        ILaunchManager launchManager = plugin.getLaunchManager();
        ILaunchConfigurationType launchConfigType = launchManager.getLaunchConfigurationType(
          IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
        ILaunchConfigurationWorkingCopy wc = launchConfigType.newInstance(null, configName);
        wc.setAttribute(
          IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, javaProject.getElementName());
        wc.setAttribute(
          IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, main);
        wc.setAttribute(
                IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
        wc.setAttribute(
                IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, builder.toString());
        List<String> classpath = new ArrayList<String>();
        for (Path path: classpathList)
            classpath.add(JavaRuntime.newArchiveRuntimeClasspathEntry(path).getMemento());
        classpath.add(JavaRuntime.newRuntimeContainerClasspathEntry(new Path( JavaRuntime.JRE_CONTAINER ), IRuntimeClasspathEntry.STANDARD_CLASSES, javaProject).getMemento());
        classpath.add(new DefaultProjectClasspathEntry(javaProject).getMemento());
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, classpath);
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, false);
        final ILaunchConfiguration config = wc.doSave();   
        config.launch(ILaunchManager.RUN_MODE, null);
        final DebugEventListener listener = new DebugEventListener(config);
        final Display display = AndmoreAndroidPlugin.getDisplay();
        display.syncExec(new Runnable() {
            
            @Override
            public void run() {
                DebugPlugin.getDefault().addDebugEventListener(listener);
                synchronized(listener) {
                    try {
                        listener.wait();
                    }   catch (InterruptedException e) {
                    }
                }
            }
            
        });
        int returnCode = -1;
        try
        {
            IProcess process = listener.getProcess();
            if (process != null) // Check for null in case of interrupt terminating wait
                returnCode = process.getExitValue();
        }
        catch (DebugException e)
        {
            String message = String.format(Messages.DexWrapper_Unable_To_Execute_Dex_s, e.getMessage());
            AndmoreAndroidPlugin.log(e, message);
            AndmoreAndroidPlugin.printErrorToConsole(Messages.DexWrapper_Dex_Loader, message);
            throw new CoreException(new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID, message, e));
        }
        return returnCode;
    }


    private String getDexFileName(File inputFile) {
        // get the filename
        String name = inputFile.getName();
        // remove the extension
        int pos = name.lastIndexOf('.');
        if (pos != -1) {
            name = name.substring(0, pos);
        }

        // add a hash of the original file path
        HashFunction hashFunction = Hashing.md5();
        HashCode hashCode = hashFunction.hashString(inputFile.getAbsolutePath(), Charset.defaultCharset());

        return name + "-" + hashCode.toString() + ".jar";
    }

    /**
     * Executes aapt. If any error happen, files or the project will be marked.
     * @param command The command for aapt to execute. Currently supported: package and crunch
     * @param osManifestPath The path to the manifest file
     * @param osResPath The path to the res folder
     * @param osAssetsPath The path to the assets folder. This can be null.
     * @param osOutFilePath The path to the temporary resource file to create,
     *   or in the case of crunching the path to the cache to create/update.
     * @param configFilter The configuration filter for the resources to include
     * (used with -c option, for example "port,en,fr" to include portrait, English and French
     * resources.)
     * @param versionCode optional version code to insert in the manifest during packaging. If <=0
     * then no value is inserted
     * @throws AaptExecException
     * @throws AaptResultException
     */
    private void executeAapt(String aaptCommand, String osManifestPath,
            List<String> osResPaths, String osAssetsPath, String osOutFilePath,
            String configFilter, int versionCode) throws AaptExecException, AaptResultException {
        IAndroidTarget target = Sdk.getCurrent().getTarget(mProject);

        String aapt = mBuildToolInfo.getPath(BuildToolInfo.PathId.AAPT);

        // Create the command line.
        ArrayList<String> commandArray = new ArrayList<String>();
        commandArray.add(aapt);
        commandArray.add(aaptCommand);
        if (AdtPrefs.getPrefs().getBuildVerbosity() == BuildVerbosity.VERBOSE) {
            commandArray.add("-v"); //$NON-NLS-1$
        }

        // Common to all commands
        for (String path : osResPaths) {
            commandArray.add("-S"); //$NON-NLS-1$
            commandArray.add(path);
        }

        if (aaptCommand.equals(COMMAND_PACKAGE)) {
            commandArray.add("-f");          //$NON-NLS-1$
            commandArray.add("--no-crunch"); //$NON-NLS-1$

            // if more than one res, this means there's a library (or more) and we need
            // to activate the auto-add-overlay
            if (osResPaths.size() > 1) {
                commandArray.add("--auto-add-overlay"); //$NON-NLS-1$
            }

            if (mDebugMode) {
                commandArray.add("--debug-mode"); //$NON-NLS-1$
            }

            if (versionCode > 0) {
                commandArray.add("--version-code"); //$NON-NLS-1$
                commandArray.add(Integer.toString(versionCode));
            }

            if (configFilter != null) {
                commandArray.add("-c"); //$NON-NLS-1$
                commandArray.add(configFilter);
            }

            // never compress apks.
            commandArray.add("-0");
            commandArray.add("apk");

            commandArray.add("-M"); //$NON-NLS-1$
            commandArray.add(osManifestPath);

            if (osAssetsPath != null) {
                commandArray.add("-A"); //$NON-NLS-1$
                commandArray.add(osAssetsPath);
            }

            commandArray.add("-I"); //$NON-NLS-1$
            commandArray.add(target.getPath(IAndroidTarget.ANDROID_JAR));

            commandArray.add("-F"); //$NON-NLS-1$
            commandArray.add(osOutFilePath);
        } else if (aaptCommand.equals(COMMAND_CRUNCH)) {
            commandArray.add("-C"); //$NON-NLS-1$
            commandArray.add(osOutFilePath);
        }

        String command[] = commandArray.toArray(
                new String[commandArray.size()]);

        if (AdtPrefs.getPrefs().getBuildVerbosity() == BuildVerbosity.VERBOSE) {
            StringBuilder sb = new StringBuilder();
            for (String c : command) {
                sb.append(c);
                sb.append(' ');
            }
            AndmoreAndroidPlugin.printToConsole(mProject, sb.toString());
        }

        // Benchmarking start
        long startAaptTime = 0;
        if (BENCHMARK_FLAG) {
            String msg = "BENCHMARK ADT: Starting " + aaptCommand  //$NON-NLS-1$
                         + " call to Aapt";                        //$NON-NLS-1$
            AndmoreAndroidPlugin.printBuildToConsole(BuildVerbosity.ALWAYS, mProject, msg);
            startAaptTime = System.nanoTime();
        }

        // launch
        try {
            // launch the command line process
            Process process = Runtime.getRuntime().exec(command);

            // list to store each line of stderr
            ArrayList<String> stdErr = new ArrayList<String>();

            // get the output and return code from the process
            int returnCode = grabProcessOutput(mProject, process, stdErr);

            if (mVerbose) {
                for (String stdErrString : stdErr) {
                    mOutStream.println(stdErrString);
                }
            }
            if (returnCode != 0) {
                throw new AaptResultException(returnCode,
                        stdErr.toArray(new String[stdErr.size()]));
            }
        } catch (IOException e) {
            String msg = String.format(Messages.AAPT_Exec_Error_s, command[0]);
            throw new AaptExecException(msg, e);
        } catch (InterruptedException e) {
            String msg = String.format(Messages.AAPT_Exec_Error_s, command[0]);
            throw new AaptExecException(msg, e);
        }

        // Benchmarking end
        if (BENCHMARK_FLAG) {
            String msg = "BENCHMARK ADT: Ending " + aaptCommand                  //$NON-NLS-1$
                         + " call to Aapt.\nBENCHMARK ADT: Time Elapsed: "       //$NON-NLS-1$
                         + ((System.nanoTime() - startAaptTime)/MILLION) + "ms"; //$NON-NLS-1$
            AndmoreAndroidPlugin.printBuildToConsole(BuildVerbosity.ALWAYS, mProject, msg);
        }
    }

    /**
     * Computes all the project output and dependencies that must go into building the apk.
     *
     * @param resMarker
     * @throws CoreException
     */
    private void gatherPaths(ResourceMarker resMarker)
            throws CoreException {
        IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();

        // get a java project for the project.
        IJavaProject javaProject = JavaCore.create(mProject);


        // get the output of the main project
        IPath path = javaProject.getOutputLocation();
        IResource outputResource = wsRoot.findMember(path);
        if (outputResource != null && outputResource.getType() == IResource.FOLDER) {
            mCompiledCodePaths.add(outputResource.getLocation().toOSString());
        }

        // we could use IJavaProject.getResolvedClasspath directly, but we actually
        // want to see the containers themselves.
        IClasspathEntry[] classpaths = javaProject.readRawClasspath();
        if (classpaths != null) {
            for (IClasspathEntry e : classpaths) {
                // ignore non exported entries, unless they're in the DEPEDENCIES container,
                // in which case we always want it (there may be some older projects that
                // have it as non exported).
                if (e.isExported() ||
                        (e.getEntryKind() == IClasspathEntry.CPE_CONTAINER &&
                         e.getPath().toString().equals(AndmoreAndroidConstants.CONTAINER_DEPENDENCIES))) {
                    handleCPE(e, javaProject, wsRoot, resMarker);
                }
            }
        }
    }

    private void handleCPE(IClasspathEntry entry, IJavaProject javaProject,
            IWorkspaceRoot wsRoot, ResourceMarker resMarker) {

        // if this is a classpath variable reference, we resolve it.
        if (entry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
            entry = JavaCore.getResolvedClasspathEntry(entry);
        }

        if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
            IProject refProject = wsRoot.getProject(entry.getPath().lastSegment());
            try {
                // ignore if it's an Android project, or if it's not a Java Project
                if (refProject.hasNature(JavaCore.NATURE_ID) &&
                        refProject.hasNature(AndmoreAndroidConstants.NATURE_DEFAULT) == false) {
                    IJavaProject refJavaProject = JavaCore.create(refProject);

                    // get the output folder
                    IPath path = refJavaProject.getOutputLocation();
                    IResource outputResource = wsRoot.findMember(path);
                    if (outputResource != null && outputResource.getType() == IResource.FOLDER) {
                        mCompiledCodePaths.add(outputResource.getLocation().toOSString());
                    }
                }
            } catch (CoreException exception) {
                // can't query the project nature? ignore
            }

        } else if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
            handleClasspathLibrary(entry, wsRoot, resMarker);
        } else if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
            // get the container
            try {
                IClasspathContainer container = JavaCore.getClasspathContainer(
                        entry.getPath(), javaProject);
                // ignore the system and default_system types as they represent
                // libraries that are part of the runtime.
                if (container != null && container.getKind() == IClasspathContainer.K_APPLICATION) {
                    IClasspathEntry[] entries = container.getClasspathEntries();
                    for (IClasspathEntry cpe : entries) {
                        handleCPE(cpe, javaProject, wsRoot, resMarker);
                    }
                }
            } catch (JavaModelException jme) {
                // can't resolve the container? ignore it.
                AndmoreAndroidPlugin.log(jme, "Failed to resolve ClasspathContainer: %s", entry.getPath());
            }
        }
    }

    private void handleClasspathLibrary(IClasspathEntry e, IWorkspaceRoot wsRoot,
            ResourceMarker resMarker) {
        // get the IPath
        IPath path = e.getPath();

        IResource resource = wsRoot.findMember(path);

        if (resource != null && resource.getType() == IResource.PROJECT) {
            // if it's a project we should just ignore it because it's going to be added
            // later when we add all the referenced projects.

        } else if (SdkConstants.EXT_JAR.equalsIgnoreCase(path.getFileExtension())) {
            // case of a jar file (which could be relative to the workspace or a full path)
            if (resource != null && resource.exists() &&
                    resource.getType() == IResource.FILE) {
                mCompiledCodePaths.add(resource.getLocation().toOSString());
            } else {
                // if the jar path doesn't match a workspace resource,
                // then we get an OSString and check if this links to a valid file.
                String osFullPath = path.toOSString();

                File f = new File(osFullPath);
                if (f.isFile()) {
                    mCompiledCodePaths.add(osFullPath);
                } else {
                    String message = String.format( Messages.Couldnt_Locate_s_Error,
                            path);
                    // always output to the console
                    mOutStream.println(message);

                    // put a marker
                    if (resMarker != null) {
                        resMarker.setWarning(mProject, message);
                    }
                }
            }
        } else {
            // this can be the case for a class folder.
            if (resource != null && resource.exists() &&
                    resource.getType() == IResource.FOLDER) {
                mCompiledCodePaths.add(resource.getLocation().toOSString());
            } else {
                // if the path doesn't match a workspace resource,
                // then we get an OSString and check if this links to a valid folder.
                String osFullPath = path.toOSString();

                File f = new File(osFullPath);
                if (f.isDirectory()) {
                    mCompiledCodePaths.add(osFullPath);
                }
            }
        }
    }

    /**
     * Checks a {@link IFile} to make sure it should be packaged as standard resources.
     * @param file the IFile representing the file.
     * @return true if the file should be packaged as standard java resources.
     */
    public static boolean checkFileForPackaging(IFile file) {
        String name = file.getName();

        String ext = file.getFileExtension();
        return ApkBuilder.checkFileForPackaging(name, ext);
    }

    /**
     * Checks whether an {@link IFolder} and its content is valid for packaging into the .apk as
     * standard Java resource.
     * @param folder the {@link IFolder} to check.
     */
    public static boolean checkFolderForPackaging(IFolder folder) {
        String name = folder.getName();
        return ApkBuilder.checkFolderForPackaging(name);
    }

    /**
     * Returns a list of {@link IJavaProject} matching the provided {@link IProject} objects.
     * @param projects the IProject objects.
     * @return a new list object containing the IJavaProject object for the given IProject objects.
     * @throws CoreException
     */
    public static List<IJavaProject> getJavaProjects(List<IProject> projects) throws CoreException {
        ArrayList<IJavaProject> list = new ArrayList<IJavaProject>();

        for (IProject p : projects) {
            if (p.isOpen() && p.hasNature(JavaCore.NATURE_ID)) {

                list.add(JavaCore.create(p));
            }
        }

        return list;
    }

    /**
     * Get the stderr output of a process and return when the process is done.
     * @param process The process to get the output from
     * @param stderr The array to store the stderr output
     * @return the process return code.
     * @throws InterruptedException
     */
    public final static int grabProcessOutput(
            final IProject project,
            final Process process,
            final ArrayList<String> stderr)
            throws InterruptedException {

        return GrabProcessOutput.grabProcessOutput(
                process,
                Wait.WAIT_FOR_READERS, // we really want to make sure we get all the output!
                new IProcessOutput() {

                    @SuppressWarnings("unused")
                    @Override
                    public void out(@Nullable String line) {
                        if (line != null) {
                            // If benchmarking always print the lines that
                            // correspond to benchmarking info returned by ADT
                            if (BENCHMARK_FLAG && line.startsWith("BENCHMARK:")) {    //$NON-NLS-1$
                                AndmoreAndroidPlugin.printBuildToConsole(BuildVerbosity.ALWAYS,
                                        project, line);
                            } else {
                                AndmoreAndroidPlugin.printBuildToConsole(BuildVerbosity.VERBOSE,
                                        project, line);
                            }
                        }
                    }

                    @Override
                    public void err(@Nullable String line) {
                        if (line != null) {
                            stderr.add(line);
                            if (BuildVerbosity.VERBOSE == AdtPrefs.getPrefs().getBuildVerbosity()) {
                                AndmoreAndroidPlugin.printErrorToConsole(project, line);
                            }
                        }
                    }
                });
    }
}
