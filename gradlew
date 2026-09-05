#!/bin/sh
# 极简 gradlew：直接调用 gradle wrapper（需要 JAVA_HOME 或 java 在 PATH 中）
# 使用方式与官方 gradlew 一致：./gradlew assembleDebug
DIR="$(cd "$(dirname "$0")" && pwd)"
exec java -cp "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
