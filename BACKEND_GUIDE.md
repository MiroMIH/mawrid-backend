# Spring Boot Backend — Practical Guide
### Patterns, Decisions, and Best Practices from the Mawrid Project

---

## Table of Contents

1. [Project Structure — Package by Feature](#1-project-structure--package-by-feature)
2. [Entity Design — JPA Best Practices](#2-entity-design--jpa-best-practices)
3. [API Response Contract](#3-api-response-contract)
4. [Exception Handling — The Three-Exception Model](#4-exception-handling--the-three-exception-model)
5. [Security — JWT + Spring Security](#5-security--jwt--spring-security)
6. [The JWT Filter Pipeline](#6-the-jwt-filter-pipeline)
7. [Controller Design](#7-controller-design)
8. [Service Layer Patterns](#8-service-layer-patterns)
9. [Async Processing — The Proxy Bean Pattern](#9-async-processing--the-proxy-bean-pattern)
10. [Pagination — How to Do It Right](#10-pagination--how-to-do-it-right)
11. [Configuration Management](#11-configuration-management)
12. [Redis — Caching and Blacklisting](#12-redis--caching-and-blacklisting)
13. [Scoring Systems — How to Build One](#13-scoring-systems--how-to-build-one)
14. [API Testing with Bruno](#14-api-testing-with-bruno)
15. [Common Pitfalls — Lessons Learned](#15-common-pitfalls--lessons-learned)
16. [Scheduled Jobs — Cron vs Fixed Delay](#16-scheduled-jobs--cron-vs-fixed-delay)
17. [Score-Tiered Notification Dispatch](#17-score-tiered-notification-dispatch)
18. [Admin Back-Office — Stats, Export, Simulation](#18-admin-back-office--stats-export-simulation)
19. [Seller Category Subscriptions](#19-seller-category-subscriptions)

---

## 1. Project Structure — Package by Feature

### The Pattern

Organize code by **feature (domain)**, not by layer (controllers, services, repositories).

```
src/main/java/com/mawrid/
├── auth/               ← everything auth: controller, service, JwtService, DTOs
├── user/               ← User entity, UserService, UserController, DTOs
├── demande/            ← Demande entity, service, controller, DTOs, status enum
├── reponse/            ← Reponse entity, service, controller, DTOs
├── category/           ← Category entity, service, controller, mapper, DTOs
├── scoring/            ← scoring engine, score entity, config, decay scheduler
├── matching/           ← matching service + async orchestrator
├── notification/       ← notification entity, service, email, FCM, scheduler
├── admin/              ← admin controllers for user/demande/category management
├── seller/             ← seller-specific controllers (feed, category subscriptions)
├── storage/            ← MinIO file storage service
└── common/
    ├── config/         ← AsyncConfig, RedisConfig, OpenApiConfig, SchedulingConfig, DataInitializer
    ├── security/       ← SecurityConfig, JwtAuthFilter, UserDetailsServiceImpl
    ├── exception/      ← GlobalExceptionHandler + 3 exception types
    ├── response/       ← ApiResponse<T> wrapper
    ├── enums/          ← shared enums (AttributeType, NodeType, NotifType...)
    └── util/           ← SlugUtils and other stateless helpers
```

### Why This Works

- A new developer can find everything about `demande` in one folder
- Feature branches rarely touch the same files
- Adding a new feature = add a new package; nothing else changes
- `common/` is the only shared code — if something is only used by one feature, it lives in that feature's package

### The Rule

> If a class is only needed by one feature, it lives in that feature's package.
> If it's needed by two or more features, it goes in `common/`.

---

## 2. Entity Design — JPA Best Practices

### UUID Primary Keys

Use `UUID` for entities that cross API boundaries (shared by external clients).
Use `Long` (auto-increment) for internal/admin entities like categories.

```java
// Exposed to clients — use UUID
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;

// Internal tree structure — auto-increment is fine
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**Why UUID for user-facing IDs:** Prevents enumeration attacks (attacker can't guess `id=2` to get the next user). Decouples ID from insert order.

### Auditing — Never Set Timestamps Manually

Enable Spring Data JPA auditing once in your main class:

```java
@SpringBootApplication
@EnableJpaAuditing
public class MawridApplication { ... }
```

Then use the annotations on your entities:

```java
@EntityListeners(AuditingEntityListener.class)
public class Demande {

    @CreatedDate
    @Column(updatable = false)       // ← CRITICAL: prevents accidental updates
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

You never touch `createdAt` or `updatedAt` in service code. JPA handles it.

### equals() and hashCode() — The JPA Rule

Never use Lombok's `@EqualsAndHashCode` on JPA entities. Use identity-based equality:

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Demande d)) return false;
    return id != null && id.equals(d.id);  // null check: new (unsaved) entities
}

@Override
public int hashCode() {
    return getClass().hashCode();  // constant — safe for sets/maps pre-persist
}
```

**Why:** Hibernate proxies and detached entities behave incorrectly with field-based equality. The constant `hashCode()` is intentional — it prevents entities from being lost from `HashSet` when they transition from transient to persistent.

### FetchType — Always LAZY

```java
@ManyToOne(fetch = FetchType.LAZY)       // ✓ load on demand
@OneToMany(fetch = FetchType.LAZY)       // ✓ load on demand
```

Never use `EAGER`. It causes N+1 query problems silently. Load relationships explicitly when you need them in your service.

### Enum Storage — Always STRING

```java
@Enumerated(EnumType.STRING)   // stores "OPEN", "CLOSED"
private DemandeStatus status;
```

Never use `EnumType.ORDINAL`. Adding a new enum value in the middle of the list breaks all existing data.

### Builder Default Values

```java
@Builder.Default
private DemandeStatus status = DemandeStatus.OPEN;

@Builder.Default
private boolean active = true;

@Builder.Default
private List<DemandeAttribute> attributes = new ArrayList<>();
```

Without `@Builder.Default`, Lombok's `@Builder` ignores field initializers — you'd get `null` instead of `OPEN` or an empty list.

---

## 3. API Response Contract

### The Wrapper

Every endpoint returns `ApiResponse<T>`. This is the single most important consistency decision in a REST API.

```java
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)   // ← don't serialize null fields
public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;
    private final String error;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> ok(T data) { ... }
    public static <T> ApiResponse<T> ok(T data, String message) { ... }
    public static <T> ApiResponse<T> error(String message) { ... }
}
```

### Why `@JsonInclude(NON_NULL)`

- Success response: `data` is set, `error` is null → `error` doesn't appear in JSON
- Error response: `data` is null → `data` doesn't appear in JSON
- Frontend always gets clean, minimal JSON

### What the Frontend Always Gets

```json
// Success
{ "success": true, "message": "ok", "data": { ... }, "timestamp": "..." }

// Error
{ "success": false, "message": "Deadline must be at least 1 day in the future", "timestamp": "..." }
```

### How to Use It

```java
// 200 OK with data
return ResponseEntity.ok(ApiResponse.ok(result));

// 201 Created
return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(result, "Demande created"));

// Error — let GlobalExceptionHandler do it, don't do it manually
throw new BusinessException("Cannot close a demande that is not OPEN");
```

---

## 4. Exception Handling — The Three-Exception Model

Use exactly three exception types. Map them to HTTP status codes in one place.

### The Three Exceptions

```java
// 404 — resource not found
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String entity, Object id) {
        super(entity + " not found with id: " + id);
    }
}

// 409 — duplicate (unique constraint violation)
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) { super(message); }
}

// 400 (or any status) — business rule violated
public class BusinessException extends RuntimeException {
    private final HttpStatus status;

    public BusinessException(String message) {
        this.status = HttpStatus.BAD_REQUEST;
    }
    public BusinessException(String message, HttpStatus status) {
        this.status = status;  // use for 403, 409, 422, etc.
    }
}
```

### The Handler

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        // Collect all field validation messages into one string
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
    }

    // Catch-all — log it, return 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }
}
```

### How to Use in Services

```java
// ✓ Clean — no try/catch in service, no HTTP concern in service
public Demande findOrThrow(UUID id) {
    return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Demande", id));
}

public DemandeResponse closeDemande(UUID id, User buyer) {
    Demande d = findOrThrow(id);
    if (!d.getBuyer().getId().equals(buyer.getId()))
        throw new BusinessException("Not your demande", HttpStatus.FORBIDDEN);
    if (d.getStatus() != DemandeStatus.OPEN)
        throw new BusinessException("Cannot close a demande that is not OPEN");
    // ...
}
```

Services only throw domain exceptions. HTTP concerns live only in the handler.

---

## 5. Security — JWT + Spring Security

### The Full Security Stack

```
Request
  │
  ▼
JwtAuthFilter (OncePerRequestFilter)
  │  ├─ Extract token from Authorization header
  │  ├─ Check Redis blacklist
  │  ├─ Validate signature + expiry
  │  └─ Set SecurityContext
  │
  ▼
Spring Security FilterChain
  │  ├─ URL-level: /admin/** → ADMIN/SUPERADMIN only
  │  ├─ URL-level: /buyer/** → BUYER only
  │  ├─ URL-level: /seller/** → SUPPLIER only
  │  └─ Method-level: @PreAuthorize on individual methods
  │
  ▼
Controller
```

### SecurityConfig — The Key Decisions

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize on methods
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)     // stateless API — no CSRF needed
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers("/api/v1/buyer/**").hasRole("BUYER")
                .requestMatchers("/api/v1/seller/**").hasRole("SUPPLIER")
                .anyRequest().authenticated()
            )
            // Return 401 for unauthenticated, not a login redirect
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

**Key decisions explained:**
- `STATELESS` — no server-side sessions; JWT carries all state
- `HttpStatusEntryPoint(401)` — without this, Spring redirects to a login page for unauthenticated requests instead of returning 401
- `addFilterBefore` — your JWT filter runs before Spring's built-in username/password filter

### Role Storage Convention

```java
// Role enum
public enum Role { BUYER, SUPPLIER, ADMIN, SUPERADMIN }

// User entity — stored as ROLE_BUYER, ROLE_SUPPLIER, etc.
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
}
```

**Important:** Spring Security's `.hasRole("BUYER")` automatically prepends `ROLE_`. So `ROLE_BUYER` in the authority matches `hasRole("BUYER")`. Never use `hasAuthority("BUYER")` unless you stored the authority without the prefix.

### Password Encoding

Always BCrypt. Never store plain text, never use MD5/SHA.

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// On register
user.setPassword(passwordEncoder.encode(request.getPassword()));

// Spring's DaoAuthenticationProvider handles password comparison automatically
```

---

## 6. The JWT Filter Pipeline

### JwtService — Key Methods

```java
@Service
public class JwtService {

    private String buildToken(String subject, String type, long expiryMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("type", type)        // "access" or "refresh"
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMs))
                .signWith(signingKey())
                .compact();
    }

    // Logout: write token to Redis with TTL = remaining lifetime
    public void blacklistToken(String token) {
        long ttl = getRemainingTtlMs(token);
        if (ttl > 0) {
            redisTemplate.opsForValue()
                    .set("jwt:blacklist:" + token, "1", ttl, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("jwt:blacklist:" + token));
    }
}
```

### JwtAuthFilter

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(request, response, filterChain) {
        String authHeader = request.getHeader("Authorization");

        // No header or wrong format → skip (Spring Security handles as anonymous)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // Check blacklist BEFORE DB lookup
            if (jwtService.isTokenBlacklisted(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtService.extractUsername(token);

            // Only set auth if not already set (filter may run multiple times)
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)) {
                    // Build auth token and load into context
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            // Don't throw — let the filter chain continue; Spring Security will reject
        }

        filterChain.doFilter(request, response);
    }
}
```

**Critical:** Never return early with an error response from the filter. Always call `filterChain.doFilter()`. If the token is invalid, just don't set the `SecurityContext` — Spring Security will handle the 401 response.

### Access vs Refresh Token Separation

```java
// Refresh endpoint — verify it's actually a refresh token
public AuthResponse refresh(String refreshToken) {
    if (!jwtService.isRefreshToken(refreshToken)) {
        throw new BusinessException("Not a refresh token", HttpStatus.FORBIDDEN);
    }
    // ... generate new pair
}
```

Store `type: "refresh"` as a JWT claim so clients can't use a refresh token as an access token and vice versa.

---

## 7. Controller Design

### The Pattern

Controllers have one job: receive HTTP, delegate to service, return HTTP. No business logic.

```java
@RestController
@RequestMapping("/api/v1/buyer/demandes")
@RequiredArgsConstructor
@Tag(name = "Demandes", description = "Buyer demande management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('BUYER')")         // ← class-level: applies to all methods
public class DemandeController {

    private final DemandeService demandeService;

    @PostMapping
    @Operation(summary = "Create a demande")
    public ResponseEntity<ApiResponse<DemandeResponse>> create(
            @Valid @RequestBody DemandeRequest request,    // ← @Valid triggers Bean Validation
            @AuthenticationPrincipal User buyer            // ← injects authenticated user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(demandeService.create(request, buyer), "Demande created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DemandeSummaryResponse>>> listMy(
            @AuthenticationPrincipal User buyer,
            @RequestParam(required = false) DemandeStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable  // ← auto-parsed
    ) {
        return ResponseEntity.ok(ApiResponse.ok(demandeService.listMyDemandes(buyer, status, pageable)));
    }
}
```

### `@AuthenticationPrincipal User buyer`

This injects the fully-loaded `User` object from the `SecurityContext`. The `UserDetailsServiceImpl` loaded it during JWT validation. You get the full entity — no need to look it up again in the service.

```java
// ✓ DO THIS — user already loaded by the filter
public ResponseEntity<?> create(@AuthenticationPrincipal User buyer) {
    demandeService.create(request, buyer);
}

// ✗ NOT THIS — unnecessary DB hit
public ResponseEntity<?> create(@AuthenticationPrincipal UserDetails userDetails) {
    User buyer = userRepository.findByEmail(userDetails.getUsername()).get();
    demandeService.create(request, buyer);
}
```

### HTTP Status Codes

| Operation | Status |
|---|---|
| GET, successful | 200 |
| POST, creates resource | 201 |
| PATCH/PUT, updates resource | 200 |
| DELETE (soft) | 200 |
| DELETE (hard) | 204 (no body) |
| Validation error | 400 |
| Unauthenticated | 401 |
| Authenticated but wrong role/owner | 403 |
| Resource not found | 404 |
| Duplicate | 409 |

---

## 8. Service Layer Patterns

### Transaction Discipline

```java
// Read-only transactions: faster, no dirty-check overhead
@Transactional(readOnly = true)
public Page<DemandeSummaryResponse> listMyDemandes(...) { ... }

// Write transactions
@Transactional
public DemandeResponse closeDemande(UUID id, User buyer) { ... }
```

Always annotate services, not controllers. Never put `@Transactional` on a controller.

### Ownership Checks

Extract repeated ownership logic into a private method:

```java
private void assertOwner(Demande demande, User user) {
    if (!demande.getBuyer().getId().equals(user.getId())) {
        throw new BusinessException("You are not the owner of this demande", HttpStatus.FORBIDDEN);
    }
}

// Usage
public DemandeResponse closeDemande(UUID id, User buyer) {
    Demande demande = findOrThrow(id);
    assertOwner(demande, buyer);       // ← clean, reusable
    // ...
}
```

### Quality Scoring at Create Time

Computed fields that depend on the input (not on other entities) should be computed during creation:

```java
private int computeQualityScore(DemandeRequest request, Category category) {
    int score = 20;
    if (category.getDepth() >= 3) score += 10;                                    // deep category
    if (mandatory attributes > 0) score += 25;                                    // filled attrs
    if (description.length() >= 50) score += 15;                                  // detailed desc
    if (deadline is more than 3 days away) score += 10;                           // realistic deadline
    return Math.min(score, 100);
}
```

Store the result on the entity. Don't recompute on every read.

---

## 9. Async Processing — The Proxy Bean Pattern

### The Problem

`@Async` on a method only works when called **through a Spring proxy** — i.e., the call must come from a different bean. If `DemandeService` calls its own `@Async` method, Spring's AOP proxy is bypassed and the method runs synchronously.

### The Solution: Separate Orchestrator Bean

```java
// MatchingOrchestrator is a SEPARATE Spring bean from DemandeService
@Component
public class MatchingOrchestrator {

    @Async("taskExecutor")   // ← runs on the thread pool, not the request thread
    @Transactional
    public void run(Demande demande) {
        List<User> suppliers = matchingService.findEligibleSuppliers(demande);
        List<DemandeSupplierScore> scores = suppliers.stream()
                .map(s -> scoreEngine.computeScore(demande, s))
                .toList();
        scoreRepository.saveAll(scores);
        notificationService.scheduleNotifications(demande, scores);
    }
}

// DemandeService injects the orchestrator bean — call goes through the proxy
@Service
public class DemandeService {
    private final MatchingOrchestrator matchingOrchestrator;

    public DemandeResponse create(DemandeRequest request, User buyer) {
        Demande saved = demandeRepository.save(demande);
        matchingOrchestrator.run(saved);   // ← async, runs in background
        return demandeMapper.toResponse(saved);  // ← returns immediately
    }
}
```

### Always Wrap @Async in Try/Catch

```java
@Async("taskExecutor")
public void run(Demande demande) {
    try {
        // ... work
    } catch (Exception ex) {
        log.error("Matching failed for demande {}: {}", demande.getId(), ex.getMessage(), ex);
        // Never let exceptions bubble out of @Async — they'd be swallowed silently
    }
}
```

### ThreadPool Configuration

```java
@Bean(name = "taskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);           // always-alive threads
    executor.setMaxPoolSize(20);           // max under load
    executor.setQueueCapacity(100);        // jobs queued before new threads spawn
    executor.setKeepAliveSeconds(60);
    executor.setThreadNamePrefix("mawrid-async-");
    executor.setWaitForTasksToCompleteOnShutdown(true);  // graceful shutdown
    executor.setAwaitTerminationSeconds(60);
    executor.initialize();
    return executor;
}
```

`setWaitForTasksToCompleteOnShutdown(true)` is critical — without it, in-progress matching jobs are killed on app restart.

---

## 10. Pagination — How to Do It Right

### Setup

Add `@PageableDefault` to set sensible defaults. Spring auto-parses `?page=0&size=20&sort=createdAt,desc`.

```java
@GetMapping
public ResponseEntity<ApiResponse<Page<DemandeSummaryResponse>>> list(
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
) {
    return ResponseEntity.ok(ApiResponse.ok(service.list(pageable)));
}
```

### Service

```java
@Transactional(readOnly = true)
public Page<DemandeSummaryResponse> list(DemandeStatus status, Pageable pageable) {
    Page<Demande> page = (status != null)
            ? repository.findByStatus(status, pageable)
            : repository.findAll(pageable);

    return page.map(this::toSummary);   // ← map each element, preserves pagination metadata
}
```

### What the Client Receives

Spring's `Page<T>` serializes to:

```json
{
  "data": {
    "content": [ { ... }, { ... } ],
    "totalElements": 47,
    "totalPages": 3,
    "size": 20,
    "number": 0,
    "first": true,
    "last": false
  }
}
```

**Never** return a raw `List<T>` for endpoints that could grow. The frontend can't paginate without `totalElements`.

### Negative Size — Spring Coerces It

If a client sends `?size=-1`, Spring doesn't reject it — it silently uses the default. Don't add validation for this unless you need strict behavior.

---

## 11. Configuration Management

### application.yml Structure

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mawrid
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      leak-detection-threshold: 60000  # ← logs queries holding a connection >60s

  jpa:
    hibernate:
      ddl-auto: update       # dev only — never in prod
    open-in-view: false      # ← always disable: prevents lazy loading in view layer

app:
  jwt:
    secret: <base64-encoded-256bit-key>
    access-token-expiry: 900000     # 15 minutes in ms
    refresh-token-expiry: 604800000 # 7 days in ms

  cors:
    allowed-origins: http://localhost:3000
```

### `open-in-view: false`

By default, Spring Boot keeps a Hibernate session open until the HTTP response is written (OSIV pattern). This sounds convenient but causes silent N+1 queries when controllers access lazy relationships. Always disable it and load what you need in the service layer.

### `ddl-auto` Rules

| Environment | Setting | Why |
|---|---|---|
| Dev | `update` | Hibernate syncs schema from entities automatically |
| Test | `create-drop` | Fresh schema per test run |
| Prod | `validate` or `none` | **Never `update`** — use Flyway migrations |

**The `update` trap:** Hibernate's `update` mode adds columns but **never drops them and never modifies existing constraints**. The EXPIRED status bug happened exactly because of this — adding `EXPIRED` to the enum didn't update the DB check constraint. In prod, use Flyway.

### Hikari Connection Pool

The key settings:
- `leak-detection-threshold: 60000` — logs a warning if a connection is held more than 60 seconds (catches forgotten `@Transactional` or long queries)
- `maximum-pool-size: 20` — match this to `max_connections` on your PostgreSQL server

---

## 12. Redis — Caching and Blacklisting

### Two Uses in This Project

**1. JWT Blacklist** — manual key/value with TTL:

```java
// Key pattern: jwt:blacklist:<token>
// Value: "1" (doesn't matter, just presence matters)
// TTL: token's remaining lifetime

redisTemplate.opsForValue()
        .set("jwt:blacklist:" + token, "1", ttl, TimeUnit.MILLISECONDS);

boolean blacklisted = Boolean.TRUE.equals(redisTemplate.hasKey("jwt:blacklist:" + token));
```

The TTL equals the token's remaining life so Redis auto-expires the entry when the token would have expired anyway. No cleanup job needed.

**2. Spring Cache for Category Tree** — declarative:

```java
@Cacheable("categories")
public List<Category> getTree() { ... }

@CacheEvict(value = "categories", allEntries = true)
public Category create(CategoryRequest request) { ... }
```

Cache the full tree on read, evict the entire cache on any write. Simple and correct.

### Test Setup for Redis

In tests, replace Redis with Spring's in-memory cache so you don't need a running Redis:

```yaml
# application-test.yml
spring:
  cache:
    type: simple     # in-memory, no Redis required

  data:
    redis:
      host: localhost   # still defined, but StringRedisTemplate is mocked
```

```java
@MockBean
StringRedisTemplate redisTemplate;  // mock Redis in tests
```

---

## 13. Scoring Systems — How to Build One

The scoring system in this project is a clean template for any weighted-factor scoring.

### The Pattern

```
ScoringConfig (weights per sector)
    │
    ▼
DemandeScoreEngine.computeScore(demande, supplier)
    │  ├─ categoryScore   = f(category path overlap)
    │  ├─ proximityScore  = f(wilaya match)
    │  ├─ urgencyScore    = f(days until deadline)
    │  ├─ buyerScore      = f(buyer reputation)
    │  └─ quantityScore   = f(quantity specified)
    │
    ▼
DemandeSupplierScore (persisted, includes breakdown)
    │
    ▼
ScoreDecayScheduler (daily job, reduces score over time)
```

### Keep Scoring Pure

The score engine has no side effects — it receives a demande and supplier, returns a score object. It doesn't save anything.

```java
// ✓ Pure function — easy to test, easy to reason about
public DemandeSupplierScore computeScore(Demande demande, User supplier) {
    // ... pure computation
    return DemandeSupplierScore.builder()
            .finalScore(computed)
            .build();
}

// The orchestrator handles persistence
List<DemandeSupplierScore> scores = suppliers.stream()
        .map(s -> scoreEngine.computeScore(demande, s))  // pure
        .toList();
scoreRepository.saveAll(scores);   // side effect is here, clearly separated
```

### Score Decay

Scores should decay over time — a demande from 2 weeks ago should score lower than a fresh one:

```java
public BigDecimal computeDecayFactor(Demande demande) {
    long days = ChronoUnit.DAYS.between(demande.getCreatedAt().toLocalDate(), LocalDate.now());

    if (days <= 0)  return BigDecimal.valueOf(1.00);
    if (days <= 2)  return BigDecimal.valueOf(0.85);
    if (days <= 5)  return BigDecimal.valueOf(0.70);
    if (days <= 7)  return BigDecimal.valueOf(0.50);
    return BigDecimal.valueOf(0.20);
}
```

A scheduler runs daily, recomputes `finalScore = baseScore * decayFactor` for all open demandes.

---

## 14. API Testing with Bruno

### Why Bruno Over Postman

- Tests are plain `.bru` text files — they live in git alongside the code
- No sync account required
- `bru run` in CI without any GUI
- Full JS scripting in `script:post-response` blocks

### File Structure

```
bruno/
├── environments/local.bru   ← all env vars
├── auth/                    ← auth requests
├── demandes/                ← demande requests
├── workflows/               ← multi-step E2E flows
└── edge_cases/              ← negative/boundary tests
```

### Environment Variables

Declare all variables in `local.bru`:

```
vars {
  baseUrl: http://localhost:8080/api/v1
  buyerToken:
  demandeId:
}
```

Save values from responses in `script:post-response`:

```javascript
script:post-response {
  if (res.getStatus() === 201) {
    bru.setEnvVar("demandeId", String(res.getBody().data.id));
  }
}
```

### Test Assertions

```javascript
tests {
  test("status 201", function() {
    expect(res.getStatus()).to.equal(201);
  });
  test("status is OPEN", function() {
    expect(res.getBody().data.status).to.equal("OPEN");
  });
  // Idempotent: pass on re-run
  test("201 or 409", function() {
    expect([201, 409]).to.include(res.getStatus());
  });
}
```

### Idempotent Tests — Non-Negotiable

Always design tests to pass on repeated runs against the same database:

```javascript
// ✓ Idempotent — accepts both fresh create and already-exists
test("created or already exists", function() {
  expect([201, 409]).to.include(res.getStatus());
});

// ✗ Brittle — fails on second run
test("status 201", function() {
  expect(res.getStatus()).to.equal(201);
});
```

### Run Commands

```bash
# Full sweep
bru run auth users demandes reponses admin --env local

# E2E workflow
bru run workflows/e2e-category-demande-reponse --env local

# Single folder
bru run edge_cases/auth --env local
```

---

## 15. Common Pitfalls — Lessons Learned

### 1. JWT Same-Second Collision

**What happens:** Two JWTs generated for the same user within the same second are **byte-for-byte identical**. `jjwt` uses epoch-second precision for `iat`. If you generate an access token, then immediately call refresh (same second), both tokens are the same string. Logging out one logs out both.

**Fix:** Add a `jti` (JWT ID) UUID claim:
```java
.claim("jti", UUID.randomUUID().toString())
```

This makes every token unique regardless of timing.

### 2. DB Check Constraints Are Not Updated by Hibernate

`ddl-auto: update` adds new columns and tables but **never modifies existing constraints**. When you add a new enum value, the DB check constraint doesn't know about it.

**Fix:** Maintain a Flyway migration file for every structural change:
```sql
-- V3__add_expired_status.sql
ALTER TABLE demandes DROP CONSTRAINT demandes_status_check;
ALTER TABLE demandes ADD CONSTRAINT demandes_status_check
  CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED', 'EXPIRED'));
```

### 3. @Async Doesn't Work on Self-Calls

```java
@Service
public class MyService {

    // ✗ This does NOT run async — same bean, proxy bypassed
    @Async
    public void doSomething() { ... }

    public void callIt() {
        this.doSomething();   // ← calls the raw object, not the proxy
    }
}
```

Always inject async work into a separate `@Component` (the Orchestrator pattern above).

### 4. CREATE Response vs GET Response

When you save an entity and immediately map it to a response, **lazy-loaded relationships may not be populated yet**. This is why `POST /demandes` returned an empty `attributes[]` — attributes were saved after the demande, but the mapper ran on the entity before they were loaded.

**Fix:** Either reload from DB after saving, or accept that the POST response is minimal and the GET response is the source of truth.

### 5. 401 vs 403 from Spring Security

Spring Security returns **401** (not 403) when an authenticated user accesses a path their role doesn't allow. This is unintuitive but by design.

- **401** = not authenticated OR authenticated with wrong role at URL level
- **403** = authenticated, correct role, but business logic denies access (e.g., not the owner)

Design your tests and your frontend to expect 401 for wrong-role scenarios.

### 6. Shared Test State = Cascade Failures

If test A closes a demande and test B tries to submit a response to that same demande, test B fails. Never share mutable state (like `demandeId`) across tests that mutate the resource.

**Pattern:** Each test that needs a resource in a specific state creates its own:
```javascript
// Each folder that needs an OPEN demande creates one in a "00-setup.bru"
script:post-response {
  bru.setEnvVar("reponseDemandeId", String(res.getBody().data.id));
}
```

### 7. `open-in-view: true` (the default) Hides N+1 Problems

With `open-in-view: true`, you can access `demande.getBuyer().getEmail()` in your controller or even in your JSON serializer — it just silently fires a query. This works but scales terribly.

Disable it (`open-in-view: false`) and force yourself to load everything you need in the `@Transactional` service method.

### 8. Don't Forget `@Builder.Default` on Collections

```java
// ✗ This gives you null, not an empty list
@Builder.Default
private List<Category> children = new ArrayList<>();  // ← without @Builder.Default: null

// ✓ Always add @Builder.Default to initialized fields in @Builder classes
@Builder.Default
private List<Category> children = new ArrayList<>();
```

Without `@Builder.Default`, calling `Category.builder().build()` sets `children = null` even though you initialized it. Lombok's builder ignores field initializers unless this annotation is present.

---

## Quick Reference

### Starting a New Feature

1. Create package `src/main/java/com/project/<feature>/`
2. Add entity with UUID pk, `@EntityListeners(AuditingEntityListener.class)`, `@Enumerated(STRING)`, LAZY fetch
3. Add repository extending `JpaRepository<Entity, UUID>`
4. Add service with `@Service @RequiredArgsConstructor @Transactional`
5. Add controller with `@RestController @RequestMapping @PreAuthorize`
6. Use `ResourceNotFoundException` / `DuplicateResourceException` / `BusinessException` — never return error ResponseEntities directly
7. Return `ApiResponse.ok(result)` from every controller method
8. Add Bruno tests: happy path + at least `missing required field`, `wrong owner`, `not found`

### Dependency Stack

```
Controller
  └─► Service (@Transactional)
        ├─► Repository (JpaRepository)
        ├─► Other Services
        └─► Orchestrator (@Async for background work)
              ├─► MatchingService (find eligible suppliers)
              ├─► DemandeScoreEngine (compute scores, pure)
              └─► NotificationService (schedule notifications)
                        │  writes Notification rows (sent=false)
                        ▼
              [fixedDelay scheduler]
              NotificationDispatchScheduler
                    └─► FcmService / EmailService
```

### Security Rule Decision Tree

```
Does the request need authentication?
  No  → .permitAll()
  Yes → Is it role-specific?
          Yes → URL path maps to role (/buyer/**, /seller/**, /admin/**)
                OR @PreAuthorize("hasRole('X')") on the method
          No  → .anyRequest().authenticated()
```

---

## 16. Scheduled Jobs — Cron vs Fixed Delay

Two `@Scheduled` flavors are used in this project. Choose based on semantics.

### `fixedDelay` — Polling Loop

Use when you want to process a queue of pending work as fast as possible, but not pile up if the previous run is slow.

```java
// NotificationDispatchScheduler — runs 30s after previous run completes
@Scheduled(fixedDelay = 30_000)
@Transactional
public void dispatchPendingNotifications() {
    List<Notification> pending = notificationRepository.findPendingToDispatch(LocalDateTime.now());
    if (pending.isEmpty()) return;

    for (Notification notif : pending) {
        try {
            dispatch(notif);
            notif.setSent(true);
            notif.setSentAt(LocalDateTime.now());
        } catch (Exception ex) {
            notif.setFailureReason(ex.getMessage());   // ← record failure, don't throw
            log.error("Failed to dispatch notification {}: {}", notif.getId(), ex.getMessage());
        }
    }
    notificationRepository.saveAll(pending);  // ← persist sent/failed state in one batch
}
```

**Key behaviors:**
- `fixedDelay` starts the 30s countdown *after* the previous execution finishes — no overlap possible
- Always catch per-item exceptions so one bad notification doesn't abort the whole batch
- Never use `fixedRate` for DB polling — it can overlap if the previous run was slow, causing double-dispatch

### `cron` — Calendar-Aligned Jobs

Use for jobs that should run at a specific wall-clock time, not relative to the last run.

```java
// ScoreDecayScheduler — runs every night at 02:00
@Scheduled(cron = "0 0 2 * * *")
@Transactional
public void runNightlyDecay() {
    List<DemandeSupplierScore> scores = scoreRepository.findAllOpenScores();
    for (DemandeSupplierScore score : scores) {
        BigDecimal newDecay = scoreEngine.computeDecayFactor(score.getDemande());
        if (newDecay.compareTo(score.getDecayFactor()) != 0) {
            score.setDecayFactor(newDecay);
            score.setFinalScore((int) (score.getBaseScore() * newDecay.doubleValue()));
            score.setLastDecayAt(LocalDateTime.now());
        }
    }
    scoreRepository.saveAll(scores);

    expireDeadlinedDemandes();   // ← compound jobs: do all nightly cleanup in one transaction
}

private void expireDeadlinedDemandes() {
    List<Demande> expired = demandeRepository.findExpiredOpen(LocalDate.now());
    for (Demande d : expired) {
        d.setStatus(DemandeStatus.EXPIRED);
        d.setExpiredAt(LocalDateTime.now());
    }
    demandeRepository.saveAll(expired);
}
```

### Enable Scheduling

`@Scheduled` does nothing without `@EnableScheduling`. Put it on a dedicated config class, not on the main application class:

```java
// common/config/SchedulingConfig.java
@Configuration
@EnableScheduling
public class SchedulingConfig { }
```

### Summary

| | `fixedDelay` | `cron` |
|---|---|---|
| Next run starts | After previous finishes | At wall-clock time |
| Use for | Polling queues | Nightly/weekly batch jobs |
| Risk if skipped | Jobs pile up in next poll | Run missed entirely |

---

## 17. Score-Tiered Notification Dispatch

After matching, notifications are not all sent at once. They're tiered by score and scheduled into the future. This prevents low-relevance suppliers from being spammed while high-relevance ones get notified immediately.

### The Tiers

```java
// NotificationService
private static final int TIER_IMMEDIATE  = 80;   // ← send right now
private static final int TIER_DELAYED15M = 50;   // ← send in 15 minutes
private static final int TIER_DELAYED1H  = 30;   // ← send in 1 hour
                                                  // < 30 → in-app feed only

@Async("taskExecutor")
@Transactional
public void scheduleNotifications(Demande demande, List<DemandeSupplierScore> scores) {
    LocalDateTime now = LocalDateTime.now();
    for (DemandeSupplierScore score : scores) {
        int finalScore = score.getFinalScore();
        if      (finalScore >= TIER_IMMEDIATE)  createNotification(supplier, demande, now,               NotifChannel.PUSH);
        else if (finalScore >= TIER_DELAYED15M) createNotification(supplier, demande, now.plusMinutes(15), NotifChannel.PUSH);
        else if (finalScore >= TIER_DELAYED1H)  createNotification(supplier, demande, now.plusHours(1),   NotifChannel.PUSH);
        else                                    createNotification(supplier, demande, now,               NotifChannel.IN_APP);
    }
}
```

### Two-Phase Design

**Phase 1 (async, in MatchingOrchestrator):** Schedule notifications into the `notifications` table with a `scheduledAt` timestamp and `sent = false`.

**Phase 2 (scheduled poller):** `NotificationDispatchScheduler` polls every 30 seconds for records where `scheduledAt <= now AND sent = false`, then actually calls FCM / email.

```
Demande saved
    │
    ▼ (async)
MatchingOrchestrator.run()
    │ matchingService.findEligibleSuppliers()
    │ scoreEngine.computeScore() × N
    │ scoreRepository.saveAll()
    └─► notificationService.scheduleNotifications()
              │  writes Notification rows (sent=false, scheduledAt=future)
              ▼
    [30s polling loop]
NotificationDispatchScheduler.dispatchPendingNotifications()
    │  finds where scheduledAt <= now AND sent = false
    │  calls fcmService or emailService
    └─► sets sent=true, sentAt=now
```

### Why This Pattern Over Direct FCM Calls

- **Resilience:** If FCM is down, the notification stays `sent=false` and will be retried on the next poll.
- **Decoupling:** The scoring/matching logic doesn't need to know about FCM — it just writes a row.
- **Auditability:** Every notification attempt and its result (sent or failure reason) is persisted.
- **Score-based delivery delay:** Easy to implement — just set `scheduledAt` to a future time.

### NotifChannel Values

```java
public enum NotifChannel {
    PUSH,    // Firebase Cloud Messaging — requires fcmToken on User
    EMAIL,   // JavaMailSender — used for demande notifications
    IN_APP   // No external dispatch — supplier sees it in their scored feed
}
```

---

## 18. Admin Back-Office — Stats, Export, Simulation

### What Admin Gets

`/api/v1/admin/**` requires `ADMIN` or `SUPERADMIN` role (class-level `@PreAuthorize`).

| Endpoint | Method | Description |
|---|---|---|
| `/admin/users` | GET | Paginated user list |
| `/admin/users/{id}` | GET | User detail |
| `/admin/users/{id}/toggle-enabled` | PATCH | Enable/disable account |
| `/admin/users/export` | GET | Download all users as CSV |
| `/admin/stats` | GET | Dashboard stats (users, demandes, reponses counts) |
| `/admin/matching/simulate` | POST | Dry-run matching — no DB writes |
| `/admin/demandes` | GET/PATCH | Demande moderation |
| `/admin/categories/**` | * | Category tree management |

### Dry-Run / Simulation Endpoints

A simulation endpoint lets admins test matching logic against hypothetical inputs without touching the database. The pattern:

```java
@PostMapping("/matching/simulate")
@Operation(summary = "Dry-run matching simulation (reads only, no DB writes)")
public ResponseEntity<ApiResponse<List<SimulationResult>>> simulate(
        @Valid @RequestBody SimulationRequest request
) {
    return ResponseEntity.ok(ApiResponse.ok(adminService.simulate(request)));
}
```

```java
// AdminService.simulate — reads only, no @Transactional write needed
@Transactional(readOnly = true)
public List<SimulationResult> simulate(SimulationRequest request) {
    // Build a transient Demande from the request (don't save it)
    // Find eligible suppliers using MatchingService (read-only)
    // Run DemandeScoreEngine on each (pure computation, no DB writes)
    // Return results as SimulationResult DTOs
}
```

**The pattern:** Build a transient entity (not persisted), run it through the same services that production uses, collect results into a DTO. Because `DemandeScoreEngine.computeScore()` is pure (no side effects), it works identically on real and simulated demandes.

### CSV Export Without a Library

Export user data as CSV using only the standard library:

```java
public byte[] exportUsersCsv() {
    List<User> users = userRepository.findAll();
    StringBuilder sb = new StringBuilder("id,email,role,enabled,createdAt\n");
    for (User u : users) {
        sb.append(u.getId()).append(',')
          .append(u.getEmail()).append(',')
          .append(u.getRole()).append(',')
          .append(u.isEnabled()).append(',')
          .append(u.getCreatedAt()).append('\n');
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
}
```

Return it with `Content-Disposition: attachment` so the browser downloads it:

```java
return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(csv);
```

Note: `ResponseEntity<byte[]>` — not wrapped in `ApiResponse<T>` because it's a binary download, not a JSON API response.

---

## 19. Seller Category Subscriptions

Suppliers subscribe to categories they supply. This is the source of truth for matching — `MatchingService.findEligibleSuppliers()` uses path-prefix matching on the supplier's subscribed categories.

### The Controller

```java
@RestController
@RequestMapping("/api/v1/seller/categories")
@PreAuthorize("hasRole('SUPPLIER')")
public class SellerCategoryController {

    @GetMapping("/subscribed")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getSubscribed(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getSubscribed(user)));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateCategories(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateCategoriesRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateCategories(user.getId(), request, user)));
    }
}
```

### Path-Prefix Matching

Categories use a materialized path (`"1.3.7"` = sector 1 → subcategory 3 → leaf 7). The matching query finds all suppliers where *any* of their subscribed category paths is a prefix of (or equal to) the demande's category path:

```java
// UserRepository — native JPQL query
@Query("SELECT DISTINCT u FROM User u JOIN u.categories c WHERE u.role = 'SUPPLIER' AND u.enabled = true " +
       "AND (:path = c.path OR :path LIKE CONCAT(c.path, '.%'))")
List<User> findEligibleSuppliers(@Param("path") String demandePath);
```

This means a supplier who subscribed to sector `"1"` matches all demandes in `"1.*"`. A supplier who subscribed to `"1.3"` only matches `"1.3"` and `"1.3.*"`.

The `DemandeScoreEngine` then scores by how close the subscription is to the demande's category:

| Depth difference | Score fraction |
|---|---|
| 0 (exact match) | 100% of `categoryWeight` |
| 1 (parent) | 71% |
| 2 (grandparent) | 43% |
| 3+ (higher ancestor) | 29% |

### Why PATCH Not PUT

`PATCH /seller/categories` replaces the full subscription list with what the client sends. This feels like a PUT, but `PATCH` is more accurate — the client is sending a partial update to the user resource (changing only the categories field), not replacing the entire user object.
