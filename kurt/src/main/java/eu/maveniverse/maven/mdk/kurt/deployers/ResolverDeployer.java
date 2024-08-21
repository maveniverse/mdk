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
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * "Ordinary" Resolver deployer, eats everything.
 */
public class ResolverDeployer extends DeployerSupport {
    private final RepositorySystem repositorySystem;

    public ResolverDeployer(RepositorySystem repositorySystem) {
        super(ResolverDeployerFactory.NAME);
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public boolean processRequest(MavenSession mavenSession, DeployRequest deployRequest) throws DeploymentException {
        return true;
    }

    @Override
    public void deployAll(MavenSession session, Map<RemoteRepository, DeployRequest> deployRequests)
            throws DeploymentException {
        for (DeployRequest dr : deployRequests.values()) {
            repositorySystem.deploy(session.getRepositorySession(), dr);
        }
    }
}
