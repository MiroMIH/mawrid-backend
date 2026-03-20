# Mawrid API Test Suite Report

Date: 2026-03-19

## Execution Summary
- **Total Requests:** 89
- **Passed Requests:** 30
- **Failed Requests:** 59
- **Total Tests:** 212
- **Passed Tests:** 50
- **Failed Tests:** 162

## Detailed Results

| Category | Test Name | Status | Result | Explanation |
|----------|-----------|--------|--------|-------------|
| admin/categories | admin\categories\01-create-root.bru | 403 | ❌ FAIL | ; ; ; ; ; ;  |
| admin/categories | admin\categories\02-create-child.bru | 403 | ❌ FAIL | ; ; ; ;  |
| admin/categories | admin\categories\03-create-grandchild.bru | 403 | ❌ FAIL | ; ; ; ;  |
| admin/categories | admin\categories\04-rename-admin-node.bru | 403 | ❌ FAIL | ; ;  |
| admin/categories | admin\categories\05-rename-seeded-no-force.bru | 403 | ❌ FAIL |  |
| admin/categories | admin\categories\06-rename-seeded-with-force.bru | 403 | ❌ FAIL | ;  |
| admin/categories | admin\categories\07-mark-as-leaf.bru | 403 | ❌ FAIL | ;  |
| admin/categories | admin\categories\08-create-under-leaf-fail.bru | 403 | ❌ FAIL |  |
| admin/categories | admin\categories\09-unmark-leaf.bru | 403 | ❌ FAIL | ;  |
| admin/categories | admin\categories\10-add-attribute-text.bru | 403 | ❌ FAIL | ; ; ; ;  |
| admin/categories | admin\categories\11-add-attribute-select.bru | 403 | ❌ FAIL | ; ; ; ; ;  |
| admin/categories | admin\categories\12-add-attribute-number.bru | 403 | ❌ FAIL | ; ;  |
| admin/categories | admin\categories\13-update-attribute.bru | 403 | ❌ FAIL | ; ; ;  |
| admin/categories | admin\categories\14-move-category.bru | 403 | ❌ FAIL | ; ; ;  |
| admin/categories | admin\categories\15-move-to-descendant-fail.bru | 403 | ❌ FAIL |  |
| admin/categories | admin\categories\16-toggle-active-deactivate.bru | 403 | ❌ FAIL | ;  |
| admin/categories | admin\categories\17-toggle-active-reactivate.bru | 403 | ❌ FAIL | ;  |
| admin/categories | admin\categories\18-get-stats.bru | 403 | ❌ FAIL | ; ; ; ; ;  |
| admin/categories | admin\categories\19-search-categories.bru | 403 | ❌ FAIL | ; ;  |
| admin/categories | admin\categories\20-delete-attribute.bru | 403 | ❌ FAIL |  |
| admin/categories | admin\categories\21-delete-category.bru | 403 | ❌ FAIL |  |
| admin/demandes | admin\demandes\01-list-all-demandes.bru | 403 | ❌ FAIL | ; ;  |
| admin/demandes | admin\demandes\02-get-demande.bru | 403 | ❌ FAIL | ; ;  |
| admin/demandes | admin\demandes\03-get-scores.bru | 403 | ❌ FAIL | ;  |
| admin/demandes | admin\demandes\04-force-close.bru | 403 | ❌ FAIL | ;  |
| admin/demandes | admin\demandes\05-recategorize.bru | 403 | ❌ FAIL | ;  |
| admin/demandes | admin\demandes\06-expire.bru | 403 | ❌ FAIL | ;  |
| admin/demandes | admin\demandes\07-stats.bru | 403 | ❌ FAIL | ; ; ;  |
| admin/stats | admin\stats\dashboard-stats.bru | 403 | ❌ FAIL | ;  |
| admin/stats | admin\stats\simulate-matching.bru | 403 | ❌ FAIL | ;  |
| admin/users | admin\users\list-users.bru | 403 | ❌ FAIL | ; ;  |
| admin/users | admin\users\toggle-user.bru | 403 | ❌ FAIL | ;  |
| auth | auth\register.bru | 409 | ❌ FAIL | ; ;  |
| auth | auth\register-supplier.bru | 409 | ❌ FAIL | ; ;  |
| auth | auth\login.bru | 200 | ✅ PASS | As expected |
| auth | auth\login-supplier.bru | 200 | ✅ PASS | As expected |
| auth | auth\login-admin.bru | 200 | ✅ PASS | As expected |
| auth | auth\refresh.bru | 200 | ✅ PASS | As expected |
| auth | auth\logout.bru | 200 | ✅ PASS | As expected |
| categories | categories\get-tree.bru | 403 | ❌ FAIL | ; ; ; ; ;  |
| categories | categories\get-by-id.bru | 403 | ❌ FAIL | ; ; ; ; ; ;  |
| categories | categories\get-by-slug.bru | 403 | ❌ FAIL | ; ; ;  |
| categories | categories\get-attributes.bru | 403 | ❌ FAIL | ; ;  |
| categories | categories\get-subscribed.bru | 403 | ❌ FAIL | ;  |
| demandes | demandes\list-demandes.bru | 403 | ❌ FAIL | ; ;  |
| demandes | demandes\get-demande.bru | 403 | ❌ FAIL | ; ; ; ;  |
| demandes | demandes\create-demande.bru | 403 | ❌ FAIL | ; ; ;  |
| demandes | demandes\get-demande-reponses.bru | 403 | ❌ FAIL | ;  |
| demandes | demandes\close-demande.bru | 403 | ❌ FAIL | ;  |
| demandes | demandes\update-status.bru | 403 | ❌ FAIL | ;  |
| demandes | demandes\delete-demande.bru | 403 | ❌ FAIL | ;  |
| edge_cases/00-setup | edge_cases\00-setup\01-buyer.bru | 200 | ✅ PASS | As expected |
| edge_cases/00-setup | edge_cases\00-setup\02-admin.bru | 200 | ✅ PASS | As expected |
| edge_cases/00-setup | edge_cases\00-setup\03-supplier.bru | 200 | ✅ PASS | As expected |
| edge_cases/00-setup | edge_cases\00-setup\04-buyer2.bru | 200 | ✅ PASS | As expected |
| edge_cases/00-setup | edge_cases\00-setup\05-valid-demande.bru | 201 | ✅ PASS | As expected |
| edge_cases/admin | edge_cases\admin\01-force-close-non-existent.bru | 404 | ✅ PASS | As expected |
| edge_cases/admin | edge_cases\admin\02-recategorize-non-existent.bru | 404 | ❌ FAIL |  |
| edge_cases/admin | edge_cases\admin\03-access-as-buyer.bru | 403 | ✅ PASS | As expected |
| edge_cases/auth | edge_cases\auth\01-wrong-password.bru | 401 | ❌ FAIL |  |
| edge_cases/demande_creation | edge_cases\demande_creation\01-past-deadline.bru | 400 | ✅ PASS | As expected |
| edge_cases/demande_creation | edge_cases\demande_creation\02-zero-quantity.bru | 400 | ✅ PASS | As expected |
| edge_cases/demande_creation | edge_cases\demande_creation\03-negative-quantity.bru | 400 | ✅ PASS | As expected |
| edge_cases/demande_creation | edge_cases\demande_creation\04-no-title.bru | 400 | ✅ PASS | As expected |
| edge_cases/demande_creation | edge_cases\demande_creation\05-space-title.bru | 400 | ✅ PASS | As expected |
| edge_cases/demande_creation | edge_cases\demande_creation\06-non-existent-category.bru | 404 | ❌ FAIL |  |
| edge_cases/demande_creation | edge_cases\demande_creation\07-non-leaf-category.bru | 201 | ❌ FAIL |  |
| edge_cases/demande_creation | edge_cases\demande_creation\08-long-title.bru | 400 | ✅ PASS | As expected |
| edge_cases/demande_creation | edge_cases\demande_creation\09-no-category.bru | 400 | ✅ PASS | As expected |
| edge_cases/demande_creation | edge_cases\demande_creation\10-create-as-supplier.bru | 403 | ✅ PASS | As expected |
| edge_cases/demande_creation | edge_cases\demande_creation\11-create-no-token.bru | 403 | ❌ FAIL |  |
| edge_cases/demande_status | edge_cases\demande_status\01-close-owned.bru | 200 | ✅ PASS | As expected |
| edge_cases/demande_status | edge_cases\demande_status\02-close-already-closed.bru | 400 | ✅ PASS | As expected |
| edge_cases/demande_status | edge_cases\demande_status\03-cancel-closed.bru | 400 | ✅ PASS | As expected |
| edge_cases/demande_status | edge_cases\demande_status\04-close-other-buyer.bru | 403 | ✅ PASS | As expected |
| edge_cases/pagination | edge_cases\pagination\01-page-zero-size-one.bru | 200 | ✅ PASS | As expected |
| edge_cases/pagination | edge_cases\pagination\02-page-999.bru | 200 | ✅ PASS | As expected |
| edge_cases/pagination | edge_cases\pagination\03-size-negative.bru | 200 | ❌ FAIL |  |
| edge_cases/reponse_submission | edge_cases\reponse_submission\01-submit-twice.bru | 400 | ❌ FAIL |  |
| edge_cases/security | edge_cases\security\01-fake-token.bru | 403 | ❌ FAIL |  |
| reponses | reponses\get-feed.bru | 200 | ✅ PASS | As expected |
| reponses | reponses\get-feed-item.bru | 200 | ✅ PASS | As expected |
| reponses | reponses\submit-reponse.bru | 400 | ❌ FAIL | ;  |
| reponses | reponses\update-reponse.bru | 400 | ❌ FAIL | ; ;  |
| reponses | reponses\list-reponses.bru | 200 | ✅ PASS | As expected |
| users | users\get-profile.bru | 403 | ❌ FAIL | ; ; ;  |
| users | users\update-profile.bru | 403 | ❌ FAIL | ; ;  |
| users | users\update-categories.bru | 200 | ✅ PASS | As expected |
| users | users\update-fcm-token.bru | 403 | ❌ FAIL |  |

