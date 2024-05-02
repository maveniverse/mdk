package eu.maveniverse.maven.mdk.kurt.jreleaser;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mdk.kurt.KurtConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.jreleaser.config.JReleaserConfigLoader;
import org.jreleaser.engine.context.ContextCreator;
import org.jreleaser.logging.SimpleJReleaserLoggerAdapter;
import org.jreleaser.model.Active;
import org.jreleaser.model.internal.JReleaserContext;
import org.jreleaser.model.internal.JReleaserModel;
import org.jreleaser.model.internal.deploy.maven.MavenCentralMavenDeployer;
import org.jreleaser.model.internal.deploy.maven.Nexus2MavenDeployer;
import org.jreleaser.model.internal.release.GithubReleaser;

@Singleton
@Named
public class JReleaserContextFactory {
    private static final String JRELEASER_PREFIX = "jreleaser.";

    public static final KurtConfig JRELEASER_CONFIG_FILE =
            KurtConfig.create("configFile", ".mvn/jreleaser.yml", JRELEASER_PREFIX + "configFile");
    public static final KurtConfig JRELEASER_OUTPUT_DIRECTORY =
            KurtConfig.create("outputDirectory", "jreleaser", JRELEASER_PREFIX + "outputDirectory");
    public static final KurtConfig JRELEASER_DRY_RUN =
            KurtConfig.create("dryRun", Boolean.FALSE.toString(), JRELEASER_PREFIX + "dryRun");
    public static final KurtConfig JRELEASER_GIT_ROOT_SEARCH =
            KurtConfig.create("gitRootSearch", Boolean.FALSE.toString(), JRELEASER_PREFIX + "gitRootSearch");
    public static final KurtConfig JRELEASER_STRICT =
            KurtConfig.create("strict", Boolean.FALSE.toString(), JRELEASER_PREFIX + "strict");
    public static final KurtConfig JRELEASER_APPLY_MAVEN_CENTRAL_RULES = KurtConfig.create(
            "applyMavenCentralRules", Boolean.FALSE.toString(), JRELEASER_PREFIX + "applyMavenCentralRules");
    public static final KurtConfig JRELEASER_TARGET = KurtConfig.create("target", null, JRELEASER_PREFIX + "target");
    public static final KurtConfig JRELEASER_PROFILE_ID =
            KurtConfig.create("profileId", null, JRELEASER_PREFIX + "profileId");

    public JReleaserContext createContext(MavenSession session, Path stagingDirectory) {
        String target = JRELEASER_TARGET.require(session);

        String service;
        String url;
        // TODO: externalize config
        switch (target) {
            case "asf-repository": {
                service = "nx2";
                url = "https://repository.apache.org/service/local";
                break;
            }
            case "sonatype-oss": {
                service = "nx2";
                url = "https://oss.sonatype.org/service/local";
                break;
            }
            case "sonatype-s01": {
                service = "nx2";
                url = "https://s01.oss.sonatype.org/service/local";
                break;
            }
            case "sonatype-maven-central": {
                service = "central";
                url = "https://central.sonatype.com/api/v1/publisher";
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown target: " + target);
            }
        }

        Path configFile = session.getTopLevelProject()
                .getBasedir()
                .toPath()
                .resolve(JRELEASER_CONFIG_FILE.require(session))
                .toAbsolutePath();
        Path outputDirectory = Paths.get(session.getTopLevelProject().getBuild().getDirectory())
                .resolve(JRELEASER_OUTPUT_DIRECTORY.require(session))
                .toAbsolutePath();

        JReleaserModel model;
        if (Files.isRegularFile(configFile)) {
            model = JReleaserConfigLoader.loadConfig(configFile);
        } else {
            model = new JReleaserModel();
            model.getProject().getJava().setGroupId(session.getTopLevelProject().getGroupId());
            model.getProject()
                    .getJava()
                    .setArtifactId(session.getTopLevelProject().getArtifactId());
            model.getProject().getJava().setVersion(session.getTopLevelProject().getVersion());
            model.getProject().setVersion(session.getTopLevelProject().getVersion());

            model.getSigning().setActive(Active.NEVER);
            GithubReleaser githubReleaser = new GithubReleaser();
            githubReleaser.setSkipRelease(true);
            githubReleaser.setToken("fake");
            model.getRelease().setGithub(githubReleaser);

            if ("nx2".equals(service)) {
                Nexus2MavenDeployer nexus2MavenDeployer = new Nexus2MavenDeployer();
                nexus2MavenDeployer.setName("maven-central");
                nexus2MavenDeployer.setActive(Active.ALWAYS);
                nexus2MavenDeployer.setUrl(requireNonNull(url));
                nexus2MavenDeployer.setCloseRepository(true);
                nexus2MavenDeployer.setReleaseRepository(false);
                nexus2MavenDeployer.setApplyMavenCentralRules(
                        Boolean.parseBoolean(JRELEASER_APPLY_MAVEN_CENTRAL_RULES.require(session)));
                nexus2MavenDeployer.setStagingProfileId(JRELEASER_PROFILE_ID.getOrDefault(session));
                nexus2MavenDeployer.setSign(false);
                nexus2MavenDeployer.setStagingRepositories(Collections.singletonList(stagingDirectory.toString()));

                model.getDeploy().getMaven().addNexus2(nexus2MavenDeployer);
            } else {
                MavenCentralMavenDeployer mavenCentralMavenDeployer = new MavenCentralMavenDeployer();
                mavenCentralMavenDeployer.setName("maven-central");
                mavenCentralMavenDeployer.setActive(Active.ALWAYS);
                mavenCentralMavenDeployer.setUrl(requireNonNull(url));
                mavenCentralMavenDeployer.setApplyMavenCentralRules(
                        Boolean.parseBoolean(JRELEASER_APPLY_MAVEN_CENTRAL_RULES.require(session)));
                mavenCentralMavenDeployer.setSign(false);
                mavenCentralMavenDeployer.setStagingRepositories(
                        Collections.singletonList(stagingDirectory.toString()));

                model.getDeploy().getMaven().addMavenCentral(mavenCentralMavenDeployer);
            }
        }

        return ContextCreator.create(
                new SimpleJReleaserLoggerAdapter(),
                JReleaserContext.Configurer.MAVEN,
                org.jreleaser.model.api.JReleaserContext.Mode.FULL,
                model,
                session.getTopLevelProject().getBasedir().toPath(),
                outputDirectory,
                Boolean.parseBoolean(JRELEASER_DRY_RUN.require(session)),
                Boolean.parseBoolean(JRELEASER_GIT_ROOT_SEARCH.require(session)),
                Boolean.parseBoolean(JRELEASER_STRICT.require(session)),
                Collections.emptyList(),
                Collections.emptyList());
    }
}
