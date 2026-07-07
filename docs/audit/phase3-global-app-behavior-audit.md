# Phase 3 Global App Behavior Audit

Date: 2026-07-03  
Branch: code-audit  
Scope: location tracking notification, normal notifications, app launch routing, theme persistence, logout cleanup, production smoothness.

Verification run:
- `./gradlew :app:assembleDevDebug :app:testDevDebugUnitTest :app:lintDevDebug`
- Result: PASS
- Notes: Gradle reports AGP 8.5.2 is not officially tested with compileSdk 35. This is a release-readiness warning, not a Phase 3 functional blocker.

## 1. Executive Summary

Phase 3 focused on the global behavior that employees feel every day: notifications, notification tap behavior, theme consistency, logout safety, and app restart routing. Several high-impact problems were found and fixed.

The location foreground notification is now persistent, low-noise, private, and routed through a stable activity path. General push notifications now use a stable channel, stable notification IDs, immutable pending intents, private visibility, and direct splash routing. Theme preference is now applied at app startup instead of being reset to light mode. Logout now centralizes session cleanup, stops location tracking, marks tracking inactive, and clears notifications before clearing stored session data.

Remaining risk is mainly device validation: Android notification channels preserve old importance after install, OEM background behavior differs heavily, and theme/notification/logout smoothness needs real-device testing on Android 10 through Android 15.

## 2. Current App Behavior Understanding

The app has employee attendance, work management, reimbursement, calendar, authentication, and background location tracking. Phase 1 hardened attendance. Phase 2 hardened work management and reimbursement. Phase 3 hardens app-wide shell behavior.

Key files reviewed and changed:
- `app/src/main/java/com/atvantiq/wfms/services/LocationTrackingService.kt`
- `app/src/main/java/com/atvantiq/wfms/services/MyFirebaseMessagingService.kt`
- `app/src/main/java/com/atvantiq/wfms/ui/screens/SplashActivity.kt`
- `app/src/main/java/com/atvantiq/wfms/app/MApplication.kt`
- `app/src/main/java/com/atvantiq/wfms/utils/ThemeManager.kt`
- `app/src/main/java/com/atvantiq/wfms/utils/SessionCleanup.kt`
- `app/src/main/AndroidManifest.xml`

## 3. Critical Risks

Issue:
Severity: High
Module: Location notification
File/Class if known: `LocationTrackingService`
Current behavior: Prior notification channel could remain noisy on installed devices because Android channel importance cannot be lowered after creation.
Expected behavior: Location tracking notification must remain visible without repeatedly disturbing the user.
Risk: Employees may disable notifications, which can break foreground-service visibility and trust.
Recommended fix: Use a new low-importance channel ID and silent ongoing notification.
Implementation notes: Fixed with `location_service_channel_v2`, `IMPORTANCE_LOW`, `PRIORITY_LOW`, `setSilent(true)`, `setOnlyAlertOnce(true)`, and private visibility.
Test case: PH3-NOTIF-001

Issue:
Severity: High
Module: Logout/session
File/Class if known: `DashboardActivity`, `SharedDashboardActivity`, `BaseActivitySimple`, `BaseFragmentSimple`
Current behavior: Logout paths cleared preferences directly and could leave location service/notification state behind.
Expected behavior: Logout must stop tracking, clear notifications, mark tracking inactive, and clear session consistently.
Risk: Location notification or service may continue after logout, creating privacy and trust issues.
Recommended fix: Centralize logout cleanup.
Implementation notes: Fixed with `SessionCleanup.clearForLogout(...)`.
Test case: PH3-LOGOUT-001

Issue:
Severity: Medium
Module: Theme
File/Class if known: `MApplication`
Current behavior: App startup forced light mode, overriding stored dark/system preference.
Expected behavior: Stored theme must apply immediately on app launch.
Risk: Theme appears broken after restart and causes UI flicker or inconsistent experience.
Recommended fix: Apply stored dark mode from `ThemeManager` during `Application.onCreate`.
Implementation notes: Fixed with `ThemeManager.applyStoredDarkMode(this)`.
Test case: PH3-THEME-001

