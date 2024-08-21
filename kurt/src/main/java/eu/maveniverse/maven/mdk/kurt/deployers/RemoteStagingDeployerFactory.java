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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.DeploymentRepository;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

@Singleton
@Named(RemoteStagingDeployerFactory.NAME)
public class RemoteStagingDeployerFactory implements DeployerFactory {
    public static final String NAME = "remote-staging";

    private final RepositorySystem repositorySystem;

    @Inject
    public RemoteStagingDeployerFactory(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public RemoteStagingDeployer createDeployer(MavenSession session) {
        boolean tlpHasRepo = false;
        String id = null;
        String url = null;
        if (session.getTopLevelProject() != null
                && session.getTopLevelProject().getDistributionManagement() != null
                && session.getTopLevelProject().getDistributionManagement().getRepository() != null) {
            DeploymentRepository repository =
                    session.getTopLevelProject().getDistributionManagement().getRepository();
            if (repository.getId() != null && repository.getUrl() != null) {
                id = KurtConfig.REMOTE_STAGING_ID.withDefault(repository::getId).require(session);
                url = KurtConfig.REMOTE_STAGING_URL
                        .withDefault(repository::getUrl)
                        .require(session);
                tlpHasRepo = true;
            }
        }
        if (!tlpHasRepo) {
            // this will hard fail if user did not set URL explicitly
            id = KurtConfig.REMOTE_STAGING_ID.require(session);
            url = KurtConfig.REMOTE_STAGING_URL.require(session);
        }

        RemoteRepository stagingRepository = repositorySystem.newDeploymentRepository(
                session.getRepositorySession(), new RemoteRepository.Builder(id, "default", url).build());
        return new RemoteStagingDeployer(repositorySystem, stagingRepository);
    }
}
