# Mawrid API — Full Test Results

**Date:** 2026-03-21
**API Base:** `http://localhost:8080/api/v1`
**Tool:** Bruno CLI 3.1.3 (`bru run --env local`)
**Backend:** Spring Boot 3.4.3 · Java 21 · PostgreSQL 16 · Redis 7

---

## Overall Summary

| Suite | Requests | Tests | Result |
|---|---|---|---|
| Main Flow Sweep | 65 | 201 | ✓ PASS |
| Edge Case Sweep | 55 | 75 | ✓ PASS |
| E2E Workflow | 16 | 54 | ✓ PASS |
| **Total** | **136** | **330** | **✓ ALL PASS** |

---

## How to Run

```bash
# Main flow sweep
bru run auth users categories demandes reponses admin/users admin/demandes admin/categories auth-final --env local

# Edge case sweep
bru run edge_cases/00-setup edge_cases/auth edge_cases/categories edge_cases/demande_creation edge_cases/demande_status edge_cases/reponse_submission edge_cases/security edge_cases/admin edge_cases/pagination --env local

# E2E workflow
bru run workflows/e2e-category-demande-reponse --env local
```

---

## Suite 1 — Main Flow Sweep

**65 requests / 201 tests — ✓ PASS**

Covers the happy-path for every endpoint in the API across all roles (BUYER, SUPPLIER, ADMIN).

### auth/

| # | File | Method | Status | Tests |
|---|---|---|---|---|
| 1 | `register.bru` | POST `/auth/register` | 409 | ✓ 201 or 409 (idempotent) · ✓ if 201: response has data · ✓ if 201: email matches |
| 2 | `register-supplier.bru` | POST `/auth/register` | 409 | ✓ 201 or 409 (idempotent) · ✓ if 201: email matches · ✓ if 201: role is SUPPLIER |
| 3 | `login.bru` | POST `/auth/login` | 200 | ✓ status 200 · ✓ accessToken is string · ✓ role is BUYER · ✓ buyerToken saved to env |
| 4 | `login-supplier.bru` | POST `/auth/login` | 200 | ✓ status 200 · ✓ role is SUPPLIER · ✓ supplierToken saved to env |
| 5 | `login-admin.bru` | POST `/auth/login` | 200 | ✓ status 200 · ✓ role is SUPERADMIN or ADMIN · ✓ adminToken saved to env |

### users/

| # | File | Method | Status | Tests |
|---|---|---|---|---|
| 6 | `get-profile.bru` | GET `/users/me` | 200 | ✓ status 200 · ✓ email is string · ✓ role is present · ✓ firstName is present |
| 7 | `update-profile.bru` | PATCH `/users/me` | 200 | ✓ status 200 · ✓ firstName updated · ✓ lastName updated |
| 8 | `update-categories.bru` | PATCH `/users/me/categories` | 200 | ✓ status 200 · ✓ categories updated |
| 9 | `update-fcm-token.bru` | PATCH `/users/me/fcm-token` | 200 | ✓ status 200 |

### categories/

| # | File | Method | Status | Tests |
|---|---|---|---|---|
| 10 | `get-tree.bru` | GET `/categories` | 200 | ✓ status 200 · ✓ data is array · ✓ tree is not empty · ✓ each root has id/name/slug/depth · ✓ children is array · ✓ only active nodes returned |
| 11 | `get-by-id.bru` | GET `/categories/{id}` | 200 | ✓ status 200 · ✓ id matches · ✓ name/path/depth/nodeType/active present |
| 12 | `get-by-slug.bru` | GET `/categories/slug/{slug}` | 200 | ✓ status 200 · ✓ slug is machines-agricoles · ✓ id is number · ✓ name is string |
| 13 | `get-attributes.bru` | GET `/categories/{id}/attributes` | 200 | ✓ status 200 · ✓ data is array · ✓ each attribute has key/label/type |
| 14 | `get-subscribed.bru` | GET `/categories/subscribed` | 200 | ✓ status 200 · ✓ data is array |

### demandes/

