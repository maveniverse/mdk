/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mdk.kurt.deployers;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mdk.kurt.Deployer;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DeployerSupport implements Deployer {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final String name;

    protected DeployerSupport(String name) {
        this.name = requireNonNull(name, "name");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void close() throws IOException {}
}
