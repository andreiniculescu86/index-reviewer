# JUnit and Mockito preferences

For new Java test setup, prefer JUnit Jupiter, AssertJ and Mockito. This preference does not replace an existing stack.

For JUnit 5 parameterized tests, prefer @MethodSource over @CsvSource. Give every case a description through
@ParameterizedTest(name = ...). Use Arguments.argumentSet("description", ...) with {argumentSetName}; These API choices
apply only to the selected version.

Prefer MockitoExtension for plain JUnit/Mockito tests when available. If manual openMocks initialization is necessary,
close its returned resource through the test lifecycle. Prefer public interfaces over framework implementation classes
when mocking.
