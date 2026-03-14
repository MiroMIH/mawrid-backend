# Mawrid Backend — Full Technical Documentation

> **Stack:** Spring Boot 3.4.3 · Java 21 · PostgreSQL 16 · Redis 7 · MinIO · Firebase FCM · Maven

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Project Structure](#3-project-structure)
4. [Domain Model & Entities](#4-domain-model--entities)
5. [REST API Reference](#5-rest-api-reference)
6. [Security & Authentication](#6-security--authentication)
7. [Scoring & Matching Engine](#7-scoring--matching-engine)
8. [Notification System](#8-notification-system)
9. [Scheduled Jobs](#9-scheduled-jobs)
10. [Configuration Reference](#10-configuration-reference)
11. [Infrastructure & Deployment](#11-infrastructure--deployment)
12. [Testing](#12-testing)
13. [Local Development Setup](#13-local-development-setup)
14. [Key Libraries & Versions](#14-key-libraries--versions)

---

## 1. Project Overview

Mawrid is a **B2B procurement marketplace** for Algeria. It connects **Buyers** who post procurement requests (*demandes*) with **Suppliers** who respond to them (*reponses*).

### Core Flow

```
Buyer posts Demande
       │
       ▼
MatchingOrchestrator (async)
  └─ finds eligible suppliers by category path-prefix
  └─ DemandeScoreEngine computes a multi-factor score per supplier
  └─ NotificationService creates Notification records (immediate / delayed)
       │
       ▼
NotificationDispatchScheduler (every 30s)
  └─ sends FCM push or email when scheduledAt arrives
       │
       ▼
Supplier sees scored feed & submits Reponse (DISPONIBLE / INDISPONIBLE)
       │
       ▼
Buyer views ranked supplier list & closes Demande
```

### User Roles

| Role | Description |
|------|-------------|
| `BUYER` | Posts demandes, manages their status, views supplier responses |
| `SUPPLIER` | Subscribes to categories, submits reponses |
| `ADMIN` | Back-office management, category tree admin |
| `SUPERADMIN` | Full admin — seeded automatically on first boot |

---

## 2. Architecture

### Package-by-Feature Layout

```
com.mawrid/
├── admin/          — back-office stats, user management, simulation
├── auth/           — register, login, refresh, logout
├── category/       — self-referencing category tree with attributes
├── common/
│   ├── config/     — Firebase, MinIO, Redis, Async, OpenAPI, DataInitializer
│   ├── enums/      — NodeType, AttributeType, NotifType, NotifChannel
│   ├── exception/  — GlobalExceptionHandler + custom exceptions
│   ├── response/   — ApiResponse<T> standard wrapper
│   └── security/   — SecurityConfig, JwtAuthFilter, UserDetailsServiceImpl
├── demande/        — procurement request lifecycle
├── matching/       — supplier eligibility & async orchestration
├── notification/   — Notification entity, FCM/email dispatch
├── reponse/        — supplier responses
├── scoring/        — DemandeScoreEngine, decay scheduler, ScoringConfig
├── storage/        — MinIO file uploads
└── user/           — profile management, FCM tokens, category subscriptions
```

### Technology Integrations

| Service | Purpose |
|---------|---------|
| **PostgreSQL 16** | Primary relational database |
| **Redis 7** | Category tree cache (`@Cacheable("categories")`), JWT blacklist |
| **MinIO** | Object storage for demande attachments |
| **Firebase FCM** | Mobile push notifications (Flutter app) |
| **JavaMailSender** | Email notifications via SMTP (Gmail) |
| **SpringDoc OpenAPI 3** | Auto-generated Swagger UI at `/swagger-ui.html` |

---

## 3. Project Structure

```
mawrid-backend/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── DOCUMENTATION.md
├── .github/
│   └── workflows/
│       └── deploy.yml
└── src/
    ├── main/
    │   ├── java/com/mawrid/
    │   │   ├── MawridApplication.java
    │   │   ├── admin/
    │   │   │   ├── AdminController.java
    │   │   │   ├── AdminService.java
    │   │   │   └── dto/
    │   │   │       ├── AdminStatsResponse.java
    │   │   │       ├── SimulationRequest.java
    │   │   │       └── SimulationResult.java
    │   │   ├── auth/
    │   │   │   ├── AuthController.java
    │   │   │   ├── AuthService.java
    │   │   │   ├── JwtService.java
    │   │   │   └── dto/
    │   │   │       ├── AuthResponse.java
    │   │   │       ├── LoginRequest.java
    │   │   │       ├── RefreshRequest.java
    │   │   │       └── RegisterRequest.java
    │   │   ├── category/
    │   │   │   ├── Category.java
    │   │   │   ├── CategoryAttribute.java
    │   │   │   ├── CategoryAttributeRepository.java
    │   │   │   ├── CategoryController.java
    │   │   │   ├── CategoryMapper.java
    │   │   │   ├── CategoryRepository.java
    │   │   │   ├── CategoryService.java
    │   │   │   └── dto/
    │   │   │       ├── CategoryAttributeRequest.java
    │   │   │       ├── CategoryAttributeResponse.java
    │   │   │       ├── CategoryRequest.java
    │   │   │       ├── CategoryResponse.java
    │   │   │       └── MoveRequest.java
    │   │   ├── common/
    │   │   │   ├── config/
    │   │   │   │   ├── AsyncConfig.java
    │   │   │   │   ├── DataInitializer.java
    │   │   │   │   ├── FirebaseConfig.java
    │   │   │   │   ├── MinioConfig.java
    │   │   │   │   ├── OpenApiConfig.java
    │   │   │   │   ├── RedisConfig.java
    │   │   │   │   └── SchedulingConfig.java
    │   │   │   ├── enums/
    │   │   │   │   ├── AttributeType.java
    │   │   │   │   ├── NodeType.java
    │   │   │   │   ├── NotifChannel.java
    │   │   │   │   └── NotifType.java
    │   │   │   ├── exception/
    │   │   │   │   ├── BusinessException.java
    │   │   │   │   ├── DuplicateResourceException.java
    │   │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   │   └── ResourceNotFoundException.java
    │   │   │   ├── response/
    │   │   │   │   └── ApiResponse.java
    │   │   │   └── security/
    │   │   │       ├── JwtAuthFilter.java
    │   │   │       ├── SecurityConfig.java
    │   │   │       └── UserDetailsServiceImpl.java
    │   │   ├── demande/
    │   │   │   ├── Demande.java
    │   │   │   ├── DemandeAttribute.java
    │   │   │   ├── DemandeAttributeRepository.java
    │   │   │   ├── DemandeController.java
    │   │   │   ├── DemandeMapper.java
    │   │   │   ├── DemandeRepository.java
    │   │   │   ├── DemandeService.java
    │   │   │   ├── DemandeStatus.java
    │   │   │   └── dto/
    │   │   │       ├── DemandeAttributeDto.java
    │   │   │       ├── DemandeRequest.java
    │   │   │       ├── DemandeResponse.java
    │   │   │       └── DemandeStatusUpdate.java
    │   │   ├── matching/
    │   │   │   ├── MatchingOrchestrator.java
    │   │   │   └── MatchingService.java
    │   │   ├── notification/
    │   │   │   ├── EmailService.java
    │   │   │   ├── FcmService.java
    │   │   │   ├── Notification.java
    │   │   │   ├── NotificationDispatchScheduler.java
    │   │   │   ├── NotificationRepository.java
    │   │   │   └── NotificationService.java
    │   │   ├── reponse/
    │   │   │   ├── Reponse.java
    │   │   │   ├── ReponseController.java
    │   │   │   ├── ReponseMapper.java
    │   │   │   ├── ReponseRepository.java
    │   │   │   ├── ReponseService.java
    │   │   │   ├── ReponseStatus.java
    │   │   │   └── dto/
    │   │   │       ├── ReponseRequest.java
    │   │   │       └── ReponseResponse.java
    │   │   ├── scoring/
    │   │   │   ├── DemandeScoreEngine.java
    │   │   │   ├── DemandeSupplierScore.java
    │   │   │   ├── DemandeSupplierScoreId.java
    │   │   │   ├── DemandeSupplierScoreRepository.java
    │   │   │   ├── ScoreDecayScheduler.java
    │   │   │   ├── ScoringConfig.java
    │   │   │   └── ScoringConfigRepository.java
    │   │   ├── storage/
    │   │   │   └── MinioStorageService.java
    │   │   └── user/
    │   │       ├── Role.java
    │   │       ├── User.java
    │   │       ├── UserController.java
    │   │       ├── UserMapper.java
    │   │       ├── UserRepository.java
    │   │       ├── UserService.java
    │   │       └── dto/
    │   │           ├── FcmTokenRequest.java
    │   │           ├── UpdateCategoriesRequest.java
    │   │           ├── UpdateUserRequest.java
    │   │           └── UserResponse.java
    │   └── resources/
    │       ├── application.yml
    │       └── application-prod.yml
    └── test/
        └── java/com/mawrid/
            ├── AbstractIntegrationTest.java
            ├── MawridApplicationTests.java
            ├── admin/AdminIntegrationTest.java
            ├── auth/AuthIntegrationTest.java
            ├── category/CategoryIntegrationTest.java
            ├── demande/DemandeIntegrationTest.java
            ├── reponse/ReponseIntegrationTest.java
            └── scoring/ScoringIntegrationTest.java
```

---

## 4. Domain Model & Entities

### Entity Relationship Diagram

```
users ──────────────────────────────────────────────────────────────────────┐
  │ id (UUID)                                                               │
  │ email, password, firstName, lastName                                    │
  │ phone, companyName, wilaya, registreCommerce                            │
  │ role: BUYER | SUPPLIER | ADMIN | SUPERADMIN                             │
  │ fcmToken, enabled, createdAt, updatedAt                                 │
  │                                                                         │
  ├──[ManyToMany]── user_categories ──[ManyToMany]── categories            │
  │                                                       │                 │
  │                                                 id (Long)               │
  │                                                 name, slug              │
  │                                                 path (materialized)     │
  │                                                 depth, nodeType         │
  │                                                 parent_id (self-ref)    │
  │                                                 active, demandeCount    │
  │                                                       │                 │
  │                                               category_attributes       │
  │                                               (name, type, required)    │
  │                                                                         │
  ├──[OneToMany]── demandes                                                 │
  │                 │ id (UUID)                                             │
  │                 │ title, description, quantity, unit                    │
  │                 │ deadline, wilaya                                      │
  │                 │ qualityScore, status: OPEN|CLOSED|CANCELLED           │
  │                 │ attachmentUrl, createdAt, updatedAt                   │
  │                 │                                                       │
  │                 ├──[OneToMany]── demande_attributes                    │
  │                 │               (key, value per category attribute)     │
  │                 │                                                       │
  │                 ├──[OneToMany]── reponses ──────────────────────────── │
  │                 │               │ id (UUID)                            │
  │                 │               │ status: DISPONIBLE | INDISPONIBLE    │
  │                 │               │ note, createdAt                       │
  │                 │               │ UNIQUE(demande_id, supplier_id)      │
  │                 │               └──[ManyToOne]── supplier (User) ──────┘
  │                 │
  │                 └──[OneToMany]── demande_supplier_scores
  │                                 │ PK: (demande_id, supplier_id)
  │                                 │ categoryScore, proximityScore
  │                                 │ urgencyScore, buyerScore, quantityScore
  │                                 │ baseScore, decayFactor, finalScore
  │                                 │ scoredAt, lastDecayAt
  │
  └──[OneToMany]── notifications
                   │ id (UUID)
                   │ type: NotifType
                   │ channel: PUSH | EMAIL | IN_APP
                   │ sent, scheduledAt, sentAt, failureReason
                   └──[ManyToOne]── demande
```

### Entity Details

#### `User` — table `users`

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID | Generated, PK |
| `email` | VARCHAR UNIQUE | Username for auth |
| `password` | VARCHAR | BCrypt-hashed |
| `firstName`, `lastName` | VARCHAR | |
| `phone` | VARCHAR | |
| `companyName` | VARCHAR | |
| `wilaya` | VARCHAR | Algerian wilaya code 1–58 |
| `registreCommerce` | VARCHAR | RC number |
| `role` | ENUM | `BUYER`, `SUPPLIER`, `ADMIN`, `SUPERADMIN` |
| `fcmToken` | VARCHAR(500) | Firebase push token |
| `enabled` | BOOLEAN | Default `true`; toggled by admin |
| `createdAt`, `updatedAt` | TIMESTAMP | JPA auditing |

#### `Category` — table `categories`

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT | Auto-increment, PK |
| `name` | VARCHAR(100) | Display name |
| `slug` | VARCHAR(100) UNIQUE | URL-safe identifier |
| `path` | VARCHAR(500) | Materialized path e.g. `"1.11.111"` |
| `depth` | INT | 0 = root/sector |
| `nodeType` | ENUM | `SEEDED` (read-only) or `CUSTOM` |
| `active` | BOOLEAN | |
| `demandeCount` | BIGINT | Running counter |
| `parent_id` | BIGINT FK | Self-referencing |

#### `Demande` — table `demandes`

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID | PK |
| `title` | VARCHAR | |
| `description` | TEXT | |
| `quantity` | INT | |
| `unit` | VARCHAR | e.g. kg, pcs |
| `deadline` | DATE | |
| `wilaya` | VARCHAR | Buyer's location (proximity scoring) |
| `qualityScore` | INT | 0–100, computed at creation |
| `status` | ENUM | `OPEN`, `CLOSED`, `CANCELLED` |
| `attachmentUrl` | VARCHAR(500) | MinIO URL |
| `category_id` | BIGINT FK | |
| `buyer_id` | UUID FK | |

#### `Reponse` — table `reponses`

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID | PK |
| `demande_id` | UUID FK | |
| `supplier_id` | UUID FK | |
| `status` | ENUM | `DISPONIBLE`, `INDISPONIBLE` |
| `note` | TEXT | Optional supplier note |
| `createdAt` | TIMESTAMP | |
| UNIQUE | — | `(demande_id, supplier_id)` — one response per supplier per demande |

#### `Notification` — table `notifications`

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID | PK |
| `user_id` | UUID FK | Target user |
| `demande_id` | UUID FK | Related demande (nullable) |
| `type` | ENUM | `NotifType` |
| `channel` | ENUM | `PUSH`, `EMAIL`, `IN_APP` |
| `sent` | BOOLEAN | Default `false` |
| `scheduledAt` | TIMESTAMP | When to dispatch |
| `sentAt` | TIMESTAMP | Actual dispatch time |
| `failureReason` | VARCHAR(500) | Set on dispatch failure |

#### `DemandeSupplierScore` — table `demande_supplier_scores`

| Column | Type | Notes |
|--------|------|-------|
| `demande_id` + `supplier_id` | COMPOSITE PK | `@EmbeddedId` |
| `categoryScore` | INT | 0–35 |
| `proximityScore` | INT | 0–25 |
| `urgencyScore` | INT | 0–20 |
| `buyerScore` | INT | 0–10 |
| `quantityScore` | INT | 0–10 |
| `baseScore` | INT | Sum of above |
| `decayFactor` | DECIMAL(4,2) | 1.00 → 0.20 over time |
| `finalScore` | INT | `baseScore × decayFactor` |
| `scoredAt`, `lastDecayAt` | TIMESTAMP | |

#### `ScoringConfig` — table `scoring_config`

| Column | Type | Default | Notes |
|--------|------|---------|-------|
| `sector_id` | BIGINT FK | — | Top-level category (depth=0) |
| `categoryWeight` | INT | 35 | Max category score |
| `proximityWeight` | INT | 25 | Max proximity score |
| `urgencyWeight` | INT | 20 | Max urgency score |
| `buyerWeight` | INT | 10 | Max buyer score |
| `quantityWeight` | INT | 10 | Max quantity score |
| `notifThresholdImmediate` | INT | 80 | Score → notify immediately |
| `notifThresholdDelayed15m` | INT | 50 | Score → notify after 15 min |
| `notifThresholdDelayed1h` | INT | 30 | Score → notify after 1 hour |

### Enums

```java
// user/Role.java
enum Role { BUYER, SUPPLIER, ADMIN, SUPERADMIN }

// demande/DemandeStatus.java
enum DemandeStatus { OPEN, CLOSED, CANCELLED }

// reponse/ReponseStatus.java
enum ReponseStatus { DISPONIBLE, INDISPONIBLE }

// common/enums/NodeType.java
enum NodeType { SEEDED, CUSTOM }

// common/enums/AttributeType.java
enum AttributeType { TEXT, NUMBER, BOOLEAN, DATE, SELECT }

// common/enums/NotifType.java
enum NotifType { NEW_DEMANDE, DEMANDE_CLOSED, REPONSE_RECEIVED, ... }

// common/enums/NotifChannel.java
enum NotifChannel { PUSH, EMAIL, IN_APP }
```

---

## 5. REST API Reference

All responses are wrapped in `ApiResponse<T>`:
```json
{
  "success": true,
  "message": "optional message",
  "data": { ... }
}
```

Base URL: `http://localhost:8080/api/v1`

### 5.1 Auth — `/api/v1/auth` (public)

| Method | Path | Body | Response | Description |
|--------|------|------|----------|-------------|
| `POST` | `/register` | `RegisterRequest` | `AuthResponse` | Register a new BUYER or SUPPLIER account |
| `POST` | `/login` | `LoginRequest` | `AuthResponse` | Login and receive JWT tokens |
| `POST` | `/refresh` | `RefreshRequest` | `AuthResponse` | Refresh access token using refresh token |
| `POST` | `/logout` | — (header: `Authorization`) | `Void` | Blacklist current access token |

**`RegisterRequest`**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "firstName": "Ahmed",
  "lastName": "Benali",
  "role": "BUYER",
  "phone": "+213555000000",
  "companyName": "SARL Example",
  "wilaya": "16",
  "registreCommerce": "16/00-000000B00"
}
```

**`AuthResponse`**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer"
}
```

---

### 5.2 Users — `/api/v1/users` (authenticated)

| Method | Path | Auth | Body | Description |
|--------|------|------|------|-------------|
| `GET` | `/me` | Any | — | Get current user profile |
| `PATCH` | `/me` | Any | `UpdateUserRequest` | Update current user profile |
| `PATCH` | `/me/categories` | SUPPLIER | `UpdateCategoriesRequest` | Update subscribed category IDs |
| `PATCH` | `/me/fcm-token` | Any | `FcmTokenRequest` | Register Firebase FCM device token |

---

### 5.3 Categories — `/api/v1/categories`

| Method | Path | Auth | Body | Description |
|--------|------|------|------|-------------|
| `GET` | `/` | Public | — | Get full category tree (Redis-cached) |
| `GET` | `/{id}/attributes` | Public | — | Get effective attributes (including inherited) |
| `POST` | `/` | ADMIN | `CategoryRequest` | Create a new category |
| `PATCH` | `/{id}` | ADMIN | `CategoryRequest` | Rename a category (non-SEEDED only) |
| `POST` | `/{id}/move` | ADMIN | `MoveRequest` | Move category subtree to new parent |
| `POST` | `/{id}/attributes` | ADMIN | `CategoryAttributeRequest` | Add attribute schema to category |

**`CategoryResponse`**
```json
{
  "id": 1,
  "name": "Métaux et Acier",
  "slug": "metaux-acier",
  "path": "1",
  "depth": 0,
  "nodeType": "SEEDED",
  "active": true,
  "demandeCount": 42,
  "children": [ { ... } ],
  "attributes": [ { "name": "Nuance", "type": "TEXT", "required": true } ]
}
```

---

### 5.4 Demandes — `/api/v1/demandes` (authenticated)

| Method | Path | Auth | Body | Description |
|--------|------|------|------|-------------|
| `GET` | `/` | Any | — | List demandes (buyers see own; suppliers see OPEN in their categories) |
| `GET` | `/{id}` | Any | — | Get a single demande by UUID |
| `POST` | `/` | BUYER | `DemandeRequest` | Create a demande → triggers async matching |
| `PATCH` | `/{id}/status` | BUYER | `DemandeStatusUpdate` | Close or cancel a demande |
| `DELETE` | `/{id}` | BUYER or ADMIN | — | Delete a demande |

All list endpoints support Spring Data pagination: `?page=0&size=20&sort=createdAt,desc`

**`DemandeRequest`**
```json
{
  "title": "Achat tôle galvanisée",
  "description": "Besoin de 500 feuilles...",
  "quantity": 500,
  "unit": "feuilles",
  "deadline": "2026-04-01",
  "categoryId": 12,
  "attributes": [
    { "attributeId": 3, "value": "S235" }
  ]
}
```

---

### 5.5 Reponses — `/api/v1/demandes/{demandeId}/reponses` (authenticated)

| Method | Path | Auth | Body | Description |
|--------|------|------|------|-------------|
| `POST` | `/` | SUPPLIER | `ReponseRequest` | Respond to a demande |
| `GET` | `/` | BUYER or ADMIN | — | Get DISPONIBLE supplier responses (paginated) |

**`ReponseRequest`**
```json
{
  "status": "DISPONIBLE",
  "note": "Disponible sous 5 jours ouvrés"
}
```

---

### 5.6 Admin — `/api/v1/admin` (ADMIN or SUPERADMIN)

| Method | Path | Body | Description |
|--------|------|------|-------------|
| `GET` | `/users` | — | Paginated user list |
| `PATCH` | `/users/{id}/toggle-enabled` | — | Enable / disable a user account |
| `GET` | `/stats` | — | Dashboard statistics |
| `POST` | `/matching/simulate` | `SimulationRequest` | Dry-run matching (read-only, no DB writes) |

**`AdminStatsResponse`** (example fields)
```json
{
  "totalUsers": 320,
  "totalBuyers": 120,
  "totalSuppliers": 198,
  "totalDemandes": 540,
  "openDemandes": 87,
  "totalReponses": 1420
}
```

---

## 6. Security & Authentication

### JWT Token Flow

```
Client                          Server
  │                               │
  │── POST /auth/login ──────────►│ verify credentials
  │                               │ issue accessToken (15min TTL)
  │◄── { accessToken, refreshToken }  issue refreshToken (7 days TTL)
  │                               │
  │── GET /api/v1/... ───────────►│ JwtAuthFilter intercepts
  │   Authorization: Bearer <at>  │ parse + verify token signature
  │                               │ check Redis blacklist
  │                               │ load UserDetails → SecurityContext
  │◄── 200 OK ────────────────────│
  │                               │
  │── POST /auth/logout ─────────►│ extract token TTL
  │   Authorization: Bearer <at>  │ store in Redis: jwt:blacklist:<token>
  │◄── 200 OK ────────────────────│   with TTL = remaining token lifetime
```

### JWT Implementation (jjwt 0.12.x)

```java
// Token creation
Jwts.builder()
    .subject(user.getEmail())
    .claim("role", user.getRole())
    .issuedAt(now)
    .expiration(expiry)
    .signWith(secretKey)
    .compact();

// Token validation
Jwts.parser()
    .verifyWith(secretKey)
    .build()
    .parseSignedClaims(token)
    .getPayload();
```

### Access Control Matrix

| Endpoint Pattern | Public | BUYER | SUPPLIER | ADMIN/SUPERADMIN |
|-----------------|--------|-------|----------|-----------------|
| `POST /auth/**` | ✓ | ✓ | ✓ | ✓ |
| `GET /categories/**` | ✓ | ✓ | ✓ | ✓ |
| `POST /categories/**` | — | — | — | ✓ |
| `GET /demandes` | — | ✓ (own) | ✓ (open/matched) | ✓ |
| `POST /demandes` | — | ✓ | — | — |
| `PATCH /demandes/{id}/status` | — | ✓ (own) | — | — |
| `DELETE /demandes/{id}` | — | ✓ (own) | — | ✓ |
| `POST /demandes/{id}/reponses` | — | — | ✓ | — |
| `GET /demandes/{id}/reponses` | — | ✓ (own demandes) | — | ✓ |
| `PATCH /users/me/categories` | — | — | ✓ | — |
| `/admin/**` | — | — | — | ✓ |

### Exception → HTTP Status Mapping

| Exception | HTTP Status |
|-----------|-------------|
| `MethodArgumentNotValidException` | `400 Bad Request` |
| `ResourceNotFoundException` | `404 Not Found` |
| `DuplicateResourceException` | `409 Conflict` |
| `BusinessException` | Configurable (stored in exception) |
| `AccessDeniedException` | `403 Forbidden` |
| `BadCredentialsException` | `401 Unauthorized` |
| `Exception` (generic) | `500 Internal Server Error` |

---

## 7. Scoring & Matching Engine

### Overview

When a Buyer creates a Demande, the system asynchronously:
1. Finds all eligible suppliers via category path-prefix matching
2. Computes a `DemandeSupplierScore` per supplier
3. Schedules notifications based on score thresholds

### Supplier Eligibility (MatchingService)

A supplier is eligible if **at least one subscribed category** has a path that is a prefix of the demande's category path:

```java
// demande category path: "1.12.120"
// supplier subscribed to: "1.12" → eligible (parent match)
// supplier subscribed to: "2.20" → NOT eligible
demandePath.startsWith(supplierCategory.getPath() + ".")
  || demandePath.equals(supplierCategory.getPath())
```

### Score Components (DemandeScoreEngine)

Total max score = **100 points**

| Component | Weight (default) | Logic |
|-----------|-----------------|-------|
| **Category** | 35 | Exact match: 35 · Parent (-1 depth): 71% · Grandparent (-2): 43% · Higher ancestor: 29% |
| **Proximity** | 25 | Same wilaya: 25 · Different wilaya: 48% · No location: 20% |
| **Urgency** | 20 | Deadline < 3 days: 20 · < 7 days: 75% · < 14 days: 50% · ≥ 14 days or null: 25% |
| **Buyer** | 10 | MVP neutral: 60% (placeholder for future reputation) |
| **Quantity** | 10 | Specified & > 0: 10 · Not specified: 50% |

### Score Decay (ScoreDecayScheduler)

Runs nightly at **02:00**. `finalScore = baseScore × decayFactor`.

| Age of Demande | decayFactor |
|----------------|-------------|
| 0 days | 1.00 |
| 1–2 days | 0.85 |
| 3–5 days | 0.70 |
| 6–7 days | 0.50 |
| > 7 days | 0.20 |

The nightly job also **auto-expires** OPEN demandes whose deadline has passed (sets status to `CLOSED`).

### Notification Thresholds (per ScoringConfig)

| Score | Notification |
|-------|-------------|
| ≥ 80 | Immediate dispatch |
| ≥ 50 | Delayed 15 minutes |
| ≥ 30 | Delayed 1 hour |
| < 30 | IN_APP only (no push/email) |

These thresholds are configurable per sector via the `scoring_config` table.

---

## 8. Notification System

### Architecture

```
NotificationService.notifySuppliers(demande, suppliers, scores)
  │  [called @Async — non-blocking]
  │
  ├─ For each eligible supplier:
  │   ├─ Compute scheduledAt based on score threshold
  │   ├─ Create Notification(channel=PUSH, scheduledAt=...)
  │   └─ Create Notification(channel=EMAIL, scheduledAt=...)
  │
  └─ notificationRepository.saveAll(notifications)

NotificationDispatchScheduler [@Scheduled every 30s]
  │
  ├─ findPendingToDispatch(now) → unsent notifications where scheduledAt ≤ now
  │
  ├─ PUSH → FcmService.sendPushNotification(fcmToken, title, body)
  │          Uses Firebase Admin SDK
  │
  └─ EMAIL → EmailService.sendDemandeNotification(email, demande)
             Uses JavaMailSender (Gmail SMTP)
```

### Notification Channels

| Channel | Implementation | Trigger |
|---------|---------------|---------|
| `PUSH` | Firebase Admin SDK → Flutter app | Supplier has FCM token |
| `EMAIL` | JavaMailSender → Gmail SMTP | Always for eligible suppliers |
| `IN_APP` | No external dispatch — supplier sees scored feed | Always |

---

## 9. Scheduled Jobs

| Job | Schedule | Description |
|-----|----------|-------------|
| `ScoreDecayScheduler.runNightlyDecay()` | `cron: 0 0 2 * * *` (02:00 daily) | Decay `finalScore` for all OPEN demande scores; auto-expire past-deadline demandes |
| `NotificationDispatchScheduler.dispatchPendingNotifications()` | `fixedDelay: 30_000ms` | Poll DB for pending notifications and dispatch via FCM or email |

---

## 10. Configuration Reference

### `application.yml` — Key Properties

```yaml
app:
  jwt:
    secret: <hex-encoded-256-bit-key>
    access-token-expiry: 900000      # 15 minutes (ms)
    refresh-token-expiry: 604800000  # 7 days (ms)

  minio:
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: <secret>
    bucket: mawrid-attachments

  firebase:
    credentials-path: config/firebase-service-account.json

  cors:
    allowed-origins: http://localhost:3000,http://localhost:5173

  seed:
    superadmin:
      email: superadmin@mawrid.dz
      password: SuperAdmin@2026
      first-name: Super
      last-name: Admin

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mawrid
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20

  jpa:
    hibernate:
      ddl-auto: update    # dev — schema managed by Hibernate
    open-in-view: false

  flyway:
    enabled: false        # Flyway disabled; Hibernate manages schema

  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms

  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hour

  task:
    execution:
      pool:
        core-size: 5
        max-size: 20
        queue-capacity: 100

server:
  port: 8080
  compression:
    enabled: true

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
```

### Production Environment Variables

Used in `docker-compose.yml` and `application-prod.yml`:

| Variable | Description |
|----------|-------------|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | PostgreSQL connection |
| `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL credentials |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis connection |
| `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` | MinIO |
| `JWT_SECRET` | HMAC-SHA256 secret key |
| `JWT_ACCESS_EXPIRY`, `JWT_REFRESH_EXPIRY` | Token TTLs in ms |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP |
| `FIREBASE_CREDENTIALS_PATH` | Path to service account JSON |

---

## 11. Infrastructure & Deployment

### Docker Services

```
mawrid-net (bridge)
├── mawrid-postgres  (postgres:16-alpine, port 5432)
├── mawrid-redis     (redis:7-alpine, port 6379, password-protected)
├── mawrid-minio     (minio/minio:latest, ports 9000/9001)
└── mawrid-app       (built from Dockerfile, port 8080)
                     depends_on: postgres (healthy), redis (healthy), minio (healthy)
```

### Dockerfile (Multi-Stage)

```
Stage 1: maven:3.9-eclipse-temurin-21 AS builder
  ├── COPY pom.xml → mvn dependency:go-offline  (cache layer)
  └── COPY src    → mvn package -DskipTests

Stage 2: eclipse-temurin:21-jre-alpine (runtime)
  ├── addgroup/adduser mawrid (non-root)
  ├── COPY --from=builder *.jar → app.jar
  └── ENTRYPOINT java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar app.jar
```

### CI/CD Pipeline (`.github/workflows/deploy.yml`)

Triggered on push to `main`:

```
Job: test
  └─ mvn test -B

Job: build-and-deploy (needs: test)
  ├─ mvn package -DskipTests -B
  ├─ docker build -t mawrid-backend:<sha> -t mawrid-backend:latest .
  ├─ docker save | gzip → image.tar.gz
  ├─ scp image.tar.gz + docker-compose.yml → VPS /opt/mawrid/
  └─ SSH into VPS:
       docker load < image.tar.gz
       docker compose up -d --remove-orphans
       docker image prune -f
```

Required GitHub Secrets: `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`

---

## 12. Testing

### Test Strategy

- All integration tests extend `AbstractIntegrationTest`
- In-memory **H2** database (`ddl-auto: create-drop`)
- Cache uses `spring.cache.type: simple` (no Redis needed)
- `StringRedisTemplate` is mocked via `@MockBean`
- No Docker required for tests

### Test Classes

| Test Class | Coverage |
|-----------|----------|
| `AuthIntegrationTest` | Register, login, refresh, logout, duplicate email |
| `CategoryIntegrationTest` | Tree fetch, create, rename, move, add attribute |
| `DemandeIntegrationTest` | Create, list (role-based), get, status update, delete |
| `ReponseIntegrationTest` | Submit response, duplicate prevention, fetch available |
| `ScoringIntegrationTest` | Score computation, category/proximity/urgency components |
| `AdminIntegrationTest` | User listing, toggle-enabled, stats, simulation |
| `MawridApplicationTests` | Context loads |

Run all tests:
```bash
mvn test
```

All **15 integration tests pass** with `BUILD SUCCESS`.

---

## 13. Local Development Setup

### Prerequisites

- Java 21 (Temurin)
- Maven 3.9+
- PostgreSQL 16 running locally on port 5432
- Docker (for Redis & MinIO)

### Step-by-Step

**1. Start Redis and MinIO**
```bash
docker compose up redis minio -d
```

**2. Create PostgreSQL database**
```sql
CREATE USER mawrid WITH PASSWORD 'mawrid_dev_pass_123';
CREATE DATABASE mawrid OWNER mawrid;
```

**3. Run the application**
```bash
mvn spring-boot:run
```
> Hibernate auto-creates the schema on first run (`ddl-auto: update`)
> `DataInitializer` seeds 10 sector categories + subcategories + SUPERADMIN

**4. Access**

| URL | Description |
|-----|-------------|
| `http://localhost:8080/swagger-ui.html` | Swagger UI |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON |
| `http://localhost:8080/actuator/health` | Health check |
| `http://localhost:9001` | MinIO console |

### Default Dev Credentials

| Service | Credential |
|---------|-----------|
| PostgreSQL | `mawrid` / `mawrid_dev_pass_123` |
| Redis | password: `redis_dev_pass_456` |
| MinIO | `minioadmin` / `minioadmin_dev_789` |
| SUPERADMIN | `superadmin@mawrid.dz` / `SuperAdmin@2026` |

> **Note:** If port 8080 is occupied by Apache httpd, stop it: `net stop <service-name>`

---

## 14. Key Libraries & Versions

| Library | Version | Purpose |
|---------|---------|---------|
| Spring Boot | 3.4.3 | Framework |
| Java | 21 | Language (virtual threads ready) |
| jjwt | 0.12.x | JWT creation & validation |
| MapStruct | 1.6.x | DTO ↔ entity mapping |
| Lombok | latest | Boilerplate reduction |
| SpringDoc OpenAPI | 3 | Swagger UI auto-generation |
| PostgreSQL Driver | latest | JDBC driver |
| Lettuce | (via Spring Data Redis) | Redis client |
| MinIO SDK | latest | Object storage client |
| Firebase Admin SDK | latest | FCM push notifications |
| H2 | (test scope) | In-memory DB for tests |
| Maven | 3.9 | Build tool |

---

*Documentation generated for Mawrid Backend — Last updated: 2026-03-14*