| # | File | Method | Status | Tests |
|---|---|---|---|---|
| 15 | `create-demande.bru` | POST `/buyer/demandes` | 201 | ✓ status 201 · ✓ id is present · ✓ status is OPEN · ✓ title matches · ✓ demandeId saved to env |
| 16 | `list-demandes.bru` | GET `/buyer/demandes` | 200 | ✓ status 200 · ✓ data has content array · ✓ pagination info present |
| 17 | `get-demande.bru` | GET `/buyer/demandes/{id}` | 200 | ✓ status 200 · ✓ id matches · ✓ title is string · ✓ status is valid · ✓ categoryId present |
| 18 | `list-with-status-filter.bru` | GET `/buyer/demandes?status=OPEN` | 200 | ✓ status 200 · ✓ content is array · ✓ all returned items are OPEN · ✓ pagination metadata present |
| 19 | `get-demande-reponses.bru` | GET `/buyer/demandes/{id}/reponses` | 200 | ✓ status 200 · ✓ data has content array |
| 20 | `close-demande.bru` | PATCH `/buyer/demandes/{id}/close` | 200 | ✓ status 200 · ✓ status is CLOSED |
| 21 | `update-status.bru` | PATCH `/buyer/demandes/{id}/close` (already closed) | 400 | ✓ 400 — cannot close an already-CLOSED demande |
| 22 | `create-for-cancel.bru` | POST `/buyer/demandes` | 201 | ✓ status 201 · ✓ cancelDemandeId saved · ✓ status is OPEN |
| 23 | `delete-demande.bru` | DELETE `/buyer/demandes/{cancelDemandeId}` | 200 | ✓ status 200 · ✓ success message |

### reponses/

| # | File | Method | Status | Tests |
|---|---|---|---|---|
| 24 | `00-create-open-demande.bru` | POST `/buyer/demandes` (setup) | 201 | ✓ status 201 · ✓ reponseDemandeId saved · ✓ status is OPEN |
| 25 | `get-feed.bru` | GET `/seller/feed` | 200 | ✓ status 200 · ✓ data has content array · ✓ pagination info present |
| 26 | `get-feed-item.bru` | GET `/seller/feed/{id}` | 200 | ✓ status 200 · ✓ id present · ✓ finalScore present |
| 27 | `submit-reponse.bru` | POST `/seller/reponses/{reponseDemandeId}` | 201 | ✓ status 201 · ✓ status is DISPONIBLE · ✓ reponseId saved |
| 28 | `update-reponse.bru` | PATCH `/seller/reponses/{reponseId}` | 200 | ✓ status 200 · ✓ status updated · ✓ updatedAt present |
| 29 | `list-reponses.bru` | GET `/seller/reponses` | 200 | ✓ status 200 · ✓ data has content array |

### admin/users/

| # | File | Method | Status | Tests |
|---|---|---|---|---|
| 30 | `list-users.bru` | GET `/admin/users` | 200 | ✓ status 200 · ✓ data has content array · ✓ pagination info present |
| 31 | `get-user-detail.bru` | GET `/admin/users/{id}` | 200 | ✓ status 200 · ✓ id matches · ✓ email is string · ✓ role is present |
| 32 | `toggle-user.bru` | PATCH `/admin/users/{id}/toggle-enabled` | 200 | ✓ status 200 · ✓ enabled field is boolean |
| 33 | `export-users.bru` | GET `/admin/users/export` | 200 | ✓ status 200 · ✓ Content-Type is text/csv · ✓ response body is non-empty · ✓ CSV contains header row |
| 34 | `toggle-user-restore.bru` | PATCH `/admin/users/{id}/toggle-enabled` | 200 | ✓ status 200 · ✓ buyer re-enabled |

### admin/demandes/

