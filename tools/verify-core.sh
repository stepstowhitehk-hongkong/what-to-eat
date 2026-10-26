#!/bin/sh
# Run the real JUnit core tests with Android Studio's bundled compiler, without Gradle downloads.
# For all Android/Compose compilation and lint checks, use ./gradlew instead.
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
STUDIO_DIR=${ANDROID_STUDIO_HOME:-/Applications/Android Studio.app/Contents}
KOTLIN_LIB="$STUDIO_DIR/plugins/Kotlin/kotlinc/lib"
JAVA_COMMAND="$STUDIO_DIR/jbr/Contents/Home/bin/java"
JUNIT_JAR="$STUDIO_DIR/lib/junit4.jar"
HAMCREST_JAR="$STUDIO_DIR/lib/testFramework.jar"
if [ ! -x "$JAVA_COMMAND" ] || [ ! -f "$KOTLIN_LIB/kotlin-compiler.jar" ] || [ ! -f "$JUNIT_JAR" ] || [ ! -f "$HAMCREST_JAR" ]; then
    echo 'Bundled compiler/test libraries unavailable. Use ./gradlew :app:testDebugUnitTest.' >&2
    exit 1
fi
cd "$PROJECT_DIR"
mkdir -p .verification
"$JAVA_COMMAND" -cp "$KOTLIN_LIB/*" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -jvm-target 17 -classpath "$JUNIT_JAR" \
    app/src/main/java/com/whattoeat/app/Game.kt \
    app/src/main/java/com/whattoeat/app/WheelMath.kt \
    app/src/test/java/com/whattoeat/app/MatcherTest.kt \
    app/src/test/java/com/whattoeat/app/GameEngineTest.kt \
    app/src/test/java/com/whattoeat/app/WheelMathTest.kt \
    -d .verification/tests.jar
"$JAVA_COMMAND" -cp ".verification/tests.jar:$JUNIT_JAR:$HAMCREST_JAR:$KOTLIN_LIB/kotlin-stdlib.jar" \
    org.junit.runner.JUnitCore com.whattoeat.app.MatcherTest \
    com.whattoeat.app.GameEngineTest com.whattoeat.app.WheelMathTest
