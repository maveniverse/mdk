package eu.maveniverse.maven.mdk.kurt.jreleaser;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mdk.kurt.Deployer;
import eu.maveniverse.maven.mdk.kurt.DeployerFactory;
import eu.maveniverse.maven.mdk.kurt.deployers.LocalStagingDeployer;
import eu.maveniverse.maven.mdk.kurt.deployers.LocalStagingDeployerFactory;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;

@Singleton
@Named(DeployDeployerFactory.NAME)
public class DeployDeployerFactory implements DeployerFactory {
    public static final String NAME = "jreleaser-deploy";

    private final LocalStagingDeployerFactory localStagingDeployerFactory;
    private final JReleaserContextFactory contextFactory;

    @Inject
    public DeployDeployerFactory(
            LocalStagingDeployerFactory localStagingDeployerFactory, JReleaserContextFactory contextFactory) {
        this.localStagingDeployerFactory = requireNonNull(localStagingDeployerFactory);
        this.contextFactory = requireNonNull(contextFactory);
    }

    @Override
    public Deployer createDeployer(MavenSession session) {
        LocalStagingDeployer localStagingDeployer = localStagingDeployerFactory.createDeployer(session);
        return new DeployDeployer(
                localStagingDeployer,
                contextFactory.createContext(session, localStagingDeployer.getLocalStagingDirectory()));
    }
}
