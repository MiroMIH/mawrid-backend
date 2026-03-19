# Mawrid API — Test Results

## Latest: Post-Refactor State (March 19, 2026)

API routes were refactored to role-scoped prefixes. All Bruno request URLs have been updated.
The test suite below should be re-run against the updated collection to produce a fresh report.

### Route Changes Applied

| Old Path | New Path |
|----------|----------|
| `POST /demandes` | `POST /buyer/demandes` |
| `GET /demandes/my` | `GET /buyer/demandes` |
| `GET /demandes/{id}` | `GET /buyer/demandes/{id}` |
| `GET /demandes/{id}/reponses` | `GET /buyer/demandes/{id}/reponses` |
| `PATCH /demandes/{id}/close` | `PATCH /buyer/demandes/{id}/close` |
| `DELETE /demandes/{id}` | `DELETE /buyer/demandes/{id}` |
| `GET /reponses/feed` | `GET /seller/feed` |
| `GET /reponses/feed/{id}` | `GET /seller/feed/{id}` |
| `POST /reponses/{id}` | `POST /seller/reponses/{id}` |
| `PATCH /reponses/{id}` | `PATCH /seller/reponses/{id}` |
| `GET /reponses/my` | `GET /seller/reponses` |
| `GET /categories/subscribed` | `GET /seller/categories/subscribed` |
| `PATCH /users/me/categories` | `PATCH /seller/categories` |
| `GET /categories/**` (public) | `GET /categories/**` (authenticated) |

Admin routes (`/admin/**`) are unchanged.

---

## Previous Run: March 18, 2026

> This run was executed against the pre-refactor API. Endpoint paths are now outdated.
> Kept here for issue tracking purposes only.

### Summary

| Metric | Result |
|--------|--------|
| Total Requests Run | 22 |
| Total Tests Run | 64 |
| Passed | 20 |
| Failed | 2 |
| Success Rate | 91% |

### Known Issues (pre-refactor run)

#### Issue 1 — Manual Expire Crash (OPEN)
- **Endpoint:** `PATCH /admin/demandes/{id}/expire`
- **Status:** 500 Internal Server Error
- **Diagnosis:** Server-side bug on manual expire trigger. Nightly scheduler works; manual trigger crashes.
- **Action needed:** Investigate backend logs for this endpoint.

#### Issue 2 — 401 vs 403 on Unauthenticated Request (OPEN)
- **Endpoint:** `GET /demandes` (no token)
- **Expected:** 401 Unauthorized
- **Actual:** 403 Forbidden
- **Diagnosis:** Spring Security returns 403 when no authentication entry point is configured for JWT filter.
- **Note:** With the refactor, the endpoint is now `GET /buyer/demandes`. The security behavior may have changed — re-test to confirm.

### Phase Results (March 18, 2026)

#### Phase 1 — Auth & Setup
| Step | Endpoint | Result |
|------|----------|--------|
| 1.1 | Login (Buyer) | ✅ 200 |
| 1.2 | Login (Supplier) | ✅ 200 |
| 1.3 | Login (Admin) | ✅ 200 |
| 1.4 | Update Supplier Categories | ✅ 200 |

#### Phase 2 — Procurement Lifecycle
| Step | Endpoint | Result |
|------|----------|--------|
| 2.1 | Create Demande | ✅ 201 |
| 2.2 | Matching Engine (Scores) | ✅ 200 — score: 61 |
| 3.1 | Get Supplier Feed | ✅ 200 |
| 4.1 | Submit Response (DISPONIBLE) | ✅ 201 |
| 5.1 | View Responses (Buyer) | ✅ 200 |
| 6.1 | Close Demande | ✅ 200 |
| 7.1 | Cancel Demande (fresh one) | ✅ 200 |

#### Phase 3 — Admin Oversight
| Step | Endpoint | Result |
|------|----------|--------|
| 9.1 | List All Demandes | ✅ 200 |
| 9.2 | Force Close | ✅ 200 |
| 9.3 | Recategorize + re-match | ✅ 200 |
| 9.4 | Platform Stats | ✅ 200 |
| 9.5 | Manual Expire | ❌ 500 — bug |

#### Phase 4 — Security Checks
| Step | Test | Result |
|------|------|--------|
| 10.1 | Buyer blocked from admin routes | ✅ 403 |
| 10.2 | Unauthenticated request response code | ❌ Got 403, expected 401 |

---

## Bruno Collection Fix Applied (March 18, 2026)

- **File:** `users/update-categories.bru`
- **Change:** Test assertion updated from `categories` → `categoryIds` to match actual response field name.
