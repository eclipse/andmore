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

package org.eclipse.andmore.internal.build;

import java.io.File;
import java.io.PrintStream;
import java.lang.StringBuilder;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.android.SdkConstants;

/**
 * Wrapper to access dx.jar through reflection.
 * <p/>Since there is no proper api to call the method in the dex library, this wrapper is going
 * to access it through reflection.
 */
public final class DexWrapper {

    public final static String DEX_MAIN = "com.android.dx.command.dexer.Main"; //$NON-NLS-1$
    public final static String DEX_ARGS = "com.android.dx.command.dexer.Main$Arguments"; //$NON-NLS-1$

    public final static String DEX_MAIN_FIELD_OUTPUT_FUTURES = "dexOutputFutures"; //$NON-NLS-1$
    public final static String DEX_MAIN_FIELD_OUTPUT_ARRAYS = "dexOutputArrays"; //$NON-NLS-1$
    public final static String DEX_MAIN_FIELD_CLASSES_IN_MAIN_DEX = "classesInMainDex"; //$NON-NLS-1$
    public final static String DEX_MAIN_FIELD_OUTPUT_RESOURCES = "outputResources"; //$NON-NLS-1$
    
    public final static String DEX_ARGUMENTS_FIELD_OUT_NAME = "outName"; //$NON-NLS-1$
    public final static String DEX_ARGUMENTS_FIELD_JAR_OUTPUT = "jarOutput"; //$NON-NLS-1$
    public final static String DEX_ARGUMENTS_FIELD_FILES_NAMES = "fileNames"; //$NON-NLS-1$
    public final static String DEX_ARGUMENTS_FIELD_VERBOSE = "verbose"; //$NON-NLS-1$
    public final static String DEX_ARGUMENTS_FIELD_FORCE_JUMBO = "forceJumbo"; //$NON-NLS-1$
    public final static String DEX_ARGUMENTS_FIELD_MULTI_DEX = "multiDex"; //$NON-NLS-1$
    public final static String DEX_ARGUMENTS_FIELD_MAIN_DEX_LIST_FILE = "mainDexListFile"; //$NON-NLS-1$
    public final static String DEX_ARGUMENTS_FIELD_MINIMAL_MAIN_DEX = "minimalMainDex"; //$NON-NLS-1$
    
    private final static String MAIN_RUN = "run"; //$NON-NLS-1$

    private Method mRunMethod;
    
    private Field mDexOutputFutures;
    private Field mDexOutputArrays;
    private Field mClassesInMainDex;
    private Field mOutputResources;

    private Constructor<?> mArgConstructor;
    private Field mArgOutName;
    private Field mArgVerbose;
    private Field mArgJarOutput;
    private Field mArgFileNames;
    private Field mArgForceJumbo;
    private Field mArgMultiDex;
    private Field mArgMainDexListFile;
    private Field mArgMinimalMainDex;
    
