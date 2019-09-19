package eu.aequos.gogas.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class CustomRoutingDataSource extends AbstractRoutingDataSource {

    private static final String TENANT_KEY = "tenantId";

    public static final String extractTenantId(HttpServletRequest request) {
        return request.getParameter(TENANT_KEY);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();     // get request object
        if (attr == null) {
            return "localhost";   // default data source TODO: throw error
        }

        return extractTenantId(attr.getRequest());
    }
}