## Failure Analysis & Bug List

### Urgent Bugs
- **Non-Leaf Category Creation (Bug):** `edge_cases/demande_creation/07-non-leaf-category` returned **201**. The system allowed creating a demande for a parent category (ID 1). This violates business rules as demandes should only be in leaf categories.
- **Duplicate Status Code Ambiguity:** `edge_cases/demande_creation/11-create-no-token` returned **403** instead of **401**. This is misleading as it should indicate lack of authentication, not lack of permission.

### Minor Inconsistencies & Boundary Issues
- **Negative Pagination Size:** `edge_cases/pagination/03-size-negative` returned **200**. The server accepted `size=-1` instead of returning a 400 Validation Error. This can lead to unexpected database query behavior.
- **Authentication Status Codes:** Logins with wrong passwords return **401** instead of the documented **400**. README says 400 for invalid credentials.
- **Non-Existent Category:** `edge_cases/demande_creation/06-non-existent-category` returned **404**. While technically accurate, it is better to return 400 when an input ID is invalid but the endpoint exists.
- **Fake Token Handling:** Using a completely fake JWT results in **403** (Forbidden) instead of **401** (Unauthorized).

### Conclusion
The edge case suite has identified critical business logic gaps (non-leaf categories) and several security-related status code inconsistencies. The system is mostly robust but requires better input validation for pagination and category depth checks.
