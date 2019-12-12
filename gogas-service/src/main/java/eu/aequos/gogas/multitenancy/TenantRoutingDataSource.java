package eu.aequos.gogas.multitenancy;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private TenantRegistry tenantRegistry;

    public TenantRoutingDataSource(TenantRegistry tenantRegistry) {
        this.tenantRegistry = tenantRegistry;
        this.setTargetDataSources(tenantRegistry.getDataSourceMap());
    }

    @Override
    protected Object determineCurrentLookupKey() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();     // get request object
        if (attr == null)
            return "localhost";   // default data source TODO: throw error

        return Optional.ofNullable(tenantRegistry.extractFromHostName(attr.getRequest().getServerName()))
                .orElse("localhost");
    }
}