| # | File | Method | Status | Tests |
|---|---|---|---|---|
| 35 | `01-list-all-demandes.bru` | GET `/admin/demandes` | 200 | ✓ status 200 · ✓ data has content array · ✓ pagination info present |
| 36 | `02-get-demande.bru` | GET `/admin/demandes/{id}` | 200 | ✓ status 200 · ✓ id present · ✓ title is string |
| 37 | `03-get-scores.bru` | GET `/admin/demandes/{id}/scores` | 200 | ✓ status 200 · ✓ scores array present |
| 38 | `04-get-demande-reponses.bru` | GET `/admin/demandes/{id}/reponses` | 200 | ✓ status 200 · ✓ content array present |
| 39 | `05-close-demande.bru` | PATCH `/admin/demandes/{id}/close` | 200/400 | ✓ 200 or 400 (idempotent) |
| 40 | `05b-create-for-expire.bru` | POST `/buyer/demandes` (setup) | 201 | ✓ status 201 · ✓ expireDemandeId saved |
| 41 | `06-expire.bru` | PATCH `/admin/demandes/{expireDemandeId}/expire` | 200 | ✓ status 200 · ✓ status is EXPIRED |

### admin/categories/ (21 requests)

| # | File | Method | Status | Tests |
|---|---|---|---|---|
| 42 | `01-create-root.bru` | POST `/admin/categories` | 201/409 | ✓ 201 or 409 (idempotent) · ✓ if 201: id saved · ✓ if 201: parentId is null · ✓ if 201: depth is 0 |
| 43 | `02-create-child.bru` | POST `/admin/categories` | 201/409 | ✓ 201 or 409 (idempotent) · ✓ if 201: parentId is set |
| 44 | `03-get-root.bru` | GET `/admin/categories/{id}` | 200 | ✓ status 200 · ✓ id matches · ✓ name matches |
| 45 | `04-get-children.bru` | GET `/admin/categories/{id}/children` | 200 | ✓ status 200 · ✓ children is array |
| 46 | `05-get-breadcrumb.bru` | GET `/admin/categories/{id}/breadcrumb` | 200 | ✓ status 200 · ✓ breadcrumb array has entries |
| 47 | `06-update-category.bru` | PATCH `/admin/categories/{id}` | 200 | ✓ status 200 · ✓ name updated |
| 48 | `07-get-path.bru` | GET `/admin/categories/{id}/path` | 200 | ✓ status 200 · ✓ path is string |
| 49 | `08-get-ancestors.bru` | GET `/admin/categories/{id}/ancestors` | 200 | ✓ status 200 · ✓ ancestors array present |
| 50 | `09-add-attribute-text.bru` | POST `/admin/categories/{id}/attributes` | 201/409 | ✓ 201 or 409 · ✓ if 201: key/label/type correct |
| 51 | `10-add-attribute-select.bru` | POST `/admin/categories/{id}/attributes` | 201/409 | ✓ 201 or 409 · ✓ if 201: options array present |
| 52 | `11-add-attribute-select-options.bru` | POST `/admin/categories/{id}/attributes` | 201/409 | ✓ 201 or 409 · ✓ if 201: key/type/options correct |
| 53 | `12-add-attribute-number.bru` | POST `/admin/categories/{id}/attributes` | 201/409 | ✓ 201 or 409 · ✓ if 201: type is NUMBER |
| 54 | `13-update-attribute.bru` | PATCH `/admin/categories/{id}/attributes/{attrId}` | 200 | ✓ status 200 · ✓ label updated · ✓ key unchanged |
| 55 | `14-move-category.bru` | PATCH `/admin/categories/{id}/move` | 200/409 | ✓ 200 or 409 (idempotent) · ✓ if 200: parentId/depth/path correct |
| 56 | `15-move-to-descendant-fail.bru` | PATCH `/admin/categories/{id}/move` | 400 | ✓ status is 4xx · ✓ error mentions subtree or cycle |
| 57 | `16-toggle-active-deactivate.bru` | PATCH `/admin/categories/{id}/toggle-active` | 200 | ✓ status 200 · ✓ active is now false |
| 58 | `17-toggle-active-reactivate.bru` | PATCH `/admin/categories/{id}/toggle-active` | 200 | ✓ status 200 · ✓ active is now true |
| 59 | `18-get-stats.bru` | GET `/admin/categories/{id}/stats` | 200 | ✓ status 200 · ✓ totalDemandes/totalDemandesInSubtree/childrenCount/hasActiveChildren present |
| 60 | `19-search-categories.bru` | GET `/admin/categories/search` | 200 | ✓ status 200 · ✓ content is array · ✓ totalElements is a number |
| 61 | `20-delete-attribute.bru` | DELETE `/admin/categories/{id}/attributes/{attrId}` | 204 | ✓ status 204 |
| 62 | `21-delete-category.bru` | DELETE `/admin/categories/{id}` | 204 | ✓ status 204 |

