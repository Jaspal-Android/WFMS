# Phase 1 Production Readiness Audit

Audit date: 2026-07-03

Scope: static code audit of the WFMS Android app with deep focus on attendance check-in/check-out, foreground/background location tracking, persistent notification, calendar attendance status, and auth/session behavior.

## 1. Executive Summary

The app is functional, but the core attendance module is not yet production-grade for daily employee use under real-world failure conditions. The happy path exists: the app reads attendance status, lets the user check in/out, starts a foreground location service after check-in, stops it after checkout, and refreshes the calendar after some attendance mutations.

The production risk is in state correctness and recovery. Attendance state currently lives across `DashboardFragment`, `SharedDashboardActivity`, `DashboardViewModel`, `LocationTrackingService`, and calendar fragments with local booleans, one-shot LiveData, and no durable attendance state machine. If the app is killed, the service is killed, permission is revoked, GPS is disabled, network is lost, or checkout is blocked by incomplete work, the app has no unified recovery model.

Top risks:

- Wrong attendance state can be shown after process death, service death, or stale API response.
- Location tracking is not durable, offline-capable, or self-healing.
- Checkout validation is reactive to checkout API response instead of an explicit work-status gate.
- Duplicate check-in/check-out taps are not robustly prevented by a centralized in-flight state.
- Background location permission and notification permission flows are mixed with operational actions.
- Network logging and cleartext config create security/privacy risk.
- Calendar refresh is partial and date/timezone handling is fragile.

Phase 1 should not be released as "perfect core attendance" until the must-fix items in section 20 are complete.

## 2. Current App Understanding

Architecture:

- App style: Kotlin + XML layouts + DataBinding/ViewBinding.
- DI: Hilt modules for repositories, prefs, network.
- Network: Retrofit `ApiService`, endpoint constants in `NetworkEndPoints`, OkHttp in `NetModule`.
- State: ViewModels with `MutableLiveData<ApiState<T>>`; limited `StateFlow` helper exists but is not used in attendance.
- Persistence: `SecurePrefMain` for token/user/employee data; `PrefMain`/raw shared prefs for theme.
- Navigation: `SplashActivity` routes to `LoginActivity`, employee `DashboardActivity`, or admin/non-employee `SharedDashboardActivity`.
- Attendance home button:
  - Employee: `DashboardFragment`.
  - Admin/non-employee: `SharedDashboardActivity`.
  - Both use a slide button whose label flips between Start Day and End Day.
- Tracking: `LocationTrackingService` uses `FusedLocationProviderClient`, foreground service, high-accuracy updates every 15 minutes, and sends each location directly to API.
- Calendar: `AttendanceStatusFragment` fetches monthly attendance details and feeds a custom `CalendarView`.
- Auth: `SplashActivity` auto-routes based on encrypted token/user; `LoginActivity` supports password and OTP login; logout clears secure prefs and deletes FCM token.

Important source files:

- `app/src/main/java/com/atvantiq/wfms/ui/screens/dashboard/DashboardFragment.kt`
- `app/src/main/java/com/atvantiq/wfms/ui/screens/admin/SharedDashboardActivity.kt`
- `app/src/main/java/com/atvantiq/wfms/ui/screens/dashboard/DashboardViewModel.kt`
- `app/src/main/java/com/atvantiq/wfms/services/LocationTrackingService.kt`
- `app/src/main/java/com/atvantiq/wfms/ui/screens/dashboard/tabs/attendance/AttendanceStatusFragment.kt`
- `app/src/main/java/com/atvantiq/wfms/ui/screens/attendance/AttendanceViewModel.kt`
- `app/src/main/java/com/atvantiq/wfms/ui/screens/attendance/assignedTasks/AssignedTaskDetailActivity.kt`
- `app/src/main/java/com/atvantiq/wfms/network/ApiService.kt`
- `app/src/main/java/com/atvantiq/wfms/base/BaseViewModel.kt`
- `app/src/main/java/com/atvantiq/wfms/data/prefs/SecurePrefMain.kt`

## 3. Critical Risks

Issue:
Severity: Critical
Module: Attendance state
File/Class if known: `DashboardFragment`, `SharedDashboardActivity`, `DashboardViewModel`
Current behavior: Attendance state is inferred from API response and local `isDayStarted` booleans. There is no single durable state machine.
Expected behavior: A single source of truth should drive button label, enabled state, API calls, tracking state, recovery, and calendar refresh.
Risk: Wrong state can allow duplicate check-in, blocked checkout confusion, or missing tracking while user is checked in.
Recommended fix: Add `AttendanceState` sealed class and `AttendanceStateStore` backed by DataStore/Room plus server reconciliation.
Implementation notes: Create `AttendanceControllerViewModel` with `StateFlow<AttendanceUiState>`. All UI shells observe it. Never let UI directly decide check-in/out from local booleans.
Test case: `ATT-STATE-001`, `ATT-STATE-002`, `ATT-STATE-003`.

Issue:
Severity: Critical
Module: Location tracking
File/Class if known: `LocationTrackingService`
Current behavior: Service posts locations directly to API, has no durable queue, no checked-in state validation, no restart recovery, no boot receiver.
Expected behavior: Tracking should persist/recover while checked in, queue offline locations if required, and stop reliably after checkout.
Risk: Employees can be checked in without tracking, or tracking can silently fail after process kill/network loss.
Recommended fix: Persist tracking session state, add location queue, use WorkManager for sync/retry, and reconcile service state against server on app/service start.
Implementation notes: Store `trackingSessionId`, `attendanceId`, `checkedIn=true`, `lastSyncAt`, and queued location rows in Room. Service only captures; repository queues/sends.
Test case: `LOC-001`, `LOC-004`, `LOC-006`, `LOC-008`.

