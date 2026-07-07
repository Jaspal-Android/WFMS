# API and Data Model Inventory

## Network Configuration

- Retrofit service: `app/src/main/java/com/atvantiq/wfms/network/ApiService.kt`
- Endpoint constants: `app/src/main/java/com/atvantiq/wfms/network/NetworkEndPoints.kt`
- Hilt network module: `app/src/main/java/com/atvantiq/wfms/di/modules/NetModule.kt`
- Base URLs:
  - `dev`: `https://devapi.onaqt.com/`
  - `beta`: `https://betaapi.onaqt.com/`
  - `demo`: `https://demoapi.onaqt.com/`
  - `prod`: `https://api.onaqt.com/`
- Serialization: Gson with `LOWER_CASE_WITH_UNDERSCORES` and lenient parsing.
- Timeouts: 2 minutes connect/read.
- Protocols: forced to HTTP/1.1.
- Logging: `HttpLoggingInterceptor.Level.BODY` for all builds.
- Debug network tooling: Stetho interceptor only when `BuildConfig.DEBUG` in `NetModule`, but `MApplication` imports `com.facebook.stetho.BuildConfig`, which may not reflect app debug state.

## API List

| Method | Path | Function | Auth | Request | Response |
|---|---|---|---|---|---|
| POST | `login` | `loginRequest` | No | `JsonObject` email/password | `LoginResponse` |
| GET | `employee/me` | `empDetails` | Bearer | none | `EmpDetailResponse` |
| POST | `attendance/checkin` | `attendanceCheckIn` | Bearer | `JsonObject` | `CheckInOutResponse` |
| POST | `attendance/checkout` | `attendanceCheckOut` | Bearer | `JsonObject` | `CheckInOutResponse` |
| GET | `attendance/checkin/status` | `attendanceCheckInStatus` | Bearer | none | `CheckInStatusResponse` |
| GET | `attendance/details` | `attendanceDetails` | Bearer | `month`, `year`, `is_export` | `AttendanceDetailListResponse` |
| GET | `work/site/assigned` | `workAssignedAll` | Bearer | `page`, `page_size`, optional `search`, optional encoded `status` | `WorkAssignedResponse` |
| GET | `work/site/{work_site_id}/types` | `workById` | Bearer | `work_site_id` path | `WorkDetailResponse` |
| GET | `work/details` | `workDetailByDate` | Bearer | `date` | `WorkDetailsByDateResponse` |
| PUT | `work/accept/{work_site_id}` | `workAccept` | Bearer | `work_site_id` path | `WorkDetailResponse` |
| POST multipart | `work/start` | `workStart` | Bearer | `work_site_id`, `latitude`, `longitude`, `photo` | `WorkDetailResponse` |
| POST | `work/end` | `workEnd` | Bearer | `JsonObject` | `WorkDetailResponse` |
| POST | `work/self-assign` | `workSelfAssign` | Bearer | `JsonObject` | `SelfAssignResponse` |
| GET | `client/all` | `clientList` | Bearer | none | `ClientListResponse` |
| GET | `project/client/{client_id}` | `projectListByClientId` | Bearer | `client_id` path | `ProjectListByClientResponse` |
| GET | `po/rec/{project_id}` | `poNumberListByProject` | Bearer | `project_id` path | `PoListByProjectResponse` |
| GET | `project/circle/{project_id}` | `circleByProject` | Bearer | `project_id` path | `CircleListByProjectResponse` |
| GET | `site/project/{project_id}` | `siteListByProject` | Bearer | `project_id` path | `SiteListByProjectResponse` |
| GET | `type/po/{po_id}` | `typeListByPo` | Bearer | `po_id` path | `TypeListByProjectResponse` |
| GET | `activity/po-type` | `activityListByPoType` | Bearer | `po_id`, `type_id` | `ActivityListByProjectTypeResponse` |
| POST | `geo-tracking/location` | `sendLocation` | Bearer | `JsonObject` latitude/longitude | `SendLocationResponse` |
| POST | `notifications/notification-token` | `sendNotificationToken` | Bearer | `JsonObject` employee_id/token/device_type | `UpdateNotificationTokenResponse` |
| GET | `site/all` | `siteListAll` | Bearer | `page`, `limit`, `is_active` | `SitesListAllResponse` |
| POST | `site/create` | `createSite` | Bearer | `JsonObject` | `CreateSiteResponse` |
| GET | `work/sites/{employee_id}` | `workSites` | Bearer | `employee_id` path, `date` query | `WorkSitesResponse` |
| GET | `/work/site/progress/{work_site_id}` | `workSiteDetailsAdmin` | Bearer | `work_site_id`, `employee_id`, `date` | `WorkSiteDetailResponse` |
| POST | `work/approve` | `approveWorkSite` | Bearer | `JsonArray` | `ApproveWorkSiteTypeResponse` |
| POST | `attendance/emp/remarks/{attendance_id}` | `attendanceEmpRemarks` | Bearer | `attendance_id`, `JsonObject` | `AttendanceRemarksResponse` |
| POST | `forgot-password` | `forgotPassword` | Bearer | `JsonObject` | `ForgotPasswordResponse` |
| POST multipart | `attendance/apply-leave` | `applyLeave` | Bearer | leave type/date/reason, optional attachment | `ApplyLeaveResponse` |
| POST | `request-otp` | `requestOTP` | No | `JsonObject` email | `RequestOtpResponse` |
| POST | `verify-otp` | `verifyOTP` | No | `JsonObject` email/otp | `LoginResponse` |
| GET | `claim/sites/date` | `workSiteByDate` | Bearer | `date` | `WorkSiteByDateResponse` |
| GET | `project/all?is_all=true&is_active=1` | `allProjects` | Bearer | none | `AllProjectsResponse` |
| GET | `circle/employee` | `employeeByCircle` | Bearer | `code` | `EmployeeByCircleResponse` |
| POST multipart | `claim/create-claim` | `createClaim` | Bearer | `data`, file list | `CreateClaimResponse` |
| GET | `claim/all/mobile` | `allClaims` | Bearer | `page`, `page_size` | `AllClaimsResponse` |
| GET | `claim/{claim_id}` | `claimById` | Bearer | `claim_id` path | `ClaimDetailResponse` |
| GET | `inventory/by-project/{project_id}` | `inventoryByProject` | Bearer | `project_id` path | `InventoryByProjectResponse` |
| GET | `budget/emp/my-targets` | `myTargets` | Bearer | optional `month` | `MyTargetsResponse` |
| GET | `budget/emp/my-projects` | `myProjects` | Bearer | optional `month` | `MyProjectsResponse` |

