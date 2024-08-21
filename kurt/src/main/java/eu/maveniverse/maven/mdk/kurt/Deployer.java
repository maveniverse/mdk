/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mdk.kurt;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Deployer component.
 */
public interface Deployer extends Closeable {
    /**
     * Returns the name of deployer (for logging and human consumption purposes).
     */
    String getName();

    /**
     * This method is called whenever Maven Deploy Plugin wants to deploy. Here implementation
     * have chance to inspect request and decide does it "accept" it (returns {@code true}) or
     * "refuses" it (returns {@code false}) but may throw as well to fail whole build.
     * <p>
     * Theoretically, implementation could deploy even here, but doing so is discouraged: MDK
     * follows "best practices" and interleaved deployment (per module) is discouraged, so the "least"
     * implementation should do is "deploy at end". This is what happens when MDK is present in
     * system as extension, but nothing more MDK related is configured by user.
     */
    boolean processRequest(MavenSession mavenSession, DeployRequest deployRequest)
            throws DeploymentException, IOException;

    /**
     * Performs "batched" deploy, if implementation wants that (see {@link #processRequest(MavenSession, DeployRequest)}).
     */
    void deployAll(MavenSession session, Map<RemoteRepository, DeployRequest> deployRequests)
            throws DeploymentException, IOException;
}
