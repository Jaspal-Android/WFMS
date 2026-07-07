# Risky Android-Specific Behavior

Severity labels are audit-oriented: high means likely security/privacy/policy impact; medium means reliability, compliance, or maintainability concern; low means worth tracking but lower immediate impact.

## High

### BODY HTTP Logging in All Builds

- Location: `NetModule.provideOkhttpClient`
- Behavior: `HttpLoggingInterceptor.Level.BODY` is always added, not gated to debug builds.
- Impact: Request/response bodies, credentials, bearer tokens, FCM tokens, locations, claim data, and uploaded metadata can be written to logs.
- Recommendation: Gate BODY logging to debug builds only, redact `Authorization`, and use `NONE` or `BASIC` in release.

### Cleartext Traffic Allowed for API Hosts

- Location: `res/xml/network_security_config.xml`
- Behavior: `cleartextTrafficPermitted="true"` for `31.97.62.78`, `157.173.220.189`, `devapi.onaqt.com`, and `api.onaqt.com`.
- Impact: Permits HTTP traffic to production and development hosts if any code/config uses `http://`, weakening transport security.
- Recommendation: Disable cleartext for production; only permit scoped dev/debug domains if absolutely required.

### Backup Exclusion May Not Match Secure Preference File

- Locations: `PrefKeys.WFMS_SECURE_PREF`, `backup_rules.xml`, `data_extraction_rules.xml`
- Behavior: secure prefs are named `wfms_secure_prefs`, but backup exclusions reference `wfms_secure_pref.xml`.
- Impact: Encrypted preference files and/or crypto keyset files may be included in backup/device transfer despite intent to exclude them.
- Recommendation: Verify generated filenames on device and update backup/data-extraction rules for `wfms_secure_prefs.xml` and AndroidX crypto keyset files.

### Broad FileProvider Paths

- Location: `res/xml/path_provider.xml`
- Behavior: Provider exposes broad roots: entire external storage, external files, cache, external cache, and internal files.
- Impact: If a URI construction bug or future feature passes an unintended file, broad roots increase the blast radius of file sharing.
- Recommendation: Narrow to specific app-controlled subdirectories, for example `files-path` `WFMS/` and specific cache subfolders.

## Medium

### Sticky Background Location Service

- Location: `LocationTrackingService`
- Behavior: `START_STICKY`, high-accuracy updates every 15 minutes, ongoing public high-importance notification, sends latitude/longitude to backend.
- Impact: Sensitive background tracking with battery/privacy/policy implications. Service may restart after process death while checked-in state is not revalidated in service.
- Recommendation: Confirm Play policy disclosure/consent, show precise user value, stop reliably on checkout/logout, and consider checking server/local attendance state before restarting.

### Background Location Requested With Other Permissions

- Locations: `DashboardFragment`, `SharedDashboardActivity`
- Behavior: foreground, background, and notification permissions are assembled into one request array on Android Q+.
- Impact: Android background location flows have special UX/policy requirements; mixing can result in denial patterns or confusing disclosure.
- Recommendation: Request foreground first, then show prominent disclosure, then route to Android settings for background location when required.

### Foreground Service Notification Uses High Importance

- Location: `LocationTrackingService`
- Behavior: notification channel importance high and priority high for ongoing location service.
- Impact: User-visible interruption/noise; may be perceived as aggressive for a persistent tracking indicator.
- Recommendation: Consider default/low importance where policy and UX permit, while preserving mandatory foreground service visibility.

### FCM Token Refresh Not Uploaded

- Location: `MyFirebaseMessagingService.onNewToken`
- Behavior: refreshed token is logged only.
- Impact: Backend can hold stale notification tokens after rotation unless user logs in again.
- Recommendation: Upload refreshed token when authenticated, or persist pending token and send when auth is available.

### Crashlytics Always Enabled

- Location: `MApplication.onCreate`
- Behavior: `setCrashlyticsCollectionEnabled(true)` is unconditional.
- Impact: May conflict with privacy/consent requirements in some deployments; debug builds may upload crashes.
- Recommendation: Gate by build type, flavor, and/or explicit user/company policy consent.

### Play In-App Update Listener Registration

- Locations: `DashboardActivity`, `SharedDashboardActivity`
- Behavior: flexible update listener is registered but no unregister was found.
- Impact: Potential leak or duplicate callbacks across activity lifecycle.
- Recommendation: Keep listener reference and unregister in `onDestroy`.

### Camera Feature May Exclude Non-Camera Devices

- Location: manifest `uses-feature android.hardware.camera.any`
- Behavior: feature has no `android:required="false"`.
- Impact: Play Store may filter devices without a camera even if some workflows could operate without capture.
- Recommendation: Set `required=false` if camera is optional for installing the app.

### Secrets in BuildConfig

- Location: `app/build.gradle.kts`
- Behavior: Google Maps key and `PAYMENT_KEY` are injected into `BuildConfig`; Maps key also in manifest metadata.
- Impact: Values embedded in APK are extractable. `PAYMENT_KEY` is unused but still shipped.
- Recommendation: Restrict API keys server-side and by package/signing cert; remove unused keys from client builds.

## Low / Follow-Up

### Stetho App Initialization Uses Library BuildConfig

- Location: `MApplication`
- Behavior: imports `com.facebook.stetho.BuildConfig`, not app `com.atvantiq.wfms.BuildConfig`.
- Impact: Debug gating may not behave as intended.
- Recommendation: Use the app module `BuildConfig`.

### No Cache Cleanup for Picked/Compressed Images

- Locations: `FileUtils`, `PickMediaHelper`
- Behavior: copied and compressed images are stored in cache; explicit cleanup was not found.
- Impact: Cache growth and stale attachment data until OS/app cleanup.
- Recommendation: Delete temp files after successful upload or during periodic cleanup.

### Deprecated Real Path Access for Pre-Q Gallery

- Location: `FileUtils.getRealPathFromUri`
- Behavior: uses `MediaStore.Images.Media.DATA` for pre-Q.
- Impact: Legacy path access is brittle; mostly avoided by `PickMediaHelper` copy-to-cache approach.
- Recommendation: Prefer stream-copy for all Android versions.

### Logs Include Sensitive Data

- Locations: `LocationTrackingService`, `MyFirebaseMessagingService`, `LoginActivity`
- Behavior: logs FCM payloads/tokens, location coordinates, and location API responses.
- Impact: Sensitive operational data in logcat, especially on debug/internal builds.
- Recommendation: Remove or gate sensitive logs and avoid logging tokens/coordinates.

### Drawer Destinations Declared but Blocked

- Locations: `mobile_navigation.xml`, `DashboardActivity`
- Behavior: vendor/cab/material/about/feedback are in nav graph but overridden with "under development" dialogs.
- Impact: QA/product confusion; dead code paths can drift.
- Recommendation: Either remove/hide blocked menu items or enable navigation consistently.

### Request Models Are Mostly Untyped

- Locations: ViewModels/repositories using `JsonObject` and `JsonArray`.
- Impact: Schema drift is harder to test, discover, and refactor.
- Recommendation: Introduce typed request DTOs for high-risk mutations such as attendance, work end, site create, and claim creation.
