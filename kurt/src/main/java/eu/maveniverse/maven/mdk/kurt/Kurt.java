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

import eu.maveniverse.maven.mdk.kurt.internal.ResolverDeployerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.deploy.spi.DeployerSPI;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.ConfigUtils;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kurt deployer SPI
 */
@SessionScoped
@Named("kurt")
@Priority(10)
public class Kurt extends AbstractMavenLifecycleParticipant implements DeployerSPI {
    private static final String DEPLOYER = Kurt.class.getName() + ".deployer";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, DeployerFactory> deployerFactories;

    private final List<DeployRequest> deployAtEndRequests;

    private final AtomicReference<MavenSession> sessionRef;

    @Inject
    public Kurt(Map<String, DeployerFactory> deployerFactories) {
        this.deployerFactories = requireNonNull(deployerFactories);
        this.deployAtEndRequests = Collections.synchronizedList(new ArrayList<>());
        this.sessionRef = new AtomicReference<>(null);
    }

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
        if (sessionRef.compareAndSet(null, session)) {
            RepositorySystemSession repoSession = session.getRepositorySession();
            String deployerName = ConfigUtils.getString(repoSession, ResolverDeployerFactory.NAME, KurtConfig.KURT_DEPLOYER);
            DeployerFactory deployerFactory = deployerFactories.get(deployerName);
            if (deployerFactory == null) {
                throw new MavenExecutionException(
                        "Non existing deployer selected, supported ones are: " + deployerFactories.keySet(),
                        session.getCurrentProject().getFile());
            }
            repoSession
                    .getData()
                    .set(DEPLOYER, deployerFactories.get(deployerName).createDeployer(session));
        }
    }

    @Override
    public boolean deploy(RepositorySystemSession session, DeployRequest deployRequest)
            throws DeploymentException, IOException {
        Deployer deployer = getSelectedDeployer(session);
        Deployer.RequestStatus status = deployer.processRequest(sessionRef.get(), deployRequest);
        switch (status) {
            case PROCESSED: {
                return true;
            }
            case DELAYED: {
                deployAtEndRequests.add(deployRequest);
                return true;
            }
            case REFUSED: {
                return false;
            }
            default:
                throw new IllegalStateException("Unknown status: " + status);
        }
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        try (Deployer deployer = getSelectedDeployer(session.getRepositorySession())) {
            boolean errors = !session.getResult().getExceptions().isEmpty();
            if (!deployAtEndRequests.isEmpty()) {
                log.info("");
                log.info("------------------------------------------------------------------------");
                if (errors) {
                    log.info("-- Not performing deploy at end due to errors");
                } else {
                    log.info("-- Performing deploy with " + deployer.getName());
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
                        deployer.processAll(session, batched);
                    }
                }

                log.info("------------------------------------------------------------------------");
            }
        } catch (DeploymentException | IOException e) {
            log.error(e.getMessage(), e);
            throw new MavenExecutionException(e.getMessage(), e);
        } finally {
            deployAtEndRequests.clear();
            sessionRef.set(null);
        }
    }

    private Deployer getSelectedDeployer(RepositorySystemSession session) {
        Deployer deployer = (Deployer) session.getData().get(DEPLOYER);
        if (deployer == null) {
            throw new IllegalStateException("No selected deployer found");
        }
        return deployer;
    }
}
