# Phase 3 — Products

> Status: **implemented and verified** — type-check (`tsc --noEmit`) and production build (`vite build`) both pass. See §5.

Phase 3 of the [legacy UI → React migration](./legacy-migration-plan.md), building on the [Phase 0 foundation](./step-0.md) and Phases [1](./step-1.md)/[2](./step-2.md). It migrates **Products** (`admin/products.html`) — the first **high-complexity** page — from the legacy jQuery/jqxGrid template to a native React page. It is the first real use of the Phase 0 **download/export** helpers, plus a new multipart upload helper. Frontend-only; no backend changes.

---

## 1. What the legacy page did

`admin/products.html` was a catalogue manager for an order type:
- Filter by **order type** (manager scope), **category** (loaded per type), **stato** (ordinabili / non ordinabili / tutti) and **annullati** (sì / no / tutti).
- A jqxGrid listing products with an edit popup (type → category cascading dropdowns, supplier dropdown, numeric fields, "solo collo" checkbox).
- A toolbar whose buttons depend on whether the order type is **linked to Aequos**:
  - linked → **Sincronizza listino** (+ last-sync label);
  - not linked → **Importa listino** (Excel upload), **Esporta listino** (Excel download), **Nuovo prodotto**.
- Aequos-imported products: the edit fields are **locked** and the product **cannot be deleted**.

---

## 2. React implementation — `src/pages/admin/products/`

| File | Purpose |
|------|---------|
| `types.d.ts` | `Product` (ProductDTO JSON), `ProductInput`, `SynchroInfo`, `SelectItem`, `ProductFilter`. |
| `useProductsAPI.tsx` | All data access (see endpoint list below). Returns the products, order types, suppliers, loading state, and the search/CRUD/sync/import/export operations. |
| `ProductEditDialog.tsx` | Add/edit form on `FormDialog` with cascading type → category dropdowns, supplier select, numeric fields, "solo collo" checkbox, and Aequos field locking. |
| `ProductsPage.tsx` | Filters + conditional toolbar + results `SortableTable` + import file picker + delete confirmation. |

### Endpoints used
- `GET /api/ordertype/select/manager` — order types (manager scope).
- `GET /api/category/list/{typeId}` — categories for a type.
- `GET /api/supplier/select` — suppliers.
- `GET /api/ordertype/{typeId}/synchro/info` — Aequos link + last sync (`{ lastSynchro, aequosOrderId }`).
- `GET /api/products/list/{typeId}?category&available&cancelled` — search.
- `POST /api/products`, `PUT /api/products/{id}`, `DELETE /api/products/{id}` — CRUD.
- `PUT /api/products/{typeId}/sync` — sync with Aequos.
- `POST /api/products/{typeId}/import` — import Excel price list (multipart).
- `GET /api/products/list/{typeId}/export` — export Excel price list.

### Behaviours preserved from legacy
- **Cascading filters/dialog:** selecting an order type loads its categories and its Aequos sync info; in the dialog, changing the type reloads categories and resets the selected category.
- **Conditional toolbar:** Aequos-linked type → **Sincronizza listino** + "Ultima sincronizzazione: …"; otherwise → **Importa / Esporta listino** + **Nuovo prodotto**. A **Ricerca** button is always present. All actions are disabled until an order type is selected.
- **Aequos protection:** in the edit dialog the core fields are disabled for Aequos products; in the table, delete is blocked for them via `SortableTable`'s `canDeleteRow` (the legacy page showed an info notification instead).
- **Validation (dialog):** descrizione, U.M., prezzo > 0, peso collo > 0, produttore, tipo and categoria required; box U.M. required only when "solo collo" is checked; `multiplo` optional but > 0 when set.

### Import / export (Phase 0 helpers)
- **Export** uses `downloadFile` (axios blob) from `utils/download_utils.ts`, fixing the legacy `document.location.href` that dropped the auth header.
- **Import** uses a hidden `<input type="file">` (accept `.xls,.xlsx`) and a new `apiPostMultipart` helper (`utils/upload_utils.ts`) that posts `FormData`. On success it reports the number of updated articles and refreshes the list.

---

## 3. New shared helper

`src/utils/upload_utils.ts` — `apiPostMultipart(url, formData)`: posts a `multipart/form-data` body via axios. Added for the price-list import; reusable by future upload features (e.g. invoice attachments in later phases).

---

## 4. Routing change — `src/pages/Routes.tsx`
- Imported `ProductsPage`.
- Repointed `/legacy/products` → `ProductsPage`.
- Removed the now-unused `LegacyProductsManagerPage` import.

The route path is unchanged, so the existing **"Prodotti"** menu item (under "Referente") keeps working.

---

## 5. Verification

Verified locally with Node 18.19.1 / npm 9.2.0, from `gogas-service/frontend`:

| Step | Command | Result |
|------|---------|--------|
| Type-check | `npx tsc --noEmit` | **Pass** (exit 0). |
| Production build | `npx vite build` | **Pass** — built in ~22s. |

No fixes were required. The build was run as `vite build` directly (not `npm run build`), so `buildManager.sh` did not run and the backend `templates/` / `static/` were untouched; the verification `dist/` output was removed afterwards.

---

## 6. Difference vs. the legacy UX

The legacy jqxGrid showed one row per product with inline edit/delete buttons and an "A listino" inline checkbox. The React page keeps the same columns (incl. read-only "A listino" / "Solo collo" indicators) but edits via the shared `FormDialog` and uses the standard table edit/delete actions — the agreed dialog-over-inline tradeoff. Same data, endpoints, filters, toolbar logic and validation.

---

## 7. Files added / changed

**Added**
- `src/utils/upload_utils.ts`
- `src/pages/admin/products/types.d.ts`
- `src/pages/admin/products/useProductsAPI.tsx`
- `src/pages/admin/products/ProductEditDialog.tsx`
- `src/pages/admin/products/ProductsPage.tsx`

**Changed**
- `src/pages/Routes.tsx` (route swap + imports)

---

## 8. Status of the migration

| Phase | Page(s) | Status |
|-------|---------|--------|
| 0 | Foundation + Suppliers | Done ([step-0.md](./step-0.md)) |
| 1 | Configuration | Done ([step-1.md](./step-1.md)) |
| 2 | User orders list, Manage friends | Done ([step-2.md](./step-2.md)) |
| 3 | Products | **Done** (this document) |
| 4 | Referenti orders list + order details (×2) | Next |
| 5 | User & friend order details | Pending |
| 6 | Decommission legacy assets | Pending |

**Next step:** Phase 4 — the **Referenti order management** screens (`referenti/orders-list.html`, `referenti/order-details.html`, `referenti/order-details-byuser.html`), the highest-effort phase: the order status workflow actions plus the by-product / by-user grids with aggregation (first use of the Phase 0 **Tier B** `GridTable`). See [`legacy-migration-plan.md`](./legacy-migration-plan.md).
