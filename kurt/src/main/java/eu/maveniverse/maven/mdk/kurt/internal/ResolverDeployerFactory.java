package eu.maveniverse.maven.mdk.kurt.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mdk.kurt.Deployer;
import eu.maveniverse.maven.mdk.kurt.DeployerFactory;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;

@Singleton
@Named(ResolverDeployerFactory.NAME)
public class ResolverDeployerFactory implements DeployerFactory {
    public static final String NAME = "resolver";

    private final RepositorySystem repositorySystem;

    @Inject
    public ResolverDeployerFactory(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public Deployer createDeployer(MavenSession session) {
        boolean deployAtEnd = true;
        return new ResolverDeployer(repositorySystem, deployAtEnd);
    }
}
