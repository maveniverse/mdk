/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
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
