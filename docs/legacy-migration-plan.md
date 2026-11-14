# Legacy UI → React Migration — Analysis & Plan

> Status: **analysis / proposal only**. No code changes have been made. This document assesses what the legacy pages do and proposes how to port them to the existing React SPA with identical functionality.

## Short answer

Yes, the legacy pages can be rebuilt in React with the same functionality. They are standalone jQuery pages that talk to the **same REST API** the React app already uses, so no backend changes are required for the core migration. The work is substantial but mechanical and low-risk: the main cost is reimplementing the grid-heavy screens (jqWidgets `jqxGrid`) and their edit dialogs in React/MUI. It is well suited to an incremental, page-by-page rollout because each legacy page is already isolated behind an `/legacy/*` route and an iframe.

---

## 1. What the legacy UI is today

The legacy UI is the pre-React application, still shipped inside the backend and embedded in the React shell via `<iframe>`.

- **Entry point (React side):** `gogas-service/frontend/src/pages/legacy/LegacyPage.tsx` renders `<iframe src="/legacy-ui/{page}">` inside the SPA. Eight wrapper components map to the `/legacy/*` routes declared in `Routes.tsx`.
- **Server side:** `FrontendController.java` serves each legacy page as a Thymeleaf template under `/legacy-ui/*`, guarded by the same method-security annotations as the API (`@IsManager`, `@IsOrderManager`, `@IsAdmin`). The two order-detail pages receive `orderId` / `userId` as model attributes.
- **Templates:** `gogas-service/src/main/resources/templates/legacy/{admin,referenti,user}/*.html`, all decorated by `legacy/master.html` (Thymeleaf Layout Dialect).
- **Static assets:** `gogas-service/src/main/resources/static/legacy-ui/` — jQuery 1.11, jQuery UI, **jqWidgets** (jqxGrid etc.), Bootstrap 3, select2, bootstrap-datepicker, pnotify, qTip, moment, plus app scripts under `Scripts/GoGas/` (`utility.js`, `tables-definition.js`, `password.js`).

### Legacy tech stack

| Concern | Legacy | React app (target) |
|---------|--------|--------------------|
| DOM / rendering | jQuery 1.11 + Thymeleaf fragments | React 17 + TSX |
| Data grid | jqWidgets `jqxGrid` (sort/filter/page/inline edit/export) | Extended `DataTable.tsx` (MUI) for CRUD lists + TanStack Table (headless) for complex grids — see §3 |
| Dialogs | `jqxWindow` | MUI `Dialog` / existing `ActionDialog` |
| Dropdowns | select2 + `jqxDropDownList` | `react-select` (already a dependency) |
| Date inputs | `jqxDateTimeInput` + bootstrap-datepicker | `@material-ui/pickers` (already a dependency) |
| Notifications | pnotify (`createNotification`) | notistack (`enqueueSnackbar`) |
| Validation | jquery.validate + `jqxValidator` | form-level checks in React |
| HTTP | `$.ajax` with `beforeSend: setJWT` | `axios_utils` + `useXxxAPI` hooks |

### How auth works in the legacy pages (important)

The legacy pages read the JWT **from `localStorage`** (`state.authentication.jwtToken`, written by the React Redux store) and attach it as an `Authorization: Bearer` header on every `$.ajax` call (`setJWT` in `master.html`). This differs from the React app, which relies on the `jwt-token` cookie. Both reach the same backend. After migration, each ported page simply uses the existing axios layer and the cookie/token handling already in place — no new auth code.

---

## 2. Inventory of legacy pages

| # | Route (`/legacy/*`) | Template | Role | Complexity | Primary API calls |
|---|---------------------|----------|------|-----------|-------------------|
| 1 | `suppliers` | `admin/suppliers.html` | Manager | **Low** (~225 lines) | `GET/POST/PUT/DELETE /api/supplier`, `/api/supplier/list` |
| 2 | `configuration` | `admin/configuration.html` | Admin | **Low** (~67 lines) | `GET/PUT /api/configuration` |
| 3 | `manage-friends` | `user/manage-friends.html` | User | **Medium** (~305 lines) | `/api/friend` CRUD + select |
| 4 | `user-orders-list` | `user/orders-list.html` | User | **Medium** (~220 lines) | `POST /api/order/user/list`, `/api/ordertype/select`, `/api/home/order/status`, `/api/friend/select` |
| 5 | `products` | `admin/products.html` | Manager | **High** (~807 lines) | `/api/products` CRUD, `/list/{id}`, `/sync`, `/import`, `/export`, `/api/category/list/{id}`, `/api/supplier/select`, `/api/ordertype/...` |
| 6 | `orders-list` (referenti) | `referenti/orders-list.html` | Manager | **High** (~842 lines) | `/api/order/manage` CRUD + `/action/{action}`, `/aequos/available`, `/api/ordertype/select/manager`, `/api/home/order/status` |
| 7 | `user-order-details` | `user/order-details.html` | User | **High** (~469 lines) | order detail / item entry endpoints |
| 8 | `friend-order-details` | `user/friend-order.html` | User | **High** (~514 lines) | friend order split endpoints |
| 9 | `order-details` (referenti) | `referenti/order-details.html` | Manager | **Very high** (~1278 lines) | order management detail: by-product & by-user grids, quantities, boxes, shipping cost, accounting |
| 10 | `order-details-byuser` (referenti) | `referenti/order-details-byuser.html` | Manager | **High** (~505 lines) | `/api/order/manage/{id}/byUser/*`, `/shippingCost`, `/availableUsers` |

