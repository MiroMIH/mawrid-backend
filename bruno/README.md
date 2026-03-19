# Mawrid API — Bruno Collection Reference

## Platform Overview

Mawrid is a B2B procurement platform connecting buyers and suppliers in Algeria.

| Role | Description |
|------|-------------|
| **BUYER** | Posts procurement requests (demandes) and reviews supplier responses |
| **SUPPLIER** | Receives matched demandes via a scored feed and submits availability responses |
| **ADMIN / SUPERADMIN** | Full platform oversight — users, demandes, categories, stats |

---

## Environment: `local`

**Base URL:** `http://localhost:8080/api/v1`

### Environment Variables

| Variable | Description | Set by |
|----------|-------------|--------|
| `baseUrl` | `http://localhost:8080/api/v1` | `environments/local.bru` |
| `buyerToken` | Access JWT for buyer | `auth/login.bru` post-response script |
| `supplierToken` | Access JWT for supplier | `auth/login-supplier.bru` post-response script |
| `adminToken` | Access JWT for admin | `auth/login-admin.bru` post-response script |
| `accessToken` | Generic access token (shared scripts) | Set by login scripts |
| `refreshToken` | Refresh JWT | Set by login scripts |
| `demandeId` | UUID of last created demande | `buyer/demandes/create-demande.bru` post-response script |
| `reponseId` | UUID of last submitted reponse | `seller/reponses/submit-reponse.bru` post-response script |
| `categoryId` | Category ID for admin tests (default: 1) | Set manually or by admin category tests |
| `categorySlug` | Slug of a category for slug lookup tests | Set manually |

### Test Accounts (auto-seeded on app startup)

| Email | Password | Role |
|-------|----------|------|
| buyer@test.com | Password1! | BUYER |
| buyer2@test.com | Password1! | BUYER |
| supplier@test.com | Password1! | SUPPLIER |
| supplier2@test.com | Password1! | SUPPLIER |
| supplier3@test.com | Password1! | SUPPLIER |
| superadmin@mawrid.dz | SuperAdmin@2026 | SUPERADMIN |

---

## API Architecture

All routes are under `/api/v1`. Routes are segmented by role:

| Prefix | Who can call it | Auth required |
|--------|----------------|---------------|
| `/auth/**` | Anyone | No |
| `/users/me` | Any logged-in user | Yes (any role) |
| `/categories/**` | Any logged-in user | Yes (any role) |
| `/buyer/**` | BUYER role only | Yes (`ROLE_BUYER`) |
| `/seller/**` | SUPPLIER role only | Yes (`ROLE_SUPPLIER`) |
| `/admin/**` | ADMIN or SUPERADMIN only | Yes (`ROLE_ADMIN` or `ROLE_SUPERADMIN`) |

---

## Standard Response Format

All endpoints return this envelope:

```json
{
  "success": true,
  "message": "optional message",
  "data": { },
  "timestamp": "2026-03-19T10:00:00Z"
}
```

Error responses:
```json
{
  "success": false,
  "message": "Human-readable error description",
  "timestamp": "2026-03-19T10:00:00Z"
}
```

Paginated `data` shape:
```json
{
  "content": [...],
  "totalElements": 42,
  "totalPages": 3,
  "size": 20,
  "number": 0
}
```

---

## HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK |
| 201 | Created |
| 400 | Validation error or business rule violation — read `message` |
| 401 | Not authenticated — token missing or expired |
| 403 | Authenticated but wrong role or not the owner |
| 404 | Resource not found |
| 409 | Conflict — duplicate or state violation |
| 500 | Server error |

---

## Endpoint Reference

### 1. Auth — `/api/v1/auth/**` (public, no token required)

#### POST `/auth/register`
Register a new user as BUYER or SUPPLIER.

**Request body:**
```json
{
  "email": "newuser@test.com",
  "password": "Password1!",
  "firstName": "Ahmed",
  "lastName": "Benali",
  "phone": "0555123456",
  "wilaya": "16",
  "role": "BUYER"
}
```
`role` must be `"BUYER"` or `"SUPPLIER"`.

**Response (201):** `AuthTokenResponse` with `accessToken`, `refreshToken`, `user`.

**Error cases:**
- 400 — missing/invalid fields, weak password
- 409 — email already registered

---

