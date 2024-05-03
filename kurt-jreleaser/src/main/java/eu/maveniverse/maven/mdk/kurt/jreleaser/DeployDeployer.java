package eu.maveniverse.maven.mdk.kurt.jreleaser;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mdk.kurt.deployers.DeployerSupport;
import eu.maveniverse.maven.mdk.kurt.deployers.LocalStagingDeployer;
import java.io.IOException;
import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.jreleaser.workflow.Workflows;

/**
 * A JReleaser deployer, that does not accept SNAPSHOT artifacts. It uses "deploy" workflow
 * of JReleaser. Reuses {@link LocalStagingDeployer} delegates all local staging thing to it,
 * and then takes over.
 */
public class DeployDeployer extends DeployerSupport {
    private final LocalStagingDeployer localStagingDeployer;
    private final JReleaserContextFactory contextFactory;

    public DeployDeployer(LocalStagingDeployer localStagingDeployer, JReleaserContextFactory contextFactory) {
        super(DeployDeployerFactory.NAME);
        this.localStagingDeployer = requireNonNull(localStagingDeployer);
        this.contextFactory = requireNonNull(contextFactory);
    }

    @Override
    public RequestStatus processRequest(MavenSession mavenSession, DeployRequest deployRequest) {
        return localStagingDeployer.processRequest(mavenSession, deployRequest);
    }

    @Override
    public void processAll(MavenSession session, Map<RemoteRepository, DeployRequest> deployRequests)
            throws DeploymentException, IOException {
        localStagingDeployer.processAll(session, deployRequests);
        Workflows.deploy(contextFactory.createContext(session, localStagingDeployer.getStagingDirectory()))
                .execute();
    }
}
