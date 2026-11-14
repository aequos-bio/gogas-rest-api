# Frontend Documentation

The frontend is a React + TypeScript single-page application located at `gogas-service/frontend`. It is built with Vite and, at build time, folded into the Spring Boot backend as static assets plus a Thymeleaf template, so in production it is served from the same origin as the API.

It is a partial rewrite of an older UI: several "legacy" pages are still embedded via `<iframe>` and coexist with the new React pages under one shell.

- **Framework:** React 17 (`ReactDOM.render`, classic mode)
- **Language:** TypeScript 4.9 (`strict` mode)
- **Build tool:** Vite 4 (`@vitejs/plugin-react`)
- **UI library:** Material-UI (MUI) v4
- **State:** Redux Toolkit + React Redux
- **Routing:** React Router v5
- **HTTP:** axios
- **UI language:** Italian (no i18n library — strings are hard-coded)

---

## 1. Technology stack

From `package.json`:

| Area | Library | Version |
|------|---------|---------|
| Core | `react` / `react-dom` | 17.0.2 |
| Build | `vite` / `@vitejs/plugin-react` / `typescript` | 4.5 / 3.1 / 4.9 |
| UI | `@material-ui/core` / `icons` / `lab` / `pickers` / `styles` | v4 |
| Dates | `@date-io/moment`, `moment-timezone` | — |
| Routing | `react-router-dom` | 5.3.4 |
| State | `@reduxjs/toolkit` / `react-redux` | 1.9 / 8.0 |
| HTTP | `axios` | 1.2.5 |
| Auth helpers | `jwt-decode`, `cookies-js` | — |
| Notifications | `notistack` | 1.0.10 |
| Utilities | `lodash`, `query-string`, `react-select`, `exceljs` | — |

`tsconfig.json`: `strict: true`, `target/module: ESNext`, `jsx: react-jsx`, `noEmit: true` (Vite transpiles; `tsc` is used only for type-checking during the build).

> MUI v4 and React Router v5 are both older major versions — worth noting when adding new libraries or upgrading.

---

## 2. Project structure

```
src/
├── main.tsx          # entry point: ReactDOM.render(<App/>) into #root
├── App.tsx           # providers: MUI ThemeProvider, Redux Provider, notistack -> <Routes/>
├── assets/           # images (logo, etc.)
├── components/       # shared/reusable UI
│   ├── navbar/           # NavBar, ChangePasswordDialog, PasswordField, useUserBalanceAPI
│   └── navigationmenu/   # NavigationMenu, MenuChapter, MenuItem, menuConfiguration
├── hooks/            # JwtHooks.tsx (useJwt)
├── store/            # Redux Toolkit store, slices, hydration, types
│   └── features/         # authentication / info / accounting slices
├── pages/            # route targets, organized by domain
│   ├── Routes.tsx
│   ├── login/            # Login, reset-password
│   ├── home/             # HomePage
│   ├── accounting/       # years, gasaccounting, gasmovements, invoicemanagement,
│   │                     #   useraccountingtotals, useraccountingdetail, reasons, accountingcodes
│   ├── admin/            # users, orderTypes, managers
│   └── legacy/           # LegacyPage.tsx (iframe wrappers for the old UI)
├── style/            # app.css
└── utils/            # axios_utils.ts (HTTP layer)
```

### Per-feature convention

Each feature folder follows the same pattern (the canonical example is `admin/users`):

- `XxxPage.tsx` — the route view.
- `useXxxAPI.tsx` — a custom hook holding all REST calls + error handling.
- `types.d.ts` — TypeScript models for the feature.
- Row/dialog components (e.g. `UserRow.tsx`, `UserEditDialog.tsx`).

### Main pages by domain

- **Home:** `home/HomePage`.
- **Accounting (admin, role `A`):** accounting years, reasons (causali), accounting codes, gas movements, user totals, invoices, GAS accounting, per-user detail. The totals/detail pages are reused for "friends" via a `friends={true}` prop.
- **Admin / management:** users, order types, managers (referenti).
- **Legacy (iframe):** order management, products, suppliers, configuration, order history/details, friend orders, manage friends — all rendered by `legacy/LegacyPage.tsx`.
- **Auth:** `login/Login`.

---

## 3. Routing