Issue:
Severity: Critical
Module: Checkout gating
File/Class if known: `DashboardFragment.handleCheckOutResponse`, `DashboardViewModel.checkOutAttendance`
Current behavior: Home checkout calls checkout API first; if response code `3001`, UI asks user to enter work details or mark idle.
Expected behavior: Checkout should explicitly check work/day status before checkout. If incomplete, user should resolve work or mark idle, then checkout resumes.
Risk: Checkout flow depends on an error-like response; API ordering is fragile and user can be left in ambiguous state.
Recommended fix: Add explicit `getDayWorkStatus(date/attendanceId)` use case before checkout; only call checkout in `ReadyToCheckout`.
Implementation notes: Model states `WorkStatusChecking`, `WorkIncomplete`, `MarkingIdle`, `ReadyToCheckout`, `CheckingOut`.
Test case: `ATT-CHECKOUT-003`, `ATT-CHECKOUT-004`.

Issue:
Severity: High
Module: Security/privacy
File/Class if known: `NetModule`, `network_security_config.xml`, `LocationTrackingService`
Current behavior: BODY logging is enabled for all builds, cleartext is allowed for production host, and location coordinates/responses are logged.
Expected behavior: No sensitive logs in release; HTTPS enforced; location/token logs removed or gated.
Risk: Token, location, credentials, and attendance payloads can leak through logs or cleartext fallback.
Recommended fix: Gate logging to debug, redact headers, disable production cleartext, remove sensitive log statements.
Implementation notes: Use `if (BuildConfig.DEBUG) logging.level = BODY else NONE`; call `logging.redactHeader("Authorization")`.
Test case: `SEC-001`, `SEC-002`, `SEC-003`.

Issue:
Severity: High
Module: Permissions
File/Class if known: `DashboardFragment`, `SharedDashboardActivity`, `LoginActivity`
Current behavior: Background location, foreground location, and notification permissions can be requested as part of the same operational path.
Expected behavior: Foreground location first, then prominent disclosure, then Android settings/background permission path, then notification permission.
Risk: Denial loops, Play policy risk, and poor user recovery.
Recommended fix: Centralize permission orchestration with explicit state and recovery screens.
Implementation notes: `PermissionManager` should emit `PermissionRequired`, `LocationDisabled`, `NotificationDenied`, `PermanentlyDenied`.
Test case: `PERM-001` to `PERM-007`.

## 4. Attendance Flow Audit

Current Start Day flow:

1. UI fetches `attendance/checkin/status` on resume.
2. UI updates `isDayStarted` and slide button label.
3. User slides Start Day.
4. UI requests location/background/notification permissions.
5. UI reads `fusedLocationClient.lastLocation`.
6. `DashboardViewModel.checkInAttendance(lat, lon)` calls `attendance/checkin`.
7. On success, UI sets `isDayStarted = true`, updates button, refreshes calendar, and starts tracking service.

Problems:

- `lastLocation` can be null/stale; no active current-location request fallback.
- Button is reset after responses, but no centralized double-submit guard exists.
- UI relies on local boolean and current LiveData response, not a durable state.
- Service start failure is not surfaced into attendance state.
- If check-in succeeds but service start fails, UI still shows checked-in.
- On restart, UI calls status but does not ensure service recovery if status says checked in.
- `SharedDashboardActivity` and `DashboardFragment` duplicate logic and can drift.

Current End Day flow:

1. User slides End Day.
2. UI requests permissions and location.
3. UI calls `attendance/checkout` with `day_progress`.
4. On code `200`, UI stops tracking.
5. On code `3001`, UI shows "enter work details" or "mark idle".
6. Mark idle calls `attendance/emp/remarks/{attendance_id}` and then refreshes status.

Problems:

- No explicit work-status preflight API before checkout.
- "Enter work details" action is currently empty in `DashboardFragment.handleNoWorkForDay`.
- Mark idle does not automatically resume checkout after success.
- Calendar refresh after checkout is not explicit in every shell.
- Tracking is stopped only on successful checkout, which is correct, but stop failures are not handled.
- Token expiry or malformed responses during checkout can leave UI in unknown state.

Issue:
Severity: Critical
Module: Attendance checkout
File/Class if known: `DashboardFragment.handleNoWorkForDay`
Current behavior: Positive action for "enter work details" is empty.
Expected behavior: User should be routed to work details/start work flow or assigned work screen.
Risk: User cannot resolve incomplete work from the blocking dialog.
Recommended fix: Navigate to `AttendanceFragment` or a specific work detail screen with current date context.
Implementation notes: Inject nav event `NavigateToWorkForToday`; keep pending checkout intent so checkout can resume.
Test case: `ATT-CHECKOUT-005`.

Issue:
Severity: High
Module: Attendance check-in
File/Class if known: `DashboardFragment.manageDayStartEnd`, `SharedDashboardActivity.manageDayStartEnd`
Current behavior: Uses `lastLocation` only.
Expected behavior: Request fresh current location with timeout, accuracy validation, and fallback messaging.
Risk: Check-in can fail even when GPS is available; stale location can be submitted.
Recommended fix: Use `getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken)` or request one update.
Implementation notes: Reject locations older than configured threshold or accuracy worse than business threshold.
Test case: `LOC-003`, `ATT-CHECKIN-006`.

Issue:
Severity: High
Module: Attendance duplicate actions
File/Class if known: `DashboardFragment`, `SharedDashboardActivity`
Current behavior: Slide button is visually reset, but in-flight state is not authoritative.
Expected behavior: Once check-in/out starts, button must be disabled until terminal success/failure.
Risk: Duplicate API calls and inconsistent attendance.
Recommended fix: Add `operationInFlight` in state machine and server idempotency keys if backend supports them.
Implementation notes: Generate `requestId` per mutation; ignore button action unless state allows it.
Test case: `ATT-CHECKIN-002`, `ATT-CHECKOUT-002`.

## 5. Attendance State Machine

