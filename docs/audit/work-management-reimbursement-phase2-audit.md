# Phase 2 Audit: Work Management And Reimbursement

Date: 2026-07-03
Branch: code-audit

## Scope

- Work Management side-nav section: assigned work list, search, filters, pagination, detail screen, accept/start/end transitions, self-assign flow, and material usage before work completion.
- Reimbursement side-nav section: claim list, claim detail navigation, create-claim flow, single-site/multiple-site conditions, local/outstation conditions, expense entry handling, and claim submission.

## Critical Fixes Applied

### Work Management

Issue:
Severity: Critical
Module: Attendance / Work Management
File/Class if known: `AttendanceFragment.onDestroyView`
Current behavior: Opening or leaving the Work Management fragment stopped attendance tracking through `viewModel.stopTracking()`.
Expected behavior: Day-level tracking must stop only after successful checkout or session/logout recovery.
Risk: Employees could remain checked in while background tracking silently stops.
Recommended fix: Removed the tracking stop from fragment destruction and left tracking control to checkout/session lifecycle.
Implementation notes: Also removed pending search callbacks during view cleanup to avoid stale UI work.
Test case: Navigate from Dashboard to Work Management and back while checked in; verify foreground notification and location service remain active.

Issue:
Severity: High
Module: Assigned Work Detail
File/Class if known: `AssignedTaskDetailActivity`
Current behavior: API error handling force-cast `Throwable` to `HttpException`.
Expected behavior: Network errors, timeout errors, no-internet errors, and HTTP errors should all be handled safely.
Risk: Non-HTTP failures could crash the detail screen.
Recommended fix: Replaced unsafe cast with type-checking and retained token-expiry handling.
Test case: Simulate timeout/no-internet on work detail API; screen must show an actionable error and not crash.

Issue:
Severity: High
Module: Assigned Work Detail
File/Class if known: `AssignedTaskDetailActivity`
Current behavior: Accept/start/end actions could be tapped repeatedly before API state completed.
Expected behavior: Work status transitions should be single-flight.
Risk: Duplicate accept/start/end requests and stale UI state.
Recommended fix: Added in-flight gating and temporary button disabling during transition handling.
Test case: Rapid tap Accept/Start/End ten times; only one corresponding API transition should be issued.

Issue:
Severity: High
Module: Self Assign
File/Class if known: `AddSignInActivity`, `AddSignInVM`
Current behavior: The activity selection list was stored separately, but the self-assign payload used `type.activities` from the type model.
Expected behavior: Payload must use the activity IDs selected by the employee.
Risk: Wrong activities may be assigned, especially when type API responses do not contain the user's selected activities.
Recommended fix: Selecting a type now refreshes activities, activity selection is required, and the request sends selected activity IDs.
Test case: Select type with stale embedded activities, select activity IDs 77 and 88, submit; API payload must contain 77 and 88.

Issue:
Severity: High
Module: Assigned Work Detail
File/Class if known: `AssignedTaskDetailActivity`
Current behavior: If location permission was requested from Start/End Work, granting permission did not resume the pending action.
Expected behavior: Permission approval should continue the exact pending Start/End Work action.
Risk: First-time users could grant permission and still see no work transition.
Recommended fix: Added a pending location action and resume/cancel handling for grant, denial, and rationale cancellation.
Test case: Fresh install, open WIP task, tap End Work, grant location permission; end-work bottom sheet should open without needing a second tap.

Issue:
Severity: Medium
Module: Start Work Photo
File/Class if known: `StartWorkBottomSheet`
Current behavior: The compressed file path could be null while the preview showed success.
Expected behavior: The submitted file path should be compressed path when available, otherwise original path.
Risk: Start Work may fail after user captured a valid photo.
Recommended fix: Added safe fallback from compression result to original image path.
Test case: Capture an image where compression returns null; Start Work should still submit with a valid path.

### Reimbursement

Issue:
Severity: Medium
Module: Reimbursement List
File/Class if known: `ReimbursementFragment`
Current behavior: Swipe refresh bypassed the guarded pagination loader.
Expected behavior: Refresh should reuse the same `isLoading` and `isLastPage` control path as scrolling.
Risk: Duplicate list calls and inconsistent loading footer/empty state.
Recommended fix: Refresh now resets pagination flags and calls `getAllClaims()`.
Test case: Swipe refresh while near the bottom of the list; verify one page-1 request and no duplicate footer.

Issue:
Severity: High
Module: Create Claim
File/Class if known: `CreateClaimVM`, `CreateClaimActivity`
Current behavior: Claim submission had no single-flight guard and logged full claim JSON/file metadata.
Expected behavior: Submission should be idempotent on the client and must not log claim details.
Risk: Duplicate claims and sensitive reimbursement/location/site data in logs.
Recommended fix: Added submit guard/UI disabling and removed sensitive logs.
Test case: Rapid tap Submit ten times; only one create-claim request should be sent.

