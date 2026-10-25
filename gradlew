#!/usr/bin/env sh

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
APP_HOME=${APP_HOME%/}

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ ! -f "$CLASSPATH" ]; then
  echo "Gradle wrapper JAR not found at $CLASSPATH" >&2
  echo "Run 'gradle wrapper' once in a Gradle-enabled environment or download the wrapper jar." >&2
  exit 1
fi

JAVACMD=${JAVACMD:-java}
exec "$JAVACMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
