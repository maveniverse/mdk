/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mdk.kurt;

import static java.util.Objects.requireNonNull;

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
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kurt Hectic reports.
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
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        if (sessionRef.compareAndSet(null, session)) {
            RepositorySystemSession repoSession = session.getRepositorySession();
            String deployerName = KurtConfig.DEPLOYER.require(repoSession);
            DeployerFactory deployerFactory = deployerFactories.get(deployerName);
            if (deployerFactory == null) {
                throw new MavenExecutionException(
                        "Non existing deployer selected, supported ones are: " + deployerFactories.keySet(),
                        (Throwable) null);
            }
            repoSession
                    .getData()
                    .set(DEPLOYER, deployerFactories.get(deployerName).createDeployer(session));
        }
    }

    @Override
    public boolean deploy(RepositorySystemSession session, DeployRequest deployRequest)
            throws DeploymentException, IOException {
        boolean accepted = getSelectedDeployer(session).processRequest(sessionRef.get(), deployRequest);
        if (accepted) {
            deployAtEndRequests.add(deployRequest);
        }
        return accepted;
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
                        deployer.deployAll(session, batched);
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
