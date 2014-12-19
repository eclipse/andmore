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

package org.eclipse.andmore.ddms.systrace;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * {@link SystraceOutputParser} receives the output of atrace command run on the
 * device, parses it and generates html based on the trace
 */
public class SystraceOutputParser {
	private static final String TRACE_START = "TRACE:\n"; //$NON-NLS-1$

	private final boolean mUncompress;
	private final String mJs;
	private final String mCss;
	private final String mHtmlPrefix;
	private final String mHtmlSuffix;

	private byte[] mAtraceOutput;
	private int mAtraceLength;
	private int mSystraceIndex = -1;

	/**
	 * Constructs a atrace output parser.
	 * 
	 * @param compressedStream
	 *            Is the input stream compressed using zlib?
	 * @param systraceJs
	 *            systrace javascript content
	 * @param systraceCss
	 *            systrace css content
	 */
	public SystraceOutputParser(boolean compressedStream, String systraceJs, String systraceCss, String htmlPrefix,
			String htmlSuffix) {
		mUncompress = compressedStream;
		mJs = systraceJs;
		mCss = systraceCss;
		mHtmlPrefix = htmlPrefix;
		mHtmlSuffix = htmlSuffix;
	}

	/**
	 * Parses the atrace output for systrace content.
	 * 
	 * @param atraceOutput
	 *            output bytes from atrace
	 */
	public void parse(byte[] atraceOutput) {
		mAtraceOutput = atraceOutput;
		mAtraceLength = atraceOutput.length;

		removeCrLf();

		// locate the trace start marker within the first hundred bytes
		String header = new String(mAtraceOutput, 0, Math.min(100, mAtraceLength));
		mSystraceIndex = locateSystraceData(header);

		if (mSystraceIndex < 0) {
			throw new RuntimeException("Unable to find trace start marker 'TRACE:':\n" + header);
		}
	}

	/** Replaces \r\n with \n in {@link #mAtraceOutput}. */
	private void removeCrLf() {
		int dst = 0;
		for (int src = 0; src < mAtraceLength - 1; src++, dst++) {
			byte copy;
			if (mAtraceOutput[src] == '\r' && mAtraceOutput[src + 1] == '\n') {
				copy = '\n';
				src++;
			} else {
				copy = mAtraceOutput[src];
			}
			mAtraceOutput[dst] = copy;
		}

		mAtraceLength = dst;
	}

	private int locateSystraceData(String header) {
		int index = header.indexOf(TRACE_START);
		if (index < 0) {
			return -1;
		} else {
			return index + TRACE_START.length();
		}
	}

	public String getSystraceHtml() {
		if (mSystraceIndex < 0) {
			return "";
		}

		String trace = "";
		if (mUncompress) {
			Inflater decompressor = new Inflater();
			decompressor.setInput(mAtraceOutput, mSystraceIndex, mAtraceLength - mSystraceIndex);

			byte[] buf = new byte[4096];
			int n;
			StringBuilder sb = new StringBuilder(1000);
			try {
				while ((n = decompressor.inflate(buf)) > 0) {
					sb.append(new String(buf, 0, n));
				}
			} catch (DataFormatException e) {
				throw new RuntimeException(e);
			}
			decompressor.end();

			trace = sb.toString();
		} else {
			trace = new String(mAtraceOutput, mSystraceIndex, mAtraceLength - mSystraceIndex);
		}

		// each line should end with the characters \n\ followed by a newline
		String html_out = trace.replaceAll("\n", "\\\\n\\\\\n");
		String header = String.format(mHtmlPrefix, mCss, mJs, "");
		String footer = mHtmlSuffix;
		return header + html_out + footer;
	}

	public static String getJs(File assetsFolder) {
		try {
			return String.format("<script language=\"javascript\">%s</script>",
					Files.toString(new File(assetsFolder, "script.js"), Charsets.UTF_8));
		} catch (IOException e) {
			return "";
		}
	}

	public static String getCss(File assetsFolder) {
		try {
			return String.format("<style type=\"text/css\">%s</style>",
					Files.toString(new File(assetsFolder, "style.css"), Charsets.UTF_8));
		} catch (IOException e) {
			return "";
		}
	}

	public static String getHtmlPrefix(File assetsFolder) {
		return getHtmlTemplate(assetsFolder, "prefix.html");
	}

	public static String getHtmlSuffix(File assetsFolder) {
		return getHtmlTemplate(assetsFolder, "suffix.html");
	}

	private static String getHtmlTemplate(File assetsFolder, String htmlFileName) {
		try {
			return Files.toString(new File(assetsFolder, htmlFileName), Charsets.UTF_8);
		} catch (IOException e) {
			return "";
		}
	}
}
