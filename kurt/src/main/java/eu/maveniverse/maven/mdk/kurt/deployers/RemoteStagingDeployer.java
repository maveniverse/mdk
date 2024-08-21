/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mdk.kurt.deployers;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * A "remote staging" deployer that does not accept SNAPSHOT artifacts.
 */
public class RemoteStagingDeployer extends DeployerSupport {
    private final RepositorySystem repositorySystem;
    private final RemoteRepository stagingRepository;

    public RemoteStagingDeployer(RepositorySystem repositorySystem, RemoteRepository stagingRepository) {
        super(RemoteStagingDeployerFactory.NAME);
        this.repositorySystem = requireNonNull(repositorySystem);
        this.stagingRepository = requireNonNull(stagingRepository);
    }

    public RemoteRepository getStagingRepository() {
        return stagingRepository;
    }

    @Override
    public boolean processRequest(MavenSession mavenSession, DeployRequest deployRequest) {
        return deployRequest.getArtifacts().stream().noneMatch(Artifact::isSnapshot);
    }

    @Override
    public void deployAll(MavenSession session, Map<RemoteRepository, DeployRequest> deployRequests)
            throws DeploymentException {
        logger.info(
                "Remote staging {} artifacts",
                deployRequests.values().stream()
                        .mapToLong(r -> r.getArtifacts().size())
                        .sum());
        for (DeployRequest dr : deployRequests.values()) {
            DeployRequest stagingRequest = new DeployRequest();
            stagingRequest.setRepository(stagingRepository);
            stagingRequest.setArtifacts(dr.getArtifacts());
            stagingRequest.setMetadata(dr.getMetadata());
            repositorySystem.deploy(session.getRepositorySession(), stagingRequest);
        }
    }
}
