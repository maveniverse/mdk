# MDK

To derail you want MDK is, see https://en.wikipedia.org/wiki/MDK

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
* `plugin` is 1:1 copy of [maven-deploy-plugin](https://github.com/apache/maven-deploy-plugin) with all of it ITs
* `api` is a small API artifact that defines SPI for m-deploy-p
* `kurt` is one SPI implementation

The goal is ability to "take over" behaviour of `maven-deploy-plugin` with smallest interference into project itself.

# How MDK achieves this?

MDK implemented following changes: The `maven-deploy-plugin` is modified, to depend on `api`, and search for 
components implementing it. If it finds one (and always will), it simply passes the deployment request to it. The 
reason why "always will" is that plugins own (existing code) was refactored/moved out into one "fallback" component, 
and it resides within the plugin itself.

When the plugin executes, this happens: Maven resolves `maven-deploy-plugin` and hence its own `api` dependency will
be resolved as well and added to plugin classpath.

```txt
<m-deploy-p> <--depends-- <api (resolved)>
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

And Maven Core activates this extension. Following change happens (see [extension.xml](kurt/src/main/resources/META-INF/maven/extension.xml)).
* The extension is pulled into Maven Extension (along with dependencies)
* the `api` artifact becomes "core provided" (see `exportedArtifact`).

In essence, when MDK Extension is present in Maven, the plugin execution changes like this:

```txt
<m-deploy-p> <--depends-- <api (provided from extension)>
```

This ensures that there is one `api` in system, hence there are no classloading issues. Next, MDK itself defines
`DeployerSPI` implementation, the `Kurt`, which at this moment "takes over" `maven-deploy-plugin` duties, the plugins
really becoming "just a messenger".

Kurt integrates into Maven Lifecycle, and provides following deployers:
* "resolver" -- is almost same as `maven-deploy-plugin` is
* "local-staging" -- stages all artifacts locally, into (by default) top level project `target/staging-deploy` directory.
* "remote-staging" -- stages all artifacts into (explicitly given) remote repository TBD
* "jreleaser" -- this will combine "local-staging" and JReleaser "full-release" workflow TBD

In short, idea is to be least intrusive and future-proof, while have access to always changing current (and 
possible future) services for Artifact publishing.