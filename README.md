# What to Eat? 今餐食乜？

**Spin • Match • Eat.** A native Kotlin / Jetpack Compose / Material 3 Android game for 1–8 people sharing one phone.

All gameplay runs locally. The app has **no Internet or location permissions, backend, accounts, APIs, maps, cloud services, analytics or database**. Building the project for the first time needs Internet access to download normal Android build dependencies; playing the installed app does not.

## Open and run in Android Studio / 開啟方法

1. Open **Android Studio → Open**, then select the **WhatToEat** folder containing `settings.gradle.kts`. Do not select only `app`.
2. In **Settings → Build, Execution, Deployment → Build Tools → Gradle**, choose the **Gradle wrapper** and Android Studio's **Embedded JDK / jbr** for Gradle JDK.
3. Let Gradle Sync finish. The first sync downloads Gradle and AndroidX dependencies.
4. In **Tools → SDK Manager**, install **Android SDK Platform 37 / Android 17 (37.0)** and the SDK Build Tools requested by Gradle. Accept the SDK licences. The project targets API 37 and runs on **Android 8.0 / API 26 or newer**.
5. Create an emulator in **Tools → Device Manager**, or connect an Android phone with USB debugging enabled.
6. Select the `app` run configuration and your device, then click **Run ▶**.

繁體中文：揀 **WhatToEat 整個資料夾**開啟，等 Gradle 同步完成，再揀模擬器或手機，按 ▶。首次編譯要上網下載開發工具；安裝後可開飛行模式玩。若出現 Gradle 下載錯誤，先檢查電腦網絡／Android Studio proxy 設定。

`local.properties` is machine-specific. Android Studio normally creates it automatically. If an SDK path error appears after moving the project to another computer, remove that file and select the installed SDK in Android Studio. No API key is needed.

## Play

- English is the initial in-app language. Tap **繁體中文 / English** to switch at any point.
- Choose 1–8 players. Names are optional; blank names become Player 1, Player 2, etc.
- The nine default options are Japanese, Chinese, Western, Korean, Thai, Hot Pot, Pizza, Burgers and Indian.
- **Edit menu** lets you add or remove options, with 1–12 options supported. Duplicate names are blocked. A custom name is displayed unchanged in both languages.
- Each player can spin up to three times, with no repeated food for that player. With fewer than three menu options, the limit is the number of options.
- The wheel must stop before voting: **Like +3**, **Skip 0**, or **Reject −3**. Like ends further spins and enables **Finish turn**. Skip/Reject permit another spin until the limit, then enable Finish turn.
- **Follow the Crowd** is available before a player's first spin. It skips the entire turn, contributes no votes, and moves to the handoff or final result.
- The handoff screen shows only player names and a Continue button. Previous votes and interim results are hidden.
- **Play again** keeps players and the menu and clears the round. **New game** returns to editable setup. Android Back asks before discarding an active game.

## Matching rules

The displayed +3/0/−3 values express each reaction; results use the requested ranking rules rather than a summed score, so a high score cannot override a Reject.

1. Exclude Follow the Crowd players from all vote/unknown counts.
2. Put all options with **zero Rejects** first, ordered by most Likes.
3. Fill any remaining Top 3 places with rejected options, ordered by **fewest Rejects**, then most Likes. Every such recommendation is labelled **Compromise**. If all options were rejected, this same fallback determines the entire ranking.
4. Exact ties use the original menu order for predictable, explainable results. Show up to three choices; a smaller menu gives fewer results.
5. A Skip is explicitly neutral. An unseen option is counted as **not reviewed**, never as a Skip or approval. A choice can therefore rank highly without any Likes if it has no Rejects; its card states that explicitly.
6. Say **Approved by every voting player** only when every non-following player explicitly Liked it. The number of followers is shown separately.
7. If everyone follows, choose one food locally at random and reveal it with the animated wheel. No votes or Top 3 consensus are fabricated.

## Simple MVVM structure

