# Phase 2 — User orders list & Manage friends

> Status: **implemented and verified** — type-check (`tsc --noEmit`) and production build (`vite build`) both pass. See §5.

Phase 2 of the [legacy UI → React migration](./legacy-migration-plan.md), building on the [Phase 0 foundation](./step-0.md) and [Phase 1](./step-1.md). It migrates the two **medium-complexity** user pages — **User orders list** (`user/orders-list.html`) and **Manage friends** (`user/manage-friends.html`) — introducing filters and select-driven searches. Frontend-only; no backend changes.

---

## 1. User orders list — `src/pages/orders/user/`

Native replacement for the legacy `user/orders-list.html` jqxGrid.

| File | Purpose |
|------|---------|
| `types.d.ts` | `UserOrder` (OrderDTO JSON: `idtipoordine`, `tipoordine`, dates, `codicestato`/`stato`, `numarticoli`, `totaleordine`, `amici`, `contabilizzabile`, `contabilizzato`), `OrderSearchFilter`, `SelectItem`. |
| `useUserOrdersAPI.tsx` | Loads filter options and runs the search. `GET /api/ordertype/select`, `GET /api/home/order/status`, `GET /api/friend/select` (to know if the user has friends), `POST /api/order/user/list`. Returns `{ orders, orderTypes, statuses, hasFriends, loading, loadFilters, search }`. |
| `UserOrdersListPage.tsx` | Filter form + results `SortableTable`. |

### Filters
- **Tipologia ordine** — `SelectField` (clearable), from `/api/ordertype/select`.
- **Stato** — `MultiSelectField`, from `/api/home/order/status`, defaulting to **Aperto (0) + Chiuso (1)** as in legacy.
- **Data Chiusura / Consegna (dal / al)** — four `DatePickerField`s. **Chiusura dal** defaults to ~2 months back (legacy default).
- A **Ricerca** button runs the search; the page also runs an initial search on mount.

Dates are converted from the picker's ISO (`YYYY-MM-DD`) to the backend's `DD/MM/YYYY` in the filter payload.

### Results table
Columns: Tipo Ordine, Data Apertura, Data Chiusura, Ora Chiusura, Data Consegna, Stato, Num. Articoli, Importo. Preserved behaviours:
- The **Contab.to amici** checkbox column is shown only when the current user **has friends** (`hasFriends`).
- A custom last column renders a **dettaglio** link and, when `amici && contabilizzabile`, a **Gestisci ripartizione amici** link.

### Navigation note
The detail and friend-split links navigate to the still-legacy iframe routes (`/legacy/ordersdetails?orderId=&userId=`, `/legacy/friendorders?orderId=`) via react-router's `useHistory`. Those two detail pages are migrated in **Phase 5**; until then the list integrates cleanly with the legacy shell.

---

## 2. Manage friends — `src/pages/friends/`

Native replacement for the legacy `user/manage-friends.html` jqxGrid + popup form.

| File | Purpose |
|------|---------|
| `types.d.ts` | `Friend` (UserDTO JSON) and `FriendInput` (create/update payload). |
| `useFriendsAPI.tsx` | `GET /api/friend/list`, `POST /api/friend`, `PUT/DELETE /api/friend/{id}`, and `GET /api/user/exists?username=` for availability. Returns `{ friends, loading, reload, usernameExists, createFriend, updateFriend, deleteFriend }`. |
| `FriendEditDialog.tsx` | Add/edit form on `FormDialog`. |
| `ManageFriendsPage.tsx` | `SortableTable` (nome, cognome, username, e-mail, attivo) with search, add/edit/delete + delete confirmation. |

### Behaviour preserved from legacy
- New friends are created with `ruolo = 'S'`, `attivo = true`, and `idReferente` = the current user's id (`jwt.id`).
- **On create:** all fields editable; **username is required and checked for uniqueness** via `/api/user/exists` before saving (shows "Username già esistente" on conflict); **password required**; e-mail validated if provided.
- **On edit:** username is locked; **password is optional** (left blank → unchanged).
- Delete shows a confirmation dialog with the friend's name.

---

## 3. New shared component

`src/components/form/MultiSelectField.tsx` — a multi-select over `react-select` (`isMulti`), the multi-value sibling of the Phase 0 `SelectField`, used for the status filter (replacing the legacy select2 multiple). It reuses `SelectField`'s `SelectOption` type and the same menu-portal/z-index handling.

---

## 4. Routing changes — `src/pages/Routes.tsx`
- Imported `UserOrdersListPage` and `ManageFriendsPage`.
- Repointed `/legacy/ordershistory` → `UserOrdersListPage` and `/legacy/managefriends` → `ManageFriendsPage`.
- Removed the now-unused `LegacyUserOrderPage` and `LegacyManageFriendsPage` imports.

The route paths are unchanged, so the existing **"Gestione amici"** menu item (and the links into the orders list) keep working. `/legacy/ordersdetails` and `/legacy/friendorders` still map to the legacy iframe wrappers (Phase 5).

---

## 5. Verification

Verified locally with Node 18.19.1 / npm 9.2.0, from `gogas-service/frontend`:

| Step | Command | Result |
|------|---------|--------|
| Type-check | `npx tsc --noEmit` | **Pass** (exit 0). |
| Production build | `npx vite build` | **Pass** — built in ~24s. |

No fixes were required. The build was run as `vite build` directly (not `npm run build`), so `buildManager.sh` did not run and the backend `templates/` / `static/` were untouched; the verification `dist/` output was removed afterwards.

---

## 6. Difference vs. the legacy UX

As with earlier phases, the legacy jqxGrids edited rows **inline / via jqxWindow popups**; the React pages use the shared `FormDialog`. The manage-friends grid had inline edit/delete buttons per row — now the standard table edit/delete actions. Same data, same endpoints, same validation rules.

---

## 7. Files added / changed

**Added**
- `src/components/form/MultiSelectField.tsx`
- `src/pages/orders/user/types.d.ts`
- `src/pages/orders/user/useUserOrdersAPI.tsx`
- `src/pages/orders/user/UserOrdersListPage.tsx`
- `src/pages/friends/types.d.ts`
- `src/pages/friends/useFriendsAPI.tsx`
- `src/pages/friends/FriendEditDialog.tsx`
- `src/pages/friends/ManageFriendsPage.tsx`

**Changed**
- `src/pages/Routes.tsx` (route swaps + imports)

---

## 8. Status of the migration

| Phase | Page(s) | Status |
|-------|---------|--------|
| 0 | Foundation + Suppliers | Done ([step-0.md](./step-0.md)) |
| 1 | Configuration | Done ([step-1.md](./step-1.md)) |
| 2 | User orders list, Manage friends | **Done** (this document) |
| 3 | Products | Next |
| 4 | Referenti orders list + order details (×2) | Pending |
| 5 | User & friend order details | Pending |
| 6 | Decommission legacy assets | Pending |

**Next step:** Phase 3 — **Products** (`admin/products.html`), the first high-complexity page: catalog grid with category/supplier/order-type filters, price-list sync, and Excel import/export (first use of the Phase 0 download/export helpers). See [`legacy-migration-plan.md`](./legacy-migration-plan.md).
