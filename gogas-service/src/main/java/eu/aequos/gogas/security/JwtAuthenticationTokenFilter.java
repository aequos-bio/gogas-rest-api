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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private TenantRegistry tenantRegistry;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String authToken = request.getHeader(TOKEN_HEADER);

        GoGasUserDetails userDetails = null;

        if (authToken != null && authToken.startsWith(TOKEN_PREFIX)) {
            userDetails = jwtTokenUtil.getUserDetails(authToken.replace(TOKEN_PREFIX, ""));
        }

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
