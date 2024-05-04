/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
