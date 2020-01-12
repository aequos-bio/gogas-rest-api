package eu.aequos.gogas.multitenancy;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    public TenantRoutingDataSource(TenantRegistry tenantRegistry) {
        this.setTargetDataSources(tenantRegistry.getDataSourceMap());
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getTenantId().orElse("localhost");
    }
}