#### POST `/auth/login`
Authenticate and receive JWT tokens.

**Request body:**
```json
{
  "email": "buyer@test.com",
  "password": "Password1!"
}
```

**Response (200):**
```json
{
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "user": {
      "id": "uuid",
      "email": "buyer@test.com",
      "role": "BUYER",
      "firstName": "Ahmed",
      "wilaya": "16"
    }
  }
}
```

**Error cases:**
- 400 — invalid credentials

---

#### POST `/auth/refresh`
Exchange a refresh token for a new access token.

**Request body:**
```json
{ "refreshToken": "eyJ..." }
```

**Response (200):** new `accessToken` and `refreshToken`.

**Error cases:**
- 401 — refresh token expired or blacklisted

---

#### POST `/auth/logout`
Blacklist the current access token (Redis TTL = remaining token life).

**Headers:** `Authorization: Bearer {accessToken}`

**Response (200):** `{ "data": null, "message": "Logged out" }`

---

### 2. User Profile — `/api/v1/users/**` (any authenticated role)

#### GET `/users/me`
Get the profile of the currently authenticated user (works for all roles).

**Headers:** `Authorization: Bearer {token}`

**Response (200):**
```json
{
  "data": {
    "id": "uuid",
    "email": "buyer@test.com",
    "firstName": "Ahmed",
    "lastName": "Benali",
    "phone": "0555123456",
    "wilaya": "16",
    "role": "BUYER",
    "enabled": true,
    "categoryIds": []
  }
}
```
`categoryIds` is always present — non-empty only for SUPPLIER.

---

#### PATCH `/users/me`
Update profile fields for the authenticated user.

**Request body (all fields optional):**
```json
{
  "firstName": "Ahmed",
  "lastName": "Benali",
  "phone": "0555999888",
  "wilaya": "31"
}
```

**Response (200):** Updated `UserResponse`.

---

#### PATCH `/users/me/fcm-token`
Register or update the Firebase Cloud Messaging device token (used for push notifications).

**Request body:**
```json
{ "fcmToken": "device-fcm-token-from-flutter" }
```

**Response (200):** `{ "data": null, "message": "FCM token updated" }`

---

### 3. Categories — `/api/v1/categories/**` (any authenticated role)

These are read-only endpoints shared across all roles. Authentication is required.

#### GET `/categories/tree`
Get the full active category tree (recursive, all depths).

**Response (200):**
```json
{
  "data": [
    {
      "id": 1,
      "name": "Construction",
      "slug": "construction",
      "depth": 0,
      "nodeType": "SECTOR",
      "active": true,
      "children": [
        {
          "id": 2,
          "name": "Ciment",
          "slug": "ciment",
          "depth": 1,
          "nodeType": "CATEGORY",
          "active": true,
          "children": []
        }
      ]
    }
  ]
}
```

**Notes:**
- Inactive categories and their subtrees are excluded.
- Cached in Redis under key `"categories"`.

---

#### GET `/categories/{id}`
Get a single category by its numeric ID.

**Path param:** `id` — Long (e.g., `1`)

**Response (200):** `CategoryResponse` with `id`, `name`, `slug`, `depth`, `nodeType`, `parentId`, `active`, `leaf`.

**Error cases:**
- 404 — category not found

---

#### GET `/categories/slug/{slug}`
Get a category by its URL slug.

**Path param:** `slug` — String (e.g., `"ciment"`)

**Response (200):** Same as `GET /categories/{id}`.

**Error cases:**
- 404 — slug not found

---

#### GET `/categories/{id}/attributes`
Get the effective attribute schema for a category — own attributes plus inherited from all ancestors.

**Response (200):**
```json
{
  "data": [
    {
      "id": 10,
      "key": "marque",
      "label": "Marque",
      "type": "TEXT",
      "required": false,
      "displayOrder": 1,
      "options": null
    }
  ]
}
```

`type` values: `TEXT`, `NUMBER`, `SELECT`.
`options` is non-null only for `SELECT` type.

---

### 4. Buyer Endpoints — `/api/v1/buyer/**` (BUYER role only)

All buyer endpoints require `Authorization: Bearer {buyerToken}`.

#### POST `/buyer/demandes`
Create a new procurement request.

