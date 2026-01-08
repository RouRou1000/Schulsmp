#!/bin/sh
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec java -Dmaven.multiModuleProjectDirectory="$here" -cp "$here/.mvn/wrapper/maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain "$@"
