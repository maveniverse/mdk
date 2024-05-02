package eu.maveniverse.maven.mdk.kurt.internal;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * "Ordinary" Resolver deployer, eats everything.
 */
public class ResolverDeployer extends DeployerSupport {
    private final RepositorySystem repositorySystem;
    private final boolean deployAtEnd;

    public ResolverDeployer(RepositorySystem repositorySystem, boolean deployAtEnd) {
        super(ResolverDeployerFactory.NAME);
        this.repositorySystem = requireNonNull(repositorySystem);
        this.deployAtEnd = deployAtEnd;
    }

    @Override
    public RequestStatus processRequest(MavenSession mavenSession, DeployRequest deployRequest)
            throws DeploymentException {
        if (deployAtEnd) {
            return RequestStatus.DELAYED;
        }
        doDeploy(mavenSession.getRepositorySession(), deployRequest);
        return RequestStatus.PROCESSED;
    }

    @Override
    public void processAll(MavenSession session, Map<RemoteRepository, DeployRequest> deployRequests)
            throws DeploymentException {
        for (DeployRequest dr : deployRequests.values()) {
            doDeploy(session.getRepositorySession(), dr);
        }
    }

    private void doDeploy(RepositorySystemSession session, DeployRequest deployRequest) throws DeploymentException {
        repositorySystem.deploy(session, deployRequest);
    }
}
