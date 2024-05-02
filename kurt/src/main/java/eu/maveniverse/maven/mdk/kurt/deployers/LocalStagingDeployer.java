package eu.maveniverse.maven.mdk.kurt.deployers;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mdk.kurt.KurtConfig;
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
    private final Path localStagingDirectory;

    public LocalStagingDeployer(RepositorySystem repositorySystem, Path localStagingDirectory) {
        super(LocalStagingDeployerFactory.NAME);
        this.repositorySystem = requireNonNull(repositorySystem);
        this.localStagingDirectory = requireNonNull(localStagingDirectory);
    }

    public Path getLocalStagingDirectory() {
        return localStagingDirectory;
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
        RemoteRepository stagingRepository = repositorySystem.newDeploymentRepository(
                session.getRepositorySession(),
                new RemoteRepository.Builder(
                                KurtConfig.LOCAL_STAGING_ID.require(session),
                                "default",
                                localStagingDirectory.toFile().toURI().toASCIIString())
                        .build());

        for (DeployRequest dr : deployRequests.values()) {
            DeployRequest stagingRequest = new DeployRequest();
            stagingRequest.setRepository(stagingRepository);
            stagingRequest.setArtifacts(dr.getArtifacts());
            stagingRequest.setMetadata(dr.getMetadata());
            repositorySystem.deploy(session.getRepositorySession(), stagingRequest);
        }
    }
}