**Request body:**
```json
{
  "title": "Besoin de ciment Portland 500 sacs",
  "description": "Ciment de qualité supérieure pour construction résidentielle",
  "quantity": 500,
  "unit": "sacs",
  "deadline": "2026-06-30",
  "categoryId": 1,
  "wilaya": "16",
  "attributes": [
    { "key": "marque", "value": "Lafarge", "custom": false }
  ]
}
```

**Response (201):** Full `DemandeResponse`.

**Side effects:**
- Matching engine runs asynchronously (~100ms): finds all suppliers subscribed to the demande's category or any ancestor, creates `DemandeSupplierScore` records, and queues notifications by tier.

**Error cases:**
- 400 — validation failure, invalid `categoryId`, `deadline` in the past
- 403 — caller is not BUYER

---

#### GET `/buyer/demandes`
List all demandes created by the authenticated buyer (paginated).

**Query params:**
- `status` (optional): `OPEN`, `CLOSED`, `CANCELLED`, `EXPIRED`
- `page` (default: 0), `size` (default: 20), `sort` (default: `createdAt,desc`)

**Response (200):** Paginated `DemandeSummaryResponse`.

---

#### GET `/buyer/demandes/{id}`
Get full detail of one of the buyer's demandes.

**Path param:** `id` — UUID

**Response (200):** `DemandeResponse`.

**Error cases:**
- 403 — demande belongs to another buyer
- 404 — demande not found

---

#### GET `/buyer/demandes/{id}/reponses`
Get all supplier responses for a specific demande, ranked by matching score (descending).

**Path param:** `id` — UUID

**Query params:** `page`, `size`

**Response (200):** Paginated `ReponseResponse` with supplier info and score breakdown.

**Error cases:**
- 403 — not the owner of this demande

---

#### PATCH `/buyer/demandes/{id}/close`
Manually close an OPEN demande.

**Path param:** `id` — UUID

**Response (200):** Updated `DemandeResponse` with `status: "CLOSED"`.

**Error cases:**
- 400 — demande is not OPEN
- 403 — not the owner

---

#### DELETE `/buyer/demandes/{id}`
Cancel a demande (soft delete — sets status to `CANCELLED`).

**Path param:** `id` — UUID

**Response (200):** `{ "data": null, "message": "Demande cancelled" }`

**Error cases:**
- 400 — demande already closed or expired
- 403 — not the owner
- 409 — one or more suppliers responded DISPONIBLE (use close instead)

---

### 5. Seller Endpoints — `/api/v1/seller/**` (SUPPLIER role only)

All seller endpoints require `Authorization: Bearer {supplierToken}`.

#### GET `/seller/categories/subscribed`
Get the list of categories the authenticated supplier is subscribed to.

**Response (200):**
```json
{
  "data": [
    { "id": 1, "name": "Construction", "slug": "construction" },
    { "id": 2, "name": "Ciment", "slug": "ciment" }
  ]
}
```

---

#### PATCH `/seller/categories`
Update (replace) the supplier's category subscriptions.

**Request body:**
```json
{ "categoryIds": [1, 2, 3] }
```

**Response (200):** Updated `UserResponse` with the new `categoryIds`.

**Notes:**
- This is a full replace — existing subscriptions not in the list are removed.
- Only leaf-node categories should be used; subscribing to a parent also matches all its children.

**Error cases:**
- 400 — one or more category IDs do not exist

---

#### GET `/seller/feed`
Get the supplier's personalized demande feed — only demandes for which the supplier has been matched, ordered by `finalScore` descending.

**Query params:**
- `categoryId` (optional): filter feed by category
- `page` (default: 0), `size` (default: 20)

**Response (200):** Paginated `DemandeSummaryResponse` with score fields:
- `finalScore` — overall match score (0–100)
- `categoryScore` — category match quality
- `proximityScore` — buyer/supplier wilaya proximity
- `urgencyScore` — based on deadline closeness
- `notificationTier` — `IMMEDIATE`, `DELAYED_15MIN`, `DELAYED_1H`, `FEED_ONLY`

**Notes:**
- Empty feed = supplier not subscribed to any category matching existing demandes.
- Fix: call `PATCH /seller/categories` with relevant category IDs, then create a new demande.

---

#### GET `/seller/feed/{demandeId}`
Get the detail of a single demande from the supplier's feed, including the score breakdown for this specific supplier.