    /**
     * Loads the dex library from a file path.
     *
     * The loaded library can be used via
     * {@link DexWrapper#run(String, String[], boolean, PrintStream, PrintStream)}.
     *
     * @param osFilepath the location of the dx.jar file.
     * @return an IStatus indicating the result of the load.
     */
    public synchronized IStatus loadDex(String osFilepath) {
        try {
            File f = new File(osFilepath);
            if (f.isFile() == false) {
                return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID, String.format(
                        Messages.DexWrapper_s_does_not_exists, osFilepath));
            }
            URL url = f.toURI().toURL();

            @SuppressWarnings("resource")
			URLClassLoader loader = new URLClassLoader(new URL[] { url },
                    DexWrapper.class.getClassLoader());

            // get the classes.
            Class<?> mainClass = loader.loadClass(DEX_MAIN);
            Class<?> argClass = loader.loadClass(DEX_ARGS);

            try {
                // now get the fields/methods we need
                mRunMethod = mainClass.getMethod(MAIN_RUN, argClass);
                
                try {
                	// this field is not required when it is not yet available in Main.
                	mDexOutputFutures = mainClass.getDeclaredField(DEX_MAIN_FIELD_OUTPUT_FUTURES);
                } catch (Exception e) {}
                
                mDexOutputArrays = mainClass.getDeclaredField(DEX_MAIN_FIELD_OUTPUT_ARRAYS);
                mClassesInMainDex = mainClass.getDeclaredField(DEX_MAIN_FIELD_CLASSES_IN_MAIN_DEX);
                mOutputResources = mainClass.getDeclaredField(DEX_MAIN_FIELD_OUTPUT_RESOURCES);
                
                mArgConstructor = argClass.getConstructor();
                mArgOutName = argClass.getField(DEX_ARGUMENTS_FIELD_OUT_NAME); //$NON-NLS-1$
                mArgJarOutput = argClass.getField(DEX_ARGUMENTS_FIELD_JAR_OUTPUT); //$NON-NLS-1$
                mArgFileNames = argClass.getField(DEX_ARGUMENTS_FIELD_FILES_NAMES); //$NON-NLS-1$
                mArgVerbose = argClass.getField(DEX_ARGUMENTS_FIELD_VERBOSE); //$NON-NLS-1$
                mArgForceJumbo = argClass.getField(DEX_ARGUMENTS_FIELD_FORCE_JUMBO); //$NON-NLS-1$
                mArgMultiDex = argClass.getField(DEX_ARGUMENTS_FIELD_MULTI_DEX); //$NON-NLS-1$
                mArgMainDexListFile = argClass.getField(DEX_ARGUMENTS_FIELD_MAIN_DEX_LIST_FILE);
                mArgMinimalMainDex = argClass.getField(DEX_ARGUMENTS_FIELD_MINIMAL_MAIN_DEX);

            } catch (SecurityException e) {
                return createErrorStatus(Messages.DexWrapper_SecuryEx_Unable_To_Find_API, e);
            } catch (NoSuchMethodException e) {
                return createErrorStatus(Messages.DexWrapper_SecuryEx_Unable_To_Find_Method, e);
            } catch (NoSuchFieldException e) {
                return createErrorStatus(Messages.DexWrapper_SecuryEx_Unable_To_Find_Field, e);
            }

            return Status.OK_STATUS;
        } catch (MalformedURLException e) {
            // really this should not happen.
            return createErrorStatus(
                    String.format(Messages.DexWrapper_Failed_to_load_s, osFilepath), e);
        } catch (ClassNotFoundException e) {
            return createErrorStatus(
                    String.format(Messages.DexWrapper_Failed_to_load_s, osFilepath), e);
        }
    }

    /**
     * Removes any reference to the dex library.
     * <p/>
     * {@link #loadDex(String)} must be called on the wrapper
     * before {@link #run(String, String[], boolean, PrintStream, PrintStream)} can
     * be used again.
     */
    public synchronized void unload() {
        mRunMethod = null;
        
        mDexOutputFutures = null;
        mDexOutputArrays = null;
        mClassesInMainDex = null;
        mOutputResources = null;
        
        mArgConstructor = null;
        mArgOutName = null;
        mArgJarOutput = null;
        mArgFileNames = null;
        mArgVerbose = null;
        mArgMultiDex = null;
        mArgMainDexListFile = null;
        mArgMinimalMainDex = null;
        System.gc();
    }

    /**
     * Since we are not running Main.java from the command-line we have to reset some values when building.
     * 
     * @throws Exception
     */
    private synchronized void resetDexMain() throws Exception {
    	assert mDexOutputArrays != null;
    	assert mClassesInMainDex != null;
    	assert mOutputResources != null;
    	
    	if(mDexOutputFutures != null) { // not required when not available
	    	if(!mDexOutputFutures.isAccessible()) {
	    		mDexOutputFutures.setAccessible(true);
	    	}
	    	
	    	mDexOutputFutures.set(null, new ArrayList<Future<byte[]>>());
    	}
    	
    	if(!mDexOutputArrays.isAccessible()) {
    		mDexOutputArrays.setAccessible(true);
    	}
    	
    	if(!mClassesInMainDex.isAccessible()) {
    		mClassesInMainDex.setAccessible(true);
    	}
    	
    	if(!mOutputResources.isAccessible()) {
    		mOutputResources.setAccessible(true);
    	}
    	
        mDexOutputArrays.set(null, new ArrayList<byte[]>());
        mClassesInMainDex.set(null, null);
        mOutputResources.set(null, null);
     }
    
    /**
     * Runs the dex command.
     * The wrapper must have been initialized via {@link #loadDex(String)} first.
     *
     * @param osOutFilePath the OS path to the outputfile (classes.dex
     * @param osFilenames list of input source files (.class and .jar files)
     * @param forceJumbo force jumbo mode.
     * @param verbose verbose mode.
     * @param outStream the stdout console
     * @param errStream the stderr console
     * @return the integer return code of com.android.dx.command.dexer.Main.run()
     * @throws CoreException
     */
    public synchronized int run(String osOutFilePath, Collection<String> osFilenames,
            boolean forceJumbo, boolean enableMultiDex, String mainDexListFile, boolean minimalMainDex, boolean verbose,
            PrintStream outStream, PrintStream errStream) throws CoreException {

        assert mRunMethod != null;
        assert mArgConstructor != null;
        assert mArgOutName != null;
        assert mArgJarOutput != null;
        assert mArgFileNames != null;
        assert mArgForceJumbo != null;
        assert mArgVerbose != null;
        assert mArgMultiDex != null;
        assert mArgMainDexListFile != null;
        assert mArgMinimalMainDex != null;

        if (mRunMethod == null) {
            throw new CoreException(createErrorStatus(
                    String.format(Messages.DexWrapper_Unable_To_Execute_Dex_s,
                            "wrapper was not properly loaded first"),
                    null /*exception*/));
        }

        try {
            
            // create the Arguments object.
            Object args = mArgConstructor.newInstance();
            mArgMultiDex.set(args, enableMultiDex);
            
            if(enableMultiDex)
            {
            	resetDexMain();
            	
            	mArgMainDexListFile.set(args, mainDexListFile);
            	mArgMinimalMainDex.set(args, minimalMainDex);
            }
            
            mArgFileNames.set(args, osFilenames.toArray(new String[osFilenames.size()]));
            
            mArgOutName.set(args, osOutFilePath);
            mArgJarOutput.set(args, osOutFilePath.endsWith(SdkConstants.DOT_JAR));
            mArgForceJumbo.set(args, forceJumbo);
            mArgVerbose.set(args, verbose);

            // call the run method
            Object res = mRunMethod.invoke(null /* obj: static method */, args);

            if (res instanceof Integer) {
                return ((Integer)res).intValue();
            }

            return -1;
        } catch (Exception e) {
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }

            String msg = t.getMessage();
            if (msg == null) {
                msg = String.format("%s. Check the Eclipse log for stack trace.",
                        t.getClass().getName());
            }
            boolean jarOutput = osOutFilePath.endsWith(SdkConstants.DOT_JAR);
            String nl = System.getProperty("line.separator");
            StringBuilder builder = new StringBuilder(msg).append(nl);
            builder.append("outName").append('=').append('"').append(osOutFilePath).append('"').append(nl);
            builder.append("jarOutput").append('=').append(jarOutput).append(nl);
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
            throw new CoreException(createErrorStatus(
                    String.format(Messages.DexWrapper_Unable_To_Execute_Dex_s, builder.toString()), t));
        }
    }

    private static IStatus createErrorStatus(String message, Throwable e) {
        AndmoreAndroidPlugin.log(e, message);
        AndmoreAndroidPlugin.printErrorToConsole(Messages.DexWrapper_Dex_Loader, message);

        return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID, message, e);
    }
}
