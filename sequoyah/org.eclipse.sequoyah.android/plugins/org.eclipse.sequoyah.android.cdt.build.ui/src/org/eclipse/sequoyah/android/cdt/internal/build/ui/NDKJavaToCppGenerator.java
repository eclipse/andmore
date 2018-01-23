/*******************************************************************************
 * Copyright (c) 2010 Motorola, Inc. All rights reserved.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial contributors:
 * Paulo Renato de Faria (Eldorado)
 * 
 * Contributors:
 * Carlos Alberto Souto Junior (Eldorado) - [317327] Major UI bugfixes and improvements in Android Native support
 *******************************************************************************/

package org.eclipse.sequoyah.android.cdt.internal.build.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.sequoyah.android.cdt.build.core.NDKUtils;
import org.eclipse.swt.widgets.Display;

/**
 * Class that encapsulates the logic to: - call javah command to generate C
 * header file based on one Java class file - create a C source file based on
 * the C header file created - ask user where to place the output (jni folder)
 * 
 * @author Paulo Renato de Faria
 */
public class NDKJavaToCppGenerator {
	private static final String METHOD_BODY = "\n" + "{" + "\n" + "\t"
			+ "//TODO" + "\n" + "\t"
			+ "return (*env)->$native_method_to_call(env, $params);" + "\n"
			+ "}" + "\n";

	/**
	 * Variable to replace with the method signature
	 */
	private static final String DECL_METHOD = "#decl_method#";

	/**
	 * Absolute path to JDK bin folder
	 */
	private String jdkPath = "";

	/**
	 * Project to generated files
	 */
	private IProject project = null;

	/**
	 * Name without .java or .class
	 */
	private String classname = "";

	/**
	 * Qualified path to class
	 */
	private String classPackage = "";

	/**
	 * Full path to directory where classes will be placed
	 */
	private String outputDirectoryFromSource = "";

	/**
	 * H file absolute path
	 */
	private String hFileName = "";

	/**
	 * C file absolute path
	 */
	private String cFileName = "";

	/**
	 * File where to run javah command
	 */
	private File destinationDir = null;

	/**
	 * Maps JNI type to the base name for variable on c file
	 */
	private static Map<String, String> jniTypeToTemplateName = new HashMap<String, String>();

	private static final String BIN_FOLDER_NAME = "bin";

	static {
		jniTypeToTemplateName.put("jboolean", "javaBool");
		jniTypeToTemplateName.put("jbyte", "javaByte");
		jniTypeToTemplateName.put("jchar", "javaChar");
		jniTypeToTemplateName.put("jshort", "javaShort");
		jniTypeToTemplateName.put("jint", "javaInt");
		jniTypeToTemplateName.put("jlong", "javaLong");
		jniTypeToTemplateName.put("jfloat", "javaFloat");
		jniTypeToTemplateName.put("jdouble", "javaDouble");
		jniTypeToTemplateName.put("jstring", "javaString");

		jniTypeToTemplateName.put("jbooleanArray", "javaBoolArray");
		jniTypeToTemplateName.put("jbyteArray", "javaByteArray");
		jniTypeToTemplateName.put("jcharArray", "javaCharArray");
		jniTypeToTemplateName.put("jshortArray", "javaShortArray");
		jniTypeToTemplateName.put("jintArray", "javaIntArray");
		jniTypeToTemplateName.put("jlongArray", "javaLongArray");
		jniTypeToTemplateName.put("jfloatArray", "javaFloatArray");
		jniTypeToTemplateName.put("jdoubleArray", "javaDoubleArray");
		jniTypeToTemplateName.put("jstringArray", "javaStringArray");

		jniTypeToTemplateName.put("jobject", "javaObject");
	}

