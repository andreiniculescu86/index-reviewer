# Spring testing preferences

Avoid starting a Spring context when the tested behavior does not require Spring.

For Spring MVC controllers, use MockMvc to verify relevant request binding, validation, status and response contracts.
Choose standalone setup or a Spring test slice according to the behavior under test. Verify rejected requests do not
invoke business operations when that is the contract. Keep routine request/response printing disabled; use sanitized
diagnostic output when needed.

Start a full application context only when integration behavior requires it. Coordinate container lifetime with Spring
context lifetime so cached contexts never reference stopped containers. For Spring Boot, prefer service connections
supported by the selected version and container type; otherwise use dynamic test properties. Use supported lifecycle
management and avoid copying service-specific settings or machine-specific JVM flags from examples.
