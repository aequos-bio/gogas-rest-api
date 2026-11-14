# Phase 0 — Foundation & Suppliers reference page

> Status: **implemented and verified** — type-check (`tsc --noEmit`) and production build (`vite build`) both pass. See §6.

This is the first phase of the [legacy UI → React migration](./legacy-migration-plan.md). It builds the reusable table / dialog / field toolkit that every later phase depends on, following the agreed **two-tier table strategy** (extend the existing `DataTable` for CRUD lists; TanStack Table headless for the complex grids), and migrates the **Suppliers (Produttori)** page end-to-end as the reference implementation.

All work is frontend-only, under `gogas-service/frontend/src`. No backend changes were made.

---

## 1. Table toolkit — `src/components/table/`

### Tier A — extended `DataTable`

| File | Purpose |
|------|---------|
| `useTableSort.tsx` | Client-side, non-mutating, **stable** column sorting. Type-aware comparator (`String` / `Number` / `Amount` / `Date` / `DateTime` / `Time` / `Boolean`). Header toggle cycles asc → desc → cleared. Returns `{ sortedRows, sort, toggleSort, setSort }`. |
| `useTableFilter.tsx` | Client-side case-insensitive free-text filtering over a chosen set of properties, with an optional `extraPredicate` for structured/column filters. Returns `{ filteredRows, term, setTerm, clear }`. |
| `SortableTable.tsx` | The original `DataTable` plus sortable headers (via `useTableSort`), per-row action gating (`canEditRow` / `canDeleteRow`), optional custom cell renderers (`Column.render`), and client-side pagination. |

`SortableTable` is a **new** component — the existing `src/components/DataTable.tsx` was left untouched so current pages keep working. Its `Column` type is a superset of the original (adds `sortable?` and `render?`, and a `Boolean` type).

### Tier B — TanStack Table

| File | Purpose |
|------|---------|
| `GridTable.tsx` | Headless **TanStack Table v8** rendered through MUI v4 table components. Supports column **sorting**, **grouping** (expand/collapse group rows with counts), **aggregation** (`aggregatedCell`), and a **footer** totals row. Covers the jqWidgets `jqxGrid` features the referenti order-detail pages (Phase 4) rely on, without adopting a visual datagrid. |

---

## 2. Form toolkit — `src/components/form/`

| File | Purpose |
|------|---------|
| `FormDialog.tsx` | Shared add/edit dialog built on MUI `Dialog`. Standard **Annulla / Salva** actions, confirm button disabled until `canSave`, in-flight spinner (`saving`) that blocks closing. Form layout + validation live in the caller via `children` / `canSave`. Replaces the per-page legacy `jqxWindow` forms. |
| `SelectField.tsx` | Single-select over `react-select` (replaces select2 / `jqxDropDownList`). Simple `{ value, label }` options, optional clear, reports the raw value. Menu is portalled to `document.body` with a high z-index so it isn't clipped inside dialogs. |
| `DatePickerField.tsx` | Date field over `@material-ui/pickers` `KeyboardDatePicker` + `@date-io/moment` (replaces `jqxDateTimeInput` / bootstrap-datepicker). Displays Italian `DD/MM/YYYY`, emits an ISO `YYYY-MM-DD` string. Mirrors the existing `MuiPickersUtilsProvider` + `libInstance={moment}` pattern already used in the app. |
| `NumberField.tsx` | Numeric input (replaces `jqxNumberInput`). Keeps a `number \| undefined`, accepts comma or dot decimals, clamps to `min` / `max`, limits `decimals`, optional end adornment (e.g. `€`). |

---

## 3. Utilities — `src/utils/download_utils.ts`

| Export | Purpose |
|--------|---------|
| `saveBlob(blob, filename)` | Triggers a browser download from an in-memory Blob via a temporary object URL. |
| `downloadFile(url, fallbackFilename)` | Authenticated file download through **axios** (so the request carries the usual credentials/headers), honouring `Content-Disposition` for the filename. Fixes the legacy `document.location.href` download that dropped the auth header. |
| `exportToExcel(rows, columns, filename, sheetName?)` | Builds and downloads an `.xlsx` from rows + column definitions, using the existing `exceljs` dependency. Bold header row, configurable widths. Replaces the legacy `jqxGrid` export. |

---

## 4. Suppliers reference page — `src/pages/admin/suppliers/`

Full migration of the legacy `templates/legacy/admin/suppliers.html` to a native React page.