**Path param:** `demandeId` — UUID

**Response (200):** `DemandeSummaryResponse` with score breakdown.

**Error cases:**
- 403 — supplier is not matched to this demande (not in their feed)
- 404 — demande not found

---

#### POST `/seller/reponses/{demandeId}`
Submit a response to a matched demande.

**Path param:** `demandeId` — UUID

**Request body:**
```json
{
  "status": "DISPONIBLE",
  "note": "En stock, livraison sous 3 jours"
}
```
`status` must be `"DISPONIBLE"` or `"INDISPONIBLE"`.

**Response (201):** `ReponseResponse`.

**Error cases:**
- 403 — supplier not matched to this demande
- 400 — demande is not OPEN
- 409 — supplier already responded to this demande (unique constraint per supplier per demande)

---

#### PATCH `/seller/reponses/{demandeId}`
Update a previously submitted response. Only allowed within 1 hour of initial submission.

**Path param:** `demandeId` — UUID

**Request body:** Same as POST above.

**Response (200):** Updated `ReponseResponse`.

**Error cases:**
- 400 — more than 60 minutes since submission
- 404 — no prior response found for this supplier/demande combination

---

#### GET `/seller/reponses`
List all responses the authenticated supplier has submitted (paginated).

**Query params:** `page` (default: 0), `size` (default: 20)

**Response (200):** Paginated `ReponseResponse` list.

---

### 6. Admin Endpoints — `/api/v1/admin/**` (ADMIN or SUPERADMIN only)

All admin endpoints require `Authorization: Bearer {adminToken}`.

#### GET `/admin/users`
List all platform users (paginated).

**Query params:** `page`, `size`

**Response (200):** Paginated `UserResponse`.

---

#### PATCH `/admin/users/{id}/toggle-enabled`
Enable or disable a user account. Disabled users cannot log in.

**Path param:** `id` — Long (user numeric ID)

**Response (200):** `{ "data": { "enabled": false } }`

---

#### GET `/admin/stats`
Get platform dashboard statistics.

**Response (200):**
```json
{
  "data": {
    "totalUsers": 120,
    "totalBuyers": 60,
    "totalSuppliers": 58,
    "totalAdmins": 2,
    "totalDemandes": 340,
    "openDemandes": 45,
    "closedDemandes": 210,
    "cancelledDemandes": 55,
    "expiredDemandes": 30
  }
}
```

---

#### POST `/admin/matching/simulate`
Dry-run the matching engine for a hypothetical demande. No DB writes.

**Request body:**
```json
{
  "categoryId": 1,
  "wilaya": "16",
  "quantity": 500
}
```

**Response (200):** List of matched suppliers with predicted scores.

---

#### GET `/admin/demandes`
List all demandes platform-wide, with full filters.

**Query params:**
- `status`: `OPEN`, `CLOSED`, `CANCELLED`, `EXPIRED`
- `buyerId` (UUID, optional)
- `categoryId` (Long, optional)
- `page`, `size`

**Response (200):** Paginated `DemandeResponse` (admin view, no ownership restriction).

---

#### GET `/admin/demandes/{id}`
Get full admin view of a demande: includes all responses and score records.

**Path param:** `id` — UUID

**Response (200):** Extended `DemandeResponse` with `reponses[]` and `scores[]`.

**Error cases:**
- 404 — not found

---

#### GET `/admin/demandes/{id}/scores`
Get the matching score breakdown for every supplier matched to this demande.

**Response (200):**
```json
{
  "data": [
    {
      "supplierId": "uuid",
      "supplierEmail": "supplier@test.com",
      "categoryScore": 40,
      "proximityScore": 20,
      "urgencyScore": 10,
      "buyerScore": 5,
      "quantityScore": 5,
      "baseScore": 80,
      "decayFactor": 0.95,
      "finalScore": 76.0,
      "notificationTier": "DELAYED_15MIN"
    }
  ]
}
```

---

#### PATCH `/admin/demandes/{id}/force-close`
Force a demande to CLOSED status regardless of ownership or current status.

**Response (200):** Updated `DemandeResponse`.

---

#### PATCH `/admin/demandes/{id}/recategorize`
Move a demande to a different category and re-run the matching engine.

**Request body:**
```json
{ "newCategoryId": 3 }
```

**Response (200):** Updated `DemandeResponse`.

