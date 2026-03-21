# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

"小川记账" (Chuan Bill) -- a personal & family accounting app. Monorepo with two applications:

- `chuan-bill-app/` -- Frontend (uni-app / Vue 3 / TypeScript, cross-platform mobile)
- `chuan-bill-server/` -- Backend (Spring Boot 3 / Java 17, MySQL + Redis)

## Common Commands

### Root (monorepo orchestration)
- `pnpm start` -- Start both frontend and backend concurrently
- `pnpm lint` / `pnpm lint:fix` -- Lint both projects
- `pnpm lint:frontend` / `pnpm lint:backend` -- Lint individual project

### Frontend (chuan-bill-app/)
- `pnpm dev` -- H5 dev server | `pnpm dev:mp-weixin` -- WeChat mini-program dev
- `pnpm build` / `pnpm build:mp-weixin` / `pnpm build:app` -- Platform builds
- `pnpm lint` / `pnpm lint:fix` -- ESLint
- `pnpm type-check` -- Vue TypeScript type checking (vue-tsc)
- `pnpm alova-gen` -- Regenerate API definitions from OpenAPI spec

### Backend (chuan-bill-server/)
- `mvn spring-boot:run` -- Run server
- `mvn spotless:check` / `mvn spotless:apply` -- Code formatting

## Architecture

### Frontend
- **Routing**: File-based via `vite-plugin-uni-pages`. Pages in `src/pages/`, sub-packages in `src/subPages/`. Config in `pages.config.ts`.
- **State**: Pinia with custom persistence plugin (`src/store/persist.ts`) using `uni.getStorageSync`. Stores use `use{Name}Store` naming.
- **API layer**: Alova.js (`src/api/core/instance.ts`) with `@alova/adapter-uniapp`. Response handlers in `src/api/core/handlers.ts` (401/403 redirects to login, error handling). Mock data in `src/api/mock/`.
- **UI**: wot-design-uni components (`wd-` prefix), UnoCSS for styling, layouts via `vite-plugin-uni-layouts` (`src/layouts/`).
- **Auto-imports**: Vue Composition API, Pinia, VueUse, Alova hooks, and custom composables/stores/utils are auto-imported -- no manual imports needed.
- **Path alias**: `@/*` -> `./src/*`
- **Sub-packages**: ECharts and demo pages in `subPages/`, `subEcharts/`, `subAsyncEcharts/`.

### Backend
- **Layered**: Controller -> Service (interface + impl) -> Mapper (DAO) -> Entity
- **Auth**: Sa-Token interceptor, all paths except `/auth/**` require authentication, tokens stored in Redis.
- **Response**: Unified `Result<T>` wrapper (code, message, data, timestamp). Status codes in `ResultEnum` (200 success, 4xx client, 5xx server, 1000+ business errors).
- **ORM**: MyBatis-Plus with soft delete (`deleted` field: 0/1).
- **Exception handling**: `@ControllerAdvice` for `NotLoginException`, `BusinessException`, generic `Exception`.

## Coding Conventions

### Frontend
- Vue 3 `<script setup>` exclusively, TypeScript strict mode
- UnoCSS utilities as primary styling; SCSS only for complex cases
- **Never** use raw `uni.showToast` -- use `GlobalToast`/`GlobalMessage`/`GlobalLoading` from the global feedback system
- Pinia stores: use `defineStore` with `use{Name}Store` naming
- 2-space indent, LF line endings, UTF-8 (`.editorconfig`)

### Backend
- Lombok (`@Data`, `@Getter`, etc.) for boilerplate
- Palantir Java Format enforced via Spotless
- Chinese comments and documentation
- Package naming exception dir has a typo: `expection/` (not `exception`)

### Git
- Conventional Commits required (`feat:`, `fix:`, `chore:`, `refactor:`)
- Pre-commit: lint-staged (ESLint fix on frontend) + Spotless apply on backend

## Claude Skills

Project-specific scaffolding skills in `chuan-bill-app/.claude/skills/`:
- `pinia-store-generator` -- Create Pinia stores following project conventions
- `uni-page-generator` -- Generate uni-app pages with routing config
- `alova-api-module` -- Create Alova request modules and mock data
- `wot-router-usage` -- Router navigation patterns and guards
- `global-feedback` -- Toast/Message/Loading usage guide
- `wot-ui` -- wot-design-uni component development guide
- `frontend-design` -- Production-grade frontend interface design
- `vue-composable-creator` -- Create Vue 3 composables