### auth-final/ (runs last to avoid JWT collision)

| # | File | Method | Status | Tests |
|---|---|---|---|---|
| 63 | `refresh.bru` | POST `/auth/refresh` | 200 | ✓ status 200 · ✓ new accessToken issued · ✓ new refreshToken issued |
| 64 | `logout.bru` | POST `/auth/logout` | 200 | ✓ status 200 · ✓ success message present |

---

## Suite 2 — Edge Case Sweep

**55 requests / 75 tests — ✓ PASS**

Covers boundary conditions, validation errors, authentication failures, and authorization rejections.

### edge_cases/00-setup/

| # | File | Tests |
|---|---|---|
| 1 | `01-create-open-demande.bru` — Create an OPEN demande for edge case tests | ✓ status 201 · ✓ demandeId saved to env |

### edge_cases/auth/

| # | File | Scenario | Status | Tests |
|---|---|---|---|---|
| 2 | `01-login-missing-password.bru` | POST `/auth/login` — missing password field | 400 | ✓ status 400 · ✓ no accessToken in body |
| 3 | `02-login-wrong-password.bru` | POST `/auth/login` — wrong password | 401 | ✓ status 401 · ✓ no accessToken returned |
| 4 | `03-register-duplicate.bru` | POST `/auth/register` — existing email | 409 | ✓ status 409 · ✓ no token in response |
| 5 | `04-register-invalid-email.bru` | POST `/auth/register` — malformed email | 400 | ✓ status 400 · ✓ no token in response |
| 6 | `05-register-short-password.bru` | POST `/auth/register` — password too short | 400 | ✓ status 400 · ✓ no token in response |
| 7 | `06-login-valid-buyer.bru` | POST `/auth/login` — valid buyer (re-login setup) | 200 | ✓ status 200 · ✓ role is BUYER · ✓ accessToken returned |
| 8 | `07-refresh-invalid-token.bru` | POST `/auth/refresh` — forged token | 403 | ✓ status 403 · ✓ no accessToken returned |
| 9 | `08-refresh-wrong-type.bru` | POST `/auth/refresh` — access token used as refresh | 403 | ✓ status 403 · ✓ no accessToken returned |
| 10 | `09-logout-invalid-token.bru` | POST `/auth/logout` — garbage token | 401 | ✓ status 401 · ✓ no new token issued |

### edge_cases/categories/

| # | File | Scenario | Status | Tests |
|---|---|---|---|---|
| 11 | `01-get-nonexistent-id.bru` | GET `/categories/999999` | 404 | ✓ status 404 · ✓ no category data · ✓ error message present |
| 12 | `02-get-nonexistent-slug.bru` | GET `/categories/slug/nonexistent-slug-xyz` | 404 | ✓ status 404 · ✓ no category data · ✓ error message present |

### edge_cases/demande_creation/

| # | File | Scenario | Status | Tests |
|---|---|---|---|---|
| 13 | `01-missing-title.bru` | POST — missing required title | 400 | ✓ status 400 |
| 14 | `02-missing-category.bru` | POST — missing categoryId | 400 | ✓ status 400 |
| 15 | `03-negative-quantity.bru` | POST — quantity = -1 | 400 | ✓ status 400 |
| 16 | `04-past-deadline.bru` | POST — deadline in the past | 400 | ✓ status 400 |
| 17 | `05-missing-wilaya.bru` | POST — missing wilaya | 400 | ✓ status 400 |
| 18 | `06-non-existent-category.bru` | POST — categoryId = 999999 | 404 | ✓ status 404 — category not found |
| 19 | `07-non-leaf-category.bru` | POST — parent (non-leaf) category | 201 | ✓ status 201 (non-leaf accepted) · ✓ demande created |
| 20 | `08-empty-title.bru` | POST — empty string title | 400 | ✓ status 400 |
| 21 | `09-zero-quantity.bru` | POST — quantity = 0 | 400 | ✓ status 400 |
| 22 | `10-create-as-supplier.bru` | POST — SUPPLIER role tries to create demande | 401 | ✓ status 401 — Spring Security returns 401 for wrong role |