| State | Button label | Enabled | UI message | Allowed action | API call | Success next state | Failure next state |
|---|---|---:|---|---|---|---|---|
| `NotLoggedIn` | Login | Yes | Please login | Open login | none | `LoadingSession` | `Error` |
| `LoadingSession` | Loading | No | Checking session | none | read secure prefs, optional `employee/me` | `CheckingAttendanceStatus` | `NotLoggedIn` |
| `LoggedInNoAttendance` | Loading | No | Preparing attendance | none | none | `CheckingAttendanceStatus` | `Error` |
| `CheckingAttendanceStatus` | Loading | No | Checking today's attendance | none | `attendance/checkin/status` | `NotCheckedIn`, `CheckedInTrackingActive`, `CheckedInTrackingInactiveButShouldBeActive`, or `CheckedOut` | `NetworkUnavailable`, `SessionExpired`, `Error` |
| `NotCheckedIn` | Start Day | Yes | You have not started your day | Start check-in | none yet | `PermissionRequired` or `CheckingIn` | `Error` |
| `PermissionRequired` | Continue | Yes | Location/notification permission required | Request permission | none | `CheckingIn` or previous intended state | `Error` or `LocationDisabled` |
| `LocationDisabled` | Open Settings | Yes | Turn on device location | Open settings | none | `CheckingAttendanceStatus` | `Error` |
| `CheckingIn` | Starting... | No | Starting your day | none | `attendance/checkin` | `CheckedInTrackingActive` after service starts | `NotCheckedIn`, `SessionExpired`, `NetworkUnavailable`, `Error` |
| `CheckedInTrackingActive` | End Day | Yes | Day started. Tracking active. | End day | none yet | `WorkStatusChecking` | `Error` |
| `CheckedInTrackingInactiveButShouldBeActive` | Resume Tracking | Yes | Day started but tracking is inactive | Start/recover service | start service, optional status reconcile | `CheckedInTrackingActive` | `PermissionRequired`, `LocationDisabled`, `Error` |
| `WorkStatusChecking` | Checking... | No | Checking today's work | none | explicit work status API or `work/details` equivalent | `ReadyToCheckout` or `WorkIncomplete` | `NetworkUnavailable`, `SessionExpired`, `Error` |
| `WorkIncomplete` | Resolve Work | Yes | Complete work or mark Idle before checkout | Navigate to work or mark idle | none | `MarkingIdle` or work flow | `Error` |
| `MarkingIdle` | Marking Idle... | No | Marking today as idle | none | `attendance/emp/remarks/{attendance_id}` or dedicated idle API | `ReadyToCheckout` | `WorkIncomplete`, `SessionExpired`, `NetworkUnavailable`, `Error` |
| `ReadyToCheckout` | End Day | Yes | Ready to end day | Checkout | none | `CheckingOut` | `Error` |
| `CheckingOut` | Ending... | No | Ending your day | none | `attendance/checkout` | `CheckedOut` after tracking stopped | `CheckedInTrackingActive`, `SessionExpired`, `NetworkUnavailable`, `Error` |
| `CheckedOut` | Day Ended | No | Your day is complete | View details/calendar | `attendance/details` refresh | `CheckedOut` | `Error` |
| `NetworkUnavailable` | Retry | Yes | No internet connection | Retry last safe read/mutation | status or pending call | previous intended state | `Error` |
| `SessionExpired` | Login | Yes | Session expired | Logout and login | none | `NotLoggedIn` | `Error` |
| `Error` | Retry | Yes | Action failed | Retry/recover | context-specific | context-specific | `Error` |

Implementation guidance:

- Make `AttendanceState` a sealed interface with data payloads where needed.
- Keep mutations one-way: UI intent -> ViewModel use case -> state update -> UI render.
- Persist durable fields: `attendanceId`, `checkedIn`, `checkedOut`, `trackingShouldBeActive`, `lastKnownServerStateAt`, `pendingCheckoutAfterIdle`.
- On app start and on service start, reconcile local state with `attendance/checkin/status`.

## 6. API Reliability Audit

Attendance status API:

- Endpoint: GET `attendance/checkin/status`
- Current model: `CheckInStatusResponse` with non-null `attendanceId`, `checkedIn`, `checkedOut`, `checkinTime`, `checkoutTime`.
- Risks: Non-null model can crash/parse badly if server sends null; no domain mapping; no cache of last server state.
- Fix: Make response DTO nullable, map to domain `AttendanceSnapshot`, validate impossible combinations.

Check-in API:

- Endpoint: POST `attendance/checkin`
- Request: `latitude`, `longitude`.
- Risks: No request idempotency; no fresh-location validation; no explicit duplicate prevention; logs can expose payload.
- Fix: Add in-flight state, backend idempotency key if possible, fresh-location source and accuracy checks.

Check-out API:

- Endpoint: POST `attendance/checkout`
- Request: `latitude`, `longitude`, `day_progress`.
- Risks: Work validation is reactive; no pending checkout recovery; no explicit calendar refresh after every success path.
- Fix: Add pre-check work status use case, pending intent state, calendar refresh event on terminal checkout success.

Work status API:

- Current equivalent: GET `work/details`, GET `work/site/assigned`, GET `work/site/{work_site_id}/types`.
- Gap: No explicit "today work complete?" API is used before home checkout.
- Fix: Add or use a deterministic day-work-status endpoint before checkout.

Mark idle API:

- Current API: POST `attendance/emp/remarks/{attendance_id}`.
- Gap: Name/model implies remarks, not idle state; no automatic checkout resume after success.
- Fix: Prefer dedicated idle API or typed `markIdle(attendanceId, reason)` repository method.

Calendar attendance API:

- Endpoint: GET `attendance/details?month&year&is_export=true`
- Risks: Calendar displays local date derived from UTC string; timezone boundary can shift day.
- Fix: Backend should return canonical attendance date; app should render that date directly.

Day attendance details API:

- Current behavior: Day click passes existing `AttendanceRecord` to `AttendanceDetailActivity`; no fresh detail fetch on click.
- Risk: Details can be stale or incomplete.
- Fix: Fetch day detail by date/attendance id on click, with cached record as skeleton.

Day work details API:

- Endpoint: GET `work/details?date=...` exists through `AttendanceViewModel.workDetailsByDate`.
- Gap: Calendar day click currently does not call work details API from `AttendanceStatusFragment`.
- Fix: Day click should navigate to a screen that loads both attendance detail and work detail for selected date.

