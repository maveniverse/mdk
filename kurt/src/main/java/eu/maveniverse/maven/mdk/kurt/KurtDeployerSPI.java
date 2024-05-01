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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.deploy.spi.DeployerSPI;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kurt deployer SPI
 */
@Singleton
@Named("kurt")
@Priority(10)
public class KurtDeployerSPI extends AbstractMavenLifecycleParticipant implements DeployerSPI {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RepositorySystem repositorySystem;

    private final List<DeployRequest> deployAtEndRequests = Collections.synchronizedList(new ArrayList<>());

    @Inject
    public KurtDeployerSPI(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public void deploy(RepositorySystemSession session, DeployRequest deployRequest, Map<String, Object> parameters)
            throws DeploymentException {
        deployAtEndRequests.add(deployRequest);
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        boolean errors = !session.getResult().getExceptions().isEmpty();

        if (!deployAtEndRequests.isEmpty()) {

            log.info("");
            log.info("------------------------------------------------------------------------");

            if (errors) {
                log.info("-- Not performing deploy at end due to errors                         --");
            } else {
                log.info("-- Performing deploy at end                                           --");
                log.info("------------------------------------------------------------------------");

                synchronized (deployAtEndRequests) {
                    HashMap<RemoteRepository, DeployRequest> batched = new HashMap<>();
                    for (DeployRequest deployRequest : deployAtEndRequests) {
                        if (!batched.containsKey(deployRequest.getRepository())) {
                            batched.put(deployRequest.getRepository(), deployRequest);
                        } else {
                            DeployRequest dr = batched.get(deployRequest.getRepository());
                            deployRequest.getArtifacts().forEach(dr::addArtifact);
                            deployRequest.getMetadata().forEach(dr::addMetadata);
                        }
                    }
                    try {
                        for (DeployRequest dr : batched.values()) {
                            repositorySystem.deploy(session.getRepositorySession(), dr);
                        }
                    } catch (DeploymentException e) {
                        log.error(e.getMessage(), e);
                        throw new MavenExecutionException(e.getMessage(), e);
                    }
                    deployAtEndRequests.clear();
                }
            }

            log.info("------------------------------------------------------------------------");
        }
    }
}
