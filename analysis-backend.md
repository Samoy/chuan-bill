# Chuan-Bill Backend Analysis Report

> Generated from source code analysis of `chuan-bill-server/` (Spring Boot 3 / Java 17)

---

## 1. REST API Endpoints

### 1.1 Auth Controller (`/auth`) — No auth required (excluded via interceptor)

| Method | Path | Purpose | Auth | Notes |
|--------|------|---------|------|-------|
| POST | `/auth/login-password` | Login with phone + password | No | Returns `TokenVO` |
| POST | `/auth/login-phone` | Login with phone + SMS code | No | Auto-creates user if not exists |
| POST | `/auth/login-wechat` | Login with WeChat mini-program code | No | Uses `WxMaService.jsCode2SessionInfo` |
| POST | `/auth/send-code` | Send SMS verification code | `@SaIgnore` | Stores code in Redis (5min TTL) |
| POST | `/auth/logout` | Logout current user | Yes | Calls `StpUtil.logout()` |

### 1.2 User Controller (`/user`)

| Method | Path | Purpose | Auth | Notes |
|--------|------|---------|------|-------|
| GET | `/user/profile` | Get current user profile | Yes | Phone number is masked in response |
| POST | `/user/profile/update` | Update nickname, avatar, gender | Yes | |
| POST | `/user/password/update-by-old` | Change password (old password) | Yes | BCrypt verification |
| POST | `/user/password/update-by-code` | Reset password (SMS code) | `@SaIgnore` | No login required |
| GET | `/user/has-password` | Check if user has set a password | Yes | |

### 1.3 Bill Controller (`/bill`)

| Method | Path | Purpose | Auth | Notes |
|--------|------|---------|------|-------|
| GET | `/bill/page-list` | Paginated bill list | Yes | Supports filtering by date, type, category, payment method, amount range, keyword |
| GET | `/bill/detail` | Get single bill detail | Yes | Ownership check |
| POST | `/bill/add` | Create a new bill | Yes | Supports optional `familyId` |
| POST | `/bill/batchCreate` | Batch create bills | Yes | For data sync/import |
| POST | `/bill/update` | Update existing bill | Yes | Ownership check |
| POST | `/bill/delete` | Soft-delete a bill | Yes | Ownership check |
| GET | `/bill/categories` | List categories (income/expense) | Yes | Optional `type` filter |
| GET | `/bill/payment-methods` | List payment methods | Yes | |
| GET | `/bill/monthly-stats` | Monthly income/expense/balance stats | Yes | |

### 1.4 Statistics Controller (`/statistics`)

| Method | Path | Purpose | Auth | Notes |
|--------|------|---------|------|-------|
| GET | `/statistics/overview` | Monthly stats overview | Yes | Supports familyId |
| GET | `/statistics/category` | Category breakdown with percentages | Yes | income/expense switch |
| GET | `/statistics/daily-trend` | Daily income/expense trend for chart | Yes | Fills missing days with zeros |
| GET | `/statistics/members-bill` | Per-member bill stats (family) | Yes | Family membership check |

### 1.5 Family Controller (`/family`)

| Method | Path | Purpose | Auth | Notes |
|--------|------|---------|------|-------|
| POST | `/family/create` | Create a family | Yes | Max 5 families per user; creator becomes owner |
| POST | `/family/update` | Update family name/avatar/desc | Yes | Owner only |
| GET | `/family/detail` | Get family detail | Yes | Membership check |
| POST | `/family/delete` | Delete family | Yes | Owner only; cascading delete of members & applies |
| GET | `/family/my-families` | List user's families | Yes | |
| POST | `/family/join` | Apply to join via invite code | Yes | Creates pending application; notifies owner |
| POST | `/family/leave` | Leave a family | Yes | Owner cannot leave |
| POST | `/family/remove-member` | Remove a member | Yes | Owner only; cannot remove owner |
| POST | `/family/transfer-owner` | Transfer ownership | Yes | Owner only; target must be member |
| GET | `/family/members` | List family members | Yes | Membership check |
| GET | `/family/pending-applies` | List pending join applications | Yes | Owner only |
| POST | `/family/handle-approve` | Approve/reject join application | Yes | Owner only; sends message |
| POST | `/family/refresh-invite-code` | Regenerate invite code | Yes | Owner only |