`src/pages/Routes.tsx` uses React Router v5 inside `<Router basename='/'>`. On mount it dispatches `init()` to load `/info` into the store. There are two stacked `<Switch>` blocks:

1. **NavBar switch** — renders nothing for `/login`, and `<NavBar/>` for every other route (so the app bar appears on all authenticated pages).
2. **Content switch** — `/login` → `Login`; everything else comes from a `privateRoutes` array mapped to `<PrivateRoute>`.

`privateRoutes` is a plain array of `{ path, component }` (or `{ path, render }` for the friends variants). It includes the native pages plus eight `/legacy/*` routes pointing at the iframe wrappers.

### Protected routes

`components/PrivateRoute.tsx` is the auth gate: it calls `useJwt()`. If a valid (non-expired) decoded JWT exists it renders the target; otherwise it `<Redirect>`s to `/login`, carrying `state.from = location` so the user can be sent back after login.

### Navigation menu

`components/navigationmenu/menuConfiguration.tsx` declares `menuItems: MenuChapter[]` — chapters "Utente", "Amici", "Contabilità [year]", "Gestione", "Referente". Each item is `{ label, url, icon, restrictions?, newWindow? }`.

`restrictions` controls visibility and is evaluated in `MenuChapter.tsx`:

| Restriction | Checked against |
|-------------|-----------------|
| `roles: string[]` | `jwt.role` (`U` / `S` / `A`) |
| `orderManager: boolean` | `jwt.manager` |
| `friendsEnabled: boolean` | `info['friends.enabled']` from the store |

`NavigationMenu.tsx` renders a MUI `Drawer`, replaces `:userId` in URLs with `jwt.id`, substitutes `[year]` in chapter labels with `accounting.currentYear`, and either `window.open`s (newWindow) or `history.push`es on click.

> Role gating in the menu is **client-side only**. The backend still enforces authorization server-side.

---

## 4. Authentication

### Login flow

`Login.tsx` submits username/password to the `login` async thunk (`store/features/authentication.slice.ts`), which `POST`s to `/authenticate`. On success it stores the returned token string in `state.authentication.jwtToken`; on failure it stores an `error_message` shown in a MUI `Alert`.

### Token storage & validity

- The token lives in Redux (`authentication.jwtToken`), and the whole Redux state is persisted to `localStorage` (key `state`) via `store/hydration.ts`.
- Both the store and the auth slice seed the initial token from a **`jwt-token` cookie** (`Cookies.get('jwt-token')`), which the backend sets on authentication.
- `logout()` expires the `jwt-token` cookie and dispatches `setLoggedOut`.
- `hooks/JwtHooks.tsx` (`useJwt`) decodes the token with `jwt-decode` and treats it as `null` when `exp` is in the past. This hook is the single source of truth for "am I logged in" and is used by `PrivateRoute`, `NavBar`, `NavigationMenu`, `MenuChapter`, and balance hooks.

### How the token reaches the backend — important

There is **no axios interceptor, no `Authorization` / `Bearer` header, and no `withCredentials`** in the frontend code. Authentication relies on the browser automatically sending the `jwt-token` **cookie** (same-origin in production, via the Vite proxy in dev). The JWT held in Redux is used purely client-side for routing, role gating, and display.

> **Gotcha:** because auth depends on the cookie, deploying the frontend on a different origin from the API would break authentication. Also, the JWT is mirrored in two places (the cookie and the `localStorage`-persisted Redux state), which must stay in sync.

---

## 5. API integration

`utils/axios_utils.ts` is a thin HTTP layer:

- `apiGet` / `apiPost` / `apiPut` / `apiDelete` wrap `axios.*`.
- `apiPostText` sends `Content-Type: text/plain`.
- `json<T>()` unwraps `response.data`; `apiGetJson<T>(url, data)` builds a query string and returns typed JSON; `apiPostJson<T>` posts and unwraps.
- `extractResponseFromError` guards with `axios.isAxiosError` and returns `error.response`.

**Base URL:** none — all calls use root-relative paths (`/api/...`, `/authenticate`, `/info`). In dev these hit the Vite proxy to `localhost:8081`; in production they are same-origin.

### Hook pattern

Each feature wraps its calls in a `useXxxAPI` hook that returns `{ data, loading, reload, ...mutations }`. Example (`pages/admin/users/useUsersAPI.tsx`):

