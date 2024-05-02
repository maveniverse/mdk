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
     * The status of deploy request.
     */
    enum RequestStatus {
        /**
         * The request was processed, nothing more to be done with it.
         */
        PROCESSED,
        /**
         * The request was acknowledged, but should be "batched". No deploy happened yet.
         */
        DELAYED,
        /**
         * The request is refused for whatever reason (i.e. this component accepts only RELEASE artifacts but request had
         * SNAPSHOT artifacts).
         */
        REFUSED
    }

    /**
     * Returns the name of deployer (for logging and human consumption purposes).
     */
    String getName();

    /**
     * This method is called whenever Maven Deploy Plugin wants to deploy (have on mind it can "deploy at end"). The
     * implementation have several choices: it can process the request and return {@link RequestStatus#PROCESSED},
     * it can inspect the request and return {@link RequestStatus#DELAYED} to have it delivered "batched" into
     * {@link #processAll(MavenSession, Map)} method, or it can simply refuse by returning {@link RequestStatus#REFUSED}
     * for any reason (for example it handles only RELEASE artifacts and request carries SNAPSHOT ones). Finally, it
     * can throw as well, that will make deployment (and whole build) fail.
     */
    RequestStatus processRequest(MavenSession mavenSession, DeployRequest deployRequest)
            throws DeploymentException, IOException;

    /**
     * Performs "batched" deploy, if implementation wants that (see {@link #processRequest(MavenSession, DeployRequest)}).
     */
    void processAll(MavenSession session, Map<RemoteRepository, DeployRequest> deployRequests)
            throws DeploymentException, IOException;
}