Issue:
Severity: High
Module: API error handling
File/Class if known: `BaseViewModel.executeApiCall`
Current behavior: All exceptions are wrapped as `ApiState.error(e)`; no typed network/server/session result.
Expected behavior: Centralized typed errors: network, timeout, unauthorized, server, validation, malformed.
Risk: UI shows weak messages and can miss session expiry if throwable casting is unsafe.
Recommended fix: Create `ApiResult<Success, ApiError>` and Retrofit error parser.
Implementation notes: Map `HttpException(401/403)` centrally and emit session-expired event.
Test case: `API-001`, `API-002`, `AUTH-006`.

Issue:
Severity: Medium
Module: Null safety
File/Class if known: `models/attendance/checkInStatus/Data.kt`
Current behavior: Response fields are non-null.
Expected behavior: DTO should tolerate missing/null fields and domain mapper should validate.
Risk: Malformed or partial API response can crash or produce incorrect state.
Recommended fix: Use nullable DTO fields and a mapper that returns `ApiError.MalformedResponse`.
Implementation notes: `data class CheckInStatusDto(val attendanceId: Long?, val checkedIn: Boolean?, ...)`.
Test case: `API-004`.

## 7. Location Tracking Audit

Current implementation:

- `LocationTrackingService` starts foreground immediately in `onStartCommand`.
- Uses `LocationRequest.Builder(15 * 60 * 1000L)` with high accuracy.
- Checks only fine location permission.
- Sends every received location directly to `geo-tracking/location`.
- Uses `START_STICKY`.
- No persisted queue, no retry policy, no service/session reconciliation, no boot receiver.

Required production behavior gaps:

- No offline queue.
- No WorkManager sync.
- No accuracy filtering.
- No stale location filtering.
- No duplicate location filtering.
- No retry/backoff for failed send.
- No persisted tracking session or attendance id.
- No clear handling if permission revoked while running.
- No GPS disabled recovery flow inside service.
- No boot/device restart recovery.
- No tracking status visible in dashboard beyond service notification.

Issue:
Severity: Critical
Module: Location tracking reliability
File/Class if known: `LocationTrackingService.sendLocationToServer`
Current behavior: Direct network call from service coroutine; exceptions are printed and dropped.
Expected behavior: Location events should be persisted then synced with retry/backoff.
Risk: Network loss silently loses tracking data.
Recommended fix: Room `LocationEventEntity` queue + WorkManager uploader.
Implementation notes: Service inserts event; uploader sends batches and marks synced. Include accuracy, provider, timestamp, attendance id.
Test case: `LOC-004`.

Issue:
Severity: High
Module: Service lifecycle
File/Class if known: `LocationTrackingService.onStartCommand`
Current behavior: Service starts tracking without checking current server attendance state.
Expected behavior: Service should verify local `trackingShouldBeActive` and optionally server status.
Risk: Sticky restart can track when not checked in, or fail to resume correctly when checked in.
Recommended fix: Inject `TrackingSessionStore`; stop if not active; recover if active.
Implementation notes: Persist session on check-in success before service start; clear only after checkout success and service stop.
Test case: `LOC-006`, `LOC-007`.

Issue:
Severity: High
Module: Location permissions
File/Class if known: `LocationTrackingService.checkLocationPermission`
Current behavior: Service checks only `ACCESS_FINE_LOCATION`.
Expected behavior: Track precise/coarse state and foreground/background grant state.
Risk: Service may stop even when coarse-only is acceptable, or fail to explain missing background access.
Recommended fix: Central permission evaluator returns domain status.
Implementation notes: If business requires precise, block with actionable UI; otherwise allow coarse with lower accuracy flag.
Test case: `PERM-005`.

## 8. Permission Handling Audit

Permission flow map:

First install:

1. User opens app.
2. No runtime permissions requested immediately.
3. Permissions are requested on location-related actions.

First Start Day tap:

1. Show background-location disclosure dialog in employee dashboard.
2. Request fine/coarse, background on Android 10+, notification on Android 13+.
3. On all granted, get last location and call check-in.
4. On denied, show rationale/settings.

Permission accepted:

- Continue to location retrieval and attendance mutation.

Permission denied:

- Current behavior varies by screen; some show rationale, some open settings.
- Expected: clear blocked state with retry and explanation.

Permission permanently denied:

- Current behavior opens app settings.
- Expected: explicit instructions for Location -> Allow all the time / precise / notifications.

Background permission required:

- Current behavior can request as part of multi-permission launcher.
- Expected: foreground first, then disclosure, then background settings flow.

Notification permission denied:

- Current behavior treats all permissions as required for tracking start.
- Expected: if Android requires foreground service notification visibility, block tracking with clear recovery or explain reduced behavior.

Location disabled:

- Current behavior often returns null location and shows generic unable-to-fetch message.
- Expected: detect `LocationManager`/settings and show "Turn on Location" action.

Recovery path:

- Return from settings -> re-run permission and attendance status checks -> resume intended action.

Issue:
Severity: High
Module: Permission UX
File/Class if known: `DashboardFragment.getRequiredPermissions`, `SharedDashboardActivity.getRequiredPermissions`
Current behavior: Background location and notification can be requested in same batch as foreground permissions.
Expected behavior: staged permission flow.
Risk: User denial and Play policy risk.
Recommended fix: Introduce `AttendancePermissionFlow`.
Implementation notes: Steps: foreground location -> precise check -> location services enabled -> notification -> background location -> check-in.
Test case: `PERM-001`, `PERM-003`, `PERM-004`.

Issue:
Severity: Medium
Module: App settings helper
File/Class if known: `Utils.openAppSettings`
Current behavior: Hardcoded package `com.atvantiq.parqngo`.
Expected behavior: Use `context.packageName`.
Risk: Wrong settings screen if used.
Recommended fix: Replace hardcoded package with app package.
Implementation notes: Audit callers; most screens use local correct method, but utility is unsafe.
Test case: `PERM-008`.

## 9. Foreground Service And Notification Audit

Current service declaration:

