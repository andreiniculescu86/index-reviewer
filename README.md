## Start here

TODO add instructions to setup and run the project.

!!!  TODO  !!!  add instructions also how to run this via the cmd line with file upload

++ add details in readme that SWAGGER is available at /swagger-ui.html

## Working commands

- `./mvnw verify`: build and run all checks currently configured in pom.xml.

## Future development

!!!  TODO -- change  !!!!

Currently the project reads the data from the files in memory, everything at once, before starting the process. This is
due to the way the data is spread across the 3 files.
If the files and the universe are huge, then this way of receving the data is not efficient, both for parallelization
and
memory usage.
Ideally, data should be accessed from a database or via APIs, of if files are kept, these have to be set up in a way so
that the file rows can be read one by one, each row containing the required data not to require the read of the entire
file in memory.

Important, a must, aspect missing for production is application Observability - Implement OpenTelemetry to monitor the
application (traces, metrics, logs), in a distributed system.

## Documentation and Agentic setup

The project was developed with the aid of Claude. The setup files (agents, skill, plugins) are kept on purpose in this
project to provide clarity on what and how the agent setup was done.

Read the [docs/README-AI.md](docs/README-AI.md) for the Agentic setup map.
Standing prompt/rules live in [CLAUDE.md](CLAUDE.md), with task skills, agent definitions, shared references, and
project guides linked from that map.

Project documentation is present in the [docs](docs) directory, in .md files.

- [Methodology](docs/methodology.md): dates, FFMCAP, ranking with ties, buffer selection, capping and rulebook
  assumptions.
- [Data validation](docs/data-validation.md): errors and warnings of the review data checks in the service layer.
- [Traceability](docs/traceability-and-auditability.md): traceability and audit of stored review executions, with
  examples.