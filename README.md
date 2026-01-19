# Radio Info Launcher

Лёгкое приложение для Android 11+, которое пытается открыть скрытый экран Phone info / RadioInfo (аналог кода *#*#4636#*#*). Если производитель его спрятал, показываются варианты: повторить попытку, открыть звонилку с кодом или перейти в системные настройки сети. Разрешения не требуются.

## Сборка
- Откройте проект в Android Studio (Gradle Plugin 8.2+, Gradle 8.4+, Kotlin 1.9+) или выполните `gradle wrapper --gradle-version 8.4` чтобы сгенерировать `gradle/wrapper/gradle-wrapper.jar`, затем `./gradlew assembleDebug`.
- При выборе режима «LTE only / NR only» убедитесь, что на устройстве есть VoLTE, иначе звонки могут не проходить.