### 1.6 File Controller (`/file`)

| Method | Path | Purpose | Auth | Notes |
|--------|------|---------|------|-------|
| POST | `/file/temp/upload` | Upload temp file (for OCR) | Yes | Image only; stored locally |
| POST | `/file/upload` | Upload file to Cloudflare R2 | Yes | Returns CDN URL |

### 1.7 AI Controller (`/ai`)

| Method | Path | Purpose | Auth | Notes |
|--------|------|---------|------|-------|
| GET | `/ai/ocr` | OCR bill recognition from image | Yes | Uses DashScope agent; returns `BillVO` |
| GET | `/ai/text` | Extract bill info from text | Yes | Uses DashScope agent; returns `BillVO` |
| GET | `/ai/analysis` | AI bill analysis (cached) | Yes | Rate-limited (5/day for non-VIP); supports regeneration |

### 1.8 Message Controller (`/message`)

| Method | Path | Purpose | Auth | Notes |
|--------|------|---------|------|-------|
| GET | `/message/page-list` | Paginated message list | Yes | Filter by type and status |
| POST | `/message/mark-read` | Mark single message read | Yes | Ownership check |
| POST | `/message/mark-all-read` | Mark all messages read | Yes | |
| GET | `/message/unread-count` | Get unread count (total + family) | Yes | |

### 1.9 WebSocket Endpoint

| Protocol | Path | Purpose | Auth | Notes |
|----------|------|---------|------|-------|
| WS | `/asr` | Real-time voice recognition (ASR) | Token query param | Uses DashScope ASR SDK; binary audio streaming |

---

## 2. Database Entity Models & Relationships

### 2.1 Entity Summary

All entities use **String** primary keys (likely snowflake/objectId) and share common fields: `create_time`, `update_time`, `deleted` (soft delete).

#### `t_user` — User
| Field | Type | Description |
|-------|------|-------------|
| id | String (PK) | User ID |
| phone | String | Phone number (unique identifier for login) |
| openid | String | WeChat mini-program openid |
| password | String | BCrypt-hashed password |
| nickname | String | Display name |
| avatar | String | Avatar URL |
| gender | Byte | 0=unknown, 1=male, 2=female |
| status | Boolean | 0=disabled, 1=normal |
| is_vip | Boolean | VIP flag (unlocks unlimited AI analysis) |
| last_login_time | LocalDateTime | |
| deleted | Boolean | Soft delete flag |

#### `t_bill` — Bill
| Field | Type | Description |
|-------|------|-------------|
| id | String (PK) | |
| user_id | String (FK→t_user) | Bill creator |
| family_id | String (FK→t_family) | Nullable; set for shared family bills |
| name | String | Bill name/description |
| category_id | String (FK→t_category) | |
| payment_method_id | String (FK→t_payment_method) | |
| type | String | `income` or `expense` |
| amount | BigDecimal | |
| time | LocalDateTime | Bill date |
| remark | String | Optional note |
| source | String | `manual`, `ocr`, `voice`, `import` |
| deleted | Boolean | Soft delete |

#### `t_category` — Category
| Field | Type | Description |
|-------|------|-------------|
| id | String (PK) | |
| name | String | |
| icon | String | |
| type | String | `income` or `expense` |
| sort_order | Integer | |
| is_default | Boolean | |
| user_id | String | Null = system preset; non-null = user custom |
| deleted | Boolean | Soft delete |

#### `t_payment_method` — Payment Method
| Field | Type | Description |
|-------|------|-------------|
| id | String (PK) | |
| name | String | |
| icon | String | |
| sort_order | Integer | |
| is_default | Boolean | |
| user_id | String | Null = system preset |
| deleted | Boolean | Soft delete |

#### `t_family` — Family
| Field | Type | Description |
|-------|------|-------------|
| id | String (PK) | |
| name | String | |
| avatar | String | |
| owner_id | String (FK→t_user) | Current head-of-family |
| invite_code | String | 6-digit numeric code |
| description | String | |
| deleted | Boolean | Soft delete |

