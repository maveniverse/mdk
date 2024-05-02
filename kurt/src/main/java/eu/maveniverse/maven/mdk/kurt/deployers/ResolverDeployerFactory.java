package eu.maveniverse.maven.mdk.kurt.deployers;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mdk.kurt.DeployerFactory;
import eu.maveniverse.maven.mdk.kurt.KurtConfig;
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
    public ResolverDeployer createDeployer(MavenSession session) {
        return new ResolverDeployer(repositorySystem, Boolean.parseBoolean(KurtConfig.DEPLOY_AT_END.require(session)));
    }
}
