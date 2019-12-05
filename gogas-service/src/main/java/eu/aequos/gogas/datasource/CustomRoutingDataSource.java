package eu.aequos.gogas.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class CustomRoutingDataSource extends AbstractRoutingDataSource {

    /**
     * Extract tenantID from the host name by taking the most internal subdomain
     * Example: gasname.aequos.eu -> gasname
     * @param request the HTTP request
     * @return the tenant id
     */
    public static final String extractTenantIdFromHostName(HttpServletRequest request) {
        return request.getServerName().split("\\.")[0];
    }

    @Override
    protected Object determineCurrentLookupKey() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();     // get request object
        if (attr == null) {
            return "localhost";   // default data source TODO: throw error
        }

        return extractTenantIdFromHostName(attr.getRequest());
    }
}