---

#### PATCH `/admin/demandes/{id}/expire`
Manually mark a demande as EXPIRED. Normally done automatically by the nightly scheduler.

**Response (200):** Updated `DemandeResponse` with `status: "EXPIRED"`.

---

#### GET `/admin/demandes/stats`
Demande-specific platform statistics.

**Response (200):**
```json
{
  "data": {
    "totalOpen": 45,
    "totalClosed": 210,
    "totalCancelled": 55,
    "totalExpired": 30,
    "totalAll": 340
  }
}
```

---

### 7. Admin Category Management — `/api/v1/admin/categories/**`

Full CRUD for the category tree. All endpoints require `adminToken`.

#### POST `/admin/categories`
Create a new category node.

**Request body:**
```json
{
  "name": "Acier",
  "parentId": 1
}
```
Omit `parentId` to create a root-level sector.

**Response (201):** `CategoryResponse`.

**Error cases:**
- 400 — parent not found, parent is a leaf node
- 409 — name already exists under that parent

---

#### GET `/admin/categories/search`
Search and filter categories with pagination.

**Query params:**
- `q` — name search string
- `depth` — filter by depth level (0 = root sectors)
- `nodeType` — `SECTOR`, `CATEGORY`, `SUBCATEGORY`
- `active` — `true` or `false`
- `page`, `size`

**Response (200):** Paginated `CategoryResponse`.

---

#### GET `/admin/categories/{id}/stats`
Get statistics for a category: demande count, active supplier subscriptions.

**Response (200):** `CategoryStatsResponse`.

---

#### PATCH `/admin/categories/{id}/rename`
Rename a category.

**Request body:**
```json
{ "name": "New Name", "forceRename": false }
```
`forceRename: true` is required for seeded (built-in) categories.

**Error cases:**
- 400 — empty name, or seeded node without `forceRename`
- 409 — name already taken at that level

---

#### PATCH `/admin/categories/{id}/toggle-active`
Toggle active status. Deactivating a parent cascades to all children.

**Response (200):** Updated `CategoryResponse`.

---

#### POST `/admin/categories/{id}/move`
Move a category subtree to a new parent.

**Request body:**
```json
{ "newParentId": 5 }
```

**Error cases:**
- 400 — target parent is a leaf node, or target is a descendant of the node being moved (cycle prevention)

---

#### PATCH `/admin/categories/{id}/mark-leaf`
Mark a category as a leaf node — no children can be added under it.

**Error cases:**
- 400 — category already has children

---

#### PATCH `/admin/categories/{id}/unmark-leaf`
Remove leaf status from a category.

---

#### DELETE `/admin/categories/{id}`
Delete a category node.

**Error cases:**
- 400 — seeded node (built-in categories cannot be hard-deleted)
- 409 — category has children (delete children first)

---

#### POST `/admin/categories/{id}/attributes`
Add an attribute schema to a category. Child categories inherit this attribute.

**Request body:**
```json
{
  "key": "marque",
  "label": "Marque",
  "type": "TEXT",
  "required": false,
  "displayOrder": 1,
  "options": null
}
```
`type`: `TEXT`, `NUMBER`, `SELECT`. `options` required when type is `SELECT`.

**Error cases:**
- 409 — `key` already exists on this category

---

#### PATCH `/admin/categories/{id}/attributes/{attributeId}`
Update an attribute. The `key` field is immutable — it cannot be changed.

**Request body:** Same shape as POST above (new key value is ignored).

---

#### DELETE `/admin/categories/{id}/attributes/{attributeId}`
Delete an attribute schema from a category.

---

## Domain Models

### DemandeStatus

| Value | Description |
|-------|-------------|
| `OPEN` | Active — suppliers can respond |
| `CLOSED` | Manually closed by buyer or force-closed by admin |
| `CANCELLED` | Cancelled by buyer (only allowed if no DISPONIBLE responses) |
| `EXPIRED` | Deadline passed — set automatically by nightly scheduler |

### ReponseStatus

| Value | Description |
|-------|-------------|
| `DISPONIBLE` | Supplier confirms stock/service availability |
| `INDISPONIBLE` | Supplier cannot fulfill this request |

### NotificationTier