| File | Purpose |
|------|---------|
| `types.d.ts` | `Supplier` model matching the backend `SupplierDTO` JSON: `{ id, ragionesociale, provincia, idesterno }`. |
| `useSuppliersAPI.tsx` | Data hook following the `useUsersAPI` convention. `GET /api/supplier/list`, `POST /api/supplier`, `PUT /api/supplier/{id}`, `DELETE /api/supplier/{id}`. Returns `{ suppliers, loading, reload, createSupplier, updateSupplier, deleteSupplier }`, surfacing outcomes via notistack. |
| `SupplierEditDialog.tsx` | Add/edit form on `FormDialog` with required `Ragione Sociale` + `Provincia`, matching the legacy validation. |
| `SuppliersPage.tsx` | `PageTitle` + search box + Excel export, `SortableTable` with pagination, delete confirmation via `ActionDialog`. |

**Preserved business rule:** auto-imported suppliers (those with an `idesterno`) cannot be edited or deleted — enforced through `SortableTable`'s `canEditRow` / `canDeleteRow` (the legacy page showed an info notification instead).

### Routing change — `src/pages/Routes.tsx`
- Imported `SuppliersPage`.
- Repointed the `/legacy/suppliers` route from the iframe wrapper `LegacySuppliersManagerPage` to the native `SuppliersPage`.
- Removed the now-unused `LegacySuppliersManagerPage` import.

Because the route path is unchanged, the existing **"Produttori"** menu item in `menuConfiguration.tsx` continues to work with no change. The iframe wrapper is still exported from `legacy/LegacyPage.tsx` (unused) and will be removed in the Phase 6 decommission.

---

## 5. Dependency added

`gogas-service/frontend/package.json`:

```json
"@tanstack/react-table": "^8.21.3"
```

TanStack Table v8 supports React 16.8+, so it is compatible with the app's React 17. It is a single, framework-agnostic, headless dependency (no datagrid, no MUI v5 upgrade).

---

## 6. Verification

Verified locally with Node 18.19.1 / npm 9.2.0:

| Step | Command | Result |
|------|---------|--------|
| Install deps | `npm install` | OK — 344 packages, including `@tanstack/react-table`. |
| Type-check | `npx tsc --noEmit` | **Pass** (exit 0) after one fix (below). |
| Production build | `npx vite build` | **Pass** — 6,350 modules transformed in ~49s. |

**Fix found during verification:** `tsc` flagged an implicit-`any` on the `react-select` `menuPortal` style callback in `SelectField.tsx` (`TS7006`). Resolved by typing the parameter (`(base: any) => ...`). Re-run was clean.

Notes:
- The production build was run as `vite build` directly (not `npm run build`), so `buildManager.sh` did **not** run and the backend `templates/` / `static/` were left untouched. The verification `dist/` output was removed afterwards.
- Pre-existing (not introduced by Phase 0): npm reports dependency vulnerabilities and MUI v4 / `@material-ui/pickers` deprecation warnings; Vite warns the single JS chunk is >500 kB. These relate to the app's existing stack, not this phase.

**To build for deployment** (folds the SPA into the backend), from `gogas-service/frontend`:

```bash
npm run build      # tsc + vite build + buildManager.sh
# or, to smoke-test only:
npm start          # dev server; open /legacy/suppliers
```

---

## 7. Files added / changed

**Added**
- `src/components/table/useTableSort.tsx`
- `src/components/table/useTableFilter.tsx`
- `src/components/table/SortableTable.tsx`
- `src/components/table/GridTable.tsx`
- `src/components/form/FormDialog.tsx`
- `src/components/form/SelectField.tsx`
- `src/components/form/DatePickerField.tsx`
- `src/components/form/NumberField.tsx`
- `src/utils/download_utils.ts`
- `src/pages/admin/suppliers/types.d.ts`
- `src/pages/admin/suppliers/useSuppliersAPI.tsx`
- `src/pages/admin/suppliers/SupplierEditDialog.tsx`
- `src/pages/admin/suppliers/SuppliersPage.tsx`

**Changed**
- `package.json` (added `@tanstack/react-table`)
- `src/pages/Routes.tsx` (route swap + imports)

---

## 8. Next step

Phase 1 — migrate **Configuration** (`admin/configuration.html`, the other low-complexity page), reusing this toolkit, then proceed through the phases in [`legacy-migration-plan.md`](./legacy-migration-plan.md).
