# 2GRazblock

Приложение для Android 11+, которое открывает системный экран Phone info / RadioInfo (как при вводе *#*#4636#*#*). Само ничего не переключает: выбор режима сети полностью на пользователе. Есть предупреждения о рисках при выборе «LTE only» без VoLTE.

## Сборка
Требования: JDK 17, Android SDK 34+, Gradle Wrapper 8.4 (в проекте), Kotlin 1.9+.

1. Клонировать репозиторий.
2. (Опционально для релиза) Положить файл ключа `cert/release-keystore.jks` и `cert/keystore.properties` со свойствами `storeFile`, `storePassword`, `keyAlias`, `keyPassword` (эти файлы не в репозитории).
3. Сборка:
   - Debug: `./gradlew assembleDebug`
   - Release APK: `./gradlew --no-daemon assembleRelease`
   - Release AAB: `./gradlew --no-daemon bundleRelease`

Выходные файлы кладутся в `%TEMP%/gsm-build/outputs/apk/release/` и `%TEMP%/gsm-build/outputs/bundle/release/` (buildDir переопределён).

Если Gradle ругается на доступ к `wrapper/dists`, запустите с `GRADLE_USER_HOME=%TEMP%/gradle-home`.
