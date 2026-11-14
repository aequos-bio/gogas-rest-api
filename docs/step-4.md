# Phase 4 — Referente order management

> This phase is the highest-effort one and is delivered in **two steps**, each stopping for review:
> - **Step 1 — Orders list** (`referenti/orders-list.html`) — **done** (this document).
> - **Step 2 — Order details / management** (`referenti/order-details.html`, `order-details-byuser.html`) — pending.

Builds on the [Phase 0 foundation](./step-0.md) and Phases [1](./step-1.md)–[3](./step-3.md). Frontend-only; no backend changes.

---

## Step 1 — Manager orders list

> Status: **implemented and verified** — `tsc --noEmit` and `vite build` both pass. See §1.5.

Native replacement for the legacy `referenti/orders-list.html` jqxGrid — the order-management list with the full status workflow.

### 1.1 Files — `src/pages/orders/manager/`

| File | Purpose |
|------|---------|
| `types.d.ts` | `ManagerOrder` (OrderDTO JSON incl. `inviato`, `evaso`, `totalefattura`, `actions`), `OrderSearchFilter`, `OrderInput`, `OrderTypeSelectItem`, `AequosAvailableOrder`. |
| `useOrderManagerAPI.tsx` | Search, filter options, CRUD, `changeStatus`, and Aequos availability (endpoints below). |
| `OrderEditDialog.tsx` | Create/edit order dialog. |
| `CloseOrderDialog.tsx` | Rounding-mode dialog for the "chiudi" action. |
| `ManagerOrdersListPage.tsx` | Filters + results table + per-row action buttons + dialogs. |

### 1.2 Endpoints used
- `POST /api/order/manage/list` — search (`OrderSearchFilter`).
- `GET /api/ordertype/select/manager?firstEmpty=false&extended=true` — order types.
- `GET /api/home/order/status` — status list.
- `POST /api/order/manage`, `PUT /api/order/manage/{id}`, `DELETE /api/order/manage/{id}` — CRUD.
- `POST /api/order/manage/{id}/action/{code}[?roundType=]` — workflow actions.
- `GET /api/order/manage/aequos/available` — Aequos orders not yet opened.

### 1.3 Behaviour preserved from legacy

**Filters:** order type, status (multi-select, default **[0,1,2]** for admins, **[0,1]** otherwise), due-date range, delivery-date range, and **Evaso** (paid). Due-date-from defaults to ~2 months back. A **Ricerca** button runs the search; the page searches once on mount.

**Results table:** Tipo Ordine, Data Apertura / Chiusura / Consegna, Ora, Stato, Totale Ordine, Totale Fattura, Inviato, Pagato.

**Per-row workflow actions:** the backend computes the available actions per order (`OrderDTO.actions`, comma-separated, from `OrderWorkflowHandler.getAvailableActions`). Rather than the legacy "select row → toolbar" pattern, each row renders its own icon buttons for exactly those actions:

| Action | Icon tooltip | Effect |
|--------|--------------|--------|
| `modifica` | Modifica | open edit dialog |
| `elimina` | Elimina | delete (confirm) |
| `dettaglio` | Dettaglio | navigate to order details |
| `gestisci` | Gestisci | navigate to order details |
| `chiudi` | Chiudi | close dialog (rounding) → `action/close?roundType=` |
| `riapri` | Riapri | `action/reopen` (confirm) |
| `contabilizza` | Contabilizza | `action/contabilizza` (confirm) |
| `storna` | Storna | `action/tornachiuso` (confirm) |
| `cancel` | Annulla | `action/cancel` (confirm) |
| `undocancel` | Ripristina | `action/undocancel` (confirm) |

Each destructive/stateful action shows a confirmation dialog with the same wording as the legacy prompts.

**New order / edit:** `OrderEditDialog` — order type (locked on edit), opening/due/delivery dates, due hour (0–23), external link (only for external types), and "Sincronizza il listino alla creazione" (only for Aequos types, only when creating). Opening date must precede due and delivery dates.

**Close action:** `CloseOrderDialog` — rounding mode select (0 = secondo soglia, 1 = per eccesso, 2 = per difetto), posted as `roundType`.

**Aequos available orders:** on load, `GET …/aequos/available` results are surfaced as persistent notistack info notifications ("È disponibile un nuovo ordine di …"), each with two actions: **Aprilo ora** — opens the new-order dialog prefilled with that order's type and dates and the sync-listino box checked (as in the legacy `newItemFromAequos`) — and **Chiudi** to dismiss.

### 1.4 Detail navigation (temporary)

The `dettaglio` / `gestisci` actions navigate to `/legacy/orderdetails?orderId=…`, a **new legacy iframe route** (`LegacyManagerOrderDetailsPage` → `/legacy-ui/order-details`). This keeps the list fully functional now; **Step 2** replaces that route with the native order-details/management page.