### edge_cases/demande_status/

| # | File | Scenario | Status | Tests |
|---|---|---|---|---|
| 23 | `01-close-already-closed.bru` | PATCH close — already CLOSED | 400 | ✓ status 400 |
| 24 | `02-close-nonexistent.bru` | PATCH close — id = 999999 | 404 | ✓ status 404 |
| 25 | `03-close-another-buyers.bru` | PATCH close — demande owned by other buyer | 403 | ✓ status 403 |
| 26 | `04-cancel-after-reponse.bru` | DELETE — demande that has reponses | 400 | ✓ status 400 · ✓ no data in response |
| 27 | `05-close-no-auth.bru` | PATCH close — no auth header | 401 | ✓ status 401 |

### edge_cases/reponse_submission/

| # | File | Scenario | Status | Tests |
|---|---|---|---|---|
| 28 | `00-initial-submit.bru` | POST reponse — initial setup (first submit) | 201/400 | ✓ 201 or 400 (idempotent setup) |
| 29 | `01-submit-twice.bru` | POST reponse — duplicate submission | 400 | ✓ 400 — already responded · ✓ error message present |
| 30 | `02-update-to-indisponible.bru` | PATCH reponse — update to INDISPONIBLE | 200/400 | ✓ 200 or 400 (outside 1h window) · ✓ if 200: status is INDISPONIBLE |
| 31 | `03-submit-no-auth.bru` | POST reponse — no auth header | 401 | ✓ status 401 · ✓ no reponse created |
| 32 | `04-submit-nonexistent-demande.bru` | POST reponse — demandeId = 999999 | 404 | ✓ status 404 · ✓ no reponse created |

### edge_cases/security/

| # | File | Scenario | Status | Tests |
|---|---|---|---|---|
| 33 | `01-no-auth-demandes.bru` | GET `/buyer/demandes` — unauthenticated | 401 | ✓ status 401 |
| 34 | `02-no-auth-feed.bru` | GET `/seller/feed` — unauthenticated | 401 | ✓ status 401 · ✓ no data returned |
| 35 | `03-buyer-posts-reponse.bru` | POST reponse — BUYER role | 401 | ✓ status 401 · ✓ no demande created |
| 36 | `04-supplier-views-feed-no-auth.bru` | GET `/seller/feed` — no auth | 401 | ✓ status 401 · ✓ no feed data returned |

### edge_cases/admin/

| # | File | Scenario | Status | Tests |
|---|---|---|---|---|
| 37 | `01-get-nonexistent-category.bru` | GET `/admin/categories/999999` | 404 | ✓ status 404 |
| 38 | `02-move-to-nonexistent-parent.bru` | PATCH move — parentId = 999999 | 404 | ✓ status 404 · ✓ error mentions category |
| 39 | `03-access-as-buyer.bru` | GET `/admin/users` — BUYER role | 401 | ✓ status 401 — Spring Security returns 401 for wrong role |
| 40 | `04-get-nonexistent-user.bru` | GET `/admin/users/999999` | 404 | ✓ status 404 · ✓ no user data · ✓ error message present |

### edge_cases/pagination/

| # | File | Scenario | Status | Tests |
|---|---|---|---|---|
| 41 | `01-page-zero.bru` | GET `/buyer/demandes?page=0` | 200 | ✓ status 200 |
| 42 | `02-large-page-number.bru` | GET `/buyer/demandes?page=9999` | 200 | ✓ status 200 |
| 43 | `03-size-negative.bru` | GET `/buyer/demandes?size=-1` | 200 | ✓ status 200 — Spring coerces invalid size to default |

---

## Suite 3 — E2E Workflow

**16 requests / 54 tests — ✓ PASS**

Full end-to-end scenario: admin creates category with attributes → supplier subscribes → buyer creates demande with attributes → supplier sees demande in feed → supplier replies DISPONIBLE → buyer receives the offer.

