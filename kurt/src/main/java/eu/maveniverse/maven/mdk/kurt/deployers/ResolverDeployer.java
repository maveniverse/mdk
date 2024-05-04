package eu.maveniverse.maven.mdk.kurt.deployers;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * "Ordinary" Resolver deployer, eats everything.
 */
public class ResolverDeployer extends DeployerSupport {
    private final RepositorySystem repositorySystem;

    public ResolverDeployer(RepositorySystem repositorySystem) {
        super(ResolverDeployerFactory.NAME);
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public boolean processRequest(MavenSession mavenSession, DeployRequest deployRequest) throws DeploymentException {
        return true;
    }

    @Override
    public void deployAll(MavenSession session, Map<RemoteRepository, DeployRequest> deployRequests)
            throws DeploymentException {
        for (DeployRequest dr : deployRequests.values()) {
            repositorySystem.deploy(session.getRepositorySession(), dr);
        }
    }
}