Issue:
Severity: Medium
Module: Normal notifications
File/Class if known: `MyFirebaseMessagingService`
Current behavior: Notification routing was generic and IDs could collide unpredictably.
Expected behavior: Tapping a notification should route predictably and update matching notifications instead of creating duplicates.
Risk: Bad back stack, duplicate alerts, or confusing navigation.
Recommended fix: Use explicit splash routing, stable IDs, immutable pending intents, private visibility.
Implementation notes: Fixed with `ACTION_PUSH_NOTIFICATION`, stable ID generation, `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`.
Test case: PH3-PUSH-001

## 4. Location Tracking Notification Audit

Fixed behavior:
- Foreground-service channel moved to `location_service_channel_v2`.
- Channel importance changed to low.
- Notification is ongoing, silent, private, and service categorized.
- Notification tap opens `SplashActivity` with `ACTION_LOCATION_NOTIFICATION`.
- Existing session routing decides employee/admin dashboard safely.
- Service refuses to start when `PrefKeys.IS_TRACKING_ACTIVE` is false.

Remaining risks:
- Existing installed users may still have the old notification channel. New channel solves future behavior, but QA should uninstall/reinstall and also test upgrade from old builds.
- Android 13+ notification permission denial must be tested on real devices.
- OEMs may still display foreground-service chips or task-manager notices differently.

## 5. Location Notification State Machine

State: LoggedOut
Notification: None
Allowed action: Login only
Next: LoggedInTrackingInactive

State: LoggedInTrackingInactive
Notification: None
Allowed action: Check in
Next success: TrackingStarting
Next failure: TrackingInactiveError

State: TrackingStarting
Notification: Created by foreground service after active flag is true
Allowed action: Wait
Next success: TrackingActiveNotificationVisible
Next failure: TrackingStartFailed

State: TrackingActiveNotificationVisible
Notification: Ongoing, low-priority, silent location notification
Allowed action: Tap notification, checkout, logout
Next tap: RouteThroughSplash
Next checkout success: TrackingStopping
Next logout: LoggedOutCleanup

State: RouteThroughSplash
Notification: Remains visible
Allowed action: App routes to dashboard by valid session
Next success: DashboardVisible
Next no session: LoginVisible

State: TrackingStopping
Notification: Removed after service stops/notification manager cleanup
Allowed action: Wait
Next success: LoggedInTrackingInactive
Next failure: TrackingStopFailed

State: LoggedOutCleanup
Notification: Cancel all notifications, stop location service, clear session
Allowed action: Login
Next: LoggedOut

## 6. Location Notification Test Matrix

Test ID: PH3-NOTIF-001
Module: Location notification
Scenario: Start tracking after successful check-in.
Preconditions: Logged in, location permission granted, notification permission granted.
Steps: Check in, observe notification shade.
Expected result: One ongoing low-noise location notification appears and cannot be swiped away while service is active.
Actual result: Pending real-device validation.
Priority: Critical
Automation possible: Partial

Test ID: PH3-NOTIF-002
Module: Location notification
Scenario: Tap location notification.
Preconditions: Tracking active, app backgrounded.
Steps: Tap notification.
Expected result: App opens through splash without 2-second delay and lands on correct dashboard.
Actual result: Pending real-device validation.
Priority: High
Automation possible: Partial

Test ID: PH3-NOTIF-003
Module: Location notification
Scenario: Checkout stops notification.
Preconditions: Tracking active.
Steps: Checkout successfully.
Expected result: Foreground service stops and location notification disappears.
Actual result: Pending real-device validation.
Priority: Critical
Automation possible: Partial

Test ID: PH3-NOTIF-004
Module: Location notification
Scenario: Logout while checked in/tracking.
Preconditions: Tracking active.
Steps: Open drawer, logout.
Expected result: Tracking flag becomes false, service stops, notifications clear, login screen opens.
Actual result: Pending real-device validation.
Priority: Critical
Automation possible: Partial

## 7. Normal Notification Audit

Fixed behavior:
- General notifications use `wfms_general_notifications_v2`.
- Channel importance is default, not high.
- Notification tap uses explicit `SplashActivity` with `ACTION_PUSH_NOTIFICATION`.
- Payload extras are preserved for future deep-link routing.
- Pending intent is immutable and update-current.
- Notification IDs are stable from payload `notification_id`, `id`, or content key.
- Notification visibility is private.

