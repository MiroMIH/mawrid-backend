# Mawrid API — Bruno Collection Guide

## Overview

Mawrid is a B2B procurement platform. There are 3 roles:

| Role | What they do |
|------|-------------|
| **BUYER** | Creates procurement requests (demandes) |
| **SUPPLIER** | Receives matched demandes and responds with availability |
| **ADMIN / SUPERADMIN** | Full platform oversight |

---

## Environment: `local`

Base URL: `http://localhost:8080/api/v1`

| Variable | Value | Set by |
|----------|-------|--------|
| `buyerToken` | JWT for buyer | `auth/login.bru` script |
| `supplierToken` | JWT for supplier | `auth/login-supplier.bru` script |
| `adminToken` | JWT for admin | `auth/login-admin.bru` script |
| `demandeId` | UUID of last created demande | `demandes/create-demande.bru` script |
| `reponseId` | UUID of last submitted response | `reponses/submit-reponse.bru` script |
| `categoryId` | Category ID to use (default: 1) | Manual |

---

## Test Accounts (auto-seeded on app startup)

| Email | Password | Role |
|-------|----------|------|
| buyer@test.com | Password1! | BUYER |
| buyer2@test.com | Password1! | BUYER |
| supplier@test.com | Password1! | SUPPLIER |
| supplier2@test.com | Password1! | SUPPLIER |
| supplier3@test.com | Password1! | SUPPLIER |
| superadmin@mawrid.dz | SuperAdmin@2026 | SUPERADMIN |

---

## CRITICAL: How Matching Works

**The supplier feed only shows demandes after matching has run.**

Matching is triggered automatically when a demande is created. It finds all suppliers
whose subscribed categories match the demande's category. The process is:

1. Buyer creates a demande in category X
2. Matching engine (async, runs in ~100ms) finds suppliers subscribed to category X
   or any ancestor category
3. A `DemandeSupplierScore` record is created for each matched supplier
4. The supplier's `/reponses/feed` only shows demandes they are matched to

**If the supplier feed is empty:** the supplier is not subscribed to any category
that matches the demande. Fix: call `PATCH /users/me/categories` with the same
category IDs used in the demande.

---

## Complete Test Flow (run in this exact order)

### PHASE 1 — Setup (run once)

**Step 1.1** — Login as buyer → saves `buyerToken`
```
auth/login.bru
```

**Step 1.2** — Login as supplier → saves `supplierToken`
```
auth/login-supplier.bru
```

**Step 1.3** — Login as admin → saves `adminToken`
```
auth/login-admin.bru
```

**Step 1.4** — Subscribe supplier to categories (REQUIRED for matching to work)
```
users/update-categories.bru
```
This uses `supplierToken`. It subscribes the supplier to categoryIds [1, 2, 3].
The demande will use categoryId 1, so this ensures the supplier is matched.

---

### PHASE 2 — Buyer creates a demande

**Step 2.1** — Create demande (saves `demandeId` to env)
```
demandes/create-demande.bru
```
Uses `buyerToken`. Creates a demande in category 1.
After 201 response, `demandeId` is saved automatically by the script.

**Step 2.2** — List my demandes
```
demandes/list-demandes.bru
```
Optional `?status=OPEN` filter. Buyer sees only their own demandes.

**Step 2.3** — Get demande detail
```
demandes/get-demande.bru
```
Buyer sees full detail. Returns 403 if not owner.

---

### PHASE 3 — Supplier sees and responds

**Step 3.1** — Check supplier feed
```
reponses/get-feed.bru
```
Uses `supplierToken`. Shows matched open demandes ordered by score.
If empty: repeat Step 1.4 first, then Step 2.1 again with a new demande.

**Step 3.2** — Get feed item detail
```
reponses/get-feed-item.bru
```
Uses `supplierToken` + `demandeId`. Shows score breakdown for this demande.
Returns 403 if supplier is not matched to this demande.

**Step 3.3** — Submit response as DISPONIBLE
```
reponses/submit-reponse.bru
```
Uses `supplierToken` + `demandeId`. Saves `reponseId`.
Will return 409 if already responded.
Will return 403 if not matched to this demande.

