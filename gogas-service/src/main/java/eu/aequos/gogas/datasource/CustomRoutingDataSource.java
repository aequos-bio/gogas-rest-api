package eu.aequos.gogas.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class CustomRoutingDataSource extends AbstractRoutingDataSource {

    public static final String extractTenantId(HttpServletRequest request) {
        String address = request.getServerName();
        int firstPoint = address.indexOf(".");
        if (firstPoint==-1) firstPoint = address.length();
        String tenantId = address.substring(0, firstPoint);
        return tenantId;
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
