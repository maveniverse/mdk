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

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Kurt configuration.
 */
public class KurtConfig {
    private final String description;
    private final String defaultValue;
    private final String[] keys;

    private KurtConfig(String description, String defaultValue, String... keys) {
        this.description = requireNonNull(description);
        this.defaultValue = defaultValue; // nullable
        this.keys = keys;
        if (keys.length < 1) {
            throw new IllegalArgumentException("At least one key must be provided");
        }
    }

    public String getDescription() {
        return description;
    }

    /**
     * Gets value or return default value. May return {@code null} (if no value present and default is {@code null}).
     */
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

    public String require(Map<String, Object> map) {

    }

    public static final String KURT_PREFIX = "kurt.";
    public static final String KURT_DEPLOYER = KURT_PREFIX + "deployer";
}
