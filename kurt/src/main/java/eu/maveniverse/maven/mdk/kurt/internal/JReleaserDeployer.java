package eu.maveniverse.maven.mdk.kurt.internal;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.jreleaser.model.internal.JReleaserContext;
import org.jreleaser.workflow.Workflows;

/**
 * A JReleaser deployer, that does not accept SNAPSHOT artifacts. It uses "full-release" workflow
 * of JReleaser.
 */
public class JReleaserDeployer extends DeployerSupport {
    private final JReleaserContext context;

    public JReleaserDeployer(JReleaserContext context) {
        super(JReleaserDeployerFactory.NAME);
        this.context = requireNonNull(context);
    }

    @Override
    public RequestStatus processRequest(MavenSession mavenSession, DeployRequest deployRequest) {
        if (deployRequest.getArtifacts().stream().anyMatch(Artifact::isSnapshot)) {
            return RequestStatus.REFUSED;
        }
        return RequestStatus.DELAYED;
    }

    @Override
    public void processAll(MavenSession session, Map<RemoteRepository, DeployRequest> deployRequests)
            throws DeploymentException {
        Workflows.fullRelease(context).execute();
    }
}
