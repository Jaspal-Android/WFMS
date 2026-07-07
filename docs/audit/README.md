# WFMS Android Audit

Audit date: 2026-06-29

Scope: static review of the Android project under `app/src/main`, Gradle config, manifests, navigation XML, repositories, network layer, model classes, storage utilities, Firebase services, permissions, and Android platform integrations. No application source code was modified.

## Documents

- [Screen and Navigation Inventory](screen-navigation-inventory.md)
- [API and Data Model Inventory](api-data-model-inventory.md)
- [Storage, Permissions, Notifications, Payments, Analytics](platform-inventory.md)
- [Risky Android-Specific Behavior](android-risk-review.md)
- [Phase 1 Production Readiness Audit](phase1-production-readiness-report.md)

## High-Level App Shape

- Native Android app written mostly in Kotlin with some Java utility/widget code.
- Package/namespace: `com.atvantiq.wfms`.
- Min/target/compile SDK: min 24, target 35, compile 35.
- Product flavors: `dev`, `beta`, `demo`, `prod`.
- Network stack: Retrofit + OkHttp + Gson; base URL comes from `BuildConfig.BASE_URL`.
- DI: Hilt.
- UI: XML layouts, Android Navigation component for the employee dashboard drawer, data binding and view binding.
- Firebase: Messaging and Crashlytics.
- Location: foreground/background location permissions plus a foreground `LocationTrackingService`.
- Local persistence: encrypted shared preferences for auth/profile data; regular shared preferences for theme.

## Primary Findings

- The app has two post-login shells: employee `DashboardActivity` and admin/non-employee `SharedDashboardActivity`.
- Employee drawer destinations include dashboard, attendance/work management, reimbursement, vendor, cab, material reconciliation, about, and feedback. Several are intercepted as "under development" in `DashboardActivity` despite being declared in the nav graph.
- API coverage is concentrated in one Retrofit interface, `ApiService`, with 41 endpoint methods.
- Auth token, user data, and employee data are stored with `EncryptedSharedPreferences`.
- `allowBackup="true"` is enabled, but backup exclusion paths appear to use `wfms_secure_pref.xml` while the secure preference file name is `wfms_secure_prefs`, so encrypted preference backup exclusion should be verified.
- `HttpLoggingInterceptor.Level.BODY` is enabled for all builds, which can expose credentials, auth headers, uploaded metadata, and location payloads in logs.
- `network_security_config.xml` permits cleartext traffic for several API domains/IPs, including production `api.onaqt.com`.
- `path_provider.xml` grants broad roots, including all external storage and all files/cache roots.
- Location tracking is sticky, high-accuracy, high-importance notification based, and can run in the background after attendance check-in.
- Payments are not implemented in code, but `PAYMENT_KEY` is injected from `secrets.properties` into `BuildConfig`.
- Analytics event logging is not present. Crashlytics collection is explicitly enabled.

## Source Anchors

- Manifest: `app/src/main/AndroidManifest.xml`
- Navigation graph: `app/src/main/res/navigation/mobile_navigation.xml`
- Build config: `app/build.gradle.kts`
- API service/endpoints: `app/src/main/java/com/atvantiq/wfms/network/ApiService.kt`, `NetworkEndPoints.kt`
- Network module: `app/src/main/java/com/atvantiq/wfms/di/modules/NetModule.kt`
- Preferences: `app/src/main/java/com/atvantiq/wfms/data/prefs`
- Location service: `app/src/main/java/com/atvantiq/wfms/services/LocationTrackingService.kt`
- Firebase messaging service: `app/src/main/java/com/atvantiq/wfms/services/MyFirebaseMessagingService.kt`
- File/media helpers: `app/src/main/java/com/atvantiq/wfms/utils/files`