### 1.5 Routing changes — `src/pages/Routes.tsx`
- Imported `ManagerOrdersListPage` and `LegacyManagerOrderDetailsPage`.
- Repointed `/legacy/orderslist` → `ManagerOrdersListPage`.
- Added `/legacy/orderdetails` → `LegacyManagerOrderDetailsPage` (temporary, for Step 2).
- Removed the now-unused `LegacyOrderManagerPage` import.

The "Gestione ordini" menu item keeps working unchanged.

### 1.6 Verification

Verified locally with Node 18.19.1 / npm 9.2.0, from `gogas-service/frontend`:

| Step | Command | Result |
|------|---------|--------|
| Type-check | `npx tsc --noEmit` | **Pass** (exit 0). |
| Production build | `npx vite build` | **Pass** — built in ~22s. |

No fixes required. Build run as `vite build` directly, so `buildManager.sh` did not run; the `dist/` output was removed afterwards.

### 1.6b Post-review refinements

Applied after the first review of Step 1 (all verified with `tsc` + `vite build`):
- **Aequos "Aprilo ora"** one-click implemented (see §1.3) — `OrderEditDialog` gained an optional `prefill` prop.
- **Action code fix:** the "Contabilizza" button posts to `action/contabilizza` (not `account`), matching `OrderWorkflowHandler`.
- **Order dialog layout:** each date on its own row, with "Data chiusura" + "Ora chiusura" sharing one row.
- **Inviato / Pagato columns:** render a tick icon for true and a dash for false.
- **Currency formatting:** `Amount` columns now format with Italian locale + euro (`1.234,56 €`). This was a change to the shared `SortableTable` formatter, so it applies to all `Amount` columns across every migrated page (Totale Ordine, Totale Fattura, Importo, Prezzo unità, …).

### 1.7 Files added / changed (Step 1)

**Added**
- `src/pages/orders/manager/types.d.ts`
- `src/pages/orders/manager/useOrderManagerAPI.tsx`
- `src/pages/orders/manager/OrderEditDialog.tsx`
- `src/pages/orders/manager/CloseOrderDialog.tsx`
- `src/pages/orders/manager/ManagerOrdersListPage.tsx`

**Changed**
- `src/pages/legacy/LegacyPage.tsx` (added `LegacyManagerOrderDetailsPage`)
- `src/pages/Routes.tsx` (route swaps + imports)
- `src/components/table/SortableTable.tsx` (Italian euro formatting for `Amount` columns — shared)

---

## Step 2 — Manager order details / management

> Status: **implemented and verified** — `tsc --noEmit` and `vite build` pass. Built and reviewed in four sub-steps (2a–2d).

Native replacement for the legacy `referenti/order-details.html` and `order-details-byuser.html` — the heaviest screens in the migration. Two linked views toggle between each other: **by product** and **by user**.

### 2.1 The editable keyboard grid (2a) — `src/components/table/EditableGridTable.tsx`

The fast-entry behaviour of the legacy jqxGrid is preserved with a dedicated component:
- type-to-edit on a numeric cell;
- **Enter / Tab** commit the value and move to the next editable row (Shift+Tab: previous);
- **Arrow Up / Down** discard the current edit and move between editable rows;
- **Esc** cancels the edit;
- blur commits;
- editing is limited to the designated numeric column(s) (no full spreadsheet navigation, by design).

Columns are either read-only (`render`) or a single editable numeric column (`getValue` / `canEdit` / `decimals`), with an optional `footer` aggregate, `rowClassName`, and `onRowClick` + `selectedRowId` for master/detail selection. Commits call `onCommit(row, columnId, value) => Promise<boolean>`.

### 2.2 By-product view (2b) — `src/pages/orders/manager/detail/`

| File | Purpose |
|------|---------|
| `types.d.ts` | `OrderDetails`, `OrderByProduct`, `OrderItemByProduct`, by-user types, `SelectItem`. |
| `useOrderByProductAPI.tsx` | Header, by-product rows, per-product items, and all mutations + toolbar/invoice ops. |
| `OrderByProductPage.tsx` | Master/detail page. |
| `AddUserOrderDialog`, `ModifyPriceDialog`, `ReplaceProductDialog` | Supporting dialogs. |

- **Left (by-product) grid**: editable **"Colli da ordinare"**, aggregates, remaining-qty **semaphore colour** (green/orange/red), strike-through for cancelled, row actions (cancel/restore, undo, modify price). Columns adapt to synced/accounted state.
- **Right (product-detail) grid**: editable **"Qta ritirata"**, row actions (cancel/restore, undo, replace product). Selecting a product loads its users.
- **Dialogs**: add user order, modify price, **replace product** (explicit product picker instead of legacy drag-and-drop), distribute remaining (confirm).
- Endpoints: `PUT …/product/{id}/supplier`, `PUT …/item/{id}`, `PUT …/{type}/{rowId}/{action}`, `POST …/item`, `PUT …/product/{id}/price`, `PUT …/product/{id}/distribute`, `PUT …/item/{id}/replace`, `GET …/product/{id}/availableUsers`.