Remaining risks:
- No product-level deep-link map exists yet for work, reimbursement, approval, or attendance notification types.
- FCM token refresh still logs only and does not sync token to backend.
- If the server sends notification-only payloads while app is backgrounded, Android system may display its own notification outside this service path.

## 8. Normal Notification Routing Matrix

Notification type: Unknown/general
Current route: Splash -> session check -> dashboard/login
Expected behavior: Safe default route
Status: Fixed

Notification type: Attendance
Current route: Dashboard only
Expected behavior: Deep link to attendance/current status
Status: Enhancement

Notification type: Work assigned
Current route: Dashboard only
Expected behavior: Work detail/list with correct task
Status: Enhancement

Notification type: Reimbursement
Current route: Dashboard only
Expected behavior: Claim detail/list with correct claim
Status: Enhancement

Notification type: Session/security
Current route: Dashboard/login
Expected behavior: Login/session screen
Status: Enhancement

## 9. App Launch And Back Stack Audit

Fixed behavior:
- `SplashActivity` is `singleTask`.
- `DashboardActivity` is `singleTop`.
- Notification launches bypass splash delay.
- `onNewIntent` updates the current splash intent before routing.
- Session routing remains centralized in splash.

Risk still present:
- The app does not yet have a typed notification router or deep-link contract.
- Back-stack behavior must be manually verified when app is cold, warm, backgrounded, and already on a nested screen.

## 10. Theme Change Code Audit

Fixed behavior:
- App startup now applies the stored dark/system mode through `ThemeManager`.
- The app no longer forces `MODE_NIGHT_NO` on every launch.
- Existing theme picker logic remains intact.

Risk still present:
- Some screens may still use hardcoded colors or drawables that do not adapt cleanly.
- Activity recreation during theme change needs visual QA across forms, dialogs, bottom sheets, date pickers, and list screens.

## 11. Theme Smoothness Test Matrix

Test ID: PH3-THEME-001
Module: Theme
Scenario: Persist dark theme after restart.
Preconditions: Logged in.
Steps: Select dark mode, kill app, relaunch.
Expected result: App opens in dark mode immediately without reverting to light.
Actual result: Pending device validation.
Priority: High
Automation possible: Yes

Test ID: PH3-THEME-002
Module: Theme
Scenario: System theme follows OS setting.
Preconditions: App set to system theme.
Steps: Toggle OS dark mode, reopen app.
Expected result: App follows OS theme.
Actual result: Pending device validation.
Priority: Medium
Automation possible: Partial

Test ID: PH3-THEME-003
Module: Theme
Scenario: Theme change while claim/work form is open.
Preconditions: Form screen open with entered data.
Steps: Change theme using app setting if available or OS setting.
Expected result: UI remains readable and form state is preserved.
Actual result: Pending device validation.
Priority: High
Automation possible: Partial

## 12. Logout Process Audit

Fixed behavior:
- Logout cleanup is centralized in `SessionCleanup`.
- Tracking active flag is set to false before session clear.
- Location service is stopped.
- Notifications are cleared.
- Secure preferences are cleared.
- Employee dashboard, shared/admin dashboard, base activity unauthorized logout, and base fragment logout use the same cleanup.

Remaining risks:
- Backend logout/token revocation was not confirmed in this phase.
- Cancel-all notification cleanup is safe for this app, but if future notifications need to survive logout, cleanup should target app-owned IDs by category.

## 13. Logout Test Matrix

Test ID: PH3-LOGOUT-001
Module: Logout
Scenario: Logout while tracking is active.
Preconditions: Logged in and checked in.
Steps: Logout from drawer.
Expected result: Service stops, persistent notification disappears, session is cleared, login screen opens.
Actual result: Pending real-device validation.
Priority: Critical
Automation possible: Partial

Test ID: PH3-LOGOUT-002
Module: Logout
Scenario: Unauthorized API triggers global logout.
Preconditions: Expired token.
Steps: Trigger protected API call.
Expected result: Session cleanup path runs and user lands on login.
Actual result: Pending integration validation.
Priority: High
Automation possible: Yes

