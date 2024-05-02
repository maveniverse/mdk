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
package org.apache.maven.plugins.deploy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugins.deploy.spi.DeployerSPI;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * Fallback deployer, this is the "original" m-deploy-p code in separate component. This code is always present, and
 * plugin really can always do what it did before: deploy using Resolver. This deployer always accepts deploy
 * requests and does whatever this plugin was doing with them before.
 *
 * @since 3.2.0
 */
@Singleton
@Named
@Priority(Integer.MIN_VALUE + 1)
public class FallbackDeployerSPI implements DeployerSPI {
    public static final String RETRY_FAILED_DEPLOYMENT_COUNT = "retryFailedDeploymentCount";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RepositorySystem repositorySystem;

    @Inject
    public FallbackDeployerSPI(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public boolean deploy(RepositorySystemSession session, DeployRequest deployRequest) throws DeploymentException {
        int retryFailedDeploymentCounter =
                Math.max(1, Math.min(10, (Integer) session.getData().get(RETRY_FAILED_DEPLOYMENT_COUNT)));
        DeploymentException exception = null;
        for (int count = 0; count < retryFailedDeploymentCounter; count++) {
            try {
                if (count > 0) {
                    logger.info("Retrying deployment attempt " + (count + 1) + " of " + retryFailedDeploymentCounter);
                }

                repositorySystem.deploy(session, deployRequest);
                exception = null;
                break;
            } catch (DeploymentException e) {
                if (count + 1 < retryFailedDeploymentCounter) {
                    logger.warn("Encountered issue during deployment: {}", e.getLocalizedMessage());
                    logger.debug("", e);
                }
                if (exception == null) {
                    exception = e;
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
        return true;
    }
}