### 2.3 By-user view (2c)

| File | Purpose |
|------|---------|
| `useOrderByUserAPI.tsx` | Header, by-user rows, per-user items, net amount / movements / shipping cost. |
| `OrderByUserPage.tsx` | Master/detail page. |
| `AddMovementDialog` | User + importo, with "Conferma" and "Conferma e continua". |

- **By-user grid**: editable **"Costo Netto"** (only for external / non-computed orders), costo trasporto, costo totale (footer totals), a **conto-in-rosso** warning, delete-movement (external orders).
- **Shipping cost** input recomputes everyone's totals (`POST …/shippingCost`); re-applied after net/movement changes.
- **Per-user detail grid** (computed orders only): prodotto, qta rit, um, prezzo (read-only).
- Endpoints: `GET …/byUser/list`, `GET …/byUser/{id}`, `POST …/byUser/{id}`, `DELETE …/byUser/{id}`, `POST …/shippingCost`, `GET …/byUser/availableUsers`.

### 2.4 Toolbar & invoice panel (2d)

On the by-product page:
- **Toolbar** (editable): export for smistamento (`GET …/delivery/{id}/file`), upload quantities (`POST …/delivery/{id}/file`), print user sheets **PDF** and **XLS** (`…/byUser/sheets[?type=excel]`), Excel export (`…/export`), send to Aequos (`…/aequos/order/send`); plus send weights (`…/aequos/order/weights`) and sync (`…/aequos/order/synch`) when applicable, with a last-synchro label.
- **Invoice / contabilità panel** (accounted orders): shows numero/importo/data fattura, pagato, data pagamento, allegato. Admins edit via `InvoiceDataDialog` (`POST …/invoice/data`); attachment upload (`POST …/invoice/attachment`) / download (`GET …/invoice/attachment`).
- Downloads use the Phase 0 `downloadFile`; uploads use `apiPostMultipart`.

### 2.5 Fixes made during review

- **415 Unsupported Media Type on scalar bodies:** endpoints taking a bare number/string (`@RequestBody BigDecimal`/`String`) were rejecting axios' primitive bodies. Added `apiPutJson` / `apiPostJsonBody` helpers (explicit `JSON.stringify` + `application/json`) and used them for qty/colli/price/netto/shipping/replace.
- **Detail not refreshing after a master action:** the selected row was held as a stale object and action-button clicks bubbled to row selection, racing a stale re-fetch. Fixed by deriving the selection from the live array + a selection ref, refreshing both grids on every mutation, and `stopPropagation` on action cells.
- **Commit toasts:** success notifications on qty / colli / netto commits.
- Arrow-key navigation added to the editable grid; order dialog date layout; tick/dash and Italian-euro formatting (shared in `SortableTable`).

### 2.6 Routing & decommission

- `/legacy/orderdetails` → native `OrderByProductPage`; `/legacy/orderdetailsbyuser` → native `OrderByUserPage`; the two views toggle between each other.
- Removed the temporary manager-detail iframe wrappers and the now-dead legacy iframe exports for already-migrated pages. Only `LegacyUserOrderDetailsPage` and `LegacyFriendOrderDetailsPage` remain (Phase 5). The legacy `templates/` / `static/legacy-ui/` assets are left in place until Phase 6.

### 2.7 Known simplifications

- **Replace product** uses a product-picker dialog instead of the legacy drag-and-drop.
- The **conto-in-rosso** warning is a simple indicator; the legacy hover tooltip showing the user's last accounting movements is not reproduced.

---

## Migration status

| Phase | Page(s) | Status |
|-------|---------|--------|
| 0 | Foundation + Suppliers | Done ([step-0.md](./step-0.md)) |
| 1 | Configuration | Done ([step-1.md](./step-1.md)) |
| 2 | User orders list, Manage friends | Done ([step-2.md](./step-2.md)) |
| 3 | Products | Done ([step-3.md](./step-3.md)) |
| 4 · step 1 | Manager orders list | Done (this document) |
| 4 · step 2 | Manager order details / management | **Done** (this document) |
| 5 | User & friend order details | Next |
| 6 | Decommission legacy assets | Pending |

**Next:** Phase 5 — the **user & friend order details** pages (`user/order-details.html`, `user/friend-order.html`), the last iframe pages (`/legacy/ordersdetails`, `/legacy/friendorders`), after which Phase 6 removes the legacy `templates/legacy/**` and `static/legacy-ui/**` assets.
