package eu.maveniverse.maven.mdk.kurt.deployers;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * A "local staging" deployer that does not accept SNAPSHOT artifacts.
 */
public class LocalStagingDeployer extends DeployerSupport {
    private final RepositorySystem repositorySystem;
    private final RemoteRepository stagingRepository;
    private final Path stagingDirectory;

    public LocalStagingDeployer(
            RepositorySystem repositorySystem, RemoteRepository stagingRepository, Path stagingDirectory) {
        super(LocalStagingDeployerFactory.NAME);
        this.repositorySystem = requireNonNull(repositorySystem);
        this.stagingRepository = requireNonNull(stagingRepository);
        this.stagingDirectory = requireNonNull(stagingDirectory);
    }

    public Path getStagingDirectory() {
        return stagingDirectory;
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
        for (DeployRequest dr : deployRequests.values()) {
            DeployRequest stagingRequest = new DeployRequest();
            stagingRequest.setRepository(stagingRepository);
            stagingRequest.setArtifacts(dr.getArtifacts());
            stagingRequest.setMetadata(dr.getMetadata());
            repositorySystem.deploy(session.getRepositorySession(), stagingRequest);
        }
    }
}