	/**
	 * 
	 * @param project
	 * @param classname
	 * @param classPackage
	 * @param outputDirectoryFromSource
	 * @throws IOException
	 */
	public NDKJavaToCppGenerator(IProject project, String classname,
			String classPackage, String outputDirectoryFromSource) {
		super();

		File f = null;
		if (checkJavaSdkExistence()) {
			f = retrieveJavaSdk();
		} else {
			messageWhenJavaSdkNotFound();
		}

		if (f != null) {
			this.jdkPath = f.getAbsolutePath();
		}

		this.project = project;
		this.classname = classname;
		this.classPackage = classPackage;
		this.outputDirectoryFromSource = outputDirectoryFromSource;
		String sourceFileName = this.classPackage.replace(".", "_")
				+ this.classname;
		this.hFileName = sourceFileName + ".h";
		this.cFileName = sourceFileName + ".c";
		this.destinationDir = new File(this.outputDirectoryFromSource);
	}

	private void messageWhenJavaSdkNotFound() {
		UIPlugin.log(
				IStatus.ERROR,
				"Java SDK not found. It is required to run application to generate C source and header based on Java class");
	}

	/**
	 * Generates source and header files
	 * 
	 * @throws IOException
	 *             if an error occurs executing javah command or creating .c
	 *             file based on .h created
	 * @throws CoreException
	 * @throws InterruptedException
	 */
	public void generateCppSourceAndHeader(IProgressMonitor monitor) {
		try {
			if (monitor == null) {
				monitor = new NullProgressMonitor();
			}
			monitor.beginTask(
					Messages.JNI_SOURCE_HEADER_CREATION_MONITOR_TASK_NAME, 90);
			monitor.setTaskName(Messages.JNI_SOURCE_HEADER_CREATION_MONITOR_STEP0);
			generateHeader();
			monitor.worked(40);
			monitor.setTaskName(Messages.JNI_SOURCE_HEADER_CREATION_MONITOR_STEP1);
			generateSource();
			monitor.worked(40);
			monitor.setTaskName(Messages.JNI_SOURCE_HEADER_CREATION_MONITOR_STEP2);
			project.refreshLocal(IResource.DEPTH_INFINITE,
					new SubProgressMonitor(monitor, 10));
		} catch (Exception e) {
			String title = Messages.JNI_SOURCE_HEADER_CREATION_MONITOR_FILES_ERROR;
			String message = e.getLocalizedMessage();
			if (message == null || message.equals("")) {
				message = Messages.JNI_SOURCE_HEADER_CREATION_MONITOR_FILES_ERROR;
			}
			MessageUtils.showErrorDialog(title, message);
			UIPlugin.log(title, e);
		}

		monitor.done();

	}

	/**
	 * Generates header files
	 * 
	 * @throws IOException
	 *             if an error occurs executing javah command
	 */
	private void generateHeader() throws IOException {
		String cmd = null;
		if (Platform.getOS().equals(Platform.WS_WIN32)) {
			cmd = "\"" + jdkPath + File.separator + "javah\"" + " -classpath "
					+ "\".;" + project.getLocation().toOSString()
					+ File.separator + BIN_FOLDER_NAME + File.separator
					+ "classes" + "\"" + " -verbose " + classPackage
					+ classname;
		} else if (Platform.getOS().equals(Platform.OS_MACOSX)
				|| Platform.getOS().equals(Platform.OS_LINUX)) {
			cmd = jdkPath + File.separator + "javah" + " -classpath "
					+ project.getLocation().toOSString() + File.separator
					+ BIN_FOLDER_NAME + File.separator + "classes"
					+ " -verbose " + classPackage + classname;
		}
		UIPlugin.log(IStatus.INFO, "Executing cmd:" + cmd);
		Runtime.getRuntime().exec(cmd, null, destinationDir);

	}

