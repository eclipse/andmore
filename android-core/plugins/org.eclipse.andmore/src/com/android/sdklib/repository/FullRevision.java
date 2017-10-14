/*******************************************************************************
 * Copyright (c) 2016 Matthew Piggott
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *	  Matthew Piggott - implementation
 *******************************************************************************/
package com.android.sdklib.repository;

/*
 * Wrapper class to bridge old & new Android SDK APIs
 */
import com.android.annotations.NonNull;
import com.android.repository.Revision;

public class FullRevision extends Revision {

	public FullRevision(int major, int minor, int micro, int preview) {
		super(major, minor, micro, preview);
	}

	public FullRevision(int major, int minor, int micro) {
		super(major, minor, micro);
	}

	public FullRevision(int major, int minor) {
		super(major, minor);
	}

	public FullRevision(int major, Integer minor, Integer micro, Integer preview) {
		super(major, minor, micro, preview);
	}

	public FullRevision(int major) {
		super(major);
	}

	public FullRevision(Revision revision) {
		super(revision);
	}


	@NonNull
	public static FullRevision parseRevision(@NonNull String revisionString)
			throws NumberFormatException {
		return new FullRevision(Revision.parseRevision(revisionString));
	}
}