- `reload()` sets `loading`, calls `apiGetJson<User[] | ErrorResponse>('/api/user/list')`, and distinguishes success from an `ErrorResponse` (`typeof === 'object' && .error`), surfacing failures via notistack `enqueueSnackbar(..., { variant: 'error' })`.
- Mutations (`apiPut` / `apiDelete`) show success/error snackbars and call `reload()` afterward. Error messages are read from `err.response.data.debugMessage || .message || .error`.

Response shapes (`ErrorResponse`, `DataResponse<T>`) live in `store/types.d.ts`.

---

## 6. Reusable components

| Component | Purpose |
|-----------|---------|
| `DataTable.tsx` | Generic MUI table driven by `Column[]` (`label`, `type` of String/Date/DateTime/Time/Number/Amount/Icon, `alignment`, `property`) and `Settings` (`canEdit/canDelete/canAdd/showHeader/showFooter/pagination`). Formats dates/amounts with moment, renders edit/delete buttons and an add FAB, and optional pagination. |
| `ActionDialog.tsx` | Confirmation dialog with a title/message, a Cancel ("Annulla") button, and configurable `actions: string[]` → `onAction(index)`. Used for delete/reset confirmations. |
| `PageTitle.tsx` | Standard page header (`h4`) with a right-aligned slot for action buttons via `children`. |
| `NavBar.tsx` | Fixed MUI `AppBar` on all authenticated routes. Shows the GAS name, the user's name and balance (red when negative), the menu drawer toggle, and an account menu ("Cambio password", "Disconnetti"). |
| `LoadingRow` / `LoadingListItem` | Skeleton placeholders while data loads. |
| `Logo.tsx` | Aequos logo image. |

---

## 7. State management & cross-cutting concerns

### Redux slices (`store/features`)

| Slice | Responsibility |
|-------|----------------|
| `authentication` | Login thunk, `jwtToken`, `error_message`, `running`, logout. |
| `info` | GAS config loaded from `/info` (`gas.nome`, rounding threshold, Aequos credentials, user name order `visualizzazione.utenti`, `friends.enabled`); also sets `document.title`. |
| `accounting` | `currentYear` (defaults to the current year). |

Typed hooks `useAppSelector` / `useAppDispatch` are defined in `store/store.ts`. The whole state tree is persisted to `localStorage` by `store/hydration.ts` (note: this includes the JWT and the info config).

### Theming & notifications

- MUI `createTheme` in `App.tsx` (primary green, secondary amber) via `ThemeProvider`; component styling via `makeStyles`; global CSS in `src/style/app.css`.
- `notistack` `SnackbarProvider` wraps the app; API hooks surface success/error via `enqueueSnackbar`.

### Legacy integration

`legacy/LegacyPage.tsx` embeds the old UI through `<iframe src="/legacy-ui/...">`, letting old and new UIs coexist under one SPA shell and navbar.

---

## 8. Build, run & backend integration

### Scripts (`package.json`)

| Script | Command | Purpose |
|--------|---------|---------|
| `dev` / `start` | `vite --port 3000` | Dev server with hot reload at `localhost:3000`. |
| `build` | `tsc && vite build && ./buildManager.sh` | Type-check, build `dist/`, then integrate into the backend. |
| `preview` | `vite preview` | Preview the production build. |

### Dev proxy (`vite.config.ts`)

The dev server proxies backend paths to `http://localhost:8081/`: `/authenticate`, `/info`, `/api`, `/documentation`, `/tools`, and `^/legacy-ui/.*`. This avoids CORS during development.

### Production integration (`buildManager.sh`)

After `vite build`, the script wires the SPA into the Spring Boot module:

```bash
rm -f ../src/main/resources/templates/singlepage.html
mv ./dist/index.html ../src/main/resources/templates/singlepage.html
mv ./dist/assets/*   ../src/main/resources/static/assets/
```

So Vite's `index.html` becomes a Thymeleaf template `singlepage.html`, and the hashed JS/CSS assets move into the backend's `static/assets`. In production the backend serves both the SPA and the REST API from the same origin, which is what makes the cookie-based authentication work without CORS or a configured base URL.

### Getting started (dev)

```bash
cd gogas-service/frontend
npm install
npm start        # http://localhost:3000, proxying API calls to :8081
```

Make sure the backend is running on port `8081` so the proxy has a target.