#### `t_family_member` — Family Member (junction table)
| Field | Type | Description |
|-------|------|-------------|
| id | String (PK) | |
| family_id | String (FK→t_family) | |
| user_id | String (FK→t_user) | |
| nickname | String | Member display name in family context |
| is_owner | Boolean | 0=normal, 1=head-of-family |
| join_time | LocalDateTime | |
| deleted | Boolean | Soft delete |

#### `t_family_join_apply` — Family Join Application
| Field | Type | Description |
|-------|------|-------------|
| id | String (PK) | |
| family_id | String (FK→t_family) | |
| user_id | String (FK→t_user) | Applicant |
| remark | String | Application note |
| status | Integer | 0=pending, 1=approved, 2=rejected |
| handle_user_id | String | Owner who processed the application |
| handle_time | LocalDateTime | |
| deleted | Boolean | Soft delete |

#### `t_message` — Notification Message
| Field | Type | Description |
|-------|------|-------------|
| id | String (PK) | |
| user_id | String (FK→t_user) | Recipient |
| title | String | |
| content | String | |
| type | String | `system`, `family`, `bill`, `budget` |
| status | Integer | 0=unread, 1=read |
| related_id | String | Polymorphic FK (family/bill/budget ID) |
| related_type | String | `family`, `bill`, `budget` |
| deleted | Boolean | Soft delete |

#### `t_budget` — Budget
| Field | Type | Description |
|-------|------|-------------|
| id | String (PK) | |
| user_id | String (FK→t_user) | |
| family_id | String (FK→t_family) | Nullable |
| month | LocalDate | Stored as 1st of month |
| amount | BigDecimal | Budget limit |
| use_amount | BigDecimal | Spent amount |
| deleted | Boolean | Soft delete |

#### `t_ai_usage` — AI Usage Tracking
| Field | Type | Description |
|-------|------|-------------|
| id | String (PK) | |
| user_id | String (FK→t_user) | |
| usage_date | LocalDate | Per-day tracking |
| analysis_count | Integer | Daily AI analysis call count |
| deleted | Boolean | Soft delete |

#### `t_ai_suggestion` — AI Analysis Cache
| Field | Type | Description |
|-------|------|-------------|
| id | String (PK) | |
| user_id | String (FK→t_user) | |
| month | String | Format: `YYYY-MM` |
| analysis_type | Integer | 1=personal, 2=family |
| target_id | String | user_id (personal) or family_id (family) |
| content | String | Cached AI analysis text |
| deleted | Boolean | Soft delete |

### 2.2 Entity Relationship Diagram

```
t_user ──1:N──> t_bill
t_user ──1:N──> t_category (user_id nullable = system preset)
t_user ──1:N──> t_payment_method (user_id nullable = system preset)
t_user ──1:N──> t_message
t_user ──1:N──> t_budget
t_user ──1:N──> t_ai_usage
t_user ──1:N──> t_ai_suggestion
t_user ──1:N──> t_family (as owner via owner_id)
t_user ──M:N──> t_family (via t_family_member junction)
t_family ──1:N──> t_family_member
t_family ──1:N──> t_family_join_apply
t_family ──1:N──> t_bill (via family_id, nullable)
t_family ──1:N──> t_budget (via family_id, nullable)
t_bill ──N:1──> t_category
t_bill ──N:1──> t_payment_method
```

---

## 3. Security Mechanisms

### 3.1 Sa-Token Authentication Flow

**Configuration** (`MyWebMvcConfig`):
- `SaInterceptor` applied to `/**` (all routes)
- Excluded paths: `/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`
- Login check: `StpUtil.checkLogin()` on every non-excluded request