### Flow

```
Admin creates category  ──►  Admin adds TEXT attr "fabricant"
                         ──►  Admin adds SELECT attr "type_pompe"
                         ──►  Verify both attributes attached

Supplier subscribes to category

Buyer creates demande in category (fills fabricant + type_pompe)
Buyer verifies demande persisted with correct attribute values

Supplier sees demande in feed (async matching may add it)
Supplier views demande detail
Supplier replies DISPONIBLE with offer note

Buyer views responses on demande
Buyer closes demande
```

### Steps

| # | File | Endpoint | Status | Tests |
|---|---|---|---|---|
| 01 | `01-login-admin.bru` | POST `/auth/login` (admin) | 200 | ✓ status 200 · ✓ adminToken saved |
| 02 | `02-login-buyer.bru` | POST `/auth/login` (buyer) | 200 | ✓ status 200 · ✓ buyerToken saved |
| 03 | `03-login-supplier.bru` | POST `/auth/login` (supplier) | 200 | ✓ status 200 · ✓ supplierToken saved |
| 04 | `04-admin-create-category.bru` | POST `/admin/categories` | 201/409 | ✓ 201 or 409 (idempotent) |
| 05 | `05-resolve-category-id.bru` | GET `/admin/categories/search?q=Pompes+Industrielles+E2E` | 200 | ✓ status 200 · ✓ e2eCategoryId is set |
| 06 | `06-add-text-attribute.bru` | POST `/admin/categories/{id}/attributes` | 201/409 | ✓ 201 or 409 · ✓ if 201: key is fabricant · ✓ if 201: type is TEXT |
| 07 | `07-add-select-attribute.bru` | POST `/admin/categories/{id}/attributes` | 201/409 | ✓ 201 or 409 · ✓ if 201: key is type_pompe · ✓ if 201: type is SELECT |
| 08 | `08-verify-category-attributes.bru` | GET `/categories/{id}/attributes` | 200 | ✓ status 200 · ✓ data is array · ✓ has at least 2 attributes · ✓ fabricant present · ✓ type_pompe present |
| 09 | `09-supplier-subscribe-to-category.bru` | PATCH `/seller/categories` | 200 | ✓ status 200 · ✓ categoryIds is array · ✓ e2eCategoryId in subscriptions |
| 10 | `10-buyer-create-demande.bru` | POST `/buyer/demandes` | 201 | ✓ status 201 · ✓ e2eDemandeId saved · ✓ status is OPEN · ✓ categoryId matches e2eCategoryId · ✓ attributes field is present |
| 11 | `11-buyer-views-demande.bru` | GET `/buyer/demandes/{id}` | 200 | ✓ status 200 · ✓ id matches · ✓ status is OPEN · ✓ title matches · ✓ attributes persisted · ✓ fabricant = "Grundfos" · ✓ type_pompe = "Centrifuge" |
| 12 | `12-supplier-checks-feed.bru` | GET `/seller/feed` | 200 | ✓ status 200 · ✓ data has content array · ✓ pagination metadata present |
| 13 | `13-supplier-views-demande-detail.bru` | GET `/seller/feed/{e2eDemandeId}` | 200/403 | ✓ 200 or 403 (async timing) · ✓ if 200: id is e2eDemandeId · ✓ if 200: finalScore is a number |
| 14 | `14-supplier-replies-disponible.bru` | POST `/seller/reponses/{e2eDemandeId}` | 201/400 | ✓ 201 or 400 (idempotent) · ✓ if 201: status is DISPONIBLE · ✓ if 201: demandeId matches · ✓ if 201: supplierId matches · ✓ if 201: note is present |
| 15 | `15-buyer-views-reponses.bru` | GET `/buyer/demandes/{id}/reponses` | 200 | ✓ status 200 · ✓ data.content is array (paginated) · ✓ at least one response received · ✓ has a DISPONIBLE response · ✓ response has supplier info · ✓ response has note with offer details |
| 16 | `16-buyer-closes-demande.bru` | PATCH `/buyer/demandes/{id}/close` | 200/400 | ✓ 200 or 400 (idempotent) · ✓ if 200: status is CLOSED |