| Score Range | Tier | Behavior |
|-------------|------|----------|
| ≥ 80 | `IMMEDIATE` | Push notification sent instantly |
| 50–79 | `DELAYED_15MIN` | Push sent after 15 minutes |
| 30–49 | `DELAYED_1H` | Push sent after 1 hour |
| < 30 | `FEED_ONLY` | No push — appears in feed only |

### NodeType (categories)

| Value | Depth | Description |
|-------|-------|-------------|
| `SECTOR` | 0 | Top-level industry sector |
| `CATEGORY` | 1 | Main category under a sector |
| `SUBCATEGORY` | 2+ | Leaf-level subcategory |

---

## How Matching Works

1. Buyer creates a demande in category X (e.g., "Ciment", id=2).
2. Matching engine runs **asynchronously** (within ~100ms) — finds all suppliers subscribed to category 2, or any ancestor (e.g., category 1 "Construction").
3. For each matched supplier, a `DemandeSupplierScore` record is created with these components:
   - **categoryScore** — how closely the supplier's subscribed categories match
   - **proximityScore** — how close buyer and supplier wilayas are
   - **urgencyScore** — based on how soon the deadline is
   - **buyerScore** — buyer's historical activity score
   - **quantityScore** — based on quantity requested
   - **decayFactor** — score degrades over time for older demandes
   - **finalScore** = weighted sum × decayFactor
4. `GET /seller/feed` returns only demandes where a `DemandeSupplierScore` record exists for the authenticated supplier.

**Troubleshooting empty feed:**
- Ensure the supplier is subscribed (`PATCH /seller/categories`) to categories that overlap with the demande's category.
- Create a new demande after subscribing (matching only runs at demande creation time, or on admin recategorize).

---

## Complete Test Sequence

Run requests in this exact order for a full end-to-end flow:

### Phase 1 — Authentication & Setup

| Step | File | Token used | What it does |
|------|------|------------|--------------|
| 1.1 | `auth/login.bru` | none | Login as buyer → saves `buyerToken` |
| 1.2 | `auth/login-supplier.bru` | none | Login as supplier → saves `supplierToken` |
| 1.3 | `auth/login-admin.bru` | none | Login as admin → saves `adminToken` |
| 1.4 | `seller/reponses/update-categories.bru` *(was users/)* | `supplierToken` | Subscribe supplier to categoryIds [1, 2, 3] |

### Phase 2 — Buyer Creates a Demande

| Step | File | Token used | What it does |
|------|------|------------|--------------|
| 2.1 | `demandes/create-demande.bru` | `buyerToken` | Create demande in category 1 → saves `demandeId` |
| 2.2 | `demandes/list-demandes.bru` | `buyerToken` | List buyer's demandes (paginated) |
| 2.3 | `demandes/get-demande.bru` | `buyerToken` | Get full demande detail |

### Phase 3 — Supplier Sees and Responds

| Step | File | Token used | What it does |
|------|------|------------|--------------|
| 3.1 | `reponses/get-feed.bru` | `supplierToken` | View matched demande feed |
| 3.2 | `reponses/get-feed-item.bru` | `supplierToken` | View score detail for `demandeId` |
| 3.3 | `reponses/submit-reponse.bru` | `supplierToken` | Submit DISPONIBLE response → saves `reponseId` |
| 3.4 | `reponses/update-reponse.bru` | `supplierToken` | Update response to INDISPONIBLE (within 1h) |
| 3.5 | `reponses/list-reponses.bru` | `supplierToken` | List all supplier's responses |

### Phase 4 — Buyer Reviews Responses

| Step | File | Token used | What it does |
|------|------|------------|--------------|
| 4.1 | `demandes/get-demande-reponses.bru` | `buyerToken` | See responses ranked by score |
| 4.2 | `demandes/close-demande.bru` | `buyerToken` | Close the demande manually |

### Phase 5 — Admin Oversight