**Step 3.4** — Update response (within 1 hour only)
```
reponses/update-reponse.bru
```
Uses `supplierToken` + `demandeId`. Changes to INDISPONIBLE.
Returns 400 if more than 60 minutes since initial submission.

**Step 3.5** — List my responses
```
reponses/list-reponses.bru
```
Uses `supplierToken`. Shows all responses this supplier has submitted.

---

### PHASE 4 — Buyer sees responses

**Step 4.1** — Get responses for my demande
```
demandes/get-demande-reponses.bru
```
Uses `buyerToken` + `demandeId`. Returns all responses ranked by matching score.
`supplierScore` field shows how well the supplier matched.

**Step 4.2** — Close the demande
```
demandes/close-demande.bru
```
Uses `buyerToken` + `demandeId`. Sets status to CLOSED.
Only works on OPEN demandes.

---

### PHASE 5 — Admin oversight

All admin endpoints use `adminToken`.

**Step 5.1** — List all demandes
```
admin/demandes/01-list-all-demandes.bru
```
Optional `?status=OPEN|CLOSED|CANCELLED|EXPIRED` filter.

**Step 5.2** — Get demande detail (admin view, no ownership check)
```
admin/demandes/02-get-demande.bru
```

**Step 5.3** — Get score breakdown for all matched suppliers
```
admin/demandes/03-get-scores.bru
```
Returns: categoryScore, proximityScore, urgencyScore, buyerScore, quantityScore,
baseScore, decayFactor, finalScore, notificationTier per supplier.

**Step 5.4** — Force close any demande
```
admin/demandes/04-force-close.bru
```

**Step 5.5** — Recategorize a demande (re-runs matching)
```
admin/demandes/05-recategorize.bru
```
Body: `{ "newCategoryId": 1 }`

**Step 5.6** — Expire a demande manually
```
admin/demandes/06-expire.bru
```
Normally done automatically nightly by the scheduler.

**Step 5.7** — Platform stats
```
admin/demandes/07-stats.bru
```
Returns: totalOpen, totalClosed, totalCancelled, totalExpired, totalAll.

---

## Error Reference

| Status | Meaning |
|--------|---------|
| 400 | Validation failed or business rule violation (see `message` field) |
| 401 | Not authenticated — run the login request first |
| 403 | Wrong role or not the owner / not matched |
| 404 | Resource not found |
| 409 | Conflict — already responded, or can't cancel with DISPONIBLE responses |

### Common errors and fixes

| Error message | Fix |
|---------------|-----|
| "Invalid email or password" | Check credentials in env vars |
| "You are not matched to this demande" | Subscribe supplier to category first (Step 1.4), then create a new demande |
| "Cannot respond to a CLOSED demande" | The demande was closed — create a fresh one |
| "You have already responded to this demande" | Each supplier can only respond once per demande |
| "Response can only be updated within 1 hour" | Update window expired |
| "Cannot cancel: X supplier(s) have confirmed availability" | A supplier responded DISPONIBLE — use close instead of cancel |

---

## API Response Format

All endpoints return:
```json
{
  "success": true,
  "message": "optional message",
  "data": { ... },
  "timestamp": "2026-03-18T12:00:00Z"
}
```

On error:
```json
{
  "success": false,
  "message": "error description",
  "timestamp": "2026-03-18T12:00:00Z"
}
```

Paginated responses have `data` shaped as:
```json
{
  "content": [...],
  "totalElements": 5,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

---

## DemandeStatus values

| Status | Meaning |
|--------|---------|
| OPEN | Active, suppliers can respond |
| CLOSED | Manually closed by buyer or force-closed by admin |
| CANCELLED | Cancelled by buyer (only if no DISPONIBLE responses) |
| EXPIRED | Deadline passed — set automatically by nightly scheduler |

## Score Tiers (notification)

| Score | Tier | Notification |
|-------|------|-------------|
| >= 80 | IMMEDIATE | Push sent instantly |
| 50-79 | DELAYED_15MIN | Push sent after 15 min |
| 30-49 | DELAYED_1H | Push sent after 1 hour |
| < 30 | FEED_ONLY | No push, appears in feed only |
