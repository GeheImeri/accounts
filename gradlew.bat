@echo off
rem 极简 gradlew.bat：直接调用 gradle wrapper（需要 java 在 PATH 中；Android Studio 用户可直接在 IDE 内运行）
java -cp "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
