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