Test ID: PH3-LOGOUT-003
Module: Logout
Scenario: Reopen app after logout.
Preconditions: User logged out.
Steps: Kill and relaunch app.
Expected result: Splash routes to login; no dashboard flash; no tracking notification.
Actual result: Pending device validation.
Priority: High
Automation possible: Yes

## 14. Performance And UX Smoothness Review

Improvements completed:
- Location notification no longer uses high-priority alert behavior.
- Notification taps bypass splash timer, reducing perceived latency.
- Theme no longer resets at startup, reducing flicker.
- Stable notification IDs reduce duplicate notification noise.

Remaining improvements:
- Add typed deep-link routing so notifications open exact destination.
- Add real-device startup timing measurement.
- Add Macrobenchmark for cold start, notification tap to dashboard, and theme toggle.
- Add StrictMode debug checks for accidental main-thread I/O.

## 15. Security And Privacy Review

Improvements completed:
- Notification visibility set to private for location and push notifications.
- Logout stops location service and clears notifications.
- Pending intents are immutable.
- Tracking service will not run when active tracking flag is false.

Remaining risks:
- FCM payload extras are passed through without a typed allowlist.
- Backend token revocation on logout was not verified.
- Notification text should be reviewed by product/legal for location privacy clarity.
- Old installs may retain old notification channels until upgrade behavior is tested.

## 16. Code Architecture Refinement Plan

Recommended next refinements:
- Add `NotificationRouter` with typed destinations for attendance, work, reimbursement, and approvals.
- Add `NotificationChannelManager` to own all channel IDs, names, importance, privacy, and migration notes.
- Add `SessionManager.logout(reason)` as the single public logout API, with backend revoke support if available.
- Add `ThemeStateRepository` backed by DataStore or encrypted preferences if theme becomes user-profile controlled.
- Add instrumentation helpers for notification and activity routing tests.
- Add app-level analytics events: notification received, notification opened, logout started, logout completed, theme changed.

## 17. Complete Test Cases

Test ID: PH3-PUSH-001
Module: Normal notification
Scenario: Receive data FCM while app is foregrounded.
Preconditions: Logged in, notification permission granted.
Steps: Send FCM data payload with title/body/id.
Expected result: One private notification appears using stable ID and opens app on tap.
Actual result: Pending FCM/device validation.
Priority: High
Automation possible: Partial

Test ID: PH3-PUSH-002
Module: Normal notification
Scenario: Receive same notification twice.
Preconditions: Logged in.
Steps: Send same FCM payload twice with same id.
Expected result: Existing notification updates instead of duplicate spam.
Actual result: Pending FCM/device validation.
Priority: Medium
Automation possible: Partial

Test ID: PH3-PUSH-003
Module: Normal notification
Scenario: Tap notification while logged out.
Preconditions: Session cleared.
Steps: Tap existing notification.
Expected result: Splash routes to login.
Actual result: Pending device validation.
Priority: High
Automation possible: Yes

Test ID: PH3-BACKSTACK-001
Module: App launch
Scenario: Tap notification while app is already open on nested screen.
Preconditions: Logged in, nested screen open.
Steps: Tap push notification.
Expected result: Existing task is reused; no duplicate splash/dashboard stack trap.
Actual result: Pending device validation.
Priority: High
Automation possible: Partial

Test ID: PH3-BACKSTACK-002
Module: App launch
Scenario: Cold start from launcher.
Preconditions: Logged in.
Steps: Force stop app, open launcher.
Expected result: Splash waits normally, then routes to correct dashboard.
Actual result: Pending device validation.
Priority: Medium
Automation possible: Yes

Test ID: PH3-SEC-001
Module: Privacy
Scenario: Lock screen notification visibility.
Preconditions: Device locked, notification received.
Steps: View lock screen.
Expected result: Sensitive payload content is not exposed beyond private notification rules.
Actual result: Pending device validation.
Priority: High
Automation possible: No

## 18. Bug List Format

