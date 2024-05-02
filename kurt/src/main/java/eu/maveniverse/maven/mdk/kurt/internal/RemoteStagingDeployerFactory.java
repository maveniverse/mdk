package eu.maveniverse.maven.mdk.kurt.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mdk.kurt.Deployer;
import eu.maveniverse.maven.mdk.kurt.DeployerFactory;
import eu.maveniverse.maven.mdk.kurt.KurtConfig;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

@Singleton
@Named(RemoteStagingDeployerFactory.NAME)
public class RemoteStagingDeployerFactory implements DeployerFactory {
    public static final String NAME = "remote-staging";

    private final RepositorySystem repositorySystem;

    @Inject
    public RemoteStagingDeployerFactory(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public Deployer createDeployer(MavenSession session) {
        String id = KurtConfig.REMOTE_STAGING_ID.require(session);
        String url = KurtConfig.REMOTE_STAGING_URL.require(session);
        RemoteRepository stagingRepository = repositorySystem.newDeploymentRepository(
                session.getRepositorySession(), new RemoteRepository.Builder(id, "default", url).build());
        return new StagingDeployer(NAME, repositorySystem, stagingRepository);
    }
}
