package eu.aequos.gogas.security;

import eu.aequos.gogas.multitenancy.TenantRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private TenantRegistry tenantRegistry;

    private Cookie findAuthCookie(HttpServletRequest req) {
        if (req.getCookies()!=null) {
            for(Cookie ck : req.getCookies()) {
                if (ck.getName().equals(COOKIE_NAME))
                    return ck;
            }
        }
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        GoGasUserDetails userDetails = null;

        Cookie authCookie = findAuthCookie(request);
        if (authCookie!=null) {
            userDetails = jwtTokenUtil.getUserDetails(authCookie.getValue());
        } else {
            String authToken = request.getHeader(TOKEN_HEADER);
            if (authToken != null && authToken.startsWith(TOKEN_PREFIX)) {
                userDetails = jwtTokenUtil.getUserDetails(authToken.replace(TOKEN_PREFIX, ""));
            }
        }
        // TODO: check jwt expiration
        if (isValidUser(userDetails, request) && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        chain.doFilter(request, response);
    }

    private boolean isValidUser(GoGasUserDetails userDetails, HttpServletRequest request) {
        if (userDetails == null)
            return false;
        String tenantId = tenantRegistry.extractFromHostName(request.getServerName());
        if (!tenantRegistry.isValidTenant(tenantId) || !tenantId.equals(userDetails.getTenant())) {
            log.warn("Missing or mismatching tenant id, user not authorized");
            return false;
        }

        return true; //TODO: check user on DB
    }
}
