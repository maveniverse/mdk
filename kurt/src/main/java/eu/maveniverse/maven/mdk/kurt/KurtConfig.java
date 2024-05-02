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
import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Kurt's configuration. It uses {@link RepositorySystemSession#getConfigProperties()} as these are covering whole
 * session life-span and there is no need for any dynamism. Once set, should remain same during the existence of
 * session.
 */
public class KurtConfig {
    private final String name;
    private final String defaultValue;
    private final String[] keys;

    private KurtConfig(String name, String defaultValue, String... keys) {
        this.name = requireNonNull(name);
        this.defaultValue = defaultValue; // nullable
        this.keys = keys;
        if (keys.length < 1) {
            throw new IllegalArgumentException("At least one key must be provided");
        }
    }

    public String getName() {
        return name;
    }

    public String getOrDefault(Map<String, Object> map) {
        String result;
        for (String key : keys) {
            result = (String) map.get(key);
            if (result != null) {
                return result;
            }
        }
        return defaultValue;
    }

    public String getOrDefault(RepositorySystemSession session) {
        return getOrDefault(session.getConfigProperties());
    }

    public String getOrDefault(MavenSession session) {
        return getOrDefault(session.getRepositorySession());
    }

    public String require(Map<String, Object> map) {
        String result = getOrDefault(map);
        if (result == null) {
            throw new IllegalArgumentException("Parameter " + keys[0] + " is required.");
        }
        return result;
    }

    public String require(RepositorySystemSession session) {
        return require(session.getConfigProperties());
    }

    public String require(MavenSession session) {
        return require(session.getRepositorySession());
    }

    private static final String KURT_PREFIX = "kurt.";

    public static final KurtConfig DEPLOYER =
            new KurtConfig("deployer", ResolverDeployerFactory.NAME, KURT_PREFIX + "deployer");

    public static final KurtConfig DEPLOY_AT_END =
            new KurtConfig("deployAtEnd", Boolean.TRUE.toString(), KURT_PREFIX + "deployAtEnd");

    public static final KurtConfig LOCAL_STAGING_ID =
            new KurtConfig("localStagingId", "staging-deploy", KURT_PREFIX + "localStagingId");

    public static final KurtConfig LOCAL_STAGING_DIRECTORY =
            new KurtConfig("localStagingDirectory", "staging-deploy", KURT_PREFIX + "localStagingDirectory");

    public static final KurtConfig REMOTE_STAGING_ID =
            new KurtConfig("remoteStagingId", "staging-deploy", KURT_PREFIX + "remoteStagingId");

    public static final KurtConfig REMOTE_STAGING_URL =
            new KurtConfig("remoteStagingUrl", null, KURT_PREFIX + "remoteStagingUrl");
}
