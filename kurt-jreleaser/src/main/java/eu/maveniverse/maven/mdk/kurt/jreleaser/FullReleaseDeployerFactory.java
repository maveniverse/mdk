/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mdk.kurt.jreleaser;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mdk.kurt.Deployer;
import eu.maveniverse.maven.mdk.kurt.DeployerFactory;
import eu.maveniverse.maven.mdk.kurt.deployers.LocalStagingDeployerFactory;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;

@Singleton
@Named(FullReleaseDeployerFactory.NAME)
public class FullReleaseDeployerFactory implements DeployerFactory {
    public static final String NAME = "jreleaser-full-release";

    private final LocalStagingDeployerFactory localStagingDeployerFactory;
    private final JReleaserContextFactory contextFactory;

    @Inject
    public FullReleaseDeployerFactory(
            LocalStagingDeployerFactory localStagingDeployerFactory, JReleaserContextFactory contextFactory) {
        this.localStagingDeployerFactory = requireNonNull(localStagingDeployerFactory);
        this.contextFactory = requireNonNull(contextFactory);
    }

    @Override
    public Deployer createDeployer(MavenSession session) {
        return new FullReleaseDeployer(localStagingDeployerFactory.createDeployer(session), contextFactory);
    }
}
