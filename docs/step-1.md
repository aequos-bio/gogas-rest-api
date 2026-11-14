# Phase 1 — Configuration page

> Status: **implemented and verified** — type-check (`tsc --noEmit`) and production build (`vite build`) both pass. See §4.

Phase 1 of the [legacy UI → React migration](./legacy-migration-plan.md), building on the [Phase 0 foundation](./step-0.md). It migrates the **Configuration (Configurazione generale)** admin page — the second low-complexity page — from the legacy jQuery/jqxGrid template to a native React page, reusing the Phase 0 toolkit. Frontend-only; no backend changes.

---

## 1. What the legacy page did

`gogas-service/src/main/resources/templates/legacy/admin/configuration.html` rendered a single editable `jqxGrid`:

- Loaded all visible config items from `GET /api/configuration` (`{ key, value, description }`).
- Columns: **Chiave** (key) and **Descrizione** (description) were read-only (`cellbeginedit` returned false); only **Valore** (value) was inline-editable.
- The value could not be empty (grid cell validation).
- Saving a cell issued `PUT /api/configuration` with the whole row.
- No add, no delete. Guarded by `@IsAdmin` on the backend.

The backend `ConfigurationController` also exposes logo upload/download/remove endpoints, but those were **not** part of this page in the legacy UI, so they are intentionally left out to preserve parity.

---

## 2. React implementation — `src/pages/admin/configuration/`

| File | Purpose |
|------|---------|
| `types.d.ts` | `ConfigurationItem` model: `{ key, value, description }`, matching the backend `ConfigurationItemDTO`. |
| `useConfigurationAPI.tsx` | Data hook (same convention as `useSuppliersAPI`). `GET /api/configuration` → sorted list; `updateItem` → `PUT /api/configuration` with the full item. Outcomes via notistack. Returns `{ items, loading, reload, updateItem }`. |
| `ConfigurationEditDialog.tsx` | Edit dialog on the shared `FormDialog`. Shows **Chiave** and **Descrizione** read-only; **Valore** is the only editable field, required non-empty (`canSave`). |
| `ConfigurationPage.tsx` | `PageTitle` + search box; `SortableTable` with edit-only actions (`canAdd: false`, `canDelete: false`, `canEdit: true`), client-side pagination and column sorting. |

### Behaviour preserved from the legacy page
- Key and description are not editable; only the value is.
- The value cannot be empty — the Salva button stays disabled and the field shows an error until a non-empty value is entered.
- No add / no delete.

### Toolkit reused from Phase 0
`SortableTable`, `useTableFilter`, and `FormDialog` — no new shared components were needed, which validates the Phase 0 foundation on a second page.

### Routing change — `src/pages/Routes.tsx`
- Imported `ConfigurationPage`.
- Repointed the `/legacy/configuration` route from the iframe wrapper `LegacyConfigurationPage` to the native `ConfigurationPage`.
- Removed the now-unused `LegacyConfigurationPage` import.

The route path is unchanged, so the existing **"Configurazione generale"** menu item in `menuConfiguration.tsx` keeps working. The iframe wrapper remains exported from `legacy/LegacyPage.tsx` (unused) and will be removed in the Phase 6 decommission.

---

## 3. Difference vs. the legacy UX

The legacy grid edited the value **inline** in the cell. The React page edits it in a **dialog** instead — the deliberate, agreed tradeoff from the migration plan (dialog editing in place of jqxGrid inline editing). Same data, same validation, same endpoints.

---

## 4. Verification

Verified locally with Node 18.19.1 / npm 9.2.0, from `gogas-service/frontend`:

| Step | Command | Result |
|------|---------|--------|
| Type-check | `npx tsc --noEmit` | **Pass** (exit 0). |
| Production build | `npx vite build` | **Pass** — built in ~58s. |

No fixes were required this time. The build was run as `vite build` directly (not `npm run build`), so `buildManager.sh` did not run and the backend `templates/` / `static/` were untouched; the verification `dist/` output was removed afterwards.

---

## 5. Files added / changed

**Added**
- `src/pages/admin/configuration/types.d.ts`
- `src/pages/admin/configuration/useConfigurationAPI.tsx`
- `src/pages/admin/configuration/ConfigurationEditDialog.tsx`
- `src/pages/admin/configuration/ConfigurationPage.tsx`

**Changed**
- `src/pages/Routes.tsx` (route swap + imports)

---

## 6. Status of the migration

| Phase | Page(s) | Status |
|-------|---------|--------|
| 0 | Foundation + Suppliers | Done ([step-0.md](./step-0.md)) |
| 1 | Configuration | **Done** (this document) |
| 2 | User orders list, Manage friends | Next |
| 3 | Products | Pending |
| 4 | Referenti orders list + order details (×2) | Pending |
| 5 | User & friend order details | Pending |
| 6 | Decommission legacy assets | Pending |

**Next step:** Phase 2 — the medium-complexity pages (**User orders list** and **Manage friends**), introducing filters and select-driven searches. See [`legacy-migration-plan.md`](./legacy-migration-plan.md).
