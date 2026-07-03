# Storage, Permissions, Notifications, Payments, Analytics

## Storage Usage

| Storage Area | Code | Data Stored | Notes |
|---|---|---|---|
| Encrypted shared preferences | `SecurePrefMain`, file name `wfms_secure_prefs` | `LOGIN_TOKEN`, `USER_DATA`, `EMP_DATA` | Uses `EncryptedSharedPreferences` and Android Keystore `MasterKey`. If initialization fails, secure prefs and the master key alias are deleted, then rebuilt. |
| Regular shared preferences | `PrefMain`, file name `wfms_prefs` | Theme and dark-mode keys: `selected_theme`, `dark_mode` | Non-sensitive UI settings. |
| App internal files | `FileUtils.createImageFile` | Camera captures under `filesDir/WFMS/WFMS_yyyyMMdd_HHmmss.jpg` | Used through FileProvider for camera captures. |
| App cache | `FileUtils`, `PickMediaHelper` | Picked gallery copies, resized/compressed images, temp JPGs | Used before multipart uploads. Cleanup policy not found. |
| MediaStore external pictures | `FileUtils.createImageFileQ` | Images under `Pictures/WFMS` | Function exists; primary helper currently uses internal files. |
| FileProvider | `path_provider.xml` | Grants content URIs from external, external-files, cache, external-cache, and files roots | Paths are broad. The provider is not exported and grants URI permissions. |

## Backup / Device Transfer

- `AndroidManifest.xml` sets `android:allowBackup="true"`.
- `backup_rules.xml` and `data_extraction_rules.xml` exclude `sharedpref` path `wfms_secure_pref.xml`.
- The actual secure pref key is `PrefKeys.WFMS_SECURE_PREF = "wfms_secure_prefs"`, which should typically produce `wfms_secure_prefs.xml`.
- Finding: backup exclusions appear to use a singular path and may not exclude the actual encrypted preference file. Verify generated file names and exclude encrypted prefs/keyset files explicitly.

## Permissions

Declared in `app/src/main/AndroidManifest.xml`:

| Permission / Feature | Purpose Observed |
|---|---|
| `ACCESS_NETWORK_STATE` | Connectivity checks/network state. |
| `INTERNET` | Retrofit APIs, Firebase, Play updates, maps/geocoding. |
| `android.hardware.camera.any` feature | Camera availability. Not marked `required=false`, so Play distribution may require camera-capable devices. |
| `CAMERA` | Work start photos, leave/cab/claim attachments, image capture. |
| `ACCESS_FINE_LOCATION` | Current location, check-in/out, work start/end, cab fare, background tracking. |
| `ACCESS_COARSE_LOCATION` | Same location flows. |
| `ACCESS_BACKGROUND_LOCATION` | Live location tracking after day start/check-in. |
| `FOREGROUND_SERVICE` | Foreground service runtime. |
| `FOREGROUND_SERVICE_LOCATION` | Android 14+ foreground location service type. |
| `POST_NOTIFICATIONS` | FCM notifications and foreground location service notification on Android 13+. |
| `WAKE_LOCK` | Likely Firebase/FCM or service keep-awake support; direct explicit usage not found. |

Runtime permission flows:

- `LoginActivity`: foreground location request for "fetch current location"; has background flow helper but currently calls fetch without requiring background.
- `DashboardFragment` and `SharedDashboardActivity`: request foreground, background location, and notifications before day start/live tracking.
- `AttendanceFragment` and `AssignedTaskDetailActivity`: request foreground location for work start/end.
- `AddCabFareActivity`: requests foreground location and camera/photo permissions.
- `PickMediaHelper`: requests only `CAMERA`; uses Android Photo Picker when available and `ACTION_PICK` fallback without storage permission.

## Notifications

| Notification Source | Channel | Importance/Priority | Behavior |
|---|---|---|---|
| `MyFirebaseMessagingService` | `default_channel_id` / "Default Channel" | Importance high, priority high | Displays FCM data or notification payloads. Adds all data extras to launch intent. Uses unique ID from current time modulo 10000. |
| `LocationTrackingService` | `location_service_channel` / "Location Tracking" | Importance high, priority high, public visibility | Ongoing foreground service notification that opens `SplashActivity`. |

FCM token lifecycle:

- `LoginActivity` fetches `FirebaseMessaging.getInstance().token` after login and sends it to `notifications/notification-token`.
- `onNewToken` logs the token but does not send it to the backend.
- Logout deletes the FCM token via `FirebaseMessaging.getInstance().deleteToken()` in base/logout flows.

## Payments

- No payment SDK, billing SDK, Razorpay, Stripe, PayU, Cashfree, or in-app billing usage was found.
- `app/build.gradle.kts` injects `PAYMENT_KEY` from `secrets.properties` into `BuildConfig`.
- `PAYMENT_KEY` is not referenced by app source code in the scan.

## Analytics and Crash Reporting

| Tool | Status |
|---|---|
| Firebase Crashlytics | Enabled in Gradle and explicitly enabled in `MApplication.onCreate`. |
| Firebase Analytics | No dependency or `FirebaseAnalytics`/`logEvent` usage found. |
| Custom analytics | No analytics/event logging usage found. |
| Stetho | Dependency included; network interceptor added only when app `BuildConfig.DEBUG`; app initialization uses `com.facebook.stetho.BuildConfig`, which should be checked. |

## Third-Party / Platform Services

- Google Maps API key is injected into manifest meta-data and `BuildConfig.GOOGLE_MAPS_API_KEY`.
- Play Services Location and Maps are used.
- Google Play In-App Updates are used in both employee and admin shells.
- Firebase Messaging and Crashlytics are used.
- Lottie, MPAndroidChart, Flexbox, SlideToAct, CircleImageView, Hilt, Retrofit/OkHttp, Security Crypto are included.
