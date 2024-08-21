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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;

@Singleton
@Named(ResolverDeployerFactory.NAME)
public class ResolverDeployerFactory implements DeployerFactory {
    public static final String NAME = "resolver";

    private final RepositorySystem repositorySystem;

    @Inject
    public ResolverDeployerFactory(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public ResolverDeployer createDeployer(MavenSession session) {
        return new ResolverDeployer(repositorySystem);
    }
}