Total legacy UI logic: roughly **5,900 lines** of HTML + inline JS (templates) plus ~660 lines of shared scripts.

> Note: the two order-detail "referenti" pages (#9, #10) are the heaviest — they are grid-centric management screens with inline editing, aggregation, box filling, and accounting actions. They dominate the effort.

### No backend changes required (core migration)

Every legacy page calls endpoints that already exist on the REST API (`/api/supplier`, `/api/products`, `/api/order/manage`, `/api/order/user`, `/api/friend`, `/api/category`, `/api/ordertype`, `/api/configuration`, `/api/home`). Controllers are the ones documented in `backend.md`. The migration is therefore a **frontend-only** effort, except for cleanup steps (removing the `FrontendController` legacy mappings and templates at the very end).

---

## 3. Table strategy (no datagrid)

We will **not** adopt a heavyweight datagrid (MUI X `DataGrid`, AG Grid, etc.). Those would force an MUI v5 upgrade (the app is on MUI **v4**), clash with the existing theme, or pull paid features for inline edit / server data sources — none of which this app needs given its modest, client-side data volumes (tens-to-hundreds of rows per list).

Instead we use a **two-tier approach**:

- **Tier A — extend the existing `DataTable.tsx`** for the low/medium CRUD list pages (suppliers, configuration, products, user orders list, manage friends). These are simple lists where the legacy UI already pops a dialog to edit, so no inline-grid editing is required. Add capabilities as small, independent, client-side pieces:
  - `useTableSort` / `useTableFilter` hooks for column sorting and filtering.
  - Reuse the existing `TablePagination`.
  - A reusable `FormDialog` for add/edit (replacing the `jqxWindow` forms).
- **Tier B — TanStack Table v8 (headless)** for the genuinely complex grids: the two referenti order-detail pages (by-product / by-user with aggregation and box filling) and anywhere grouping/aggregation is needed. TanStack provides the sorting/filtering/grouping/aggregation **logic only** and is rendered through our own MUI `Table` components, so it stays on MUI v4 and matches the theme. It is one small, framework-agnostic dependency.

> Rationale: stays on MUI v4 and the existing theme, reuses what the app already has (`DataTable`, `TablePagination`, `react-select`, `@material-ui/pickers`, `exceljs`, notistack), and keeps all table state client-side. The one accepted tradeoff is replacing `jqxGrid` inline-cell editing with dialog-based editing (which most legacy pages already do); where true inline editing is needed on the quantity grids, TanStack supports editable cells via custom cell renderers (input + save-on-blur, wired by us).

### Other reusable building blocks to add

1. **A standard edit-dialog pattern.** One reusable `FormDialog` (MUI `Dialog` + fields + validation) to replace the per-page `jqxWindow` forms (used by both tiers).
2. **Shared field components** with existing library backing: a `SelectField` over `react-select` (replacing select2 / `jqxDropDownList`), a `DatePickerField` over `@material-ui/pickers` (replacing `jqxDateTimeInput`), and a numeric input.
3. **File upload / download helpers** for the products page (`import` upload, `export`/`download` via authenticated request) — the legacy code uses `document.location.href` for downloads, which won't carry the Bearer header; the React version should download via axios + blob.
4. **Excel export** — `exceljs` is already a dependency; standardize a helper so lists can export like `jqxGrid`.

---

## 4. Proposed approach

Incremental, strangler-style. Keep the iframe shell working; replace one page at a time by repointing its `/legacy/*` route from the `Legacy*Page` iframe wrapper to a new native React page. Because routing already lists each legacy page individually in `Routes.tsx`, swapping one route is a one-line change and is independently shippable and revertible.

### Phase 0 — Foundations (enables everything else)
- **Tier A:** extend `DataTable.tsx` with `useTableSort` / `useTableFilter` hooks (client-side), reusing the existing `TablePagination`.
- **Tier B:** add TanStack Table v8 and build a thin `GridTable` wrapper that renders TanStack's state through MUI `Table` components (for the complex order-detail grids in Phase 4).
- Build the shared `FormDialog`, `SelectField`, `DatePickerField`, numeric input, and download/export helpers.
- Establish the `useXxxAPI` hook convention for the new domains (mirroring `useUsersAPI.tsx`).
- Deliverable: a reference page (migrate **Suppliers**, the simplest) end-to-end to validate the Tier A toolkit.

### Phase 1 — Low-complexity pages
- **Suppliers** (done as the reference in Phase 0), **Configuration**.
- Establishes patterns for list + inline/edit + simple CRUD.

### Phase 2 — Medium pages
- **User orders list**, **Manage friends**.
- Introduces filters, select-driven searches, and friend-related conditional UI.

### Phase 3 — Products (high)
- Full catalog grid with category/supplier/order-type filters, price-list sync, Excel import/export, inline edit.
- First heavy use of the new grid + file upload/download helpers.

### Phase 4 — Order management (referenti), highest effort
- **Orders list (referenti)** including status workflow actions (`close`, `cancel`, `reopen`, `account`, …), the edit dialog, and Aequos "available orders" notifications.
- **Order details (referenti)** and **Order details by user** — the by-product/by-user grids, quantity/box editing, shipping cost, accounting.
- These mirror the backend order `workflow/` state machine; port behavior carefully and test each action.

### Phase 5 — User & friend order details
- **User order details**, **Friend order details** (item entry and friend split).

### Phase 6 — Decommission legacy
- Remove the `Legacy*Page` wrappers and `/legacy/*` iframe routes from `Routes.tsx`.
- Remove the legacy mappings from `FrontendController.java`, the `templates/legacy/**` files, and the `static/legacy-ui/**` assets (jQuery, jqWidgets, Bootstrap 3, etc.).
- Drop the `^/legacy-ui/.*` dev proxy entry in `vite.config.ts`.
- Verify no remaining references (e.g. menu items in `menuConfiguration.tsx` that point at `/legacy/*`).

---

## 5. Risks & considerations

- **Grid feature parity is the main risk.** jqWidgets `jqxGrid` is feature-rich (inline edit, server data adapters, aggregates, export). The two-tier table strategy (§3) covers it without a datagrid: Tier A (extended `DataTable`) for CRUD lists, Tier B (TanStack Table) for aggregation/grouping. Validate the Tier A toolkit on Suppliers in Phase 0 and the Tier B `GridTable` on the first referenti order-detail page in Phase 4. If parity proves hard on the heaviest two pages (#9, #10), keep them on the iframe longest.
- **Inline-cell editing.** The chosen approach replaces `jqxGrid` inline editing with dialog-based editing (as most legacy pages already do). The order-detail quantity grids are the exception; if they require true inline editing, implement editable cells via TanStack custom cell renderers (input + save-on-blur). Budget extra time for these specifically.
- **Behavioral fidelity on order workflow.** The referenti order pages encode business rules (which action buttons show per status, confirmation prompts, rounding on close). Treat the legacy JS as the spec and validate action-by-action against the backend `OrderStatusAction` set.
- **Auth storage divergence.** Legacy uses the `localStorage` token + Bearer header; the React app uses the cookie. Ported pages should use the axios layer only. No functional change expected, but confirm downloads/exports send credentials correctly (use axios + blob rather than `document.location.href`).
- **Italian UI strings** should be carried over verbatim to avoid surprising users; there is no i18n layer to update.
- **Menu config** in `menuConfiguration.tsx` references `/legacy/*` URLs and some external tools (`.exe`/`.jar`); update these as pages are migrated.
- **No test suite on the frontend today.** Consider adding at least smoke/integration tests (e.g. Vitest + React Testing Library) alongside the migrated pages, starting in Phase 0, to lock in parity.

---

## 6. Rough effort (relative, not calendar estimates)

| Phase | Scope | Relative effort |
|-------|-------|----------------|
| 0 | Table tiers (extend `DataTable` + TanStack `GridTable`) + shared components foundation | Large |
| 1 | Suppliers, Configuration | Small |
| 2 | User orders list, Manage friends | Medium |
| 3 | Products | Large |
| 4 | Referenti orders list + order details (×2) | Very large |
| 5 | User & friend order details | Large |
| 6 | Decommission legacy assets | Small |

The two referenti order-detail pages and the foundation phase account for the majority of the work. Everything else is comparatively routine CRUD-over-grid.

---

## 7. Recommended next step

If you want to proceed, the highest-leverage first move is **Phase 0**: build the Tier A table toolkit (extend `DataTable` with sort/filter hooks) and the shared `FormDialog` / field components, add TanStack Table for the Tier B `GridTable` wrapper, then migrate **Suppliers** as the reference implementation. That validates the whole approach on a low-risk page before committing to the heavy order-management screens. I can draft that foundation and the Suppliers page when you give the go-ahead.