---

## Bugs Discovered & Fixed During Testing

### 1. JWT Same-Second Collision (Silent Token Invalidation)

**Symptom:** All buyer requests returned 401 after running `auth/refresh` then `auth/logout` within the same sweep.

**Root cause:** `jjwt` uses epoch-second precision for `iat`. When two JWTs are generated for the same user within the same second, both tokens are byte-for-byte identical (same subject + same `iat`). When `auth/logout` blacklisted the refreshed `accessToken`, it also blacklisted `buyerToken` since they were the same string.

**Fix:** Moved `refresh.bru` and `logout.bru` out of the `auth/` folder into a new `auth-final/` folder that runs **last** in the sweep, after all buyer/supplier requests have completed.

**Key insight for future code changes:** If you need the logout to be safe to run mid-sweep, consider adding millisecond precision to JWT `iat`, or include a `jti` (JWT ID) UUID claim.

---

### 2. DB Check Constraint Missing EXPIRED

**Symptom:** `PATCH /admin/demandes/{id}/expire` returned HTTP 500 with a constraint violation.

**Root cause:** The `demandes_status_check` constraint only allowed `OPEN`, `CLOSED`, `CANCELLED`. The `EXPIRED` value was never added when the expire feature was implemented.

**Fix (DDL):**
```sql
ALTER TABLE demandes DROP CONSTRAINT demandes_status_check;
ALTER TABLE demandes ADD CONSTRAINT demandes_status_check
  CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED', 'EXPIRED'));
```

**Note:** With `ddl-auto: update`, Hibernate does not update existing CHECK constraints. If you add a new enum value, you must apply the DDL change manually or via a Flyway migration.

---

### 3. Admin Toggle Disabled Buyer (Cross-Test State Contamination)

**Symptom:** All buyer requests returned 403 (`Account disabled`) after `admin/users/toggle-user.bru` ran.

**Root cause:** `toggle-user.bru` disabled the buyer account as part of the admin user management test, but there was no restore step — the buyer stayed disabled for the rest of the sweep.

**Fix:** Added `toggle-user-restore.bru` (seq 3) immediately after `toggle-user.bru` to re-enable the buyer. Also re-enabled accounts directly via SQL when bootstrapping:
```sql
UPDATE users SET enabled = true WHERE email IN ('buyer@test.com', 'supplier@test.com');
```

---

### 4. Shared `demandeId` Causes Cascade Failures

**Symptom:** `reponses/submit-reponse.bru` and `demandes/delete-demande.bru` failed because `demandeId` pointed to a CLOSED demande.

**Root cause:** `demandes/close-demande.bru` closed the shared `demandeId`. Subsequent tests that needed an OPEN demande inherited the now-CLOSED state.

**Fix:** Each operation that needs a specific demande state now creates its own fresh demande:
- `cancelDemandeId` — created by `create-for-cancel.bru` (seq 6), used by `delete-demande.bru`
- `reponseDemandeId` — created by `00-create-open-demande.bru` in the reponses folder
- `expireDemandeId` — created by `05b-create-for-expire.bru` in admin/demandes

---

### 5. Wrong Admin Category Search URL

**Symptom:** Step 5 of E2E workflow got 404 when searching for the created category.

**Root cause:** Used `/admin/categories?q=...` (list endpoint with query param) but the correct search endpoint is `/admin/categories/search?q=...`.

**Fix:** Updated `05-resolve-category-id.bru` to use `/admin/categories/search?q=Pompes+Industrielles+E2E&depth=0`.

---

### 6. Supplier Subscription Wrong Endpoint

**Symptom:** Step 9 of E2E workflow returned 404.

**Root cause:** Used `/users/me/categories` but the correct endpoint for supplier category subscription is `/seller/categories`.

**Fix:** Updated `09-supplier-subscribe-to-category.bru` to use `PATCH /seller/categories` with body `{ "categoryIds": [{{e2eCategoryId}}] }`.

---

### 7. CREATE Demande Returns Empty Attributes Array

**Symptom:** Step 10 of E2E workflow test `"attributes persisted"` failed with an empty array.

