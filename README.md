# MDK

MDK, or "Maven Deployment Kit" is universal extension that is able to take over `maven-deploy-plugin` 
behaviour, wildly change it, and apply all the hoops and loops for you, to save you doing the same. 
Goal is the least intrusion and the least config with most flexibility.

MDK goals are defined below:

### MDK does not interfere

MDK does not try to outsmart user, in fact, it leaves user in full control. It means that general
assumption is that your project fulfills all the required criteria of the targeted deployment service.
For example, if you aim to deploy to Maven Central, MDK will never "magically" sign artifacts for you,
create required sources and javadoc artifacts for you. You most probably want to follow "best practices"
and have a setup (for example a "release" profile) that does all these for you. MDK handles deployment
requests of artifacts it receives from your build only, it does not "come up" with any missing
information. In short, MDK does not interfere nor tries to be "super smart". User is the one who 
is in control.

### MDK does not reinvent anything

MDK relies on Maven (and Resolver), and it does not aim to be ["Iznogoud"](https://en.wikipedia.org/wiki/Iznogoud)
with his phrase "I want to be Caliph instead of the Caliph". MDK stands aside and merely helps you to 
achieve your publishing goals to a supported service. Is built on "best and proven practices" that were 
already present in Maven ecosystem. Artifact creation should happen by your build (and corresponding 
Maven plugins like `maven-jar-plugin`, `maven-source-plugin` and `maven-javadoc-plugin` are) and same
stands for signing (using `maven-gpg-plugin` or alike). Checksums are created by [Resolver](https://maven.apache.org/resolver/about-checksums.html), 
as configured by user. Everything happens as user would expect, there is no alternate universe to 
configure from the scratch.

### MDK herds toward "best practices"

Out of the box Maven always provided "interleaved deploy" (that happens each time per module built). Later
with introduction of parallel builds, but also the realization that this kind of deploy leaves "partial deploys"
in case of some module failure (for example UT), showed that "deploy at end" is actually the best practice:
deploy only when you are sure, that you have everything successfully prepared. Some extensions, like Takari Lifecycle did
implement this feature, and later even `maven-deploy-plugin` got it. But latter is still able to deploy only
"at moment last module using `maven-deploy-plugin` runs", which is technically still not "the end" of the
build.

## What is this about?

Maven has well groomed ecosystem of core and non-core plugins, but since long time it was a pain point when deployment
target (remote repo or service) needed some extra bit of "flow", like publishing to Central is.

The existing solutions all required to integrate some extra plugins into build, potentially violating Maven lifecycle
and even maybe working in non-future proof way. MDK aims to fix this.

The crux MDK tries to implement is following:
* let's assume you have a "proper project" that is working and is fully set up for release (like any ASF Maven project is)
* you want to deploy it to somewhere where some extra "flow" is needed (like Central, but ASF Nx2 or any other service)
* you **would not want** to add extra lines and config to POM, special cases about plugin inheritance, increase profile
  chaos and so on. You want something **simple**.

# What MDK does?

This experiment contains 3 modules:
* `maven-deploy-plugin-spi` is a small artifact that defines SPI used by maven-deploy-plugin
* `maven-deploy-plugin` is 1:1 copy of [maven-deploy-plugin](https://github.com/apache/maven-deploy-plugin)
* `kurt` is SPI implementation and Maven extension in one
* `kurt-jreleaser` is Kurt extension and [JReleaser](https://jreleaser.org/) integration

The goal is ability to "take over" behaviour of `maven-deploy-plugin` with smallest interference into project itself.

# How MDK achieves this?

MDK applies pattern described here: https://cwiki.apache.org/confluence/display/MAVEN/Maven+Plugin+SPI

MDK implemented following changes: The `maven-deploy-plugin` is changed, to use `maven-deploy-plugin-spi`, and search for 
components implementing it. If it finds one (and always will), it simply passes the deployment request to it. The 
reason why "always will" is that plugins own (existing code) was refactored/moved out into one "fallback" component, 
and it resides within the plugin itself. So when plugin loaded, there is always at least 1 SPI implementation present.

At runtime this happens: Maven resolves `maven-deploy-plugin` and hence its `maven-deploy-plugin-spi` 
dependency will be resolved as well:

```txt
<m-deploy-p> <--depends-- <spi (resolved)>
```

Essentially, all works as today (and very same plugin feature set is preserved).

To add MDK to the picture, do following change to `.mvn/extensions.xml`:

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

Maven Core activates this extension, and following change happens (see [extension.xml](kurt/src/main/resources/META-INF/maven/extension.xml)).
* Kurt is pulled into Maven Extension (along with dependencies)
* `maven-deploy-plugin-spi` artifact becomes "Maven provided" (see `exportedArtifact`).

In essence, when MDK Extension is present in Maven, the plugin execution changes like this:

```txt
<m-deploy-p> <--depends-- <spi (provided from Maven)>
```

This ensures that there is one SPI in system, hence there are no classloading issues. Next, MDK itself defines
`DeployerSPI` implementation, the `Kurt`, which at this moment "takes over" `maven-deploy-plugin` duties, the plugins
really becoming "just a messenger".

Kurt integrates into Maven Lifecycle, and provides following deployers:
* "resolver" -- is almost same as `maven-deploy-plugin` is (this is the default, ensures that Maven w/ Kurt installed but not configured behave in same way as without Kurt)
* "local-staging" -- stages all artifacts locally, into (by default) top level project `target/staging-deploy` directory.
* "remote-staging" -- stages all artifacts into (explicitly given) remote repository.

Kurt-JReleaser extension adds more:
* "jreleaser-full-release" -- this combines "local-staging" and JReleaser "full-release" workflow.
* "jreleaser-deploy" -- this combines "local-staging" and JReleaser "deploy" workflow.

In short, idea is to be the least intrusive and future-proof, while have access to always changing current (and 
possible future) services for Artifact publishing.

By default, when MDK is installed, but NOTHING of it is configured, user ends up with plain "deploy at end" behaviour.
MDK tries to provide sensible defaults, but also enforce "best practices", as interleaved deploy should
be really abandoned in favor or more correct "deploy at end".

# JReleaser integration example

MDK was inspired by upcoming Sonatype Central publishing changes (we already did not support Nx2 staging, but
now "Central Portal" came in picture as well that wildly changes things. Nx2 still supported use of 
`maven-deploy-plugin` but portal does not). The fact that JReleaser provided some solutions to the publishing
problem, it became "good candidate" to integrate into MDK as a "showcase" of what becomes possible with MDK. 
Still, use of JReleases alone needs some [hoops and loops](https://jreleaser.org/guide/latest/examples/maven/index.html)
to make it work, and these all require non-trivial changes to POMs. The idea was to showcase that MDK can handle all this: 
it can "stage locally", and then just invoke JReleaser and let it "take over" from that point. 

# Summary

Sadly, the publishing services and how they work is wildly different, and Maven and Maven Deploy Plugin cannot fully
cover all the cases. Moreover, as history shows, users are many times baffled with all the changes they need to add
to their POMs to publish their artifacts to this or that remote service.

MDK allows any project to be published by:
* not doing any POM change
* still executing only `mvn deploy` to deploy
* build still fails (as expected) in case of publishing problem, or if build passed, you are sure publishing succeeded as well
* makes project build and publishing endpoint disconnected, one could even "publish hopping" (a la distro-hopping) without
  any change to project

# I want to try it out!

Is simple:
* check out this repository, and build/install it (`mvn clean install`)
* take any project of yours that can be released to Central, and
* replace `maven-deploy-plugin` version from version you use to `3.1.3-mdk-SNAPSHOT` (like in parent POM, pluginMgmt)
* install extension like this and configure it https://gist.github.com/cstamas/19fe81319139f04cee3ea3b63090b7da
  (note: `jreleaser.target` can be one of these `sonatype-oss`, `sonatype-s01`, use for whichever service you have credentials)
* execute the project like this (make sure is release version, and you have all signatures/javadoc/sources in place, enforcer is needed only if you enforce non-snapshot plugins): `JRELEASER_NEXUS2_USERNAME=***** JRELEASER_NEXUS2_PASSWORD=***** mvn clean deploy -P my-release -Denforcer.skip`

# Example run

I took this project https://github.com/maveniverse/mima that is already published on Central and
* locally "tricked" it to be release (`mvn versions:set` to some release version)
* applied changes (this project uses Sonatype S01) https://gist.github.com/cstamas/19fe81319139f04cee3ea3b63090b7da
* in parent POM changed `maven-deploy-plugin` version to `3.1.3-mdk-SNAPSHOT` (hence enforcer skip is needed as plugin is SNAPSHOT)
* invoked Maven as https://gist.github.com/cstamas/322c11599a03d04f6eebfb7d036343ae

# FAQ

* Why `Kurt`? It comes from [MDK](https://maxdockurtmdk.fandom.com/wiki/Kurt_Hectic)
