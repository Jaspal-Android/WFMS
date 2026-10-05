# WFMS Android — Engineering Standards Audit

**Date:** 2026-10-04 · **Branch audited:** `preDev` (includes PRs #96–#120) · **Method:** static review by five independent reviewers (architecture, resources/accessibility, async/performance, security/build, tests/quality), plus measurements by the lead and a minified release build.

Standards measured against: the Senior Staff Android brief (strict MVVM, no hardcoding, reuse, performance, security, accessibility, testing). Per the brief's own rule 2, **existing project conventions override its defaults**; where WFMS deliberately uses LiveData, data binding and Activities, that is reported as a *gap to a target*, not a defect.

---

## 1. Executive summary

| Area | Score | One-line verdict |
|---|---|---|
| Security & build | **7.5 / 10** | Solid posture; weak build hygiene and a debug tool shipped to production. |
| Resources & accessibility | **7 / 10** | Theming, RTL and key centralisation are strong; stray literal/mock text, English-only, unused resources. |
| Async, lifecycle, errors, performance | **6.5 / 10** | Error handling and paging are well built; no process-death handling, silent service failure, a real crash path. |
| MVVM architecture & layering | **5.5 / 10** | Plumbing is good; the View layer still carries business flow and duplicated error/permission code. |
| Tests & code quality | **5.5 / 10** | Pure logic well tested; only 32% of ViewModels covered; no static analysis; ~1,500+ lines of dead code. |
| **Overall (simple mean)** | **≈ 6.4 / 10** | A working, shippable app whose newest code (MyDay, MaterialUsage, paged lists) shows the target pattern; the other ~30 screens predate it. |

**What is genuinely good:** cleartext denied; Authorization redacted and body logging off in release; all components non-exported except the launcher; immutable PendingIntents; encrypted prefs with correct backup exclusions; typed, centralised API error mapping (`BaseViewModel.executeApiCall` + `ApiErrorMapper`); a robust paging base (`PagedList`); no `GlobalScope`, `AsyncTask` or `runBlocking`; no ViewModel touches Retrofit or a View; 6-palette theme with dark mode and WCAG contrast tests; RTL-clean layouts; CI with pinned actions and a production gate.

**Fix first (all small):**
1. **Stetho ships in production** (510 classes kept in the minified release). → `debugImplementation`.
2. **Crash path:** `DashboardFragment` calls `requireContext()` inside async location/Geocoder callbacks.
3. **Tracking service dies silently:** failures call `stopSelf()` while "tracking active" stays true; the user is never told.
4. **R8 hygiene:** `isShrinkResources` off; `proguard-rules.pro` is two lines.
5. **Dead code:** 5 unreachable activities, 5 unreachable nav destinations, ~1,000 lines of Vendor/Cab/About/Feedback/MaterialReco, 4 dead base classes, 6 unused libraries, 200 unused resources.

---

## 2. Baseline numbers

| Metric | Value |
|---|---|
| Kotlin files / lines | 440 / ~27,000 |
| Layouts / drawables | 124 / 164 |
| Activities / Fragments / ViewModels | 40 / 15 / 37 concrete (23 `@HiltViewModel`) |
| Unit tests | 54 files, 346 `@Test`, ~6,200 lines; androidTest 7 files, 20 tests |
| ViewModels with a test | 12 of 37 (32%) |
| Android lint | **698 warnings, 0 errors** (UnusedResources 200, HardcodedText 144, GradleDependency 52, UseKtx 43, UseTomlInstead 33, SetTextI18n 29, NotifyDataSetChanged 26, LockedOrientationActivity 25, LabelFor 16) |
| Minified release APK | builds cleanly with R8: 5.7 MB vs 16.1 MB debug |
| Not present | Timber, Turbine, DataStore, domain/use-case layer, detekt/ktlint, lint config/baseline, `SavedStateHandle` (1 of 37 VMs), `localeConfig`, translations |

*Corrections to the lead's early greps, after reviewer cross-checks:* ViewModel count is 37 (not 45); real `Handler(` uses are 3 (not 7); `!!` is 22 lines of which 2 are commented out.

---

## 3. Findings by area

Severity: **C**ritical · **H**igh · **M**edium · **L**ow. Effort: **S** (< 1 PR, hours) · **M** (1–3 PRs) · **L** (a multi-PR programme).

### 3.1 Security & build — 7.5/10

| ID | Sev | Finding | Fix | Effort |
|---|---|---|---|---|
| SB-1 | **H** | **Stetho is a normal `implementation` dependency.** Init and interceptor are `BuildConfig.DEBUG`-guarded, but the classes are not stripped: 510 Stetho classes are kept in the release build (verified in R8 `mapping.txt`). A debug inspection tool ships to users. | `debugImplementation`; delete the unused `StethoInterceptor` import path in release. | S |
| SB-2 | M | `isShrinkResources` not set; `proguard-rules.pro` has only the Crashlytics attributes. R8 builds cleanly and the models that were checked are safe (expense models are Parcelable + hand-built `JSONObject`; persisted tracking models carry `@SerializedName`). 23 model files lack `@SerializedName`; most are Parcelable or dead. | `isShrinkResources = true`; add `-keep class com.atvantiq.wfms.models.** { *; }` as cheap insurance; **smoke-test the release build** (login, claims, My Day, location queue). | S |
| SB-3 | M | `androidx.security:security-crypto:1.1.0-alpha06` (an **alpha**) holds the token, user/employee JSON, location queue and shift state. `SecurePrefMain` wipes prefs on any `GeneralSecurityException`, logging the user out silently. | Move to a stable release (version not verified online here) or Keystore + DataStore; keep the recovery logic. | M |
| SB-4 | M | `allowBackup="true"`. Encrypted prefs and keyset are excluded correctly, but receipt/work photos in `filesDir/WFMS` are not. `docs/audit/android-risk-review.md` is stale on this point. | `allowBackup="false"`, or exclude `domain="file"` `WFMS/`. | S |
| SB-5 | M | Five `google-services.json` files tracked; the root `app/google-services.json` is a different Firebase project (`wfms-24378`) from prod and looks stale. CI already injects the real ones. | Confirm API keys are restricted (package + SHA-1); delete the root copy; optionally gitignore the rest. | S |
| SB-6 | M | No lint config/baseline, no detekt/ktlint/`.editorconfig`; CI runs only debug lint and unit tests (no `connectedAndroidTest`, no release lint). No `FLAG_SECURE` on Login/New Password/claim screens (visible in Recents). | `lint {}` block (`abortOnError`, `checkReleaseBuilds`) + baseline; `FLAG_SECURE` on sensitive screens. **Tooling needs your approval.** | M |
| SB-7 | L | Debug builds of every flavour (incl. `prodDebug`) log request **bodies**, including the login password; Authorization is redacted. | `Level.HEADERS`, or redact `/login` bodies. | S |
| SB-8 | L | No certificate pinning. | Optional; weigh against rotation risk. | M |
| SB-9 | L | Logout (`SessionCleanup`) clears prefs, service, notifications and FCM token but not `filesDir/WFMS` photos or cache copies. `path_provider.xml` exposes all of `cache-path "."`; legacy `file://` URIs are accepted by `PickMediaHelper`. | Delete photos/cache on logout; narrow paths; reject foreign `file://`. | S |
| SB-10 | L | 33 dependencies bypass the version catalog; 4 declared twice with different versions (location 21.3.0/21.0.1, navigation 2.8.3/2.7.7, security-crypto ×2); `firebase-messaging-ktx:24.0.0` pinned over `firebase-bom:32.7.0`. | One catalog, one version each, BoM-managed Firebase. | M |
| SB-11 | L | **Unused libraries:** MPAndroidChart (JitPack), android-maps-utils, retrofit2-rxjava2-adapter (still registered in `NetModule`), legacy-support-v4, navigation-dynamic-features, likely `hilt-android-testing`/`kaptTest`. | Remove; delete the `RxJava2CallAdapterFactory` line. | S |
| SB-12 | L | Leftovers: `composeOptions` with no Compose; `jvmTarget` 1.8; Hilt on kapt; dated Firebase BoM 32.7.0 / Stetho 1.5.1 / splashscreen 1.0.0 / coroutines-test 1.7.3; no Gradle parallel/caching/configuration-cache; `secrets-gradle-plugin` applied but unused. | Remove; JVM 17; KSP for Hilt (**needs approval**); enable build caching. | M |

### 3.2 Resources & accessibility — 7/10

| ID | Sev | Finding | Fix | Effort |
|---|---|---|---|---|
| RA-1 | **H** | **121 literal `android:text`** in 27 layouts (+20 literal hints, 3 contentDescriptions). Real static copy: `fragment_about`, `activity_new_password`, `fragment_cab`, `fragment_feedback`, vendor/cab forms, weekday letters in `calendar_view`. **Mock data that looks live** in `activity_progress_details`, `activity_sign_in_detail`, `activity_my_claim_details`, `item_sign_in`, vendor layouts ("Amount: 25 ₹", names, dates). `layout/temp.xml` is unused. | Strings for real copy; mock values → `tools:text` or delete; delete `temp.xml`; weekday headers from `DayOfWeek`. | M |
| RA-2 | **H** | ~29 user-visible literals in Kotlin: "Create Password", "Announcements", `"of … target"`, `"Entry ${n}"`, `"PO: …"`, "Select Month and Year", "Share app via", two "Unable to get location" toasts, `MaterialUsageViewModel` "Failed to load materials"; `ThemePickerBottomSheet` builds its UI in code with literal labels, emoji and `textSize = 16f`. | Parameterised strings/plurals; theme-picker labels/sizes to resources. | M |
| RA-3 | **H** | **Money formatting is not centralised.** Hard-coded `"₹${amount}"` and `"$"` with K/M abbreviation duplicated in two places (`String.format` without Locale); `Utils` forces `Locale.ENGLISH`. | One `CurrencyFormatter` (`NumberFormat`, `en-IN`). | S |
| RA-4 | **H** | **Date formatting.** ~10 server-format parses use `Locale.getDefault()` (should be `Locale.US`/`ROOT`); file names use the default locale; `ProjectDashboardVM`/`MyTargetsVM` use locale-dependent `"%04d-%02d".format`. `Utils.dateToString` has a `mm`/`hh` month bug (**unused — delete**). | API/file patterns → `Locale.US`; prefer `java.time`; delete the dead function. | S–M |
| RA-5 | M | **43 literal HTTP codes** (200/201/206/401) in 9 files (`AddSignInActivity` 14, `AssignedTaskDetailActivity` 10…); `ValConstants` exists and is used ~71 times. Also `3001`, `"2001"` request codes. | Replace with constants; named request-code constants. | S |
| RA-6 | M | English-only: no `values-xx`, no `localeConfig`; 21 duplicate string groups; 70 unused strings. | Prune; add `localeConfig`/pipeline when needed. | M |
| RA-7 | M | **Unused resources:** 49/164 drawables, 26/90 dimens, 20/192 colors, 15/92 styles, 70/587 strings (lint: 200). 14 dimens named by value (`margin_8dp`), 14 value-groups with several names (16dp ×9). | Delete via lint; one name per scale step. | S |
| RA-8 | M | Dark mode leaks: ~8 drawables with fixed fills (`bg_status_on_track`, `bg_status_behind`, `progress_revenue`…), `status_*` pastel palette without night variants, `Color.BLACK/GRAY` in `StatusAdapter`, `ThemeManager` duplicates colours as hex. | Move to `?attr/wfmsColor*`; add night variants. | M |
| RA-9 | M | Repeated attribute blocks not yet styles: 66 TextInputLayouts (44 styled), 22+52 cards (≈44 identical), 61 dividers, 254 LinearLayouts (7 styled). No `textAppearance` use; 15 unused `Custom*` styles. | `Widget.WFMS.Card/Card.Outlined/Dropdown/Divider`; `TextAppearance.WFMS.*`. | M |
| RA-10 | M | **Touch targets:** the new shared header's back button is **44 dp** (below 48 dp) across 36 screens — *introduced in PR #119*; `small_button_height` 40 dp, `switch_button_height` 45 dp, tracking button 34 dp. 5 of 103 images lack a description or `importantForAccessibility="no"`; 16 `LabelFor` warnings; 25 activities lock orientation. | 48 dp (or `TouchDelegate`); add descriptions; review orientation locks. | S |
| RA-11 | L | 180 real literal dp values (53 × 1 dp dividers, 37 × 2 dp, 20 × 14 dp icons); 4 literal sp; one `layout_marginLeft/Right` pair. | `divider_height`, `icon_size_xs/m` dimens. | S |

### 3.3 Async, lifecycle, errors & performance — 6.5/10

| ID | Sev | Finding | Fix | Effort |
|---|---|---|---|---|
| AP-1 | **H** | **No process-death handling.** `SavedStateHandle` in 1 of 37 VMs (`MaterialUsageViewModel`); no `onSaveInstanceState`. In-progress claim, sign-in and add-site forms live in plain VM fields; 13 bottom-sheet callbacks are lambdas lost on recreation. | `SavedStateHandle` for form state; `setFragmentResultListener` for sheets. | M |
| AP-2 | **H** | **Tracking service fails silently** (`LocationTrackingService:176-179, 290-293, 306-309`): any exception or lost permission → `stopSelf()` while `IS_TRACKING_ACTIVE` stays true; Android 12+ background restarts can throw in `startForeground`. The user is never told. *(verified in code; the file was extended in PRs #108–#109, but who first wrote these paths was not traced)* | Clear the flag and post a "tracking stopped" notification. | S |
| AP-3 | **H** | **Real crash path** — `DashboardFragment:679-681` calls `requireContext()`/`requireActivity().runOnUiThread` inside async `lastLocation` + Geocoder callbacks; throws if the fragment detached. *(verified)* | Guard with `isAdded`/`viewLifecycleOwner` or a main-safe suspend call. | S |
| AP-4 | M | Service upload failures are only logged; with an expired token it retries every fix and drops the oldest of 500 queued events. No 401 handling. `readQueue` returns `emptyList()` on any parse failure, silently losing the queue. | Stop/notify on 401, back off; log parse failures. | M |
| AP-5 | M | `LocationTrackingService:370` catches `Exception` in a suspend function without rethrowing `CancellationException`. *(verified)* | Add the rethrow. | S |
| AP-6 | M | `BaseFragment` never nulls `binding` (14 fragments retain their view tree); `mContext`/`mActivity` never cleared; other bases only `unbind()`. | `_binding` + null in `onDestroyView`. | M |
| AP-7 | M | **No injected dispatchers**: hardcoded in `LocationTrackingService:91`, `SplashVM:30` (overridable var), `PickMediaHelper:220`. | `@Qualifier` dispatcher module. | M |
| AP-8 | M | **29 `notifyDataSetChanged` in 24 files**; 3 `ListAdapter`s vs 33 plain adapters — including adapters added during the admin work (see §5). `PagedList.publish()` already emits immutable snapshots. | Migrate paged adapters first to `ListAdapter` + `DiffUtil`. | L |
| AP-9 | M | Main-thread work: synchronous `decodeBitmap` (`AddCabFareActivity`, `VendorStartActivity`); EncryptedSharedPreferences reads/writes in `DashboardViewModel:51,77,89,103`; Gson parse in `ProfileVM` initializer. | `withContext(io)` / `prepareImage`. | S–M |
| AP-10 | M | `LocationEventQueue` decrypts, parses and re-encrypts up to 500 events on every fix (every 10 s in trip mode). | File- or Room-backed queue. | M |
| AP-11 | M | `android.util.Log` 31 calls/11 files, no Timber; one unguarded `Log.d` runs in release; 3 `printStackTrace`. No sensitive data found in logs. | Timber + release tree to Crashlytics (**needs approval**). | M |
| AP-12 | L | 22 `!!` (none a confirmed realistic crash); 86 `lateinit`; unsafe `as ArrayList` in `CalendarView`; `ConnectivityReceiver` never registered (no live connectivity UX); unused `WAKE_LOCK`; dead `FooterRecyclerView` fields; `ApiErrorMapper` maps every `IOException` (incl. SSL) to "no internet"; no `callTimeout`; service channel name hardcoded. | Clean up individually. | S |

### 3.4 MVVM architecture & layering — 5.5/10

| ID | Sev | Finding | Fix | Effort |
|---|---|---|---|---|
| AR-1 | **H** | **No `UiState`.** ViewModels hold 89 `MutableLiveData`, 47 `ObservableField`, 45 `LiveData<ApiState<…>>`. `AddSignInVM` has 8 loading flags + 8 response LiveData; `CreateClaimVM` 22 state holders. | One `StateFlow<UiState>` per screen, heaviest first: AddSignIn, CreateClaim, AddSite. *(Programme — a gap to the target, since LiveData is the project convention.)* | L |
| AR-2 | **H** | **One-time events replay.** 15 `clickEvents` LiveData + error LiveData are never cleared; `ApiState.consumeOnce()` used in only 14 files against 49 `when(response.status)` blocks; ten observers work around replay with `isLifeCycleResumed()`. | `Event<T>`/`Channel` wrapper; migrate clickEvents, errors, ApiState. | M |
| AR-3 | **H** | **Views hold business flow:** `DashboardFragment` owns `isDayStarted`, `pendingCheckoutLocation`, check-in/out decisions; `CreateClaimActivity:393-416` flips VM flags and clears fields; `AddSignInActivity` makes 18 direct VM writes; 48 `viewModel.x =` and 43 `.set()` calls in Views; bottom sheets validate amounts. | Intent-style VM methods (`onClientSelected(c)`); decisions into VMs. | L |
| AR-4 | **H** | **No domain/use-case layer; DTOs flow into UI.** 175 model files are Gson DTOs used directly by 14 adapters and 19 layouts; adapters/Views mutate DTOs (`AssignedTasksListAdapter:103`); repositories are pure passthroughs exposing `JsonObject`/`RequestBody`. | UI models + mappers for decision-bearing screens; use cases only where logic is non-trivial (check-in, claim submit); build wire bodies inside repositories. | L |
| AR-5 | **H** | **Duplicated error handling:** `when(response.code){200/401/else}` ×23 in 9 files; `tokenExpiresAlert()` ×42 sites; `handleError` re-implemented in 6 files, `handleErrorResponse` in 5. Base `handleApiFailure`/`handleRejectedResponse` already exist (26 and 16 uses). | One `ApiState.render(...)` helper in the base, or 401 in an OkHttp `Authenticator`. | M |
| AR-6 | **H** | **Location-permission flow copied 4×** (`hasAllPermissions`, rationale, permanently-denied, open settings in `AttendanceFragment`, `AssignedTaskDetailActivity`, `LoginActivity`, `DashboardFragment`); `RequestMultiplePermissions` launchers in 11 files; `.lastLocation` in 8 places. | `LocationPermissionDelegate` + `LocationProvider`. | M |
| AR-7 | M | ViewModels build wire format (`JsonObject` in 9 VMs; `CreateClaimVM` 20 uses; `File`+Multipart in `AttendanceViewModel`); `LoginVM` calls `FirebaseMessaging` and prefs; 8 VMs inject `SecurePrefMain`. | `SessionRepo`; request building into repositories. | M |
| AR-8 | M | VMs use `getApplication().getString()` for UI text (`ClaimApprovalVM`, `AttendanceApprovalVM`) and `Utils.isInternet(app)` (`BaseViewModel`, 3 more); `DashboardViewModel` builds Intents for the service. No VM references a View/Activity/Fragment/binding. | `@StringRes`/message sealed class; `ConnectivityChecker`, `TrackingController`. | M |
| AR-9 | M | `alertDialogShow` has 29 overloads across 5 bases; `hideKeyboard` defined 7×, used once; logout/session-expired block duplicated Activity vs Fragment; 4 dead base classes (~550 lines); `BaseFragment` requires `AndroidViewModel`. | Extensions; delete dead bases; align generics. | S |
| AR-10 | M | **Navigation split:** 37 activities; Navigation Component only for the 11 tab fragments; no Safe Args; 30 `startActivity` + 20 `StartActivityForResult`. Some extras keys live outside `SharingKeys`. | Centralise keys now; single-activity over time. | L |
| AR-11 | M | God classes: `AddSignInActivity` 737, `CreateClaimActivity` 717, `DashboardFragment` 699, `AssignedTaskDetailActivity` 609, `CreateClaimVM` 588, `AttendanceFragment` 501, `Utils` 460, `LocationTrackingService` 407. A 343-line `subscribeToEvents` (`AddSignInActivity`) and a 188-line `onCreateView` (`ThemePickerBottomSheet`). | Split by responsibility. | L |
| AR-12 | L | `CreateClaimVM`/`MaterialUsageViewModel` inject concrete repositories; `PrefMethods.kt` lives in `data/prefs` but declares `package com.ssas.jibli.data.prefs` (12 imports) and a 0-byte stray duplicate exists. | Inject interfaces; fix package; delete stray. | S |

### 3.5 Tests & code quality — 5.5/10

| ID | Sev | Finding | Fix | Effort |
|---|---|---|---|---|
| TQ-1 | **H** | **25 of 37 ViewModels untested** (16 with logic): `ApplyLeaveVM`, `ClaimApprovalVM`, `ClaimApprovalDetailVM`, `MonthlyAttendanceListVM`, `SitesVM`, `MyDayVM`, `ProjectDashboardVM`, `MyTargetsVM`, `MaterialUsageViewModel`, `ForgotPasswordVM`, `CreatePasswordVM`, `AddTravelDetailViewModel`, … `CreateClaimVM` has 5 tests for 588 lines. | Start with ApplyLeave, ClaimApproval ×2, MonthlyAttendanceListVM. | L |
| TQ-2 | **H** | `ClaimRepo`, `TrackingRepo`, `BudgetRepo` untested; the 4 repo tests are pass-through smoke tests against relaxed mocks. | Add tests; add error/empty/malformed cases. | S–M |
| TQ-3 | M | Zero hand-written fakes (89 `mockk`, 82 `relaxed`); `mockkObject(Utils)` ×11 and `PrefMethods` ×3 because of hidden static dependencies; no injectable `Clock` (54 direct time calls); `hilt-android-testing` declared but unused. | Fakes for the 7 `I*Repo` interfaces; inject connectivity/prefs/clock. | M |
| TQ-4 | M | **Dead code ships:** 5 manifest activities never launched (`NewPasswordActivity`, `AddMutilSiteDetailsActivity`, `ClaimApprovalsActivity`, `MyClaimsActivity`, attendance `ApprovalsActivity`); 5 nav destinations unreachable (`nav_vendor`, `nav_cab`, `nav_about`, `nav_feedback`, `nav_material_reco`) with everything behind them (Vendor ≈524 lines/4 activities, Cab ≈373, MaterialReco, About, Feedback); ~12 dead classes; 15 unused `Utils` members. *(verified: 0 references)* | Delete, or gate behind a flag — **your call; some may be planned features.** | M |
| TQ-5 | L | 155 unused imports (heuristic); ~90 lines of commented-out code in ~5 blocks; typos `intentory`, `setupWokTypeRecyclerView`, `AddMutilSiteDetailsActivity`, `sumitted`; 23 tab-indented files; 13 functions > 60 lines. | Formatter/IDE clean-up; rename. | S |
| TQ-6 | L | Instrumented tests: 20 total; CI never runs them. | Add a CI job on an emulator, later. | M |

**Already good:** pure-logic objects with injectable seams (`ShiftTracker`, `TrackMath`, `TripModePolicy`, `MyDayContent`, `DashboardTabs`, `AddSiteValidation`, `ClaimDecision`); disciplined coroutine tests (`runTest`, no `Thread.sleep`/`runBlocking`, `unmockkAll`); high assertion density; accessibility and dark-theme resource tests.

---

## 4. Impact analysis

| Change class | Affects | What could break |
|---|---|---|
| Stetho → `debugImplementation` | Debug builds only | A release-source reference would fail to compile — surfaces immediately. |
| `isShrinkResources` / keep rule | Release builds | Resources reached by name at runtime (`getIdentifier` — none found) or reflection; **run a release smoke test**. |
| Backup / `FLAG_SECURE` | Users' restore behaviour; screenshots on 3 screens | Fewer restored files; screenshots blocked there by design. |
| Delete dead screens/classes | Nothing reachable (verified 0 references) | A screen planned for a future release; hence the confirmation. |
| Error-handling / permission consolidation | ~30 screens | Subtle behavioural drift per screen; needs per-screen manual checks. |
| `UiState`/`StateFlow` migration | One screen per PR | Behaviour change on rotation/process death — a benefit, but test it. |
| `DiffUtil` adapters | List screens | Item-animation changes; selection state in adapters. |
| Tooling (detekt/ktlint/Timber/KSP) | Whole repo / CI | Large initial diffs — land with a baseline. |

---

## 5. Self-review: gaps introduced by recent work (this session's PRs #96–#120)

Held to the same standard, these are mine and are included above:
- **Header back button 44 dp** (PR #119), under the 48 dp minimum (RA-10).
- **New adapters use `notifyDataSetChanged`:** `WorkSubmissionsAdapter`, `AttendanceApprovalAdapter`, `ClaimReviewAdapter`, `WorkSitesAdapter` (AP-8).
- **`AttendanceApprovalVM` / `ClaimApprovalVM` call `getApplication().getString()`** and use the static `PrefMethods` (AR-8, TQ-3).
- **Tracking-service weaknesses** AP-2/AP-4/AP-5 are in `LocationTrackingService`, a file my PRs #108–#109 extended; I did not trace which lines predate them, but I own fixing them.
- **New screens use LiveData, not `StateFlow`/sealed UiState** (matches the existing convention; AR-1).
- **Not yet tested:** `ClaimApprovalVM`, `ClaimApprovalDetailVM`, `MonthlyAttendanceListVM`, `SitesVM` (TQ-1).

---

## 6. Recommended roadmap

Each item is its own small PR into `preDev`, independently revertible.

**Phase 0 — Safety & hygiene (≈ 10 small PRs; no new libraries)**
1. Stetho → `debugImplementation` (SB-1); redact login body (SB-7).
2. Fix `DashboardFragment` async-callback crash (AP-3).
3. Tracking service: clear flag + "stopped" notification, 401 handling, `CancellationException` rethrow (AP-2/4/5).
4. `isShrinkResources` + model keep rule + release smoke test (SB-2); `allowBackup`/photo exclusion (SB-4); delete stale root `google-services.json` after key check (SB-5).
5. Header back button → 48 dp (RA-10).
6. Remove unused dependencies and the Rx adapter (SB-11); consolidate duplicate versions (SB-10).
7. Delete confirmed dead code and unused resources (TQ-4, RA-7, AR-9) — **after you confirm which Vendor/Cab/About/Feedback/MaterialReco screens are retired.**
8. Mock/literal layout text and Kotlin literals → resources (RA-1/2); replace literal HTTP codes (RA-5); `CurrencyFormatter` + locale fixes (RA-3/4).

**Phase 1 — De-duplication & correctness (≈ 8–10 PRs)**
9. `ApiState.render(...)` / central 401 handling (AR-5).
10. `LocationPermissionDelegate` + `LocationProvider` (AR-6).
11. `Event<T>` for one-time events (AR-2).
12. Base-class clean-up; null `binding` (AR-9, AP-6); inject dispatchers (AP-7).
13. `ListAdapter` + `DiffUtil` for the paged adapters first (AP-8).
14. `SavedStateHandle` on the three big forms (AP-1).
15. Tests: the 4 admin VMs + `ApplyLeaveVM`, `ClaimRepo`/`TrackingRepo`/`BudgetRepo`, fakes for the `I*Repo` interfaces (TQ-1/2/3).

**Phase 2 — Structural programme (rolling)**
16. `UiState`/`StateFlow` per screen, heaviest first (AR-1); move decisions out of Views (AR-3).
17. UI models + mappers and wire-building into repositories (AR-4, AR-7).
18. Split god classes (AR-11); long-term single-activity navigation with Safe Args (AR-10).

---

## 7. Decisions needed (new libraries/tooling — rule 2)

| Item | Why | Needs |
|---|---|---|
| Timber | Replace `android.util.Log`; release tree to Crashlytics | approval |
| detekt + ktlint + lint baseline | Static analysis, import/format clean-up | approval |
| Turbine | Flow testing (only when screens move to `StateFlow`) | approval, later |
| KSP for Hilt | Faster builds; kapt is legacy | approval |
| DataStore / security-crypto replacement | `security-crypto` is alpha and deprecated | approval + migration plan |
| Retire Vendor/Cab/About/Feedback/MaterialReco | ≈1,000+ dead lines | your confirmation |

---

## 8. Rollback strategy

Every item ships as an independent PR into `preDev`; rollback is a single `git revert` of that merge. Higher-risk changes (R8 shrinking, backup rules, 401 handling, `UiState` migrations) land one per PR with a manual test checklist, so a regression is isolated to one revert. Nothing in this roadmap requires a data migration, except replacing `security-crypto`, which must keep the existing wipe-and-recover path and ship behind a one-release read-fallback.

---

## 9. Limits of this audit

Static review plus selected verification. **Verified directly by the lead:** Stetho kept in release (R8 mapping), R8 builds cleanly, the `DashboardFragment` crash path, the service's silent `stopSelf()`, the swallowed `CancellationException`, dead activities/nav destinations (0 references), lint totals. **Reviewer-reported, spot-checked only:** most counts (unused imports, unused drawables, long functions are heuristic). **Not done:** no instrumented/TalkBack test, no profiling, no functional test of the release APK, library versions not checked online, no review of CI beyond what the reviewers read, and the two other `docs/audit` reports were not re-validated (one is known stale).