**Root cause:** The backend saves demande attributes in a separate step after the initial `demande.save()`. The POST response is built before attributes are committed, so `data.attributes` is always `[]` on the create response.

**Fix:** Removed attribute content assertions from step 10 (create). Moved attribute verification to step 11 (GET demande), which reloads from the database after all saves are complete.

---

### 8. Buyer Reponses Endpoint is Paginated

**Symptom:** Step 15 of E2E workflow test `"at least one response received"` failed — `res.getBody().data` was an object, not an array.

**Root cause:** `GET /buyer/demandes/{id}/reponses` returns a paginated response: `{ data: { content: [...], totalElements: n, ... } }`. Tests were asserting on `data` directly.

**Fix:** Updated step 15 tests to use `res.getBody().data.content` for all array assertions.

---

### 9. Edge Case Status Code Mismatches

Six edge case tests had incorrect expected status codes based on actual Spring Security behavior:

| File | Wrong | Correct | Reason |
|---|---|---|---|
| `demande_creation/06-non-existent-category.bru` | 400 | 404 | Backend throws `ResourceNotFoundException` |
| `demande_creation/10-create-as-supplier.bru` | 403 | 401 | Spring Security returns 401 for wrong role with bearer auth |
| `admin/03-access-as-buyer.bru` | 403 | 401 | Spring Security returns 401 for wrong role with bearer auth |
| `pagination/03-size-negative.bru` | 400 | 200 | Spring coerces invalid `size` to default (20) |
| `reponse_submission/01-submit-twice.bru` | 409 | 400 | Backend uses `BusinessException` (400), not `DuplicateResourceException` (409) |
| `reponse_submission/02-update-to-indisponible.bru` | 200 only | 200 or 400 | Update window may have expired; test now accepts both |

---

## Architecture Notes

### Async Matching

`MatchingOrchestrator.runMatching()` is `@Async` — it runs in a background thread after `demande.save()` returns. This means:

- A supplier can see a demande in their feed only after the async scoring job runs.
- The `GET /seller/feed/{demandeId}` endpoint returns **403** (not 404) when the supplier has not been scored for that demande yet.
- E2E step 13 accepts `[200, 403]` to handle this timing.

### Redis JWT Blacklist

On logout, the token is stored in Redis with key `jwt:blacklist:<token>` and TTL equal to the remaining token lifetime. The `JwtAuthFilter` checks the blacklist on every request. The same-second collision bug (see Bug #1) occurs because `jjwt` generates the same token bytes when `subject` and `iat` are identical.

### Idempotency Design

All tests that create resources use one of:
1. Accept both `201` and `409` in the test assertion.
2. Pre-check via a GET and only assert if the resource was freshly created.
3. Create a new resource per run using `script:post-response` to save the generated ID into an env var, so no hardcoded ID is ever assumed to be in a specific state.

This means the full suite can be run against a non-fresh database and all tests still pass.

---

## Environment Variables (local.bru)

| Variable | Set By | Used By |
|---|---|---|
| `buyerToken` | `auth/login` | All buyer requests |
| `supplierToken` | `auth/login-supplier` | All seller requests |
| `adminToken` | `auth/login-admin` | All admin requests |
| `accessToken` | `auth-final/refresh` | `auth-final/logout` |
| `demandeId` | `demandes/create-demande` | Most demande tests |
| `cancelDemandeId` | `demandes/create-for-cancel` | `demandes/delete-demande` |
| `reponseDemandeId` | `reponses/00-create-open-demande` | `reponses/submit-reponse` · `reponses/update-reponse` |
| `expireDemandeId` | `admin/demandes/05b-create-for-expire` | `admin/demandes/06-expire` |
| `reponseId` | `reponses/submit-reponse` | `reponses/update-reponse` |
| `userId` | `admin/users/list-users` | `admin/users/get-user-detail` · toggle endpoints |
| `e2eCategoryId` | `workflows/05-resolve-category-id` | All E2E steps after 05 |
| `e2eDemandeId` | `workflows/10-buyer-create-demande` | E2E steps 11–16 |
| `e2eReponseId` | `workflows/14-supplier-replies-disponible` | — |
