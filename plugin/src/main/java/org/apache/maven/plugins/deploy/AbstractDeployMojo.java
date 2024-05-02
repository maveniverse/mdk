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

import java.io.IOException;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.deploy.spi.DeployerSPI;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;

/**
 * Abstract class for Deploy mojo's.
 */
public abstract class AbstractDeployMojo extends AbstractMojo {
    /**
     * Flag whether Maven is currently in online/offline mode.
     */
    @Parameter(defaultValue = "${settings.offline}", readonly = true)
    private boolean offline;

    /**
     * Parameter used to control how many times a failed deployment will be retried before giving up and failing. If a
     * value outside the range 1-10 is specified it will be pulled to the nearest value within the range 1-10.
     *
     * @since 2.7
     */
    @Parameter(property = "retryFailedDeploymentCount", defaultValue = "1")
    private int retryFailedDeploymentCount;

    @Component
    private RuntimeInformation runtimeInformation;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Inject
    private List<DeployerSPI> deployers;

    private static final String AFFECTED_MAVEN_PACKAGING = "maven-plugin";

    private static final String FIXED_MAVEN_VERSION = "3.9.0";

    /* Setters and Getters */

    void failIfOffline() throws MojoFailureException {
        if (offline) {
            throw new MojoFailureException("Cannot deploy artifacts when Maven is in offline mode");
        }
    }

    /**
     * If this plugin used in pre-3.9.0 Maven, the packaging {@code maven-plugin} will not deploy G level metadata.
     */
    protected void warnIfAffectedPackagingAndMaven(final String packaging) {
        if (AFFECTED_MAVEN_PACKAGING.equals(packaging)) {
            try {
                GenericVersionScheme versionScheme = new GenericVersionScheme();
                Version fixedMavenVersion = versionScheme.parseVersion(FIXED_MAVEN_VERSION);
                Version currentMavenVersion = versionScheme.parseVersion(runtimeInformation.getMavenVersion());
                if (fixedMavenVersion.compareTo(currentMavenVersion) > 0) {
                    getLog().warn("");
                    getLog().warn("You are about to deploy a maven-plugin using Maven " + currentMavenVersion + ".");
                    getLog().warn("This plugin should be used ONLY with Maven 3.9.0 and newer, as MNG-7055");
                    getLog().warn("is fixed in those versions of Maven only!");
                    getLog().warn("");
                }
            } catch (InvalidVersionSpecificationException e) {
                // skip it: Generic does not throw, only API contains this exception
            }
        }
    }

    /**
     * Creates resolver {@link RemoteRepository} equipped with needed whistles and bells.
     */
    protected RemoteRepository getRemoteRepository(final String repositoryId, final String url) {
        // TODO: RepositorySystem#newDeploymentRepository does this very same thing!
        RemoteRepository result = new RemoteRepository.Builder(repositoryId, "default", url).build();

        if (result.getAuthentication() == null || result.getProxy() == null) {
            RemoteRepository.Builder builder = new RemoteRepository.Builder(result);

            if (result.getAuthentication() == null) {
                builder.setAuthentication(session.getRepositorySession()
                        .getAuthenticationSelector()
                        .getAuthentication(result));
            }

            if (result.getProxy() == null) {
                builder.setProxy(
                        session.getRepositorySession().getProxySelector().getProxy(result));
            }

            result = builder.build();
        }

        return result;
    }

    protected void deploy(DeployRequest deployRequest) throws MojoExecutionException {
        try {
            RepositorySystemSession repositorySystemSession = session.getRepositorySession();
            repositorySystemSession
                    .getData()
                    .set(FallbackDeployerSPI.RETRY_FAILED_DEPLOYMENT_COUNT, retryFailedDeploymentCount);
            boolean accepted = false;
            for (DeployerSPI deployerSPI : deployers) {
                accepted = deployerSPI.deploy(session.getRepositorySession(), deployRequest);
                if (accepted) {
                    break;
                }
            }
            if (!accepted) {
                throw new MojoExecutionException("No deployer SPI accepted the deploy: failed to deploy");
            }
        } catch (DeploymentException | IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
