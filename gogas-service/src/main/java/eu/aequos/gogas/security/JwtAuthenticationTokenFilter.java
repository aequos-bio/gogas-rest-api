package eu.aequos.gogas.security;

import eu.aequos.gogas.datasource.CustomRoutingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationTokenFilter.class);

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

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

        String tenantId = CustomRoutingDataSource.extractTenantIdFromHostName(request);
        if (tenantId == null || !tenantId.equals(userDetails.getTenant())) {
            LOGGER.warn("Missing or mismatching tenant id, user not authorized");
            return false;
        }

        return true; //TODO: check user on DB
    }
}