| Step | File | Token used | What it does |
|------|------|------------|--------------|
| 5.1 | `admin/demandes/01-list-all-demandes.bru` | `adminToken` | List all demandes platform-wide |
| 5.2 | `admin/demandes/02-get-demande.bru` | `adminToken` | Full admin view of a demande |
| 5.3 | `admin/demandes/03-get-scores.bru` | `adminToken` | Score breakdown per supplier |
| 5.4 | `admin/demandes/04-force-close.bru` | `adminToken` | Force close any demande |
| 5.5 | `admin/demandes/05-recategorize.bru` | `adminToken` | Move to new category + re-match |
| 5.6 | `admin/demandes/06-expire.bru` | `adminToken` | Manually expire a demande |
| 5.7 | `admin/demandes/07-stats.bru` | `adminToken` | Platform demande stats |
| 5.8 | `admin/stats/dashboard-stats.bru` | `adminToken` | Full dashboard stats |
| 5.9 | `admin/stats/simulate-matching.bru` | `adminToken` | Dry-run matching (no DB writes) |
| 5.10 | `admin/users/list-users.bru` | `adminToken` | List all users |
| 5.11 | `admin/users/toggle-user.bru` | `adminToken` | Enable/disable a user |

### Phase 6 — Admin Category Management

| Step | File | Token used | What it does |
|------|------|------------|--------------|
| 6.1 | `admin/categories/01-create-root.bru` | `adminToken` | Create root sector |
| 6.2 | `admin/categories/02-create-child.bru` | `adminToken` | Create child category |
| 6.3 | `admin/categories/03-create-grandchild.bru` | `adminToken` | Create grandchild subcategory |
| 6.4 | `admin/categories/04-rename-admin-node.bru` | `adminToken` | Rename an admin-created node |
| 6.5 | `admin/categories/05-rename-seeded-no-force.bru` | `adminToken` | Rename seeded node without force → expect 400 |
| 6.6 | `admin/categories/06-rename-seeded-with-force.bru` | `adminToken` | Rename seeded node with forceRename |
| 6.7 | `admin/categories/07-mark-as-leaf.bru` | `adminToken` | Mark node as leaf |
| 6.8 | `admin/categories/08-create-under-leaf-fail.bru` | `adminToken` | Try creating under a leaf → expect 400 |
| 6.9 | `admin/categories/09-unmark-leaf.bru` | `adminToken` | Remove leaf status |
| 6.10 | `admin/categories/10-add-attribute-text.bru` | `adminToken` | Add TEXT attribute |
| 6.11 | `admin/categories/11-add-attribute-select.bru` | `adminToken` | Add SELECT attribute with options |
| 6.12 | `admin/categories/12-add-attribute-number.bru` | `adminToken` | Add NUMBER attribute |
| 6.13 | `admin/categories/13-update-attribute.bru` | `adminToken` | Update attribute label/options |
| 6.14 | `admin/categories/14-move-category.bru` | `adminToken` | Move subtree to new parent |
| 6.15 | `admin/categories/15-move-to-descendant-fail.bru` | `adminToken` | Move to own descendant → expect 400 |
| 6.16 | `admin/categories/16-toggle-active-deactivate.bru` | `adminToken` | Deactivate (cascades to children) |
| 6.17 | `admin/categories/17-toggle-active-reactivate.bru` | `adminToken` | Reactivate |
| 6.18 | `admin/categories/18-get-stats.bru` | `adminToken` | Get category stats |
| 6.19 | `admin/categories/19-search-categories.bru` | `adminToken` | Search/filter categories |
| 6.20 | `admin/categories/20-delete-attribute.bru` | `adminToken` | Delete an attribute |
| 6.21 | `admin/categories/21-delete-category.bru` | `adminToken` | Delete a category node |

---

## Common Error Scenarios & Fixes

| Error message | HTTP | Root cause | Fix |
|---------------|------|-----------|-----|
| "Invalid email or password" | 400 | Wrong credentials | Check email/password in request |
| "You are not matched to this demande" | 403 | Supplier not in matching results | Subscribe to relevant category, create new demande |
| "Cannot respond to a CLOSED demande" | 400 | Demande already closed | Create a new demande |
| "You have already responded to this demande" | 409 | Unique constraint: 1 response per supplier per demande | Cannot respond twice |
| "Response can only be updated within 1 hour" | 400 | 60-minute update window expired | Cannot update anymore |
| "Cannot cancel: X supplier(s) have confirmed availability" | 409 | DISPONIBLE response exists | Use `PATCH /buyer/demandes/{id}/close` instead |
| "Category is a leaf node" | 400 | Trying to create a child under a leaf | Unmark leaf first |
| "SEEDED node cannot be renamed without forceRename" | 400 | Built-in category | Set `forceRename: true` |
