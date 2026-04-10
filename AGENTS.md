# AGENTS.md

This file provides guidance to Qoder (qoder.com) when working with code in this repository.

## Project Overview

"小川记账" (Chuan Bill) — a personal & family accounting app. Monorepo with two applications:

- `chuan-bill-app/` — Frontend (uni-app / Vue 3 / TypeScript, cross-platform mobile)
- `chuan-bill-server/` — Backend (Spring Boot 3 / Java 17, MySQL + Redis)

## Commands

### Root (run from repo root)
- `pnpm start` — Start both frontend and backend concurrently
- `pnpm start:frontend` / `pnpm start:backend` — Start individually
- `pnpm lint` / `pnpm lint:fix` — Lint both projects
- `pnpm lint:frontend` / `pnpm lint:backend` — Lint individual project
- `pnpm lint:frontend:fix` / `pnpm lint:backend:fix` — Autofix individual project

### Frontend (`chuan-bill-app/`)
- `pnpm dev` — H5 dev server
- `pnpm dev:mp-weixin` — WeChat mini-program dev
- `pnpm dev:app` — Native app dev
- `pnpm build` / `pnpm build:mp-weixin` / `pnpm build:app` — Platform builds
- `pnpm type-check` — Vue TypeScript type checking (`vue-tsc --noEmit`)
- `pnpm lint` / `pnpm lint:fix` — ESLint
- `pnpm alova-gen` — Regenerate API type definitions from backend OpenAPI spec

### Backend (`chuan-bill-server/`)
- `mvn spring-boot:run` — Run server
- `mvn test` — Run tests
- `mvn package` — Full build
- `mvn spotless:check` / `mvn spotless:apply` — Palantir Java Format (also runs on `validate` phase automatically)

## Architecture

### Frontend

**Routing**: File-based via `vite-plugin-uni-pages`. Pages discovered from `src/pages/` automatically — the `pages` array in `pages.config.ts` is intentionally empty. Sub-packages (`subPages/`, `subEcharts/`, `subAsyncEcharts/`) keep the main bundle lean. Route definitions can use `<route>` blocks in SFCs.

**Tabbar**: The native tabbar is hidden (`height: '0'` in `pages.config.ts`; `uni.hideTabBar()` called on APP). The `src/layouts/tabbar.vue` layout renders a custom `<wd-tabbar>` (wot-design-uni). Navigation uses `@wot-ui/router` (`useRouter`, `useRoute` — both auto-imported).

**Auto-imports**: Vue Composition API, Pinia, VueUse, Alova hooks (`useRequest`, etc.), `Apis` (the global API proxy), all stores (`useBillStore`, `useThemeStore`, etc.), and project composables/utils are auto-imported everywhere — no import statements needed in `.vue` files.

**UI**: wot-design-uni components (`wd-` prefix). UnoCSS for styling (utilities first; SCSS only for complex cases). Available icon sets: `carbon`, `icon-park-outline`, `lucide`, `material-symbols`, `mingcute`, `tabler`.

**State**:
- Pinia stores in `src/store/`, named `use{Name}Store`.
- Custom persistence plugin (`src/store/persist.ts`) auto-persists all stores to `uni.setStorageSync` keyed by `store.$id`. The `bill` store is excluded from persistence.
- `useThemeStore` — tracks `'light' | 'dark'` theme and a `themeVars` object of CSS variable values for runtime wot-design-uni theming.

**API layer** (`src/api/`):
- `src/api/apiDefinitions.ts` and `src/api/globals.d.ts` are **auto-generated** by `pnpm alova-gen` — never edit manually.
- `src/api/core/createApis.ts` builds a `Proxy`-based `Apis` object; property chains like `Apis.bill.getBillList(...)` resolve to Alova `Method` instances.
- In H5 dev, `beforeRequest` prepends `/api` to the base URL (matching a Vite proxy). Other platforms use `VITE_API_BASE_URL` directly.
- A `_t: Date.now()` param is appended to all GET requests to bust cache. Global cache is disabled (`cacheFor: null`).
- Loading indicator uses a 300ms delay to avoid flicker; supports `meta.silent` (suppress loading), `meta.loadingDelay`, and `meta.loadingText` per-request overrides.
- `src/api/mock/` — `@alova/mock` intercepts requests in development.