- Manifest declares `LocationTrackingService`.
- `android:foregroundServiceType="location"` is present.
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_LOCATION` permissions are declared.

Current notification:

- Channel ID: `location_service_channel`.
- Channel name: "Location Tracking".
- Importance: HIGH.
- Notification title: `location_tracking_active`.
- Text: `your_location_tracked`.
- Ongoing: true.
- AutoCancel: false.
- Visibility: public.
- Tap opens `SplashActivity`.

Strengths:

- `startForeground()` is called promptly in `onStartCommand`.
- PendingIntent is immutable.
- Ongoing notification is used.

Gaps:

- No action buttons such as "Open attendance" or "End day" if product allows.
- Importance HIGH may be noisy for a persistent operational status.
- No notification update with last sync/online/offline/tracking problem.
- No check to avoid duplicate `requestLocationUpdates` on repeated service starts.
- Stop behavior relies on `stopService`; no explicit `stopForeground` call before destroy.

Issue:
Severity: Medium
Module: Foreground notification
File/Class if known: `LocationTrackingService.buildNotification`
Current behavior: High-importance public ongoing notification with generic text.
Expected behavior: Clear, calm, privacy-appropriate text: "WFMS is tracking location during your active work day."
Risk: Poor user trust and notification fatigue.
Recommended fix: Use default/low importance if acceptable and add explicit tracking context.
Implementation notes: Consider channel migration behavior; Android channel importance cannot be lowered after creation without new ID.
Test case: `FGS-002`.

Issue:
Severity: High
Module: Duplicate service starts
File/Class if known: `LocationTrackingService.onStartCommand`
Current behavior: Every start command calls `startLocationUpdates`; `isServiceRunning` is process-local only.
Expected behavior: Idempotent service start.
Risk: Duplicate callbacks or inconsistent service state after repeated starts.
Recommended fix: Track active callback registration and ignore duplicate starts.
Implementation notes: Use `isUpdatingLocation` and synchronize start/stop.
Test case: `FGS-004`.

## 10. Calendar Attendance Audit

Current behavior:

- `AttendanceStatusFragment` fetches current month on view creation.
- Calendar refresh is triggered by `AttendanceCommunicationViewModel` after check-in and attendance remarks.
- Records map to `AttendanceDay` by check-in time or createdAt.
- Day click opens `AttendanceDetailActivity` only when a record exists.
- Month navigation fetches attendance details.

Strengths:

- Month paging exists.
- Summary counts are computed.
- Empty and loading states exist.

Gaps:

- Checkout does not consistently trigger calendar refresh.
- Day click does not fetch fresh attendance details or work details API.
- Timezone conversion from UTC to local date can shift attendance day.
- Future dates are counted as no API days because `CalendarView` counts every day with no data.
- Rotation state for selected month/date is not persisted.
- `CalendarView.isInvalidDate()` exists but is unused.

Issue:
Severity: High
Module: Calendar refresh
File/Class if known: `DashboardFragment.handleCheckOutResponse`, `SharedDashboardActivity.handleCheckOutResponse`
Current behavior: Check-in triggers calendar refresh in employee dashboard; checkout does not consistently trigger it.
Expected behavior: Calendar refresh after check-in, checkout, mark idle, leave, and work-end if status changes.
Risk: User sees stale attendance status.
Recommended fix: Emit `AttendanceChanged(date)` event from attendance state machine on every terminal mutation.
Implementation notes: Calendar should observe a shared `AttendanceEvents` flow.
Test case: `CAL-001`, `CAL-002`.

Issue:
Severity: Medium
Module: Calendar date handling
File/Class if known: `AttendanceStatusFragment.handleAttendanceDetailsResponse`, `DateUtils.formatApiDateToYMD`
Current behavior: Converts UTC timestamp to local date.
Expected behavior: Use server-provided attendance date or explicit timezone policy.
Risk: Late-night check-in/checkout can appear on wrong day.
Recommended fix: Ask backend for canonical `attendance_date`; otherwise document timezone and test midnight cases.
Implementation notes: Use java.time on API 26+ or desugaring.
Test case: `CAL-006`, `CAL-007`.

## 11. Auth Section Audit And Enhancement Plan

Current auth behavior:

- Splash reads encrypted token and user JSON.
- No token validation call before routing except later screens fetch `employee/me`/status.
- Login stores token/user in encrypted prefs.
- OTP login and password login share success flow.
- Logout clears secure prefs and deletes FCM token.
- Token expiry is handled ad hoc with `tokenExpiresAlert()` in many screens.

Gaps:

- No central `SessionManager`.
- No token refresh support visible.
- Splash can route with expired token and only later force logout.
- `onNewToken` does not upload refreshed FCM token.
- Unauthorized handling is duplicated.
- Some error handlers cast throwable to `HttpException` unsafely.

Issue:
Severity: High
Module: Session management
File/Class if known: `SplashActivity`, `BaseActivitySimple`, `BaseFragmentSimple`
Current behavior: Session validity is inferred from token presence.
Expected behavior: Session validity should be verified or expired deterministically.
Risk: Users can enter app with expired token and hit inconsistent screens/dialogs.
Recommended fix: Add `SessionManager` with `sessionState: StateFlow<SessionState>`.
Implementation notes: On app start: read token -> call lightweight `employee/me` or status -> route.
Test case: `AUTH-006`, `AUTH-008`.

Issue:
Severity: Medium
Module: FCM auth integration
File/Class if known: `MyFirebaseMessagingService.onNewToken`
Current behavior: New token is logged only.
Expected behavior: Send token if authenticated or persist for next authenticated session.
Risk: Push notifications can stop after token rotation.
Recommended fix: Persist pending FCM token and upload via auth repository.
Implementation notes: Use DataStore key `pendingFcmToken`.
Test case: `AUTH-010`.

## 12. UI/UX Refinement Plan

Home/attendance:

- Show explicit status card: "Not checked in", "Checked in, tracking active", "Checked in, tracking problem", "Checked out".
- Show last server sync time and last location sync time.
- Disable attendance button during all mutation states.
- Add retry button for status load failure.
- Add "Tracking needs attention" banner if checked in but service inactive.

Permission prompts:

- Replace generic permission dialogs with staged, business-specific copy.
- Provide settings recovery checklist for background location and notifications.

Checkout blocked flow:

- Explain why checkout is blocked.
- Make "Start/complete work" action navigate to the work list/detail.
- Make "Mark Idle" explain what idle means.
- After idle success, automatically continue checkout or ask "End Day now?".

Calendar:

- Mark future days as future/disabled, not no data.
- Show skeleton/progress only inside calendar panel.
- Day click should load both attendance and work details.

Auth:

- Splash should show "Checking session" and route only after session validation when possible.
- Session expired dialog should clear stack.
- OTP should show expiry/resend timer if backend supports it.

## 13. Robustness Edge Cases

Issue:
Severity: High
Module: App restart while checked in
File/Class if known: `DashboardFragment.onResume`, `DashboardViewModel.startTracking`
Current behavior: If status says checked in, UI calls `checkPermissionForLiveLocation()` and starts service if permissions exist.
Expected behavior: Restart should reconcile server state, local tracking session, and service actual state.
Risk: App may show checked-in while tracking inactive or repeatedly start service.
Recommended fix: Add tracking recovery use case and idempotent service controller.
Implementation notes: On resume: fetch status -> compare persisted tracking -> start/stop service accordingly.
Test case: `ATT-STATE-003`, `LOC-006`.

Issue:
Severity: Medium
Module: Malformed API response
File/Class if known: API DTOs
Current behavior: DTOs often use non-null fields.
Expected behavior: Malformed response should become user-visible retryable error.
Risk: Crash or invalid UI state.
Recommended fix: DTO nullable + mapper validation.
Implementation notes: Keep domain models non-null after validation.
Test case: `API-004`.

## 14. Performance Audit

Findings:

- Location interval is 15 minutes high accuracy; battery impact is moderate but acceptable only if business needs high accuracy.
- Direct network call on every location can wake radio each interval and lose data offline.
- BODY logging in release is heavy and risky.
- Calendar re-renders 42 cells on each data set; acceptable for now.
- Geocoder calls can be slow; most are asynchronous or callback-based on newer Android, but older API path can block.
- Progress dialogs are modal and can feel heavy.

Recommendations:

- Use balanced power or adaptive priority unless a work action requires high accuracy.
- Batch offline location uploads.
- Use WorkManager constraints for sync on network available.
- Remove BODY logging in release.
- Avoid geocoding during critical check-in/out mutation path unless required.
- Add performance metrics: app start, status API latency, check-in latency, service start latency, first location time.

## 15. Security And Privacy Audit

Issue:
Severity: High
Module: Sensitive logging
File/Class if known: `NetModule`, `LocationTrackingService`, `MyFirebaseMessagingService`
Current behavior: BODY logs, FCM token/payload logs, coordinates logs.
Expected behavior: Sensitive data should not be logged in release.
Risk: Data leakage.
Recommended fix: Gate/remove logs, redact headers.
Implementation notes: Add custom logger wrapper with build-type policy.
Test case: `SEC-001`.

Issue:
Severity: High
Module: Network security
File/Class if known: `network_security_config.xml`
Current behavior: Cleartext allowed for prod/dev domains.
Expected behavior: HTTPS only in prod.
Risk: MITM if http endpoint is used.
Recommended fix: Remove prod cleartext; use debug-only network config for dev IPs.
Implementation notes: Configure per-flavor manifest/network config.
Test case: `SEC-002`.

Issue:
Severity: High
Module: Backup/security
File/Class if known: `backup_rules.xml`, `data_extraction_rules.xml`, `PrefKeys`
Current behavior: Excludes `wfms_secure_pref.xml`; actual name appears to be `wfms_secure_prefs`.
Expected behavior: Secure prefs and crypto keysets excluded.
Risk: Sensitive encrypted blobs/keysets transferred/backed up unintentionally.
Recommended fix: Correct exclude paths and verify generated files.
Implementation notes: Exclude `wfms_secure_prefs.xml` and `__androidx_security_crypto_encrypted_prefs_keyset__.xml`.
Test case: `SEC-004`.

Issue:
Severity: Medium
Module: File sharing
File/Class if known: `path_provider.xml`
Current behavior: Broad roots include all external storage.
Expected behavior: Narrow app-specific roots.
Risk: Future URI bugs can expose unintended files.
Recommended fix: Scope provider paths to `WFMS/` and cache attachment dirs.
Implementation notes: Create `cache-path name="attachments" path="attachments/"`.
Test case: `SEC-005`.

## 16. Code Architecture Refinement Plan

Target structure:

```text
data/api
  ApiService.kt
  dto/...
  ApiErrorMapper.kt
