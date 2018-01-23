/**
 * 
 */
package org.eclipse.sequoyah.android.cdt.internal.build.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Reads from a template substituting marked values from the supplied Map.
 *
 */
public class TemplatedInputStream extends InputStream {

	final InputStream in;
	final Map<String, String> map;
	char[] sub;
	int pos;
	int mark;
	
	public TemplatedInputStream(InputStream in, Map<String, String> map) {
		this.in = in;
		this.map = map;
	}
	
	@Override
	public int read() throws IOException {
		// if from a mark, return the char
		if (mark != 0) {
			int c = mark;
			mark = 0;
			return c;
		}

		// return char from sub layer if available
		if (sub != null) {
			char c = sub[pos++];
			if (pos >= sub.length)
				sub = null;
			return c;
		}
		
		int c = in.read();
		if (c == '%') {
			// check if it's a sub
			c = in.read();
			if (c == '{') {
				// it's a sub
				StringBuffer buff = new StringBuffer();
				for (c = in.read(); c != '}' && c >= 0; c = in.read())
					buff.append((char)c);
				String str = map.get(buff.toString());
				if (str != null) {
					sub = str.toCharArray();
					pos = 0;
				}
				return read(); // recurse to get the real char
			} else {
				// not a sub
				mark = c;
				return '%';
			}
		}
		
		return c;
	}

	@Override
	public void close() throws IOException {
		super.close();
		in.close();
	}
	
}
