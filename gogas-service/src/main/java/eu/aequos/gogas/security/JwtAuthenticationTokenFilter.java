package eu.aequos.gogas.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import eu.aequos.gogas.exception.UserNotAuthorizedException;
import eu.aequos.gogas.multitenancy.TenantRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Slf4j
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String COOKIE_NAME = "jwt-token";

    private static final List<String> SKIP_EXACT_PATHS = List.of(
            "/authenticate", "/favicon.png", "/api/user/password/reset"
    );

    private static final List<String> SKIP_STARTING_PATHS = List.of(
            "/login", "/info", "/static", "/assets"
    );

    @Autowired
    private JwtTokenHandler jwtTokenHandler;

    @Autowired
    private TenantRegistry tenantRegistry;

    private Cookie findAuthCookie(HttpServletRequest req) {
        if (req.getCookies() == null)
            return null;

        for (Cookie ck : req.getCookies()) {
            if (ck.getName().equals(COOKIE_NAME))
                return ck;
        }

        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        if (skipFilter(request)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String authToken = extractAuthTokenFromRequest(request);
            GoGasUserDetails userDetails = jwtTokenHandler.getUserDetails(authToken);

            if (userDetails != null && !isValidTenant(userDetails.getTenant(), request)) {
                log.warn("Missing or mismatching tenant id, user not authorized");
                throw new UserNotAuthorizedException();
            }

            if (userDetails != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                MDC.put("user", userDetails.getUsername());
            }

            chain.doFilter(request, response);

        } catch (TokenExpiredException ex) {
            if (isApiEndpoint(request))
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Token expired");
            else {
                response.sendRedirect("/login");
            }
        } catch (JWTVerificationException ex) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Token expired or not valid");
        } catch (UserNotAuthorizedException ex) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Utente o tenant non valido");
        }
    }

    private static boolean isApiEndpoint(HttpServletRequest request) {
        return request.getServletPath().startsWith("/api");
    }

    private boolean skipFilter(HttpServletRequest request) {
        String servletPath = request.getServletPath();

        return SKIP_EXACT_PATHS.stream().anyMatch(servletPath::equals) ||
                SKIP_STARTING_PATHS.stream().anyMatch(servletPath::startsWith);
    }

    private String extractAuthTokenFromRequest(HttpServletRequest request) {
        Cookie authTokenCookie = findAuthCookie(request);
        if (authTokenCookie != null)
            return authTokenCookie.getValue();

        String authTokenHeader = request.getHeader(TOKEN_HEADER);
        if (authTokenHeader != null && authTokenHeader.startsWith(TOKEN_PREFIX))
            return authTokenHeader.replace(TOKEN_PREFIX, "");

        return null;
    }

    private boolean isValidTenant(String userTenantId, HttpServletRequest request) throws UserNotAuthorizedException {
        String hostTenantId = tenantRegistry.extractFromHostName(request);
        return tenantRegistry.isValidTenant(hostTenantId) && hostTenantId.equals(userTenantId);
    }
}
