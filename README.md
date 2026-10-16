## Start here

This is an empty Java project scaffold: no Java classes or tests are included. Maven's conventional src/main/java,
src/main/resources, src/test/java, and src/test/resources folders are retained with .gitkeep files.

The Java compiler target and build dependencies are defined in [pom.xml](pom.xml). No application framework or
test-library dependency is selected.

Read [docs/README.md](docs/README.md) for the AI setup map. Standing rules live in [CLAUDE.md](CLAUDE.md), with task skills,
agent definitions, shared references, and project guides linked from that map.

Build gates, profile overrides, and IDE setup live in [development setup](docs/development.md).

## Working commands

- `./mvnw verify`: build and run all checks currently configured in pom.xml.
- `git diff`: inspect tracked changes before committing.

An empty-build success verifies tooling, not application behavior or tests. Do not add a framework, database, or
application code until a feature needs it.
