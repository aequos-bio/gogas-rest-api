# GoGas — Technical Debt & Follow-ups

Items noted during the documentation review. To be handled later.

## Security

- [ ] **Externalize JWT signing keys.** The current and legacy JWT signing keys are hard-coded in `JwtTokenHandler` (`gogas-service`). Move them to configuration / environment variables / a secrets store.
- [ ] **Cookie-based auth is same-origin only.** The frontend sends no `Authorization` header and does not set `withCredentials`; authentication relies entirely on the `jwt-token` cookie being same-origin. A cross-origin frontend deployment would break login. Decide whether to keep same-origin only or add explicit token/header handling.

## Dependencies

- [ ] **Clean up stale Log4j entries.** The root `pom.xml` still contains `log4j-to-slf4j` / `log4j-api` 2.15.0 entries alongside the pinned 2.17.0 ones. Remove the stale 2.15.0 declarations.

## Notes

- These were identified while writing `docs/backend.md` and `docs/frontend.md`; see those files for the relevant context.