Issue:
Severity: Medium
Module: Create Claim
File/Class if known: `CreateClaimVM`
Current behavior: Receipt attachment file parts were built from paths without checking file existence.
Expected behavior: Missing/stale file paths should not crash payload construction.
Risk: Claim submit can fail locally after media cleanup or file-provider changes.
Recommended fix: File parts are now attached only when the file exists and is a regular file.
Test case: Add an expense with a stale receipt path and submit; app should not crash.

Issue:
Severity: High
Module: Claim Detail
File/Class if known: `ClaimDetailActivity`
Current behavior: Claim detail used a Hilt-backed `ReimbursementViewModel` without `@AndroidEntryPoint`.
Expected behavior: Activity should be a valid Hilt entry point.
Risk: Opening claim detail can crash at runtime.
Recommended fix: Added `@AndroidEntryPoint` and invalid claim ID handling.
Test case: Tap a claim row and verify detail opens; launch with invalid ID and verify no crash.

Issue:
Severity: Medium
Module: Travel Expense Entry
File/Class if known: `AddTravelDetailViewModel`, `AddTravelingDetailActivity`
Current behavior: Amount validation accepted non-numeric text and attachment compression result was ignored.
Expected behavior: Amount must be numeric and positive; submitted attachment path should match compressed output when available.
Risk: Bad claims can be created locally or upload can reference the wrong file.
Recommended fix: Added numeric validation and compressed-path fallback handling.
Test case: Enter `abc`, `0`, and valid amount; only valid positive amount should pass.

Issue:
Severity: Medium
Module: Multi-Site Claim Selection
File/Class if known: `SiteSelectionBottomSheetDialog`
Current behavior: Checkbox rebinds used adapter position directly and preselected PO restoration was incomplete.
Expected behavior: Recycler mutations/filtering should not crash and selected PO state should round-trip.
Risk: Filtering or fast selection changes can crash or lose selected PO.
Recommended fix: Guarded `NO_POSITION` and restored preselected PO values.
Test case: Select multiple sites, choose POs, filter list, unfilter, and submit; selected POs should remain correct.

## Remaining Validation Required

- Physical device testing for camera/photo capture during Start Work and receipt uploads.
- API contract confirmation for multi-type self-assign activity mapping if backend expects activities scoped differently per type.
- End Work with material usage should be tested with no materials, invalid quantities, partial material entries, and status `206`.
- Reimbursement outstation claims should be tested with hotel-only, travel-only, DA-only, and mixed expense combinations.
- Low-memory and process-death restoration should be tested for create-claim draft data; current screen state is not persisted as a draft.

## Recommended Phase 2 Device Matrix

- Android 10, 11, 12, 13, 14, 15.
- Pixel/reference device.
- Samsung device.
- Xiaomi/MIUI device.
- Low RAM device.

## Must-Pass Regression Tests

Test ID: WM-001
Module: Work Management
Scenario: Attendance tracking survives navigation to/from Work Management.
Preconditions: User is checked in and tracking notification is visible.
Steps: Open Work Management, open assigned work detail, return to dashboard.
Expected result: Foreground notification remains visible and tracking service remains active.
Actual result:
Priority: Critical
Automation possible: Partial

Test ID: WM-002
Module: Work Management
Scenario: Rapid duplicate Accept/Start/End taps.
Preconditions: Assigned work exists in OPEN, ACCEPTED, and WIP states.
Steps: Rapid tap the visible transition button multiple times.
Expected result: One request is sent and UI remains stable.
Actual result:
Priority: High
Automation possible: Yes

Test ID: WM-003
Module: Self Assign
Scenario: Selected activities are submitted.
Preconditions: Client/project/PO/site/type/activity data available.
Steps: Select type, select activities, submit self-assignment.
Expected result: API payload contains selected activity IDs.
Actual result:
Priority: High
Automation possible: Yes

Test ID: RB-001
Module: Reimbursement
Scenario: Claim list refresh and pagination.
Preconditions: More than one page of claims exists.
Steps: Scroll to bottom, swipe refresh during/after load.
Expected result: No duplicate calls, correct page reset, clean list state.
Actual result:
Priority: Medium
Automation possible: Yes

Test ID: RB-002
Module: Create Claim
Scenario: Duplicate submit prevention.
Preconditions: Valid single-site or multi-site claim form.
Steps: Rapid tap Submit repeatedly.
Expected result: Only one create-claim request is sent.
Actual result:
Priority: High
Automation possible: Yes

Test ID: RB-003
Module: Create Claim
Scenario: Conditional expense validation.
Preconditions: User can switch between local/outstation and single/multiple sites.
Steps: Submit local/outstation claims with missing and valid expense combinations.
Expected result: Missing data shows field-level errors; valid data submits cleanly.
Actual result:
Priority: High
Automation possible: Partial
