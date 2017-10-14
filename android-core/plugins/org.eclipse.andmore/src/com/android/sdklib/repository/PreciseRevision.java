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

import com.android.annotations.NonNull;
import com.android.repository.Revision;

/*
 * Wrapper class to bridge old & new Android SDK APIs
 */
public class PreciseRevision extends FullRevision {

	public PreciseRevision(int major, int minor, int micro, int preview) {
		super(major, minor, micro, preview);
	}

	public PreciseRevision(int major, int minor, int micro) {
		super(major, minor, micro);
	}

	public PreciseRevision(int major, int minor) {
		super(major, minor);
	}

	public PreciseRevision(int major, Integer minor, Integer micro, Integer preview) {
		super(major, minor, micro, preview);
	}

	public PreciseRevision(int major) {
		super(major);
	}

	public PreciseRevision(Revision revision) {
		super(revision);
	}


	@NonNull
	public static PreciseRevision parseRevision(@NonNull String revisionString)
			throws NumberFormatException {
		return new PreciseRevision(Revision.parseRevision(revisionString));
	}
}
