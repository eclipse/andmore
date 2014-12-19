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
package org.eclipse.andmore.android.installer.utilities;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.andmore.android.installer.InstallerException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * Factory for {@link IMetadataRepository} and {@link IArtifactRepository}. Will
 * retrieve P2 repositories based on it's URI, keeping a cached version of the
 * repositories in order to increase speed. Note: This class is a singleton, use
 * the getInstance() method.
 */
class P2RepositoriesFactory {

	private static P2RepositoriesFactory instance;

	private final Map<URI, IMetadataRepository> metadataRepositories;

	private final Map<URI, IArtifactRepository> artifactRepositories;

	/*
	 * This class is a singleton.
	 */
	private P2RepositoriesFactory() {
		metadataRepositories = new HashMap<URI, IMetadataRepository>();
		artifactRepositories = new HashMap<URI, IArtifactRepository>();
	}

	public static P2RepositoriesFactory getInstance() {
		if (instance == null) {
			instance = new P2RepositoriesFactory();
		}

		return instance;
	}

	/**
	 * Retrieves a {@link IMetadataRepository} from a given URI. If the
	 * repository is found on the internal cache then the cached version is
	 * retrieved, otherwise the repository is loaded and added to the cache. The
	 * behavior can be overridden by setting the force flag to true. When this
	 * happens the repository will be loaded again and a new version will be put
	 * on cache.
	 * 
	 * @param uri
	 *            repository URI
	 * @param force
	 *            force loading the repository from the P2 mechanism.
	 * @param monitor
	 * @return {@link IMetadataRepository} from the given URI, if none is found
	 *         null is returned.
	 * @throws InstallerException
	 */
	public IMetadataRepository getMetadataRepository(URI uri, boolean force, IProgressMonitor monitor)
			throws InstallerException {
		IMetadataRepository repository = null;

		repository = metadataRepositories.get(uri);

		if ((repository == null) || force) {
			repository = P2Utilities.getMetadataRepository(uri, monitor);
			metadataRepositories.put(uri, repository);
		} else {
			monitor.done();
		}
		return repository;
	}

	/**
	 * Retrieves a {@link IArtifactRepository} from a given URI. If the
	 * repository is found on the internal cache then the cached version is
	 * retrieved, otherwise the repository is loaded and added to the cache. The
	 * behavior can be overridden by setting the force flag to true. When this
	 * happens the repository will be loaded again and a new version will be put
	 * on cache.
	 * 
	 * @param uri
	 *            repository URI
	 * @param force
	 *            force loading the repository from the P2 mechanism.
	 * @param monitor
	 * @return {@link IArtifactRepository} from the given URI, if none is found
	 *         null is returned.
	 * @throws InstallerException
	 */
	public IArtifactRepository getArtifactRepository(URI uri, boolean force, IProgressMonitor monitor)
			throws InstallerException {
		IArtifactRepository repository = null;

		repository = artifactRepositories.get(uri);

		if ((repository == null) || force) {
			repository = P2Utilities.getArtifactRepository(uri, monitor);
			artifactRepositories.put(uri, repository);
		} else {
			monitor.done();
		}
		return repository;
	}

}
