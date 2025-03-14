/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mdk.kurt.jreleaser;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mdk.kurt.deployers.DeployerSupport;
import eu.maveniverse.maven.mdk.kurt.deployers.LocalStagingDeployer;
import java.io.IOException;
import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.jreleaser.workflow.Workflows;

/**
 * A JReleaser deployer, that does not accept SNAPSHOT artifacts. It uses "full-release" workflow
 * of JReleaser. Reuses {@link LocalStagingDeployer} delegates all local staging thing to it,
 * and then takes over.
 */
public class FullReleaseDeployer extends DeployerSupport {
    private final LocalStagingDeployer localStagingDeployer;
    private final JReleaserContextFactory contextFactory;

    public FullReleaseDeployer(LocalStagingDeployer localStagingDeployer, JReleaserContextFactory contextFactory) {
        super(FullReleaseDeployerFactory.NAME);
        this.localStagingDeployer = requireNonNull(localStagingDeployer);
        this.contextFactory = requireNonNull(contextFactory);
    }

    @Override
    public boolean processRequest(MavenSession mavenSession, DeployRequest deployRequest) {
        return localStagingDeployer.processRequest(mavenSession, deployRequest);
    }

    @Override
    public void deployAll(MavenSession session, Map<RemoteRepository, DeployRequest> deployRequests)
            throws DeploymentException, IOException {
        localStagingDeployer.deployAll(session, deployRequests);
        logger.info("Configuring and invoking JReleaser...");
        logger.info("======================================");
        Workflows.fullRelease(contextFactory.createContext(
                        session,
                        localStagingDeployer.getStagingDirectory(),
                        org.jreleaser.model.api.JReleaserCommand.FULL_RELEASE))
                .execute();
    }
}
