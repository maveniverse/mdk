package eu.maveniverse.maven.mdk.kurt.jreleaser;

import static java.util.Objects.requireNonNull;
import static org.jreleaser.util.FileUtils.resolveOutputDirectory;

import eu.maveniverse.maven.mdk.kurt.Deployer;
import eu.maveniverse.maven.mdk.kurt.DeployerFactory;
import eu.maveniverse.maven.mdk.kurt.deployers.LocalStagingDeployerFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.ServiceLoader;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.jreleaser.config.JReleaserConfigParser;
import org.jreleaser.engine.context.ContextCreator;
import org.jreleaser.model.internal.JReleaserContext;
import org.jreleaser.util.Env;
import org.jreleaser.util.StringUtils;

@Singleton
@Named(FullReleaseDeployerFactory.NAME)
public class FullReleaseDeployerFactory implements DeployerFactory {
    public static final String NAME = "jreleaser-full-release";

    private final LocalStagingDeployerFactory localStagingDeployerFactory;
    protected File basedir;
    protected File configFile;
    protected Boolean dryRun;
    protected Boolean gitRootSearch;
    protected Boolean strict;
    protected Path outputDir;

    protected Path actualConfigFile;
    protected Path actualBasedir;

    @Inject
    public FullReleaseDeployerFactory(LocalStagingDeployerFactory localStagingDeployerFactory) {
        this.localStagingDeployerFactory = requireNonNull(localStagingDeployerFactory);
    }

    @Override
    public Deployer createDeployer(MavenSession session) {
        resolveBasedir();
        resolveConfigFile();

        JReleaserContext context = ContextCreator.create(
                null, // logger,
                resolveConfigurer(actualConfigFile),
                org.jreleaser.model.api.JReleaserContext.Mode.FULL,
                actualConfigFile,
                actualBasedir,
                resolveOutputDirectory(actualBasedir, outputDir, "build"),
                resolveBoolean(org.jreleaser.model.api.JReleaserContext.DRY_RUN, dryRun),
                resolveBoolean(org.jreleaser.model.api.JReleaserContext.GIT_ROOT_SEARCH, gitRootSearch),
                resolveBoolean(org.jreleaser.model.api.JReleaserContext.STRICT, strict),
                Collections.emptyList(),
                Collections.emptyList());
        return new FullReleaseDeployer(localStagingDeployerFactory.createDeployer(session), context);
    }

    protected JReleaserContext.Configurer resolveConfigurer(Path configFile) {
        switch (StringUtils.getFilenameExtension(configFile.getFileName().toString())) {
            case "yml":
            case "yaml":
                return JReleaserContext.Configurer.CLI_YAML;
            case "toml":
                return JReleaserContext.Configurer.CLI_TOML;
            case "json":
                return JReleaserContext.Configurer.CLI_JSON;
            default:
                // should not happen!
                throw new IllegalArgumentException("Invalid configuration format: " + configFile.getFileName());
        }
    }

    protected boolean resolveBoolean(String key, Boolean value) {
        if (null != value) return value;
        String resolvedValue = Env.resolve(key, "");
        return resolvedValue != null && !resolvedValue.trim().isEmpty() && Boolean.parseBoolean(resolvedValue);
    }

    private void resolveConfigFile() {
        if (null != configFile) {
            actualConfigFile = configFile.toPath();
        } else {
            ServiceLoader<JReleaserConfigParser> parsers =
                    ServiceLoader.load(JReleaserConfigParser.class, JReleaserConfigParser.class.getClassLoader());

            for (JReleaserConfigParser parser : parsers) {
                Path file = Paths.get(".").normalize().resolve("jreleaser." + parser.getPreferredFileExtension());
                if (Files.exists(file)) {
                    actualConfigFile = file;
                    break;
                }
            }
        }

        if (null == actualConfigFile || !Files.exists(actualConfigFile)) {
            throw new IllegalArgumentException(
                    "Missing required option 'configFile' " + "or local file named jreleaser["
                            + String.join("|", getSupportedConfigFormats())
                            + "]");
        }
    }

    private void resolveBasedir() {
        String resolvedBasedir =
                Env.resolve(org.jreleaser.model.api.JReleaserContext.BASEDIR, null != basedir ? basedir.getPath() : "");
        actualBasedir = ((resolvedBasedir != null && !resolvedBasedir.trim().isEmpty())
                        ? Paths.get(resolvedBasedir)
                        : actualConfigFile.toAbsolutePath().getParent())
                .normalize();
    }

    private Set<String> getSupportedConfigFormats() {
        Set<String> extensions = new LinkedHashSet<>();

        ServiceLoader<JReleaserConfigParser> parsers =
                ServiceLoader.load(JReleaserConfigParser.class, JReleaserConfigParser.class.getClassLoader());

        for (JReleaserConfigParser parser : parsers) {
            extensions.add("." + parser.getPreferredFileExtension());
        }

        return extensions;
    }
}