**Global feedback**: Never use raw `uni.showToast`/`uni.showLoading`. Always use the auto-imported composables: `useGlobalToast()`, `useGlobalMessage()`, `useGlobalLoading()`.

**Conditional compilation**: uni-app preprocessor directives (`// #ifdef H5`, `// #ifdef MP-WEIXIN`, `// #ifndef H5`, etc.) are used throughout for platform-specific code.

### Backend

Standard Spring Boot layered architecture under `com.samoy.chuanbillserver`:

```
config/       — Spring beans (MybatisPlus pagination, Sa-Token interceptor, OpenAPI, Redis)
controller/   — REST endpoints
dao/          — MyBatis-Plus Mapper interfaces
dto/          — Inbound request objects (@Validated)
entity/       — MyBatis entities
expection/    — GlobalExceptionHandler + BusinessException  [NOTE: intentional typo, do not rename]
result/       — Result<T> wrapper + ResultEnum
service/      — I*Service interfaces + impl/ subdirectory
vo/           — Outbound view objects
utils/        — MybatisCodeGeneration, OCRUtil
```

**Auth**: Sa-Token `SaInterceptor` on `/**`, excluding `/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`. Tokens stored in Redis. `NotLoginException` returns `Result.error(401, ...)`. Frontend 401/403 responses trigger a redirect to the login page.

**Response contract**: All endpoints return `Result<T>` — `{ code, message, data, timestamp }`. Success is `code == 200`. Business error codes: 1xxx (user), 2xxx (bill), 3xxx (file).

**ORM**: MyBatis-Plus with soft delete (`deleted`: 0=alive, 1=deleted) applied automatically to all queries.

**DTO/VO separation**: Inbound = `*DTO` with `@Validated`. Outbound = `*VO`. Never return raw entities from controllers.

**AI**: `AIController` → `OCRUtil` → Alibaba DashScope SDK for receipt/bill OCR recognition.

**Environment**: `springboot3-dotenv` is included — `.env` at the backend root is supported.

## Key Workflows

### Adding a new API endpoint
1. Add the controller method + service in the backend.
2. Run `pnpm alova-gen` in `chuan-bill-app/` to regenerate `apiDefinitions.ts` and `globals.d.ts`.
3. Use via `Apis.<tag>.<methodName>(...)` — types are fully inferred.

### Adding a new page
1. Create `src/pages/<name>/index.vue` (or use the `uni-page-generator` skill).
2. The page is auto-discovered by `vite-plugin-uni-pages`. Add a `<route>` block in the SFC for metadata.
3. For tabbar pages, update `pages.config.ts` tabBar items and `src/layouts/tabbar.vue`.

### Adding a new Pinia store
Follow the `pinia-store-generator` skill pattern. The persistence plugin auto-applies to all stores; add the store `$id` to the `excludedIds` array in `persist.ts` to opt out.

## Project-Specific Skills

Scaffolding skills in `chuan-bill-app/.claude/skills/`:
- `pinia-store-generator` — Pinia store boilerplate
- `uni-page-generator` — uni-app page + routing config
- `alova-api-module` — Alova request modules and mock data
- `wot-router-usage` — Router navigation patterns and guards
- `global-feedback` — Toast/Message/Loading usage guide
- `wot-ui` — wot-design-uni component development guide
- `frontend-design` — Production-grade frontend interface design
- `vue-composable-creator` — Vue 3 composable boilerplate

## Conventions

- Vue 3 `<script setup>` exclusively; TypeScript strict mode
- 2-space indent, LF line endings, UTF-8 (`.editorconfig`)
- Lombok (`@Data`, `@Getter`, etc.) for all Java boilerplate
- Chinese comments and documentation in the backend
- Conventional Commits enforced via commitlint: `feat:`, `fix:`, `chore:`, `refactor:`, `docs:`
- Pre-commit hook: lint-staged (ESLint autofix on frontend) + Spotless apply on backend
- Path alias: `@/*` → `./src/*` in the frontend
