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

import eu.maveniverse.maven.mdk.kurt.deployers.ResolverDeployerFactory;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Kurt's configuration. It uses {@link RepositorySystemSession#getConfigProperties()} as these are covering whole
 * session life-span and there is no need for any dynamism. Once set, should remain same during the existence of
 * session.
 */
public final class KurtConfig {
    private final Supplier<String> defaultValueSupplier;
    private final String[] keys;

    public static KurtConfig createWithoutDefault(String... keys) {
        return new KurtConfig(null, keys);
    }

    public static KurtConfig createWithDefault(Supplier<String> defaultValueSupplier, String... keys) {
        return new KurtConfig(defaultValueSupplier, keys);
    }

    private KurtConfig(Supplier<String> defaultValueSupplier, String... keys) {
        this.defaultValueSupplier = defaultValueSupplier; // nullable
        this.keys = keys;
        if (keys.length < 1) {
            throw new IllegalArgumentException("At least one key must be provided");
        }
    }

    public String getOrDefault(Map<String, Object> map) {
        String result;
        for (String key : keys) {
            result = (String) map.get(key);
            if (result != null) {
                return result;
            }
        }
        return defaultValueSupplier != null ? defaultValueSupplier.get() : null;
    }

    public KurtConfig withDefault(Supplier<String> defSupplier) {
        return new KurtConfig(defSupplier, keys);
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
            createWithDefault(() -> ResolverDeployerFactory.NAME, KURT_PREFIX + "deployer");

    public static final KurtConfig LOCAL_STAGING_ID =
            createWithDefault(() -> "staging-deploy", KURT_PREFIX + "localStagingId");

    public static final KurtConfig LOCAL_STAGING_DIRECTORY =
            createWithDefault(() -> "staging-deploy", KURT_PREFIX + "localStagingDirectory");

    public static final KurtConfig REMOTE_STAGING_ID =
            createWithDefault(() -> "staging-deploy", KURT_PREFIX + "remoteStagingId");

    public static final KurtConfig REMOTE_STAGING_URL = createWithoutDefault(KURT_PREFIX + "remoteStagingUrl");
}
