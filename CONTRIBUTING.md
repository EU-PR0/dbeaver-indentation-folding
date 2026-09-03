# Contributing

1. Fork the repository.
2. Use JDK 21 and Maven 3.9+.
3. Run `mvn -B clean verify`.
4. Keep DBeaver bundle ranges narrow unless the new range has been verified.
5. Do not copy DBeaver implementation code into this plug-in; use public/accessible extension APIs and OSGi dependencies.
6. Open a pull request with a short compatibility note.

For DBeaver-version compatibility changes, include the tested DBeaver version and the relevant bundle versions.