	/**
	 * Reads h file generated by javah and create c file with the stub calls to
	 * JNI
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void generateSource() throws IOException, InterruptedException,
			Exception {
		List<String> methodDeclarations = new ArrayList<String>();
		CSourceModel cModelIfWrittenIntoEmptyFile = extractDeclarationMethodFromHFile(methodDeclarations);

		// create new c class
		File cFile = new File(destinationDir, cFileName);
		if (cFile.exists()) {
			CSourceModel cModelExistentNow = readCFileAndExtractModel();
			CSourceModel cModelToWriteOnlyNewMethods = new CSourceModel();
			cModelToWriteOnlyNewMethods.createModelThroughDiff(
					cModelIfWrittenIntoEmptyFile, cModelExistentNow);
			// rewrite c file (inserting only new methods)
			copyAndAppendNewMethodsToSource(cModelToWriteOnlyNewMethods, cFile);
		} else {
			writeSource(methodDeclarations, cFile);

			// add new source file to makefile
			IResource makefileRes = project
					.findMember(NDKUtils.DEFAULT_JNI_FOLDER_NAME + "/"
							+ NDKUtils.MAKEFILE_FILE_NAME);
			if (makefileRes != null) {
				NDKUtils.addSourceFileToMakefile(makefileRes, cFile.getName());
			}
		}

		MessageUtils
				.showInformationDialog(
						Messages.JNI_SOURCE_HEADER_CREATION_MONITOR_FILES_SUCCESSFULLY_CREATED,
						Messages.JNI_SOURCE_HEADER_CREATION_MONITOR_FILES_SUCCESSFULLY_SOURCEHEADER_CREATED_MESSAGE);
	}

	/**
	 * Reads C file on disk and extract model with the methods
	 * 
	 * @return
	 * @throws IOException
	 */
	private CSourceModel readCFileAndExtractModel() throws IOException {
		CSourceModel model = new CSourceModel();
		// read c file
		File cFile = new File(destinationDir, cFileName);
		BufferedReader reader = new BufferedReader(new FileReader(cFile));
		try {
			String line = reader.readLine();
			CSourceMethod method = null;
			// read lines while end of file not found
			while (line != null) {
				if (!line.equals("")) {
					String methodDeclaration = "";
					Set<String> returnTypesAllowed = new HashSet<String>();
					returnTypesAllowed.addAll(jniTypeToTemplateName.keySet());
					returnTypesAllowed.add("void");
					boolean foundReturnTypeValid = false;
					String returnTypeToken = null;
					for (String typeAllowed : returnTypesAllowed) {
						StringTokenizer token = new StringTokenizer(line);
						returnTypeToken = token.nextToken();
						if (returnTypeToken.equals(typeAllowed)) {
							foundReturnTypeValid = true;
							break;
						}
					}

					if (foundReturnTypeValid) {
						// found JNI method declaration on c file
						methodDeclaration = line;
						String aux = reader.readLine();
						// check if declaration spans by more than one line
						// so read until find terminator ;
						while ((aux != null) && !aux.trim().endsWith("{")) {
							methodDeclaration += aux;
							aux = reader.readLine();
						}
						if ((aux != null) && aux.trim().endsWith("{")) {
							methodDeclaration += aux;
						}

						method = new CSourceMethod();
						addMethodReturnTypeAndSignature(methodDeclaration,
								method);

						// add names to variables according to type
						int initialIndex = methodDeclaration.indexOf("(");
						int endIndex = methodDeclaration.indexOf(")");
						String argsDecl = methodDeclaration.substring(
								initialIndex + 1, endIndex);
						StringTokenizer argsTokenizer = new StringTokenizer(
								argsDecl, ",");
						while (argsTokenizer.hasMoreTokens()) {
							String parameterTypeAndName = argsTokenizer
									.nextToken().trim();
							StringTokenizer parTypeToken = new StringTokenizer(
									parameterTypeAndName);
							String parameterType = parTypeToken.hasMoreTokens() ? parTypeToken
									.nextToken() : null;
							if (parameterType != null) {
								method.add(parameterType);
							} else {

								UIPlugin.log(IStatus.WARNING,
										"Method does not have right parameter type: "
												+ parameterTypeAndName);

							}
						}
						model.getMethods().add(method);
					}
				}
				line = reader.readLine();
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		return model;
	}

	/**
	 * Copies content and append new methods to source
	 * 
	 * @param newMethodsModel
	 * @param cFile
	 * @throws IOException
	 */
	private void copyAndAppendNewMethodsToSource(CSourceModel newMethodsModel,
			File cFile) throws IOException, Exception {
		// try to make copy of current file for backup purposes (but do not
		// generate error in case this process fails; it is not essential)
		boolean backupGenerated = false;
		File backupFile = null;
		try {
			backupFile = File.createTempFile(cFile.getName(), null);

			// Getting file channels
			FileChannel in = new FileInputStream(cFile).getChannel();
			FileChannel out = new FileOutputStream(backupFile).getChannel();

			// JavaVM does its best to do this as native I/O operations.
			in.transferTo(0, in.size(), out);

			// Closing file channels will close corresponding stream objects as
			// well.
			out.close();
			in.close();
			backupGenerated = true;
		} catch (Exception e) {
			// do nothing; only protect against errors
		}

		// read current file
		String currentFileText = "";
		BufferedReader reader = new BufferedReader(new FileReader(cFile));
		BufferedWriter writer = null;
		try {
			String line = reader.readLine();
			// read lines while end of file not found
			while (line != null) {
				currentFileText += line + "\n";
				line = reader.readLine();
			}

			// append new methods in the end
			writer = new BufferedWriter(new FileWriter(cFile));
			writer.append(currentFileText);
			String methodTemplate = "#returnType# #signature# ( #params# ) "
					+ METHOD_BODY;

			if ((newMethodsModel != null)
					&& (newMethodsModel.getMethods() != null)) {
				for (CSourceMethod method : newMethodsModel.getMethods()) {
					String callMethod = "";
					callMethod = methodTemplate.replace("#returnType#",
							method.getReturnType());
					callMethod = callMethod.replace("#signature#",
							method.getSignature());
					String params = "";

					int i = 0;
					int argIndex = 0;
					for (String param : method.getParameterTypes()) {
						String variableTypeAndVariableName = "";
						if (i == 0) {
							variableTypeAndVariableName = "JNIEnv* env";
							params += variableTypeAndVariableName;
						} else if (i == 1) {
							variableTypeAndVariableName = "jobject thiz";
							params += ", " + variableTypeAndVariableName;
						} else {
							String variableName = jniTypeToTemplateName
									.get(param);
							params += ", " + param + " " + variableName
									+ argIndex;
							argIndex++;
						}
						i++;
					}
					callMethod = callMethod.replace("#params#", params);
					writer.append(callMethod);
				}
			}

			// delete backup file, if it exists
			if (backupFile != null && backupFile.exists()) {
				try {
					backupFile.delete();
				} catch (Exception ex) {
					// do nothing; only protect against problems
				}
			}
		} catch (Exception e) {
			// recover backup file, if it exists
			if (backupGenerated) {
				recoverBackupAfterError(backupFile, cFile);
			}
			throw e;
		} finally {
			if (reader != null) {
				reader.close();
			}

			if (writer != null) {
				writer.close();
			}
		}
	}

	private void recoverBackupAfterError(File backupFile, File originalFile) {
		if (backupFile != null && backupFile.exists()) {
			try {
				// get file channels
				FileChannel in = new FileInputStream(backupFile).getChannel();
				FileChannel out = new FileOutputStream(originalFile)
						.getChannel();

				// JavaVM does its best to do this as native I/O operations.
				in.transferTo(0, in.size(), out);

				// closing file channels will close corresponding stream objects
				// as well.
				out.close();
				in.close();

				// delete backup file
				backupFile.delete();
			} catch (Exception ex) {
				// do nothing; only protect against problems
			}
		}
	}

	/**
	 * Writes source file
	 * 
	 * @param methodDeclarations
	 * @param cFile
	 * @throws IOException
	 */
	private void writeSource(List<String> methodDeclarations, File cFile)
			throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(cFile));
		String methodTemplate = DECL_METHOD + METHOD_BODY;
		try {
			if (methodDeclarations != null) {
				writer.append("#include <string.h>" + "\n");
				writer.append("#include <jni.h>" + "\n\n");
				for (String methodDeclaration : methodDeclarations) {
					String callMethod = methodTemplate.replace(DECL_METHOD,
							methodDeclaration);
					writer.append(callMethod);
				}
			}
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	/**
	 * Extract list of methods based on h file and creates the CSourceModel that
	 * should be created (if written from an empty file).
	 * 
	 * @param methodDeclarations
	 * @throws FileNotFoundException
	 *             h file not found
	 * @throws IOException
	 *             error occurs on reading h file
	 * @throws InterruptedException
	 */
	private CSourceModel extractDeclarationMethodFromHFile(
			List<String> methodDeclarations) throws FileNotFoundException,
			IOException, InterruptedException {
		CSourceModel model = new CSourceModel();
		// read h file
		File hFile = new File(destinationDir, hFileName);
		// WORKAROUND: Need to wait for h file to be ready to be read
		int numberOfRetries = 0;
		while (!hFile.canRead()) {
			Thread.sleep(300);
			if (numberOfRetries > 100) {
				break;
			}
			numberOfRetries++;
		}
		if (numberOfRetries >= 100) {
			throw new FileNotFoundException(
					"Header file was not generated. Check for compilation issues on project.");
		}
		BufferedReader reader = new BufferedReader(new FileReader(hFile));
		try {
			String line = reader.readLine();
			// read lines while end of file not found
			while (line != null) {
				String methodDeclaration = "";
				if (line.trim().toUpperCase().startsWith("JNIEXPORT")) {
					CSourceMethod method = new CSourceMethod();
					// found JNI method declaration on h file
					methodDeclaration = line;
					String aux = reader.readLine();
					// check if declaration spans by more than one line
					// so read until find terminator ;
					while ((aux != null) && !aux.trim().endsWith(";")) {
						methodDeclaration += aux;
						aux = reader.readLine();
					}
					if ((aux != null) && aux.trim().endsWith(";")) {
						methodDeclaration += aux;
					}

					// remove JNIEXPORT from declaration (not required at c file
					// to create)
					methodDeclaration = methodDeclaration.replace("JNIEXPORT",
							"");
					// remove JNICALL from declaration (not required at c file
					// to create)
					methodDeclaration = methodDeclaration
							.replace("JNICALL", "");

					addMethodReturnTypeAndSignature(methodDeclaration, method);

					// substitute by JNIEnv* env (need to be replaceAll due to
					// *)
					method.add("JNIEnv*");
					methodDeclaration = methodDeclaration.replaceAll(
							"JNIEnv\\s?\\*", "JNIEnv\\* env");
					// substitute jobject by jobject thiz
					method.add("jobject");
					methodDeclaration = methodDeclaration.replaceFirst(
							"jobject", "jobject thiz");
					// remove ; from declaration (not required at c file to
					// create)
					methodDeclaration = methodDeclaration.replace(";", "");

					// add names to variables according to type
					int argIndex = 0;
					int initialIndex = methodDeclaration.indexOf("(");
					int endIndex = methodDeclaration.indexOf(")");
					String argsDecl = methodDeclaration.substring(
							initialIndex + 1, endIndex);
					StringTokenizer argsTokenizer = new StringTokenizer(
							argsDecl, ",");
					String newArgsDecl = "";
					int i = 0;
					while (argsTokenizer.hasMoreTokens()) {
						String parameterType = argsTokenizer.nextToken().trim();
						String substituteType = jniTypeToTemplateName
								.get(parameterType);
						if (substituteType != null) {
							method.add(parameterType);
							// found type to add variable name
							String newArg = parameterType + " "
									+ substituteType + argIndex;
							argIndex++;
							newArgsDecl += ", "
									+ parameterType.replaceFirst(parameterType,
											newArg);
						} else {
							if (i > 0) {
								newArgsDecl += ", " + parameterType;
							} else {
								newArgsDecl += parameterType;
							}
						}
						i++;
					}
					methodDeclaration = methodDeclaration.substring(0,
							initialIndex - 1) + "(" + newArgsDecl + ")";
					// keep method declaration on list
					methodDeclarations.add(methodDeclaration);
					model.getMethods().add(method);
				}
				line = reader.readLine();
			}
		} finally {
			if (reader != null) {
				reader.close();
			}

			if (hFile.exists()) {
				hFile.delete();
			}
		}
		return model;
	}

	/**
	 * Add method return type and signature
	 * 
	 * @param methodString
	 * @param method
	 */
	private void addMethodReturnTypeAndSignature(String methodString,
			CSourceMethod method) {
		StringTokenizer signatureReturnBreaker = new StringTokenizer(
				methodString);
		String returnType = signatureReturnBreaker.hasMoreTokens() ? signatureReturnBreaker
				.nextToken() : null;
		String signature = signatureReturnBreaker.hasMoreTokens() ? signatureReturnBreaker
				.nextToken() : null;
		if ((returnType != null) && (signature != null)) {
			method.setReturnType(returnType);
			method.setSignature(signature);
		} else {
			UIPlugin.log(IStatus.WARNING,
					"Method does not have right returnType - signature: "
							+ methodString);
		}
	}

	/**
	 * Utility method to check if the user has installed the JavaSDK in his
	 * machine.
	 * 
	 * @return True if the JDK was found, false if not.
	 * @throws IOException
	 */
	public static boolean checkJavaSdkExistence() {

		boolean result = true;

		File jdkFile = null;

		IVMInstall defaultJvm = JavaRuntime.getDefaultVMInstall();
		if (defaultJvm != null) {
			String javaHome = defaultJvm.getInstallLocation().getAbsolutePath();

			if (javaHome != null) {

				// Check if jdk is running as jre
				jdkFile = new File(javaHome, ".." + File.separator
						+ BIN_FOLDER_NAME);
				if (!jdkFile.exists()) {
					jdkFile = new File(javaHome, BIN_FOLDER_NAME);

				}
			}

			if ((jdkFile != null) && jdkFile.exists()) {
				// Execute javac -version command and see if there are errors
				String cmd = null;
				if (Platform.getOS().equals(Platform.WS_WIN32)) {
					cmd = "\"" + jdkFile.getAbsolutePath() + File.separator
							+ "javac\"" + " -version";
				} else if (Platform.getOS().equals(Platform.OS_MACOSX)
						|| Platform.getOS().equals(Platform.OS_LINUX)) {
					cmd = jdkFile.getAbsolutePath() + File.separator + "javac"
							+ " -version";
				}
				UIPlugin.log(IStatus.INFO, "Executing cmd:" + cmd);

				try {
					Runtime.getRuntime().exec(cmd);
				} catch (IOException e) {
					result = false;
					UIPlugin.log("Error while checking for JDK existence.", e);

				}

			}
		} else {
			result = false;
		}

		return result;
	}

	/**
	 * Utility method to retrieve a file handler to the Java SDK. If it does not
	 * exist or it can't be found, null will be returned.
	 * 
	 * @return A file handler to the Java SDK
	 */
	public static File retrieveJavaSdk() {
		File jdkFile = null;

		IVMInstall defaultJvm = JavaRuntime.getDefaultVMInstall();
		if (defaultJvm != null) {

			String javaHome = defaultJvm.getInstallLocation().getAbsolutePath();

			if (javaHome != null) {
				// Check if jdk is running as jre
				jdkFile = new File(javaHome, ".." + File.separator
						+ BIN_FOLDER_NAME);
				if (!jdkFile.exists()) {
					jdkFile = new File(javaHome, BIN_FOLDER_NAME);
				}
			}
		}
		return jdkFile;
	}

}
