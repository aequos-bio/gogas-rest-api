package eu.aequos.gogas.security;

import eu.aequos.gogas.datasource.DataSourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint, Serializable {

    private static final long serialVersionUID = -8970718410437077606L;
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceRegistry.class);

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        LOGGER.warn("Unauthorized user request to {}", request.getPathTranslated());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        //response.sendRedirect("login");
    }
}
