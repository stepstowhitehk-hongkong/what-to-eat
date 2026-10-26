# Gradle wrapper

The wrapper runtime in `gradle/wrapper/gradle-wrapper.jar` contains unmodified Gradle wrapper, CLI, locking and supporting utility classes extracted from the Gradle API 9.6.0 JAR bundled with this machine's Android Studio (`Contents/plugins/gradle-plugin/lib/gradle-api-9.6.0.jar`). It is a locally assembled wrapper runtime, rather than the byte-identical wrapper JAR normally produced by `gradle wrapper`. Its launcher/download path was executed successfully up to the environment's DNS failure. The application itself does not depend on this JAR at runtime.

The wrapper downloads and runs the **Gradle 9.4.1** distribution selected in `gradle-wrapper.properties`; the wrapper's own class version does not select the build version. Once Gradle is available, you can regenerate the standard wrapper files using `./gradlew wrapper --gradle-version 9.4.1 --distribution-type bin`.

Gradle is copyright its contributors and distributed under the Apache License, Version 2.0, included at `gradle/wrapper/LICENSE.txt`: https://www.apache.org/licenses/LICENSE-2.0 . Source: https://github.com/gradle/gradle . No Gradle classes were modified. The local archive assembly and small shell/Windows launchers were added for this project.
