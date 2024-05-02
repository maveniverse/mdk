package eu.maveniverse.maven.mdk.kurt.deployers;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mdk.kurt.Deployer;
import eu.maveniverse.maven.mdk.kurt.DeployerFactory;
import eu.maveniverse.maven.mdk.kurt.KurtConfig;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;

@Singleton
@Named(LocalStagingDeployerFactory.NAME)
public class LocalStagingDeployerFactory implements DeployerFactory {
    public static final String NAME = "local-staging";

    private final RepositorySystem repositorySystem;

    @Inject
    public LocalStagingDeployerFactory(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public Deployer createDeployer(MavenSession session) {
        Path target = Paths.get(session.getTopLevelProject().getBuild().getDirectory());
        Path staging = target.resolve(KurtConfig.LOCAL_STAGING_DIRECTORY.require(session));
        return new LocalStagingDeployer(repositorySystem, staging);
    }
}