Issue:
Severity: High
Module: Location notification
File/Class if known: `LocationTrackingService`
Current behavior: Old channel could be high-noise and cannot be downgraded in place.
Expected behavior: Ongoing location notification is visible but low-noise.
Risk: Users disable notifications or distrust tracking.
Recommended fix: New channel ID with low importance and silent ongoing behavior.
Implementation notes: Completed.
Test case: PH3-NOTIF-001

Issue:
Severity: High
Module: Logout
File/Class if known: `SessionCleanup`
Current behavior: Logout was duplicated and preference-only in multiple callers.
Expected behavior: One cleanup path stops tracking and clears notifications/session.
Risk: Privacy leak after logout.
Recommended fix: Centralize cleanup.
Implementation notes: Completed.
Test case: PH3-LOGOUT-001

Issue:
Severity: Medium
Module: Theme
File/Class if known: `MApplication`
Current behavior: Startup forced light mode.
Expected behavior: Stored mode applied.
Risk: Broken theme persistence.
Recommended fix: Apply stored mode during app create.
Implementation notes: Completed.
Test case: PH3-THEME-001

Issue:
Severity: Medium
Module: Push notification
File/Class if known: `MyFirebaseMessagingService`
Current behavior: Generic routing and less stable notification identity.
Expected behavior: Stable routing/identity.
Risk: Duplicate notifications or confusing taps.
Recommended fix: Stable ID, explicit action, immutable pending intent.
Implementation notes: Completed.
Test case: PH3-PUSH-001

## 19. Enhancement List

1. Add typed notification deep links for attendance, assigned work, reimbursement, approvals, and announcements.
2. Sync refreshed FCM token to backend with retry.
3. Add notification analytics for received/opened/dismissed where allowed.
4. Add Macrobenchmark tests for cold start and notification tap route.
5. Add instrumented tests for logout while foreground service is active.
6. Add user-facing notification settings screen.
7. Add channel migration QA script for old-to-new installs.
8. Add theme screenshot tests for key screens.
9. Add backend logout/revoke API if supported.
10. Replace cancel-all logout cleanup with category-specific cancellation if future logged-out notifications are required.

## 20. Final Release Checklist

Required before Phase 3 release:
- Build, unit test, and lint pass. Status: Done.
- Android 10, 11, 12, 13, 14, and 15 notification tests. Status: Pending.
- Upgrade install test from previous build with old notification channel. Status: Pending.
- Logout while tracking active on Pixel/Samsung/Xiaomi. Status: Pending.
- Theme persistence and no-flicker test on light/dark/system. Status: Pending.
- FCM foreground/background/killed-app tests. Status: Pending.
- Back-stack tests from launcher, location notification, and push notification. Status: Pending.
- Privacy review for lock-screen notification content. Status: Pending.

## 21. Final Verdict

Notification reliability score: 82/100  
Location notification UX score: 86/100  
Normal notification routing score: 74/100  
Theme implementation score: 84/100  
Logout safety score: 88/100  
Production readiness score: 80/100

Top 10 must-fix issues:
1. Complete real-device notification channel migration testing.
2. Verify location notification remains visible and low-noise while tracking.
3. Verify checkout and logout remove location notification on every supported OS.
4. Verify Android 13+ notification permission denial behavior.
5. Verify Android 14/15 foreground service behavior on real devices.
6. Verify notification taps do not create duplicate dashboard/splash stacks.
7. Verify theme persists after force-stop and process death.
8. Verify logout after token expiry stops tracking and clears notifications.
9. Verify FCM behavior for data, notification, and mixed payloads.
10. Add typed deep links before relying on notifications for task-specific workflows.

Top 10 enhancements:
1. NotificationRouter.
2. NotificationChannelManager.
3. Backend FCM token sync.
4. Macrobenchmark coverage.
5. Device farm notification tests.
6. Theme screenshot regression tests.
7. SessionManager logout API.
8. Notification analytics.
9. User notification preferences.
10. Backend token revoke on logout.

Ready for production: Not yet.  
Phase 3 can be released: Yes, as an internal QA build only.  
What must be done before release: Complete the real-device notification, theme, back-stack, and logout validation checklist above. Code-level build/lint/unit verification is green, but production readiness requires device evidence because Android notification and foreground-service behavior is OS/OEM-sensitive.