**Token Configuration** (`application.yaml`):
- Token name: `token` (passed in request header or param)
- Timeout: `2592000` seconds (30 days)
- Active timeout: `-1` (no sliding expiration)
- Concurrent login: allowed
- Token sharing: disabled (each login creates a new token)
- Token style: `random-64` (64-char random string)
- Storage: Redis (Sa-Token's default Spring Boot integration)

**Authentication Flow**:
1. User calls one of 3 login endpoints (password / phone+code / WeChat)
2. Service validates credentials, finds or creates `User` entity
3. `StpUtil.login(userId)` creates a Sa-Token session in Redis
4. Returns `TokenVO` with `token`, `userId`, `nickname`, `expireTime`
5. Client sends `token` header on subsequent requests
6. `SaInterceptor` validates token against Redis on each request

**Per-request identity**: Controllers obtain `userId` via `StpUtil.getLoginIdAsString()`.

**WebSocket Auth**: Separate `WebSocketAuthInterceptor` extracts `token` from query parameter, validates via `StpUtil.getLoginIdByToken(token)`, stores `userId` in WebSocket session attributes.

### 3.2 Permission Control

No role-based access control exists. Authorization is **ownership-based**:
- **Bill operations**: Only the bill creator (`userId`) can view/update/delete
- **Family operations**: Owner-only operations (delete, update, manage members, handle applications, transfer ownership, refresh invite code) check `isOwner()` via `FamilyMember.is_owner`
- **Family membership**: Viewing family details, members, bills, and stats requires membership check
- **Password reset by code**: Open endpoint (`@SaIgnore`) — anyone with phone + valid SMS code can reset

### 3.3 Soft Delete

Configured globally in MyBatis-Plus via `application.yaml`:
```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

Every entity has a `deleted` field (Boolean). MyBatis-Plus automatically:
- Rewrites `DELETE` → `UPDATE SET deleted = 1`
- Adds `AND deleted = 0` to all `SELECT` queries
- Custom SQL in `BillMapper.xml` manually includes `AND deleted = 0` in raw queries

### 3.4 Password Security

- Passwords hashed with **BCrypt** (`cn.hutool.crypto.digest.BCrypt`)
- Login verification: `BCrypt.checkpw(rawPassword, hashedPassword)`
- Password update: `BCrypt.hashpw(newPassword)`
- Password is **never returned** in `UserVO` responses

### 3.5 Verification Code

- 6-digit numeric code generated locally
- Stored in Redis with key `sms:code:{phone}`, TTL 5 minutes
- Verified by comparing cached code; deleted from Redis after successful verification
- **Note**: SMS sending is stubbed (TODO in code) — only logs the code

---

## 4. Business Logic Flows

### 4.1 Bill CRUD

**Create Bill**:
1. Extract `userId` from Sa-Token session
2. Validate DTO (`@Validated`): name required, amount > 0, type = income/expense, source = manual/ocr/voice
3. If `familyId` provided, verify user is a family member
4. Persist `Bill` entity via MyBatis-Plus

**List Bills (Paginated)**:
1. If `familyId` provided, verify membership
2. Build `LambdaQueryWrapper` with filters: family/user, date range, type, category, payment method, amount range, keyword (name/remark LIKE)
3. Execute paginated query
4. Batch-load related entities (Category, PaymentMethod, Family, User) to avoid N+1
5. Convert to `BillVO` list with nested objects

**Update/Delete Bills**:
1. Fetch bill by ID
2. Verify ownership (`bill.userId == currentUserId`)
3. Update fields / soft-delete

**Batch Create**:
1. Convert list of `AddBillDTO` to `Bill` entities
2. Use `saveBatch()` via AOP proxy (for transactional support)
3. Returns count of created bills

### 4.2 User Management

**Login by Password**:
1. Validate phone + password non-blank
2. Find user by phone (must be status=normal)
3. Verify password with BCrypt
4. Call `StpUtil.login(userId)` → token created in Redis
5. Update `lastLoginTime`

**Login by Phone**:
1. Validate phone non-blank
2. Verify SMS code against Redis
3. Find or auto-create user (nickname: "用户" + masked phone)
4. Generate token

**Login by WeChat**:
1. Call WeChat `jsCode2SessionInfo` to get `openid`
2. Find or auto-create user (nickname: "微信用户" + random string)
3. Generate token

**Update Profile**:
1. Find user by ID
2. Update non-blank fields (nickname, avatar, gender) — **Note**: has a bug — conditions check `user.getX()` instead of `updateDTO.getX()`

**Get Profile**:
1. Fetch user by ID
2. Copy to `UserVO` (excludes password, openid)
3. Mask phone number: `PhoneUtil.hideBetween()` → e.g., "138****8000"

### 4.3 OCR & AI Recognition

**OCR Flow**:
1. Client uploads image via `/file/temp/upload` → saved to `temp/upload/{fileId}`
2. Client calls `/ai/ocr?fileId=xxx&fileExt=jpg`
3. Service reads temp file, encodes to Base64 data URI
4. Calls DashScope Agent (bill-recognition app) with image
5. Parses JSON response → extracts `ocrResult` → returns `BillVO`
6. Deletes temp file after processing

**Text Recognition Flow**:
1. Client calls `/ai/text?text=lunch+15+yuan`
2. Service calls DashScope Agent (bill-recognition app) with text prompt
3. Parses JSON response → extracts `nlpResult` → returns `BillVO`

**AI Analysis Flow**:
1. Client calls `/ai/analysis?analysisType=1&month=2026-04&regenerate=false`
2. Check VIP status for rate limiting
3. If not regenerate: return cached `AiSuggestion` from DB if exists
4. If regenerate or no cache:
   - Check remaining daily quota (5/day for non-VIP, unlimited for VIP)
   - Gather bill data: monthly stats + full bill list for the month
   - Build prompt with income/expense/balance/bill details
   - Call DashScope Agent (bill-analysis app)
   - Save result to `t_ai_suggestion` (upsert by user+month+type)
   - Increment daily usage counter in `t_ai_usage`

### 4.4 Voice Recognition (ASR)

**WebSocket-based real-time ASR**:
1. Client connects to `ws://host/asr?token=xxx`
2. `WebSocketAuthInterceptor` validates token, stores `userId`
3. Client sends `{"action": "start", "format": "pcm", "sampleRate": 16000}`
4. Server initializes DashScope ASR recognition session
5. Client streams binary audio data
6. Server sends real-time recognition results back as JSON: `{"type": "result", "data": {"text": "...", "sentenceEnd": true/false}}`
7. Client sends `{"action": "stop"}` to end recognition

### 4.5 Family Management

**Create Family**:
1. Check user hasn't reached 5-family limit
2. Create `Family` with generated 6-digit invite code
3. Create `FamilyMember` record (is_owner=true)
4. Return `FamilyVO` with member count

**Join Family**:
1. Find family by invite code
2. Check not already a member
3. Check no pending application exists
4. Create `FamilyJoinApply` (status=0)
5. Send notification message to family owner

**Handle Application**:
1. Validate application exists and is pending
2. Verify current user is family owner
3. If approved: create `FamilyMember` + notify applicant
4. If rejected: notify applicant

**Transfer Ownership**:
1. Verify current user is owner
2. Verify target is a family member
3. Update `is_owner` flags on both members
4. Update `family.ownerId`
5. Send notification to new owner

### 4.6 Statistics

**Monthly Overview**: Aggregates income/expense/balance via custom SQL (`BillMapper.xml`)

**Category Stats**: Groups by category with SUM, calculates percentage of total

**Daily Trend**: Groups by date, fills missing days with zeros for chart rendering

**Member Stats**: Groups by user within a family, calculates per-member expense/income with percentages

All statistics endpoints support optional `familyId` for family-scoped queries with membership verification.

---

## 5. Response Contract & Error Codes

### 5.1 `Result<T>` Response Structure

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1714900000000
}
```

**Factory methods**:
- `Result.success()` → code=200, data=null
- `Result.success(data)` → code=200, data=T
- `Result.error(message)` → code=500, custom message
- `Result.error(code, message)` → custom code + message
- `Result.error(ResultEnum)` → enum-driven

**`isSuccess()`**: Returns `true` when `code == 200`

### 5.2 Error Code Table

#### HTTP Status Codes
| Code | Enum | Description |
|------|------|-------------|
| 200 | `SUCCESS` | 操作成功 (Success) |
| 400 | `BAD_REQUEST` | 请求参数错误 (Bad request) |
| 401 | `UNAUTHORIZED` | 请求未授权 (Unauthorized) |
| 403 | `FORBIDDEN` | 请求被拒绝 (Forbidden) |
| 404 | `NOT_FOUND` | 请求资源不存在 (Not found) |
| 405 | `METHOD_NOT_ALLOWED` | 请求方法不允许 |
| 422 | `UNPROCESSABLE_ENTITY` | 请求参数校验失败 |
| 429 | `TOO_MANY_REQUESTS` | 请求过于频繁 |
| 500 | `ERROR` | 服务器内部错误 |
| 502 | `BAD_GATEWAY` | 网关错误 |
| 503 | `SERVICE_UNAVAILABLE` | 服务不可用 |
| 504 | `GATEWAY_TIMEOUT` | 网关超时 |

#### Business Error Codes — User (1xxx)
| Code | Enum | Description |
|------|------|-------------|
| 1001 | `USER_NOT_FOUND` | 用户不存在 |
| 1002 | `USER_DISABLED` | 用户已被禁用 |
| 1003 | `PASSWORD_ERROR` | 密码错误 |
| 1004 | `TOKEN_INVALID` | 验证码错误 |
| 1005 | `TOKEN_EXPIRED` | 验证码已过期 |
| 1006 | `PHONE_OR_PASSWORD_MISSING` | 手机号或密码不能为空 |
| 1007 | `PHONE_MISSING` | 手机号不能为空 |
| 1008 | `PASSWORD_MISSING` | 密码不能为空 |
| 1009 | `PASSWORD_NOT_SET` | 密码未设置 |
| 1010 | `LOGIN_ERROR` | 登录失败 |
| 1011 | `PARAM_VALID_ERROR` | 参数校验失败 |

#### Business Error Codes — Bill (2xxx)
| Code | Enum | Description |
|------|------|-------------|
| 2001 | `BILL_NOT_FOUND` | 账单不存在 |
| 2002 | `BILL_NOT_ALLOW_VIEW` | 无权查看此账单 |
| 2003 | `BILL_NOT_ALLOW_UPDATE` | 无权修改此账单 |
| 2004 | `BILL_NOT_ALLOW_DELETE` | 无权删除此账单 |
| 2101 | `BILL_OCR_FAILED` | 账单OCR识别失败 |
| 2102 | `BILL_TEXT_FAILED` | 账单文本识别失败 |
| 2103 | `BILL_ANALYSIS_FAILED` | 账单分析失败 |
| 2104 | `AI_ANALYSIS_RATE_LIMITED` | 今日AI分析次数已用完 |

#### Business Error Codes — File (3xxx)
| Code | Enum | Description |
|------|------|-------------|
| 3001 | `FILE_NOT_FOUND` | 文件不存在 |
| 3002 | `FILE_UPLOAD_FAILED` | 文件上传失败 |
| 3003 | `FILE_NOT_IMAGE` | 不是图片文件 |

#### Business Error Codes — Family (4xxx)
| Code | Enum | Description |
|------|------|-------------|
| 4001 | `FAMILY_NOT_FOUND` | 家庭不存在 |
| 4002 | `FAMILY_NOT_OWNER` | 仅户主可执行此操作 |
| 4003 | `FAMILY_NOT_MEMBER` | 您不是该家庭成员 |
| 4004 | `FAMILY_ALREADY_MEMBER` | 您已是该家庭成员 |
| 4005 | `FAMILY_INVITE_CODE_INVALID` | 邀请码无效 |
| 4006 | `FAMILY_OWNER_CANNOT_LEAVE` | 户主不能退出家庭 |
| 4007 | `FAMILY_APPLY_NOT_FOUND` | 加入申请不存在 |
| 4008 | `FAMILY_APPLY_ALREADY_PENDING` | 您已提交过申请 |
| 4009 | `FAMILY_APPLY_ALREADY_HANDLED` | 该申请已被处理 |
| 4010 | `FAMILY_TRANSFER_TARGET_NOT_MEMBER` | 目标用户不是家庭成员 |
| 4011 | `FAMILY_CANNOT_REMOVE_OWNER` | 不能移除户主 |
| 4012 | `FAMILY_CREATE_LIMIT_REACHED` | 每个用户最多创建5个家庭 |

#### Business Error Codes — Message (4050+)
| Code | Enum | Description |
|------|------|-------------|
| 4051 | `MESSAGE_NOT_FOUND` | 消息不存在 |

### 5.3 Exception Handling (`GlobalExceptionHandler`)

| Exception | Response |
|-----------|----------|
| `NotLoginException` (Sa-Token) | code=401 + exception message |
| `BusinessException` | code=custom + message |
| `MethodArgumentNotValidException` | code=400 + first field error message |
| `BindException` | code=400 + first field error message |
| `Exception` (catch-all) | code=500 + "系统异常，请稍后再试" |

---

## 6. Sensitive Data Handling

### 6.1 Phone Number Masking
- `UserServiceImpl.getProfileById()` calls `PhoneUtil.hideBetween()` on phone before returning `UserVO`
- Example: `13800138000` → `138****8000`

### 6.2 Password Handling
- Passwords stored as BCrypt hashes (never plaintext)
- `UserVO` does **not** include the password field
- `User.password` field exists in entity but is never serialized to response

### 6.3 WeChat OpenID
- `openid` is stored in `t_user` but **never returned** in any API response (not in `UserVO`)

### 6.4 Credential Storage
- MySQL password: configured via `${MYSQL_PASSWORD}` env var (default: `123456` in dev)
- Redis password: configured via `${REDIS_PASSWORD}` env var
- DashScope API key: `${DASHSCOPE_API_KEY}`
- WeChat appid/secret: `${WX_MINIAPP_APPID}`, `${WX_MINIAPP_SECRET}`
- R2 access keys: `${R2_ACCESS_KEY_ID}`, `${R2_ACCESS_KEY_SECRET}`
- All sensitive config values use environment variable placeholders

### 6.5 Verification Codes
- SMS codes stored in Redis with 5-minute TTL
- Deleted from Redis immediately after successful verification
- **Note**: SMS sending is currently stubbed (code only logged, not actually sent)

### 6.6 Potential Security Concerns
1. **Password reset endpoint is open** (`@SaIgnore` on `/user/password/update-by-code`) — relies solely on SMS code verification
2. **No rate limiting** on login endpoints (except AI analysis daily limit)
3. **No CSRF protection** configured (typical for token-based mobile API)
4. **WebSocket `setAllowedOrigins("*")`** — allows any origin to connect
5. **Temp file cleanup** only happens on successful OCR — failed OCR leaves orphaned files in `temp/upload/`
6. **AI analysis endpoint is GET** — analysis with `regenerate=true` mutates data, should arguably be POST
7. **`updateProfile` has a bug** — conditions check `user.getX()` (existing values) instead of `updateDTO.getX()` (new values), meaning fields may not actually update

---

## 7. Architecture Summary

### Tech Stack
- **Framework**: Spring Boot 3 / Java 17
- **ORM**: MyBatis-Plus with MySQL
- **Auth**: Sa-Token (Redis-backed sessions)
- **Cache**: Redis (Letture pool)
- **AI**: Alibaba DashScope SDK (Agents for OCR/text/analysis, ASR for voice)
- **File Storage**: Cloudflare R2 (S3-compatible) via AWS SDK v2
- **WeChat**: weixin-java-miniapp SDK
- **Validation**: Jakarta Bean Validation (`@Validated`)
- **Docs**: SpringDoc OpenAPI 3 (Swagger)
- **WebSocket**: Spring WebSocket for real-time ASR
- **Utilities**: Hutool, Lombok

### Project Structure
```
src/main/java/com/samoy/chuanbillserver/
├── config/          # WebMvc, MyBatis-Plus, Redis, R2, WebSocket, WxMa, OpenAPI
├── constant/        # SystemConstants
├── controller/      # 8 REST controllers + WebSocket handler
├── dao/             # 11 MyBatis mappers (BaseMapper extensions)
├── dto/             # 21 inbound DTOs with @Validated annotations
├── entity/          # 11 database entities
├── exception/       # BusinessException + GlobalExceptionHandler
├── handler/         # AsrHandler (WebSocket) + AsrSession
├── result/          # Result<T> + ResultEnum (error codes)
├── service/         # 11 service interfaces + 11 implementations
├── utils/           # AgentUtil (DashScope wrapper)
└── vo/              # 20 outbound view objects
```

### Key Design Patterns
- **DTO/VO separation**: Inbound DTOs with validation, outbound VOs with safe fields
- **Service layer**: Interface + Impl pattern, extends MyBatis-Plus `ServiceImpl`
- **Batch query optimization**: `BillServiceImpl` pre-loads related entities in batch to avoid N+1
- **Transactional boundaries**: `@Transactional` on family mutations (create, delete, join, transfer)
- **AOP proxy**: `AopContext.currentProxy()` used in `batchCreate` to ensure `@Transactional` on `saveBatch()`