data/model
  attendance/AttendanceDtos.kt
  auth/AuthDtos.kt
data/repository
  AttendanceRepository.kt
  TrackingRepository.kt
  AuthRepository.kt
domain/model
  AttendanceState.kt
  AttendanceSnapshot.kt
  TrackingSession.kt
domain/usecase
  ObserveAttendanceStateUseCase.kt
  RefreshAttendanceStatusUseCase.kt
  CheckInUseCase.kt
  CheckWorkStatusUseCase.kt
  MarkIdleUseCase.kt
  CheckOutUseCase.kt
  RecoverTrackingUseCase.kt
service/location
  LocationTrackingService.kt
  LocationServiceController.kt
  LocationEventQueue.kt
ui/auth
ui/home
ui/attendance
ui/calendar
utils/permissions
  AttendancePermissionManager.kt
utils/session
  SessionManager.kt
utils/network
  NetworkMonitor.kt
```

Code-level recommendations:

- Replace per-screen attendance booleans with `AttendanceState`.
- Use `StateFlow` for attendance UI state and one-shot `SharedFlow` for events.
- Keep repositories free of Android UI concerns.
- Add use cases for check-in/check-out sequencing.
- Add `SessionManager` that receives all 401/403 events centrally.
- Add `LocationServiceController` that is idempotent and testable.
- Add Room for location queue and optional attendance snapshot cache.
- Use typed request DTOs instead of ad hoc `JsonObject` for attendance mutations.
- Use `ApiResult` with typed `ApiError`.
- Make permission handling reusable and state-driven.

## 17. Testing Plan

Manual testing checklist:

- Fresh install, login, check-in, verify notification, background app, verify location sync, return, checkout, verify notification removed.
- Already checked-in user opens app after process kill; service recovery behavior.
- Checkout with complete work.
- Checkout with incomplete work; start work path.
- Checkout with incomplete work; mark idle path.
- Calendar updates after check-in, checkout, and idle.
- Token expiry during status/check-in/checkout.
- Network loss before/during/after location sync.
- Permission denial/permanent denial/settings recovery.

Unit tests:

- `AttendanceStateReducerTest`
- `CheckInUseCaseTest`
- `CheckOutUseCaseTest`
- `MarkIdleUseCaseTest`
- `ApiErrorMapperTest`
- `SessionManagerTest`
- `DateMapperTest`

ViewModel tests:

- Button state for all attendance states.
- Duplicate action ignored while in-flight.
- Check-in success starts service command.
- Check-in success but service start failure creates warning state.
- Checkout incomplete transitions to `WorkIncomplete`.
- Mark idle success transitions to `ReadyToCheckout`.

Repository/API mock tests:

- MockWebServer for all attendance APIs.
- 200/206/3001/400/401/500/timeouts/malformed JSON.
- Verify auth header.
- Verify request body.

Permission tests:

- Robolectric/unit tests for permission state mapper.
- Instrumented tests for denied/permanent denied where feasible.

Location/foreground service tests:

- Service starts foreground within Android requirement.
- Duplicate start does not duplicate callbacks.
- Stop removes location updates.
- Offline event is queued.
- Queue flushes on network restored.

UI tests:

- Espresso for login, dashboard state, button disabled/loading, checkout blocked dialog, calendar day click.
- Use fake repositories or mock server.

Regression tests:

- Every bug in section 18 gets at least one permanent regression test.

Device matrix:

- Android 8, 9, 10, 11, 12, 13, 14, 15 if available.
- Pixel/reference device.
- Samsung.
- Xiaomi/MIUI.
- OnePlus/Oppo/Vivo if possible.
- Low RAM device.
- Battery saver enabled.

Test case format examples:

Test ID: ATT-CHECKIN-001
Module: Attendance
Scenario: First successful Start Day
Preconditions: Logged in, not checked in, all permissions granted, GPS enabled, network available.
Steps: Open dashboard; wait for status; tap Start Day; allow location; wait for API success.
Expected result: Button changes to End Day; tracking service starts; notification visible; calendar refreshes today.
Actual result: To be recorded during execution.
Priority: P0
Automation possible: Yes

Test ID: ATT-CHECKIN-002
Module: Attendance
Scenario: Double tap Start Day
Preconditions: Logged in, not checked in, network slow.
Steps: Trigger Start Day twice quickly.
Expected result: Only one check-in request is sent; button disabled while loading; final state correct.
Actual result: To be recorded during execution.
Priority: P0
Automation possible: Yes

Test ID: ATT-CHECKOUT-003
Module: Attendance
Scenario: Checkout blocked by incomplete work
Preconditions: Logged in, checked in, tracking active, backend reports incomplete work.
Steps: Tap End Day.
Expected result: App checks work status; checkout is not called until work complete or idle marked; user sees clear options.
Actual result: Current implementation calls checkout and reacts to code `3001`.
Priority: P0
Automation possible: Yes

Test ID: ATT-CHECKOUT-004
Module: Attendance
Scenario: Mark Idle then checkout
Preconditions: Checked in with no completed work.
Steps: Tap End Day; choose Mark Idle; enter remarks; submit; continue checkout.
Expected result: Idle API succeeds; checkout resumes; service stops; calendar updates.
Actual result: Current implementation refreshes status but does not clearly resume checkout.
Priority: P0
Automation possible: Yes

Test ID: LOC-004
Module: Location
Scenario: Network lost while checked in
Preconditions: Checked in, tracking active.
Steps: Disable network; wait for location interval; enable network.
Expected result: Location event is queued and synced later; no data loss.
Actual result: Current implementation drops failed send.
Priority: P0
Automation possible: Partially

Test ID: PERM-003
Module: Permissions
Scenario: Background location denied
Preconditions: Android 10+, logged in, foreground location granted.
Steps: Start Day; deny background location.
Expected result: App explains why tracking cannot continue and provides settings recovery.
Actual result: To be recorded.
Priority: P0
Automation possible: Partially

Test ID: CAL-002
Module: Calendar
Scenario: Calendar updates after checkout
Preconditions: Checked in, calendar visible for current month.
Steps: Checkout successfully.
Expected result: Current day status updates without full app restart.
Actual result: Current implementation is inconsistent.
Priority: P1
Automation possible: Yes

Test ID: AUTH-006
Module: Auth
Scenario: Token expires during check-in
Preconditions: Logged in with expired token, not checked in.
Steps: Tap Start Day.
Expected result: App shows session expired, clears session, navigates to login with cleared back stack.
Actual result: To be recorded.
Priority: P0
Automation possible: Yes

## 18. Bug List

Issue:
Severity: Critical
Module: Attendance
File/Class if known: `DashboardFragment.handleNoWorkForDay`
Current behavior: "Enter work details" action is empty.
Expected behavior: User navigates to work resolution flow.
Risk: Checkout blocker cannot be resolved from dialog.
Recommended fix: Navigate to assigned work/work details and preserve pending checkout.
Implementation notes: Add nav event and pending checkout state.
Test case: `ATT-CHECKOUT-005`.

Issue:
Severity: Critical
Module: Location
File/Class if known: `LocationTrackingService`
Current behavior: Failed location sends are dropped.
Expected behavior: Failed sends are queued and retried.
Risk: Lost tracking data.
Recommended fix: Room queue + WorkManager.
Implementation notes: Persist first, then sync.
Test case: `LOC-004`.

Issue:
Severity: Critical
Module: Attendance state
File/Class if known: `DashboardFragment`, `SharedDashboardActivity`
Current behavior: Local `isDayStarted` drives button state.
Expected behavior: Server-reconciled state machine drives button.
Risk: Wrong check-in/out state.
Recommended fix: Central `AttendanceState`.
Implementation notes: All shells observe one ViewModel/use case.
Test case: `ATT-STATE-001`.

Issue:
Severity: High
Module: API/security
File/Class if known: `NetModule`
Current behavior: BODY logging enabled globally.
Expected behavior: Debug-only redacted logging.
Risk: Sensitive data leak.
Recommended fix: Gate logging and redact headers.
Implementation notes: Use app `BuildConfig.DEBUG`.
Test case: `SEC-001`.

Issue:
Severity: High
Module: Network security
File/Class if known: `network_security_config.xml`
Current behavior: Cleartext permitted for prod domain.
Expected behavior: HTTPS only for prod.
Risk: Transport security weakness.
Recommended fix: Flavor-specific config.
Implementation notes: Remove prod from cleartext domain-config.
Test case: `SEC-002`.

Issue:
Severity: High
Module: Permission flow
File/Class if known: `DashboardFragment`, `SharedDashboardActivity`
Current behavior: Background permission is requested with foreground/notification permissions.
Expected behavior: Staged flow.
Risk: Denial and policy risk.
Recommended fix: Central permission manager.
Implementation notes: Foreground -> disclosure -> background settings -> notification.
Test case: `PERM-001`.

Issue:
Severity: High
Module: Calendar
File/Class if known: `AttendanceStatusFragment`
Current behavior: Day click does not call day work details API.
Expected behavior: Day click loads attendance and work details.
Risk: Incomplete day view.
Recommended fix: Add day details coordinator.
Implementation notes: Pass date, not just parcelled record.
Test case: `CAL-004`.

Issue:
Severity: Medium
Module: Auth
File/Class if known: `SplashActivity`
Current behavior: Token presence routes to home.
Expected behavior: Validate session before home or fail gracefully.
Risk: Expired sessions enter app.
Recommended fix: SessionManager validation.
Implementation notes: lightweight authenticated call.
Test case: `AUTH-008`.

Issue:
Severity: Medium
Module: File/security
File/Class if known: `path_provider.xml`
Current behavior: Broad provider paths.
Expected behavior: Narrow provider roots.
Risk: Accidental file exposure.
Recommended fix: Scope to app attachment dirs.
Implementation notes: Update capture/cache paths accordingly.
Test case: `SEC-005`.

Issue:
Severity: Medium
Module: Backup
File/Class if known: `backup_rules.xml`, `data_extraction_rules.xml`
Current behavior: Secure pref exclusion likely wrong filename.
Expected behavior: Correct secure pref/keyset exclusions.
Risk: Sensitive backup leakage.
Recommended fix: Correct paths and verify on device.
Implementation notes: Check actual generated XML names.
Test case: `SEC-004`.

## 19. Enhancement List

1. Add explicit attendance state machine and state reducer.
2. Add use cases for CheckIn, CheckWorkStatus, MarkIdle, CheckOut, RecoverTracking.
3. Add Room-backed offline location queue.
4. Add WorkManager sync for queued location events.
5. Add dashboard tracking health indicator.
6. Add staged permission recovery UI.
7. Add day details screen that loads attendance and work details by date.
8. Add analytics events for check-in/out attempts and failures, without sensitive data.
9. Add MockWebServer API reliability tests.
10. Add device matrix regression suite for Android 10+ background location behavior.

## 20. Phase 1 Release Checklist

Must complete before Phase 1 release:

- [ ] Central attendance state machine implemented.
- [ ] Duplicate check-in/check-out prevention implemented and tested.
- [ ] Explicit checkout work-status preflight or deterministic replacement implemented.
- [ ] "Enter work details" checkout-blocked action implemented.
- [ ] Mark Idle resumes checkout or shows clear next action.
- [ ] Tracking session persisted after check-in and cleared after checkout.
- [ ] Location service idempotent and recoverable.
- [ ] Offline location queue implemented or business explicitly accepts data loss.
- [ ] Calendar refreshes after check-in, checkout, and idle.
- [ ] Token expiry handled centrally with cleared back stack.
- [ ] Release BODY logging disabled.
- [ ] Production cleartext disabled.
- [ ] Background location permission flow staged and policy-safe.
- [ ] P0 tests pass on Android 10, 13, and 14 at minimum.

## 21. Final Verdict

App reliability score: 62/100

Attendance module score: 58/100

Location tracking score: 45/100

Auth readiness score: 70/100

Calendar module score: 64/100

Production readiness score: 55/100

Top 10 must-fix issues:

1. Add central attendance state machine.
2. Add durable tracking session state and recovery.
3. Add offline location queue/retry or explicitly document no offline tracking guarantee.
4. Fix checkout incomplete-work flow and implement "enter work details" action.
5. Prevent duplicate attendance mutations.
6. Stage background location and notification permission flow.
7. Disable sensitive release logging.
8. Enforce HTTPS for production.
9. Refresh calendar after checkout/idle.
10. Centralize token expiry/session handling.

Top 10 enhancements:

1. Tracking health UI on home.
2. Fresh current-location request with accuracy/staleness validation.
3. Work status preflight API/use case.
4. Day detail screen that loads attendance and work details.
5. MockWebServer test suite.
6. WorkManager batch sync.
7. Typed request DTOs.
8. ApiResult/error mapper.
9. SessionManager.
10. PermissionManager.

Ready for production: No.

Phase 1 can be released: Not as "production-grade attendance." It can be released only as an internal/staged build after P0 attendance, checkout, tracking, permission, and security fixes are completed and tested.

What must be done before release:

- Build the attendance state machine and route both employee/admin shells through it.
- Make tracking durable and recoverable.
- Fix checkout gating and idle flow.
- Harden permissions for Android 10, 13, and 14+.
- Remove sensitive logging and cleartext production allowance.
- Add P0 automated/manual coverage from the testing plan.