| File | Responsibility |
| --- | --- |
| `app/src/main/java/com/whattoeat/app/Game.kt` | Stable food data, votes, pure matcher and local turn state machine |
| `app/src/main/java/com/whattoeat/app/GameViewModel.kt` | Observable in-memory setup, language and game state; UI actions |
| `app/src/main/java/com/whattoeat/app/MainActivity.kt` | Compose screens, theme and animated Canvas wheel |
| `app/src/main/java/com/whattoeat/app/WheelMath.kt` | Pure, tested wheel landing calculations |
| `app/src/main/res/` | App icon, theme and launcher name resources |
| `app/src/test/java/com/whattoeat/app/` | JUnit matching, gameplay and wheel tests |

For this beginner MVP, translated screen text is paired directly in Compose `t(English, TraditionalChinese)` calls. The ViewModel language flag switches all screen text immediately. Cuisine names have English and Traditional Chinese fields. Launcher names are Android string resources.

There is one Activity and no navigation framework, dependency injection, repository layer, persistence or service. ViewModel state survives Activity recreation such as rotation. **Force-stopping, process death or closing and reopening the app may clear the game and custom setup.** This is intentional for the in-memory MVP. Default-generated player names retain the language used when starting the round.

Food IDs are separate from their display names. Default IDs remain stable and a custom option gets a UUID when added. Future restaurant support can supply different food data, but there is no location/API/restaurant infrastructure in this version.

## Build and test

On macOS, when Java is not configured in Terminal:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

On Windows, use Android Studio's embedded JDK or set `JAVA_HOME`, then:

```bat
gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The debug APK, after a successful build, is `app/build/outputs/apk/debug/app-debug.apk`.

This project pins AGP 9.2.1 (built-in Kotlin 2.3.10), Compose compiler 2.3.10, Gradle 9.4.1, Compose BOM 2026.09.00, Activity 1.12.4 and Lifecycle 2.10.0. It uses Java/Kotlin bytecode target 17. Use a recent Android Studio compatible with AGP 9.2.1. See the [official AGP release notes](https://developer.android.com/build/releases/agp-9-2-0-release-notes) and [Compose setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler).

The Gradle wrapper launcher and JAR are included, so no separate Gradle installation is needed. See `THIRD_PARTY_NOTICES.md` for the wrapper's provenance.

### Verification in this workspace

**28 real JUnit tests passed**, compiled and run with Android Studio's bundled Kotlin/JUnit tools. This tested the Android-independent production matcher, engine and wheel calculations, including 1/8 players, invalid inputs, unknown versus Skip, no-repeat spins, vote guards, private handoff flow, replay/reset, compromise rankings, unanimity and everyone-following random selection. Android resources also passed standalone AAPT2 compilation and linking against the installed API 37 SDK; the source manifest contains no requested permissions. The shell launchers passed syntax checks.

The equivalent macOS fallback can be rerun without downloads:

```sh
sh tools/verify-core.sh
```

**Full Android/Compose compilation, APK installation and emulator UI tests are not yet verified.** The build was attempted, but the execution environment could not resolve `services.gradle.org`, so Gradle could not download. Installed Android Studio and SDK alone are insufficient without Gradle and Maven dependencies. This is a build-environment limitation, not a requirement for the app to access the Internet. Run the full Gradle command above after opening the project with network access.

## Manual acceptance check after building

1. Turn on airplane mode; start a two-player game, spin and Like; finish, hand off and Follow the Crowd. Verify results appear only at the end.
2. For a player, Skip/Reject three spins; confirm no repeats and no fourth spin.
3. Have everyone follow; confirm the final wheel animates and reveals one random choice.
4. Try English/Traditional Chinese, rotate during a spin, use a small screen/large font, and confirm controls remain reachable by scrolling.
5. Add/remove a custom choice, try one-option and eight-player games, Play again, New game and Android Back.

## Course stages

1. Project and navigation: one Activity, Compose and four local screens.
2. Roulette: Canvas wedges, fixed pointer and decelerating animation.
3. Turns: local engine, voting guards and privacy handoff.
4. Matching: pure tested ranking and fallback rules.
5. Polish: bilingual UI, responsive scrolling and optional custom menu.
6. Verification: 28 passing core tests; full Android build blocked by the environment as described above.

Future Places, GPS, maps, cloud sync and online multiplayer are deliberately outside this MVP.
