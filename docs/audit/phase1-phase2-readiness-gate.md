# Phase 1 And Phase 2 Readiness Gate

Date: 2026-07-03
Branch: code-audit

## Scope

This readiness gate covers the completed Phase 1 and Phase 2 work:

- Attendance check-in/check-out flow
- Foreground/background location tracking
- Persistent location notification behavior
- Attendance calendar refresh and detail flow
- Session/logout/token-expiry handling touched during the audit
- Work Management assigned work list/detail flow
- Work accept/start/end flow
- Self-assign work flow
- Material usage handoff from End Work
- Reimbursement claim list/detail/create flow
- Single-site and multi-site claim logic
- Local and outstation claim logic
- Receipt/photo attachment handling
- Security/privacy hardening completed during the audit

## Automated Verification

Command executed:

```bash
./gradlew :app:assembleDevDebug :app:testDevDebugUnitTest :app:lintDevDebug
```

Result: Passed

Verified:

- Dev debug APK compiles successfully.
- Dev debug unit tests pass.
- Android lint dev debug passes with no blocking errors.
- Previous lint blocker was fixed: stale manifest reference to missing `AddMaterialActivity`.
- Existing AGP/KAPT tool warnings remain informational and do not block the build.

## Targeted Static Checks

Checks run with `rg` across app code, manifest, navigation, and audit docs.

Passed:

- No `TODO()` crash paths remain in app source.
- No stale `AddMaterialActivity` manifest reference remains.
- No `materialReco.add` stale manifest reference remains.
- No fake no-internet API calls remain for self-assign or create-claim submit.
- No unsafe `as HttpException` casts remain in audited paths.
- Work Management fragment no longer stops attendance tracking from `onDestroyView`.
- Sensitive location/work-id debug logs removed from the location service.
- Calendar/FCM debug logs are gated for debug builds where applicable.

## Fixes Completed During This Gate

Issue:
Severity: High
Module: Android Manifest / Material Navigation
File/Class if known: `AndroidManifest.xml`, `PurchaseFragment`
Current behavior: Manifest referenced a missing `AddMaterialActivity`, causing lint failure.
Expected behavior: Manifest must reference only real activities.
Risk: Release gate failure and stale navigation/code health risk.
Recommended fix: Removed missing activity registration and added a safe placeholder `PurchaseFragment` for the existing material navigation destination.
Implementation notes: Material drawer item remains inactive/under development; placeholder prevents navigation/lint breakage without inventing incomplete behavior.
Test case: Run `:app:lintDevDebug`; expected no `MissingClass` error.

Issue:
Severity: High
Module: Dashboard
File/Class if known: `DashboardFragment`
Current behavior: Several dashboard click branches used `TODO()`.
Expected behavior: No user-triggered branch should crash the app.
Risk: Guaranteed runtime crash if those click events fire.
Recommended fix: Replaced unfinished actions with safe under-development feedback and added logout confirmation handling.
Implementation notes: Existing active flows remain unchanged.
Test case: Trigger each dashboard click event; expected no crash.

Issue:
Severity: Medium
Module: Self Assign / Reimbursement Submit
File/Class if known: `AddSignInVM`, `CreateClaimVM`
Current behavior: No-internet short-circuit used dummy repository calls that were not executed but made the code misleading.
Expected behavior: No-internet should emit explicit error state.
Risk: Poor maintainability and misleading test/debug behavior.
Recommended fix: Emit `ApiState.error(NoInternetException(...))` directly.
Implementation notes: UI behavior remains the same.
Test case: Disable internet and submit; expected no repository call and visible error handling.

Issue:
Severity: Medium
Module: Location Tracking / Calendar / FCM
File/Class if known: `LocationTrackingService`, `AttendanceStatusFragment`, `MyFirebaseMessagingService`
Current behavior: Some debug logs were ungated or contained unnecessary operational details.
Expected behavior: Release builds should avoid sensitive or noisy logging.
Risk: Privacy/log noise.
Recommended fix: Removed work-id log and gated debug informational logs.
Implementation notes: Error logs remain for operational failures.
Test case: Release logging review; expected no location/work payload debug logs.

## Phase 1 Status

Attendance:

- Check-in/check-out duplicate-action protections added.
- Tracking active flag persists through secure preferences.
- Foreground service checks persisted tracking state before running.
- Location event queue added for failed/offline sync recovery.
- Checkout and dashboard state reset more reliably after success/error.
- Calendar refresh is triggered after attendance state changes.
- Logout/token-expiry cleanup redirects through a clear task.
- Build, unit tests, and lint pass.

Remaining Phase 1 validation:

- Real-device foreground service behavior across Android 10-15.
- OEM background restrictions on Samsung/Xiaomi/Oppo/Vivo/OnePlus.
- Live API tests for check-in/check-out/work-status/idle edge cases.
- Physical GPS disabled/revoked permission recovery.

## Phase 2 Status

Work Management:

- Assigned work list pagination/search refresh logic reviewed.
- Work detail unsafe error handling fixed.
- Accept/start/end duplicate transition protection added.
- Location permission grant now resumes pending Start/End Work.
- Invalid work IDs are blocked from API calls.
- Start Work image path uses compressed file path with fallback.
- Self-assign requires selected activities and sends selected activity IDs.
- Material usage handoff remains build-verified.

Reimbursement:

- Claim list pagination refresh fixed.
- Claim detail Hilt entry point fixed.
- Invalid claim ID handled safely.
- Create-claim duplicate submit protection added.
- Missing receipt files skipped safely.
- Sensitive claim logs removed.
- Travel amount validation requires positive numeric value.
- Receipt image paths use compressed file path with fallback.
- Multi-site PO picker is safer during filtering/rebinding.

Remaining Phase 2 validation:

- Live backend tests for assigned work accept/start/end transitions.
- Camera/gallery capture on real devices.
- Material usage with real inventory/project data.
- Single-site and multi-site reimbursement submit against live backend.
- Local/outstation combinations including hotel, travel, DA, and other entries.

## Gate Result

Local automated gate: Passed

Phase 1/2 code readiness for moving to Phase 3: Conditionally approved

Condition:

- It is safe to start Phase 3 from a code/build/lint/unit-test perspective.
- Before production release, real-device and live-backend QA must still be completed for location, camera, foreground service, material usage, and reimbursement permutations.

## Final Verdict

The completed Phase 1 and Phase 2 work is functional and refined enough to proceed to Phase 3 development. The codebase now passes compile, unit tests, and lint for the dev debug variant, and the latest readiness sweep fixed the remaining blocking/stale/crash-prone issues discovered during verification.
