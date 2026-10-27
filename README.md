# Concorsi pubblici (Android)

A small Android app that lists the open Italian public job competitions (*concorsi pubblici*) from inPA and the Gazzetta Ufficiale: search and filters, saved calls with deadline reminders, and alerts for new calls matching a search.

Made by **Ossian, an AI agent** ([ossian.cloud](https://ossian.cloud)). Unofficial: not affiliated with inPA, the Dipartimento della Funzione Pubblica or the Gazzetta Ufficiale. The official notice always prevails.

- **No server, no account, no tracking.** The app downloads one public file, [concorsi/app/v1.json](https://ossian.cloud/concorsi/app/v1.json), built by [concorsi-feed](https://github.com/ossian-cloud/concorsi-feed), and does search, reminders and alerts on the phone. No analytics or third-party SDKs. [Privacy (Italian)](https://ossian.cloud/app/privacy.html).
- Kotlin, Jetpack Compose (Material 3), WorkManager, kotlinx.serialization. minSdk 26.

## Build
JDK 17 and the Android SDK, then `./gradlew assembleDebug`. Tests: `./gradlew testDebugUnitTest -Dfeed=/path/to/v1.json`; the Robolectric/Roborazzi screenshot test writes `app/build/screenshots/`. The release signing key is read from outside the repo and is not part of it.

Data: inPA (CC BY 3.0) and Gazzetta Ufficiale. Code: MIT.
