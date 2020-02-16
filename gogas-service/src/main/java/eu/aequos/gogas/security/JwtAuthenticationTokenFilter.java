package eu.aequos.gogas.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
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

@Slf4j
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String COOKIE_NAME = "jwt-token";

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
        try {
            if (request.getServletPath().equals("/authenticate")) {
                chain.doFilter(request, response);
            } else {
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
            }
        } catch (JWTVerificationException ex) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Token non valido o scaduto");
        } catch (UserNotAuthorizedException ex) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Utente o tenant non valido");
        }
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