## Repository Grouping

| Repository | Responsibilities |
|---|---|
| `AuthRepo` | Login, employee details, notification token upload, forgot password, OTP request/verify. |
| `AttendanceRepo` | Attendance check-in/out/status/details, admin work site approvals, attendance remarks, leave application. |
| `WorkRepo` | Assigned work list, work accept/start/end/self-assign, work detail by ID/date, inventory by project. |
| `CreationRepo` | Client/project/PO/circle/site/type/activity lookup, site list/create, all projects. |
| `ClaimRepo` | Claim site lookup, circle employee lookup, create claim, all claims, claim detail. |
| `TrackingRepo` | Foreground service location upload. |
| `BudgetRepo` | My targets and my projects. |

## Data Model List

The project keeps model classes under `app/src/main/java/com/atvantiq/wfms/models`. Top-level model domains:

| Domain | Model Files / Classes |
|---|---|
| Auth/login | `LoginResponse`, login `Data`, `User`, `AccessLevel`, `Permission`, `OfficialLocation`, OTP `RequestOtpResponse`, `ForgotPasswordResponse`. |
| Employee detail | `EmpDetailResponse`, `EmpData`, `AccessLevel`, `Permission`, `ReportingManager`. |
| Attendance | `CheckInOutResponse`, `CheckoutData`, `CheckInStatusResponse`, attendance detail list/data/record/log/checkin/checkout/status/employee/admin/ops/pm, `AttendanceRemarksResponse`, leave `ApplyLeaveResponse`, `AttendanceDay`. |
| Work assignment | `WorkAssignedResponse`, `AssignedWorkData`, `Site`, `Project`, `Circle`, `Status`, `Type`; all-assigned variants under `work/assignedAll`; self-assign variants under `work/selfAssign`. |
| Work detail/progress | `WorkDetailResponse`, `WorkDetailData`, `Type`, `Status`, `WorkDetailsByDateResponse`; work-site approval/progress models under `workSites/*`. |
| Admin/site creation | `ClientListResponse`, `ClientData`, `Client`, `ProjectListByClientResponse`, `ProjectData`, `PoListByProjectResponse`, `PoData`, `CircleListByProjectResponse`, `CircleData`, `SiteListByProjectResponse`, `SiteData`, `SitesListAllResponse`, `AllSiteData`, site `CreateSiteResponse`, `CreateSiteData`. |
| Claims/reimbursement | Local expense models `TravelExpense`, `HotelExpense`, `DAExpense`, `OtherExpense`, `MultipleSite`, `TravelModeOption`; API models `AllClaimsResponse`, claim `Record`, `ClaimDetailResponse`, `ClaimData`, `Expense`, `Site`, `Employee`, `CreateClaimResponse`. |
| Claims helpers | `WorkSiteByDateResponse`, site-by-date `Data`/`Site`, `EmployeeByCircleResponse`. |
| Budget/targets | `MyTargetsResponse`, `MyTargetsData`, `TargetEmployee`, `TargetRevenue`, `TargetSites`, `TargetActiveDays`, `TargetHoursWorked`, `MyProjectsResponse`, `ProjectBudgetItem`, `ProjectInfo`, `ClientInfo`, `CircleInfo`. |
| Inventory/material | `InventoryByProjectResponse`, `InventoryData`, `UsedMaterial`, `MaterialRecord`. |
| Location/notification | `SendLocationResponse`, `CustomLocationRequest`, `UpdateNotificationTokenResponse`. |
| Misc/UI | `AssignedTasks`, `CabRide`, `Posts`, `PostsItem`, `StatusOption`, activity/type/all-project data models. |

## Data Flow Notes

- Bearer token retrieval is repeated in repositories from `SecurePrefMain` using `PrefKeys.LOGIN_TOKEN`.
- Login success persists token and user data before uploading FCM token.
- Employee details are cached into encrypted prefs from `DashboardFragment`.
- Multipart uploads are used for work start photos, leave attachments, and claim files.
- Most create/update APIs use raw `JsonObject`/`JsonArray`, so request schema is mostly implicit in view models rather than typed request models.
