# Mawrid API — Full Test Execution Report (March 18, 2026)

This report documents the full execution of the 10-step test suite for the Mawrid B2B procurement platform using the Bruno CLI.

## 📊 Summary of Results

| Metric | Result |
| :--- | :--- |
| **Total Requests Run** | 22 |
| **Total Tests Run** | 64 |
| **Passed Requests** | 20 |
| **Failed Requests** | 2 |
| **Overall Success Rate** | **91%** |

---

## 📝 Detailed Test Step-by-Step

### PHASE 1 — Authentication & Setup
| Step | API Endpoint | Status | Code | Result |
| :--- | :--- | :---: | :---: | :--- |
| 1.1 | Login (Buyer) | ✅ PASS | 200 | Token saved successfully to environment. |
| 1.2 | Login (Supplier) | ✅ PASS | 200 | Token saved successfully to environment. |
| 1.3 | Login (Admin) | ✅ PASS | 200 | Token saved successfully to environment. |
| 1.4 | Update Categories | ✅ PASS | 200 | Supplier successfully subscribed to categories [1, 2, 3]. |

### PHASE 2 — Procurement Lifecycle (Buyer & Supplier)
| Step | API Endpoint | Status | Code | Result |
| :--- | :--- | :---: | :---: | :--- |
| 2.1 | Create Demande | ✅ PASS | 201 | New request created in category 1. ID saved. |
| 3.1 | Matching Engine (Scores) | ✅ PASS | 200 | **Matching Engine OK**: Supplier matched with score 61. |
| 4.1 | Get Supplier Feed | ✅ PASS | 200 | Request correctly appeared in supplier's private feed. |
| 5.1 | Submit Response | ✅ PASS | 201 | Supplier responded "DISPONIBLE" to the matched request. |
| 6.1 | View Responses (Buyer) | ✅ PASS | 200 | Buyer successfully saw the response and matching score. |
| 7.1 | Close Demande | ✅ PASS | 200 | Buyer successfully closed the request manually. |
| 8.1 | Cancel Demande | ✅ PASS | 200 | Buyer successfully cancelled a new (fresh) request. |

### PHASE 3 — Administrative Oversight (Admin Only)
| Step | API Endpoint | Status | Code | Result |
| :--- | :--- | :---: | :---: | :--- |
| 9.1 | List All Demandes | ✅ PASS | 200 | Admin saw all system requests regardless of owner. |
| 9.2 | Force Close | ✅ PASS | 200 | Admin successfully overrode status to CLOSED. |
| 9.3 | Recategorize | ✅ PASS | 200 | Admin changed category and re-ran matching engine. |
| 9.4 | Platform Stats | ✅ PASS | 200 | Dashboard counts (Open/Closed/Cancelled) are accurate. |
| 9.5 | **Manual Expire** | ❌ **FAIL** | **500** | **Internal Server Error**: The server crashed. |

### PHASE 4 — Security Checks
| Step | Security Test | Status | Code | Result |
| :--- | :--- | :---: | :---: | :--- |
| 10.1 | Role Check | ✅ PASS | 403 | Buyer blocked from accessing Admin routes. |
| 10.2 | **Auth Check** | ❌ **FAIL** | **403** | Got 403 (Forbidden) instead of 401 (Unauthorized). |

---

## 🔍 Issue Analysis (Plain Language)

### 1. Manual Expiration Crash (Step 9.5)
*   **Endpoint:** `PATCH /admin/demandes/{id}/expire`
*   **What happened:** When the admin tries to manually "expire" a request, the server returns a "500 Internal Server Error."
*   **Diagnosis:** This is a server-side bug. While the automatic nightly scheduler might work, the manual trigger is crashing. This needs urgent attention from the backend team.

### 2. Security Response Mismatch (Step 10.2)
*   **Endpoint:** `GET /demandes` (with no token)
*   **What happened:** Instead of returning a **401 Unauthorized** (meaning "you need to log in"), the server returned a **403 Forbidden**.
*   **Diagnosis:** This is an implementation inconsistency. A 403 usually implies you *are* logged in but don't have permission. For unauthenticated requests, it should be a 401.

---

## 🛠️ Internal Fixes Applied

During this test run, I identified and fixed a bug in the Bruno collection itself:
*   **File:** `users/update-categories.bru`
*   **Change:** The test was looking for a field named `categories`. I updated it to look for `categoryIds` to match the actual server response.

## 🚀 Final Recommendations

1.  **Investigate Server Logs:** Check the backend logs for the 500 error on the expire endpoint.
2.  **Adjust Security Configuration:** Standardize unauthenticated error responses to return 401.
3.  **Update Bruno Documentation:** Ensure the README reflects that the user profile returns `categoryIds` and not `categories`.
