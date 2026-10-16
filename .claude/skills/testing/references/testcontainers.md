# Testcontainers preferences

Use Testcontainers for integration tests requiring containerizable infrastructure, such as databases or brokers.
Exercise the real adapter against that infrastructure. Pin images compatible with the intended deployment.
Use container-provided hosts and dynamically mapped ports, with connection settings injected through test
configuration. Do not assume localhost or fixed host ports. Isolate test data and make cleanup deterministic.

Missing container infrastructure is a blocker to report, not a reason to silently substitute mocks or claim integration
coverage. Required modules and framework support must match the selected stack.
