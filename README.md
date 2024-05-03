# MDK

To derail you learning about MDK, see https://en.wikipedia.org/wiki/MDK

## What is this about?

Maven has well groomed ecosystem of core and non-core plugins, but since long time it was a pain point when deployment
target (remote repo or service) needed some extra bit of "flow", like publishing to Central is.

The existing solutions all required to integrate some extra plugins into build, potentially violating Maven lifecycle
and even maybe working in non-future proof way. MDK aims to fix this.

The crux MDK tries to implement is following:
* let's assume you have a "proper project" that is working (like any ASF Maven project is)
* you want to deploy it to somewhere where some extra "flow" is needed (like Central, but ASF Nx2 as well)
* you would not want to add extra lines and config to POM, special cases about plugin inheritance, increase profile
  chaos and so on. You want something **simple**.

# What MDK does?

This experiment contains 3 modules:
* `maven-deploy-plugin-spi` is a small artifact that defines SPI used by maven-deploy-plugin
* `maven-deploy-plugin` is 1:1 copy of [maven-deploy-plugin](https://github.com/apache/maven-deploy-plugin) with all of it ITs
* `kurt` is SPI implementation and Maven extension in one
* `kurt-jreleaser` is Kurt extension and [JReleaser](https://jreleaser.org/) integration

The goal is ability to "take over" behaviour of `maven-deploy-plugin` with smallest interference into project itself.

# How MDK achieves this?

MDK implemented following changes: The `maven-deploy-plugin` is modified, to depend on `maven-deploy-plugin-spi`, and search for 
components implementing it. If it finds one (and always will), it simply passes the deployment request to it. The 
reason why "always will" is that plugins own (existing code) was refactored/moved out into one "fallback" component, 
and it resides within the plugin itself.

When the plugin executes, this happens: Maven resolves `maven-deploy-plugin` and hence its own `maven-deploy-plugin-spi` 
dependency will  be resolved as well and added to plugin classpath.

```txt
<m-deploy-p> <--depends-- <spi (resolved)>
```

Essentially, all works as today (very same feature set is preserved).

With MDK present, user does following change to `.mvn/extensions.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
  <extension>
    <groupId>eu.maveniverse.maven.mdk</groupId>
    <artifactId>kurt</artifactId>
    <version>0.0.1-SNAPSHOT</version>
  </extension>
</extensions>
```

(Note: if one would use JReleaser, then `kurt-jreleaser` is artifactId)

And Maven Core activates this extension. Following change happens (see [extension.xml](kurt/src/main/resources/META-INF/maven/extension.xml)).
* The extension is pulled into Maven Extension (along with dependencies)
* the `api` artifact becomes "Maven provided" (see `exportedArtifact`).

In essence, when MDK Extension is present in Maven, the plugin execution changes like this:

```txt
<m-deploy-p> <--depends-- <spi (provided from Maven)>
```

This ensures that there is one `spi` in system, hence there are no classloading issues. Next, MDK itself defines
`DeployerSPI` implementation, the `Kurt`, which at this moment "takes over" `maven-deploy-plugin` duties, the plugins
really becoming "just a messenger".

Kurt integrates into Maven Lifecycle, and provides following deployers:
* "resolver" -- is almost same as `maven-deploy-plugin` is (this is the default, ensures that Maven w/ Kurt installed but not configured behaves in same way as without it)
* "local-staging" -- stages all artifacts locally, into (by default) top level project `target/staging-deploy` directory.
* "remote-staging" -- stages all artifacts into (explicitly given) remote repository TBD

Kurt JReleaser extension adds more:
* "jreleaser-full-release" -- this combines "local-staging" and JReleaser "full-release" workflow, as JReleaser 
  documentation explains, but does not require POM changes.

In short, idea is to be the least intrusive and future-proof, while have access to always changing current (and 
possible future) services for Artifact publishing.

# JReleaser reuse

Frankly, MDK was inspired by upcoming Sonatype Central publishing changes (we already did not support Nx2 staging, but
now "Central Portal" came in picture as well). Also by the fact that JReleaser already provided solutions to all the
problems. Still, JReleases needs some [hoops and loops](https://jreleaser.org/guide/latest/examples/maven/index.html)
to make it work, and these all require non-trivial changes to POMs. The idea is that MDK could handle all this: 
it can "stage locally", and then just invoke JReleases pointing it locally staged repository, and it that "takes over" 
from that point. 

Another good improvement would be to use JReleaser "-sdk" solutions for use cases like:
* Enhance "remote-staging" to create a new staging repository, or point build at existing (already created) staging repository,
  basically making possible staging together artifacts coming from different builds (like different OSes)
