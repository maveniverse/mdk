/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mdk.kurt.deployers;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mdk.kurt.DeployerFactory;
import eu.maveniverse.maven.mdk.kurt.KurtConfig;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

@Singleton
@Named(LocalStagingDeployerFactory.NAME)
public class LocalStagingDeployerFactory implements DeployerFactory {
    public static final String NAME = "local-staging";

    private final RepositorySystem repositorySystem;

    @Inject
    public LocalStagingDeployerFactory(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public LocalStagingDeployer createDeployer(MavenSession session) {
        Path target = Paths.get(session.getTopLevelProject().getBuild().getDirectory());
        Path stagingDirectory = target.resolve(KurtConfig.LOCAL_STAGING_DIRECTORY.require(session));
        RemoteRepository stagingRepository = repositorySystem.newDeploymentRepository(
                session.getRepositorySession(),
                new RemoteRepository.Builder(
                                KurtConfig.LOCAL_STAGING_ID.require(session),
                                "default",
                                stagingDirectory.toFile().toURI().toASCIIString())
                        .build());
        return new LocalStagingDeployer(repositorySystem, stagingRepository, stagingDirectory);
    }
}
